package lingscope.io;

import generalutils.FileOperations;
import lingscope.structures.AnnotatedSentence;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes annotated sentences
 * @author shashank
 */
public class AnnotatedSentencesIO {

    /**
     * Reads and returns the list of {@link AnnotatedSentence} from the given
     * file
     * @param fileToRead
     * @return
     */
    public static List<AnnotatedSentence> read(String fileToRead) {
        List<AnnotatedSentence> sentences = null;
        try {
            List<String> rawSentences = FileOperations.readFile(fileToRead);
            sentences = new ArrayList<AnnotatedSentence>(rawSentences.size());
            for (String rawSentence : rawSentences) {
                sentences.add(new AnnotatedSentence(rawSentence));
            }
        } catch (Exception ex) {
            Logger.getLogger(AnnotatedSentencesIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sentences;
    }

    /**
     * Writes the given list of annotated sentences to the given fileToWrite
     * @param fileToWrite
     * @param annotatedSentences
     */
    public static void write(String fileToWrite, List<AnnotatedSentence> annotatedSentences) {
        List<String> rawSentences = new ArrayList<String>(annotatedSentences.size());
        for (AnnotatedSentence annotatedSentence : annotatedSentences) {
            rawSentences.add(annotatedSentence.getRawText());
        }
        try {
            FileOperations.writeFile(fileToWrite, rawSentences);
        } catch (Exception ex) {
            Logger.getLogger(AnnotatedSentencesIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
