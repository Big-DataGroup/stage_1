import sqlite3
import sys

from datamarts.book_metadata import BookMetadata

class MetadataStore:
    DB_URL = "../../data/metadata.db"

    @staticmethod
    def initializeDatabase():
        createTableSQL = """CREATE TABLE IF NOT EXISTS books (
            book_id INTEGER PRIMARY KEY,
            title TEXT,
            author TEXT,
            language TEXT
        );"""

        try:
            with sqlite3.connect(MetadataStore.DB_URL) as conn:
                stmt = conn.cursor()
                stmt.execute(createTableSQL)
                conn.commit()  # En Python debemos confirmar explícitamente los cambios
                print("Tabla 'books' inicializada correctamente.")
        except Exception as e:
            print(f"Error al inicializar la base de datos: {e}", file=sys.stderr)

    @staticmethod
    def insertMetadata(book: BookMetadata):
        insertSQL = "INSERT OR REPLACE INTO books(book_id, title, author, language) VALUES(?,?,?,?)"

        try:
            with sqlite3.connect(MetadataStore.DB_URL) as conn:
                pstmt = conn.cursor()
                pstmt.execute(insertSQL, (book.bookId, book.title, book.author, book.language))
                conn.commit()
                print(f"Metadatos insertados en SQLite para el libro: {book.bookId}")
        except Exception as e:
            print(f"Error al insertar metadatos: {e}", file=sys.stderr)