package de.julielab.jcore.consumer.es;

import com.google.gson.Gson;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.utility.JCoReTools;
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
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@ResourceMetaData(name = "JCore ElasticSearch Consumer")
public class ElasticSearchConsumer extends AbstractCasToJsonConsumer {

    public static final String PARAM_URLS = "urls";
    public static final String PARAM_INDEX_NAME = "indexName";
    /**
     * Between ES 6 and 7, the type is a constant value for each index, i.e. there
     * only is one type. From ES 7, types will be removed entirely.
     */
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_BATCH_SIZE = "batchSize";
    public static final String PARAM_DELETE_DOCS_BEFORE_INDEXING = "deleteDocumentsBeforeIndexing";
    public static final String PARAM_DOC_ID_FIELD = "documentIdField";
    final Logger log = LoggerFactory.getLogger(ElasticSearchConsumer.class);
    @ConfigurationParameter(name = PARAM_URLS, description = "A list of URLs pointing to different nodes of the ElasticSearch cluster, e.g. http://localhost:9300/. Documents will be sent bulk-wise to the nodes in a round-robin fashion.")
    private String[] urls;
    @ConfigurationParameter(name = PARAM_INDEX_NAME, description = "The ElasticSearch index name to send the created documents to.")
    private String indexName;
    @ConfigurationParameter(name = PARAM_TYPE, mandatory = false, description = "The index type the generated documents should have. The types are removed from ElasticSearch with version 7 and should be omitted for ES >= 7.")
    private String type;
    @ConfigurationParameter(name = PARAM_BATCH_SIZE, mandatory = false, description = "The number of documents to be sent to ElasticSearch in a single batch. Defaults to 50.")
    private int batchSize;
    @ConfigurationParameter(name = PARAM_DELETE_DOCS_BEFORE_INDEXING, mandatory = false, description = "Whether or not to delete documents with the docId of the UIMA CASes in ElasticSearch prior to indexing. This is useful when parts of the document are indexed whose IDs are not stable or that might change after document updates and would not just be overwritten when indexing anew. Defaults to false.")
    private boolean deleteDocsBeforeIndexing;
    @ConfigurationParameter(name = PARAM_DOC_ID_FIELD, mandatory = false, description = "Required when " + PARAM_DELETE_DOCS_BEFORE_INDEXING + " is set to true. This should be an existing index field that contains the document ID of each CAS. It is used to remove existing index documents related to the CAS document ID prior to indexing.")
    private String docIdField;

    private List<String> bulkCommand;
    private List<String> docIdsToDelete;
    private HttpPost[] indexPosts;
    private HttpPost[] indexDeletes;

    private int urlIndex = 0;

    private HttpClient httpclient;

    private int docNum = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        urls = (String[]) getContext().getConfigParameterValue(PARAM_URLS);
        indexName = (String) getContext().getConfigParameterValue(PARAM_INDEX_NAME);
        type = (String) getContext().getConfigParameterValue(PARAM_TYPE);
        batchSize = Optional.ofNullable((Integer) getContext().getConfigParameterValue(PARAM_BATCH_SIZE)).orElse(50);
        bulkCommand = new ArrayList<>(batchSize*2);
        deleteDocsBeforeIndexing = (boolean) Optional.ofNullable(getContext().getConfigParameterValue(PARAM_DELETE_DOCS_BEFORE_INDEXING)).orElse(false);
        docIdField = (String) getContext().getConfigParameterValue(PARAM_DOC_ID_FIELD);

        if (deleteDocsBeforeIndexing && docIdField == null)
            throw new ResourceInitializationException(new IllegalArgumentException(PARAM_DELETE_DOCS_BEFORE_INDEXING + " is true but no " + PARAM_DOC_ID_FIELD + " was specified."));

        httpclient = HttpClientBuilder.create().build();
        if (urls != null) {
            indexPosts = new HttpPost[urls.length];
            for (int i = 0; i < urls.length; i++) {
                String url = urls[i];
                if (null != url && !url.endsWith("/_bulk"))
                    url = url + "/_bulk";
                indexPosts[i] = new HttpPost(url);
                indexPosts[i].addHeader("Content-Type", "application/x-ndjson");
            }
        }

        if (deleteDocsBeforeIndexing) {
            indexDeletes = new HttpPost[urls.length];
            for (int i = 0; i < urls.length; i++) {
                String url = urls[i];
                if (null != url && url.endsWith("/_bulk"))
                    url = url.replace("/_bulk/?", "");
                url += "/" + indexName + "/" + "_delete_by_query";
                indexDeletes[i] = new HttpPost(url);
                indexDeletes[i].addHeader("Content-Type", "application/x-ndjson");

            }
            docIdsToDelete = new ArrayList<>();
        }

        if (log.isInfoEnabled()) {
            log.info("{}: {}", PARAM_URLS, Arrays.toString(urls));
            log.info("{}: {}", PARAM_INDEX_NAME, indexName);
            log.info("{}: {}", PARAM_TYPE, type);
            log.info("{}: {}", PARAM_DELETE_DOCS_BEFORE_INDEXING, deleteDocsBeforeIndexing);
            log.info("{}: {}", PARAM_DOC_ID_FIELD, docIdField);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            StopWatch w = new StopWatch();
            w.start();
            Gson gson = new Gson();

            if (deleteDocsBeforeIndexing) {
                docIdsToDelete.add(JCoReTools.getDocId(aJCas));
            }

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
                    addIndexAction(document.getIndex() != null ? document.getIndex() : indexName, document.getId(), document.getParentId(), gson);
                    addIndexSource(document);
                }
            }
            w.stop();
            ++docNum;
            if (docNum % batchSize == 0)
                customBatchProcessComplete();
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
        Map<String, Object> indexMap = new HashMap<>();
        indexMap.put("_index", index);
        // Since ES7 there are no types. So types are now optional.
        if (type != null)
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

    // Intentional taken out of the UIMA CPE batch processing complete flow because it only seems to work with CPEs
    // and even then only when the component is not included into an AAE.
    public void customBatchProcessComplete() throws AnalysisEngineProcessException {
        super.batchProcessComplete();
        log.debug("Batch of {} documents is sent to ElasticSearch.", docNum);
        docNum = 0;
        deleteDocuments();
        postBulkIndexAction();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        log.info("Collection complete.");
        deleteDocuments();
        postBulkIndexAction();
    }

    private void deleteDocuments() throws AnalysisEngineProcessException {
        if (deleteDocsBeforeIndexing) {
            // Post to all the ElasticSearch nodes in a round-robin fashion.
            HttpPost indexDelete = indexDeletes[urlIndex];
            urlIndex = (urlIndex + 1) % indexDeletes.length;
            try {
                int lastIndex = 0;
                List<String> subList;
                do {
                    subList = docIdsToDelete.subList(lastIndex, Math.min(docIdsToDelete.size(), lastIndex + 1000));
                    if (subList.isEmpty())
                        continue;
                    lastIndex += subList.size();
                    log.debug("Delete {} documents in index {}.", subList.size(), indexName);
                    long time = System.currentTimeMillis();
                    StringBuilder deleteQuery = new StringBuilder();
                    deleteQuery.append("{\"query\":{\"terms\":{\"").append(docIdField).append("\":[");
                    for (int i = 0; i < subList.size(); i++) {
                        String docId = subList.get(i);
                        deleteQuery.append("\"").append(docId).append("\"");
                        if (i < subList.size() - 1)
                            deleteQuery.append(",");
                    }
                    deleteQuery.append("]}}}");
                    StringEntity deleteByQueryEntity = new StringEntity(deleteQuery.toString(), "UTF-8");
                    indexDelete.setEntity(deleteByQueryEntity);
                    HttpResponse response = httpclient.execute(indexDelete);
                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity responseEntity = response.getEntity();
                    if (statusCode > 200) {
                        log.error("The server responded with a non-OK status code: {}", statusCode);
                        log.error("Response status line: {}", response.getStatusLine());
                        log.error("Response body: {}", EntityUtils.toString(responseEntity));
                        log.error("Delete-by-query command was: {}", deleteQuery);
                    }
                    EntityUtils.consume(responseEntity);
                    time = System.currentTimeMillis() - time;
                    log.debug("Sending took {}ms ({}s) and returned status code {}", time, time / 1000, statusCode);
                } while (null != subList && !subList.isEmpty());
            } catch (IOException e) {
                log.error("Error when sending data to ElasticSearch:", e);
                throw new AnalysisEngineProcessException(e);
            } finally {
                indexDelete.reset();
                docIdsToDelete.clear();
            }
        }
    }

    private void postBulkIndexAction() throws AnalysisEngineProcessException {
        if (bulkCommand.isEmpty())
            return;
        // Post to all the ElasticSearch nodes in a round-robin fashion.
        HttpPost indexPost = indexPosts[urlIndex];
        urlIndex = (urlIndex + 1) % indexPosts.length;
        try {
            int lastIndex = 0;
            List<String> subList;
            do {
                subList = bulkCommand.subList(lastIndex, Math.min(bulkCommand.size(), lastIndex + 1000));
                if (subList.isEmpty())
                    continue;
                lastIndex += subList.size();
                log.debug("Sending {} documents to index {}.", subList.size()/2, indexName);
                long time = System.currentTimeMillis();
                // The bulk format requires us to have a newline also after the
                // last
                // line; it will be ignored otherwise!
                String bulkCommandString = StringUtils.join(subList, "\n") + "\n";
                log.trace(bulkCommandString);
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
                log.debug("Sending took {}ms ({}s) and returned status code {}", time, time / 1000, statusCode);
            } while (null != subList && !subList.isEmpty());
        } catch (IOException e) {
            log.error("Error when sending data to ElasticSearch:", e);
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
    @SuppressWarnings({"unused", "unchecked"})
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
