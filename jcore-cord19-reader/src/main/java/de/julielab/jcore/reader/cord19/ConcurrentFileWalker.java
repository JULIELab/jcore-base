package de.julielab.jcore.reader.cord19;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

public class ConcurrentFileWalker extends Thread {
    private Path inputDir;
    private Cord19FileVisitor fileVisitor;

    public ConcurrentFileWalker(Path inputDir) {
        this.inputDir = inputDir;
        setName("CORD-19-Reader-File-Walker");
    }

    @Override
    public void run() {
        try {
            fileVisitor = new Cord19FileVisitor();
            Files.walkFileTree(inputDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, fileVisitor);
            fileVisitor.walkFinished();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Returns a number of files restricted by <tt>maxFiles</tt>.</p>
     * <p>Will return a list with the requested files or <tt>null</tt> if the file walk has ended and there are
     * no files left.</p>
     * @param maxFiles
     * @return
     */
    public List<Path> getFiles(int maxFiles) {
        return fileVisitor.getFiles(maxFiles);
    }
}
