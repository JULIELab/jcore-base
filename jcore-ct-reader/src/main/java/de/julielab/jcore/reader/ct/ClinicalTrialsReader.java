package de.julielab.jcore.reader.ct;

import de.julielab.jcore.reader.ct.ctmodel.ClinicalTrial;
import de.julielab.jcore.types.Keyword;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
import de.julielab.jcore.types.ct.Header;
import de.julielab.jcore.types.MeshHeading;
import de.julielab.jcore.types.ct.*;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@ResourceMetaData(name="JCoRe Clinical Trials Reader", description = "This component reads the XML format provided by ClinicalTrials.gov. To this end, the JCoRe type system contains a number of types specifically created for this kind of document. Note that the CAS text created by this reader might be confusing without checking the corresponding annotations. This is due to the fact that the CT XML contains multiple enumerations which are not very well reflected in plain text. Also, enumerations with subitems, such as the outcomes, are not displayed in the expected groups of items. Instead, each item type is displayed separately. This could be changed, if necessary. Since all items are correctly annotated by their category, this might not even be an issue, depending on the downstream tasks.")
public class ClinicalTrialsReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDirectory";
    public static final String PARAM_FILES = "FileNames";
    private final static Logger log = LoggerFactory.getLogger(ClinicalTrialsReader.class);
    @ConfigurationParameter(name = PARAM_INPUT_DIR)
    private File inputDirectory;
    @ConfigurationParameter(name = PARAM_FILES, mandatory = false, description = "For debugging: Restrict the documents read to the given document file names.")
    private String[] fileNames;

    private Iterator<File> files;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        inputDirectory = new File((String) context.getConfigParameterValue(PARAM_INPUT_DIR));
        fileNames = (String[]) context.getConfigParameterValue(PARAM_FILES);
        try {
            files = readFiles(inputDirectory);
        } catch (IOException e) {
            log.error("Could not read clinical trials files", e);
            throw new ResourceInitializationException(e);
        }
        log.info("{}: {}", PARAM_INPUT_DIR, inputDirectory);
        log.info("{}: {}", PARAM_FILES, fileNames);
    }

    private Iterator<File> readFiles(File inputDirectory) throws IOException {
        List<File> files = new ArrayList<>(250000);
        Stream<Path> pathStream = Files.walk(inputDirectory.toPath())
                .filter(Files::isRegularFile);
        if (fileNames != null && fileNames.length > 0) {
            final Set<String> idset = new HashSet<>(Arrays.asList(fileNames));
            pathStream = pathStream.filter(f -> idset.contains(f.toFile().getName()));
        }
        pathStream.forEach(f ->
                files.add(f.toFile())
        );
        return files.iterator();
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void getNext(JCas jCas) {
        if (files.hasNext()) {
            final File file = files.next();
            try {
                StringBuilder sb = new StringBuilder();
                final ClinicalTrial ct = ClinicalTrial.fromXml(file.getAbsolutePath());
                addHeader(jCas, ct);
                addManualDescriptor(jCas, ct);
                addAnnotatedText(sb, ct.brief_title, new BriefTitle(jCas));
                addAnnotatedText(sb, ct.official_title, new OfficialTitle(jCas));
                addAnnotatedText(sb, ct.summary, new Summary(jCas));
                addAnnotatedText(sb, ct.description, new Description(jCas));
                for (int i = 0; i < ct.outcomeMeasures.size(); i++) {
                    String outcomeMeasure = ct.outcomeMeasures.get(i);
                    addAnnotatedText(sb, outcomeMeasure, new OutcomeMeasure(jCas));
                    if (i < ct.outcomeDescriptions.size()) {
                        String outcomeDescription = ct.outcomeDescriptions.get(i);
                        addAnnotatedText(sb, outcomeDescription, new OutcomeDescription(jCas));
                    }
                }
                for (String condition : ct.conditions)
                    addAnnotatedText(sb, condition, new Condition(jCas));
                for (int i = 0; i < ct.interventionTypes.size(); i++) {
                    String interventionType = ct.interventionTypes.get(i);
                    String interventionName = ct.interventionNames.get(i);
                    addAnnotatedText(sb, interventionType, new InterventionType(jCas));
                    addAnnotatedText(sb, interventionName, new InterventionName(jCas));
                }
                for (String armGroupDesc : ct.armGroupDescriptions)
                    addAnnotatedText(sb, armGroupDesc, new ArmGroupDescription(jCas));
                addAnnotatedText(sb, ct.inclusion, new Inclusion(jCas));
                addAnnotatedText(sb, ct.exclusion, new Exclusion(jCas));

                jCas.setDocumentText(sb.toString());
            } catch (Throwable t) {
                log.error("Exception occurred when reading file {}", file, t);
            }
        }
    }

    private void addManualDescriptor(JCas jCas, ClinicalTrial ct) {
        ManualDescriptor md = null;
        if (!ct.meshTags.isEmpty() || !ct.keywords.isEmpty())
            md = new ManualDescriptor(jCas);
        for (String meshTag : ct.meshTags) {
            final MeshHeading mh = new MeshHeading(jCas);
            mh.setDescriptorName(meshTag);
            md.setMeSHList(JCoReTools.addToFSArray(md.getMeSHList(), mh, 1));
        }
        for (String keyword : ct.keywords) {
            final Keyword kw = new Keyword(jCas);
            kw.setName(keyword);
            md.setKeywordList(JCoReTools.addToFSArray(md.getKeywordList(), kw, 1));
        }
        if (md != null)
            md.addToIndexes();
    }

    private Annotation addAnnotatedText(StringBuilder sb, String text, Annotation annotation) {
        annotation.setBegin(sb.length());
        sb.append(text);
        annotation.setEnd(sb.length());
        annotation.addToIndexes();
        sb.append(System.getProperty("line.separator"));
        return annotation;
    }


    private void addHeader(JCas jCas, ClinicalTrial ct) {
        final Header header = new Header(jCas);
        header.setDocId(ct.id);
        header.setStudyType(ct.studyType);
        header.setMinimumAge(ct.minAge);
        header.setMaximumAge(ct.maxAge);
        final StudyDesignInfo sdi = new StudyDesignInfo(jCas);
        sdi.setInterventionModel(ct.interventionModel);
        sdi.setPrimaryPurpose(ct.primaryPurpose);
        header.setStudyDesignInfo(sdi);
        if (ct.sex != null) {
            StringArray sex = new StringArray(jCas, 0);
            for (String s : ct.sex)
                sex = JCoReTools.addToStringArray(sex, s);
            header.setGender(sex);
        }
        header.addToIndexes();
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(0, 0, "TODO")};
    }

    @Override
    public boolean hasNext() {
        return files.hasNext();
    }

}
