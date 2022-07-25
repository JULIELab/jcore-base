package de.julielab.jcore.reader.nlmgene;

import com.pengyifan.bioc.BioCAnnotation;
import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.BioCDocument;
import com.pengyifan.bioc.BioCPassage;
import com.pengyifan.bioc.io.BioCCollectionReader;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.AbstractText;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceMetaData(name = "JCoRe NLM-Gene Reader", description = "Collection reader for the BioC format of the NLM-Gene corpus.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {}, outputs = {"de.julielab.jcore.types.Gene", "de.julielab.jcore.types.ResourceEntry"})
public class NLMGeneReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDirectory";
    public static final String PARAM_ID_LIST_PATH = "IdList";
    private final static Logger log = LoggerFactory.getLogger(NLMGeneReader.class);
    @ConfigurationParameter(name = PARAM_INPUT_DIR, description = "Path to the directory that contains the BioC XML files of the NLM-Gene corpus.")
    private String inputDir;
    @ConfigurationParameter(name = PARAM_ID_LIST_PATH, mandatory = false, description = "Path to a file with a list of IDs to restrict the read files to. This will typically be the list with IDs for the training or for the test set of the corpus. When no list is specified, the whole corpus is read.")
    private String idList;
    private Iterator<Path> corpusFileIterator;
    private int numRead;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        inputDir = (String) context.getConfigParameterValue(PARAM_INPUT_DIR);
        idList = (String) context.getConfigParameterValue(PARAM_ID_LIST_PATH);
        try {
            corpusFileIterator = readInputFiles(inputDir, idList);
        } catch (IOException e) {
            log.error("Could not read NLM-Gene corpus input files.", e);
            throw new ResourceInitializationException(e);
        }
        numRead = 0;
    }

    private Iterator<Path> readInputFiles(String inputDir, String idList) throws IOException {
        Path inputPath = Path.of(inputDir);
        Path idListPath = idList != null ? Path.of(idList) : null;
        Set<String> ids = idListPath != null && Files.exists(idListPath)  ? Files.readAllLines(idListPath).stream().collect(Collectors.toSet()) : Collections.emptySet();
        return Files.list(inputPath)
                .filter(p -> p.toString().toLowerCase().endsWith(".xml") || p.toString().toLowerCase().endsWith(".xml.gz"))
                .filter(p -> ids.isEmpty() ? true : ids.contains(p.getFileName().toString().replaceAll("(?i)\\.bioc\\.xml(\\.gz)?", "")))
                .iterator();
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void getNext(JCas jCas) throws CollectionException {
        final Path nextFile = corpusFileIterator.next();
        try {
            final BioCCollectionReader reader = new BioCCollectionReader(nextFile);
            final BioCCollection collection = reader.readCollection();
            if (collection.getDocmentCount() > 1)
                throw new IllegalArgumentException("A single document per BioC collection is expected but the collection of file " + nextFile + " has " + collection.getDocmentCount() + " documents. This case is not supported.");
            final BioCDocument document = collection.getDocument(0);

            handleHeader(jCas, document);
            StringBuilder textBuilder = new StringBuilder();
            for (BioCPassage p : document.getPassages()) {
                int previousTextLength = textBuilder.length();
                textBuilder.append(p.getText().get());
                handlePassageStructureType(jCas, textBuilder, p, previousTextLength);
                handleAnnotation(jCas, document, p, textBuilder);
                textBuilder.append(System.getProperty("line.separator"));
            }

            jCas.setDocumentText(textBuilder.toString());
        } catch (XMLStreamException | IOException e) {
            log.error("Could not read NLM-Gene corpus file {}", nextFile, e);
            throw new CollectionException(e);
        }
    }

    private void handleHeader(JCas jCas, BioCDocument document) {
        final Header h = new Header(jCas);
        h.setDocId(document.getID());
        h.setComponentId(getClass().getSimpleName());
        h.setSource("NLM-Gene");
        h.addToIndexes();
    }

    private void handleAnnotation(JCas jCas, BioCDocument document, BioCPassage p, StringBuilder textBuilder) {
        for (BioCAnnotation a : p.getAnnotations()) {
            final Gene g = new Gene(jCas, a.getTotalLocation().getOffset(), a.getTotalLocation().getOffset() + a.getTotalLocation().getLength());
            final Optional<String> typeInfon = a.getInfon("type");
            final Optional<String> codeInfon = a.getInfon("code");
            handleErrors(document, p, a, g, typeInfon, textBuilder);
            handleGeneId(jCas, a, g);
            handleSpecificType(g, typeInfon, codeInfon);
            g.addToIndexes();
        }
    }

    private void handleSpecificType(Gene g, Optional<String> typeInfon, Optional<String> codeInfon) {
        g.setSpecificType(typeInfon.get());
        if (codeInfon.isPresent())
            g.setSpecificType(typeInfon.get() + "-" + codeInfon.get());
    }

    private void handleErrors(BioCDocument document, BioCPassage p, BioCAnnotation a, Gene g, Optional<String> typeInfon, StringBuilder textBuilder) {
        if (typeInfon.isPresent() && !(typeInfon.get().equals("Gene") || typeInfon.get().equals("GENERIF")))
            throw new IllegalStateException("The annotation " + a.getID() + " of passage " + p.getInfon("type").get() + " of document " + document.getID() + " was neither of type Gene nor GENERIF.");
        if (!typeInfon.isPresent())
            throw new IllegalStateException("The annotation " + a.getID() + " of passage " + p.getInfon("type").get() + " of document " + document.getID() + " does not specify a type.");
        if (!textBuilder.substring(g.getBegin(), g.getEnd()).equals(a.getText().get()))
            throw new IllegalStateException("The annotation " + a.getID() + " of passage " + p.getInfon("type").get() + " of document " + document.getID() + " has the covered text " + textBuilder.substring(g.getBegin(), g.getEnd()) + " but should have the text " + a.getText().get() + " according to the BioC XML information.");
    }

    private void handleGeneId(JCas jCas, BioCAnnotation a, Gene g) {
        final Optional<String> ncbiGeneId = a.getInfon("NCBI Gene identifier");
        if (ncbiGeneId.isPresent()) {
            final ResourceEntry re = new ResourceEntry(jCas, g.getBegin(), g.getEnd());
            re.setEntryId(ncbiGeneId.get());
            re.setComponentId(getClass().getSimpleName());
            final FSArray resourceEntryList = new FSArray(jCas, 1);
            resourceEntryList.set(0, re);
            g.setResourceEntryList(resourceEntryList);
        }
    }

    private void handlePassageStructureType(JCas jCas, StringBuilder textBuilder, BioCPassage p, int previousTextLength) {
        final Optional<String> typeInfon = p.getInfon("type");
        if (typeInfon.isPresent() && typeInfon.get().equals("title")) {
            final Title t = new Title(jCas, previousTextLength, textBuilder.length());
            t.setTitleType("document");
            t.setComponentId(getClass().getSimpleName());
            t.addToIndexes();
        } else if (typeInfon.isPresent() && typeInfon.get().equals("abstract")) {
            final AbstractText abstractText = new AbstractText(jCas, previousTextLength, textBuilder.length());
            abstractText.setComponentId(getClass().getSimpleName());
            abstractText.addToIndexes();
        }
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(numRead, 0, "documents")};
    }

    @Override
    public boolean hasNext() {
        return corpusFileIterator.hasNext();
    }

}
