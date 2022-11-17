package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.Map;

/**
 * <p>Like {@link AddonTermsFilter} but accepts single string values instead of string arrays.</p>
 */
public class SingleAddonTermsFilter extends AbstractFilter {

    private Map<String, String> addonTerms;

    public SingleAddonTermsFilter(Map<String, String> addonTerms) {
        this.addonTerms = addonTerms;
    }

    @Override
    public List<String> filter(String input) {
        newOutput();
        if (null != input) {
            output.add(input);
            String addonTerm = addonTerms.get(input);
            if (null != addonTerm) {
                output.add(addonTerm);
            }
        }
        return output;
    }

    @Override
    public Filter copy() {
        return new SingleAddonTermsFilter(addonTerms);
    }

}
