package de.julielab.jcore.reader.pubtator;

import static de.julielab.jcore.reader.pubtator.PubtatorDocument.EMPTY_DOCUMENT;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.Disease;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.Title;

public class PubtatorReader extends CollectionReader_ImplBase {

	private static final String COMPONENT_NAME = PubtatorReader.class.getCanonicalName();

	public static final String PARAM_INPUT = "Input";

	@ConfigurationParameter(name = PARAM_INPUT, mandatory = true)
	private File input;

	private File[] inputFiles;
	private BufferedReader currentReader;
	private PubtatorDocument currentDocument = EMPTY_DOCUMENT;

	private int index;
	private long completed = 0;

	@Override
	public void initialize() throws ResourceInitializationException {
		super.initialize();
		String inputDirectoryPath = (String) getConfigParameterValue(PARAM_INPUT);
		input = new File(inputDirectoryPath);
		if (!input.exists())
			throw new ResourceInitializationException(
					new IllegalArgumentException("The path " + input.getAbsolutePath() + " does not exist."));
		if (input.isDirectory())
			inputFiles = input.listFiles((f, s) -> s.endsWith(".txt"));
		else
			inputFiles = new File[] { input };
		index = 0;
	}

	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		if (hasNext()) {
			try {
				JCas jcas = aCAS.getJCas();
				Header header = new Header(jcas);
				header.setDocId(currentDocument.getDocumentId());
				header.addToIndexes();
				// the offsets of the pubtator entity format expect that title and abstracts are separated by a single character
				jcas.setDocumentText(currentDocument.getTitle() + "\n" + currentDocument.getAbstractText());
				Title title = new Title(jcas, 0, currentDocument.getTitle().length());
				title.setTitleType("document");
				title.setComponentId(COMPONENT_NAME);
				title.addToIndexes();
				AbstractText abstractText = new AbstractText(jcas, title.getEnd()+1, title.getEnd()+1+currentDocument.getAbstractText().length());
				abstractText.setComponentId(COMPONENT_NAME);
				abstractText.addToIndexes();
				
				for (PubtatorEntity e : currentDocument.getEntities()) {
					Annotation a;
					switch (e.getEntityType()) {
					case "Chemical":
						Chemical chemical = new Chemical(jcas);
						chemical.setNameOfSubstance(e.getText());
						chemical.setRegistryNumber(e.getEntityId());
						a = chemical;
						break;
					case "Disease":
						a = new Disease(jcas);
						break;
					case "Gene":
						Gene g = new Gene(jcas);
						GeneResourceEntry geneResourceEntry = new GeneResourceEntry(jcas, e.getBegin(), e.getEnd());
						geneResourceEntry.setEntryId(e.getEntityId());
						geneResourceEntry.setSource("NCBI Gene");
						geneResourceEntry.setComponentId(COMPONENT_NAME);
						FSArray geneEntryList = new FSArray(jcas, 1);
						geneEntryList.set(0, geneResourceEntry);
						g.setResourceEntryList(geneEntryList);
						a = g;
						break;
					case "Species":
						Organism o = new Organism(jcas);
						ResourceEntry organismResourceEntry = new GeneResourceEntry(jcas, e.getBegin(), e.getEnd());
						organismResourceEntry.setEntryId(e.getEntityId());
						organismResourceEntry.setSource("NCBI Taxonomy");
						organismResourceEntry.setComponentId(COMPONENT_NAME);
						FSArray organismEntryList = new FSArray(jcas, 1);
						organismEntryList.set(0, organismResourceEntry);
						o.setResourceEntryList(organismEntryList);
						a = o;
						break;
					case "Mutation":
					default:
						EntityMention em = new EntityMention(jcas);
						a = em;
						break;
					}
					if (a instanceof EntityMention)
						((EntityMention) a).setSpecificType(e.getEntityType());
					a.setBegin(e.getBegin());
					a.setEnd(e.getEnd());
					a.setComponentId(COMPONENT_NAME);
					a.addToIndexes();
				}
			} catch (CASException e) {
				throw new CollectionException(e);
			}
			currentDocument = EMPTY_DOCUMENT;
			++completed;
		}
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		if (currentReader == null && index < inputFiles.length)
			currentReader = FileUtilities.getReaderFromFile(inputFiles[index]);
		if (currentDocument == EMPTY_DOCUMENT)
			currentDocument = PubtatorDocument.parseNextDocument(currentReader);
		// if the document was empty it means the current file is at its end,
		// continue to the next
		while (currentDocument == EMPTY_DOCUMENT && index < inputFiles.length) {
			++index;
			if (index < inputFiles.length) {
				currentReader = FileUtilities.getReaderFromFile(inputFiles[index]);
				currentDocument = PubtatorDocument.parseNextDocument(currentReader);
			}
		}

		return currentDocument != PubtatorDocument.EMPTY_DOCUMENT;
	}

	@SuppressWarnings("serial")
	@Override
	public Progress[] getProgress() {
		return new Progress[] { new Progress() {

			@Override
			public long getCompleted() {
				return completed;
			}

			@Override
			public long getTotal() {
				return 0;
			}

			@Override
			public String getUnit() {
				return "Documents";
			}

			@Override
			public boolean isApproximate() {
				return true;
			}
		} };
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}

}
