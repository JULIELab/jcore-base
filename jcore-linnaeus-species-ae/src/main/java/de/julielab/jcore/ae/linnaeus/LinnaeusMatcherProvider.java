package de.julielab.jcore.ae.linnaeus;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import uk.ac.man.entitytagger.matching.Matcher;

public interface LinnaeusMatcherProvider extends SharedResourceObject {
    Matcher getMatcher();
}
