package de.julielab.jcore.ae.topicindexing;

import de.julielab.topicmodeling.businessobjects.Model;
import de.julielab.topicmodeling.businessobjects.Topic;
import org.apache.uima.resource.SharedResourceObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ITopicModelProvider extends SharedResourceObject {
    Model getModel();

    void setModelSavePath(String path);

    void allowSave();

    void saveModel() throws IOException;

    void addToIndex(String docId, List<Topic> topicList);
}
