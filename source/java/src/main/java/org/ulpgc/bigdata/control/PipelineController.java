package org.ulpgc.bigdata.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class PipelineController {

    private final Path downloadedFile;
    private final Path indexedFile;

    private final Set<Integer> downloadedBooks;
    private final Set<Integer> indexedBooks;

    public PipelineController(String downloadedFilePath, String indexedFilePath) {
        this.downloadedFile = Path.of(downloadedFilePath);
        this.indexedFile = Path.of(indexedFilePath);

        this.downloadedBooks = loadIds(downloadedFile);
        this.indexedBooks = loadIds(indexedFile);
    }

    private Set<Integer> loadIds(Path file) {
        Set<Integer> ids = new HashSet<>();
        if (!Files.exists(file)) {
            return ids;
        }
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    ids.add(Integer.parseInt(line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo fichero de control " + file + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Entrada inválida en fichero de control " + file + ": " + e.getMessage());
        }
        return ids;
    }

    public synchronized void markDownloaded(int bookId) {
        if (downloadedBooks.add(bookId)) {
            appendId(downloadedFile, bookId);
        }
    }

    public synchronized void markIndexed(int bookId) {
        if (indexedBooks.add(bookId)) {
            appendId(indexedFile, bookId);
        }
    }

    public boolean isDownloaded(int bookId) {
        return downloadedBooks.contains(bookId);
    }

    public boolean isIndexed(int bookId) {
        return indexedBooks.contains(bookId);
    }

    public List<Integer> getBooksPendingIndexing() {
        return downloadedBooks.stream()
                .filter(id -> !indexedBooks.contains(id))
                .sorted()
                .collect(Collectors.toList());
    }

    public Set<Integer> getDownloadedBooks() {
        return Set.copyOf(downloadedBooks);
    }

    public Set<Integer> getIndexedBooks() {
        return Set.copyOf(indexedBooks);
    }

    private void appendId(Path file, int bookId) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(
                    file,
                    bookId + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Error escribiendo en fichero de control " + file + ": " + e.getMessage());
        }
    }
}
