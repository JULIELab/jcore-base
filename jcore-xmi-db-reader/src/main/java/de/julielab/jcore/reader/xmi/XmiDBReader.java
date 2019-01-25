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
import de.julielab.jcore.reader.db.SubsetReaderConstants;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.CoStoSysConnection;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author faessler
 */
@ResourceMetaData(name = "JCoRe XMI Database Reader", vendor = "JULIE Lab Jena, Germany", description = "A database reader" +
        "that expects serialized UIMA CAS objects in XMI format as input. The reader has the capability to read " +
        "segmented annotation graphs that have been stored by the jcore-xmi-db-writer. This component is part of the " +
        "Jena Document Information System, JeDIS.")
public class XmiDBReader extends DBReader implements Initializable {

    public static final String PARAM_STORE_XMI_ID = Initializer.PARAM_STORE_XMI_ID;
    public static final String PARAM_LOG_FINAL_XMI = Initializer.PARAM_LOG_FINAL_XMI;
    public static final String PARAM_READS_BASE_DOCUMENT = Initializer.PARAM_READS_BASE_DOCUMENT;
    public static final String PARAM_INCREASED_ATTRIBUTE_SIZE = Initializer.PARAM_INCREASED_ATTRIBUTE_SIZE;
    public static final String PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE = Initializer.PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE;

    private final static Logger log = LoggerFactory.getLogger(XmiDBReader.class);
    /**
     * This is basically a copy of the same parameter in {@link de.julielab.jcore.reader.db.DBSubsetReader}. It is
     * not actually used to write any values into it and it shouldn't be. It is here to generate a different
     * description for the specific use of the parameter in the context of the XMI reader.
     */
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES, mandatory = false, description = "An array of qualified UIMA type names. The provided names will be converted to database table names in an equivalent manner as the XMIDBWriter does when storing the annotations. Thus, the default assumed Postgres schema for the annotation tables is the active data table as configured in the CoStoSys configuration file. This can be overwritten by appending '<schema>:' to a table name. The given type names will be converted to valid Postgres table names by replacing dots with underscores. From the resolved tables, annotation modules in segmented XMI format are read where an annotation module contains all annotation instances of a specific type in a specific document. All annotation modules read this way are merged with the base document, resulting in valid XMI data which is then deserialized into the CAS.")
    protected String[] additionalTableNames;
    private Boolean doGzip;
    @ConfigurationParameter(name = PARAM_READS_BASE_DOCUMENT, description = "Indicates if this reader reads segmented " +
            "annotation data. If set to false, the XMI data is expected to represent complete annotated documents. " +
            "If it is set to true, a segmented annotation graph is expected and the table given with the 'Table' parameter " +
            "will contain the document text together with some basic annotations. What exactly is stored in which manner " +
            "is determined by the jcore-xmi-db-consumer used to write the data into the database.")
    private Boolean readsBaseDocument;
    @ConfigurationParameter(name = PARAM_STORE_XMI_ID, mandatory = false, description = "This parameter is required " +
            "to be set to true, if this reader is contained in a pipeline that also contains a jcore-xmi-db-writer and" +
            "the writer will segment the CAS annotation graph and store only parts of it. Then, it is important to " +
            "keep track of the free XMI element IDs that may be assigned to new annotation elements to avoid " +
            "ID clashes when assembling an XMI document from separately stored annotation graph segments.")
    private Boolean storeMaxXmiId;
    @ConfigurationParameter(name = PARAM_INCREASED_ATTRIBUTE_SIZE, mandatory = false, description = "Maxmimum XML attribute " +
            "size in bytes. Since the CAS " +
            "document text is stored as an XMI attribute, it might happen for large documents that there is an error " +
            "because the maximum attribute size is exceeded. This parameter allows to specify the maxmimum " +
            " attribute size in order to avoid such errors. Should only be set if required.")
    private int maxXmlAttributeSize;
    @ConfigurationParameter(name = PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE, mandatory = false, description = "Initial XML " +
            "parser buffer size in bytes. For large documents, " +
            "it can happen that XMI parsing is extremely slow. By employing monitoring tools like the jconsole or " +
            "(j)visualvm, the hot spots of work can be identified. If one of those is the XML attribute buffer " +
            "resizing, this parameter should be set to a size that makes buffer resizing unnecessary.")
    private int xercesAttributeBufferSize;
    private Initializer initializer;
    private CasPopulator casPopulator;


    /*
     * (non-Javadoc)
     *
     * @see de.julielab.jules.reader.DBReader#initialize()
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        adaptReaderConfigurationForXmiData();
        super.initialize(context);
        dbc.reserveConnection();
        initializer = new Initializer(this, dbc, additionalTableNames, joinTables);
        initializer.initialize(context);
        casPopulator = new CasPopulator(dataTable, initializer, readDataTable, tableName);
        dbc.releaseConnections();
    }

    /**
     * Must be called before super.initialize(context). Sets up table schemas for XMI data so the user doesn't have
     * to do it.
     *
     * @throws ResourceInitializationException
     */
    private void adaptReaderConfigurationForXmiData() throws ResourceInitializationException {
        costosysConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);
        try {
            dbc = new DataBaseConnector(costosysConfig);
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
            List<Map<String, String>> primaryKeyFields = dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList());
            if ((Boolean) getConfigParameterValue(PARAM_READS_BASE_DOCUMENT)) {

                String table = (String) getConfigParameterValue(PARAM_TABLE);
                determineDataInGzipFormat(table);

                FieldConfig xmiDocumentTableSchema = dbc.addXmiTextFieldConfiguration(primaryKeyFields, doGzip);
                dbc.setActiveTableSchema(xmiDocumentTableSchema.getName());
                String[] additionalTables = (String[]) getConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLES);
                if (additionalTables != null && additionalTables.length > 0) {
                    FieldConfig xmiAnnotationTableSchema = dbc.addXmiAnnotationFieldConfiguration(primaryKeyFields, doGzip);
                    setConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS, new String[]{xmiAnnotationTableSchema.getName()});
                }
                XmiReaderUtils.checkXmiTableSchema(dbc, tableName, xmiDocumentTableSchema, getMetaData().getName());
            } else {
                // Complete XMI reading mode
                String table = (String) getConfigParameterValue(PARAM_TABLE);
                determineDataInGzipFormat(table);
                FieldConfig xmiDocumentFieldConfiguration = dbc.addXmiDocumentFieldConfiguration(primaryKeyFields, doGzip);
                dbc.setActiveTableSchema(xmiDocumentFieldConfiguration.getName());
            }
        }
    }

    private void determineDataInGzipFormat(String table) throws ResourceInitializationException {
        doGzip = true;
        dataTable = dbc.getNextOrThisDataTable(table);
        log.debug("Fetching a single row from data table {} in order to determine whether data is in GZIP format", dataTable);
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()){
            ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT xmi FROM %s LIMIT 1", dataTable));
            while (rs.next()) {
                byte[] xmiData = rs.getBytes("xmi");
                try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(xmiData))) {
                    gzis.read();
                } catch (IOException e) {
                    log.debug("Attempt to read XMI data in GZIP format failed. Assuming non-gzipped XMI data. Expected exception:", e);
                    doGzip = false;
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist"))
                log.error("An exception occurred when trying to read the xmi column of the data table \"{}\". It seems the table does not contain XMI data and this is invalid to use with this reader.", dataTable);
            throw new ResourceInitializationException(e);
        }
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
        if (data != null)
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

    /**
     * The additional table names have been initialized by {@link DBReader#initialize(UimaContext)}
     *
     * @return The additional table to be joined to the base document table.
     */
    @Override
    public String[] getAdditionalTableNames() {
        return super.additionalTableNames;
    }

    /**
     * The tables have been initialized by {@link DBReader#initialize(UimaContext)}
     *
     * @return The tables to read.
     */
    @Override
    public String[] getTables() {
        return tables;
    }
}
