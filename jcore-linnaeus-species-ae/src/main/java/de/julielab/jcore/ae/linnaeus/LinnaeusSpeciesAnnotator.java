/**
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author:
 * <p>
 * Description:
 **/
package de.julielab.jcore.ae.linnaeus;

import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import uk.ac.man.entitytagger.Mention;
import uk.ac.man.entitytagger.matching.Matcher;

import java.util.List;

/**
 * Uses the Linnaeus software (http://linnaeus.sourceforge.net/) to detect
 * species mentions and map them to NCBI taxonomy IDs.
 *
 * @author faessler
 *
 */
@ResourceMetaData(name="JCore LINNAEUS Species AE")
@TypeCapability(outputs = {"de.julielab.jcore.types.Organism", "de.julielab.jcore.types.ResourceEntry"})
public class LinnaeusSpeciesAnnotator extends JCasAnnotator_ImplBase {
    public static final String RES_KEY_LINNAEUS_MATCHER = "LinnaeusMatcher";
    public static final String PARAM_CONFIG_FILE = "ConfigFile";
    // The downloadable dictionaries from the Linnaeus page use this ID prefix
    // which we don't need.
    private static final String linnaeusIdPrefix = "species:ncbi:";
    private Matcher matcher;
    @ExternalResource(key = RES_KEY_LINNAEUS_MATCHER, description = "This resource dependency requires as resource the LINNAEUS configuration file. For each configuration file, one matcher will be created and shared between annotators.")
    private LinnaeusMatcherProvider matcherProvider;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        try {
            matcherProvider = (LinnaeusMatcherProvider) aContext.getResourceObject(RES_KEY_LINNAEUS_MATCHER);
            if (matcherProvider == null)
                throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DEPENDENCY_NOT_SATISFIED,new Object[]{ RES_KEY_LINNAEUS_MATCHER});
        } catch (ResourceAccessException e) {
            throw new ResourceInitializationException(e);
        }
        this.matcher = matcherProvider.getMatcher();
        super.initialize(aContext);
    }

    @Override
    public void process(JCas cas) throws AnalysisEngineProcessException {

        String text = cas.getDocumentText();
        List<Mention> mentions = matcher.match(text);
        for (Mention mention : mentions) {

            String mostProbableID = mention.getMostProbableID();
            if (mostProbableID.startsWith(linnaeusIdPrefix))
                mostProbableID = mostProbableID.substring(linnaeusIdPrefix.length());
            // String idsToString = mention.getIdsToString();
            Double[] probabilities = mention.getProbabilities();
            double maxprob = 0;
            if (probabilities != null) {
                for (int i = 0; i < probabilities.length; i++)
                    if (probabilities[i] != null && probabilities[i] > maxprob) {
                        maxprob = probabilities[i];
                    }
            }

            int start = mention.getStart();
            int end = mention.getEnd();

            try {
                Organism species = null;
                // species = (Organism)
                // JCoReAnnotationTools.getAnnotationAtOffset(cas,
                // Organism.class.getCanonicalName(), start, end);
                // if (null == species) {
                species = new Organism(cas);
                species.setBegin(mention.getStart());
                species.setEnd(mention.getEnd());
                species.setSpecificType("organism");
                species.addToIndexes();
                // }
                ResourceEntry resourceEntry = new ResourceEntry(cas);
                resourceEntry.setBegin(start);
                resourceEntry.setEnd(end);
                resourceEntry.setSource("NCBI Taxonomy");
                resourceEntry.setComponentId(getClass().getCanonicalName());
                resourceEntry.setEntryId(mostProbableID);
                resourceEntry.setConfidence(String.valueOf(maxprob));
                resourceEntry.addToIndexes();

                FSArray resourceEntryList = species.getResourceEntryList();
                if (null == resourceEntryList) {
                    resourceEntryList = new FSArray(cas, 1);
                    resourceEntryList.set(0, resourceEntry);
                } else {
                    resourceEntryList = JCoReTools.addToFSArray(resourceEntryList, resourceEntry);
                }
                species.setResourceEntryList(resourceEntryList);
                // species.setMostProbableSpeciesId(mostProbableID);
                // species.setAllIdsString(idsToString);
                // species.setAmbigous(mention.isAmbigous());
            } catch (SecurityException | IllegalArgumentException e) {
                throw new AnalysisEngineProcessException(e);
            }

        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        this.matcher = null;
        super.collectionProcessComplete();
    }

}
