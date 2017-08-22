package de.julielab.jules.ae.genemapper.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wcohen.ss.BasicStringWrapper;
import com.wcohen.ss.BasicStringWrapperIterator;
import com.wcohen.ss.TFIDF;
import com.wcohen.ss.api.StringWrapper;

import de.julielab.jules.ae.genemapper.GeneMapper;

public class TFIDFUtils {

	private static final Logger log = LoggerFactory.getLogger(TFIDFUtils.class);

	private TFIDF tfidf;

	public static void main(String[] args) {
		TFIDFUtils tfidfUtils = new TFIDFUtils();
		tfidfUtils.test();
	}

	public void learnFromLuceneIndex(IndexReader ir, String contentField) {
		long time = System.currentTimeMillis();
		log.info("Learning TF/IDF statistic from Lucene index, field name: {}", contentField);
		List<StringWrapper> trainData = new ArrayList<>(ir.numDocs() - ir.numDeletedDocs());
		try {
			log.info("Traversing Lucene index and collecting field values...");
			for (int i = 0; i < ir.numDocs(); ++i) {
				if (ir.isDeleted(i))
					continue;
				Document doc = ir.document(i);
				Field field = doc.getField(contentField);
				if (field == null) {
					if (!GeneMapper.LEGACY_INDEX_SUPPORT) {
						throw new IllegalArgumentException(
								"Field " + contentField + " was not found in the given index.");
					} else {
						log.info(
								"Field {} does not exist in the given index. Legacy index support is enabled, just skipping this index.", contentField);
						break;
					}
				}

				String stringValue = field.stringValue();
				trainData.add(new BasicStringWrapper(stringValue));
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		log.info("Computing TF/IDF statistics...");
		// use the default tokenizer
		tfidf = new TFIDF();
		tfidf.train(new BasicStringWrapperIterator(trainData.iterator()));
		time = System.currentTimeMillis() - time;
		log.info("Done learning TF/IDF for field {} in {} seconds.", contentField, time / 1000);
	}

	public double score(String s1, String s2) {
		if (GeneMapper.LEGACY_INDEX_SUPPORT && (null == s1 || null == s2))
			return 0;
		return tfidf.score(s1, s2);
	}

	public double score(StringWrapper s1, StringWrapper s2) {
		if (GeneMapper.LEGACY_INDEX_SUPPORT && (null == s1 || null == s2))
			return 0;
		return tfidf.score(s1, s2);
	}

	public void test() {
		TFIDF tfidf = new TFIDF();
		List<StringWrapper> strings = new ArrayList<>();
		strings.add(getbsw("1 2 3 4 "));
		strings.add(getbsw("1 4"));
		strings.add(getbsw("5 5 5 "));
		strings.add(getbsw("5 6 2 1"));
		BasicStringWrapperIterator it = new BasicStringWrapperIterator(strings.iterator());
		tfidf.train(it);

		System.out.println(tfidf.explainScore("2 4 6", "7, 4, 9"));
		System.out.println(tfidf.explainScore("1 2 3", "1 7 19"));
	}

	private StringWrapper getbsw(String string) {
		return new BasicStringWrapper(string);
	}
}
