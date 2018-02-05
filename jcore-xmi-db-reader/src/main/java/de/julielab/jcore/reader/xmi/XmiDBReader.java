/**
 * BinaryCASFromDBReader.java
 *
 * Copyright (c) 2012, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 *
 * Author: faessler
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 12.12.2012
 **/

/**
 * 
 */
package de.julielab.jcore.reader.xmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.XmiMetaData;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.XmiBuilder;
import de.julielab.xml.XmiSplitConstants;
import de.julielab.xml.XmiSplitUtilities;

/**
 * @author faessler
 * 
 */
public class XmiDBReader extends DBReader {

	private final static Logger log = LoggerFactory.getLogger(XmiDBReader.class);

	public static final String PARAM_DO_GUNZIP = "PerformGUNZIP";
	public static final String PARAM_STORE_XMI_ID = "StoreMaxXmiId";
	public static final String PARAM_LOG_FINAL_XMI = "LogFinalXmi";
	public static final String PARAM_READS_BASE_DOCUMENT = "ReadsBaseDocument";
	public static final String PARAM_INCREASED_ATTRIBUTE_SIZE = "IncreasedAttributeSize";
	public static final String PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE = "XercesAttributeBufferSize";

	private Boolean doGzip;
	private Boolean storeMaxXmiId;
	private int maxXmlAttributeSize;
	private int xercesAttributeBufferSize;

	private XmiBuilder builder;
	private boolean initializationComplete;

	private Boolean logFinalXmi;
	/**
	 * Determines whether we read the namespace table and apply its contents to
	 * the read XMI or not. For full-document storage, the namespaces are
	 * included in the document XMI and adding them again would make the XMI
	 * invalid.
	 */
	private Boolean readsBaseDocument;

	private int numDataRetrievedDataFields;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.julielab.jules.reader.DBReader#initialize()
	 */
	@Override
	public void initialize() throws ResourceInitializationException {
		super.initialize();
		// If the field is defined with gzip=true in the field configuration,
		// the unzipping happens automatically.
		boolean fieldIsSetToGzip = Boolean
				.parseBoolean(dbc.getFieldConfiguration().getFields().get(1).get(JulieXMLConstants.GZIP));
		doGzip = getConfigParameterValue(PARAM_DO_GUNZIP) == null ? false
				: ((Boolean) getConfigParameterValue(PARAM_DO_GUNZIP)) && !fieldIsSetToGzip;
		storeMaxXmiId = (Boolean) (getConfigParameterValue(PARAM_STORE_XMI_ID) == null ? false
				: getConfigParameterValue(PARAM_STORE_XMI_ID));
		logFinalXmi = (Boolean) (getConfigParameterValue(PARAM_LOG_FINAL_XMI) == null ? false
				: getConfigParameterValue(PARAM_LOG_FINAL_XMI));
		readsBaseDocument = (Boolean) (getConfigParameterValue(PARAM_READS_BASE_DOCUMENT) == null ? false
				: getConfigParameterValue(PARAM_READS_BASE_DOCUMENT));
		Optional.ofNullable((Integer) getConfigParameterValue(PARAM_INCREASED_ATTRIBUTE_SIZE))
				.ifPresent(v -> maxXmlAttributeSize = v);
		Optional.ofNullable((Integer) getConfigParameterValue(PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE))
				.ifPresent(v -> xercesAttributeBufferSize = v);
		initializationComplete = true;
		if (joinTables)
			for (String annotation : additionalTableNames) {
				if (!annotation.contains(".")) {
					initializationComplete = false;
					log.debug(annotation
							+ " is not the fully qualified java name. Will retrieve the fully qualified java name"
							+ " from the types namespace and use this as table name.");
				}
			}
		// If we don't join tables, we assume that the read documents are
		// complete and valid. Thus, ignore the namespace table.
		Map<String, String> nsAndXmiVersion = null;
		if (joinTables || readsBaseDocument)
			nsAndXmiVersion = getNamespaceMap();
		// if the maxXmlAttributeSize is 0, the default is used
		builder = new XmiBuilder(nsAndXmiVersion, additionalTableNames, maxXmlAttributeSize);

		numDataRetrievedDataFields = dbc.getFieldConfiguration().getColumnsToRetrieve().length;

		log.info("{}: {}", PARAM_DO_GUNZIP,
				doGzip + " (field configuration for field "
						+ dbc.getFieldConfiguration().getFields().get(1).get(JulieXMLConstants.NAME) + " is set to "
						+ fieldIsSetToGzip + "; if 'true', then un-gzip is performed by the XML tools)");
		log.info("{}: {}", PARAM_STORE_XMI_ID, storeMaxXmiId);
		log.info("{}: {} ", PARAM_LOG_FINAL_XMI, logFinalXmi);
		log.info("{}: {}", PARAM_READS_BASE_DOCUMENT, readsBaseDocument);
		log.info("{}: {}", PARAM_INCREASED_ATTRIBUTE_SIZE, maxXmlAttributeSize);
		log.info("{}: {}", PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE, xercesAttributeBufferSize);
		log.info("Data columns set for retrieval: {}",
				Arrays.toString(dbc.getFieldConfiguration().getColumnsToRetrieve()));

	}

	private Map<String, String> getNamespaceMap() {
		Map<String, String> map = null;
		if (dbc.tableExists(dbc.getActiveDataPGSchema() + "." + XmiSplitConstants.XMI_NS_TABLE)) {
			try (Connection conn = dbc.getConn()) {
				map = new HashMap<>();
				conn.setAutoCommit(true);
				Statement stmt = conn.createStatement();
				String sql = String.format("SELECT %s,%s FROM %s", XmiSplitConstants.PREFIX, XmiSplitConstants.NS_URI,
						dbc.getActiveDataPGSchema() + "." + XmiSplitConstants.XMI_NS_TABLE);
				ResultSet rs = stmt.executeQuery(String.format(sql));
				while (rs.next())
					map.put(rs.getString(1), rs.getString(2));
			} catch (SQLException e) {
				e.printStackTrace();
				SQLException ne = e.getNextException();
				if (null != ne)
					ne.printStackTrace();
			}
		} else {
			log.warn(
					"Table \"{}\" was not found it is assumed that the table from which is read contains complete XMI documents.",
					dbc.getActiveDataPGSchema() + "." + XmiSplitConstants.XMI_NS_TABLE);
		}
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas
	 * .CAS)
	 */
	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		log.trace("Reading next document.");
		// If annotations have not been given as fully qualified java names
		// (i.e. e.g. de.julielab.jules.types.Token)
		// the default types namespace will be added and it will be checked if
		// the type system contains the thus
		// constructed type.
		if (!initializationComplete) {
			log.debug(
					"Initializing annotation table table names from type system, if any additional table names are given.");
			TypeSystem typeSystem = aCAS.getTypeSystem();
			for (int i = 0; i < additionalTableNames.length; i++) {
				if (!additionalTableNames[i].contains(".")) {
					String typeName = XmiSplitUtilities.TYPES_NAMESPACE + additionalTableNames[i];
					if (typeSystem.getType(typeName) != null) {
						// A table cannot be created if the name contains dots.
						// All annotation tables created
						// via jules-cas-xmi-to-db-consumer will have dots
						// replaced by underline.
						String tableName = typeName.replace(".", "_");
						tableName = dbc.getActiveDataPGSchema() + "." + tableName;
						tables[i + 1] = tableName;
					} else {
						throw new IOException(new IllegalStateException(
								"Could not retrieve the fully qualified java name for type " + additionalTableNames[i]
										+ " from the types namespace in order to use it as table name."
										+ " Please specify the fully qualified java name for this type"));
					}
				}
			}
			initializationComplete = true;
		}

		LinkedHashMap<String, InputStream> xmiData = new LinkedHashMap<String, InputStream>();
		log.trace("Retrieving document data from the database.");
		byte[][] data = getNextArtefactData(aCAS);
		log.trace("Got document data with {} fields.", null != data ? data.length : 0);
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
									+ " to set the field \"pmid\" in the annotation schema to false, since this"
									+ " should be retrieved only once from the document table."));
				}
				// Construct the input for the XmiBuilder.
				if (doGzip)
					xmiData.put(dataTable, new GZIPInputStream(documentIS));
				else
					xmiData.put(dataTable, documentIS);
				if (joinTables) {
					for (int i = numDataRetrievedDataFields; i < data.length; i++) {
						documentIS = data[i] != null ? new ByteArrayInputStream(data[i]) : null;
						dataSize += data[i] != null ? data[i].length : 0;
						if (null != documentIS) {
							if (doGzip)
								xmiData.put(additionalTableNames[i - numDataRetrievedDataFields],
										new GZIPInputStream(documentIS));
							else
								xmiData.put(additionalTableNames[i - numDataRetrievedDataFields], documentIS);
						}
					}
				}

				log.trace("Received {} bytes of XMI data, taking base document and annotation XMI together", dataSize);
				if (doGzip) {
					// The effective data size is of course much higher when we
					// deal with gzip input data. It is not possible to exactly
					// predict the real size. We will assume a 1:4 compression
					// ratio
					dataSize *= 4;
					log.trace("Input data is in GZIP format, estimating effective data size to be {}", dataSize);
					if (dataSize > Integer.MAX_VALUE) {
						// 64bit JVM can address this much most of the time
						dataSize = Integer.MAX_VALUE - 2;
						log.trace("Estimated data size exceeded maximum array size, reducing size to {}", dataSize);
					}
				}
				builder.setInputSize((int) dataSize);

				log.trace(
						"Building complete XMI data from separate XMI base document and annotation data retrieved from the database.");
				ByteArrayOutputStream baos;
				try {
					baos = builder.buildXmi(xmiData, dataTable, aCAS.getTypeSystem());
				} catch (OutOfMemoryError e) {
					log.error("Document with ID {} could not be built from XMI: {}", new String(data[0]), e);
					log.error("Full error:", e);
					setPrimaryKeyAsDocId(data, true, aCAS);
					return;
				}
				byte[] xmiByteData = baos.toByteArray();
				if (logFinalXmi)
					log.info(new String(xmiByteData));
				documentIS = new ByteArrayInputStream(xmiByteData);
				try {
					log.trace("Deserializing XMI data into the CAS.");

					JCoReTools.deserializeXmi(aCAS, documentIS, xercesAttributeBufferSize);
				} catch (SAXException e) {
					log.error("SAXException while deserializing CAS XMI data.", e);
					throw new CollectionException(e);
				} catch (OutOfMemoryError e) {
					log.error("Document with ID {} caused an OutOfMemoryError; trying to skip and read the next document instead. The error was: ", docId, e);
					aCAS.reset();
					getNext(aCAS);
				}
			} else {
				// Don't join tables, assume a complete XMI document.
				try {
					if (doGzip)
						XmiCasDeserializer.deserialize(new GZIPInputStream(documentIS), aCAS);
					else
						XmiCasDeserializer.deserialize(documentIS, aCAS);
				} catch (SAXException e) {
					e.printStackTrace();
				}
			}
			log.trace("Setting max XMI ID to the CAS.");
			storeMaxXmiIdAndSofaMappings(aCAS, data);
			setDBProcessingMetaData(data, aCAS);
		} catch (Exception e) {
			// in case of an exception we at least would like to know which
			// document threw it.
			String pkString = setPrimaryKeyAsDocId(data, true, aCAS);
			log.error("Got exception while reading document " + pkString, e);
			throw new CollectionException(e);
		}
	}

	private void storeMaxXmiIdAndSofaMappings(CAS aCAS, byte[][] data) throws CollectionException, CASException {
		if (storeMaxXmiId && data.length > 2) {
			String docId = JCoReTools.getDocId(aCAS.getJCas());
			byte[] maxXmiIdBytes = data[2];
			int xmiId = Integer.parseInt(new String(maxXmiIdBytes));
			String mappingString = null;
			if (data.length > 3)
				mappingString = new String(data[3]);
			// First, remove all XmiMetaData annotations that might be
			// here for some reason.
			for (XmiMetaData xmiMetaData : JCasUtil.select(aCAS.getJCas(), XmiMetaData.class))
				xmiMetaData.removeFromIndexes();
			// Now add the current max xmi ID to the CAS.
			XmiMetaData xmiMetaData = new XmiMetaData(aCAS.getJCas());
			xmiMetaData.setMaxXmiId(xmiId);
			log.trace("Retrieved max xmi ID {} for document {}.", xmiMetaData.getMaxXmiId(), docId);
			// Now add the current sofa Id mappings to the CAS.
			String[] mappings = mappingString != null ? mappingString.split("\\|") : null;
			StringArray mappingsArray = null;
			if (mappings != null) {
				mappingsArray = new StringArray(aCAS.getJCas(), mappings.length);
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

	/**
	 * Sets the primary key of this document to the document's header if not
	 * already existent. If there is no header, also the header is created and
	 * primary key set. The document ID is left blank.
	 * 
	 * @param data
	 * @param cas
	 * @throws CollectionException
	 */
	private String setPrimaryKeyAsDocId(byte[][] data, boolean setPKAsDocId, CAS cas) throws CollectionException {
		String pkString = null;
		try {
			Header header = null;
			FSIterator<Annotation> itHeader = cas.getJCas().getAnnotationIndex(Header.type).iterator();
			if (itHeader.hasNext())
				header = (Header) itHeader.next();
			if (null == header) {
				log.trace("No header found, setting a new one.");
				header = new Header(cas.getJCas());
				header.addToIndexes();
			}
			if (setPKAsDocId) {
				pkString = getPkStringFromData(data);
				header.setDocId(pkString);
			}
			return pkString;
		} catch (CASException e) {
			throw new CollectionException(e);
		}
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

	@Override
	protected String getReaderComponentName() {
		return getClass().getSimpleName();
	}
}
