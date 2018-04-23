/**
 * BinaryCASFromDBReader.java
 * <p>
 * Copyright (c) 2012, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * <p>
 * Author: faessler
 * <p>
 * Current version: 1.0
 * Since version:   1.0
 * <p>
 * Creation date: 12.12.2012
 */

/**
 *
 */
package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.XmiMetaData;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xml.XmiBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author faessler
 */
public class XmiDBReader extends DBReader implements Initializable {

    public static final String PARAM_STORE_XMI_ID = Initializer.PARAM_STORE_XMI_ID;
    public static final String PARAM_LOG_FINAL_XMI = Initializer.PARAM_LOG_FINAL_XMI;
    public static final String PARAM_READS_BASE_DOCUMENT = Initializer.PARAM_READS_BASE_DOCUMENT;
    public static final String PARAM_INCREASED_ATTRIBUTE_SIZE = Initializer.PARAM_INCREASED_ATTRIBUTE_SIZE;
    public static final String PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE = Initializer.PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE;
    private final static Logger log = LoggerFactory.getLogger(XmiDBReader.class);
    private Boolean storeMaxXmiId;
    private int xercesAttributeBufferSize;

    private XmiBuilder builder;
    private Boolean logFinalXmi;

    /**
     * Determines whether we read the namespace table and apply its contents to
     * the read XMI or not. For full-document storage, the namespaces are
     * included in the document XMI and adding them again would make the XMI
     * invalid.
     */
    private Boolean readsBaseDocument;

    private int numDataRetrievedDataFields;
    private int numAdditionalTables;
    private Initializer initializer;
    private CasPopulator casPopulator;

    /*
     * (non-Javadoc)
     *
     * @see de.julielab.jules.reader.DBReader#initialize()
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize();
        initializer = new Initializer(this, dbc, additionalTableNames, joinTables);
        initializer.initialize(context);
        casPopulator = new CasPopulator(dataTable, initializer, readDataTable, tableName);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas
     * .CAS)
     */
    @Override
    public void getNext(JCas jCas) throws CollectionException {
        log.trace("Reading next document.");
        initializeAnnotationTableNames(jCas);

        log.trace("Retrieving document data from the database.");
        byte[][] data = getNextArtifactData();
        log.trace("Got document data with {} fields.", null != data ? data.length : 0);

        populateCas(jCas, data);
    }

    private void populateCas(JCas jCas, byte[][] data) throws CollectionException {
        try {
            casPopulator.populateCas(data, jCas);
        } catch (CasPopulationException e) {
            throw new CollectionException(e);
        }
    }

    private void initializeAnnotationTableNames(JCas jCas) throws CollectionException {
        try {
            initializer.initializeAnnotationTableNames(jCas);
        } catch (ResourceInitializationException e) {
            throw new CollectionException(e);
        }
    }


    @Override
    protected String getReaderComponentName() {
        return getClass().getSimpleName();
    }


    @Override
    public void setStoreMaxXmiId(boolean storeMaxXmiId) {
        this.storeMaxXmiId = storeMaxXmiId;
    }


    @Override
    public void setXercesAttributeBufferSize(int size) {
        this.xercesAttributeBufferSize = size;
    }

    @Override
    public void setReadsBaseDocument(boolean readsBaseDocument) {
        this.readsBaseDocument = readsBaseDocument;
    }

    @Override
    public void setNumAdditionalTables(int numAdditionalTables) {
        this.numAdditionalTables = numAdditionalTables;
    }

    @Override
    public void setNumDataRetrievedDataFields(int numDataRetrievedDataFields) {
        this.numDataRetrievedDataFields = numDataRetrievedDataFields;
    }

    @Override
    public void setBuilder(XmiBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void setLogFinalXmi(boolean logFinalXmi) {
        this.logFinalXmi = logFinalXmi;
    }

    /**
     * The additional table names have been initialized by {@link DBReader#initialize(UimaContext)}
     * @return The additional table to be joined to the base document table.
     */
    @Override
    public String[] getAdditionalTableNames() {
        return additionalTableNames;
    }

    /**
     * The tables have been initialized by {@link DBReader#initialize(UimaContext)}
     * @return The tables to read.
     */
    @Override
    public String[] getTables() {
        return tables;
    }
}
