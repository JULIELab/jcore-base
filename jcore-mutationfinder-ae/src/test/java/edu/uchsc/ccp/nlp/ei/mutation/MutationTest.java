package edu.uchsc.ccp.nlp.ei.mutation;

import junit.framework.TestCase;

/*
 * Copyright (c) 2007 Regents of the University of Colorado
 * Please refer to the licensing agreement at MUTATIONFINDER_HOME/doc/license.txt
 */

public class MutationTest extends TestCase {

    /**
     * Test the the constructor works for input of both int's and String's
     * 
     * @throws Exception
     */
    public void testConstructor() throws Exception {
        Mutation m = new Mutation(42);
        assertEquals(42, m.getPosition());
        m = new Mutation("43");
        assertEquals(43, m.getPosition());
        try {
            m = new Mutation("not at mutation");
            fail("Did not throw a MutationException...");
        } catch (MutationException me) {
            // do nothing
        }
    }

    /**
     * Test that direct calls to the unsupported methods in Mutation result in thrown Exceptions
     * 
     * @throws Exception
     */
    public void testUnsupportedMethods() throws Exception {
        Mutation m = new Mutation(42);
        try {
            m.equals(m);
            fail("Direct call of equals() did not throw an exception.");
        } catch (UnsupportedOperationException uoe) {
            // do nothing
        }

        try {
            m.hashCode();
            fail("Direct call of hashcode() did not throw an exception.");
        } catch (UnsupportedOperationException uoe) {
            // do nothing
        }

        try {
            m.toString();
            fail("Direct call of toString() did not throw an exception.");
        } catch (UnsupportedOperationException uoe) {
            // do nothing
        }

    }

}
