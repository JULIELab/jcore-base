package de.julielab.jcore.ae.topicindexing;

import de.julielab.topicmodeling.businessobjects.Model;
import de.julielab.topicmodeling.businessobjects.Topic;
import de.julielab.topicmodeling.services.MalletTopicModeling;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TopicModelProvider implements ITopicModelProvider {

    private final static Logger log = LoggerFactory.getLogger(TopicModelProvider.class);

    private Model model;
    private File modelFile;
    private String modelSavePath;
    private boolean saveAllowed;
    private MalletTopicModeling tm;
    private int numTopicWords;
    private Object[][] topicWords;

    @Override
    public void load(DataResource dataResource) throws ResourceInitializationException {
        modelFile = new File(dataResource.getUri());
        tm = new MalletTopicModeling();
        model = tm.readModel(modelFile.getAbsolutePath());
        // Fix for the issue that the model did not save the reverse ID map
        if (model.pubmedIdModelId == null || model.pubmedIdModelId.isEmpty()) {
            model.pubmedIdModelId = new HashMap<>();
            for (Integer malletId : model.ModelIdpubmedId.keySet())
                model.pubmedIdModelId.put(model.ModelIdpubmedId.get(malletId), malletId);
        }
        model.index = new HashMap<>();
        saveAllowed = true;
    }

    @Override
    public synchronized Object[][] getTopWords(int numwords){
        if (numwords > numTopicWords) {
            topicWords = model.malletModel.getTopWords(numwords);
            numTopicWords = numwords;
        }
        return topicWords;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void setModelSavePath(String path) {
        if (modelSavePath != null && !modelSavePath.equals(path))
            throw new IllegalStateException("The model path is already set to \"" + modelSavePath + "\". The current call wants to set the path to \"" + modelSavePath + "\". This points to a programming error.");
        this.modelSavePath = path;
    }

    @Override
    public void allowSave() {
        saveAllowed = true;
    }

    @Override
    public synchronized void saveModel() throws IOException {
        if (saveAllowed) {
            try {
                String filename = modelFile.getAbsolutePath() + "-" + InetAddress.getLocalHost().getHostName() + "-"
                        + ManagementFactory.getRuntimeMXBean().getName();
                tm.saveModel(model, filename);
                log.info("Model with index of size {} is written to: {}", model.index.size(), filename);
            } catch (UnknownHostException e) {
                throw new IOException(e);
            }
            saveAllowed = false;
        }
    }

    @Override
    public synchronized void addToIndex(String docId, List<Topic> topicList) {
        model.index.put(docId, topicList);
    }
}
