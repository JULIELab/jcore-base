package de.julielab.jcore.consumer.es;

import java.util.Arrays;
import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FeaturePathSet;
import de.julielab.jcore.consumer.es.FeaturePathSets;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FieldGenerator;
import de.julielab.jcore.consumer.es.FieldValueGenerator;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.es.test.AddressTestType;
import de.julielab.jcore.es.test.AuthorTestType;
import de.julielab.jcore.es.test.ESConsumerTestType;
import de.julielab.jcore.es.test.EntityTestType;
import de.julielab.jcore.es.test.HeaderTestType;
import de.julielab.jcore.es.test.TokenTestType;

public class TestFieldGeneratorBlackBeauty extends FieldGenerator {

	@SuppressWarnings("unused")
	private FilterRegistry filterRegistry;

	public TestFieldGeneratorBlackBeauty(FilterRegistry filterRegistry) {
		super(filterRegistry);
		this.filterRegistry = filterRegistry;
		addInnerDocumentGenerator(new StructuredAuthorFieldGenerator());
	}
	
	public TestFieldGeneratorBlackBeauty() {
		this(null);
	}

	public static final String TEXT_FIELD = "text";
	public static final String AUTHOR_FIELD = "authors";

	@Override
	public Document addFields(JCas aJCas, Document doc) throws CASException, FieldGenerationException {
		FeaturePathSet tokenSet = new FeaturePathSet(TokenTestType.type, null,
				null, null);
		FeaturePathSet esTestTypeSet = new FeaturePathSet(
				ESConsumerTestType.type, Arrays.asList("/stringArrayFeature"),
				null, null);
		FeaturePathSet entitySet = new FeaturePathSet(EntityTestType.type,
				Arrays.asList("/entityType"), null, null);
		List<PreanalyzedToken> tokens = getTokensForAnnotationIndexes(
				new FeaturePathSets(tokenSet, esTestTypeSet, entitySet), null,
				true, PreanalyzedToken.class, null,null, aJCas);
		doc.addField(TEXT_FIELD,
				new PreanalyzedFieldValue(aJCas.getDocumentText(), tokens));

		FeaturePathSet headerAuthorSet = new FeaturePathSet(
				HeaderTestType.type, Arrays.asList("/testAuthors/lastname",
						"/testAuthors/firstname"), ", ", null);
		List<RawToken> authorValues = getTokensForAnnotationIndexes(
				new FeaturePathSets(headerAuthorSet), null, false,
				RawToken.class, null, null, aJCas);
		doc.addField(AUTHOR_FIELD, new ArrayFieldValue(authorValues));

		doc.addField("structuredAuthor",
				getInnerDocumentGenerator(StructuredAuthorFieldGenerator.class)
						.generateFieldValue(aJCas));

		return doc;
	}

	private class StructuredAuthorFieldGenerator extends FieldValueGenerator {

		public StructuredAuthorFieldGenerator() {
			super();
			addInnerDocumentGenerator(new AddressFieldGenerator());
		}

		@Override
		public IFieldValue generateFieldValue(JCas aJCas) throws FieldGenerationException {
			HeaderTestType header = (HeaderTestType) aJCas
					.getJFSIndexRepository()
					.getAllIndexedFS(HeaderTestType.type).next();

			if (header.getTestAuthors() == null)
				return null;

			ArrayFieldValue array = new ArrayFieldValue();
			for (int i = 0; i < header.getTestAuthors().size(); ++i) {
				Document doc2 = new Document();
				AuthorTestType author = header.getTestAuthors(i);
				doc2.addField("firstname", new RawToken(author.getFirstname()));
				doc2.addField("lastname", new RawToken(author.getLastname()));
				doc2.addField("address", getInnerDocumentGenerator(AddressFieldGenerator.class).generateFieldValue(author));
				array.add(doc2);
			}

			return array;
		}


		private class AddressFieldGenerator extends FieldValueGenerator {

			@Override
			public Document generateFieldValue(FeatureStructure fs) {
				Document doc2 = new Document();
				AuthorTestType author = (AuthorTestType) fs;
				AddressTestType address = author.getAuthorAddress();
				if (null == address)
					return null;
				if (null != address.getStreet())
					doc2.addField("street", new RawToken(address.getStreet()));
				doc2.addField("number", new RawToken(address.getNumber()));
				if (null != address.getCity())
					doc2.addField("city", new RawToken(address.getCity()));

				return doc2;
			}

		}

	}

}
