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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Column {
    /**
     * Pattern to check if a string matches the column definition syntax at all.
     * This expression matches line like
     *
     * <pre>
     * entityid:Chemical,Gene=/registryNumber;Disease=/specificType
     * entityid:Chemical,Gene=/registryNumber;
     * entityid:Disease=/specificType
     * entityid:/id
     * entityid:/:getCoveredText()
     * </pre>
     *
     * but not
     *
     * <pre>
     * entityid:Gene=
     * entityid:Chemical,Gene=/registryNumber;Disease=
     * entityid:Chemical:number
     * </pre>
     */
    private static final Pattern fullColumnDefinitionFormat = Pattern.compile(".+:(([^=]+=.[^=;]*;?)+|\\/[^;,=]+)");
    /**
     * Matches:<br>
     * <b>Chemical=/registryNumber</b>;<b>Disease=/specificType</b><br>
     * <b>Chemical,Disease=/registryNumber</b> entityid:Gene=/species
     * <b>Gene,Organism=/specificType</b>
     * <b>Gene,Chemical=/specificType</b>;<b>Organism=/id</b>
     * <b>/value</b>;<b>Gene=specificType</b>
     * <b>/value</b>
     * <b>/:getCoveredText()</b>
     */
    private static final Pattern typeDefinitionFormat = Pattern.compile("([^:;]+=[^;]+|\\/:?[^;]+)");
    /**
     * Groups type definitions in their elements:<br>
     * <b>Chemical</b>,<b>Gene</b>=<b>/registryNumber</b>
     *
     */
    private static final Pattern typeDefinitionElementsPattern = Pattern.compile("([^,=]+)");
    protected String name;
    protected Map<Type, JCoReFeaturePath> featurePathMap;
    protected JCoReFeaturePath globalFeaturePath;
    protected boolean isMultiValued = false;
    /**
     * @see #typeDefinitionElementsPattern
     */
    private Matcher melements;
    /**
     * @see #fullColumnDefinitionFormat
     */
    private Matcher mfull;
    /**
     * @see #typeDefinitionFormat
     */
    private Matcher mtypes;
    private Set<String> options;

    public Column(Column other) {
        this();
        this.name = other.name;
        this.featurePathMap = other.featurePathMap;
    }

    public Column(String columnDefinition, String typePrefix, TypeSystem ts) throws CASException {
        this();
        parseAndAddDefinition(columnDefinition, typePrefix, ts);
    }

    public Column() {
        mfull = fullColumnDefinitionFormat.matcher("");
        mtypes = typeDefinitionFormat.matcher("");
        melements = typeDefinitionElementsPattern.matcher("");
        featurePathMap = new LinkedHashMap<>();
    }

    @Override
    public String toString() {
        return "Column [name=" + name + ", featurePathMap=" + featurePathMap + "]";
    }

    public String getName() {
        return name;
    }

    public void parseAndAddDefinition(String columnDefinition, String typePrefix, TypeSystem ts) throws CASException {
        if (!mfull.reset(columnDefinition).matches()) {
            throw new IllegalArgumentException(
                    "The line does not obey the column definition syntax: " + columnDefinition);
        }
        name = columnDefinition.split(":", 2)[0];
        mtypes.reset(columnDefinition);
        // find the type=/path expressions
        while (mtypes.find()) {
            String group = mtypes.group();
            melements.reset(group);
            List<String> elements = new ArrayList<>();
            while (melements.find())
                elements.add(melements.group());
            if (elements.size() > 1) {
                for (int i = 0; i < elements.size(); ++i) {
                    String element = elements.get(i);
                    if (i < elements.size() - 1) {
                        String typeName = element.trim();
                        Type type = EntityEvaluatorConsumer.findType(typeName, typePrefix, ts);
                        JCoReFeaturePath fp = new JCoReFeaturePath();
                        final String optionsAndFp = elements.get(elements.size() - 1).trim();
                        final String[] optionSplit = optionsAndFp.split("[\\[]]");
                        if (optionSplit.length > 1) {
                            options = Arrays.stream(optionSplit[1].split("\\s*,\\s*")).collect(Collectors.toSet());
                        }
                        String fpDefinition = optionSplit[optionSplit.length - 1];
                        fp.initialize(fpDefinition);
                        featurePathMap.put(type, fp);
                    }
                }
            } else {
                globalFeaturePath = new JCoReFeaturePath();
                globalFeaturePath.initialize(elements.get(0));
            }
        }
    }

    public Set<Type> getTypes() {
        return featurePathMap.keySet();
    }

    public Type getSingleType() {
        if (featurePathMap.size() > 1)
            throw new IllegalStateException("The column " + name + " has more than one type");
        return featurePathMap.keySet().stream().findFirst().get();
    }

    public Deque<String> getValue(TOP a) {
        Object value = null;
        JCoReFeaturePath fp = getMostSpecificApplicableFeaturePath(a.getType(), a.getCAS().getTypeSystem());
        if (fp != null) {
            value = fp.getValue(a, 0);
        } else if (globalFeaturePath != null) {
            value = globalFeaturePath.getValue(a, 0);
        }
        Deque<String> ret;
        if (value != null) {
            if (List.class.isAssignableFrom(value.getClass())) {
                isMultiValued = true;
                List<?> l = (List<?>) value;
                ret = new ArrayDeque<>(l.size());
                for (Object o : l)
                    ret.add(String.valueOf(o));
            } else {
                ret = new ArrayDeque<>();
                ret.add(String.valueOf(value));
            }
        } else {
            ret = new ArrayDeque<>(0);
        }
        return ret;
    }

    private JCoReFeaturePath getMostSpecificApplicableFeaturePath(Type type, TypeSystem ts) {
        Type ret = type;
        while (featurePathMap.get(ret) == null && ret != null) {
            ret = ts.getParent(ret);
        }
        if (featurePathMap.containsKey(ret)) {
            featurePathMap.put(type, featurePathMap.get(ret));
        }
        return featurePathMap.get(type);
    }

    public void reset() {
    }
}
