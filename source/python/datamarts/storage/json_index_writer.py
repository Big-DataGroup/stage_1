import sys
from pathlib import Path


class JsonIndexWriter:
    """
    Utilidad interna para volcar un mapa término -> [IDs] a un fichero JSON,
    sin depender de librerías externas (Gson/Jackson). Si el proyecto ya
    incluye una de esas librerías, se puede sustituir por ella sin tocar
    el resto del código: solo la usan JsonFileIndexStorage y
    FolderHierarchyIndexStorage.
    """

    # En Python no hace falta crear un constructor privado para evitar que la
    # clase se instancie. Simplemente, no definimos __init__ y usamos @staticmethod.

    @staticmethod
    def write(index: dict, outputFile: Path):
        try:
            # En pathlib, parents=True crea todas las carpetas intermedias (como dirs -p)
            if outputFile.parent:
                outputFile.parent.mkdir(parents=True, exist_ok=True)

            # Equivalente a TreeMap en Java: ordenamos las claves alfabéticamente
            # para que la salida sea determinista. Esto devuelve una lista de tuplas (clave, valor).
            sorted_items = sorted(index.items())

            with open(outputFile, 'w', encoding='utf-8') as writer:
                writer.write("{\n")

                total = len(sorted_items)

                # enumerate nos da el índice (i) y la tupla (key, ids) en cada vuelta
                for i, (key, ids) in enumerate(sorted_items):
                    # Usamos el método de escape manual (le pongo _ delante por convención privada)
                    writer.write(f'  "{JsonIndexWriter._escape(key)}": [')

                    # Bucle manual para escribir los IDs tal como en Java
                    # (Nota: la forma nativa y rápida en Python sería: writer.write(", ".join(map(str, ids))) )
                    for j in range(len(ids)):
                        writer.write(str(ids[j]))
                        if j < len(ids) - 1:
                            writer.write(", ")

                    writer.write("]")

                    # En Java hacías i++ antes del if, así que en Python comparamos con total - 1
                    if i < total - 1:
                        writer.write(",")
                    writer.write("\n")

                writer.write("}\n")

        except IOError as e:
            print(f"Error escribiendo índice JSON en {outputFile}: {e}", file=sys.stderr)

    @staticmethod
    def _escape(s: str) -> str:
        # En Python, el método replace de los strings funciona exactamente igual
        return s.replace("\\", "\\\\").replace("\"", "\\\"")