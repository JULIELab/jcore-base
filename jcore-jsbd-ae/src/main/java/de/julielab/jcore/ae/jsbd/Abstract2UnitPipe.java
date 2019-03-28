/**
 * Abstract2UnitPipe.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: tomanek
 * <p>
 * Current version: 2.0
 * Since version:   1.0
 * <p>
 * Creation date: Aug 01, 2006
 * <p>
 * The base pipe used converting an abstract into a sequence of Unit objects.
 **/

package de.julielab.jcore.ae.jsbd;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Abstract2UnitPipe extends Pipe {

    private final static Logger log = LoggerFactory.getLogger(Abstract2UnitPipe.class);

    private static final long serialVersionUID = 1L;

    private static final Pattern splitPattern = Pattern.compile("[^\\s]+");
    private static final Pattern punctuationPattern = Pattern.compile("\\p{P}");

    TreeSet<String> eosSymbols;

    TreeSet<String> abbrList;
    private boolean splitAfterPunctuation;

    Abstract2UnitPipe(boolean splitAfterPunctuation) {
        super(new Alphabet(), new LabelAlphabet());
        this.splitAfterPunctuation = splitAfterPunctuation;

        // initialize the list of end-of-sentence symbols and abbreviations
        eosSymbols = new EOSSymbols().getSymbols();
        abbrList = (new AbbreviationsMedical()).getSet();
    }

    /**
     * This pipe gets an Instance object, where the variable source is assumed to be the abstract
     * filename, data is the abstract file read in.
     *
     * Unit objects are created for this abstract (see MedInfo 2007 paper). For each Unit a label is
     * predicted: "IS" means "inside sentence", "EOS" means "end-of-sentence", i.e., such a Unit is
     * at the end of a sentence.
     */
    public Instance pipe(Instance carrier) {

        String abstractFileName = (String) carrier.getSource();
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) carrier.getData();
        Map<String, Integer> unitFreq = getUnitFrequency(lines);

        // the features for each token
        TokenSequence data = new TokenSequence();

        // the labels (IS/EOS) for each token of the text
        LabelSequence target = new LabelSequence((LabelAlphabet) getTargetAlphabet());

        List<Unit> unitInfo = new ArrayList<>();

        // now go through lines and add a Token object for each token
        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);
            if (line.isEmpty()) {
                // ignore empty lines
                continue;
            }

            List<Unit> units = getUnits(line);

            if (units.isEmpty())
                continue;

            for (int j = 0; j < units.size(); j++) {

                String currUnitRep = units.get(j).rep;
                String plainUnitRep = getPlainUnit(currUnitRep);// getPlainToken(curr_token);
                String label = "IS";
                Token token = new Token(currUnitRep);

                // --- add features here ---

                // check if this unit is embedded within a token, i.e. there is no whitespace after this unit
                if (units.get(j).isTokenInternal)
                    token.setFeatureValue("istokeninternal=", 1);

                // on EOSSymbols
                if (containsEOSSymbol(currUnitRep))
                    token.setFeatureValue("endwithEOSSymb=" + getEOSSymbol(currUnitRep), 1);

                if ((j + 1) == units.size()) {
                    label = "EOS";
                }

                int count = nrEOSSymbolsContained(plainUnitRep);
                if (count > 0)
                    token.setFeatureValue("hasinnerEOSSymb=" + count, 1);

                // the token itself
                token.setFeatureValue("TOKEN=" + currUnitRep, 1);

                // some regexp features
                if (currUnitRep.matches("[\\p{Lu}\\p{M}].*"))
                    token.setFeatureValue("INITCAPS", 1);

                if (currUnitRep.matches("[\\p{Lu}\\p{M}]"))
                    token.setFeatureValue("ONECAPS", 1);

                if (currUnitRep.matches("[\\p{Lu}\\p{M}]+"))
                    token.setFeatureValue("ALLCAPS", 1);

                if (currUnitRep.matches("(.*[\\p{L}\\p{M}].*[0-9].*|.*[0-9].*[\\p{L}\\p{M}].*)"))
                    token.setFeatureValue("ALPHANUMERIC", 1);

                if (currUnitRep.matches("[IVXDLCM]+"))
                    token.setFeatureValue("ROMAN", 1);

                if (currUnitRep.matches(".*\\b[IVXDLCM]+\\b.*"))
                    token.setFeatureValue("HASROMAN", 1);

                if (currUnitRep.matches("[0-9]+"))
                    token.setFeatureValue("NATURALNUMBER", 1);

                if (currUnitRep.matches("[-0-9]+[.,]+[0-9.,]+"))
                    token.setFeatureValue("REALNUMBER", 1);

                if (currUnitRep.matches(".*[0-9]+.*"))
                    token.setFeatureValue("HASDIGITS", 1);

                if (currUnitRep.matches("(\\(.*|\\[.*)"))
                    token.setFeatureValue("BEGINBRACKETS", 1);

                if (currUnitRep.matches("(\\(.*\\)|\\[.*\\])"))
                    token.setFeatureValue("INSIDEBRACKETS", 1);

                if (currUnitRep.matches("(\".*|'.*)"))
                    token.setFeatureValue("BEGINQUOTES", 1);

                if (currUnitRep.matches("(\".*\"|'.*')"))
                    token.setFeatureValue("INSIDEBQUOTES", 1);

                // length of the token
                if (currUnitRep.length() <= 3)
                    token.setFeatureValue("SIZE1", 1);
                else if (currUnitRep.length() <= 6)
                    token.setFeatureValue("SIZE2", 1);
                else
                    token.setFeatureValue("SIZE3", 1);

                // abbreviation classes
                if (currUnitRep.matches("[A-Z]\\."))
                    token.setFeatureValue("ABBR1", 1);

                if (currUnitRep.matches("([A-Za-z]\\.)+"))
                    token.setFeatureValue("ABBR2", 1);

                if (currUnitRep.matches("[abcdfghjklmnpqrstvwxyz]+\\."))
                    token.setFeatureValue("ABBR3", 1);

                // word class
                String bwc = plainUnitRep;

                bwc = bwc.replaceAll("[\\p{Lu}\\p{M}]+", "A");
                bwc = bwc.replaceAll("[\\p{Ll}\\p{M}]+", "a");
                bwc = bwc.replaceAll("[0-9]+", "0");
                bwc = bwc.replaceAll("[^\\p{L}\\p{M}0-9]+", "x");

                token.setFeatureValue("BWC=" + bwc, 1);

                // check whether token with EOSsymbol occurs more than once in
                // abstract
                if (containsEOSSymbol(currUnitRep)) {
                    int freq = ((Integer) unitFreq.get(currUnitRep)).intValue();
                    if (freq > 1)
                        token.setFeatureValue("FreqTokenEOSSymbol", 1);
                }

                // abbreviation
                if (abbrList.contains(currUnitRep))
                    token.setFeatureValue("KNOWNABBR", 1);

                // --- add all to the instance ---
                data.add(token);
                target.add(label);
            }
            unitInfo.addAll(units);
        }

        carrier.setData(data); // the features per token
        carrier.setTarget(target); // the labels per token

        carrier.setName(unitInfo); // the units of the abstract
        carrier.setSource(abstractFileName); // the filename of the piece of text to be split

        return carrier;
    }

    /**
     * counts the number of EOS symbols contained in the token
     *
     * @param token
     * @return int
     */
    private int nrEOSSymbolsContained(String token) {
        int count = 0;
        char[] c = token.toCharArray();
        for (int i = 0; i < c.length; i++) {
            char[] cc = {c[i]};
            if (eosSymbols.contains(new String(cc)))
                count++;
        }
        return count;
    }

    /**
     * checks whether the token ends with a EOSSymbol
     *
     * @param token
     * @return true if containes EOS symbol
     */
    private boolean containsEOSSymbol(String token) {
        if (token.length() > 0) {
            String lastChar = token.substring(token.length() - 1, token.length());
            if (eosSymbols.contains(lastChar))
                return true;
        }
        return false;
    }

    /**
     * returns the last char of a token, if this char is a EOSSymbol. Otherwise an empty string is
     * returned.
     *
     * @param token
     * @return
     */
    private String getEOSSymbol(String token) {
        if (token.length() > 0) {
            String lastChar = token.substring(token.length() - 1, token.length());
            if (eosSymbols.contains(lastChar))
                return lastChar;
        }
        return "";
    }

    /**
     * remove the EOSSymbol from the string token representation. If token does not end with
     * EOSsymbol, the original token is returned.
     *
     * @return
     */
    private String getPlainUnit(String unitRep) {
        if (containsEOSSymbol(unitRep))
            return unitRep.substring(0, unitRep.length() - 1);
        else
            return unitRep;
    }

    /**
     * get the frequency of occurrence of this unit in the abstract
     *
     * @param lines
     *            the input file split into single lines
     * @return
     */
    private Map<String, Integer> getUnitFrequency(List<String> lines) {
        Map<String, Integer> freq = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            List<Unit> units = getUnits(line);

            for (int j = 0; j < units.size(); j++) {
                Unit u = units.get(j);

                int count = 0;
                if (freq.containsKey(u.rep)) {
                    count = freq.get(u.rep);
                }
                count++;
                freq.put(u.rep, count);
            }
        }
        return freq;
    }

    /**
     * returns a string array containing all units for one line this is done using a regexp matcher
     * the line is split it all whitespace characters
     *
     * @param line
     * @return
     */
    private List<Unit> getUnits(String line) {

        Matcher m = splitPattern.matcher(line);
        List<Unit> units = new ArrayList<>();

        while (m.find()) {
            String rep = m.group();
            int begin = m.start();
            int end = m.end();

            int newBegin = begin;
            if (splitAfterPunctuation) {
                Matcher punctMatcher = punctuationPattern.matcher(rep);
                while (punctMatcher.find()) {
                    String punctRep = punctMatcher.group();
                    int punctEnd = begin + punctMatcher.start();
                    punctEnd = begin + punctMatcher.end();

                    boolean isTokenInternal = punctEnd < end;
                    units.add(new Unit(begin, punctEnd, line.substring(newBegin, punctEnd), isTokenInternal));
                    newBegin = punctEnd;
                }
            }
            begin = newBegin;
            if (begin < end && begin < line.length())
                units.add(new Unit(begin, end, line.substring(begin, end), false));
        }
        return units;

    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        log.info("This sentence splitter model allows sentence splits after all punctuation: " + splitAfterPunctuation);
    }
}