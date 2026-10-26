import csv
import os
import sys
import tempfile
import time
import tracemalloc

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from datamarts.inverted_index import InvertedIndex
from datamarts.storage.folder_hierarchy_index_storage import FolderHierarchyIndexStorage
from datamarts.storage.json_file_index_storage import JsonFileIndexStorage


LAYOUTS = ("by_book", "by_batch", "by_time")
INDEXES = ("json_monolithic", "mongodb", "folders")


def _corpus_path(root, layout, dataset_size):
    if layout == "by_book":
        directory = os.path.join(root, layout, "999")
    elif layout == "by_batch":
        directory = os.path.join(root, layout, "0-999")
    else:
        directory = os.path.join(root, layout, "benchmark-date", "00")

    os.makedirs(directory, exist_ok=True)
    path = os.path.join(directory, "999.body.txt")
    content = "palabra_frecuente " + " ".join(f"ter_{i}" for i in range(100)) + "\n"
    with open(path, "w", encoding="utf-8") as file:
        file.write(content * dataset_size)
    return path


def _storage(index_name, root, layout, dataset_size):
    if index_name == "json_monolithic":
        return JsonFileIndexStorage(os.path.join(root, "json", f"{layout}-{dataset_size}.json"))
    if index_name == "folders":
        return FolderHierarchyIndexStorage(os.path.join(root, "folders", f"{layout}-{dataset_size}"))
    from datamarts.storage.mongo_index_storage import MongoIndexStorage

    return MongoIndexStorage(
        os.getenv("MONGO_URI", "mongodb://localhost:27017"),
        os.getenv("MONGO_DATABASE", "gutenberg_benchmark"),
        f"python_{layout}_{dataset_size}",
    )


def run_benchmarks():
    csv_file = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "../../../data/benchmarks/metricas_python.csv")
    )
    results = [["Benchmark", "datasetSize", "Layout", "Index", "Ops/s", "RAM_MB"]]

    with tempfile.TemporaryDirectory(prefix="python_benchmark_") as benchmark_root:
        for size in (500, 1000):
            for layout in LAYOUTS:
                corpus_path = _corpus_path(benchmark_root, layout, size)
                for index_name in INDEXES:
                    tracemalloc.start()
                    start_time = time.perf_counter()
                    index = InvertedIndex()
                    index.addDocument(999, corpus_path)
                    elapsed = time.perf_counter() - start_time
                    _, peak_ram = tracemalloc.get_traced_memory()
                    tracemalloc.stop()
                    results.append([
                        "updatePerformance", size, layout, index_name,
                        round(1 / elapsed if elapsed > 0 else 0, 8),
                        round(peak_ram / (1024 * 1024), 6),
                    ])

                    pre_filled_index = InvertedIndex()
                    pre_filled_index.addDocument(1, corpus_path)

                    tracemalloc.start()
                    start_time = time.perf_counter()
                    _ = pre_filled_index.search("ter_50")
                    elapsed = time.perf_counter() - start_time
                    _, peak_ram_query = tracemalloc.get_traced_memory()
                    tracemalloc.stop()
                    results.append([
                        "queryPerformance", size, layout, index_name,
                        round(1 / elapsed if elapsed > 0 else 0, 8),
                        round(peak_ram_query / (1024 * 1024), 4),
                    ])

                    storage = _storage(index_name, benchmark_root, layout, size)
                    try:
                        tracemalloc.start()
                        start_time = time.perf_counter()
                        pre_filled_index.persistAll([storage])
                        elapsed = time.perf_counter() - start_time
                        _, peak_ram_persist = tracemalloc.get_traced_memory()
                        tracemalloc.stop()
                        results.append([
                            "diskIoPerformance", size, layout, index_name,
                            round(1 / elapsed if elapsed > 0 else 0, 8),
                            round(peak_ram_persist / (1024 * 1024), 4),
                        ])
                    finally:
                        close = getattr(storage, "close", None)
                        if close:
                            close()

    os.makedirs(os.path.dirname(csv_file), exist_ok=True)
    with open(csv_file, mode="w", newline="", encoding="utf-8") as file:
        csv.writer(file).writerows(results)

    print(f"¡Benchmarks de Python reales completados! Archivo guardado en: {csv_file}")


if __name__ == "__main__":
    run_benchmarks()
