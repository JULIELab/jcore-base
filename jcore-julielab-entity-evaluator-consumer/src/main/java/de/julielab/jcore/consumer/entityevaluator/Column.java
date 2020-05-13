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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

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
     * <p>
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
     */
    private static final Pattern typeDefinitionElementsPattern = Pattern.compile("([^,=]+)");
    /**
     * Finds options in type definitions:<br>
     * EmbeddingVector=<b>[binary,gzip]</b>/vector
     */
    private static final Pattern typeDefinitionOptionsPattern = Pattern.compile("\\[([^\\[]+)]");
    protected String name;
    protected Map<Type, JCoReFeaturePath> featurePathMap;
    /**
     * Possible options:
     * <ul>
     *     <li>concat</li>
     *     <li>binary</li>
     *     <li>gzip</li>
     *     <li>base64</li>
     * </ul>
     */
    protected Map<Type, Set<String>> optionsMap;
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
    /**
     * @see #typeDefinitionOptionsPattern
     */
    private Matcher mOptions;

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
        mOptions = typeDefinitionOptionsPattern.matcher("");
        featurePathMap = new LinkedHashMap<>();
        optionsMap = new HashMap<>();
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
            Set<String> options = null;
            mOptions.reset(group);
            if (mOptions.find()) {
                options = Arrays.stream(mOptions.group(1).split("\\s*,\\s*")).collect(Collectors.toSet());
                group = group.substring(0, mOptions.start()) + group.substring(mOptions.end());
            }
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
                        String fpDefinition = optionSplit[optionSplit.length - 1];
                        fp.initialize(fpDefinition);
                        featurePathMap.put(type, fp);
                        if (options != null) {
                            optionsMap.put(type, options);
                        }
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

    public Deque<String> getValue(TOP a, JCas aJCas) {
        if (a == null) {
            Deque<String> emptyVal = new ArrayDeque<>();
            emptyVal.add("");
            return emptyVal;
        }
        Object value = null;
        JCoReFeaturePath fp = getMostSpecificApplicableFeaturePath(a.getType(), a.getCAS().getTypeSystem());
        if (fp != null) {
            value = fp.getValue(a, 0);
        } else if (globalFeaturePath != null) {
            value = globalFeaturePath.getValue(a, 0);
        }
        Deque<String> ret;
        if (value != null) {
            if (List.class.isAssignableFrom(value.getClass()) && !((List<?>) value).isEmpty()) {
                List<?> l = (List<?>) value;
                Set<String> options = optionsMap.getOrDefault(a.getType(), Collections.emptySet());
                if (!options.contains("concat")) {
                    isMultiValued = true;
                    ret = new ArrayDeque<>(l.size());
                    for (Object o : l) {
                        if (options.contains("gzip")) {
                            byte[] bytes = String.valueOf(o).getBytes(UTF_8);
                            final String resultString = handleEncoding(bytes, options);
                            ret.add(resultString);
                        } else {
                            ret.add(String.valueOf(o));
                        }
                    }
                } else {
                    String resultString;
                    if (!options.contains("binary")) {
                        resultString = l.stream().map(String::valueOf).collect(Collectors.joining(","));
                    } else {
                        ByteBuffer theBuf = null;
                        final Object firstElement = l.get(0);
                        Consumer<Object> bufferWriter = null;
                        byte[] bytes = null;
                        if (firstElement instanceof Double) {
                            ByteBuffer bb = ByteBuffer.allocate(Double.BYTES * l.size());
                            bufferWriter = o -> bb.putDouble((Double) o);
                            theBuf = bb;
                        } else if (firstElement instanceof Float) {
                            ByteBuffer bb = ByteBuffer.allocate(Float.BYTES * l.size());
                            bufferWriter = o -> bb.putFloat((Float) o);
                            theBuf = bb;
                        } else if (firstElement instanceof Integer) {
                            ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * l.size());
                            bufferWriter = o -> bb.putInt((Integer) o);
                            theBuf = bb;
                        } else if (firstElement instanceof Long) {
                            ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * l.size());
                            bufferWriter = o -> bb.putLong((Long) o);
                            theBuf = bb;
                        } else if (firstElement instanceof Byte) {
                            ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES * l.size());
                            bufferWriter = o -> bb.put((Byte) o);
                            theBuf = bb;
                        } else if (firstElement instanceof Short) {
                            ByteBuffer bb = ByteBuffer.allocate(Short.BYTES * l.size());
                            bufferWriter = o -> bb.putShort((Short) o);
                            theBuf = bb;
                        } else if (firstElement instanceof String) {
                            bytes = l.stream().map(String.class::cast).collect(Collectors.joining(",")).getBytes(UTF_8);
                        } else {
                            throw new IllegalArgumentException("Unsupported array element type " + firstElement.getClass());
                        }
                        if (theBuf != null) {
                            for (Object o : l)
                                bufferWriter.accept(o);
                            bytes = theBuf.array();
                        }
                        resultString = handleEncoding(bytes, options);
                    }
                    ret = new ArrayDeque<>(1);
                    ret.add(resultString);
                }
            } else {
                ret = new ArrayDeque<>();
                ret.add(String.valueOf(value));
            }
        } else {
            ret = new ArrayDeque<>(0);
        }
        return ret;
    }

    private String handleEncoding(byte[] bytes, Set<String> options) {
        String resultString = null;
        if (options.contains("gzip")) {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPOutputStream gzout = new GZIPOutputStream(baos)) {
                gzout.write(bytes);
                gzout.close();
                bytes = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Default encoding is base64
        if (options.contains("base64") || (!options.contains("base64") && !options.contains("hex"))) {
            resultString = Base64.encodeBase64String(bytes);
        } else if (options.contains("hex")) {
            resultString = Hex.encodeHexString(bytes);
        }
        return resultString;
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
