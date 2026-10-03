using System;
using System.IO;
using Microsoft.Data.Sqlite;

namespace BigDataPipeline.Datamarts
{
    public class MetadataStore
    {

        private static readonly string DbUrl = "Data Source=../../data/metadata.db";

        public static void InitializeDatabase()
        {
            string createTableSql = @"CREATE TABLE IF NOT EXISTS books (
                                        book_id INTEGER PRIMARY KEY,
                                        title TEXT,
                                        author TEXT,
                                        language TEXT
                                      );";
            try
            {
                Directory.CreateDirectory("../../data");

                using SqliteConnection conn = new SqliteConnection(DbUrl);
                conn.Open();
                
                using SqliteCommand stmt = conn.CreateCommand();
                stmt.CommandText = createTableSql;
                stmt.ExecuteNonQuery();
                
                Console.WriteLine("Tabla 'books' inicializada correctamente.");
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"Error al inicializar la base de datos: {e.Message}");
            }
        }

        public static void InsertMetadata(BookMetadata book)
        {
            string insertSql = "INSERT OR REPLACE INTO books(book_id, title, author, language) VALUES(@id, @title, @author, @language)";

            try
            {
                using SqliteConnection conn = new SqliteConnection(DbUrl);
                conn.Open();

                using SqliteCommand pstmt = conn.CreateCommand();
                pstmt.CommandText = insertSql;

                pstmt.Parameters.AddWithValue("@id", book.BookId);
                pstmt.Parameters.AddWithValue("@title", book.Title);
                pstmt.Parameters.AddWithValue("@author", book.Author);
                pstmt.Parameters.AddWithValue("@language", book.Language);
                
                pstmt.ExecuteNonQuery();

                Console.WriteLine($"Metadatos insertados en SQLite para el libro: {book.BookId}");
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"Error al insertar metadatos: {e.Message}");
            }
        }
    }
}