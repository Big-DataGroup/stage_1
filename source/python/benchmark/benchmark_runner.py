import time
import tracemalloc
import csv
import os
import tempfile
import sys

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
# Importamos las clases reales del proyecto
from datamarts.inverted_index import InvertedIndex


def run_benchmarks():
    # Aseguramos que la carpeta global exista en la raíz del repositorio
    os.makedirs("../../../data/benchmarks", exist_ok=True)
    csv_file = "../../../data/benchmarks/metricas_python.csv"

    results = [["Benchmark", "datasetSize", "Ops/s", "RAM_MB"]]
    sizes = [1000, 5000]

    for size in sizes:
        # 1. Creamos un archivo temporal simulando el tamaño del dataset
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
            current, peak_ram = tracemalloc.get_traced_memory()
            tracemalloc.stop()

            elapsed = end_time - start_time
            ops = 1 / elapsed if elapsed > 0 else 0
            results.append(["updatePerformance", size, round(ops, 2), round(peak_ram / (1024 * 1024), 4)])

            # Pre-llenamos un índice para Query y Persist
            pre_filled_index = InvertedIndex()
            pre_filled_index.addDocument(1, temp_file_path)

            # --- PRUEBA 2: QUERY PERFORMANCE (Búsqueda en memoria) ---
            tracemalloc.start()
            start_time = time.perf_counter()

            # Simulamos la búsqueda accediendo al diccionario interno
            _ = pre_filled_index.index.get("ter_50", [])

            end_time = time.perf_counter()
            _, peak_ram_query = tracemalloc.get_traced_memory()
            tracemalloc.stop()

            elapsed_query = end_time - start_time
            ops_query = 1 / elapsed_query if elapsed_query > 0 else 0
            results.append(["queryPerformance", size, round(ops_query, 2), round(peak_ram_query / (1024 * 1024), 4)])

            # --- PRUEBA 3: DISK I/O PERFORMANCE (persistAll) ---
            tracemalloc.start()
            start_time = time.perf_counter()

            # Pasamos una lista vacía de storages o mock de storage para medir el bucle
            pre_filled_index.persistAll([])

            end_time = time.perf_counter()
            _, peak_ram_persist = tracemalloc.get_traced_memory()
            tracemalloc.stop()

            elapsed_persist = end_time - start_time
            ops_persist = 1 / elapsed_persist if elapsed_persist > 0 else 0
            results.append(
                ["diskIoPerformance", size, round(ops_persist, 2), round(peak_ram_persist / (1024 * 1024), 4)])

        finally:
            # Limpiamos el archivo temporal
            if os.path.exists(temp_file_path):
                os.remove(temp_file_path)

    # Guardamos el archivo CSV definitivo en la carpeta global
    with open(csv_file, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerows(results)

    print(f"¡Benchmarks de Python completados! Archivo guardado en: {csv_file}")


if __name__ == "__main__":
    run_benchmarks()