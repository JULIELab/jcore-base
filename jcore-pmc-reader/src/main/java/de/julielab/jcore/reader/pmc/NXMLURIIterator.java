package de.julielab.jcore.reader.pmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Searches over directories and, optionally, the contents of ZIP archives for files with an (n)xml extension.
 * Returns URIs that either point to single files or to entries into ZIP archives. Both can equally be accessed via
 * "uri.toURL().openStream()" which is done in the NxmlDocumentParser.
 */
public class NXMLURIIterator implements Iterator<URI> {
    private final static Logger log = LoggerFactory.getLogger(NXMLURIIterator.class);
    private final static Logger logFileSearch = LoggerFactory.getLogger(NXMLURIIterator.class.getCanonicalName() + ".FileSearch");
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
            log.debug("Starting background thread to search for PMC (.xml) files at {}", basePath);
            CompletableFuture.runAsync(() -> setFilesAndSubDirectories(basePath, false));
            fileSearchRunning = true;
        }
        try {
            if (uris != null && currentUri == null) {
                log.trace("Waiting for the next URI");
                currentUri = uris.take();
                log.trace("Got URI {} from the file list. {} URIs currently remain in the queue.", currentUri, uris.size());
            }
        } catch (InterruptedException e) {
            log.error("Interrupted exception while waiting for the next URI from the list.");
            throw new UncheckedPmcReaderException(e);
        }
        if (currentUri == null || currentUri.toString().equals("http://nonsense.non")) {
            log.debug("Retrieved URI {}, assuming no more files available.", currentUri);
            currentUri = null;
            uris = null;
        }
        return currentUri != null;
    }

    private void setFilesAndSubDirectories(File directory, boolean recursiveCall) {
        logFileSearch.debug("Reading path {}", directory);
        Deque<File> pendingSubdirs = new ArrayDeque<>();
        logFileSearch.trace("Checking if {} is eligible for PMC file search", directory);
        if (directory.isDirectory() || isZipFile(directory)) {
            if ((searchRecursively || directory.equals(basePath)) && !isZipFile(directory)) {
                logFileSearch.debug("Identified {} as a directory, reading files and subdirectories", directory);
                // set the files in the directory
                for (File file : directory.listFiles(f -> f.isFile() && f.getName().endsWith("xml") && !isZipFile(f) && isInWhitelist(f))) {
                    URI toURI = file.toURI();
                    try {
                        uris.put(toURI);
                    } catch (InterruptedException e) {
                        logFileSearch.error("The PMC file reading process was interrupted while trying to put the NXML file URIs of directory {} into the list", directory);
                        throw new UncheckedPmcReaderException(e);
                    }
                }
                // Save the subdirectories and potentially ZIP files for a recursive reading call further below
                Stream.of(directory.listFiles(f -> f.isDirectory())).forEach(pendingSubdirs::push);
                if (searchZip)
                    Stream.of(directory.listFiles(f -> f.isFile() && isZipFile(f))).forEach(pendingSubdirs::push);
                logFileSearch.trace("Added subdirectories and/or ZIP files to the list of pending directories and archives. There are now {} pending.", pendingSubdirs.size());
            } else if (searchZip && isZipFile(directory)) {
                logFileSearch.debug("Identified {} as a ZIP archive, retrieving its inventory", directory);
                logFileSearch.debug("Searching ZIP archive {} for eligible documents", directory);
                try (ZipFile zf = new ZipFile(directory)) {
                    final Enumeration<? extends ZipEntry> entries = zf.entries();
                    int numEntries = 0;
                    while (entries.hasMoreElements()) {
                        final ZipEntry e = entries.nextElement();
                        if (!e.isDirectory() && e.getName().contains(".nxml") && isInWhitelist(new File(e.getName()))) {
                            final String urlStr = "jar:" + directory.toURI() + "!/" + e.getName();
                            int exclamationIndex = urlStr.indexOf('!');
                            final String urlEncodedStr = urlStr.substring(0, exclamationIndex + 2) + Stream.of(urlStr.substring(exclamationIndex + 2).split("/")).map(x -> URLEncoder.encode(x, UTF_8)).collect(Collectors.joining("/"));
                            URL url = new URL(urlEncodedStr);
                            try {
                                final URI uri = url.toURI();
                                logFileSearch.trace("Waiting to put URI {} into queue", uri);
                                uris.put(uri);
                                ++numEntries;
                                logFileSearch.trace("Successfully put URI {} into queue. Queue size: {}", uri, uris.size());
                            } catch (InterruptedException e1) {
                                logFileSearch.error("Putting URI for URL {} into the queue was interrupted", url);
                                throw new UncheckedPmcReaderException(e1);
                            } catch (URISyntaxException e1) {
                                logFileSearch.error("Could not convert URL {} to URI.", url, e);
                                throw new UncheckedPmcReaderException(e1);
                            }
                        }
                    }
                    logFileSearch.trace("Finished retrieving files from ZIP archive {}. {} eligible documents were read.", directory, numEntries);
                } catch (IOException e) {
                    logFileSearch.error("Could not read from {}", directory);
                    throw new UncheckedPmcReaderException(e);
                } catch (Throwable t) {
                    logFileSearch.error("Unexpected error:", t);
                }
            } else {
                logFileSearch.debug("Recursive search is deactivated, skipping subdirectory {}", directory);
            }
        } else if (directory.isFile()) {
            logFileSearch.debug("Identified {} as a file, reading single file", directory);
            logFileSearch.debug("Adding file to map with key {}", directory);
            try {
                uris.put(directory.toURI());
            } catch (InterruptedException e) {
                logFileSearch.error("The PMC file reading process was interrupted while trying to put file URI {} into the list", directory.toURI());
                throw new UncheckedPmcReaderException(e);
            }
        } else {
            throw new IllegalStateException("Path " + directory.getAbsolutePath()
                    + " was identified neither a path nor a file, cannot continue. This seems to be a bug in this code.");
        }
        logFileSearch.trace("Checking if subdirectories of {} are to processed for PMC file search", directory);
        while (!pendingSubdirs.isEmpty()) {
            final File subdir = pendingSubdirs.pop();
            logFileSearch.trace("Descending into ZIP file or directory {} in search for PMC files.", subdir);
            setFilesAndSubDirectories(subdir, true);
            logFileSearch.trace("Subdir or ZIP {} finished for file PMC file search.", subdir);
        }
        logFileSearch.trace("Checking whether the end signal is to be sent");
        if (!recursiveCall) {
            try {
                logFileSearch.info("Reached the end of the eligible files, background thread for file collection is giving the end-of-files signal and terminates.");
                uris.put(URI.create("http://nonsense.non"));
            } catch (InterruptedException e) {
                logFileSearch.error("The PMC file reading process was interrupted while trying to put the ending signal into the list");
                throw new UncheckedPmcReaderException(e);
            }
        }
        logFileSearch.trace("A file search method call for {} has finished. This was a {} call.", directory, (recursiveCall ? "recursive" : "non-recursive"));
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
            logFileSearch.trace("Skipping document with name/id {} because it is not contained in the white list.", name);
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


