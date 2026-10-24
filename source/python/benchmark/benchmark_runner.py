import time
import tracemalloc
import csv
import os
import tempfile
import sys

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from datamarts.inverted_index import InvertedIndex
from datamarts.storage.json_file_index_storage import JsonFileIndexStorage

def run_benchmarks():
    os.makedirs("../../../data/benchmarks", exist_ok=True)
csv_file = "../../../data/benchmarks/metricas_python.csv"

results = [["Benchmark", "datasetSize", "Ops/s", "RAM_MB"]]
sizes = [500, 1000]

for size in sizes:
    # 1. Crear un archivo temporal con contenido real de prueba
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt', encoding='utf-8') as temp_file:
        dummy_content = "palabra_frecuente_" + " ".join([f"ter_{i}" for i in range(100)]) + "\n"
        temp_file.write(dummy_content * size)
        temp_file_path = temp_file.name

    try:
        # --- PRUEBA 1: UPDATE PERFORMANCE (addDocument) ---
        tracemalloc.start()
        start_time = time.perf_counter()

        index = InvertedIndex()
        index.addDocument(999, temp_file_path)

        end_time = time.perf_counter()
        _, peak_ram = tracemalloc.get_traced_memory()
        tracemalloc.stop()

        elapsed = end_time - start_time
        ops = 1 / elapsed if elapsed > 0 else 0
        results.append(["updatePerformance", size, round(ops, 2), round(peak_ram / (1024 * 1024), 4)])

        # Pre-llenamos el índice para Query y Disk I/O
        pre_filled_index = InvertedIndex()
        pre_filled_index.addDocument(1, temp_file_path)

        # --- PRUEBA 2: QUERY PERFORMANCE (Búsqueda real mediante método público search) ---
        tracemalloc.start()
        start_time = time.perf_counter()

        # FIX: Llamada directa a search() para alineación 1:1 con Java y C#
        _ = pre_filled_index.search("ter_50")

        end_time = time.perf_counter()
        _, peak_ram_query = tracemalloc.get_traced_memory()
        tracemalloc.stop()

        elapsed_query = end_time - start_time
        ops_query = 1 / elapsed_query if elapsed_query > 0 else 0
        results.append(["queryPerformance", size, round(ops_query, 2), round(peak_ram_query / (1024 * 1024), 4)])

        # --- PRUEBA 3: DISK I/O PERFORMANCE (Escritura real en disco - Punto 25) ---
        storage_path = tempfile.mktemp(suffix='.json')
        storages = [JsonFileIndexStorage(storage_path)]

        tracemalloc.start()
        start_time = time.perf_counter()

        # Escritura real utilizando el storage de JSON
        pre_filled_index.persistAll(storages)

        end_time = time.perf_counter()
        _, peak_ram_persist = tracemalloc.get_traced_memory()
        tracemalloc.stop()

        elapsed_persist = end_time - start_time
        ops_persist = 1 / elapsed_persist if elapsed_persist > 0 else 0
        results.append(["diskIoPerformance", size, round(ops_persist, 2), round(peak_ram_persist / (1024 * 1024), 4)])

        # Limpiar archivo de storage temporal
        if os.path.exists(storage_path):
            os.remove(storage_path)

    finally:
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)


# Crear la estructura de carpetas si no existe
    os.makedirs(os.path.dirname(os.path.abspath(csv_file)), exist_ok=True)

    with open(csv_file, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerows(results)

    print(f"¡Benchmarks de Python reales completados! Archivo guardado en: {csv_file}")


if __name__ == "__main__":
    run_benchmarks()