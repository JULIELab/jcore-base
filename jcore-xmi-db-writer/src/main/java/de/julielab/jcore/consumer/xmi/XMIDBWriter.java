/**
 * /**
 * XMIDBWriter.java
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
 * Creation date: 11.12.2012
 */

/**
 *
 */
package de.julielab.jcore.consumer.xmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import de.julielab.xml.util.XMISplitterException;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.XmiMetaData;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.XmiSplitter;
import de.julielab.xml.XmiSplitter.XmiSplitterResult;
import de.julielab.xmlData.dataBase.DataBaseConnector;

/**
 * @author faessler
 */
@ResourceMetaData(name="JCoRe XMI Database Writer", vendor = "JULIE Lab Jena, Germany", description = "This component " +
        "is capable of storing the standard UIMA serialization of documents in one or even multiple database tables. " +
        "The UIMA serialization format is XMI, an XML format that expressed an annotation graph. This component " +
        "either stores the whole annotation graph in XMI format in a database row, together with the document ID. " +
        "Alternatively, it makes use of the jcore-xmi-splitter to segment the annotation graph with respect to a " +
        "user specified list of annotation types. Then, the XMI data of each annotation type is extracted from the " +
        "document XMI data and stored in a separate table. The tables are created automatically according to the " +
        "primary key of the active table schema in the Corpus Storage System (CoStoSys) configuration file that is " +
        "also given as a parameter. The jcore-xmi-db-reader is capable of reading this kind of distributed annotation " +
        "graph and reassemble a valid XMI document which then cas be deserialized into a CAS. This component is part " +
        "of the Jena Document Information System, JeDIS.")
public class XMIDBWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_COSTOSYS_CONFIG = "CostosysConfigFile";
    public static final String PARAM_UPDATE_MODE = "UpdateMode";
    public static final String PARAM_DO_GZIP = "PerformGZIP";
    public static final String PARAM_STORE_ALL = "StoreEntireXmiData";

    public static final String PARAM_TABLE_DOCUMENT = "DocumentTable";
    public static final String PARAM_ADDITIONAL_TABLES = "AnnotationsToStore";
    public static final String PARAM_STORE_RECURSIVELY = "StoreRecursively";

    public static final String PARAM_BASE_DOCUMENT_ANNOTATION_TYPES = "BaseDocumentAnnotationTypes";

    public static final String PARAM_DELETE_OBSOLETE_ANNOTATIONS = "DeleteObsoleteAnnotations";
    public static final String PARAM_ATTRIBUTE_SIZE = "IncreasedAttributeSize";

    public static final String PARAM_COMPONENT_DB_NAME = "ComponentDbName";
    private static final Logger log = LoggerFactory.getLogger(XMIDBWriter.class);
    private static final String PARAM_STORE_BASE_DOCUMENT = "StoreBaseDocument";
    private DataBaseConnector dbc;
    @ConfigurationParameter(name = PARAM_UPDATE_MODE, description = "Is set to false, the attempt to write new data " +
            "into an XMI document or annotation table that already has data for the respective document, will result " +
            "in an error. If set to true, there will first occur a check if there already is XMI data for the " +
            "currently written document and, if so, the contents will be updated. It is important to keep in " +
            "mind that the update also includes empty data. That is, if an annotation type is specified in " +
            "'AdditionalTables' for which the current does not have data, possibly existing data will just be " +
            "deleted.")
    private Boolean updateMode;
    @ConfigurationParameter(name = PARAM_DELETE_OBSOLETE_ANNOTATIONS, mandatory = false, defaultValue = "false",
            description = "Boolean parameter that indicates whether annotations, that have become obsolete by updating " +
                    "referenced annotations, should be deleted from their table. This can help to avoid errors when there " +
                    "is a chance that the obsolete annotations could be read later, leading to invalid XMI. However, when " +
                    "those annotations will just be updated next, the overhead of deleting them would not be necessary. ")
    private Boolean deleteObsolete;
    @ConfigurationParameter(name = PARAM_DO_GZIP, description = "Determines if the XMI data should be stored " +
            "compressed or uncompressed. Without compression, the data will be directly viewable in a database " +
            "browser, whereas compressed data appears as opaque byte sequence. Compression is supposed to " +
            "reduce traffic over the network and save storage space on the database server.")
    private Boolean doGzip;
    @ConfigurationParameter(name = PARAM_ATTRIBUTE_SIZE, description = "Integer that defines the maximum attribute size for " +
            "the XMIs. Standard (parser wise) is 65536 * 8. It may be necessary to rise this value for larger documents " +
            "since the document text is stored as an attribute of an XMI element.")
    private Integer attributeSize;
    @ConfigurationParameter(name = PARAM_STORE_ALL, description = "Boolean parameter indicating if the whole document " +
            "should be stored as one large XMI data block. " +
            "In this case there must not be any annotations specified for selection and the 'StoreBaseDocument' " +
            "parameter will have no effect.")
    private Boolean storeAll;
    @ConfigurationParameter(name = PARAM_TABLE_DOCUMENT, description = "String parameter indicating the name of the " +
            "table where the XMI data will be stored (if StoreEntireXmiData is true) or " +
            "where the base document will be stored (if StoreBaseDocument is true). ")
    private String docTableName;
    private List<String> annotationsToStore;
    @ConfigurationParameter(name = PARAM_STORE_RECURSIVELY, description = "Only in effect when storing annotations " +
            "separately from the base document. If set to true, annotations that are referenced by other annotations, " +
            "i.e. are (direct or indirect) features of other annotations, they " +
            "will be stored in the same table as the referencing annotation. For example, POS tags may be store " +
            "together with tokens this way. If, however, a referenced annotation type is itself to be stored, " +
            "it will be segmented away and stored in its own table.")
    private Boolean recursively;
    @ConfigurationParameter(name = PARAM_STORE_BASE_DOCUMENT, description = "Boolean parameter indicating if the base " +
            "document should be stored as well when annotations are specified for selection. The base document is " +
            "the part of the XMI file that includes the document text. If you want to store annotations right with " +
            "the base document, specify those in the 'BaseDocumentAnnotationTypes' parameter.")
    private Boolean storeBaseDocument;
    @ConfigurationParameter(name = PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, mandatory = false, description = "Array " +
            "parameter that takes Java annotation type names. These names will be stored with the base document, " +
            "if the 'StoreBaseDocument' parameter is set to true.")
    private Set<String> baseDocumentAnnotationTypes;

    private XmiSplitter splitter;
    // Must be a linked HashMap so that the document table comes first when
    // iterating over all tables to insert data. This is required because the
    // annotation tables have a foreign key to the document table.
    private LinkedHashMap<String, List<XmiData>> serializedCASes = new LinkedHashMap<>();
    private Map<String, List<DocumentId>> tablesWithoutData = new HashMap<>();
    private String schemaDocument;
    private String schemaAnnotation;

    private String effectiveDocTableName;

    private MetaTableManager metaTableManager;

    private AnnotationTableManager annotationTableManager;

    private int headerlessDocuments = 0;

    private XmiDataInserter annotationInserter;

    @ConfigurationParameter(name = PARAM_COMPONENT_DB_NAME, description = " Subset tables store the name of the last " +
            "component that has sent data for a document. This parameter allows to specify a custom name for each CAS " +
            "DB Consumer. Defaults to the implementation class name.", defaultValue = "XMIDBWriter")
    private String componentDbName;

    private String subsetTable;
    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG, description = "File path or classpath resource location " +
            "of a Corpus Storage System (CoStoSys) configuration file. This file specifies the database to " +
            "write the XMI data into and the data table schema. This schema must at least define the primary key " +
            "columns that the storage tables should have for each document. The primary key is currently just " +
            "the document ID. Thus, at the moment, primary keys can only consist of a single element when using " +
            "this component. This is a shortcoming of this specific component and must be changed here, if necessary.")
    private String dbcConfigPath;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES, mandatory = false, description = "An array of qualified " +
            "UIMA type names. Annotations of those types are segmented away from the serialized document annotation " +
            "graph in XMI format for storage in separate tables. When the 'StoreRecursively' parameter is set to true, " +
            "annotations are stored together with referenced annotations, if those are not specified in the list " +
            "of additional tables themselves.")
    private String[] annotations;

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize
     * (org.apache.uima.UimaContext)
     */
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        checkParameters(aContext);

        dbcConfigPath = (String) aContext.getConfigParameterValue(PARAM_COSTOSYS_CONFIG);
        updateMode = aContext.getConfigParameterValue(PARAM_UPDATE_MODE) == null ? false
                : (Boolean) aContext.getConfigParameterValue(PARAM_UPDATE_MODE);
        deleteObsolete = aContext.getConfigParameterValue(PARAM_DELETE_OBSOLETE_ANNOTATIONS) == null ? false
                : (Boolean) aContext.getConfigParameterValue(PARAM_DELETE_OBSOLETE_ANNOTATIONS);
        doGzip = aContext.getConfigParameterValue(PARAM_DO_GZIP) == null ? false
                : (Boolean) aContext.getConfigParameterValue(PARAM_DO_GZIP);
        storeAll = (Boolean) aContext.getConfigParameterValue(PARAM_STORE_ALL) == null ? false
                : (Boolean) aContext.getConfigParameterValue(PARAM_STORE_ALL);
        docTableName = (String) aContext.getConfigParameterValue(PARAM_TABLE_DOCUMENT);
        storeBaseDocument = aContext.getConfigParameterValue(PARAM_STORE_BASE_DOCUMENT) == null ? false
                : (Boolean) aContext.getConfigParameterValue(PARAM_STORE_BASE_DOCUMENT);
        baseDocumentAnnotationTypes = Arrays.stream(
                Optional.ofNullable((String[]) aContext.getConfigParameterValue(PARAM_BASE_DOCUMENT_ANNOTATION_TYPES))
                        .orElse(new String[0]))
                .collect(Collectors.toSet());
        attributeSize = (Integer) aContext.getConfigParameterValue(PARAM_ATTRIBUTE_SIZE);
        componentDbName = (String) aContext.getConfigParameterValue(PARAM_COMPONENT_DB_NAME);

        if (componentDbName == null)
            componentDbName = getClass().getSimpleName();

        try {
            dbc = new DataBaseConnector(dbcConfigPath);
        } catch (FileNotFoundException e1) {
            throw new ResourceInitializationException(e1);
        }

        // TODO derive the field configuration programmatically from the active data table schema
        schemaDocument = doGzip ? "xmi_text_gzip" : "xmi_text";
        schemaAnnotation = doGzip ? "xmi_annotation_gzip" : "xmi_annotation";
        if (storeAll) {
            annotationsToStore = new ArrayList<String>();
            annotationsToStore.add(docTableName);
            recursively = false;
        } else {
            annotations = (String[]) aContext.getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
            if (null != annotations)
                annotationsToStore = new ArrayList<String>(Arrays.asList(annotations));
            else
                annotationsToStore = Collections.emptyList();
            recursively = (Boolean) aContext.getConfigParameterValue(PARAM_STORE_RECURSIVELY) == null ? false
                    : (Boolean) aContext.getConfigParameterValue(PARAM_STORE_RECURSIVELY);
        }

        try {
            annotationTableManager = new AnnotationTableManager(dbc, docTableName, annotationsToStore, schemaDocument,
                    schemaAnnotation, storeAll, storeBaseDocument);
        } catch (TableSchemaMismatchException e) {
            throw new ResourceInitializationException(e);
        }
        effectiveDocTableName = annotationTableManager.convertAnnotationTypeToTableName(docTableName, storeAll);
        // does currently only compare the primary keys...
        checkTableDefinition(effectiveDocTableName, schemaDocument);
        // Important: The document table must come first because the
        // annotation tables have foreign keys to the document table.
        // Thus, we can't add annotations for documents not in the
        // document table.
        if (storeBaseDocument) {
            serializedCASes.put(effectiveDocTableName, new ArrayList<XmiData>());
        }
        List<String> annotationsToStoreTableNames = new ArrayList<>();
        for (String annotation : annotationsToStore) {
            String annotationTableName = annotationTableManager.convertAnnotationTypeToTableName(annotation, storeAll);
            checkTableDefinition(annotationTableName, schemaAnnotation);
            serializedCASes.put(annotationTableName, new ArrayList<>());
            tablesWithoutData.put(annotationTableName, new ArrayList<>());
            annotationsToStoreTableNames.add(annotationTableName);
        }
        if (updateMode) {
            List<String> obsoleteAnnotationTableNames = annotationTableManager.getObsoleteAnnotationTableNames();
            if (!obsoleteAnnotationTableNames.isEmpty()) {
                log.info(
                        "Annotations from the following tables will be obsolete by updating the base document and will be deleted: {}"
                        , obsoleteAnnotationTableNames);
                for (String table : obsoleteAnnotationTableNames)
                    tablesWithoutData.put(table, new ArrayList<>());
            }
        }

        if (storeAll) {
            if (null != attributeSize) {
                splitter = new XmiSplitter(docTableName, attributeSize);
            } else {
                splitter = new XmiSplitter(docTableName);
            }
        } else {
            if (null != attributeSize) {
                splitter = new XmiSplitter(annotationsToStore, recursively, storeBaseDocument, docTableName,
                        baseDocumentAnnotationTypes, attributeSize);
            } else {
                splitter = new XmiSplitter(annotationsToStore, recursively, storeBaseDocument, docTableName,
                        baseDocumentAnnotationTypes);
            }
        }
        log.info(XMIDBWriter.class.getName() + " initialized.");
        log.info("Effective document table name: {}", effectiveDocTableName);
        log.info("Is base document stored: {}", storeBaseDocument);
        log.info("CAS XMI data will be GZIPed: {}", doGzip);
        log.info("Is the whole, unsplit XMI document stored: {}", storeAll);
        log.info("Annotations belonging to the base document: {}", baseDocumentAnnotationTypes);
        log.info("Annotation types to store in separate tables: {}", annotationsToStore);
        log.info("Store annotations recursively: {}", recursively);
        log.info("Update mode: {}", updateMode);
        log.info("Base document table schema: {}", schemaDocument);
        log.info("Annotation table schema (only required if annotations are stored separatly): {}", schemaAnnotation);

        metaTableManager = new MetaTableManager(dbc);
        annotationInserter = new XmiDataInserter(annotationsToStoreTableNames, docTableName, effectiveDocTableName, dbc,
                schemaDocument, schemaAnnotation, storeAll, storeBaseDocument, updateMode, componentDbName);
    }

    private void checkTableDefinition(String annotationTableName, String schemaAnnotation) throws ResourceInitializationException {
        try {
            dbc.checkTableDefinition(annotationTableName, schemaAnnotation);
        } catch (TableSchemaMismatchException e) {
            throw new ResourceInitializationException(e);
        }
    }

    private void checkParameters(UimaContext aContext) throws ResourceInitializationException {
        if (aContext.getConfigParameterValue(PARAM_COSTOSYS_CONFIG) == null)
            throw new ResourceInitializationException(new IllegalStateException(
                    "The database configuration file is null. You must provide the path to a valid configuration file."));
        if (aContext.getConfigParameterValue(PARAM_TABLE_DOCUMENT) == null)
            throw new ResourceInitializationException(new IllegalStateException(
                    "The document table is null. You must provide it to either store the entire xmi data, to store the base document "
                            + " or to update the next possible xmi id."));
        String[] annotations = (String[]) aContext.getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
        if ((Boolean) aContext.getConfigParameterValue(PARAM_STORE_ALL) == null && annotations == null
                && (Boolean) aContext.getConfigParameterValue(PARAM_STORE_BASE_DOCUMENT) == null) {
            throw new ResourceInitializationException(new IllegalStateException(
                    "The parameter to store the entire xmi data is not checked, but there are no annotations specified to"
                            + " store instead. You must provide the names of the selected annotations, if you do not want to "
                            + " write the entire CAS data."));
        } else if (aContext.getConfigParameterValue(PARAM_STORE_ALL) != null
                && (Boolean) aContext.getConfigParameterValue(PARAM_STORE_ALL) && annotations != null
                && annotations.length > 0) {
            throw new ResourceInitializationException(new IllegalStateException(
                    "The parameter to store the the entire xmi data is checked and there are annotations specified to store."
                            + " You can only either write the entire CAS data or select annotations, but not both!"));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org
     * .apache.uima.jcas.JCas)
     */
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        DocumentId docId = getDocumentId(aJCas);
        if (docId == null) return;

        int nextXmiId = determineNextXmiId(aJCas, docId);
        Map<String, Integer> baseDocumentSofaIdMap = getOriginalSofaIdMappings(aJCas, docId);
        // and now delete the XMI meta data
        Collection<XmiMetaData> xmiMetaData = JCasUtil.select(aJCas, XmiMetaData.class);
        if (xmiMetaData.size() > 1)
            throw new AnalysisEngineProcessException(new IllegalArgumentException(
                    "There are multiple XmiMetaData annotations in the cas for document " + docId + "."));
        xmiMetaData.forEach(XmiMetaData::removeFromIndexes);

        if (subsetTable == null) {
            Collection<DBProcessingMetaData> metaData = JCasUtil.select(aJCas, DBProcessingMetaData.class);
            if (!metaData.isEmpty()) {
                if (metaData.size() > 1)
                    throw new AnalysisEngineProcessException(new IllegalArgumentException(
                            "There is more than one type of DBProcessingMetaData in document " + docId));
                subsetTable = metaData.stream().findAny().get().getSubsetTable();
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos = new ByteArrayOutputStream();
            OutputStream os = baos;
            XmiCasSerializer.serialize(aJCas.getCas(), baos);
            os.close();
        } catch (SAXParseException e) {
            // we did have the issue that in PMID 23700993 there was an XMI-1.0
            // illegal character in the affiliation beginning with
            // "Department of Medical Informatics and Biostatistics, Iuliu
            // Haieganu University of Medicine and Phar"
            // Throwing an error does terminate the whole CPE which seems
            // unnecessary.
            log.error("Serialization error occurred, skipping this document: ", e);
            return;
        } catch (SAXException e) {
            e.printStackTrace();
            throw new AnalysisEngineProcessException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AnalysisEngineProcessException(e);
        }

        byte[] completeXmiData = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(completeXmiData);
        try {
            // Split the xmi data.
            XmiSplitterResult result = splitter.process(bais, aJCas, nextXmiId, baseDocumentSofaIdMap);
            Map<String, ByteArrayOutputStream> splitXmiData = result.xmiData;
            // adapt the map keys to table names (currently, the keys are the
            // Java type names)
            Map<String, ByteArrayOutputStream> convertedMap = new HashMap<>();
            for (Entry<String, ByteArrayOutputStream> e : splitXmiData.entrySet())
                convertedMap.put(annotationTableManager.convertAnnotationTypeToTableName(e.getKey(), storeAll),
                        e.getValue());
            splitXmiData = convertedMap;
            Integer newXmiId = result.maxXmiId;
            Map<String, String> nsAndXmiVersionMap = result.namespaces;
            Map<Integer, String> currentSofaXmiIdMap = result.currentSofaIdMap;
            metaTableManager.manageXMINamespaces(nsAndXmiVersionMap);

            if (currentSofaXmiIdMap.isEmpty())
                throw new IllegalStateException(
                        "The XmiSplitter returned an empty Sofa XMI ID map. This is a critical errors since it means " +
                                "that the splitter was not able to resolve the correct Sofa XMI IDs for the annotations " +
                                "that should be stored now.");
            log.trace("Updating max xmi id of document {}. New max xmi id: {}", docId, newXmiId);
            log.trace("Sofa ID map for this document: {}", currentSofaXmiIdMap);
            if (storeAll) {
                Object storedData = handleDataZipping(completeXmiData, schemaDocument);
                serializedCASes.get(effectiveDocTableName)
                        .add(new DocumentXmiData(docId, storedData, newXmiId, currentSofaXmiIdMap));// new
            } else {
                for (String tableName : serializedCASes.keySet()) {
                    boolean isDocumentTable = tableName.equals(effectiveDocTableName);
                    ByteArrayOutputStream dataBaos = splitXmiData.get(tableName);
                    if (null != dataBaos) {
                        byte[] dataBytes = dataBaos.toByteArray();
                        String tableSchemaName = isDocumentTable ? schemaDocument : schemaAnnotation;
                        // Get the second field of the appropriate table schema,
                        // since the convention is that the data
                        // goes to the second column currently.
                        Object storedData = handleDataZipping(dataBytes, tableSchemaName);
                        // tableName = tableName.replace(".", "_");
                        // tableName = dbc.getActiveDataPGSchema() + "."
                        // + tableName;
                        if (storeBaseDocument && isDocumentTable) {
                            serializedCASes.get(tableName)
                                    .add(new DocumentXmiData(docId, storedData, newXmiId, currentSofaXmiIdMap));// Object[]
                            // {
                            // docId,
                            // storedData,
                            // newXmiId
                            // });
                        } else {
                            serializedCASes.get(tableName).add(new XmiData(docId, storedData));// new
                            // Object[]
                            // {
                            // docId,
                            // storedData
                            // });
                            if (!storeBaseDocument)
                                annotationInserter.putXmiIdMapping(docId, newXmiId);

                        }
                    } else if (updateMode) {
                        // There was no data for the annotation table. Since we
                        // are updating this could mean we once had annotations
                        // but the new text version doesn't have them. We must
                        // delete the old annotations to avoid xmi:id clashes.
                        // Thus add here the document id for the table we have
                        // to clear the row from (one row per document).
                        tablesWithoutData.get(tableName).add(docId);
                    }
                }
                if (deleteObsolete) {
                    for (String obsoleteTable : annotationTableManager.getObsoleteAnnotationTableNames())
                        tablesWithoutData.get(obsoleteTable).add(docId);
                }
            }
            // as the very last thing, add this document to the processed list
            annotationInserter.addProcessedDocumentId(docId);
        } catch (IOException | XMISplitterException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private DocumentId getDocumentId(JCas aJCas) {
        DocumentId docId = null;
        try {
            DBProcessingMetaData dbProcessingMetaData = JCasUtil.selectSingle(aJCas, DBProcessingMetaData.class);
            docId = new DocumentId(dbProcessingMetaData);
        } catch (IllegalArgumentException e) {
            // it seems there is not DBProcessingMetaData we could get a complex primary key from. The document ID
            // will have to do.
            log.debug("Could not find the primary key in the DBProcessingMetaData due to exception: {}. Using the document ID as primary key.",
                    DBProcessingMetaData.class.getSimpleName());
        }
        if (docId == null) {
            AnnotationIndex<Annotation> headerIndex = aJCas.getAnnotationIndex(Header.type);
            FSIterator<Annotation> headerIt = headerIndex.iterator();
            if (!headerIt.hasNext()) {
                int min = Math.min(100, aJCas.getDocumentText().length());
                log.warn(
                        "Got document without a header and without DBProcessingMetaData; cannot obtain document ID." +
                                " This document will not be written into the database. Document text begins with: {}",
                        aJCas.getDocumentText().substring(0, min));
                ++headerlessDocuments;
                return null;
            }

            Header header = (Header) headerIt.next();
            docId = new DocumentId(header.getDocId());
        }
        return docId;
    }

    /**
     * Reads all {@link XmiMetaData} types from the CAS index, writes the
     * actual sofa xmi:id to sofaID mapping into a map, removes the annotations
     * from the index and returns the map.
     *
     * @param aJCas
     * @param docId
     * @return
     */
    private Map<String, Integer> getOriginalSofaIdMappings(JCas aJCas, DocumentId docId) {
        XmiMetaData xmiMetaData;
        try {
            xmiMetaData = JCasUtil.selectSingle(aJCas, XmiMetaData.class);
            if (xmiMetaData.getSofaIdMappings() == null)
                return Collections.emptyMap();
        } catch (IllegalArgumentException e) {
            // in case there is no XMI meta data
            return Collections.emptyMap();
        }
        // note that we change the mapping orientation; originally stored
        // was xmiID:sofaID[sofaName]; but for the XmiSplitter input we need
        // it the other way round.
        Map<String, Integer> map = Stream.of(xmiMetaData.getSofaIdMappings().toArray()).map(line -> line.split("="))
                .collect(Collectors.toMap(split -> split[1], split -> Integer.parseInt(split[0])));
        log.trace("Got Sofa XMI map from the CAS: {}", map);
        return map;
    }

    private int determineNextXmiId(JCas aJCas, DocumentId docId) throws AnalysisEngineProcessException {
        // Retrieve the max-xmi-id from the CAS, as this will have been
        // retrieved from the document table
        // via jules-cas-xmi-from-db-reader. In case the base document will be
        // stored for the first time
        // set it to 0, since it still has to be determined.
        int nextXmiId = 0;
        try {
            nextXmiId = JCasUtil.selectSingle(aJCas, XmiMetaData.class).getMaxXmiId();
        } catch (IllegalArgumentException e) {
            // If we store the base document and there is no current XmiMetaData
            // object, then we begin from scratch or want to overwrite the max
            // XMI ID on purpose. Don't throw an error then.
            if (!storeBaseDocument)
                throw new AnalysisEngineProcessException(new NullPointerException(
                        "Error: Could not find the max XMI ID in the CAS. Explanation: The option to store the base " +
                                "document (i.e. the document and possible same basic document meta " +
                                "data annotations) is set to false. Thus, it is assumed that the XMI DB Reader was used " +
                                "to read an existing base document and that only annotation data should be written now. In this " +
                                "case, the current maximum XMI ID for the respective document is required to be found " +
                                "in the CAS to keep this XMI ID unique for each annotation. This information is written " +
                                "into the CAS by the XMI DB Reader, if the " +
                                "respective configuration parameter is set to true. This seems not to be the case " +
                                "since the max XMI ID could not be found. Make sure that the reader adds the max XMI ID" +
                                "to the CAS and run the pipeline again."));
        }

        if (storeAll || storeBaseDocument || annotationsToStore.isEmpty()) {
            nextXmiId = 0;
            log.trace(
                    "Counting XMI IDs from 0 for document {} since the whole document is stored or the base document is stored or no additional annotations are stored.",
                    docId);
        } else {
            log.trace("Counting XMI IDs from {} for document {}.", nextXmiId, docId);
            if (nextXmiId == 0)
                log.warn(
                        "XMI IDs are counted from 0 for document {}. This is most probably a mistake since annotations should be stored but not the base document. In the base document are always some annotation elements with XMI IDs so those IDs will most probably already be taken and should not be assigned to new annotations.",
                        docId);
        }
        return nextXmiId;
    }

    /**
     * If <tt>doGzip</tt> is set to true, the <tt>dataBytes</tt> array will be
     * GZIPed. Otherwise, the data will be converted to a string so it can be
     * read directly from the database.
     *
     * @param dataBytes
     * @param tableSchemaName
     * @return
     * @throws IOException
     */
    protected Object handleDataZipping(byte[] dataBytes, String tableSchemaName) throws IOException {
        Object storedData = null;
        Map<String, String> field = dbc.getFieldConfiguration(tableSchemaName).getFields().get(1);
        String xmiFieldType = field.get(JulieXMLConstants.TYPE);
        if (doGzip) {
            if (!xmiFieldType.equalsIgnoreCase("bytea"))
                log.warn("The table schema \"" + tableSchemaName + "\" specifies the data type \"" + xmiFieldType
                        + "\" for the field \"" + field.get(JulieXMLConstants.NAME)
                        + "\" which is supposed to be filled with gzipped XMI data. However, binary data should go to a field of type bytea.");
            ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
            GZIPOutputStream gzos = new GZIPOutputStream(gzipBaos);
            gzos.write(dataBytes);
            gzos.close();
            storedData = gzipBaos.toByteArray();
        } else {
            if (!xmiFieldType.equalsIgnoreCase("text"))
                log.warn("The table schema \"" + tableSchemaName + "\" specifies the data type \"" + xmiFieldType
                        + "\" for the field \"" + field.get(JulieXMLConstants.NAME)
                        + "\" and the contents to be written should be some text. Please use the field type text for such contents.");
            if (xmiFieldType.equalsIgnoreCase("text"))
                storedData = new String(dataBytes);
        }
        return storedData;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#
     * batchProcessComplete()
     */
    @Override
    public void batchProcessComplete() throws AnalysisEngineProcessException {
        super.batchProcessComplete();
        log.debug("Running batchProcessComplete.");
        try {
            annotationInserter.sendXmiDataToDatabase(serializedCASes, tablesWithoutData, subsetTable);
            for (List<XmiData> tableData : serializedCASes.values())
                tableData.clear();
            for (List<DocumentId> docIds : tablesWithoutData.values())
                docIds.clear();
        } catch (XmiDataInsertionException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#
     * collectionProcessComplete()
     */
    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        log.debug("Running collectionProcessComplete.");
        try {
            annotationInserter.sendXmiDataToDatabase(serializedCASes, tablesWithoutData, subsetTable);
            for (List<XmiData> tableData : serializedCASes.values())
                tableData.clear();
            for (List<DocumentId> docIds : tablesWithoutData.values())
                docIds.clear();
        } catch (XmiDataInsertionException e) {
            throw new AnalysisEngineProcessException(e);
        }
        log.info("{} documents without a head occured overall. Those could not be written into the database.",
                headerlessDocuments);
    }

}