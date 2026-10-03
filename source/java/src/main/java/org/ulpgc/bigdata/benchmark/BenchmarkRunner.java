package org.ulpgc.bigdata.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        new java.io.File("../../data/benchmarks").mkdirs();

        Options opt = new OptionsBuilder()
                .include(".*Benchmark.*")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
                .result("../../data/benchmarks/metricas_java.csv")
                .addProfiler("gc")
                .build();

        new Runner(opt).run();
    }
}