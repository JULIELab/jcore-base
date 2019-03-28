package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalAnnotation;
import de.julielab.jcore.types.Token;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Caches information for the current document.
 */
public class AnnotationAdderHelper {
    // Required for token-offsets
    private List<Token> tokenList;

    public void setAnnotationOffsets(Annotation annotation, ExternalAnnotation a, AnnotationAdderConfiguration configuration) throws CASException {
        if (configuration.getOffsetMode() == AnnotationAdderAnnotator.OffsetMode.CHARACTER) {
            annotation.setBegin(a.getStart());
            annotation.setEnd(a.getEnd());
        } else if (configuration.getOffsetMode() == AnnotationAdderAnnotator.OffsetMode.TOKEN) {
            final JCas jCas = annotation.getCAS().getJCas();
            if (!JCasUtil.exists(jCas, Token.class))
                throw new IllegalArgumentException("The external annotations should be added according to token offset. However, no annotations of type " + Token.class.getCanonicalName() + " are present in the CAS.");
            if (tokenList == null)
                createTokenList(jCas);
            int startTokenNum = a.getStart();
            int endTokenNum = a.getEnd();
            if (startTokenNum < 1 || startTokenNum > tokenList.size())
                throw new IllegalArgumentException("The current annotation to add to the CAS starts at token " + startTokenNum + " which does not fit to the range of tokens in the document which is 1 - " + tokenList.size());
            if (endTokenNum < 1 || endTokenNum > tokenList.size())
                throw new IllegalArgumentException("The current annotation to add to the CAS ends at token " + endTokenNum + " which does not fit to the range of tokens in the document which is 1 - " + tokenList.size());
            if (endTokenNum < startTokenNum)
                throw new IllegalArgumentException("The current annotation to add has a lower end offset than start offset. Start: " + startTokenNum + ", end: " + endTokenNum);

            int begin = tokenList.get(startTokenNum-1).getBegin();
            int end = tokenList.get(endTokenNum-1).getEnd();

            annotation.setBegin(begin);
            annotation.setEnd(end);
        }
    }

    private void createTokenList(JCas jCas) {
        tokenList = new ArrayList<>();
        final FSIterator<Annotation> tokenIt = jCas.getAnnotationIndex(Token.type).iterator(false);
        while (tokenIt.hasNext()) {
            Token token = (Token) tokenIt.next();
            tokenList.add(token);
        }
    }
}
