import pandas as pd
import os


def clean_csharp_benchmark(file_path):
    # Detecta automáticamente si C# exportó con ';' (formato viejo) o ',' (formato nuevo)
    with open(file_path, 'r', encoding='utf-8') as f:
        sep = ';' if ';' in f.readline() else ','

    df = pd.read_csv(file_path, sep=sep)

    if 'Mean_ms' in df.columns:
        # Formato NUEVO (generado con nuestro StreamWriter)
        df_clean = df[['Method', 'DatasetSize', 'Mean_ms', 'Allocated_Bytes']].copy()
        df_clean.rename(columns={'Method': 'Operation', 'Mean_ms': 'Time_ms'}, inplace=True)
        df_clean['Memory_MB'] = df_clean['Allocated_Bytes'] / (1024 * 1024)
    else:
        # Formato VIEJO (si sigue leyendo el archivo fantasma antiguo)
        df_clean = df[['Method', 'DatasetSize', 'Mean', 'Allocated']].copy()
        df_clean.rename(columns={'Method': 'Operation'}, inplace=True)
        df_clean['Time_ms'] = df_clean['Mean'].apply(
            lambda x: float(str(x).replace(',', '').replace(' ns', '')) / 1_000_000 if 'ns' in str(x) else 0.0
        )
        df_clean['Memory_MB'] = df_clean['Allocated'].apply(
            lambda x: float(str(x).replace(' B', '')) / (1024 * 1024) if 'B' in str(x) else 0.0
        )

    df_clean['Language'] = 'C#'
    return df_clean[['Language', 'Operation', 'DatasetSize', 'Time_ms', 'Memory_MB']]


def clean_java_benchmark(file_path):
    df = pd.read_csv(file_path, sep=',')
    df_clean = df[['Benchmark', 'Mode', 'Param: datasetSize', 'Score', 'Unit']].copy()
    df_clean['Language'] = 'Java'
    df_clean['Raw_Operation'] = df_clean['Benchmark'].apply(lambda x: str(x).split('.')[-1])
    df_clean.rename(columns={'Param: datasetSize': 'DatasetSize'}, inplace=True)

    def extract_ms(row):
        score = float(str(row['Score']).replace(',', '.'))
        mode = str(row['Mode']).lower()
        unit = str(row['Unit']).lower()

        # JMH saca la info de formas distintas según el Mode
        if 'thrpt' in mode: return (1000.0 / score) if score > 0 else 0.0
        if 'ns' in unit: return score / 1_000_000.0
        if 'us' in unit or 'μs' in unit: return score / 1_000.0
        if 'ms' in unit: return score
        return score

    df_clean['Time_ms'] = df_clean.apply(extract_ms, axis=1)
    df_clean['Memory_MB'] = 0.0  # Java no tiene memoria en el CSV base
    return df_clean[['Language', 'Raw_Operation', 'DatasetSize', 'Time_ms', 'Memory_MB']].rename(
        columns={'Raw_Operation': 'Operation'})


def clean_python_benchmark(file_path):
    df = pd.read_csv(file_path, sep=',')
    df_clean = df[['Benchmark', 'datasetSize', 'Ops/s', 'RAM_MB']].copy()
    df_clean.rename(columns={'Benchmark': 'Operation', 'datasetSize': 'DatasetSize', 'RAM_MB': 'Memory_MB'},
                    inplace=True)
    df_clean['Language'] = 'Python'
    df_clean['Time_ms'] = df_clean['Ops/s'].apply(lambda x: 1000.0 / float(x) if pd.notna(x) and float(x) > 0 else 0.0)
    return df_clean[['Language', 'Operation', 'DatasetSize', 'Time_ms', 'Memory_MB']]


if __name__ == "__main__":
    # 1. Obtenemos la ruta de 'clean_metrics' (donde está este script)
    script_dir = os.path.dirname(os.path.abspath(__file__))

    # 2. Subimos un nivel para llegar a la raíz del proyecto
    root_dir = os.path.dirname(script_dir)

    # 3. Entramos en la carpeta hermana 'data/benchmarks'
    benchmarks_dir = os.path.join(root_dir, "data", "benchmarks")

    print(f"Buscando los CSV en la ruta exacta:\n -> {benchmarks_dir}\n")

    # Asegúrate de que los archivos se llaman "metrics_..." y no "metricas_..."
    ruta_csharp = os.path.join(benchmarks_dir, "metricas_csharp.csv")
    ruta_java = os.path.join(benchmarks_dir, "metricas_java.csv")
    ruta_python = os.path.join(benchmarks_dir, "metricas_python.csv")

    dataframes = []

    if os.path.exists(ruta_csharp):
        print("✅ Encontrado C#")
        dataframes.append(clean_csharp_benchmark(ruta_csharp))
    else:
        print("❌ Faltó C#")

    if os.path.exists(ruta_java):
        print("✅ Encontrado Java")
        dataframes.append(clean_java_benchmark(ruta_java))
    else:
        print("❌ Faltó Java")

    if os.path.exists(ruta_python):
        print("✅ Encontrado Python")
        dataframes.append(clean_python_benchmark(ruta_python))
    else:
        print("❌ Faltó Python")

    if dataframes:
        df_final = pd.concat(dataframes, ignore_index=True)


        def unify_ops(op):
            op = str(op).lower()
            if 'updat' in op: return 'Update'
            if 'quer' in op or 'search' in op: return 'Query'
            if 'disk' in op or 'index' in op: return 'DiskIO'
            return 'DESCARTE'


        df_final['Operation'] = df_final['Operation'].apply(unify_ops)
        df_final = df_final[df_final['Operation'] != 'DESCARTE']

        # 1. Limpieza de nulos y conversión de DatasetSize a entero puro
        df_final.dropna(subset=['DatasetSize'], inplace=True)
        df_final['DatasetSize'] = df_final['DatasetSize'].astype(int)

        # 2. Redondeo de métricas
        df_final['Time_ms'] = df_final['Time_ms'].round(6)
        df_final['Memory_MB'] = df_final['Memory_MB'].round(4)

        ruta_salida = os.path.join(script_dir, "unified_metrics.csv")

        # 3. Exportación: float_format solo afectará a Time_ms y Memory_MB
        df_final.to_csv(ruta_salida, index=False, float_format='%.6f')

        print("\n--- TABLA FINAL UNIFICADA ---")
        pd.options.display.float_format = '{:.6f}'.format
        print(df_final.to_string(index=False))

        print(f"\n[ÉXITO] Archivo maestro unificado guardado en: {ruta_salida}")
    else:
        print("\n[ERROR] No se unió nada. Revisa los nombres de los archivos.")