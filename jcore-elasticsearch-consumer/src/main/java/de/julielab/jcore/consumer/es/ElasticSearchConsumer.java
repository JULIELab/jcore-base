package de.julielab.jcore.consumer.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.utility.JCoReTools;

public class ElasticSearchConsumer extends AbstractCasToJsonConsumer {

	final Logger log = LoggerFactory.getLogger(ElasticSearchConsumer.class);

	public static final String PARAM_URLS = "urls";
	public static final String PARAM_INDEX_NAME = "indexName";
	/**
	 * Between ES 6 and 7, the type is a constant value for each index, i.e. there
	 * only is one type. From ES 7, types will be removed entirely.
	 */
	public static final String PARAM_TYPE = "type";

	@ConfigurationParameter(name = PARAM_URLS, mandatory = true, description = "A list of URLs pointing to different nodes of the ElasticSearch cluster, e.g. http://localhost:9300/. Documents will be sent bulk-wise to the nodes in a round-robin fashion.")
	private String[] urls;
	@ConfigurationParameter(name = PARAM_INDEX_NAME, mandatory = true, description = "The ElasticSearch index name to send the created documents to.")
	private String indexName;
	@ConfigurationParameter(name = PARAM_TYPE, mandatory = true, description = "The index type the generated documents should have. The types are removed from ElasticSearch with version 7 so this parameter is set to have the same value for all documents.")
	private String type;

	private List<String> bulkCommand;
	private HttpPost[] indexPosts;

	private int urlIndex = 0;

	private HttpClient httpclient;

	private int docNum = 0;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		urls = (String[]) getContext().getConfigParameterValue(PARAM_URLS);
		indexName = (String) getContext().getConfigParameterValue(PARAM_INDEX_NAME);
		type = (String) getContext().getConfigParameterValue(PARAM_TYPE);
		bulkCommand = new ArrayList<String>(4000);

		httpclient = HttpClientBuilder.create().build();
		if (urls != null) {
			indexPosts = new HttpPost[urls.length];
			for (int i = 0; i < urls.length; i++) {
				String url = urls[i];
				if (null != url && !url.endsWith("/_bulk"))
					url = url + "/_bulk";
				indexPosts[i] = new HttpPost(url);
			}
		}

		if (log.isInfoEnabled())
			log.info("{}: {}", PARAM_URLS, Arrays.toString(urls));
		log.info("{}: {}", PARAM_INDEX_NAME, indexName);
		log.info("{}: {}", PARAM_TYPE, type);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			StopWatch w = new StopWatch();
			w.start();
			Gson gson = new Gson();

			// This is the default case: For each CAS, create one document. This
			// document is populated with fields by field generators. The field
			// generator classes are delivered by the user.
			Document singleDocument = convertCasToDocument(aJCas);
			if (null != singleDocument && !singleDocument.isEmpty()) {
				addIndexAction(indexName, singleDocument.getId(), singleDocument.getParentId(), gson);
				addIndexSource(singleDocument);
			}

			// Advanced mode: It is also possible to create more than one
			// document per CAS. By delivering DocumentGenerators, an arbitrary
			// number of documents may be created for a CAS. Examples include
			// one document for each sentence or one document for each detected
			// gene etc.
			// Internally, the DocumentGenerators employ FieldGenerators just as
			// above.
			List<Document> documents = convertCasToDocuments(aJCas);
			if (documents != null) {
				for (Document document : documents) {
					addIndexAction(document.getIndex(), document.getId(), document.getParentId(), gson);
					addIndexSource(document);
				}
			}
			w.stop();
			++docNum;
		} catch (Exception e) {
			log.error("Error with document ID {}.", JCoReTools.getDocId(aJCas));
			throw new AnalysisEngineProcessException(e);
		}
	}

	private void addIndexAction(String index, String docId, String parentId, Gson gson) {
		// { "index" : { "_index" : "test", "_type" : "type1", "_id" : "1",
		// "parent" : "1234567 } }
		if (docId == null)
			throw new IllegalArgumentException("The document ID was not specified.");
		if (index == null)
			throw new IllegalArgumentException("No target index was specified for document " + docId + ".");
		if (type == null)
			throw new IllegalArgumentException(
					"No document type was specified. For single document creation from a CAS, this type must be set in the component descriptor. When using DocumentGenerators, the generators must set the document type to the documents they create.");
		Map<String, Object> indexMap = new HashMap<>();
		indexMap.put("_index", index);
		indexMap.put("_type", type);
		indexMap.put("_id", docId);
		if (parentId != null && parentId.trim().length() > 0)
			indexMap.put("parent", parentId);
		Map<String, Object> map = new HashMap<>();
		map.put("index", indexMap);

		bulkCommand.add(gson.toJson(map));
	}

	private void addIndexSource(Document doc) {
		bulkCommand.add(createIndexSource(doc));
	}

	protected String createIndexSource(Document doc) {
		return gson.toJson(doc);
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		super.batchProcessComplete();
		log.info("Batch of {} documents is sent to ElasticSearch.", docNum);
		docNum = 0;
		postBulkIndexAction();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		log.info("Collection complete.");
		postBulkIndexAction();
	}

	private void postBulkIndexAction() throws AnalysisEngineProcessException {
		if (bulkCommand.isEmpty())
			return;
		// Post to all the ElasticSearch nodes in a round-robin fashion.
		HttpPost indexPost = indexPosts[urlIndex];
		urlIndex = (urlIndex + 1) % indexPosts.length;
		try {
			int lastIndex = 0;
			List<String> subList = null;
			do {
				subList = bulkCommand.subList(lastIndex, Math.min(bulkCommand.size(), lastIndex + 1000));
				if (subList.isEmpty())
					continue;
				lastIndex += subList.size();
				log.info("Sending {} documents.", subList.size() / 2);
				long time = System.currentTimeMillis();
				// The bulk format requires us to have a newline also after the
				// last
				// line; it will be ignored otherwise!
				String bulkCommandString = StringUtils.join(subList, "\n") + "\n";
				StringEntity bulkIndexEntity = new StringEntity(bulkCommandString, "UTF-8");
				indexPost.setEntity(bulkIndexEntity);
				HttpResponse response = httpclient.execute(indexPost);
				int statusCode = response.getStatusLine().getStatusCode();
				HttpEntity responseEntity = response.getEntity();
				if (statusCode > 200) {
					log.error("The server responded with a non-OK status code: {}", statusCode);
					log.error("Response status line: {}", response.getStatusLine());
					log.error("Response body: {}", EntityUtils.toString(responseEntity));
					log.error("Bulk command was: {}", bulkCommandString);
				}
				EntityUtils.consume(responseEntity);
				time = System.currentTimeMillis() - time;
				log.info("Sending took {}ms ({}s)", time, time / 1000);
			} while (null != subList && !subList.isEmpty());
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		} finally {
			indexPost.reset();
			bulkCommand.clear();
		}
	}

	/**
	 * Parses the response from ElasticSearch and prints out the first case where a
	 * document indexed has been returned with a non-OK status. Can be used for
	 * debugging purposes.
	 * 
	 * @param responseEntity
	 * @throws IOException
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private void printNonOkDocument(HttpEntity responseEntity) throws IOException {
		String responseString = EntityUtils.toString(responseEntity, "UTF-8");
		/*
		 * {"took":4,"items":[{"index":{"_index":"medline","_type":"docs"
		 * ,"_id":"596164","_version":1,"ok":true}},{"index":{"_index": "medline"
		 * ,"_type":"docs","_id":"1208611","_version":1,"ok":true }},{"index"
		 * :{"_index":"medline","_type":"docs","_id":"974260", "_version":
		 * 2,"ok":true}},{"index":{"_index":"medline","_type": "docs","_id"
		 * :"12278785","_version":2,"ok":true}},{"index":{"_index" :"medline"
		 * ,"_type":"docs","_id":"1002069","_version":2,"ok":true }},{"index"
		 * :{"_index":"medline","_type":"docs","_id":"4587624" ,"_version"
		 * :2,"ok":true}},{"index":{"_index":"medline","_type" :"docs","_id"
		 * :"232559","_version":2,"ok":true}},{"index":{"_index" :"medline"
		 * ,"_type":"docs","_id":"797178","_version":2,"ok":true }},{"index"
		 * :{"_index":"medline","_type":"docs","_id":"250927",
		 * "_version":2,"ok":true}}]}
		 */
		Map<String, Object> parsedResponse = gson.fromJson(responseString, Map.class);
		List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");
		for (Map<String, Object> item : items) {
			Map<String, Object> index = (Map<String, Object>) item.get("index");
			boolean statusOk = (boolean) index.get("ok");
			if (!statusOk) {
				System.out.println(index);
				System.exit(23);
			}
		}
	}

}