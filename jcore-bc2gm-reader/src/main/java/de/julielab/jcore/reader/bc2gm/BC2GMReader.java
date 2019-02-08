package de.julielab.jcore.reader.bc2gm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Header;
import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;


@ResourceMetaData(name = "JCoRe BioCreative II Gene Mention reader", description = "This component reads gene annotated sentences in the BioCreative II Gene Mention challenge format. Each CAS will contain one annotated sentence.")
@TypeCapability(outputs = "de.julielab.jcore.types.Gene")
public class BC2GMReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_SENTENCES = "SentencesFile";
    public static final String PARAM_GENES = "GenesFile";
    @ConfigurationParameter(name = PARAM_SENTENCES, description = "The BC2GM data is comprised of one file holding one sentence per line and another file holding the annotations. This parameter should be set to the file containing the sentences.")
    private String sentenceFile;
    @ConfigurationParameter(name = PARAM_GENES, description = "The BC2GM data is comprised of one file holding one sentence per line and another file holding the annotations. This parameter should be set to the file holding the gene annotations.")
    private String genesFile;
    private Multimap<String, GeneAnnotation> geneAnnotations;
    private Iterator<String> sentencesIterator;

    /**
     * Returns a map where for each white space position, the number of
     * preceding non-whitespace characters from the beginning of <tt>input</tt>
     * is returned.<br/>
     * Thus, for each character-based offset <tt>o</tt>, the non-whitespace
     * offset may be retrieved using the floor entry for <tt>o</tt>, retrieving
     * its value and subtracting it from <tt>o</tt>.
     *
     * @param input
     * @return
     */
    public static TreeMap<Integer, Integer> createNumWsMap(String input) {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        map.put(0, 0);
        int numWs = 0;
        boolean lastCharWasWs = false;
        for (int i = 0; i < input.length(); ++i) {
            if (lastCharWasWs)
                map.put(i, numWs);
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                ++numWs;
                lastCharWasWs = true;
            } else {
                lastCharWasWs = false;
            }
        }
        return map;
    }

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        sentenceFile = (String) context.getConfigParameterValue(PARAM_SENTENCES);
        genesFile = (String) context.getConfigParameterValue(PARAM_GENES);
        if (null == sentenceFile)
            throw new ResourceInitializationException(
                    new IllegalArgumentException("Sentences file parameter is null."));
        if (null != genesFile) {
            try {
                geneAnnotations = readGeneAnnotations(genesFile);
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }
        try {
            sentencesIterator = Files.readAllLines(new File(sentenceFile).toPath(), Charset.forName("UTF-8"))
                    .iterator();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        super.initialize();
    }

    /**
     * WARNING: This would read in the annotation data, but it is not actually
     * added to the CAS (because at time of writing it was not necessary)
     *
     * @param genesFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Multimap<String, GeneAnnotation> readGeneAnnotations(String genesFile)
            throws FileNotFoundException, IOException {
        Multimap<String, GeneAnnotation> annotations = HashMultimap.create();
        try (InputStream is = new FileInputStream(genesFile)) {
            Iterator<String> lineIterator = IOUtils.readLines(is).iterator();
            while (lineIterator.hasNext()) {
                GeneAnnotation geneAnnotation = new GeneAnnotation();
                // the lines look like this: P11740571A0114|87 107|glutathione
                // peroxidase
                String genesLine = (String) lineIterator.next();
                String[] record = genesLine.split("\\|");
                String[] offsets = record[1].split(" ");
                geneAnnotation.sentenceId = record[0];
                geneAnnotation.start = Integer.parseInt(offsets[0].trim());
                geneAnnotation.end = Integer.parseInt(offsets[1].trim());
                geneAnnotation.text = record[2];
                annotations.put(geneAnnotation.sentenceId, geneAnnotation);
            }
        }
        return annotations;
    }

    @Override
    public void getNext(JCas cas) throws IOException, CollectionException {
        // line format:
        // P00008171T0000 Pharmacologic aspects of neonatal hyperbilirubinemia.

        String sentenceLine = sentencesIterator.next();
        String[] split = sentenceLine.split(" ", 2);
        String id = split[0];
        String sentence = split[1];

        cas.setDocumentText(sentence);
        Header header = new Header(cas);
        header.setDocId(id);
        header.addToIndexes();

        TreeMap<Integer, Integer> wsMap = createNumWsMap(sentence);

        if (geneAnnotations != null) {
            Collection<GeneAnnotation> sentenceAnnotations = geneAnnotations.get(id);
            for (GeneAnnotation ga : sentenceAnnotations) {
                int start = ga.start;
                String text = ga.text;
                int textStart = 0;
                boolean positionFound = false;
                while ((textStart = sentence.indexOf(text, textStart)) != -1) {
                    Integer numWs = wsMap.floorEntry(textStart).getValue();
                    // we allow some flexibility regarding the offsets to
                    // account for minor annotation errors in the original
                    // BC2GM data
                    if (start + numWs == textStart || start + numWs == textStart + 1
                            || start + numWs == textStart - 1) {
                        int end = ga.end + wsMap.floorEntry(textStart + text.length()).getValue() + 1;
                        Gene gene = new Gene(cas, start + numWs, end);
                        gene.addToIndexes();
                        positionFound = true;
                        break;
                    }
                    textStart += text.length();
                }
                if (!positionFound)
                    throw new IllegalStateException(
                            "The document-relative, whitespace-including position of the gene \"" + text
                                    + "\" with BC2GM offsets " + ga.start + "-" + ga.end + " in sentence " + id
                                    + " could not be found.");
            }
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Progress[] getProgress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return sentencesIterator.hasNext();
    }

    public class GeneAnnotation {
        public String sentenceId;
        public int start;
        public int end;
        public String text;
    }

}
