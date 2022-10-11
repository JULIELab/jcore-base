package de.julielab.jcore.ae.lingpipegazetteer.utils;

import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.ibm.icu.text.Transliterator;
import de.julielab.java.utilities.spanutils.OffsetSet;
import org.apache.commons.lang3.Range;

import java.util.*;

public class StringNormalizerForChunking {


    private static Set<Character> charsToDelete = new HashSet<>();

    static {
        charsToDelete.add('-');
        charsToDelete.add('+');
        charsToDelete.add(',');
        charsToDelete.add('.');
        charsToDelete.add(':');
        charsToDelete.add(';');
        charsToDelete.add('?');
        charsToDelete.add('!');
        charsToDelete.add('*');
        charsToDelete.add('§');
        charsToDelete.add('$');
        charsToDelete.add('%');
        charsToDelete.add('&');
        charsToDelete.add('/');
        charsToDelete.add('\\');
        charsToDelete.add('(');
        charsToDelete.add(')');
        charsToDelete.add('<');
        charsToDelete.add('>');
        charsToDelete.add('[');
        charsToDelete.add(']');
        charsToDelete.add('=');
        charsToDelete.add('\'');
        charsToDelete.add('`');
        charsToDelete.add('´');
        charsToDelete.add('"');
        charsToDelete.add('#');

        // this would normalize German umlauts like Hörsturz -> Hoersturz
        // I leave it here for the future but don't add it right now because I don't want to make this Transliterator
        // a static field due to Thread safety and also don't have time now to refactor this all
//        String rules = "[\\u00E4{a\\u0308}] > ae; " +
//                " [\\u00F6{o\\u0308}] > oe;" +
//                " [\\u00FC{u\\u0308}] > ue;" +
//                " {[\\u00C4{A\\u0308}]}[:Lowercase:] > Ae;" +
//                " {[\\u00D6{O\\u0308}]}[:Lowercase:] > Oe;" +
//                " {[\\u00DC{U\\u0308}]}[:Lowercase:] > Ue;" +
//                " [\\u00C4{A\\u0308}] > AE;" +
//                " [\\u00D6{O\\u0308}] > OE;" +
//                " [\\u00DC{U\\u0308}] > UE;" +
//                " [\\u20AC] > EUR;";
//
//        germanUmlautTransliterator = Transliterator.createFromRules("de_EUR-ASCII", rules, Transliterator.FORWARD);
    }

    /**
     * This method was meant for text normalization by just deleting punctuation
     * characters. However, the approach turned out to be suboptimal in cases
     * where a dictionary entry would be "SHP-1" and the text form would be "SHP
     * 1". That is, when in the text there is just a whitespace where there is a
     * punctuation character in the dictionary, we won't recognize the
     * dictionary entry. Thus, a different normalization was developed, namely
     * in the other normalization method. It is supposed to be used together
     * with an approximate chunker.
     *
     * @param str
     * @return
     */
    public static NormalizedString normalizeString(String str) {
        NormalizedString ns = new NormalizedString();
        StringBuilder sb = new StringBuilder();
        int deletedChars = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (charsToDelete.contains(c)) {
                deletedChars++;
                // switch (mode) {
                // case REPLACE: sb.append(" "); break;
                // case DELETE: deletedChars++; break;
                // }
            } else {
                sb.append(c);
            }
            int newOffset = Math.max(0, i - deletedChars);
            if (null == ns.offsetMap.get(newOffset))
                ns.offsetMap.put(newOffset, i);
        }
        ns.string = sb.toString();
        return ns;
    }

    /**
     * This normalization method uses a given TokenizerFactory (could also be a
     * PorterStemmerTokenizerFactory for stemming) and additionally removes
     * possessive 's constructions. Dashes and other punctuation is left
     * untouched. By using an approximate chunker, one can also handle
     * punctuation.
     *
     * @param str
     * @param tokenizerFactory
     * @return
     */
    public static NormalizedString normalizeString(String str, TokenizerFactory tokenizerFactory, boolean normalizePlural, OffsetSet pluralPositions,
                                                   Transliterator transliterator) {


        boolean stemming = tokenizerFactory instanceof
                PorterStemmerTokenizerFactory;

        NormalizedString ns = new NormalizedString();

        char[] strChars = str.toCharArray();
        Tokenizer tokenizer = tokenizerFactory.tokenizer(strChars, 0, strChars.length);
        StringBuilder sb = new StringBuilder();
        ArrayDeque<String> tokenS = new ArrayDeque<>();
        Map<Integer, Integer> deleteCandidateOffsetMap = new HashMap<>();
        // According to the lingpipe API documentation, one starts with the next
        // whitespace.
        sb.append(tokenizer.nextWhitespace());
        ns.offsetMap.put(0, 0);
        String token;
        while ((token = tokenizer.nextToken()) != null) {
            // Handle possessive 's (like Parkinson's). It will be deleted. In
            // case we have accidentally deleted some
            // tokens, those are stored in the stack and their offsets are
            // stored, too. In case it was an error, the
            // tokens are later added again in the "else" path.
            if (token.equals("'")) {
                int newStartOffset = sb.length() + sumOfStack(tokenS);
                int newEndOffset = sb.length() + sumOfStack(tokenS) + token.length();
                deleteCandidateOffsetMap.put(newStartOffset, tokenizer.lastTokenStartPosition());
                deleteCandidateOffsetMap.put(newEndOffset, tokenizer.lastTokenEndPosition());
                tokenS.push(token + tokenizer.nextWhitespace());
            } else if (token.equals("s") && tokenS.size() == 1) {
                int newStartOffset = sb.length() + sumOfStack(tokenS);
                int newEndOffset = sb.length() + sumOfStack(tokenS) + token.length();
                deleteCandidateOffsetMap.put(newStartOffset, tokenizer.lastTokenStartPosition());
                deleteCandidateOffsetMap.put(newEndOffset, tokenizer.lastTokenEndPosition());
                tokenS.push(token);
                String ws = tokenizer.nextWhitespace();
                if (ws.length() > 0) {
                    sb.append(ws);
                    tokenS.clear();
                    deleteCandidateOffsetMap.clear();
                }
            } else {
                if (!tokenS.isEmpty()) {
                    for (String s : tokenS) {
                        sb.append(s);
                    }
                    tokenS.clear();
                    ns.offsetMap.putAll(deleteCandidateOffsetMap);
                    deleteCandidateOffsetMap.clear();
                }
                token = transliterator.transform(token);
//                token = germanUmlautTransliterator.transliterate(token);
                // plural s, only when no stemming is done
                // an even better normalization would be to use the lemma, of course
                Range<Integer> tokenOffsets = Range.between(tokenizer.lastTokenStartPosition(), tokenizer.lastTokenEndPosition());
                try {
                    if (normalizePlural && !stemming && token.endsWith("s") && pluralPositions != null && !pluralPositions.isEmpty() && Optional.ofNullable(pluralPositions.locate(tokenOffsets)).orElse(Range.between(0, 0)).isOverlappedBy(tokenOffsets))
                        token = token.substring(0, token.length() - 1);
                } catch (Exception e) {
                    System.out.println("normalizePlural: " + normalizePlural);
                    System.out.println("stemming: " + stemming);
                    System.out.println("Token: " + token);
                    System.out.println("PluralPositions: " + pluralPositions);
                    System.out.println("TokenOffsets: " + tokenOffsets);
                    System.out.println("pluralPositions.locate(tokenOffsets): " + pluralPositions.locate(tokenOffsets));
                    e.printStackTrace();
                }
                sb.append(token);
                int newStartOffset = sb.length() - token.length();
                int newEndOffset = sb.length();
                ns.offsetMap.put(newStartOffset, tokenizer.lastTokenStartPosition());
                ns.offsetMap.put(newEndOffset, tokenizer.lastTokenEndPosition());
                sb.append(tokenizer.nextWhitespace());
            }
        }
        ns.string = sb.toString();
        return ns;
    }

    private static int sumOfStack(Deque<String> stack) {
        int sum = 0;
        for (String i : stack)
            sum += i.length();
        return sum;
    }

    public static NormalizedString normalizeString(String str, TokenizerFactory tokenizerFactory, Transliterator transliterator) {
        return normalizeString(str, tokenizerFactory, false, null, transliterator);
    }

    public static NormalizedString normalizeString(String str, boolean normalizePlural, OffsetSet pluralPositions, TokenizerFactory tokenizerFactory) {
        return normalizeString(str, tokenizerFactory, normalizePlural, pluralPositions, null);
    }

    public enum Mode {
        /**
         * Punctuation characters are deleted completely, shrinking the string.
         */
        DELETE,
        /**
         * Punctuation characters are replaced by white spaces.
         */
        REPLACE
    }

    public static class NormalizedString {
        public String string;
        private Map<Integer, Integer> offsetMap = new HashMap<>();
        private TreeSet<Integer> normalizedOffsetSet;

        public Map<Integer, Integer> getOffsetMap() {
            return offsetMap;
        }

        public Integer getOriginalOffset(int normalizedOffset) {
            Integer originalOffset = offsetMap.get(normalizedOffset);
            if (originalOffset == null) {
                originalOffset = deriveOriginalOffset(normalizedOffset);
                offsetMap.put(normalizedOffset, originalOffset);
            }
            return originalOffset;
        }

        private Integer deriveOriginalOffset(int normalizedOffset) {
            if (normalizedOffsetSet == null)
                normalizedOffsetSet = new TreeSet<>(offsetMap.keySet());
            Integer previousNormalizedOffset = normalizedOffsetSet.floor(normalizedOffset);
            Integer originalPreviousOffset = offsetMap.get(previousNormalizedOffset);
            int offsetShift = Math.abs(originalPreviousOffset - previousNormalizedOffset);
            // Typically, the normalized string will be shorter than the
            // original, thus the original offset would be larger.
            if (originalPreviousOffset > previousNormalizedOffset)
                return normalizedOffset + offsetShift;
            // But if, for some reason, the normalized string is longer than the
            // original, we would have to subtract the difference from the
            // normalized offset.
            return normalizedOffset - offsetShift;
        }
    }
}
