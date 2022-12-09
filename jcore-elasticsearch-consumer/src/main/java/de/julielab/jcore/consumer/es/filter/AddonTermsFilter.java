package de.julielab.jcore.consumer.es.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddonTermsFilter extends AbstractFilter {

    private Map<String, String[]> addonTerms;
    private boolean replaceInputValue;
    private boolean removeInputWithoutAddons;

    public AddonTermsFilter(Map<String, String[]> addonTerms) {
        this.addonTerms = addonTerms;
    }
    public AddonTermsFilter(Map<String, String[]> addonTerms, boolean replaceInputValue) {
        this.addonTerms = addonTerms;
        this.replaceInputValue = replaceInputValue;
    }

    public AddonTermsFilter(Map<String, String[]> addonTerms, boolean replaceInputValue, boolean removeInputWithoutAddons) {
        this.addonTerms = addonTerms;
        this.replaceInputValue = replaceInputValue;
        this.removeInputWithoutAddons = removeInputWithoutAddons;
    }

    @Override
    public List<String> filter(String input) {
        newOutput();
        if (null != input) {
            String[] addonArray = addonTerms.get(input);
            // The input value should NOT be added (=removed) if either
            // * we want to replace input values if we have addon terms
            // * we want to remove input values if we do not have addon terms
            if (!(replaceInputValue && addonArray != null) && !(removeInputWithoutAddons && addonArray == null))
                output.add(input);
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
