package org.ulpgc.bigdata.datamarts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControlLayer {
    private static final String CONTROL_DIR = "control";
    private static final Path DOWNLOADS_FILE = Paths.get(CONTROL_DIR, "downloaded_books.txt");
    private static final Path INDEXINGS_FILE = Paths.get(CONTROL_DIR, "indexed_books.txt");

    public static void initializeControlFiles() {
        try {
            Files.createDirectories(Paths.get(CONTROL_DIR));
            if (!Files.exists(DOWNLOADS_FILE)) Files.createFile(DOWNLOADS_FILE);
            if (!Files.exists(INDEXINGS_FILE)) Files.createFile(INDEXINGS_FILE);
        } catch (IOException e) {
            System.err.println("Error creando archivos de control: " + e.getMessage());
        }
    }

    private static Set<String> readIds(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return new HashSet<>(lines);
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    public static void processNextStep() {
        Set<String> downloaded = readIds(DOWNLOADS_FILE);
        Set<String> indexed = readIds(INDEXINGS_FILE);

        Set<String> readyToIndex = new HashSet<>(downloaded);
        readyToIndex.removeAll(indexed);

        if (!readyToIndex.isEmpty()) {
            String bookId = readyToIndex.iterator().next();
            System.out.println("[CONTROL] Programando libro " + bookId + " para indexación...");
            // Integración futura: Llamada al tokenizador de la Persona 3
            markAsIndexed(bookId);
        } else {
            System.out.println("[CONTROL] No hay libros pendientes. Solicitando descarga de un nuevo libro...");
            // Integración futura: Llamada al script de descarga de la Persona 1
            // markAsDownloaded("nuevo_id");
        }
    }

    private static void markAsIndexed(String bookId) {
        try {
            Files.writeString(INDEXINGS_FILE, bookId + System.lineSeparator(), StandardOpenOption.APPEND);
            System.out.println("[CONTROL] Libro " + bookId + " marcado como indexado.");
        } catch (IOException e) {
            System.err.println("Error actualizando archivo de control.");
        }
    }
}