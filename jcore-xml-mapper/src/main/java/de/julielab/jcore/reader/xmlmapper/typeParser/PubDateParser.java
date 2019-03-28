/** 
 * DateParser.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: bernd
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 10.11.2008 
 **/
package de.julielab.jcore.reader.xmlmapper.typeParser;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.genericTypes.FeatureTemplate;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.StandardTypeBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Medline TypeParser for the PubDate
 * 
 * @author weigel
 */
public class PubDateParser implements TypeParser {
	static Pattern p = Pattern.compile("<[^>]+>");

	Logger LOGGER = LoggerFactory.getLogger(PubDateParser.class);

	/**
	 * XML element, alternative to elements ELEMENT_DAY, ELEMENT_MONTH,
	 * ELEMENT_YEAR
	 */
	public static final String ELEMENT_MEDLINE_DATE = "MedlineDate";
	/**
	 * XML element nested in ELEMENT_PUB_DATE
	 */
	public static final String ELEMENT_DAY = "Day";
	/**
	 * XML element nested in ELEMENT_PUB_DATE
	 */
	public static final String ELEMENT_MONTH = "Month";
	/**
	 * XML element nested in ELEMENT_PUB_DATE
	 */
	public static final String ELEMENT_YEAR = "Year";
	/**
	 * 3-character abbreviations for months
	 */
	static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

	/**
	 * Pattern for PubDate year
	 */
	public static final String PATTERN_YEAR = "\\d\\d\\d\\d";

	/**
	 * Pattern for the context of PubDate month
	 */
	public static final String PATTERN_MONTH_CONTEXT = "[- ][a-zA-Z][a-zA-Z][a-zA-Z][- ]";

	/**
	 * Pattern for PubDate month
	 */
	public static final String PATTERN_MONTH = "[a-zA-Z][a-zA-Z][a-zA-Z]";

	/**
	 * Pattern for PubDate day
	 */
	public static final String PATTERN_DAY = "^\\d\\d\\d^\\d";

	public void parseType(ConcreteType concreteType, VTDNav nav, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception {
		Date date = new Date();

		VTDNav vn = nav.cloneNav();

		for (String xPath : concreteType.getTypeTemplate().getXPaths()) {
			AutoPilot ap = new AutoPilot(vn);
			ap.selectXPath(xPath);
			while (ap.evalXPath() != -1) {
				// ap.selectAttr("*");
				if (vn.toElement(VTDNav.FIRST_CHILD, "*")) {
					do {
						int vtIndex = vn.getCurrentIndex();
						int val = vn.getText();
						if (val != -1) {
							String pubDateContent = vn.toString(val);
							if (!pubDateContent.equals("")) {
								if (0==vn.compareTokenString(vtIndex, ELEMENT_DAY)) {
									date.day = pubDateContent;
								}
								if (0==vn.compareTokenString(vtIndex, ELEMENT_MONTH)) {
									date.month = parseMonthFormString(pubDateContent);
								}
								if (0==vn.compareTokenString(vtIndex, ELEMENT_YEAR)) {
									date.year = pubDateContent;
								}
								if (0==vn.compareTokenString(vtIndex, ELEMENT_MEDLINE_DATE)) {
									date = putMedlineDate(pubDateContent);
									break;
								}
							}
						}
					} while (vn.toElement(VTDNav.NEXT_SIBLING));
				}
				LOGGER.trace("date=" + date);

				for (FeatureTemplate featureTemplate : concreteType.getTypeTemplate().getFeatures()) {
					ConcreteFeature concreteFeature = new ConcreteFeature(featureTemplate);
					if (featureTemplate.getTsName().toLowerCase().equals("day")) {
						LOGGER.trace("setting day=" + date.day);
						concreteFeature.setValue(date.day);
					} else if (featureTemplate.getTsName().toLowerCase().equals("month")) {
						LOGGER.trace("setting month=" + date.month);
						concreteFeature.setValue(date.month);
					} else if (featureTemplate.getTsName().toLowerCase().equals("year")) {
						LOGGER.trace("setting year=" + date.year);
						concreteFeature.setValue(date.year);
					}
					concreteType.addFeature(concreteFeature);
				}
			}
		}
	}


	/**
	 * Converts an 3-character abbreviated month to an int
	 * 
	 * @param abbreviation
	 *            The abbreviation to be parsed
	 * @return A number corresponding to the 3-character abbriviation of the
	 *         month (1-based, that means, 'Jan' corresponds to 1)
	 */
	private String parseMonthFormString(String abbreviation) {

		for (int i = 0; i < MONTHS.length; i++) {
			if (MONTHS[i].equals(abbreviation)) {
				return String.valueOf(i + 1);
			}
		}
		return "0";
	}

	private class Date {

		String day = null;
		String month = null;
		String year = null;

		@Override
		public String toString() {
			return "[Date] year=" + year + ", month=" + month + ", day=" + day;
		}
	}

	/**
	 * Puts the content of the ELEMENT_DELINE_DATE tag into a
	 * de.julielab.jcore.types.Date
	 * 
	 * @param date
	 *            The date to be filled
	 * @param content
	 *            Text from what the date is extracted
	 */
	private Date putMedlineDate(String content) {

		Date date = new Date();

		ArrayList<Integer> years = new ArrayList<Integer>();
		ArrayList<String> days = new ArrayList<String>();
		ArrayList<String> months = new ArrayList<String>();

		Pattern yearPattern = Pattern.compile(PATTERN_YEAR);
		Pattern monthContextPattern = Pattern.compile(PATTERN_MONTH_CONTEXT);
		Pattern dayPattern = Pattern.compile(PATTERN_DAY);

		Matcher yearMatcher = yearPattern.matcher(content);
		Matcher dayMatcher = dayPattern.matcher(content);

		int maxYear = 0;
		while (yearMatcher.find()) {

			years.add(Integer.parseInt(yearMatcher.group()));
			maxYear = getMax(years);
		}
		if (maxYear > 0) {
			date.year = String.valueOf(maxYear);
			// this.setYear(String.valueOf(maxYear));
		}
		// check if there is a minus
		if (years.size() == 2) {
			// check what side of the minus is relevant
			if (hasExactlyOneMinus(content)) {
				monthContextPattern = getMonthContextPattern(content, monthContextPattern, maxYear);
			}
		}
		Matcher monthMatcher = monthContextPattern.matcher(content);
		while (monthMatcher.find()) {
			Pattern monthPattern = Pattern.compile(PATTERN_MONTH);
			Matcher monMatcher = monthPattern.matcher(monthMatcher.group());

			if (monMatcher.find()) {
				months.add(monMatcher.group());
			}
		}
		if (months.size() == 1) {
			date.month = parseMonthFormString(months.get(0));
			// this.setMonth(parseMonthFormString(months.get(0)));
		}
		while (dayMatcher.find()) {
			days.add(dayMatcher.group());
		}
		if (days.size() == 1) {
			date.day = days.get(0);
			// this.setDay(days.get(0));
		}
		return date;
	}

	/**
	 * Gets the Pattern monthContextPattern used to determin the month with some
	 * context, depending on which side of the minus the highest year was found
	 * 
	 * @param content
	 *            the whole date string
	 * @param monthContextPattern
	 *            the context pattern
	 * @param maxYear
	 *            the higherst year found
	 * @return the Pattern for extracting the month with some context (contains
	 *         exactly one candidate for a month)
	 */
	private Pattern getMonthContextPattern(String content, Pattern monthContextPattern, int maxYear) {
		Matcher leftYearMatcher = Pattern.compile(PATTERN_YEAR + ".*-").matcher(content);

		if (leftYearMatcher.find()) {

			String leftYearContext = leftYearMatcher.group();
			Matcher matcher = Pattern.compile(PATTERN_YEAR).matcher(leftYearContext);

			if (matcher.find()) {

				String leftYearStr = matcher.group();
				int leftYear = Integer.parseInt(leftYearStr);

				// get month from the left side of the minus
				if (leftYear == maxYear) {
					monthContextPattern = Pattern.compile(PATTERN_MONTH + ".*-");
					// get month from the right side of the minus
				} else {
					monthContextPattern = Pattern.compile("-.*" + PATTERN_MONTH);
				}
			}
		}
		return monthContextPattern;
	}

	/**
	 * Checks if content contains exactly one minus
	 * 
	 * @param content
	 *            String to be explored
	 * @return true, if there is exactly one minus
	 */
	private boolean hasExactlyOneMinus(String content) {

		Matcher minusMatcher = Pattern.compile("-").matcher(content);

		int countMinus = 0;
		while (minusMatcher.find()) {
			countMinus++;
		}
		if (countMinus == 1) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the maximum value of the ArrayList of Integers
	 * 
	 * @param values
	 *            The array to be explored
	 * @return Maximum of all Integer values as an int
	 */
	private int getMax(ArrayList<Integer> values) {

		int max = Integer.MIN_VALUE;

		for (int i = 0; i < values.size(); i++) {

			if (values.get(i) > max) {
				max = values.get(i);
			}
		}
		return max;
	}

	public TypeBuilder getTypeBuilder() {
		return new StandardTypeBuilder();
	}
}
