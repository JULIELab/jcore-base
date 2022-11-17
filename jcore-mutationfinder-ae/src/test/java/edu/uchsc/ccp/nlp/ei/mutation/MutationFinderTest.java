package edu.uchsc.ccp.nlp.ei.mutation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Copyright (c) 2007 Regents of the University of Colorado
 * Please refer to the licensing agreement at MUTATIONFINDER_HOME/doc/license.txt
 */

public class MutationFinderTest  {

    private static List<String> regularExpressions;

    private static MutationFinder mf;

    @BeforeAll
    protected static void setUp() {
        /* The first four default regular expressions */
        regularExpressions = new ArrayList<String>();
        regularExpressions
                .add("(^|[\\s\\(\\[\\'\"/,\\-])(?P<wt_res>[CISQMNPKDTFAGHLRWVEY])(?P<pos>[1-9][0-9]+)(?P<mut_res>[CISQMNPKDTFAGHLRWVEY])(?=([.,\\s)\\]\\'\":;\\-?!/]|$))[CASE_SENSITIVE]");

        regularExpressions
                .add("(^|[\\s\\(\\[\\'\"/,\\-])(?P<wt_res>(CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?P<pos>[1-9][0-9]*)(?P<mut_res>(CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?=([.,\\s)\\]\\'\":;\\-?!/]|$))");
        regularExpressions
                .add("(^|[\\s\\(\\[\\'\"/,\\-])(?P<wt_res>(CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?P<pos>[1-9][0-9]*)-->(?P<mut_res>(CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?=([.,\\s)\\]\\'\":;\\-?!/]|$))");
        regularExpressions
                .add("(^|[\\s\\(\\[\\'\"/,\\-])(?P<wt_res>(CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?P<pos>[1-9][0-9]*) to (?P<mut_res>(CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?=([.,\\s)\\]\\'\":;\\-?!/]|$))");

        mf = new MutationFinder(new HashSet<String>(regularExpressions));
    }

    /**
     * Test that the constructors returns without error
     * 
     * @throws Exception
     */
    @Test
    public void testConstructor() throws Exception {
        mf = new MutationFinder(new HashSet<String>());
        mf = new MutationFinder(new HashSet<String>(regularExpressions));
        
        String testRegexFile = "src/test/resources/test_regex.txt";
        mf = new MutationFinder(testRegexFile);
        
        File regexFile = new File(testRegexFile);
        mf = new MutationFinder(regexFile);

        final FileInputStream is = new FileInputStream(testRegexFile);
        mf = new MutationFinder(is);
    }


    /**
     * Test that the ?P<wt_res>,?P<mut_res>, and ?P<pos> tags are mapped to the correct parenthetical grouping so that they can be retrieved after
     * the regex matches *
     * 
     * @throws Exception
     */
    @Test
    public void testExtractMappingsFromPythonRegex() throws Exception {
        Map<String, Integer> groupMappings = MutationFinder.extractMappingsFromPythonRegex(regularExpressions.get(0));
        assertEquals(new Integer(2), groupMappings.get(MutationFinder.WT_RES));
        assertEquals(new Integer(3), groupMappings.get(MutationFinder.POS));
        assertEquals(new Integer(4), groupMappings.get(MutationFinder.MUT_RES));

        groupMappings = MutationFinder.extractMappingsFromPythonRegex(regularExpressions.get(1));
        assertEquals(new Integer(2), groupMappings.get(MutationFinder.WT_RES));
        assertEquals(new Integer(5), groupMappings.get(MutationFinder.POS));
        assertEquals(new Integer(6), groupMappings.get(MutationFinder.MUT_RES));

    }

    /**
     * Test the removal of the ?P<wt_res>,?P<mut_res>, and ?P<pos> tags from the python regex strings
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveTagsFromPythonRegex() throws Exception {
        String regex0WithoutTags = "(^|[\\s\\(\\[\\'\"/,\\-])([CISQMNPKDTFAGHLRWVEY])([1-9][0-9]+)([CISQMNPKDTFAGHLRWVEY])(?=([.,\\s)\\]\\'\":;\\-?!/]|$))[CASE_SENSITIVE]";
        assertEquals(regex0WithoutTags, MutationFinder.removeTagsFromPythonRegex(regularExpressions.get(0)));

        String regex3WithoutTags = "(^|[\\s\\(\\[\\'\"/,\\-])((CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))([1-9][0-9]*) to ((CYS|ILE|SER|GLN|MET|ASN|PRO|LYS|ASP|THR|PHE|ALA|GLY|HIS|LEU|ARG|TRP|VAL|GLU|TYR)|(GLUTAMINE|GLUTAMIC ACID|LEUCINE|VALINE|ISOLEUCINE|LYSINE|ALANINE|GLYCINE|ASPARTATE|METHIONINE|THREONINE|HISTIDINE|ASPARTIC ACID|ARGININE|ASPARAGINE|TRYPTOPHAN|PROLINE|PHENYLALANINE|CYSTEINE|SERINE|GLUTAMATE|TYROSINE))(?=([.,\\s)\\]\\'\":;\\-?!/]|$))";
        assertEquals(regex3WithoutTags, MutationFinder.removeTagsFromPythonRegex(regularExpressions.get(3)));
    }

    

    /**
     * Test that no mutations are returned for text that contains no mutations
     * 
     * @throws Exception
     */
    @Test
    public void testExtractionNoMutations() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("There is not mutation data here.");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("T64 is almost a vallid mutation.");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("So is 42S.");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("W42X is not a valid mutation.");
        assertEquals(0, mutations.size());
    }

    /**
     * Test extraction when one mutation is present in the input text.
     * 
     * @throws Exception
     */
    @Test
    public void testExtractSingleMutation() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("S42T");
        Set<Mutation> expectedPMs = new HashSet<Mutation>();
        Mutation expectedPM = new PointMutation(42, "S", "T");
        expectedPMs.add(expectedPM);
        assertEquals(expectedPMs, mutations.keySet());
        int[] span = new int[2];
        span[0] = 0;
        span[1] = 4;
        Set<int[]> spanSet = new HashSet<int[]>();
        spanSet.add(span);
        Map<Mutation, Set<int[]>> expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("S42T is a mutation.");
        assertEquals(expectedPMs, mutations.keySet());
    }

    /**
     * Test extraction when multiple mutations are present in the input text
     * 
     * @throws Exception
     */
    @Test
    public void testExtractMultipleMutations() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("S42T and W36Y");
        Set<Mutation> expectedPMs = new HashSet<Mutation>();
        Mutation expectedPM = new PointMutation(42, "S", "T");
        Mutation expectedPM2 = new PointMutation(36, "W", "Y");
        expectedPMs.add(expectedPM);
        expectedPMs.add(expectedPM2);
        assertEquals(expectedPMs, mutations.keySet());
        int[] span = new int[2];
        span[0] = 0;
        span[1] = 4;
        Set<int[]> spanSet = new HashSet<int[]>();
        spanSet.add(span);
        int[] span2 = new int[2];
        span2[0] = 9;
        span2[1] = 13;
        Set<int[]> spanSet2 = new HashSet<int[]>();
        spanSet2.add(span2);
        Map<Mutation, Set<int[]>> expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        expectedMutations.put(expectedPM2, spanSet2);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("Ser42Thr and Trp36Tyr");
        assertEquals(expectedPMs, mutations.keySet());
    }

    /**
     * Test extraction when there are multiple mutations and look-ahead is required
     * 
     * @throws Exception
     */
    @Test
    public void testExtractMultipleMutationsWithPositiveLookahead() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("S42T W36Y");
        Set<Mutation> expectedPMs = new HashSet<Mutation>();
        Mutation expectedPM = new PointMutation(42, "S", "T");
        Mutation expectedPM2 = new PointMutation(36, "W", "Y");
        expectedPMs.add(expectedPM);
        expectedPMs.add(expectedPM2);
        assertEquals(expectedPMs, mutations.keySet());

        mutations = mf.extractMutations("Ser42Thr Trp36Tyr");
        assertEquals(expectedPMs, mutations.keySet());
    }

    /**
     * Test span calculations for extracted mutations
     * 
     * @throws Exception
     */
    @Test
    public void testExtractionSpanCalculations() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("S42T and W36Y");
        Mutation expectedPM = new PointMutation(42, "S", "T");
        int[] span = new int[2];
        span[0] = 0;
        span[1] = 4;
        Set<int[]> spanSet = new HashSet<int[]>();
        spanSet.add(span);
        Mutation expectedPM2 = new PointMutation(36, "W", "Y");
        int[] span2 = new int[2];
        span2[0] = 9;
        span2[1] = 13;
        Set<int[]> spanSet2 = new HashSet<int[]>();
        spanSet2.add(span2);
        Map<Mutation, Set<int[]>> expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        expectedMutations.put(expectedPM2, spanSet2);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("S42T, W36Y, and W36Y");
        span2 = new int[2];
        span2[0] = 6;
        span2[1] = 10;
        int[] span3 = new int[2];
        span3[0] = 16;
        span3[1] = 20;
        spanSet2 = new HashSet<int[]>();
        spanSet2.add(span2);
        spanSet2.add(span3);
        expectedMutations.remove(expectedPM2);
        expectedMutations.put(expectedPM2, spanSet2);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("S42T, W36Y, Trp36Tyr, and W36Y");
        span2 = new int[2];
        span2[0] = 6;
        span2[1] = 10;
        span3 = new int[2];
        span3[0] = 12;
        span3[1] = 20;
        int[] span4 = new int[2];
        span4[0] = 26;
        span4[1] = 30;
        spanSet2 = new HashSet<int[]>();
        spanSet2.add(span2);
        spanSet2.add(span3);
        spanSet2.add(span4);
        expectedMutations.remove(expectedPM2);
        expectedMutations.put(expectedPM2, spanSet2);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));
    }

    /**
     * Test extraction on various commonly known mutation formats
     * 
     * @throws Exception
     */
    @Test
    public void testExtractionOfVariousFormats() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("The A42G mutation was made.");
        Mutation expectedPM = new PointMutation(42, "A", "G");
        int[] span = new int[2];
        span[0] = 4;
        span[1] = 8;
        Set<int[]> spanSet = new HashSet<int[]>();
        spanSet.add(span);
        Map<Mutation, Set<int[]>> expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("The Ala42-->Gly mutation was made.");
        span = new int[2];
        span[0] = 4;
        span[1] = 15;
        spanSet = new HashSet<int[]>();
        spanSet.add(span);
        expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("The Ala42Gly mutation was made.");
        span = new int[2];
        span[0] = 4;
        span[1] = 12;
        spanSet = new HashSet<int[]>();
        spanSet.add(span);
        expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));

        mutations = mf.extractMutations("The Ala42 to Glycine mutation was made.");
        span = new int[2];
        span[0] = 4;
        span[1] = 20;
        spanSet = new HashSet<int[]>();
        spanSet.add(span);
        expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));
    }

    /**
     * Test that the case-sensitive flag works
     * 
     * @throws Exception
     */
    @Test
    public void testRegexCaseInsensitiveFlag() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("a64t");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("A64t");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("a64T");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("A64T");
        Mutation expectedPM = new PointMutation(64, "A", "T");
        int[] span = new int[2];
        span[0] = 0;
        span[1] = 4;
        Set<int[]> spanSet = new HashSet<int[]>();
        spanSet.add(span);
        Map<Mutation, Set<int[]>> expectedMutations = new HashMap<Mutation, Set<int[]>>();
        expectedMutations.put(expectedPM, spanSet);
        assertEquals(mutationMapConverter(expectedMutations), mutationMapConverter(mutations));
    }

    /**
     * Test extraction for case-insensitive matches
     * 
     * @throws Exception
     */
    @Test
    public void testCaseInsensitiveCases() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("ala64gly");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("Ala64gly");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("aLa64gLy");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("ALA64GLY");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("Ala64Gly");
        assertEquals(1, mutations.size());
    }

    /**
     * Test post-processing step, specifically the removal of mutations with positions < 10, and that mutations with the same residue as wt and mut
     * are not kept
     * 
     * @throws Exception
     */
    @Test
    public void testPostProcessing() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("A64G");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("H2A");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("E2F");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("W460W");
        assertEquals(0, mutations.size());

    }

    /**
     * Test extraction on mutations is varying numbers of digits
     * 
     * @throws Exception
     */
    @Test
    public void testVariedDigitLength() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("ala64gly");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("Ala6444444gly");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("A6437588G");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("ALA64567856GLY");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("Ala10Gly");
        assertEquals(1, mutations.size());
    }

    /**
     * The regexes should disallow unacceptable word boundaries
     * 
     * @throws Exception
     */
    @Test
    public void testUnacceptableGeneralWordBoundaries() throws Exception {
        String startCharacters = "abcdefghijklmnopqrstuvwxyz0123456789~@#$%^&*_+=])";
        String endCharacters = "abcdefghijklmnopqrstuvwxyz0123456789~@#$%^&*_+=(['";
        String[] mutationTexts = { "A64G", "Ala64Gly", "Ala64-->Gly" };

        for (String mutationText : mutationTexts) {
            for (char start : startCharacters.toCharArray()) {
                for (char end : endCharacters.toCharArray()) {
                    Map<Mutation, Set<int[]>> mutations = mf.extractMutations(start + mutationText + end);
                    assertEquals(0, mutations.size());
                }
            }
        }
    }

     /**
         * The regexes should allow these word valueboundaries
         * 
         * @throws Exception
         */
     @Test
    public void testAcceptableGeneralWordBoundaries() throws Exception {
        char[] endCharacters = { '.', ',', ' ', '\t', '\n', ')', ']', '"', '\'', ':', ';', '?', '!', '/', '-' };
        char[] startCharacters = { ' ', '\t', '\n', '"', '\'', '(', '[', '/', ',', '-' };
        String[] mutationTexts = { "A64G", "Ala64Gly", "Ala64-->Gly" };

        for (String mutationText : mutationTexts) {
            for (char start : startCharacters) {
                for (char end : endCharacters) {
                    String testStr = (start + mutationText + end);
                    Map<Mutation, Set<int[]>> mutations = mf.extractMutations(testStr);
                     assertEquals(1, mutations.size());
                }
            }
        }
    }

    /**
     * No mutations should be returned for Strings with mixed one and three-letter abbreviations
     * 
     * @throws Exception
     */
    @Test
    public void testMixOneAndThreeLetterStrings() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("A64Gly");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("Ala64G");
        assertEquals(0, mutations.size());
    }

    /**
     * Test that MF identifies full-name mentions of amino acids
     * 
     * @throws Exception
     */
    @Test
    public void testFullNameMethods() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("alanine64-->Gly");
        assertEquals(1, mutations.size());

        mutations = mf.extractMutations("Ala64-->glycine");
        assertEquals(1, mutations.size());
    }

    /**
     * Test that Strings with single letter abbreviations fail unless in the wNm format
     * 
     * @throws Exception
     */
    @Test
    public void testOneLetterAbbreviationFailsNon_wNmFormat() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("A64-->glycine");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("A64Gly");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("Ala64-->G");
        assertEquals(0, mutations.size());
    }

    /**
     * Test that MF finds things like "Ala64 to Gly"
     * 
     * @throws Exception
     */
    @Test
    public void testTextBasedMatches() throws Exception {
        String[] mutationTexts = { "Ala64 to Gly", "Alanine64 to Glycine", "Ala64 to Glycine", "alanine64 to Gly",
                "The Ala64 to Gly substitution", "The Ala64 to glycine substitution", "The Ala64 to Gly substitution" };

        Set<Mutation> expectedPMs = new HashSet<Mutation>();
        Mutation expectedPM = new PointMutation(64, "A", "G");
        expectedPMs.add(expectedPM);

        for (String mutationText : mutationTexts) {
            Map<Mutation, Set<int[]>> mutations = mf.extractMutations(mutationText);
            assertEquals(expectedPMs, mutations.keySet());
        }
    }

    /**
     * Ensure proper spacing for matched mutations
     * 
     * @throws Exception
     */
    @Test
    public void testTextMatchSpacing() throws Exception {
        Map<Mutation, Set<int[]>> mutations = mf.extractMutations("TheAla40toGlymutation");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("arg40tomet");
        assertEquals(0, mutations.size());

        mutations = mf.extractMutations("ala25tohis");
        assertEquals(0, mutations.size());
    }

    /* This method enables the maps to be compared directly. Comparing int[] arrays does not seem to work. */
    private Map<Mutation, Set<String>> mutationMapConverter(Map<Mutation, Set<int[]>> mutationMap) {
        Map<Mutation, Set<String>> mutation2stringMap = new HashMap<Mutation, Set<String>>();

        for (Mutation key : mutationMap.keySet()) {
            Set<int[]> spans = mutationMap.get(key);
            Set<String> spanStrings = new HashSet<String>();
            for (int[] span : spans) {
                String spanStr = "[" + span[0] + ".." + span[1] + "]";
                spanStrings.add(spanStr);
            }
            mutation2stringMap.put(key, spanStrings);
        }
        return mutation2stringMap;
    }

}
