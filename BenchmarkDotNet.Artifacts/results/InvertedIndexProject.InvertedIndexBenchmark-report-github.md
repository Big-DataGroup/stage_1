```

BenchmarkDotNet v0.13.12, Windows 11 (10.0.22631.6199/23H2/2023Update/SunValley3)
12th Gen Intel Core i5-12450H, 1 CPU, 12 logical and 8 physical cores
.NET SDK 10.0.402
  [Host]     : .NET 10.0.13 (10.0.1326.50504), X64 RyuJIT AVX2
  DefaultJob : .NET 10.0.13 (10.0.1326.50504), X64 RyuJIT AVX2


```
| Method            | DatasetSize | Mean            | Error         | StdDev        | Gen0     | Gen1   | Allocated |
|------------------ |------------ |----------------:|--------------:|--------------:|---------:|-------:|----------:|
| **UpdatePerformance** | **500**         |   **780,242.83 ns** | **15,363.469 ns** | **28,856.319 ns** |  **89.8438** | **1.9531** |  **574013 B** |
| QueryPerformance  | 500         |        12.22 ns |      0.216 ns |      0.191 ns |        - |      - |         - |
| DiskIoPerformance | 500         |   224,465.76 ns |  4,462.684 ns |  6,256.059 ns |   0.9766 |      - |    7745 B |
| **UpdatePerformance** | **1000**        | **1,325,441.34 ns** | **24,473.695 ns** | **34,308.702 ns** | **179.6875** | **5.8594** | **1138056 B** |
| QueryPerformance  | 1000        |        12.56 ns |      0.187 ns |      0.156 ns |        - |      - |         - |
| DiskIoPerformance | 1000        |   226,765.63 ns |  4,481.579 ns |  8,306.895 ns |   0.9766 |      - |    7745 B |
