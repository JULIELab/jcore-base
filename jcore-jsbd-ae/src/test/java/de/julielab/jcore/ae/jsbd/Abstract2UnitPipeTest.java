/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.jsbd;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class Abstract2UnitPipeTest {

	protected static Pipe pipe;
	
	@Before
	public void init() {
		pipe = new Abstract2UnitPipe();
	}
	
	@Test
	public void testPipeInnerEosToken() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "q.e.d"
		list.add("q.e.d.");		
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token token = (Token) tokens.get(0);
		
		assertTrue(similar(token.getFeatureValue("endwithEOSSymb=."), 1.0));
		assertTrue(similar(token.getFeatureValue("hasinnerEOSSymb=2"), 1.0));
		assertTrue(similar(token.getFeatureValue("BWC=axaxa"), 1.0));
		assertTrue(similar(token.getFeatureValue("SIZE2"), 1.0));
	}
	
	@Test
	public void testPipeLowercaseToken() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "a"
		list.add("This is a TEST.");		
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token third = (Token) tokens.get(2);

		assertTrue(similar(third.getFeatureValue("TOKEN=a"), 1.0));
		assertTrue(similar(third.getFeatureValue("BWC=a"), 1.0));
		assertTrue(similar(third.getFeatureValue("SIZE1"), 1.0));
	}
	
	@Test
	public void testPipeEosCapsToken() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "TEST."
		list.add("This is a TEST.");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token fourth = (Token) tokens.get(3);

		assertTrue(Double.toString(fourth.getFeatureValue("INITCAPS")), 
				similar(fourth.getFeatureValue("INITCAPS"), 1.0));
		//Word class is computed on the plain unit (w/o EOS symbol)
		assertTrue(similar(fourth.getFeatureValue("BWC=A"), 1.0));
		assertTrue(similar(fourth.getFeatureValue("SIZE2"), 1.0));
	}
	
	@Test
	public void testPipeAllCapsToken() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "SPARTA"
		list.add("This is SPARTA !!!");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token third = (Token) tokens.get(2);

		assertTrue(Double.toString(third.getFeatureValue("ALLCAPS")), 
				similar(third.getFeatureValue("ALLCAPS"), 1.0));
		//Word class is computed on the plain unit (w/o EOS symbol)
		assertTrue(similar(third.getFeatureValue("BWC=A"), 1.0));
		assertTrue(similar(third.getFeatureValue("SIZE2"), 1.0));
	}
	
	@Test
	public void testPipeCapsToken() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "A"
		list.add("A 99.9% chance.");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token first = (Token) tokens.get(0);

		assertTrue(similar(first.getFeatureValue("ONECAPS"), 1.0));
		assertTrue(similar(first.getFeatureValue("ALLCAPS"), 1.0));
		assertTrue(similar(first.getFeatureValue("INITCAPS"), 1.0));
		assertTrue(similar(first.getFeatureValue("BWC=A"), 1.0));
		assertTrue(similar(first.getFeatureValue("SIZE1"), 1.0));
	}

	@Test
	public void testPipeHasDigitsToken() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "99.9%"
		list.add("A 99.9% chance.");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token second = (Token) tokens.get(1);

		assertTrue(similar(second.getFeatureValue("HASDIGITS"), 1.0));
		assertTrue(similar(second.getFeatureValue("hasinnerEOSSymb=1"), 1.0));
		assertTrue(similar(second.getFeatureValue("BWC=0x0x"), 1.0));
		assertTrue(similar(second.getFeatureValue("SIZE2"), 1.0));
	}
	
	@Test
	public void testPipeHSize3Token() {		
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "chance."
		list.add("A 99.9% chance.");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token third = (Token) tokens.get(2);

		assertTrue(similar(third.getFeatureValue("endwithEOSSymb=."), 1.0));
		assertTrue(similar(third.getFeatureValue("BWC=a"), 1.0));
		assertTrue(similar(third.getFeatureValue("SIZE3"), 1.0));
	}
	
	@Test
	public void testUnicodeToken() {
		ArrayList<String> list = new ArrayList<String>(1);
		//Testing "(Senior-Løken"
		list.add("Nephronophthisis (NPH) and RP (Senior-Løken syndrome).");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token token = (Token) tokens.get(4);
		
		assertTrue(similar(token.getFeatureValue("BWC=xAaxAa"), 1.0));
		assertTrue(similar(token.getFeatureValue("BEGINBRACKETS"), 1.0));
		assertTrue(similar(token.getFeatureValue("SIZE3"), 1.0));		
	}
	
	@Test
	public void testAlphanumericToken() {
		ArrayList<String> list = new ArrayList<String>(1);
		list.add("NH3");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token token = (Token) tokens.get(0);
		assertTrue(similar(token.getFeatureValue("ALPHANUMERIC"), 1.0));	
	}
	
	@Test
	public void testInitCapsLoken() {
		ArrayList<String> list = new ArrayList<String>(1);
		list.add("Ährengold");
		Instance inst = pipe.pipe(new Instance(list, "", "", ""));
		
		TokenSequence tokens = (TokenSequence) inst.getData();
		Token token = (Token) tokens.get(0);
		assertTrue(similar(token.getFeatureValue("INITCAPS"), 1.0));	
	}
	
	
	private boolean similar(double featureValue, double d) {
		if (Math.abs(featureValue - d) < Math.sqrt(Double.MIN_NORMAL)) {
			return true;
		}
		return false;
	}

}
