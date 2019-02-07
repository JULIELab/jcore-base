package de.julielab.jcore.consumer.bc2gmformat;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.utility.JCoReTools;

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


    private FileOutputStream sentenceStream;
    private FileOutputStream genesStream;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        outputDir = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR));
        sentencesFile = (String) aContext.getConfigParameterValue(PARAM_SENTENCES_FILE);
        genesFile = (String) aContext.getConfigParameterValue(PARAM_GENE_FILE);

        try {
            sentenceStream = new FileOutputStream(outputDir.getAbsolutePath() + File.separatorChar + sentencesFile);
            genesStream = new FileOutputStream(outputDir.getAbsolutePath() + File.separatorChar + genesFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
                IOUtils.write(sentId + " " + coveredText + "\n", sentenceStream, "UTF-8");

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
                    IOUtils.write(entry + "\n", genesStream, "UTF-8");
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
            sentenceStream.close();
            genesStream.close();
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
