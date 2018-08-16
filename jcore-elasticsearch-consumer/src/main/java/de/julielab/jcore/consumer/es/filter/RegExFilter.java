package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters tokens by matching them to a regular expression.
 * @author faessler
 *
 */
public class RegExFilter extends AbstractFilter {

	private final Matcher matcher;
	private String regEx;
	private boolean negativeExpression;

	/**
	 *
	 * @param regEx The regular expression to match to token strings.
	 * @param negativeExpression If <tt>true</tt>, matched tokens are excluded. If <tt>false</tt>, tokens that do not match the filter are excluded.
	 */
	public RegExFilter(String regEx, boolean negativeExpression) {
		this.regEx = regEx;
		this.negativeExpression = negativeExpression;
		matcher = Pattern.compile(this.regEx).matcher("");
	}
	
	@Override
	public List<String> filter(String input) {
		newOutput();
		if (input != null) {
			matcher.reset(input);
			if (!negativeExpression && matcher.matches()) {
				output.add(input);
			} else if (negativeExpression && !matcher.matches()) {
				output.add(input);
			}
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new RegExFilter(regEx, negativeExpression);
	}

}
