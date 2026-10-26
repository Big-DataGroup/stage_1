package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.FolderHierarchyIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.MongoIndexStorage;

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

    @Param({"by_book", "by_batch", "by_time"})
    public String layout;

    @Param({"json_monolithic", "mongodb", "folders"})
    public String index;

    private String testFilePath;
    private InvertedIndex preFilledIndex;
    private List<IndexStorage> storages;
    private MongoIndexStorage mongoStorage;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        Path datalake = Files.createTempDirectory("benchmark_datalake");
        Path layoutDir = switch (layout) {
            case "by_book" -> datalake.resolve("by_book").resolve("999");
            case "by_batch" -> datalake.resolve("by_batch").resolve("0-999");
            case "by_time" -> datalake.resolve("by_time").resolve("benchmark-date").resolve("00");
            default -> throw new IllegalArgumentException("Unknown layout: " + layout);
        };
        Files.createDirectories(layoutDir);
        Path tempFile = layoutDir.resolve("999.body.txt");
        String dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
        Files.writeString(tempFile, dummyContent.repeat(datasetSize));
        this.testFilePath = tempFile.toAbsolutePath().toString();

        preFilledIndex = new InvertedIndex();
        preFilledIndex.addDocument(1, testFilePath);

        storages = new ArrayList<>();
        switch (index) {
            case "json_monolithic" -> {
                Path tempOutput = Files.createTempFile("benchmark_output", ".json");
                storages.add(new JsonFileIndexStorage(tempOutput.toAbsolutePath().toString()));
            }
            case "folders" -> storages.add(new FolderHierarchyIndexStorage(
                    Files.createTempDirectory("benchmark_output_folders").toString()));
            case "mongodb" -> {
                mongoStorage = new MongoIndexStorage(
                        System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017"),
                        System.getenv().getOrDefault("MONGO_DATABASE", "gutenberg_benchmark"),
                        "java_" + layout + "_" + datasetSize);
                storages.add(mongoStorage);
            }
            default -> throw new IllegalArgumentException("Unknown index: " + index);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (mongoStorage != null) {
            mongoStorage.close();
        }
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