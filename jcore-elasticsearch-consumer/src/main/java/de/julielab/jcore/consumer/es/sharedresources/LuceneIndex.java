package de.julielab.jcore.consumer.es.sharedresources;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class LuceneIndex implements StringIndex {
    private final static Logger log = LoggerFactory.getLogger(LuceneIndex.class);
    private  IndexWriter iw;
    private final FSDirectory directory;
    private IndexSearcher searcher;

    public LuceneIndex(String indexDirectory) {
        try {
            Path lucene = Path.of(indexDirectory);
            File directoryFile = lucene.toFile();
            boolean indexExists = directoryFile.exists() && directoryFile.isDirectory() && directoryFile.list().length != 0;
            directory = NIOFSDirectory.open(lucene);
            // Do not open a writer to an existing index. This causes locking issues when starting multiple
            // pipelines in parallel.
            // Of course, the first pipeline still needs to create the index, so this must be a one-time effort
            // that has to be completed before the other pipelines are started.
            if (!indexExists) {
                log.debug("Creating index writer for index directory {}.", indexDirectory);
                IndexWriterConfig iwc = new IndexWriterConfig();
                iw = new IndexWriter(directory, iwc);
            } else {
                log.debug("Index directory {} already exists.", indexDirectory);
            }
        } catch (IOException e) {
            log.error("could not initialize Lucene index", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String get(String key) {
        TermQuery tq = new TermQuery(new Term("key", key));
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        b.add(tq, BooleanClause.Occur.FILTER);
        BooleanQuery q = b.build();
        try {
            TopDocs topDocs = searcher.search(q, 1);
            if (topDocs.scoreDocs.length > 0) {
                Document doc = searcher.getIndexReader().document(topDocs.scoreDocs[0].doc);
                return doc.getField("value").stringValue();
            }
        } catch (IOException e) {
            log.error("Could not retrieve results for '{}' in Lucene index.", key, e);
            throw new IllegalStateException(e);
        }
        return null;
    }

    @Override
    public String[] getArray(String key) {
        TermQuery tq = new TermQuery(new Term("key", key));
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        b.add(tq, BooleanClause.Occur.FILTER);
        BooleanQuery q = b.build();
        try {
            TopDocs topDocs = searcher.search(q, 1);
            if (topDocs.scoreDocs.length > 0) {
                Document doc = searcher.getIndexReader().document(topDocs.scoreDocs[0].doc);
                return Arrays.stream(doc.getFields("value")).map(IndexableField::stringValue).toArray(String[]::new);
            }
        } catch (IOException e) {
            log.error("Could not retrieve results for '{}' in Lucene index.", key, e);
            throw new IllegalStateException(e);
        }
        return null;
    }

    @Override
    public void put(String key, String value) {
        Field keyField = new StringField("key", key, Field.Store.NO);
        Field valueField = new StoredField("value", value);
        Document doc = new Document();
        doc.add(keyField);
        doc.add(valueField);
        try {
            iw.addDocument(doc);
        } catch (IOException e) {
            log.error("Could not index key-value pair {}:{} with Lucene", key, value, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void put(String key, String[] value) {
        Field keyField = new StringField("key", key, Field.Store.NO);
        Document doc = new Document();
        doc.add(keyField);
        for (var v : value)
            doc.add(new StoredField("value", v));
        try {
            iw.addDocument(doc);
        } catch (IOException e) {
            log.error("Could not index key-value pair {}:{} with Lucene", key, value, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void commit() {
        try {
            iw.commit();
        } catch (IOException e) {
            log.error("Could not commit Lucene index", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean requiresExplicitCommit() {
        return true;
    }

    @Override
    public void close() {
        try {
            if (searcher != null) {
                searcher.getIndexReader().close();
                searcher = null;
            }
            if (iw != null) {
                iw.close();
                iw = null;
            }
        } catch (IOException e) {
            log.error("Could not close Lucene index reader.", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void open() {
        try {
            searcher = new IndexSearcher(DirectoryReader.open(directory));
        } catch (IOException e) {
            log.error("Could not open Lucene index searcher.", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int size() {
        if (iw != null && iw.isOpen())
            return iw.numDocs();
        else if (searcher != null)
            return searcher.getIndexReader().numDocs();
        return 0;
    }
}
