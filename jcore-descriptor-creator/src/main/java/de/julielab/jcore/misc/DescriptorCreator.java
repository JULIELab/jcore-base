package de.julielab.jcore.misc;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.julielab.java.utilities.FileUtilities;

public class DescriptorCreator {


    public static final String DEFAULT_OUTPUT_ROOT = "src" + File.separator + "main" + File.separator + "resources";
    private static final Logger log = LoggerFactory.getLogger(DescriptorCreator.class);
    private static final String DESC = "desc";

    public static void main(String[] args) throws Exception {
        DescriptorCreator creator = new DescriptorCreator();
        creator.run();
    }

    public static String getComponentName() {
        return new File(".").getAbsoluteFile().getParentFile().getName();
    }

    public void run() throws Exception {
        run(DEFAULT_OUTPUT_ROOT);
    }

    public void run(String outputRoot) throws Exception {
        List<Class<? extends CollectionReader>> readers;
        List<Class<? extends AnalysisComponent>> aes;
        readers = findSubclasses(CollectionReader.class.getCanonicalName());
        aes = findSubclasses(AnalysisComponent.class.getCanonicalName());

        readers = readers.stream().filter(c -> c.getPackage().getName().contains("de.julielab.jcore.reader"))
                .collect(toList());
        // Since consumers and also multipliers can be or are AnalysisComponents, were may list all component categories here.
        // Also, remove abstract classes
        aes = aes.stream().filter(c -> !Modifier.isAbstract(c.getModifiers())).
                filter(c -> c.getPackage().getName().contains("de.julielab.jcore.ae")
                        || c.getPackage().getName().contains("de.julielab.jcore.consumer")
                        || c.getPackage().getName().contains("de.julielab.jcore.multiplier")
                        || c.getPackage().getName().contains("de.julielab.jcore.reader")).collect(toList());

        if (readers.isEmpty() && aes.isEmpty()) {
            log.warn("No JCoRe UIMA component classes were found.");
        } else {
            Stream<String> typeDescNamesStream = Stream.of(TypeSystemDescriptionFactory.scanTypeDescriptors()).
                            filter(loc -> loc.contains("de/julielab/jcore/types")).
                    // remove the .xml extension
                            map(loc -> loc.substring(0, loc.length() - 4)).
                    // make path/to/descriptor/de/julielab/... to path.to.descriptor.de.julielab....
                            map(loc -> loc.replaceAll("/", ".")).
                    // remove everything before "de.julielab.jcore.types"
                            map(loc -> loc.substring(loc.indexOf("de.julielab.jcore.types")));
            TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription(typeDescNamesStream.toArray(String[]::new));
            for (Class<? extends CollectionReader> cls : readers) {
                CollectionReaderDescription d = CollectionReaderFactory.createReaderDescription(cls, tsd);
                writeComponentDescriptor(outputRoot, cls, d, "collection reader");
            }
            for (Class<? extends AnalysisComponent> cls : aes) {
                AnalysisEngineDescription d = AnalysisEngineFactory.createEngineDescription(cls, tsd);
                writeComponentDescriptor(outputRoot, cls, d, "analysis engine / consumer");
            }
        }
    }

    private <T> List<Class<? extends T>> findSubclasses(String interfaceName) {
        List<Class<? extends T>> components;
        // consumers are also analysis components
        components = new ArrayList<>();
        ClassGraph graph = new ClassGraph();
        graph.enableClassInfo();
        final ScanResult scan = graph.scan();
        scan.getClassesImplementing(interfaceName).stream().map(info -> info.loadClass()).forEach(cls -> components.add((Class<? extends T>) cls));
        return components;
    }

    private void writeComponentDescriptor(String outputRoot, Class<?> cls, ResourceCreationSpecifier d,
                                          String componentType) throws SAXException, IOException {
        String componentName = d.getImplementationName();
        if (StringUtils.isBlank(componentName))
            componentName = getComponentName();
        String filename = componentName;
        filename += ".xml";
        List<String> pathElements = Arrays.asList(outputRoot,
                cls.getPackage().getName().replaceAll("\\.", File.separator), DESC, filename);
        File outputPath = new File(pathElements.stream().collect(joining(File.separator)));
        if (!outputPath.getParentFile().exists())
            outputPath.getParentFile().mkdirs();
        log.info("Writing {} descriptor from class {} to {}", componentType, cls, outputPath);
        try (OutputStream w = new ByteArrayOutputStream()) {
            d.toXML(w);
            final String s = new String(((ByteArrayOutputStream) w).toByteArray(), StandardCharsets.UTF_8);
            try (Writer wr = FileUtilities.getWriterToFile(outputPath); BufferedReader r = new BufferedReader(new StringReader(s))) {
                final String descriptorWOEmptyLines = r.lines().filter(l -> !l.trim().isEmpty()).collect(joining(System.getProperty("line.separator")));
                wr.write(descriptorWOEmptyLines);
            }
        }
    }
}
