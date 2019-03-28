package de.julielab.jcore.ae.fvr;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.OntClassMention;
import de.julielab.jcore.types.ResourceEntry;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class FeatureValueReplacementAnnotatorTest {
	@Test
	public void testProcess() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				FeatureValueReplacementsProvider.class, new File("src/test/resources/testReplacementFile.map"));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(FeatureValueReplacementAnnotator.class,
				FeatureValueReplacementAnnotator.PARAM_FEATURE_PATHS,
				new String[] { "de.julielab.jcore.types.OntClassMention=/sourceOntology" },
				FeatureValueReplacementAnnotator.RESOURCE_REPLACEMENTS, extDesc);

		jCas.setDocumentText("This is an arbitrary document text long enough to hold a few fake-annotations.");
		OntClassMention ocm1 = new OntClassMention(jCas, 0, 2);
		ocm1.setSourceOntology("entry1");
		ocm1.addToIndexes();
		OntClassMention ocm2 = new OntClassMention(jCas, 0, 2);
		ocm2.setSourceOntology("entry2");
		ocm2.addToIndexes();
		OntClassMention ocm3 = new OntClassMention(jCas, 0, 2);
		ocm3.setSourceOntology("entry3");
		ocm3.addToIndexes();
		OntClassMention ocm4 = new OntClassMention(jCas, 0, 2);
		ocm4.setSourceOntology("entry2");
		ocm4.addToIndexes();
		OntClassMention ocm5 = new OntClassMention(jCas, 0, 2);
		ocm5.setSourceOntology("somethingelse");
		ocm5.addToIndexes();

		ae.process(jCas.getCas());
		assertEquals("replacement1", ocm1.getSourceOntology());
		assertEquals("replacement2", ocm2.getSourceOntology());
		assertEquals("replacement3", ocm3.getSourceOntology());
		assertEquals("replacement2", ocm4.getSourceOntology());
		assertEquals("somethingelse", ocm5.getSourceOntology());
	}
	
	@Test
	public void testProcessDefautltValue() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				FeatureValueReplacementsProvider.class, new File("src/test/resources/testReplacementFile.map"));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(FeatureValueReplacementAnnotator.class,
				FeatureValueReplacementAnnotator.PARAM_FEATURE_PATHS,
				new String[] { "de.julielab.jcore.types.OntClassMention=/sourceOntology?myDefault" },
				FeatureValueReplacementAnnotator.RESOURCE_REPLACEMENTS, extDesc);

		jCas.setDocumentText("This is an arbitrary document text long enough to hold a few fake-annotations.");
		OntClassMention ocm1 = new OntClassMention(jCas, 0, 2);
		ocm1.setSourceOntology("entry1");
		ocm1.addToIndexes();
		OntClassMention ocm2 = new OntClassMention(jCas, 0, 2);
		ocm2.setSourceOntology("entry2");
		ocm2.addToIndexes();
		OntClassMention ocm3 = new OntClassMention(jCas, 0, 2);
		ocm3.setSourceOntology("entry3");
		ocm3.addToIndexes();
		OntClassMention ocm4 = new OntClassMention(jCas, 0, 2);
		ocm4.setSourceOntology("entry2");
		ocm4.addToIndexes();
		OntClassMention ocm5 = new OntClassMention(jCas, 0, 2);
		ocm5.setSourceOntology("somethingelse");
		ocm5.addToIndexes();

		ae.process(jCas.getCas());
		assertEquals("replacement1", ocm1.getSourceOntology());
		assertEquals("replacement2", ocm2.getSourceOntology());
		assertEquals("replacement3", ocm3.getSourceOntology());
		assertEquals("replacement2", ocm4.getSourceOntology());
		assertEquals("myDefault", ocm5.getSourceOntology());

	}

	@Test
	public void testProcessRecusiveFeatureStructure() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				FeatureValueReplacementsProvider.class, new File("src/test/resources/testReplacementFile.map"));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(FeatureValueReplacementAnnotator.class,
				FeatureValueReplacementAnnotator.PARAM_FEATURE_PATHS,
				new String[] { "de.julielab.jcore.types.Gene=/resourceEntryList/entryId" },
				FeatureValueReplacementAnnotator.RESOURCE_REPLACEMENTS, extDesc);

		jCas.setDocumentText("This is an arbitrary document text long enough to hold a few fake-annotations.");
		Gene gene1 = new Gene(jCas, 0, 5);
		gene1.addToIndexes();
		FSArray resourceEntryList1 = new FSArray(jCas, 1);
		ResourceEntry resourceEntry1 = new ResourceEntry(jCas, 0, 5);
		resourceEntry1.setEntryId("entry1");
		resourceEntryList1.set(0, resourceEntry1);
		gene1.setResourceEntryList(resourceEntryList1);

		Gene gene2 = new Gene(jCas, 0, 5);
		gene2.addToIndexes();
		FSArray resourceEntryList2 = new FSArray(jCas, 2);
		ResourceEntry resourceEntry2 = new ResourceEntry(jCas, 0, 5);
		resourceEntry2.setEntryId("entry2");
		ResourceEntry resourceEntry3 = new ResourceEntry(jCas, 0, 5);
		resourceEntry3.setEntryId("entry3");
		resourceEntryList2.set(0, resourceEntry2);
		resourceEntryList2.set(1, resourceEntry3);
		gene2.setResourceEntryList(resourceEntryList2);

		ae.process(jCas.getCas());
		assertEquals("replacement1", resourceEntry1.getEntryId());
		assertEquals("replacement2", resourceEntry2.getEntryId());
		assertEquals("replacement3", resourceEntry3.getEntryId());
	}
}
