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
import de.julielab.jcore.reader.db.TableReaderConstants;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.XmiMetaData;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.XmiBuilder;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.DBCIterator;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    public static final String PARAM_DO_GZIP = "DoGzip";

    private final static Logger log = LoggerFactory.getLogger(XmiDBReader.class);
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
        if ((Boolean) getConfigParameterValue(PARAM_READS_BASE_DOCUMENT)) {
            costosysConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);
            try {
                dbc = new DataBaseConnector(costosysConfig);
            } catch (FileNotFoundException e) {
                throw new ResourceInitializationException(e);
            }

            String table = (String) getConfigParameterValue(PARAM_TABLE);
            String dataTable = null;
            try {
                doGzip = true;
                dataTable = dbc.getNextDataTable(table);
                log.trace("Fetching a single row from data table {} in order to determine whether data is in GZIP format", dataTable);
                try (Connection conn = dbc.getConn()) {
                    ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT xmi FROM %s LIMIT 1", dataTable));
                    while (rs.next()) {
                        byte[] xmiData = rs.getBytes("xmi");
                        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(xmiData))) {
                            gzis.read();
                        } catch (IOException e) {
                            log.trace("Attempt to read XMI data in GZIP format failed. Assuming non-gzipped XMI data. Expected exception:", e);
                            doGzip = false;
                        }
                    }
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist"))
                    log.error("An exception occurred when trying to read the xmi column of the data table \"{}\". It seems the table does not contain XMI data and this is invalid to use with this reader.", dataTable);
                throw new ResourceInitializationException(e);
            }

            FieldConfig xmiDocumentTableSchema = dbc.addXmiTextFieldConfiguration(dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList()), doGzip);
            dbc.setActiveTableSchema(xmiDocumentTableSchema.getName());
            String[] additionalTables = (String[]) getConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLES);
            if (additionalTables != null && additionalTables.length > 0) {
                FieldConfig xmiAnnotationTableSchema = dbc.addXmiAnnotationFieldConfiguration(dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList()), doGzip);
                setConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS, new String[]{xmiAnnotationTableSchema.getName()});
            }
            XmiReaderUtils.checkXmiTableSchema(dbc, tableName, xmiDocumentTableSchema, getMetaData().getName());
        }
        super.initialize(context);
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

    /**
     * The additional table names have been initialized by {@link DBReader#initialize(UimaContext)}
     *
     * @return The additional table to be joined to the base document table.
     */
    @Override
    public String[] getAdditionalTableNames() {
        return additionalTableNames;
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
