package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.TextAnnotation;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Caches information for the current document.
 */
public class AnnotationAdderHelper {
    // Required for token-offsets
    private List<Token> tokenList;
    private Map<Sentence, List<Token>> tokensBySentences;
    private Matcher wsFinder = Pattern.compile("\\s").matcher("");
    private Matcher nonWsMatcher = Pattern.compile("[^\\s]+").matcher("");


    public void setAnnotationOffsetsRelativeToDocument(Annotation annotation, TextAnnotation a, AnnotationAdderConfiguration configuration) throws CASException, AnnotationOffsetException {
        if (configuration.getOffsetMode() == AnnotationAdderAnnotator.OffsetMode.CHARACTER) {
            annotation.setBegin(a.getStart());
            annotation.setEnd(a.getEnd());
        } else if (configuration.getOffsetMode() == AnnotationAdderAnnotator.OffsetMode.TOKEN) {
            final JCas jCas = annotation.getCAS().getJCas();
            if (!JCasUtil.exists(jCas, Token.class))
                throw new AnnotationOffsetException("The external annotations should be added according to token offset. However, no annotations of type " + Token.class.getCanonicalName() + " are present in the CAS.");
            if (tokenList == null)
                tokenList = createTokenList(jCas, configuration);
            int startTokenNum = a.getStart();
            int endTokenNum = a.getEnd();
            if (startTokenNum < 1 || startTokenNum > tokenList.size())
                throw new AnnotationOffsetException("The current annotation to add to the CAS starts at token " + startTokenNum + " which does not fit to the range of tokens in the document which is 1 - " + tokenList.size());
            if (endTokenNum < 1 || endTokenNum > tokenList.size())
                throw new AnnotationOffsetException("The current annotation to add to the CAS ends at token " + endTokenNum + " which does not fit to the range of tokens in the document which is 1 - " + tokenList.size());
            if (endTokenNum < startTokenNum)
                throw new AnnotationOffsetException("The current annotation to add has a lower end offset than start offset. Start: " + startTokenNum + ", end: " + endTokenNum);

            int begin = tokenList.get(startTokenNum - 1).getBegin();
            int end = tokenList.get(endTokenNum - 1).getEnd();

            annotation.setBegin(begin);
            annotation.setEnd(end);
        }
    }

    public void setAnnotationOffsetsRelativeToSentence(Sentence sentence, Annotation annotation, TextAnnotation a, AnnotationAdderConfiguration configuration) throws CASException, AnnotationOffsetException {
        if (configuration.getOffsetMode() == AnnotationAdderAnnotator.OffsetMode.CHARACTER) {
            annotation.setBegin(sentence.getBegin() + a.getStart());
            annotation.setEnd(sentence.getBegin() + a.getEnd());
        } else if (configuration.getOffsetMode() == AnnotationAdderAnnotator.OffsetMode.TOKEN) {
            final JCas jCas = annotation.getCAS().getJCas();
            if (!JCasUtil.exists(jCas, Token.class))
                throw new AnnotationOffsetException("The external annotations should be added according to token offset. However, no annotations of type " + Token.class.getCanonicalName() + " are present in the CAS.");
            if (!JCasUtil.exists(jCas, Sentence.class))
                throw new AnnotationOffsetException("The external annotations should be added according to token offset relative to the sentence containing the tokens. However, no annotations of type " + Sentence.class.getCanonicalName() + " are present in the CAS.");
            tokensBySentences = createSentenceTokenMap(sentence, configuration);
            List<Token> tokenList = tokensBySentences.get(sentence);
            int startTokenNum = a.getStart();
            int endTokenNum = a.getEnd();
            if (startTokenNum < 1 || startTokenNum > tokenList.size())
                throw new AnnotationOffsetException("The current annotation to add to the CAS starts at token " + startTokenNum + " which does not fit to the range of tokens in the sentence with ID " + sentence.getId() + " which is 1 - " + tokenList.size());
            if (endTokenNum < 1 || endTokenNum > tokenList.size())
                throw new AnnotationOffsetException("The current annotation to add to the CAS ends at token " + endTokenNum + " which does not fit to the range of tokens in the sentence with ID " + sentence.getId() + " which is 1 - " + tokenList.size());
            if (endTokenNum < startTokenNum)
                throw new AnnotationOffsetException("The current annotation to add has a lower end offset than start offset. Start: " + startTokenNum + ", end: " + endTokenNum);

            int begin = tokenList.get(startTokenNum - 1).getBegin();
            int end = tokenList.get(endTokenNum - 1).getEnd();

            annotation.setBegin(begin);
            annotation.setEnd(end);
        }
    }

    public Map<Sentence, List<Token>> createSentenceTokenMap(Sentence sentence, AnnotationAdderConfiguration configuration) throws CASException {
        if (tokensBySentences != null && tokensBySentences.containsKey(sentence))
            return tokensBySentences;
        Map<Sentence, List<Token>> tokensBySentences = new HashMap<>();
        final FSIterator<Token> tokenSubiterator = sentence.getCAS().getJCas().<Token>getAnnotationIndex(Token.type).subiterator(sentence);
        List<Token> tokens = new ArrayList<>();
        while (tokenSubiterator.hasNext()) {
            Token t = tokenSubiterator.next();
            final String tokenText = t.getCoveredText();
            if (configuration.isSplitTokensAtWhitespace())
                wsFinder.reset(tokenText);
            if (wsFinder.find() && configuration.isSplitTokensAtWhitespace()) {
                nonWsMatcher.reset(tokenText);
                while (nonWsMatcher.find()) {
                    final Token subtoken = new Token(sentence.getCAS().getJCas(), t.getBegin() + nonWsMatcher.start(), t.getBegin() + nonWsMatcher.end());
                    tokens.add(subtoken);
                }
            } else {
                tokens.add(t);
            }
        }
        tokensBySentences.put(sentence, tokens);
        return tokensBySentences;
    }

    public List<Token> createTokenList(JCas jCas, AnnotationAdderConfiguration configuration) {
        if (tokenList != null)
            return tokenList;
        List<Token> tokenList = new ArrayList<>();
        final FSIterator<Annotation> tokenIt = jCas.getAnnotationIndex(Token.type).iterator(false);
        while (tokenIt.hasNext()) {
            Token t = (Token) tokenIt.next();
            final String tokenText = t.getCoveredText();
            if (configuration.isSplitTokensAtWhitespace())
                wsFinder.reset(tokenText);
            if (wsFinder.find() && configuration.isSplitTokensAtWhitespace()) {
                nonWsMatcher.reset(tokenText);
                while (nonWsMatcher.find()) {
                    final Token subtoken = new Token(jCas, t.getBegin() + nonWsMatcher.start(), t.getBegin() + nonWsMatcher.end());
                    tokenList.add(subtoken);
                }
            } else {
                tokenList.add(t);
            }
        }
        return tokenList;
    }
}
