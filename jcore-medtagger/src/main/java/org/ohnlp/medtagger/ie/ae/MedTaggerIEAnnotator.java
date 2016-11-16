/*******************************************************************************
 * Copyright: (c)  2013  Mayo Foundation for Medical Education and 
 *  Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
 *  triple-shield Mayo logo are trademarks and service marks of MFMER.
 *  
 *  Except as contained in the copyright notice above, or as used to identify 
 *  MFMER as the author of this software, the trade names, trademarks, service
 *  marks, or product names of the copyright holder shall not be used in
 *  advertising, promotion or otherwise in connection with this software without
 *  prior written authorization of the copyright holder.
 *   
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *   
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 *******************************************************************************/
package org.ohnlp.medtagger.ie.ae;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import de.julielab.jcore.types.ohnlp.Match;
import de.julielab.jcore.types.ohnlp.ConceptMention;
import org.ohnlp.medtagger.ie.util.ResourceUtilManager;
import de.julielab.jcore.types.Sentence;

/**
 * MedTaggerIEAnnotator extracts information based on specified patterns
 * 
 * @author Hongfang Liu
 * 
 * Sunghwan Sohn added
 * 	1)choose the longest (deleteInsideMatch()) 
 * 	2)delete duplicates (I don't think this has been implemented)
 * 	3)REMOVE functionality (remove matches with NORM=REMOVE) 
 * 	4)EXCLUSION functionality (exclude matches if its sentence contains a certain pattern)
 * 
 */
public class MedTaggerIEAnnotator extends JCasAnnotator_ImplBase {

	private String PARAM_RESOURCE_DIR = "Resource_dir";
	// default resource directory
	private String resource_dir = "medtaggerieresources";

	// lowercase will transform the sentence to lower case for pattern matching
	private Boolean lowerCase = true;
	//private Boolean hyphen2space = false;
	private Boolean punct2space = false;
	
	private Logger iv_logger = Logger.getLogger(getClass().getName());

	public ResourceUtilManager rum;

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		resource_dir = (String) aContext
				.getConfigParameterValue(PARAM_RESOURCE_DIR);
		rum = new ResourceUtilManager(resource_dir);
	}

	public void process(JCas jcas) {	 	
		FSIterator<? extends Annotation> senIter = jcas.getAnnotationIndex(Sentence.type).iterator();

		while (senIter.hasNext()) {
			Sentence sen = (Sentence) senIter.next();
			findMatch(rum.getHmRulePattern(), rum.getHmRuleNormalization(),
					rum.getHmRuleLocation(), sen, jcas);
			deleteInsideMatch(jcas); 
			removeInvalidMatch(jcas);
		}
	}

	public void findMatch(HashMap<Pattern, String> hmPattern,
			HashMap<String, String> hmNormalization,
			HashMap<String, String> hmLocation, Sentence sen, JCas jcas) {
		String senstr = sen.getCoveredText();
		if (lowerCase)
			senstr = senstr.toLowerCase();
        
		//test for CRS 
//		if(hyphen2space)
//			senstr = senstr.replaceAll("-", " ");		
		if(punct2space)
			senstr = senstr.replaceAll("[~`!.;\\-+_/,'\"]", " "); //don't use : (exclusion - dvt:)
		
		String matchRule = null;
		String matchNorm = null;

		// find the closest and longest rules
		//
		for (Iterator<Pattern> i = ResourceUtilManager.sortByValue(hmPattern)
				.iterator(); i.hasNext();) {
			Pattern p = (Pattern) i.next();
			for (Object o : ResourceUtilManager.findMatches(p, senstr)) {
				MatchResult mr = (MatchResult) o;
				String loc = hmLocation.get(hmPattern.get(p));
				if (loc.indexOf("SEC:") >= 0) {
					String secConstrain = loc.substring(4);
					if (secConstrain.indexOf("~" + sen.getSegment().getId() + "~") < 0)
						continue;
				}
				if (loc.equals("UC")) {
					String checkcase = sen.getCoveredText().substring(
							mr.start(), mr.end());
					if (!checkcase.toUpperCase().equals(checkcase))
						continue;
				}

				int matchStart = mr.start();
				int matchEnd = mr.end();
				matchRule = hmPattern.get(p);
				matchNorm = applyRuleFunctions(hmNormalization.get(matchRule),
						mr);

				Match Annot = null;

				//if (matchStart >= 0 && !matchNorm.equals("REMOVE")) {
				if (matchStart >= 0) { //Sunghwan's change, Feb-22-2013

					if (matchRule.startsWith("cm_")) {
						ConceptMention neAnnot = new ConceptMention(jcas,
								matchStart + sen.getBegin(), matchEnd
										+ sen.getBegin());
						neAnnot.setNormTarget(matchNorm);
						neAnnot.setDetectionMethod("Matched");
						if (matchNorm.indexOf(":") >= 0) {
							String[] es = matchNorm.split(":");
							neAnnot.setNormTarget(es[1]);
							neAnnot.setSemGroup(es[0]);
						}
						neAnnot.setSentence(sen);
						neAnnot.addToIndexes();
					} else {
						Annot = new Match(jcas, matchStart + sen.getBegin(),
								matchEnd + sen.getBegin());
						Annot.setFoundByRule(matchRule);
						Annot.setValue(matchNorm);
						Annot.setSentence(sen);
						Annot.addToIndexes();
					}
				}
			}
		}
	}

	public String applyRuleFunctions(String tonormalize, MatchResult m) {
		String normalized = "";
		Pattern paNorm = Pattern
				.compile("%([A-Za-z0-9]+?)\\(group\\(([0-9]+)\\)\\)");
		Pattern paGroup = Pattern.compile("group\\(([0-9]+)\\)");
		while ((tonormalize.matches(".*?%[A-Z]+%.*?")) || (tonormalize.contains("group"))) { 
			for (Object lmr : ResourceUtilManager.findMatches(paNorm,
					tonormalize)) {
				MatchResult mr = (MatchResult) lmr;
				if (!(m.group(Integer.parseInt(mr.group(2))) == null)) {
					String partToReplace = m.group(
							Integer.parseInt(mr.group(2))).replaceAll(
							"[\n\\s]+", " ");
					if (!(rum.getHmNormEntry(mr.group(1))
							.containsKey(partToReplace))) {
						iv_logger.warn("Problem associated with Function"
								+ ((MatchResult) mr).group(1)
								+ " and/or Regular Expression" + partToReplace);
					}
					tonormalize = tonormalize.replace(mr.group(), (String) rum
							.getHmNormEntry(mr.group(1)).get(partToReplace));
				} else {
					iv_logger.warn("Nothing to normalize in " + mr.group(1));

					tonormalize = tonormalize.replace(mr.group(), "");
				}
			}

			// replace other groups
			for (Object lmr : ResourceUtilManager.findMatches(paGroup,
					tonormalize)) {
				MatchResult mr = (MatchResult) lmr;
				tonormalize = tonormalize.replace(mr.group(),
						m.group(Integer.parseInt(mr.group(1))));
			}

			// replace lowercase
			Pattern paLowercase = Pattern.compile("%LC%\\((.*?)\\)");
			for (Object lmr : ResourceUtilManager.findMatches(paLowercase,
					tonormalize)) {
				MatchResult mr = (MatchResult) lmr;
				String substring = mr.group(1).toLowerCase();
				tonormalize = tonormalize.replace(mr.group(), substring);
			}
			// Uppercase
			Pattern paUppercase = Pattern.compile("%UC%\\((.*?)\\)");
			for (Object lmr : ResourceUtilManager.findMatches(paUppercase,
					tonormalize)) {
				MatchResult mr = (MatchResult) lmr;
				String substring = mr.group(1).toUpperCase();
				tonormalize = tonormalize.replace(mr.group(), substring);
			}

			// replace substrings
			Pattern paSubstring = Pattern
					.compile("%SUBSTR%\\((.*?),([0-9]+),([0-9]+)\\)");
			for (Object lmr : ResourceUtilManager.findMatches(paSubstring,
					tonormalize)) {
				MatchResult mr = (MatchResult) lmr;
				String substring = mr.group(1).substring(
						Integer.parseInt(mr.group(2)),
						Integer.parseInt(mr.group(3)));
				tonormalize = tonormalize.replace(mr.group(), substring);
			}
		}
		normalized = tonormalize;
		return normalized;
	}
	
	/**
	 * remove Match or ConceptMention (semG="Matched") inside of the others
	 * but does not remove duplication here
	 * NORM must be same or longer span's NORM must be REMOVE
	 * 
	 * eg1) he has chest pain.
	 * eg2) he has minor chest pain.
	 * RULENAME="r1",REGEXP="\bchest pain\b",LOCATION="NA",NORM="HF"
	 * RULENAME="r1_remove",REGEXP="\bminor chest pain\b",LOCATION="NA",NORM="REMOVE"
	 * <- eg1 "chest pain" will be caught but eg2 "minor chest pain" won't be caught
	 * 
	 * @param jcas
	 */
	@SuppressWarnings("rawtypes")
	public void deleteInsideMatch(JCas jcas) {
		//for Match type 
		Set<Match> toRemove = new HashSet<Match>();
		FSIterator matIter1 = jcas.getAnnotationIndex(Match.type).iterator();
		
		while(matIter1.hasNext()) {
			Match mat1 = (Match) matIter1.next();
			FSIterator matIter2 = jcas.getAnnotationIndex(Match.type).iterator();
			while(matIter2.hasNext()) {
				Match mat2 = (Match) matIter2.next();
				if( (mat2.getBegin()>mat1.getBegin() && mat2.getEnd()<mat1.getEnd())
						|| (mat2.getBegin()>=mat1.getBegin() && mat2.getEnd()<mat1.getEnd())
						|| (mat2.getBegin()>mat1.getBegin() && mat2.getEnd()<=mat1.getEnd()))
					if(mat1.getValue().equals(mat2.getValue()) 
							|| mat1.getValue().equals("REMOVE")) {
						toRemove.add(mat2);		
						}
			}
		}
		
		for(Match m : toRemove) 
			m.removeFromIndexes();		
		
			Set<ConceptMention> toCMRemove = new HashSet<ConceptMention>();
		
		FSIterator matIter = jcas.getAnnotationIndex(Match.type).iterator();
		Set<Match> remvMatch = new HashSet<Match>();	
		while(matIter.hasNext()) {
			Match mat = (Match) matIter.next();
			if(mat.getValue().equals("REMOVE")) {
				remvMatch.add(mat);
			}
		}
		
		FSIterator cmIter1 = jcas.getAnnotationIndex(ConceptMention.type).iterator();
		while(cmIter1.hasNext()) {
			ConceptMention cm1 = (ConceptMention) cmIter1.next();
			if(!cm1.getDetectionMethod().equals("Matched")) continue;
			
			//ConceptMention vs. ConceptMention to remove subsumed ConceptMention
			FSIterator cmIter2 = jcas.getAnnotationIndex(ConceptMention.type).iterator();
			while(cmIter2.hasNext()) {
				ConceptMention cm2 = (ConceptMention) cmIter2.next();	
				if(!cm2.getDetectionMethod().equals("Matched")) continue;
				if( (cm2.getBegin()>cm1.getBegin() && cm2.getEnd()<cm1.getEnd())
						|| (cm2.getBegin()>=cm1.getBegin() && cm2.getEnd()<cm1.getEnd())
						|| (cm2.getBegin()>cm1.getBegin() && cm2.getEnd()<=cm1.getEnd()))
					if(cm1.getNormTarget().equals(cm2.getNormTarget()))
						toCMRemove.add(cm2);					
			}
			
			//ConceptMention vs. Match (NORM=REMOVE) to remove subsumed ConceptMention
			for(Match m : remvMatch) {
				if( (cm1.getBegin()>m.getBegin() && cm1.getEnd()<m.getEnd())
						|| (cm1.getBegin()>=m.getBegin() && cm1.getEnd()<m.getEnd())
						|| (cm1.getBegin()>m.getBegin() && cm1.getEnd()<=m.getEnd()))
					toCMRemove.add(cm1);
			}
		}
		
		for(ConceptMention m : toCMRemove) 
			m.removeFromIndexes();
	}
	
	/**
	 * Remove the case that has NORM=REMOVE
	 * @param jcas
	 */
	@SuppressWarnings("rawtypes")
	public void removeInvalidMatch(JCas jcas) {
		Set<Match> toRemove = new HashSet<Match>();
		FSIterator matIter = jcas.getAnnotationIndex(Match.type).iterator();
		
		while(matIter.hasNext()) {
			Match mat = (Match) matIter.next();
			if(mat.getValue().equals("REMOVE"))
				toRemove.add(mat);
		}
		
		for(Match m : toRemove) 
			m.removeFromIndexes();
	}
	
	/**
	 * Remove the previous Match or ConceptMention (getDetectionMethod()="Matched") 
	 * if it is subsumed by the sentence that contains match found by 
	 * the rule that has NORM=EXCLUSION_previousNORM
	 * (if previousNORM=*, apply to all NORM values)
	 * 
	 * eg) 
	 * He has chest pain due to cough.
	 * 
	 * RULENAME="cm_r1a",REGEXP="\bchest pain\b",LOCATION="NA",NORM="HF"
	 * RULENAME="hf_exclude",REGEXP="\bcough\b",LOCATION="NA",NORM="EXCLUSION_HF"
	 * 
	 * -> Though "chest pain" is captured by cm_r1a, it will be removed because
	 * hf_exclude's NORM="EXCLUSION_HF" and the sentence of "cough" also contains 
	 * "chest pain" 
	 * 
	 * @param jcas
	 */
	@SuppressWarnings("rawtypes")
	public void removeExclusion(JCas jcas) {
		//key=NORM, val=List of begin of NORM's sentence|end NORM's sentence
		Map<String,List<String>> exclusion = new HashMap<String,List<String>>(); 
		FSIterator matIter = jcas.getAnnotationIndex(Match.type).iterator();
		
		Set<Match> excToRemove = new HashSet<Match>();
		while(matIter.hasNext()) {
			Match mat = (Match) matIter.next();		
			if(mat.getValue().startsWith("EXCLUSION_")) {
				String norm = mat.getValue().split("_")[1];
				String span =  mat.getSentence().getBegin()+"|"+ mat.getSentence().getEnd();
				
				List<String> spans = exclusion.get(norm);				
				if(spans==null)
					spans = new ArrayList<String>();
				spans.add(span);
				exclusion.put(norm, spans);
				
				excToRemove.add(mat);
			}
		}

		//remove exclusion match itself
		for(Match m : excToRemove) 
			m.removeFromIndexes();
		
		Set<Match> matchToRemove = new HashSet<Match>();
		matIter = jcas.getAnnotationIndex(Match.type).iterator();
		while(matIter.hasNext()) {
			Match mat = (Match) matIter.next();
			String norm = mat.getValue();			
			List<String> spans = exclusion.get(norm);
			
			if(spans!=null || 
					((spans=exclusion.get("*"))!=null)) {
				for(String s : spans) {
					String[] toks = s.split("\\|");
					int begin = Integer.parseInt(toks[0]);
					int end = Integer.parseInt(toks[1]);

					if( (mat.getBegin()>begin && mat.getEnd()<end)
							|| (mat.getBegin()>=begin && mat.getEnd()<end)
							|| (mat.getBegin()>begin && mat.getEnd()<=end)) {
						matchToRemove.add(mat);
						break;
					}				
				}
			}
		}
		
		//remove match subsumed by exclusion 
		for(Match m : matchToRemove) 
			m.removeFromIndexes();
		
		Set<ConceptMention> cmToRemove = new HashSet<ConceptMention>();
		FSIterator cmIter = jcas.getAnnotationIndex(ConceptMention.type).iterator();
		while(cmIter.hasNext()) {
			ConceptMention cm = (ConceptMention) cmIter.next();
			if(!cm.getDetectionMethod().equals("Matched")) continue;
			String norm = cm.getNormTarget();			
			List<String> spans = exclusion.get(norm);
			if(spans!=null || 
					((spans=exclusion.get("*"))!=null)) {
				for(String s : spans) {
					String[] toks = s.split("\\|");
					int begin = Integer.parseInt(toks[0]);
					int end = Integer.parseInt(toks[1]);

					if( (cm.getBegin()>begin && cm.getEnd()<end)
							|| (cm.getBegin()>=begin && cm.getEnd()<end)
							|| (cm.getBegin()>begin && cm.getEnd()<=end)) {
						cmToRemove.add(cm);
						break;
					}				
				}
			}			
		}
		
		for(ConceptMention cm : cmToRemove) 
			cm.removeFromIndexes();
	}
}