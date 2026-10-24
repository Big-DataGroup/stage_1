package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class InvertedIndexBenchmark {

    @Param({"500", "1000"})
    public int datasetSize;

    private String testFilePath;
    private InvertedIndex preFilledIndex;
    private List<IndexStorage> storages;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        // 1. Crear archivo temporal de lectura para el dataset
        Path tempFile = Files.createTempFile("benchmark_book_index", ".txt");
        String dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
        Files.writeString(tempFile, dummyContent.repeat(datasetSize));
        this.testFilePath = tempFile.toAbsolutePath().toString();

        // 2. Crear un índice previamente poblado
        preFilledIndex = new InvertedIndex();
        preFilledIndex.addDocument(1, testFilePath);

        // 3. FIX DE ERROR: Especificar la ruta completa de un ARCHIVO .json, no solo el directorio
        storages = new ArrayList<>();
        Path outputDir = Files.createTempDirectory("benchmark_output");
        Path jsonFilePath = outputDir.resolve("index.json"); // <--- AQUÍ ESTÁ EL CAMBIO CLAVE

        storages.add(new JsonFileIndexStorage(jsonFilePath.toAbsolutePath().toString()));
    }

    /**
     * FIX QUERY: Se retorna el resultado para evitar que el JIT elimine la llamada (Dead Code Elimination).
     */
    @Benchmark
    public List<Integer> queryPerformance() {
        return preFilledIndex.search("indexacion");
    }

    /**
     * FIX DISK I/O: Ahora 'storages' contiene un Storage real, por lo que efectivamente escribe en disco.
     */
    @Benchmark
    public List<IndexStorage> diskIoPerformance() {
        preFilledIndex.persistAll(storages);
        return storages; // Retornamos para asegurar la ejecución completa
    }

    /**
     * Actualización/Indexación desde un fichero de prueba.
     */
    @Benchmark
    public InvertedIndex updatePerformance() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument(999, testFilePath);
        return index;
    }
}