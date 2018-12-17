/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.jsbd;

import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class Abstract2UnitPipeTest {

    protected static Pipe pipe;

    @Before
    public void init() {
        pipe = new Abstract2UnitPipe(false);
    }

    @Test
    public void testPipeInnerEosToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "q.e.d"
        list.add("q.e.d.");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token token = (Token) tokens.get(0);

        assertSimilar(token.getFeatureValue("endwithEOSSymb=."), 1.0);
        assertSimilar(token.getFeatureValue("hasinnerEOSSymb=2"), 1.0);
        assertSimilar(token.getFeatureValue("BWC=axaxa"), 1.0);
        assertSimilar(token.getFeatureValue("SIZE2"), 1.0);
    }

    @Test
    public void testSplitAtPunctuation() {
        pipe = new Abstract2UnitPipe(true);
        List<String> list = new ArrayList<>();
        list.add("sentenceEnd.SentenceBegin");
        Instance inst = pipe.pipe(new Instance(list, null, null, null));

        TokenSequence tokens = (TokenSequence) inst.getData();
        assertThat(tokens).hasSize(2);
        Token token = tokens.get(0);
        assertThat(token.getText()).isEqualTo("sentenceEnd.");
        assertSimilar(token.getFeatureValue("istokeninternal="), 1.0);
        token = tokens.get(1);
        assertThat(token.getText()).isEqualTo("SentenceBegin");
        assertSimilar(token.getFeatureValue("istokeninternal="), 0.0);
        pipe = new Abstract2UnitPipe(false);
    }

    @Test
    public void testPipeLowercaseToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "a"
        list.add("This is a TEST.");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token third = (Token) tokens.get(2);

        assertSimilar(third.getFeatureValue("TOKEN=a"), 1.0);
        assertSimilar(third.getFeatureValue("BWC=a"), 1.0);
        assertSimilar(third.getFeatureValue("SIZE1"), 1.0);
    }

    @Test
    public void testPipeEosCapsToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "TEST."
        list.add("This is a TEST.");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token fourth = (Token) tokens.get(3);

        assertSimilar(fourth.getFeatureValue("INITCAPS"), 1.0);
        //Word class is computed on the plain unit (w/o EOS symbol)
        assertSimilar(fourth.getFeatureValue("BWC=A"), 1.0);
        assertSimilar(fourth.getFeatureValue("SIZE2"), 1.0);
    }

    @Test
    public void testPipeAllCapsToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "SPARTA"
        list.add("This is SPARTA !!!");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token third = (Token) tokens.get(2);

        assertSimilar(third.getFeatureValue("ALLCAPS"), 1.0);
        //Word class is computed on the plain unit (w/o EOS symbol)
        assertSimilar(third.getFeatureValue("BWC=A"), 1.0);
        assertSimilar(third.getFeatureValue("SIZE2"), 1.0);
    }

    @Test
    public void testPipeCapsToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "A"
        list.add("A 99.9% chance.");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token first = (Token) tokens.get(0);

        assertSimilar(first.getFeatureValue("ONECAPS"), 1.0);
        assertSimilar(first.getFeatureValue("ALLCAPS"), 1.0);
        assertSimilar(first.getFeatureValue("INITCAPS"), 1.0);
        assertSimilar(first.getFeatureValue("BWC=A"), 1.0);
        assertSimilar(first.getFeatureValue("SIZE1"), 1.0);
    }

    @Test
    public void testPipeHasDigitsToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "99.9%"
        list.add("A 99.9% chance.");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token second = (Token) tokens.get(1);

        assertSimilar(second.getFeatureValue("HASDIGITS"), 1.0);
        assertSimilar(second.getFeatureValue("hasinnerEOSSymb=1"), 1.0);
        assertSimilar(second.getFeatureValue("BWC=0x0x"), 1.0);
        assertSimilar(second.getFeatureValue("SIZE2"), 1.0);
    }

    @Test
    public void testPipeHSize3Token() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "chance."
        list.add("A 99.9% chance.");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token third = (Token) tokens.get(2);

        assertSimilar(third.getFeatureValue("endwithEOSSymb=."), 1.0);
        assertSimilar(third.getFeatureValue("BWC=a"), 1.0);
        assertSimilar(third.getFeatureValue("SIZE3"), 1.0);
    }

    @Test
    public void testUnicodeToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        //Testing "(Senior-Løken"
        list.add("Nephronophthisis (NPH) and RP (Senior-Løken syndrome).");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();

        Token token = (Token) tokens.get(4);

        assertSimilar(token.getFeatureValue("BWC=xAaxAa"), 1.0);
        assertSimilar(token.getFeatureValue("BEGINBRACKETS"), 1.0);
        assertSimilar(token.getFeatureValue("SIZE3"), 1.0);
    }

    @Test
    public void testAlphanumericToken() {
        ArrayList<String> list = new ArrayList<String>(1);
        list.add("NH3");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token token = (Token) tokens.get(0);
        assertSimilar(token.getFeatureValue("ALPHANUMERIC"), 1.0);
    }

    @Test
    public void testInitCapsLoken() {
        ArrayList<String> list = new ArrayList<String>(1);
        list.add("Ährengold");
        Instance inst = pipe.pipe(new Instance(list, "", "", ""));

        TokenSequence tokens = (TokenSequence) inst.getData();
        Token token = (Token) tokens.get(0);
        assertSimilar(token.getFeatureValue("INITCAPS"), 1.0);
    }


    private void assertSimilar(double featureValue, double d) {
        assertThat(featureValue).isCloseTo(d, Offset.offset(Math.sqrt(Double.MIN_NORMAL)));
    }

}
