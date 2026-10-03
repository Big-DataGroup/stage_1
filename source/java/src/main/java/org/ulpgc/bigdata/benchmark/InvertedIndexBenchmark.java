package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

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
        Path tempFile = Files.createTempFile("benchmark_book_index", ".txt");
        String dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
        Files.writeString(tempFile, dummyContent.repeat(datasetSize));
        this.testFilePath = tempFile.toAbsolutePath().toString();

        preFilledIndex = new InvertedIndex();
        preFilledIndex.addDocument(1, testFilePath);

        storages = new ArrayList<>();
    }

    @Benchmark
    public void updatePerformance() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument(999, testFilePath);
    }

    @Benchmark
    public void queryPerformance() {
        preFilledIndex.search("indexacion");
    }

    @Benchmark
    public void diskIoPerformance() {
        preFilledIndex.persistAll(storages);
    }
}