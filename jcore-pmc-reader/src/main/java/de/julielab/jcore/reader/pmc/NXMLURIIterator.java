package de.julielab.jcore.reader.pmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NXMLURIIterator implements Iterator<URI> {
    private final static Logger log = LoggerFactory.getLogger(NXMLURIIterator.class);
    private final File basePath;
    private final boolean searchRecursively;
    private final boolean searchZip;
    private File currentDirectory;
    private LinkedHashMap<File, Stack<URI>> filesMap = new LinkedHashMap<>();
    private LinkedHashMap<File, Stack<File>> subDirectoryMap = new LinkedHashMap<>();
    private Stack<URI> EMPTY_URI_STACK = new Stack<>();
    private Stack<File> EMPTY_FILE_STACK = new Stack<>();
    private Set<String> whitelist;

    public NXMLURIIterator(File basePath, Set<String> whitelist, boolean searchRecursively, boolean searchZip) throws FileNotFoundException {
        this.whitelist = whitelist != null ? whitelist : new HashSet<>(Collections.singletonList("all"));
        if (!basePath.exists())
            throw new FileNotFoundException("The path " + basePath.getAbsolutePath() + " does not exist.");
        this.basePath = basePath;
        this.searchRecursively = searchRecursively;
        this.searchZip = searchZip;
    }

    @Override
    public boolean hasNext() {
        // The beginning: The currentDirectory is null and we start at
        // the given path (which actually might be a single file to
        // read).
        if (currentDirectory == null) {
            currentDirectory = basePath;
            setFilesAndSubDirectories(currentDirectory);
        }
        Stack<URI> filesInCurrentDirectory = filesMap.get(currentDirectory);
        if (!filesInCurrentDirectory.isEmpty())
            return true;
        else if (currentDirectory.isFile())
            // If the path points to a file an no files are left, then
            // the one file has already been read. We are finished.
            return false;
        else {
            Stack<File> subDirectories = subDirectoryMap.get(currentDirectory);
            log.trace("No more files in current directory {}", currentDirectory);
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
            if (currentDirectory.equals(basePath))
                return false;
            // If we are not in the root path, we are beneath it. Go up
            // one directory and check if there is still something to do
            currentDirectory = currentDirectory.getParentFile();
            return hasNext();
        }
    }

    private void setFilesAndSubDirectories(File directory) {
        log.debug("Reading path {}", directory);
        if (directory.isDirectory() || isZipFile(directory)) {
            if ((searchRecursively || directory.equals(basePath)) && !isZipFile(directory)) {
                log.debug("Identified {} as a directory, reading files and subdirectories", directory);
                // set the files in the directory
                Stack<URI> filesInSubDirectory = new Stack<>();
                Stream.of(directory.listFiles(f -> f.isFile() && f.getName().contains(".nxml") && !isZipFile(f) && isInWhitelist(f))).map(File::toURI)
                        .forEach(filesInSubDirectory::push);
                filesMap.put(directory, filesInSubDirectory);

                // set the subdirectories of the directory
                Stack<File> directoriesInSubDirectory = new Stack<>();
                Stream.of(directory.listFiles(f -> f.isDirectory())).forEach(directoriesInSubDirectory::push);
                if (searchZip)
                    Stream.of(directory.listFiles(f -> f.isFile() && isZipFile(f))).forEach(directoriesInSubDirectory::push);
                subDirectoryMap.put(directory, directoriesInSubDirectory);
            } else if (searchZip && isZipFile(directory)) {
                log.debug("Identified {} as a ZIP archive, retrieving its inventory", directory);
                Stack<URI> filesInZip = new Stack<>();
                log.debug("Searching ZIP archive {} for eligible documents", directory);
                try (FileSystem fs = FileSystems.newFileSystem(directory.toPath(), null)) {
                    Iterable<Path> rootDirectories = fs.getRootDirectories();
                    for (Path rootDir : rootDirectories) {
                        Stream<Path> walk = Files.walk(rootDir);
                        walk.filter(Files::isRegularFile).forEach(p -> {
                            if (p.getFileName().toString().contains(".nxml") && isInWhitelist(p)) {
                                filesInZip.add(p.toUri());
                            }
                        });
                    }
                    filesMap.put(directory, filesInZip);
                } catch (IOException e) {
                    log.error("Could not read from {}", directory);
                    throw new UncheckedPmcReaderException(e);
                }
            } else {
                filesMap.put(directory, EMPTY_URI_STACK);
                subDirectoryMap.put(directory, EMPTY_FILE_STACK);
                log.debug("Recursive search is deactivated, skipping subdirectory {}", directory);
            }
        } else if (directory.isFile()) {
            log.debug("Identified {} as a file, reading single file", directory);
            Stack<URI> fileStack = new Stack<>();
            fileStack.push(directory.toURI());
            log.debug("Adding file to map with key {}", directory);
            filesMap.put(directory, fileStack);
        } else {
            throw new IllegalStateException("Path " + directory.getAbsolutePath()
                    + " was identified neither a path nor a file, cannot continue. This seems to be a bug in this code.");
        }
    }

    private boolean isZipFile(File directory) {
        return directory.getName().toLowerCase().endsWith(".zip");
    }

    private boolean isInWhitelist(Path path) {
        return isInWhitelist(path.toString().substring(path.toString().lastIndexOf('/')+1, path.toString().indexOf('.')));
    }

    private boolean isInWhitelist(File file) {
        return isInWhitelist(file.getName().substring(0, file.getName().indexOf('.')));
    }

    private boolean isInWhitelist(String name) {
        boolean inWhitelist = whitelist.contains(name) || (whitelist.size() == 1 && whitelist.contains("all"));
        if (!inWhitelist)
            log.trace("Skipping document with name/id {} because it is not contained in the white list.", name);
        return inWhitelist;
    }

    @Override
    public URI next() {
        if (!hasNext())
            return null;
        return filesMap.get(currentDirectory).pop();
    }

}


