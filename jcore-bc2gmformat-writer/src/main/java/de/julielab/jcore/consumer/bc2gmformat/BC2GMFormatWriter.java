package de.julielab.jcore.consumer.bc2gmformat;


import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ResourceMetaData(name = "JCoRe BioCreative II Gene Mention Format writer", description = "This component writes gene annotations in the CAS to the format employed by the BioCreative II Gene Mention challenge.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Sentence", "de.julielab.jcore.types.Gene"})
public class BC2GMFormatWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_OUTPUT_DIR = "OutputDirectory";
    public static final String PARAM_SENTENCES_FILE = "SentencesFileName";
    public static final String PARAM_GENE_FILE = "GenesFileName";
    private static final Logger log = LoggerFactory.getLogger(BC2GMFormatWriter.class);
    private Matcher wsMatcher;

    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, description = "The directory to store the sentence and gene annotation files.")
    private File outputDir;
    @ConfigurationParameter(name = PARAM_SENTENCES_FILE, description = "The name of the file that will contain the sentences, one per line.")
    private String sentencesFile;
    @ConfigurationParameter(name = PARAM_GENE_FILE, description = "The name of the file that will contain the gene mention offsets for each sentence.")
    private String genesFile;


    private BufferedWriter sentenceWriter;
    private BufferedWriter genesWriter;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        outputDir = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR));
        sentencesFile = (String) aContext.getConfigParameterValue(PARAM_SENTENCES_FILE);
        genesFile = (String) aContext.getConfigParameterValue(PARAM_GENE_FILE);

        try {
            sentenceWriter = FileUtilities.getWriterToFile(Path.of(outputDir.getAbsolutePath(), sentencesFile).toFile());
            genesWriter = FileUtilities.getWriterToFile(Path.of(outputDir.getAbsolutePath(), genesFile).toFile());
        } catch (IOException e) {
            log.error("IO error when trying to open the output files", e);
        }

        wsMatcher = Pattern.compile("\\s").matcher("");

        log.info("{}: {}", PARAM_OUTPUT_DIR, outputDir);
        log.info("{}: {}", PARAM_SENTENCES_FILE, sentencesFile);
        log.info("{}: {}", PARAM_GENE_FILE, genesFile);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            String docId = JCoReTools.getDocId(aJCas);
            int sentNum = 0;
            FSIterator<Sentence> sentIt = aJCas.getAnnotationIndex(Sentence.class).iterator();
            AnnotationIndex<Gene> geneIndex = aJCas.getAnnotationIndex(Gene.class);
            while (sentIt.hasNext()) {
                Sentence sentence = sentIt.next();

                String sentId = docId + ":" + sentNum++;
                String coveredText = sentence.getCoveredText();
                sentenceWriter.write(sentId + " " + coveredText);
                sentenceWriter.newLine();

                TreeMap<Integer, Integer> wsNumMap = buildWSMap(coveredText);

                FSIterator<Gene> tokenIt = geneIndex.subiterator(sentence);
                while (tokenIt.hasNext()) {
                    Gene gene = tokenIt.next();
                    int begin = gene.getBegin() - sentence.getBegin();
                    int end = gene.getEnd() - sentence.getBegin();
                    int beginWOWs = begin - wsNumMap.floorEntry(begin).getValue();
                    // -1 because BC2 offsets are character positions
                    int endWOWs = (end - wsNumMap.floorEntry(end).getValue()) - 1;
                    String entry = sentId + "|" + beginWOWs + " " + endWOWs + "|" + gene.getCoveredText();
                    genesWriter.write(entry);
                    genesWriter.newLine();
                }
            }
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            sentenceWriter.close();
            genesWriter.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    public TreeMap<Integer, Integer> buildWSMap(String text) {
        TreeMap<Integer, Integer> wsNumMap = new TreeMap<>();
        String sentenceText = text;
        int pos = 0;
        int numWs = 0;
        wsMatcher.reset(sentenceText);
        wsNumMap.put(0, 0);
        while (wsMatcher.find(pos)) {
            pos = wsMatcher.end();
            wsNumMap.put(pos, ++numWs);
        }
        return wsNumMap;
    }

}
