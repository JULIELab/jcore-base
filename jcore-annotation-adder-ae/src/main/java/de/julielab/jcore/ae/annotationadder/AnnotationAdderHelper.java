package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.TextAnnotation;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Caches information for the current document.
 */
public class AnnotationAdderHelper {
    private final static Logger log = LoggerFactory.getLogger(AnnotationAdderHelper.class);
    // Required for token-offsets
    private List<Token> tokenList;
    private Map<Sentence, List<Token>> tokensBySentences;
    private Matcher wsFinder = Pattern.compile("\\s").matcher("");
    private Matcher nonWsMatcher = Pattern.compile("[^\\s]+").matcher("");
    /**
     * Caches methods for feature
     */
    private Map<String, Method> featureSetters;

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
            if (startTokenNum < 1 || startTokenNum > tokenList.size()) {
                log.error("Cannot create entity because of a token offset mismatch. The entity should tart at token {} and end at {}. But there are only {} tokens available: {}", startTokenNum, endTokenNum, tokenList.size(), tokenList.stream().map(Annotation::getCoveredText).collect(Collectors.joining(" ")));
                throw new AnnotationOffsetException("The current annotation to add to the CAS starts at token " + startTokenNum + " which does not fit to the range of tokens in the sentence with ID " + sentence.getId() + " which is 1 - " + tokenList.size());
            }
            if (endTokenNum < 1 || endTokenNum > tokenList.size())
                throw new AnnotationOffsetException("The current annotation to add to the CAS ends at token " + endTokenNum + " which does not fit to the range of tokens in the sentence with ID " + sentence.getId() + " which is 1 - " + tokenList.size());
            if (endTokenNum < startTokenNum)
                throw new AnnotationOffsetException("The current annotation to add has a lower end offset than start offset. Start: " + startTokenNum + ", end: " + endTokenNum);

            int begin = tokenList.get(startTokenNum - 1).getBegin();
            int end = tokenList.get(endTokenNum - 1).getEnd();

            annotation.setBegin(begin);
            annotation.setEnd(end);

            if (end > sentence.getCAS().getDocumentText().length())
                throw new IllegalStateException("The TextAnnotation " + a + " specifies an end offset that is outside of the document text which has a length of " + sentence.getCAS().getDocumentText().length());

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

    public void setAnnotationPayloadsToFeatures(Annotation annotation, ExternalTextAnnotation a) {
        Collection<String> keys = a.getPayloadKeys();
        if (!keys.isEmpty())
            featureSetters = new HashMap<>();
        try {
            for (String key : keys) {
                Object value = a.getPayload(key);
                Method setter = featureSetters.get(key);
                if (setter == null) {
                    setter = annotation.getClass().getMethod("set" + StringUtils.capitalize(key), value.getClass());
                    featureSetters.put(key, setter);
                }
                setter.invoke(annotation, value);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
