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
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NXMLURIIterator implements Iterator<URI> {
    private final static Logger log = LoggerFactory.getLogger(NXMLURIIterator.class);
    private final File basePath;
    private final boolean searchRecursively;
    private final boolean searchZip;
    private BlockingQueue<URI> uris = new ArrayBlockingQueue<>(500);
    private URI currentUri;
    private Set<String> whitelist;
    private boolean fileSearchRunning = false;

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
        if (!fileSearchRunning) {
            // The beginning: The currentDirectory is null and we start at
            // the given path (which actually might be a single file to
            // read).
            log.debug("Starting background thread to search for PMC (.nxml) files at {}", basePath);
            CompletableFuture.runAsync(() -> setFilesAndSubDirectories(basePath, false));
            fileSearchRunning = true;
        }
        try {
            if (uris != null && currentUri == null) {
                log.trace("Waiting for the next URI");
                currentUri = uris.take();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted exception while waiting for the next URI from the list.");
            throw new UncheckedPmcReaderException(e);
        }
        if (currentUri != null && currentUri.toString().equals("http://nonsense.non")) {
            currentUri = null;
            uris = null;
        }
        return currentUri != null;
    }

    private void setFilesAndSubDirectories(File directory, boolean recursiveCall) {
        log.debug("Reading path {}", directory);
        Deque<File> pendingSubdirs = new ArrayDeque<>();
        if (directory.isDirectory() || isZipFile(directory)) {
            if ((searchRecursively || directory.equals(basePath)) && !isZipFile(directory)) {
                log.debug("Identified {} as a directory, reading files and subdirectories", directory);
                // set the files in the directory
                for (File file : directory.listFiles(f -> f.isFile() && f.getName().contains(".nxml") && !isZipFile(f) && isInWhitelist(f))) {
                    URI toURI = file.toURI();
                    try {
                        uris.put(toURI);
                    } catch (InterruptedException e) {
                        log.error("The PMC file reading process was interrupted while trying to put the NXML file URIs of directory {} into the list", directory);
                        throw new UncheckedPmcReaderException(e);
                    }
                }
                Stream.of(directory.listFiles(f -> f.isDirectory())).forEach(pendingSubdirs::push);
                if (searchZip)
                    Stream.of(directory.listFiles(f -> f.isFile() && isZipFile(f))).forEach(pendingSubdirs::push);
            } else if (searchZip && isZipFile(directory)) {
                log.debug("Identified {} as a ZIP archive, retrieving its inventory", directory);
                log.debug("Searching ZIP archive {} for eligible documents", directory);
                try (FileSystem fs = FileSystems.newFileSystem(directory.toPath(), null)) {
                    Iterable<Path> rootDirectories = fs.getRootDirectories();
                    for (Path rootDir : rootDirectories) {
                        Stream<Path> walk = Files.walk(rootDir);
                        walk.filter(Files::isRegularFile).forEach(p -> {
                            if (p.getFileName().toString().contains(".nxml") && isInWhitelist(p)) {
                                try {
                                    uris.put(p.toUri());
                                } catch (InterruptedException e) {
                                    log.error("The PMC file reading process was interrupted while trying to put the next ZIP NXML URI into the list");
                                    throw new UncheckedPmcReaderException(e);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    log.error("Could not read from {}", directory);
                    throw new UncheckedPmcReaderException(e);
                }
            } else {
                log.debug("Recursive search is deactivated, skipping subdirectory {}", directory);
            }
        } else if (directory.isFile()) {
            log.debug("Identified {} as a file, reading single file", directory);
            log.debug("Adding file to map with key {}", directory);
            try {
                uris.put(directory.toURI());
            } catch (InterruptedException e) {
                log.error("The PMC file reading process was interrupted while trying to put file URI {} into the list", directory.toURI());
                throw new UncheckedPmcReaderException(e);
            }
        } else {
            throw new IllegalStateException("Path " + directory.getAbsolutePath()
                    + " was identified neither a path nor a file, cannot continue. This seems to be a bug in this code.");
        }
        while (!pendingSubdirs.isEmpty()) {
            setFilesAndSubDirectories(pendingSubdirs.pop(), true);
        }
        if (!recursiveCall) {
            try {
                uris.put(URI.create("http://nonsense.non"));
            } catch (InterruptedException e) {
                log.error("The PMC file reading process was interrupted while trying to put the ending signal into the list");
                throw new UncheckedPmcReaderException(e);
            }
        }
    }

    private boolean isZipFile(File directory) {
        return directory.getName().toLowerCase().endsWith(".zip");
    }

    private boolean isInWhitelist(Path path) {
        return isInWhitelist(path.toString().substring(path.toString().lastIndexOf('/') + 1, path.toString().indexOf('.')));
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
        URI ret = currentUri;
        currentUri = null;
        return ret;
    }

}


