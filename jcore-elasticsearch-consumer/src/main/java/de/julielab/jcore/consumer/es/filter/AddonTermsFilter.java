package de.julielab.jcore.consumer.es.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddonTermsFilter extends AbstractFilter {

	private Map<String, String[]> addonTerms;

	public AddonTermsFilter(Map<String, String[]> addonTerms) {
		this.addonTerms = addonTerms;
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			output.add(input);
			String[] hypernymArray = addonTerms.get(input);
			if (null != hypernymArray) {
				output = new ArrayList<>(hypernymArray.length + 1);
				output.add(input);
				for (int i = 0; i < hypernymArray.length; i++) {
					String hypernym = hypernymArray[i];
					output.add(hypernym);
				}
			}
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new AddonTermsFilter(addonTerms);
	}

}
