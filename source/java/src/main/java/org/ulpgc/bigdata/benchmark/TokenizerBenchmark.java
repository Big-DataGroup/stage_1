package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ulpgc.bigdata.datamarts.Tokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class TokenizerBenchmark {

    private String testFilePath;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        Path tempFile = Files.createTempFile("benchmark_book", ".txt");
        String dummyContent = "Este es un texto de prueba. Tiene mayusculas, 123 numeros y signos! \n";
        // Repetimos el texto 10.000 veces para que el Tokenizer tenga que trabajar
        Files.writeString(tempFile, dummyContent.repeat(10000));
        this.testFilePath = tempFile.toAbsolutePath().toString();
    }

    @Benchmark
    public void testTokenization() {
        // JMH ejecutará esta línea en bucle para calcular las operaciones por segundo
        Tokenizer.tokenize(testFilePath);
    }
}