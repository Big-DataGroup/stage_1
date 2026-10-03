package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import java.io.File;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        // Esta línea asegura que la carpeta se cree si no existe para que JMH no explote
        // Sube dos niveles desde source/java y crea la carpeta global
        new java.io.File("../../data/benchmarks").mkdirs();

        Options opt = new OptionsBuilder()
                .include(".*Benchmark.*")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
                // Guarda el archivo en la carpeta global
                .result("../../data/benchmarks/metricas_java.csv")
                .addProfiler("gc")
                .build();

        new Runner(opt).run();
    }
}