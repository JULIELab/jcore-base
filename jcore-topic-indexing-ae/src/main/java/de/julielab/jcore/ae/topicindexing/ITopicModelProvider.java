package de.julielab.jcore.ae.topicindexing;

import de.julielab.topicmodeling.businessobjects.Model;
import de.julielab.topicmodeling.businessobjects.Topic;
import org.apache.uima.resource.SharedResourceObject;

import java.io.IOException;
import java.util.List;

public interface ITopicModelProvider extends SharedResourceObject {

    Model getModel();

    void setModelSavePath(String path);

    void allowSave();

    void saveModel() throws IOException;

    void addToIndex(String docId, List<Topic> topicList);

    /**
     * Return an array (one element for each topic) of arrays of words, which
     * are the most probable words for that topic in descending order. These
     * are returned as Objects, but will probably be Strings.
     *
     * @param numwords The maximum length of each topic's array of words (may be less).
     */
    Object[][] getTopWords(int numwords);

}
