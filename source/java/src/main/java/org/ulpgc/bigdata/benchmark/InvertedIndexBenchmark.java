package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage; // <-- Importamos la clase

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
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
        Path tempFile = Files.createTempFile("benchmark_book_index", ".txt");
        String dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
        Files.writeString(tempFile, dummyContent.repeat(datasetSize));
        this.testFilePath = tempFile.toAbsolutePath().toString();

        preFilledIndex = new InvertedIndex();
        preFilledIndex.addDocument(1, testFilePath);

        // Creamos un archivo temporal JSON y lo añadimos a la lista de storages
        Path tempOutput = Files.createTempFile("benchmark_output", ".json");
        storages = new ArrayList<>();
        storages.add(new JsonFileIndexStorage(tempOutput.toAbsolutePath().toString()));
    }

    @Benchmark
    public InvertedIndex updatePerformance() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument(999, testFilePath);
        return index;
    }

    @Benchmark
    public List<Integer> queryPerformance() {
        return preFilledIndex.search("indexacion");
    }

    @Benchmark
    public void diskIoPerformance() {
        preFilledIndex.persistAll(storages);
    }
}