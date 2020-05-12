package de.julielab.jcore.reader.testreader;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.util.Progress;

public class TestReader extends CollectionReader_ImplBase {

	@Override
	public void getNext(CAS arg0) throws IOException, CollectionException {
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Progress[] getProgress() {
		return null;
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return false;
	}

}
