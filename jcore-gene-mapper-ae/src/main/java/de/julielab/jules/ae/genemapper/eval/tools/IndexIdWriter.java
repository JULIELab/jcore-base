package de.julielab.jules.ae.genemapper.eval.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;

public class IndexIdWriter {

	public static void main(String[] args) throws IOException  {
		String mentionIndex = "data/eval_data/biocreative2_data/entrezGeneLexicon_sense_index";
		RAMDirectory ramDir = new RAMDirectory(mentionIndex);
		IndexReader reader = new IndexSearcher(ramDir).getIndexReader();
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < reader.maxDoc(); ++i) {
			Document doc = reader.document(i);
			ids.add(doc.get("uniprot_id"));
		}
		FileUtils.writeLines(new File("ids-synonyms-old-bc2-index.lst"), "UTF-8", ids);

		mentionIndex = "data/eval_data/biocreative2_data/entrezGeneContextToken_index.bc2";
		 ramDir = new RAMDirectory(mentionIndex);
		reader = new IndexSearcher(ramDir).getIndexReader();
		ids = new ArrayList<>();
		for (int i = 0; i < reader.maxDoc(); ++i) {
			Document doc = reader.document(i);
			ids.add(doc.get("indexed_id"));
		}
		FileUtils.writeLines(new File("ids-context-old-bc2-index.lst"), "UTF-8", ids);
	}

}
