package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.XmiMetaData;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xml.XmiBuilder;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

public class CasPopulator {
    private final static Logger log = LoggerFactory.getLogger(CasPopulator.class);
    private final DataBaseConnector dbc;
    private final boolean readsBaseDocument;
    private final int numAdditionalTables;
    private final int numDataRetrievedDataFields;
    private final String dataTable;
    private final String[] additionalTableNames;
    private final XmiBuilder builder;
    private final Boolean logFinalXmi;
    private final int xercesAttributeBufferSize;
    private final Boolean storeMaxXmiId;
    private final Boolean readsDataTable;
    private final String tableName;
    private boolean joinTables;

    /**
     * Takes document and annotation data from a XMI reader or XMI multiplier. Assembles a complete XMI document from
     * these data and populates a CAS with them. This class employs the XmiBuilder to assemble the XMI document.
     *
     * @param dataTable The table that is read for document data.
     * @param initializer The {@link Initializer} instance used to initialize the component that creates this class.
     * @param readDataTable Whether the data table is read directly in contrast of reading from a subset table.
     * @param tableName The name of table that is primarily read. May be a data table or a subset table.
     */
    public CasPopulator(String dataTable, Initializer initializer, Boolean readDataTable, String tableName) {
        this.dbc = initializer.getDataBaseConnector();
        this.readsDataTable = readDataTable;
        this.tableName = tableName;
        this.readsBaseDocument = initializer.getReadsBaseDocument();
        this.joinTables = initializer.isJoinTables();
        this.numAdditionalTables = initializer.getNumAdditionalTables();
        this.numDataRetrievedDataFields = initializer.getNumDataRetrievedDataFields();
        this.dataTable = dataTable;
        this.additionalTableNames = initializer.getAdditionalTableNames();
        this.builder = initializer.getXmiBuilder();
        this.logFinalXmi = initializer.getLogFinalXmi();
        this.xercesAttributeBufferSize = initializer.getXercesAttributeBufferSize();
        this.storeMaxXmiId = initializer.getStoreMaxXmiId();
    }

    /**
     * Retrieves document text and annotation XMI from <code>data</code>, assembles it into one single XMI document,
     * if necessary, and deserializes the XMI into the <code>jCas</code>.
     * @param data The XMI data that was read from one or more database tables.
     * @param jCas The CAS to populate.
     * @throws CasPopulationException If deserialization fails.
     */
    public void populateCas(byte[][] data, JCas jCas) throws CasPopulationException {

        String docId = getPkStringFromData(data);
        log.debug("Reading document with ID {} as delivered from database.", docId);
        byte[] documentXmi = data[1];
        // In this variable we record the total size of the retrieved data. We
        // use this information for the XMIBuilder to avoid resizing buffers and
        // even OutOfMemory errors
        // We also add a few bytes just to be sure (headers or other stuff;
        // shouldn't be that large but 100 bytes don't make much difference for
        // the data sizes we work with here).
        long dataSize = documentXmi.length + 100;
        LinkedHashMap<String, InputStream> xmiData = new LinkedHashMap<>();
        try {
            ByteArrayInputStream documentIS = new ByteArrayInputStream(documentXmi);
            // joinTables is true if we have additional tables to join;
            // otherwise we
            // expect a complete XMI document, just parse it and be done.
            if (joinTables || readsBaseDocument) {
                // data will contain pmid, document-xmi, max-xmi-id,
                // sofa_id_mapping
                // and the
                // additional
                // annotation-xmis
                // (in this order).
                // UNLESS we read from a table without the max-xmi-id field
                // which
                // would
                // correspond to a table with complete XMIs in contrast to split
                // XMI
                // tables. Thus only check when joinTables is TRUE.
                if (data.length != numAdditionalTables + numDataRetrievedDataFields) {
                    throw new CollectionException(new IllegalStateException(
                            "The number of retrieved fields does not match the expected number (expected: "
                                    + (numAdditionalTables + 4) + ", actual: " + data.length + "). Make sure"
                                    + " to set the primary key fields in the annotation schema to false, since this"
                                    + " should be retrieved only once from the document table."));
                }
                // Construct the input for the XmiBuilder.
                xmiData.put(dataTable, documentIS);
                if (joinTables) {
                    for (int i = numDataRetrievedDataFields; i < data.length; i++) {
                        documentIS = data[i] != null ? new ByteArrayInputStream(data[i]) : null;
                        dataSize += data[i] != null ? data[i].length : 0;
                        if (null != documentIS) {
                            xmiData.put(additionalTableNames[i - numDataRetrievedDataFields], documentIS);
                        }
                    }
                }

                log.trace("Received {} bytes of XMI data, taking base document and annotation XMI together", dataSize);
                builder.setInputSize((int) dataSize);

                log.trace(
                        "Building complete XMI data from separate XMI base document and annotation data retrieved from the database.");
                ByteArrayOutputStream baos;
                try {
                    baos = builder.buildXmi(xmiData, dataTable, jCas.getTypeSystem());
                } catch (OutOfMemoryError e) {
                    log.error("Document with ID {} could not be built from XMI: {}", new String(data[0]), e);
                    log.error("Full error:", e);
                    setPrimaryKeyAsDocId(data, true, jCas);
                    return;
                }
                byte[] xmiByteData = baos.toByteArray();
                if (logFinalXmi)
                    log.info(new String(xmiByteData, StandardCharsets.UTF_8));
                documentIS = new ByteArrayInputStream(xmiByteData);
                try {
                    log.trace("Deserializing XMI data into the CAS.");

                    JCoReTools.deserializeXmi(jCas.getCas(), documentIS, xercesAttributeBufferSize);
                } catch (SAXException e) {
                    String docData = new String(xmiByteData, StandardCharsets.UTF_8);
                    if (!docData.contains("xmi:XMI xmlns:xmi=\"http://www.omg.org/XMI\""))
                        throw new CollectionException(new IllegalArgumentException("The document that has been received from the database does not " +
                                "appear to contain XMI data. The beginning of the document data is: " +
                                StringUtils.abbreviate(docData, 200), e));
                    log.error("SAXException while deserializing CAS XMI data from a segmented and re-assemblied XMI " +
                            "document. Beginning of data was: {}", StringUtils.abbreviate(docData, 200));
                    throw new CollectionException(e);
                }
            } else {
                // Don't join tables, assume a complete XMI document.
                try {
                    XmiCasDeserializer.deserialize(documentIS, jCas.getCas());
                } catch (SAXException e) {
                    String docData = new String(documentXmi, StandardCharsets.UTF_8);
                    if (!docData.contains("xmi:XMI xmlns:xmi=\"http://www.omg.org/XMI\""))
                        throw new CollectionException(new IllegalArgumentException("The document that has been received from the database does not " +
                                "appear to contain XMI data. The beginning of the document data is: " +
                                StringUtils.abbreviate(docData, 200)));
                    log.error("SAXException while deserializing CAS XMI data. Beginning of data was: {}", StringUtils.abbreviate(docData, 200));
                    throw new CollectionException(e);
                }
            }
            log.trace("Setting max XMI ID to the CAS.");
            storeMaxXmiIdAndSofaMappings(jCas, data);
            DBReader.setDBProcessingMetaData(dbc, readsDataTable, tableName, data, jCas);
        } catch (Exception e) {
            // in case of an exception we at least would like to know which
            // document threw it.
            String pkString = setPrimaryKeyAsDocId(data, true, jCas);
            log.error("Got exception while reading document " + pkString, e);
            throw new CasPopulationException(e);
        }
    }

    /**
     * Sets the primary key of this document to the document's header if not
     * already existent. If there is no header, also the header is created and
     * primary key set. The document ID is left blank.
     *
     * @param data
     * @param cas
     * @throws CollectionException
     */
    private String setPrimaryKeyAsDocId(byte[][] data, boolean setPKAsDocId, JCas cas) {
        String pkString = null;
        Header header = null;
        FSIterator<Annotation> itHeader = cas.getAnnotationIndex(Header.type).iterator();
        if (itHeader.hasNext())
            header = (Header) itHeader.next();
        if (null == header) {
            log.trace("No header found, setting a new one.");
            header = new Header(cas);
            header.addToIndexes();
        }
        if (setPKAsDocId) {
            pkString = getPkStringFromData(data);
            header.setDocId(pkString);
        }
        return pkString;
    }

    private String getPkStringFromData(byte[][] data) {
        List<Integer> pkIndices = dbc.getPrimaryKeyIndices();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pkIndices.size(); ++i) {
            Integer index = pkIndices.get(i);
            byte[] pkElementValue = data[index];
            String elementString = new String(pkElementValue);
            sb.append(elementString);
            if (i < pkIndices.size() - 1)
                sb.append("-");
        }
        return sb.toString();
    }

    private void storeMaxXmiIdAndSofaMappings(JCas aCAS, byte[][] data) throws CASException {
        if (storeMaxXmiId && data.length > 2) {
            String docId = JCoReTools.getDocId(aCAS);
            byte[] maxXmiIdBytes = data[2];
            int xmiId = Integer.parseInt(new String(maxXmiIdBytes));
            String mappingString = null;
            if (data.length > 3)
                mappingString = new String(data[3]);
            // First, remove all XmiMetaData annotations that might be
            // here for some reason.
            for (XmiMetaData xmiMetaData : JCasUtil.select(aCAS, XmiMetaData.class))
                xmiMetaData.removeFromIndexes();
            // Now add the current max xmi ID to the CAS.
            XmiMetaData xmiMetaData = new XmiMetaData(aCAS);
            xmiMetaData.setMaxXmiId(xmiId);
            log.trace("Retrieved max xmi ID {} for document {}.", xmiMetaData.getMaxXmiId(), docId);
            // Now add the current sofa Id mappings to the CAS.
            String[] mappings = mappingString != null ? mappingString.split("\\|") : null;
            StringArray mappingsArray = null;
            if (mappings != null) {
                mappingsArray = new StringArray(aCAS, mappings.length);
                for (int i = 0; i < mappings.length; i++) {
                    String mapping = mappings[i];
                    mappingsArray.set(i, mapping);
                    log.trace("Retrieved sofa_id_mapping {} for document {}.", mappingsArray.get(i), docId);
                }
            }
            if (mappingsArray != null)
                xmiMetaData.setSofaIdMappings(mappingsArray);
            xmiMetaData.addToIndexes();
        }
    }
}
