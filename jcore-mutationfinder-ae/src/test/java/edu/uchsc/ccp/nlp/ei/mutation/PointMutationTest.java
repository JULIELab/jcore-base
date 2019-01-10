package edu.uchsc.ccp.nlp.ei.mutation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/*
 * Copyright (c) 2007 Regents of the University of Colorado
 * Please refer to the licensing agreement at MUTATIONFINDER_HOME/doc/license.txt
 */

public class PointMutationTest extends TestCase {
    private PointMutation pointMutation;

    private Map<String, String> aminoAcidCodeLookup;

    @Override
    protected void setUp() throws Exception {
        pointMutation = new PointMutation(42, "W", "G");

        aminoAcidCodeLookup = new HashMap<String, String>();
        aminoAcidCodeLookup.put("GLY", "G");
        aminoAcidCodeLookup.put("ALA", "A");
        aminoAcidCodeLookup.put("LEU", "L");
        aminoAcidCodeLookup.put("MET", "M");
        aminoAcidCodeLookup.put("PHE", "F");
        aminoAcidCodeLookup.put("TRP", "W");
        aminoAcidCodeLookup.put("LYS", "K");
        aminoAcidCodeLookup.put("GLN", "Q");
        aminoAcidCodeLookup.put("GLU", "E");
        aminoAcidCodeLookup.put("SER", "S");
        aminoAcidCodeLookup.put("PRO", "P");
        aminoAcidCodeLookup.put("VAL", "V");
        aminoAcidCodeLookup.put("ILE", "I");
        aminoAcidCodeLookup.put("CYS", "C");
        aminoAcidCodeLookup.put("TYR", "Y");
        aminoAcidCodeLookup.put("HIS", "H");
        aminoAcidCodeLookup.put("ARG", "R");
        aminoAcidCodeLookup.put("ASN", "N");
        aminoAcidCodeLookup.put("ASP", "D");
        aminoAcidCodeLookup.put("THR", "T");
        aminoAcidCodeLookup.put("GLYCINE", "G");
        aminoAcidCodeLookup.put("ALANINE", "A");
        aminoAcidCodeLookup.put("LEUCINE", "L");
        aminoAcidCodeLookup.put("METHIONINE", "M");
        aminoAcidCodeLookup.put("PHENYLALANINE", "F");
        aminoAcidCodeLookup.put("VALINE", "V");
        aminoAcidCodeLookup.put("ISOLEUCINE", "I");
        aminoAcidCodeLookup.put("TYROSINE", "Y");
        aminoAcidCodeLookup.put("TRYPTOPHAN", "W");
        aminoAcidCodeLookup.put("SERINE", "S");
        aminoAcidCodeLookup.put("PROLINE", "P");
        aminoAcidCodeLookup.put("THREONINE", "T");
        aminoAcidCodeLookup.put("CYSTEINE", "C");
        aminoAcidCodeLookup.put("ASPARAGINE", "N");
        aminoAcidCodeLookup.put("GLUTAMINE", "Q");
        aminoAcidCodeLookup.put("LYSINE", "K");
        aminoAcidCodeLookup.put("HISTIDINE", "H");
        aminoAcidCodeLookup.put("ARGININE", "R");
        aminoAcidCodeLookup.put("ASPARTATE", "D");
        aminoAcidCodeLookup.put("GLUTAMATE", "E");
        aminoAcidCodeLookup.put("ASPARTIC ACID", "D");
        aminoAcidCodeLookup.put("GLUTAMIC ACID", "E");
        aminoAcidCodeLookup.put("G", "G");
        aminoAcidCodeLookup.put("A", "A");
        aminoAcidCodeLookup.put("V", "V");
        aminoAcidCodeLookup.put("L", "L");
        aminoAcidCodeLookup.put("I", "I");
        aminoAcidCodeLookup.put("M", "M");
        aminoAcidCodeLookup.put("F", "F");
        aminoAcidCodeLookup.put("Y", "Y");
        aminoAcidCodeLookup.put("W", "W");
        aminoAcidCodeLookup.put("S", "S");
        aminoAcidCodeLookup.put("P", "P");
        aminoAcidCodeLookup.put("T", "T");
        aminoAcidCodeLookup.put("C", "C");
        aminoAcidCodeLookup.put("N", "N");
        aminoAcidCodeLookup.put("Q", "Q");
        aminoAcidCodeLookup.put("K", "K");
        aminoAcidCodeLookup.put("H", "H");
        aminoAcidCodeLookup.put("R", "R");
        aminoAcidCodeLookup.put("D", "D");
        aminoAcidCodeLookup.put("E", "E");

        super.setUp();
    }

    /**
     * Test the constructor, as well as the amino acid normalization procedure
     * 
     * @throws Exception
     */
    public void testConstructor() throws Exception {
        PointMutation pm = new PointMutation(42, "A", "C");
        assertEquals(42, pm.getPosition());
        assertEquals('A', pm.getWtResidue());
        assertEquals('C', pm.getMutResidue());

        pm = new PointMutation(42, "Ala", "Cys");
        assertEquals(42, pm.getPosition());
        assertEquals('A', pm.getWtResidue());
        assertEquals('C', pm.getMutResidue());

        pm = new PointMutation(42, "ALA", "CYS");
        assertEquals(42, pm.getPosition());
        assertEquals('A', pm.getWtResidue());
        assertEquals('C', pm.getMutResidue());

        pm = new PointMutation(42, "A", "Cys");
        assertEquals(42, pm.getPosition());
        assertEquals('A', pm.getWtResidue());
        assertEquals('C', pm.getMutResidue());

        pm = new PointMutation("42", "A", "C");
        assertEquals(42, pm.getPosition());
        assertEquals('A', pm.getWtResidue());
        assertEquals('C', pm.getMutResidue());

    }

    /**
     * Test that the hashcode() method functions as expected
     * 
     * @throws Exception
     */
    public void testHashcode() throws Exception {
        PointMutation pm = new PointMutation(42, "W", "G");
        assertEquals((pm.getClass().getName() + pm.toString()).hashCode(), pm.hashCode());
    }

    /**
     * Test that these invalid constructor calls raise an Exception
     * 
     * @throws Exception
     */
    public void testInvalidInit() throws Exception {
        PointMutation pm;
        try {
            pm = new PointMutation("hello", "A", "C");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pm = new PointMutation(42, "X", "C");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pm = new PointMutation(0, "A", "C");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pm = new PointMutation(-42, "A", "C");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pm = new PointMutation("42", "A", "X");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 
    }

    /**
     * Test the the equals() method performs as expected.
     * 
     * @throws Exception
     */
    public void testEquals() throws Exception {
        PointMutation pm = new PointMutation(42, "W", "G");
        assertTrue(pointMutation.equals(pm));

        pm = new PointMutation("42", "W", "G");
        assertTrue(pointMutation.equals(pm));

        pm = new PointMutation(41, "W", "G");
        assertFalse(pointMutation.equals(pm));

        pm = new PointMutation(42, "Y", "G");
        assertFalse(pointMutation.equals(pm));

        pm = new PointMutation(42, "W", "C");
        assertFalse(pointMutation.equals(pm));
    }

    /**
     * Test that the amino acid normalization is working properly
     * 
     * @throws Exception
     */
    public void testNormalizationOfResidue() throws Exception {
        Set<String> residuesToNormalize = aminoAcidCodeLookup.keySet();
        for (String residue : residuesToNormalize) {
            assertEquals(aminoAcidCodeLookup.get(residue), Character.toString(pointMutation.normalizeResidueIdentity(residue)));
        }
    }

    /**
     * Test that the procedure throws and Exception when trying to normalize an invalid residue
     * 
     * @throws Exception
     */
    public void testNormalizationOfInvalidResidue() throws Exception {
        try {
            pointMutation.normalizeResidueIdentity("");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pointMutation.normalizeResidueIdentity("X");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pointMutation.normalizeResidueIdentity("xxa1a");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pointMutation.normalizeResidueIdentity("a1axx");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pointMutation.normalizeResidueIdentity("asdasd");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 

        try {
            pointMutation.normalizeResidueIdentity("42");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        } 
    }

    /**
     * Test the static method which enables creation of a PointMutation object from a String in the wNm format
     * @throws Exception
     */
    public void testCreateNewPointMutationFrom_wNm() throws Exception {
        PointMutation pm = PointMutation.createPointMutationFrom_wNm("W42G");
        assertEquals(pointMutation, pm);
        
        try {
            pm = PointMutation.createPointMutationFrom_wNm("W42X");
            fail("This should have thrown an exception...");
        } catch (MutationException me) {
            // do nothing
        }
        
    }
}
