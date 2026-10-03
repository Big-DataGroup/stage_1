import pandas as pd
import os


# ==========================================
# 1. LIMPIEZA DE C# (BENCHMARKDOTNET)
# ==========================================
def clean_csharp_benchmark(file_path):
    df = pd.read_csv(file_path, sep=';')

    cols_to_keep = ['Method', 'DatasetSize', 'Mean', 'Allocated']
    df_clean = df[cols_to_keep].copy()
    df_clean.rename(columns={'Method': 'Operation'}, inplace=True)
    df_clean['Language'] = 'C#'

    def parse_time_to_ms(time_str):
        if pd.isna(time_str) or time_str == 'NA': return 0.0
        time_str = str(time_str).strip().replace(',', '')
        if 'ns' in time_str: return float(time_str.replace('ns', '')) / 1_000_000
        if 'us' in time_str or 'μs' in time_str: return float(time_str.replace('us', '').replace('μs', '')) / 1_000
        if 'ms' in time_str: return float(time_str.replace('ms', ''))
        if ' s' in time_str: return float(time_str.replace(' s', '')) * 1000
        if ' m' in time_str: return float(time_str.replace(' m', '')) * 60000
        return 0.0

    def parse_memory_to_mb(mem_str):
        if pd.isna(mem_str) or mem_str == 'NA': return 0.0
        mem_str = str(mem_str).strip().replace(',', '')
        if ' B' in mem_str: return float(mem_str.replace(' B', '')) / (1024 * 1024)
        if ' KB' in mem_str: return float(mem_str.replace(' KB', '')) / 1024
        if ' MB' in mem_str: return float(mem_str.replace(' MB', ''))
        if ' GB' in mem_str: return float(mem_str.replace(' GB', '')) * 1024
        return 0.0

    df_clean['Time_ms'] = df_clean['Mean'].apply(parse_time_to_ms)
    df_clean['Memory_MB'] = df_clean['Allocated'].apply(parse_memory_to_mb)

    df_clean.drop(columns=['Mean', 'Allocated'], inplace=True)
    return df_clean[['Language', 'Operation', 'DatasetSize', 'Time_ms', 'Memory_MB']]


# ==========================================
# 2. LIMPIEZA DE JAVA (JMH) - FILTRO A 6 FILAS
# ==========================================
def clean_java_benchmark(file_path):
    df = pd.read_csv(file_path, sep=',')

    # 1. Añadimos 'Mode' para controlar duplicados
    cols_to_keep = ['Benchmark', 'Mode', 'Param: datasetSize', 'Score', 'Unit']
    df_clean = df[cols_to_keep].copy()

    # Extraemos el nombre real del método
    df_clean['Raw_Operation'] = df_clean['Benchmark'].apply(lambda x: str(x).split('.')[-1])
    df_clean['Language'] = 'Java'
    df_clean.rename(columns={'Param: datasetSize': 'DatasetSize'}, inplace=True)

    # Limpiamos nulos de tamaño
    df_clean.dropna(subset=['DatasetSize'], inplace=True)
    df_clean['DatasetSize'] = df_clean['DatasetSize'].astype(int)

    # --- FILTRO MÁGICO: Mapeo y descarte ---
    # --- FILTRO MÁGICO: Mapeo y descarte ---
    def map_operation(op):
        op = str(op).lower()
        # Añadimos 'disk' para que lo detecte.
        # Lo renombramos a 'Indexing' para que coincida con C# y Python,
        # o puedes cambiarlo por return 'Disk' si en los otros también se llama así.
        if 'index' in op or 'build' in op or 'load' in op or 'disk' in op: return 'Indexing'
        if 'quer' in op or 'search' in op or 'find' in op: return 'Query'
        if 'updat' in op or 'insert' in op or 'add' in op: return 'Update'
        return 'DESCARTE'

    df_clean['Operation'] = df_clean['Raw_Operation'].apply(map_operation)

    # Nos cargamos todas las filas de inicialización o ruido
    df_clean = df_clean[df_clean['Operation'] != 'DESCARTE']

    # 2. Conversión matemática a milisegundos (cubriendo todos los casos de JMH)
    def parse_jmh_time_to_ms(row):
        score_str = str(row['Score']).replace(',', '.')
        if score_str == 'nan' or pd.isna(row['Score']): return 0.0

        score = float(score_str)
        unit = str(row['Unit']).strip().lower()
        mode = str(row['Mode']).strip().lower()

        # Si JMH corrió en Throughput (Operaciones por segundo), hay que invertir la métrica como en Python
        if 'thrpt' in mode:
            return (1000.0 / score) if score > 0 else 0.0

        # Si corrió en tiempo (AverageTime)
        if 'ns' in unit: return score / 1_000_000
        if 'us' in unit or 'μs' in unit: return score / 1_000
        if 'ms' in unit: return score
        if 's' in unit and 'ms' not in unit: return score * 1000
        return score

    df_clean['Time_ms'] = df_clean.apply(parse_jmh_time_to_ms, axis=1)
    df_clean['Memory_MB'] = 0.0

    # 3. Agrupamos por si JMH midió el mismo método en dos modos distintos, sacando la media final
    df_clean = df_clean.groupby(['Language', 'Operation', 'DatasetSize', 'Memory_MB'], as_index=False)['Time_ms'].mean()

    return df_clean[['Language', 'Operation', 'DatasetSize', 'Time_ms', 'Memory_MB']]


# ==========================================
# 3. LIMPIEZA DE PYTHON
# ==========================================
def clean_python_benchmark(file_path):
    df = pd.read_csv(file_path, sep=',')

    df_clean = df.copy()
    df_clean.rename(columns={
        'Benchmark': 'Operation',
        'datasetSize': 'DatasetSize',
        'RAM_MB': 'Memory_MB'
    }, inplace=True)

    df_clean['Language'] = 'Python'

    # Convertimos Operaciones/Segundo a Milisegundos por Operación
    def ops_to_ms(ops):
        if pd.isna(ops) or float(ops) == 0.0: return 0.0
        return 1000.0 / float(ops)

    df_clean['Time_ms'] = df_clean['Ops/s'].apply(ops_to_ms)

    return df_clean[['Language', 'Operation', 'DatasetSize', 'Time_ms', 'Memory_MB']]


# ==========================================
# EJECUCIÓN PRINCIPAL: UNIFICACIÓN
# ==========================================
if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.dirname(script_dir)

    # Rutas a tus archivos (ya corregidas)
    ruta_csharp = os.path.join(root_dir, "data", "benchmarks", "metricas_csharp.csv")
    ruta_java = os.path.join(root_dir, "data", "benchmarks", "metricas_java.csv")
    ruta_python = os.path.join(root_dir, "data", "benchmarks", "metricas_python.csv")

    dataframes = []

    if os.path.exists(ruta_csharp):
        dataframes.append(clean_csharp_benchmark(ruta_csharp))
    else:
        print(f"No se encontró: {ruta_csharp}")

    if os.path.exists(ruta_java):
        dataframes.append(clean_java_benchmark(ruta_java))
    else:
        print(f"No se encontró: {ruta_java}")

    if os.path.exists(ruta_python):
        dataframes.append(clean_python_benchmark(ruta_python))
    else:
        print(f"No se encontró: {ruta_python}")

    if dataframes:
        # 1. Juntamos los tres dataframes crudos
        df_final = pd.concat(dataframes, ignore_index=True)


        # 2. --- UNIFICADOR DE NOMBRES GLOBAL ---
        def unify_operation_names(op):
            op = str(op).lower()
            if 'index' in op or 'build' in op or 'load' in op or 'disk' in op: return 'Indexing'
            if 'quer' in op or 'search' in op or 'find' in op: return 'Query'
            if 'updat' in op or 'insert' in op or 'add' in op: return 'Update'
            return 'DESCARTE'  # Por si C# o Python cuelan alguna métrica de inicialización


        # Aplicamos la normalización a toda la columna Operation
        df_final['Operation'] = df_final['Operation'].apply(unify_operation_names)

        # Limpiamos cualquier fila que haya caído en DESCARTE
        df_final = df_final[df_final['Operation'] != 'DESCARTE']

        # 3. Mostrar y exportar
        print("\n=== DATASET UNIFICADO FINAL ===")
        print(df_final.to_string(index=False))

        ruta_salida = os.path.join(script_dir, "unified_metrics.csv")
        df_final.to_csv(ruta_salida, index=False)
        print(f"\nArchivo maestro guardado con éxito en: {ruta_salida}")
    else:
        print("No se ha podido procesar ningún archivo. Revisa las rutas.")