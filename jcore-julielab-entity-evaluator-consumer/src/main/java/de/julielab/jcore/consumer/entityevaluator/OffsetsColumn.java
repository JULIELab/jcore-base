/**
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author:
 * <p>
 * Description:
 **/
package de.julielab.jcore.consumer.entityevaluator;

import de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.OffsetMode;
import de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.OffsetScope;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;

public class OffsetsColumn extends Column {

    private OffsetMode offsetMode;
    private JCoReTreeMapAnnotationIndex<Long, ? extends Annotation> sentenceIndex;
    private OffsetScope offsetScope;
    private Map<Annotation, NavigableMap<Integer, Integer>> numWsMaps;
    private NavigableMap<Integer, Integer> documentNumWsMap;

    public OffsetsColumn(NavigableMap<Integer, Integer> numWsMap, OffsetMode offsetMode) {
        this();
        this.documentNumWsMap = numWsMap;
        this.offsetMode = offsetMode;
        this.offsetScope = OffsetScope.Document;
    }

    public OffsetsColumn(JCoReTreeMapAnnotationIndex<Long, ? extends Annotation> sentenceIndex, OffsetMode offsetMode) {
        this();
        this.sentenceIndex = sentenceIndex;
        this.offsetMode = offsetMode;
        this.offsetScope = OffsetScope.Sentence;
    }

    public OffsetsColumn(OffsetMode offsetMode) {
        this();
        this.offsetMode = offsetMode;
        this.offsetScope = OffsetScope.Document;
    }

    private OffsetsColumn() {
        this.name = EntityEvaluatorConsumer.OFFSETS_COLUMN;
        this.numWsMaps = new HashMap<>();
    }

    @Override
    public Deque<String> getValue(TOP a, JCas aJCas) {
        Deque<String> ret = new ArrayDeque<>(1);
        if (a != null) {
            Annotation an = (Annotation) a;
            NavigableMap<Integer, Integer> numWsMap = documentNumWsMap;
            int annotationOffset = 0;

            if (offsetScope == OffsetScope.Sentence) {
                Annotation s = sentenceIndex.get(an);
                if (this.offsetMode == OffsetMode.NonWsCharacters)
                    numWsMap = getNumWsMapForSentence(s);
                annotationOffset = s.getBegin();
            }

            final String offsets = getOffsets(an, numWsMap, annotationOffset);
            ret.add(offsets);
        } else {
            ret.add("\t");
        }
        return ret;
    }

    /**
     * Creates and caches whitespace maps for sentence annotations.
     *
     * @param s
     *            The sentence for which a whitespace map is requested.
     * @return A map that can be queried for each position of the sentence text
     *         how many whitespaces before that position exist.
     */
    private NavigableMap<Integer, Integer> getNumWsMapForSentence(Annotation s) {
        NavigableMap<Integer, Integer> numWsMap = numWsMaps.get(s);
        if (numWsMap == null) {
            numWsMap = EntityEvaluatorConsumer.createNumWsMap(s.getCoveredText());
            numWsMaps.put(s, numWsMap);
        }
        return numWsMap;
    }

    private String getOffsets(Annotation an, NavigableMap<Integer, Integer> numWsMap, int annotationOffset) {
        int beginOffset;
        int endOffset;
        switch (offsetMode) {
            case CharacterSpan:
                beginOffset = an.getBegin() - annotationOffset;
                endOffset = an.getEnd() - annotationOffset;
                break;
            case NonWsCharacters:
                // for both offsets, subtract the number of preceding
                // white spaces up to the respective offset
                beginOffset = (an.getBegin() - annotationOffset) - numWsMap.floorEntry(an.getBegin()).getValue();
                // we even have to subtract one more because we count
                // actual characters while UIMA counts spans
                endOffset = (an.getEnd() - annotationOffset) - numWsMap.floorEntry(an.getEnd()).getValue() - 1;
                break;
            default:
                throw new IllegalArgumentException("Offset mode \"" + offsetMode + "\" is currently unsupported.");
        }
        String begin = String.valueOf(beginOffset);
        String end = String.valueOf(endOffset);
        return begin + "\t" + end;
    }

    public void reset() {
        numWsMaps.clear();
    }

}
