package de.julielab.jcore.reader.pmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.pmc.parser.DocumentParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingResult;
import de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser;
import de.julielab.jcore.reader.pmc.parser.ParsingResult;
import de.julielab.jcore.reader.pmc.parser.TextParsingResult;

public class PMCReader extends CollectionReader_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(PMCReader.class);

	public static final String PARAM_INPUT = "Input";

	@ConfigurationParameter(name = PARAM_INPUT, mandatory = true, description = "The path to an NXML file or a directory with NXML files and possibly subdirectories holding more NXML files.")
	private File input;

	private Iterator<File> pmcFiles;

	private long completed;

	private NxmlDocumentParser nxmlDocumentParser;

	@Override
	public void initialize() throws ResourceInitializationException {
		input = new File((String) getConfigParameterValue(PARAM_INPUT));
		log.info("Reading PubmedCentral NXML file(s) from {}", input);
		try {
			pmcFiles = getPmcFiles(input);
		} catch (FileNotFoundException e) {
			throw new ResourceInitializationException(e);
		}
		completed = 0;
		nxmlDocumentParser = new NxmlDocumentParser();
		try {
			nxmlDocumentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void getNext(CAS cas) throws IOException, CollectionException {
		try {
			File next = pmcFiles.next();
			log.trace("Now reading file {}", next);
			nxmlDocumentParser.reset(next, cas.getJCas());
			ElementParsingResult result = nxmlDocumentParser.parse();
			StringBuilder sb = populateCas(result, cas, new StringBuilder());
			cas.setDocumentText(sb.toString());
		} catch (CASException | DocumentParsingException | ElementParsingException e) {
			throw new CollectionException(e);
		}
		completed++;
	}

	private StringBuilder populateCas(ParsingResult result, CAS cas, StringBuilder sb) {
		switch (result.getResultType()) {
		case ELEMENT:
			ElementParsingResult elementParsingResult = (ElementParsingResult) result;
			String elementName = elementParsingResult.getElementName();
			boolean isBlockElement = elementParsingResult.isBlockElement() || (boolean) nxmlDocumentParser
					.getTagProperties(elementName).getOrDefault(ElementProperties.BLOCK_ELEMENT, false);

			// There are elements that should have line breaks before and after
			// them like paragraphs, sections, captions etc. Other elements are
			// inline-elements, like xref, which should be embedded in the
			// surrounding text without line breaks.
			if (isBlockElement && sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
				sb.append("\n");
			}

			int begin = sb.length();
			for (ParsingResult subResult : elementParsingResult.getSubResults()) {
				populateCas(subResult, cas, sb);
			}
			int end = sb.length();

			// There are elements that should have line breaks before and after
			// them like paragraphs, sections, captions etc. Other elements are
			// inline-elements, like xref, which should be embedded in the
			// surrounding text without line breaks.
			if (isBlockElement && sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
				sb.append("\n");
			}
			Annotation annotation = elementParsingResult.getAnnotation();
			// if no annotation should be created, the parser is allowed to
			// return null
			if (annotation != null) {
				annotation.setBegin(begin);
				annotation.setEnd(end);
				if (elementParsingResult.addAnnotationToIndexes())
					annotation.addToIndexes();
			}
			break;
		case TEXT:
			TextParsingResult textParsingResult = (TextParsingResult) result;
			sb.append(textParsingResult.getText());
			break;
		case NONE:
			// do nothing
			break;
		}
		return sb;
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return pmcFiles.hasNext();
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new Progress() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 6058019619024287436L;

			@Override
			public boolean isApproximate() {
				return true;
			}

			@Override
			public String getUnit() {
				return "files";
			}

			@Override
			public long getTotal() {
				return -1;
			}

			@Override
			public long getCompleted() {
				return completed;
			}
		} };
	}

	@Override
	public void close() throws IOException {
		pmcFiles = null;
	}

	private Iterator<File> getPmcFiles(final File path) throws FileNotFoundException {
		if (!path.exists())
			throw new FileNotFoundException("The path " + path.getAbsolutePath() + " does not exist.");
		return new Iterator<File>() {

			private File currentDirectory;
			private LinkedHashMap<File, Stack<File>> filesMap = new LinkedHashMap<>();
			private LinkedHashMap<File, Stack<File>> subDirectoryMap = new LinkedHashMap<>();

			@Override
			public boolean hasNext() {
				// The beginning: The currentDirectory is null and we start at
				// the given path (which actually might be a single file to
				// read).
				if (currentDirectory == null) {
					currentDirectory = path;
					setFilesAndSubDirectories(currentDirectory);
				}
				Stack<File> filesInCurrentDirectory = filesMap.get(currentDirectory);
				log.trace("Got files for key {}: {}", currentDirectory, filesInCurrentDirectory);
				if (!filesInCurrentDirectory.isEmpty())
					return true;
				else if (currentDirectory.isFile())
					// If the path points to a file an no files are left, then
					// the one file has already been read. We are finished.
					return false;
				else {
					Stack<File> subDirectories = subDirectoryMap.get(currentDirectory);
					log.trace("No more files in current directory, got subdirectories left for key {}: {}",
							currentDirectory, subDirectories);
					if (!subDirectories.isEmpty()) {
						File subDirectory = subDirectories.pop();
						log.trace("Moving to subdirectory {}", subDirectory);

						setFilesAndSubDirectories(subDirectory);

						// move to the new subdirectory
						currentDirectory = subDirectory;

						// we call hasNext() again because the new current
						// directory could be empty
						return hasNext();
					}
					// there is no subdirectory left
					// if we are in the root path, we have read everything and
					// are finished
					if (currentDirectory.equals(path))
						return false;
					// If we are not in the root path, we are beneath it. Go up
					// one directory and check if there is still something to do
					currentDirectory = currentDirectory.getParentFile();
					return hasNext();
				}
			}

			private void setFilesAndSubDirectories(File directory) {
				log.debug("Reading path {}", directory);
				if (directory.isDirectory()) {
					log.debug("Identified {} as a directory, reading files and subdirectories", directory);
					// set the files in the directory
					Stack<File> filesInSubDirectory = new Stack<File>();
					Stream.of(directory.listFiles(f -> f.isFile() && f.getName().contains(".nxml")))
							.forEach(filesInSubDirectory::push);
					filesMap.put(directory, filesInSubDirectory);

					// set the subdirectories of the directory
					Stack<File> directoriesInSubDirectory = new Stack<File>();
					Stream.of(directory.listFiles(f -> f.isDirectory())).forEach(directoriesInSubDirectory::push);
					subDirectoryMap.put(directory, directoriesInSubDirectory);
				} else if (directory.isFile()) {
					log.debug("Identified {} as a file, reading single file", directory);
					Stack<File> fileStack = new Stack<File>();
					fileStack.push(directory);
					log.debug("Adding file to map with key {}", directory);
					filesMap.put(directory, fileStack);
				} else {
					throw new IllegalStateException("Path " + directory.getAbsolutePath()
							+ " was identified neither a path nor a file, cannot continue. This seems to be a bug in this code.");
				}
			}

			@Override
			public File next() {
				if (!hasNext())
					return null;
				return filesMap.get(currentDirectory).pop();
			}

		};
	}

}
