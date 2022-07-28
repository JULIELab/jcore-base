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

import de.julielab.jcore.utility.JCoReFeaturePath;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.cas.TOP;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureValueFilter {
    private static final Pattern fullFilterDefinitionFormat = Pattern.compile("(.+:)?\\/(([^=]+=.[^=;]*;?)+)");
    protected Set<Type> types;
    protected PathValuePair pathValuePair;
    private Matcher mfull;
    private BiFunction<String, String, Boolean> featureValueMatchTest;

    public FeatureValueFilter(String columnDefinition, String typePrefix, TypeSystem ts, boolean allowRegexForFilters) {
        this();
        parseAndAddDefinition(columnDefinition, typePrefix, ts);
        featureValueMatchTest = allowRegexForFilters ? String::matches : String::equals;
    }

    public FeatureValueFilter() {
        mfull = fullFilterDefinitionFormat.matcher("");
    }

    public void parseAndAddDefinition(String filterDefinition, String typePrefix, TypeSystem ts) {
        if (!mfull.reset(filterDefinition).matches()) {
            throw new IllegalArgumentException(
                    "The line does not obey the column definition syntax: " + filterDefinition);
        }
        if (filterDefinition.contains(":")) {
            String[] colonSplit = filterDefinition.split(":");
            types = Stream.of(colonSplit[0].split("\\s*,\\s*"))
                    .map(typeName -> EntityEvaluatorConsumer.findType(typeName, typePrefix, ts))
                    .collect(Collectors.toSet());
            pathValuePair = new PathValuePair(colonSplit[1].split("="));
        } else {
            types = Collections.emptySet();
            pathValuePair = new PathValuePair(filterDefinition.split("="));
        }
    }

    public boolean contradictsFeatureFilter(TOP a) {
        Type type = a.getType();
        if (!types.contains(type) && !types.isEmpty())
            return false;
        String fpValue = pathValuePair.fp.getValueAsString(a);
        if (fpValue != null)
            return pathValuePair.targetValue == null || !featureValueMatchTest.apply(fpValue, pathValuePair.targetValue);
        return pathValuePair.targetValue != null;
    }

    public PathValuePair getPathValuePair() {
        return pathValuePair;
    }

    /**
     * A pair of a feature path and the value it should have.
     *
     * @author faessler
     *
     */
    public static class PathValuePair {
        public JCoReFeaturePath fp;
        public String targetValue;
        public PathValuePair(String[] split) {
            try {
                fp = new JCoReFeaturePath();
                fp.initialize(split[0]);
                targetValue = split[1];
            } catch (CASException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
