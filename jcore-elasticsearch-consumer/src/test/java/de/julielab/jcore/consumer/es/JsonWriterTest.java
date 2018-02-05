package de.julielab.jcore.consumer.es;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.junit.Test;

import de.julielab.jcore.consumer.es.JsonWriter;
import de.julielab.jcore.es.test.AddressTestType;
import de.julielab.jcore.es.test.AuthorTestType;
import de.julielab.jcore.es.test.ESConsumerTestType;
import de.julielab.jcore.es.test.EntityTestType;
import de.julielab.jcore.es.test.HeaderTestType;
import de.julielab.jcore.es.test.TokenTestType;

public class JsonWriterTest {
	@Test
	public void testAddIndexSource() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.consumer.es.testTypes");

		cas.setDocumentText("Black Beauty ran past the bloody barn.");
		new TokenTestType(cas, 0, 5).addToIndexes();
		new TokenTestType(cas, 6, 12).addToIndexes();
		new TokenTestType(cas, 13, 16).addToIndexes();
		new TokenTestType(cas, 17, 21).addToIndexes();
		new TokenTestType(cas, 22, 25).addToIndexes();
		new TokenTestType(cas, 26, 32).addToIndexes();
		new TokenTestType(cas, 33, 37).addToIndexes();
		new TokenTestType(cas, 37, 38).addToIndexes();

		// Black Beauty
		EntityTestType e = new EntityTestType(cas, 0, 12);
		e.setEntityType("hero");
		e.addToIndexes();

		// 'bloody barn', say this would be an NP annotation
		ESConsumerTestType esctt = new ESConsumerTestType(cas, 26, 37);
		StringArray sa = new StringArray(cas, 2);
		// just some values, no deeper meaning here
		sa.set(0, "NP");
		sa.set(1, "NNP");
		esctt.setStringArrayFeature(sa);
		esctt.addToIndexes();

		HeaderTestType header = new HeaderTestType(cas);
		header.setDocId("theBeautyDoc");
		AuthorTestType author1 = new AuthorTestType(cas);
		author1.setFirstname("Anna");
		author1.setLastname("Sewell");
		AddressTestType address1 = new AddressTestType(cas);
		address1.setStreet("42nd Street");
		address1.setNumber(47);
		address1.setCity("London");
		author1.setAuthorAddress(address1);
		AuthorTestType author2 = new AuthorTestType(cas);
		author2.setFirstname("No");
		author2.setLastname("One");
		AddressTestType address2 = new AddressTestType(cas);
		// we only set one value on purpose; we want to test that there is no information bleed due to the passing of
		// feature structured via "setTransferredFS" and "addSubgeneratorFS"
		address2.setCity("Cambridge");
		author2.setAuthorAddress(address2);
		FSArray authorArray = new FSArray(cas, 2);
		authorArray.set(0, author1);
		authorArray.set(1, author2);
		header.setTestAuthors(authorArray);
		header.addToIndexes();

		System.setProperty("ES_CONSUMER_TEST_MODE", "true");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine("src/main/resources/de/julielab/jcore/consumer/es/desc/jcore-json-consumer", JsonWriter.PARAM_FIELD_GENERATORS, new String[] {TestFieldGeneratorBlackBeauty.class.getCanonicalName()}, JsonWriter.PARAM_OUTPUT_DIR, "src/test/resources/json-output");
		consumer.process(cas.getCas());
		consumer.collectionProcessComplete();
		
		String indexSource = IOUtils.toString(new GZIPInputStream(new FileInputStream("src/test/resources/json-output/theBeautyDoc.json.gz")));
		// No, I haven't written that JSON myself. I just printed out the indexSource, checked that it is correct (I
		// recommend an online formatter for this, e.g. at http://jsonformatter.curiousconcept.com/), checked that it
		// was right and then added it into the assertion statement. Another tip: All the escaping has been done by
		// Eclipse for me, there is an editor setting that says "automatically escape when pasting into string literal".
		// Very handy at times.
		assertEquals(
				"{\"structuredAuthor\":[{\"firstname\":\"Anna\",\"address\":{\"number\":47,\"city\":\"London\",\"street\":\"42nd Street\"},\"lastname\":\"Sewell\"},{\"firstname\":\"No\",\"address\":{\"number\":0,\"city\":\"Cambridge\"},\"lastname\":\"One\"}],\"text\":\"{\\\"v\\\":\\\"1\\\",\\\"str\\\":\\\"Black Beauty ran past the bloody barn.\\\",\\\"tokens\\\":[{\\\"t\\\":\\\"Black\\\",\\\"s\\\":0,\\\"e\\\":5,\\\"i\\\":1},{\\\"t\\\":\\\"hero\\\",\\\"s\\\":0,\\\"e\\\":12,\\\"i\\\":0},{\\\"t\\\":\\\"Beauty\\\",\\\"s\\\":6,\\\"e\\\":12,\\\"i\\\":1},{\\\"t\\\":\\\"ran\\\",\\\"s\\\":13,\\\"e\\\":16,\\\"i\\\":1},{\\\"t\\\":\\\"past\\\",\\\"s\\\":17,\\\"e\\\":21,\\\"i\\\":1},{\\\"t\\\":\\\"the\\\",\\\"s\\\":22,\\\"e\\\":25,\\\"i\\\":1},{\\\"t\\\":\\\"bloody\\\",\\\"s\\\":26,\\\"e\\\":32,\\\"i\\\":1},{\\\"t\\\":\\\"NP\\\",\\\"s\\\":26,\\\"e\\\":37,\\\"i\\\":0},{\\\"t\\\":\\\"NNP\\\",\\\"s\\\":26,\\\"e\\\":37,\\\"i\\\":0},{\\\"t\\\":\\\"barn\\\",\\\"s\\\":33,\\\"e\\\":37,\\\"i\\\":1},{\\\"t\\\":\\\".\\\",\\\"s\\\":37,\\\"e\\\":38,\\\"i\\\":1}]}\",\"authors\":[\"Sewell, Anna\",\"One, No\"]}",
				indexSource);
	}
}
