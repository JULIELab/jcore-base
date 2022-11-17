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
            String[] addonArray = addonTerms.get(input);
            if (null != addonArray) {
                // Only create a new output array when the default ArrayList size can't hold all the elements
                if (addonArray.length >= 10) {
                    output = new ArrayList<>(addonArray.length + 1);
                    output.add(input);
                }
                for (int i = 0; i < addonArray.length; i++) {
                    String addonTerm = addonArray[i];
                    output.add(addonTerm);
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
