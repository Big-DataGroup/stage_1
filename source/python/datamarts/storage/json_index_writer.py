import sys
from pathlib import Path


class JsonIndexWriter:
    @staticmethod
    def write(index: dict, outputFile: Path):
        try:
            if outputFile.parent:
                outputFile.parent.mkdir(parents=True, exist_ok=True)

            sorted_items = sorted(index.items())

            with open(outputFile, 'w', encoding='utf-8') as writer:
                writer.write("{\n")

                total = len(sorted_items)

                for i, (key, ids) in enumerate(sorted_items):
                    writer.write(f'  "{JsonIndexWriter._escape(key)}": [')

                    for j in range(len(ids)):
                        writer.write(str(ids[j]))
                        if j < len(ids) - 1:
                            writer.write(", ")

                    writer.write("]")

                    if i < total - 1:
                        writer.write(",")
                    writer.write("\n")

                writer.write("}\n")

        except IOError as e:
            print(f"Error escribiendo índice JSON en {outputFile}: {e}", file=sys.stderr)

    @staticmethod
    def _escape(s: str) -> str:
        return s.replace("\\", "\\\\").replace("\"", "\\\"")