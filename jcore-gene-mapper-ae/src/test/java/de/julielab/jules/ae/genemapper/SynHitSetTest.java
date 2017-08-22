package de.julielab.jules.ae.genemapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SynHitSetTest {

	@Test
	public void testRetainAll() {
		SynHit syn1_1 = new SynHit("osteoprotegenin", 9999.0, "4982", "Tagger");
		SynHit syn1_2 = new SynHit("osteoprotegenin ligand", 9999.0, "8600", "Tagger");
		SynHitSet synSet1 = new SynHitSet();
		synSet1.add(syn1_1);
		synSet1.add(syn1_2);

		SynHit syn2_1 = new SynHit("opg", 9999.0, "4982", "Tagger");
		SynHit syn2_2 = new SynHit("opg", 9999.0, "690", "Tagger");
		SynHitSet synSet2 = new SynHitSet();
		synSet2.add(syn2_1);
		synSet2.add(syn2_2);
		
		assertTrue(synSet1.retainAll(synSet2));
		assertEquals(1, synSet1.size());
		assertTrue(synSet1.first().equals(syn1_1));
	}

	@Test
	public void testContainsAny() {
		SynHit syn1_1 = new SynHit("osteoprotegenin", 9999.0, "4982", "Tagger");
		SynHit syn1_2 = new SynHit("osteoprotegenin ligand", 9999.0, "8600", "Tagger");
		SynHitSet synSet1 = new SynHitSet();
		synSet1.add(syn1_1);
		synSet1.add(syn1_2);

		SynHit syn2_1 = new SynHit("opg", 9999.0, "4982", "Tagger");
		SynHit syn2_2 = new SynHit("opg", 9999.0, "690", "Tagger");
		SynHitSet synSet2 = new SynHitSet();
		synSet2.add(syn2_1);
		synSet2.add(syn2_2);

		assertTrue(synSet1.containsAny(synSet2));
	}

	@Test
	public void testContainsAnyFalse() {
		SynHit syn1_1 = new SynHit("osteoprotegenin", 9999.0, "4982", "Tagger");
		SynHit syn1_2 = new SynHit("osteoprotegenin ligand", 9999.0, "690", "Tagger");
		SynHitSet synSet1 = new SynHitSet();
		synSet1.add(syn1_1);
		synSet1.add(syn1_2);

		SynHit syn2_1 = new SynHit("interleukin 2 receptor beta", 9999.0, "3560", "Tagger");
		SynHit syn2_2 = new SynHit("interleukin 2 receptor alpha", 9999.0, "3559", "Tagger");
		SynHitSet synSet2 = new SynHitSet();
		synSet2.add(syn2_1);
		synSet2.add(syn2_2);

		assertFalse(synSet1.containsAny(synSet2));
	}

}
