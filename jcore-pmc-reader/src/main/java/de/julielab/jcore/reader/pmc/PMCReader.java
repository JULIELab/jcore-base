/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.pmc;

import de.julielab.jcore.reader.pmc.parser.ElementParsingException;
import de.julielab.jcore.types.Header;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

@ResourceMetaData(name = "JCoRe Pubmed Central Reader", description = "Reads Pubmed Central documents from the NXML format")
@TypeCapability(outputs = {"de.julielab.jcore.types.TitleType", "de.julielab.jcore.types.Title", "de.julielab.jcore.types.TextObject", "de.julielab.jcore.types.Table", "de.julielab.jcore.types.SectionTitle", "de.julielab.jcore.types.Section", "de.julielab.jcore.types.PubType", "de.julielab.jcore.types.Paragraph", "de.julielab.jcore.types.OtherPub", "de.julielab.jcore.types.pubmed.OtherID", "de.julielab.jcore.types.pubmed.ManualDescriptor", "de.julielab.jcore.types.Keyword", "de.julielab.jcore.types.Journal", "de.julielab.jcore.types.pubmed.Header", "de.julielab.jcore.types.Footnote", "de.julielab.jcore.types.Figure", "de.julielab.jcore.types.Date", "de.julielab.jcore.types.CaptionType", "de.julielab.jcore.types.Caption", "de.julielab.jcore.types.AutoDescriptor", "de.julielab.jcore.types.AuthorInfo", "de.julielab.jcore.types.AbstractText", "de.julielab.jcore.types.AbstractSectionHeading", "de.julielab.jcore.types.AbstractSection"})
public class PMCReader extends PMCReaderBase {

    public static final String PARAM_INPUT = PMCReaderBase.PARAM_INPUT;
    public static final String PARAM_RECURSIVELY = PMCReaderBase.PARAM_RECURSIVELY;
    public static final String PARAM_SEARCH_ZIP = PMCReaderBase.PARAM_SEARCH_ZIP;
    public static final String PARAM_WHITELIST = PMCReaderBase.PARAM_WHITELIST;
    public static final String PARAM_EXTRACT_ID_FROM_FILENAME = PMCReaderBase.PARAM_EXTRACT_ID_FROM_FILENAME;
    private static final Logger log = LoggerFactory.getLogger(PMCReader.class);
    private CasPopulator casPopulator;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        try {
            casPopulator = new CasPopulator(pmcFiles);
        } catch (IOException e) {
            log.error("Exception occurred when trying to initialize NXML parser", e);
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(JCas cas) throws CollectionException {
        URI next = null;
        try {
            next = pmcFiles.next();
            casPopulator.populateCas(next, cas);
            if (extractIdFromFilename)
                ((Header) cas.getAnnotationIndex(Header.type).iterator().next()).setDocId(getIdFromFilename(next));
        } catch (ElementParsingException e) {
            log.error("Exception occurred when trying to parse {}", next, e);
            throw new CollectionException(e);
        } catch (NoDataAvailableException e) {
            log.error("Could not populate CAS due to preceding error.");
        }
        completed++;
    }
}
