package org.ulpgc.bigdata.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Capa de control del pipeline.
 * <p>
 * Supervisa qué libros han sido descargados (Paso 1) y cuáles ya han sido
 * indexados (Paso 3), persistiendo el estado en dos ficheros de texto plano
 * (uno ID de libro por línea). Esto evita repetir descargas o indexaciones
 * si el proceso se relanza (idempotencia / recovery mode).
 */
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

    /** Carga los IDs existentes en el fichero de control, o un set vacío si no existe aún. */
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

    /** Marca un libro como descargado. No hace nada si ya estaba marcado (evita duplicados). */
    public synchronized void markDownloaded(int bookId) {
        if (downloadedBooks.add(bookId)) {
            appendId(downloadedFile, bookId);
        }
    }

    /** Marca un libro como indexado. No hace nada si ya estaba marcado. */
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

    /** Libros que están descargados pero todavía no indexados: el trabajo pendiente para el Paso 3. */
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
