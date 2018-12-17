package de.julielab.jcore.consumer.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.consumer.es.filter.Filter;
import de.julielab.jcore.consumer.es.preanalyzed.IToken;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;

public class DistributedField<T extends IToken> {

	private static final Logger log = LoggerFactory.getLogger(DistributedField.class);

	public enum Association {
		ARRAY, MAP
	}

	private Map<String, List<T>> tokenListsByFieldName;
	private String fieldBaseName;
	private Matcher termM;
	private Filter filterTemplate;
	private Map<Object, Filter> filterMap;
	private Object[][] termFieldIdMap;
	private String[][] aggFieldIdMap;
	private Matcher aggM;
	private Map<String, String> distributionMap;
	private Map<Matcher, String> distributionPatternMap;
	private boolean regex;
	private Association associationType;

	private DistributedField() {
		this.filterMap = new HashMap<>();
		this.tokenListsByFieldName = new HashMap<>();
	}

	/**
	 * Constructor for use of association array, i.e. when terms are enumerated numerically. Then, the fields for term
	 * <tt>i</tt> are to be found in the respective two-dimensional array at position <tt>i</tt>.
	 * 
	 * @param fieldBaseName
	 * @param termFieldIdMap
	 * @param aggregateFieldIdMap
	 * @param filter
	 */
	public DistributedField(String fieldBaseName, Object[][] termFieldIdMap, String[][] aggregateFieldIdMap,
			Filter filter) {
		this();
		this.fieldBaseName = fieldBaseName;
		this.termFieldIdMap = termFieldIdMap;
		this.aggFieldIdMap = aggregateFieldIdMap;
		this.filterTemplate = filter;
		this.associationType = Association.ARRAY;
		// TODO remove the semedico-term-style-ids and use arbitrary strings!!
		Pattern termP = Pattern.compile("^tid([0-9]+)");
		termM = termP.matcher("");
		Pattern aggP = Pattern.compile("^atid([0-9]+)");
		aggM = aggP.matcher("");
	}

	public DistributedField(String fieldBaseName, Map<String, String> distributionMap, boolean regex, Filter filter) {
		this();
		this.fieldBaseName = fieldBaseName;
		if (!regex) {
			this.distributionMap = distributionMap;
		} else {
			this.distributionPatternMap = new HashMap<>();
			for (Entry<String, String> entry : distributionMap.entrySet()) {
				String pattern = entry.getKey();
				Matcher m = Pattern.compile(pattern).matcher("");
				distributionPatternMap.put(m, entry.getValue());
			}
		}
		this.regex = regex;
		this.filterTemplate = filter;
		this.associationType = Association.MAP;
	}

	public String getFieldBaseName() {
		return fieldBaseName;
	}

	public void addTokenForTerm(IToken token) {
		if (null == token)
			return;

		if (associationType == Association.ARRAY) {
			termM.reset((CharSequence) token.getTokenValue());
			if (null != termFieldIdMap && termM.matches()) {
				String termNumberString = termM.group(1);
				int termNumber = Integer.parseInt(termNumberString);
				addTokenForTermFromArray(token, termNumber, termFieldIdMap);
			} else {
				aggM.reset((CharSequence) token.getTokenValue());
				if (null != aggFieldIdMap && aggM.matches()) {
					String termNumberString = aggM.group(1);
					int termNumber = Integer.parseInt(termNumberString);
					addTokenForTermFromArray(token, termNumber, aggFieldIdMap);
				}
			}
		}
		if (associationType == Association.MAP) {
			if (regex)
				addTokenFromTermFromPatternMap(token, distributionPatternMap);
			else
				addTokenFromTermStringMap(token, distributionMap);
		}
	}

	protected void addTokenFromTermStringMap(IToken token, Map<String, String> distributionMap) {
		Object category = distributionMap.get(token.getTokenValue());
		if (null != category) {
			String fieldname = fieldBaseName + category;
			addTokenToTokenList(token, fieldname);
		}
	}

	protected void addTokenFromTermFromPatternMap(IToken token, Map<Matcher, String> distributionPatternMap) {
		for (Matcher m : distributionPatternMap.keySet()) {
			m.reset((CharSequence) token.getTokenValue());
			if (m.matches()) {
				log.trace("Token {} was matched by pattern {}.", token.getTokenValue(), m.pattern());
				String category = distributionPatternMap.get(m);
				String fieldname = fieldBaseName + category;
				addTokenToTokenList(token, fieldname);
			} else
				log.trace("Token {} was not matched by pattern {}.", token.getTokenValue(), m.pattern());
		}
	}

	protected void addTokenForTermFromArray(IToken token, int termNumber, Object[][] facetIdMap) {
		if (termNumber < facetIdMap.length) {
			Object[] facetIds = facetIdMap[termNumber];
			for (int i = 0; null != facetIds && i < facetIds.length; i++) {
				Object facetId = facetIds[i];
				String fieldname = fieldBaseName + facetId;
				addTokenToTokenList(token, fieldname);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void addTokenToTokenList(IToken token, String fieldname) {
		Filter fieldFilter = getFilterForField(fieldname);
		List<String> filteredText;
		if (null != fieldFilter) {
			filteredText = fieldFilter.filter((String) token.getTokenValue());
		} else {
			filteredText = new ArrayList<>(1);
			filteredText.add((String) token.getTokenValue());
		}
		if (filteredText.size() > 1)
			throw new UnsupportedOperationException(
					"Currently, distributed fields do not support field filters that return more than one value. Filter value for text " + token
							.getTokenValue() + " was " + filteredText);
		if (!filteredText.isEmpty()) {
			List<T> tokenList = getTokenList(fieldname);
			// Adjust the position increment of the first token, if necessary (must not be 0).
			if (tokenList.isEmpty() && token.getTokenType() == IToken.TokenType.PREANALYZED
					&& ((PreanalyzedToken) token).positionIncrement == 0)
				((PreanalyzedToken) token).positionIncrement = 1;
			tokenList.add((T) token);
		}
	}

	public Set<String> namesForNonEmptyFields() {
		return tokenListsByFieldName.keySet();
	}

	private Filter getFilterForField(String fieldname) {
		if (null == filterTemplate)
			return null;
		Filter filter = filterMap.get(fieldname);
		if (null == filter) {
			filter = filterTemplate.copy();
			filterMap.put(fieldname, filter);
		}
		return filter;
	}

	/**
	 * Returns an existing list of tokens for the field with name <tt>fieldname</tt> or creates it.
	 * 
	 * @param fieldname
	 * @return
	 */
	public List<T> getTokenList(String fieldname) {
		List<T> list = tokenListsByFieldName.get(fieldname);
		if (null == list) {
			list = new ArrayList<T>();
			tokenListsByFieldName.put(fieldname, list);
		}
		return list;
	}


	public Map<String, List<T>> getTokenListsByFieldName() {
		return tokenListsByFieldName;
	}

}
