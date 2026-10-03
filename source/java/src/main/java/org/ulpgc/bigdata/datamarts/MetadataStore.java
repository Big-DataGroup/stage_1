package org.ulpgc.bigdata.datamarts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class MetadataStore {
    private static final String DB_URL = "jdbc:sqlite:../../data/metadata.db";

    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS books ("
                + "book_id INTEGER PRIMARY KEY,"
                + "title TEXT,"
                + "author TEXT,"
                + "language TEXT"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabla 'books' inicializada correctamente.");
        } catch (Exception e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    public static void insertMetadata(BookMetadata book) {
        String insertSQL = "INSERT OR REPLACE INTO books(book_id, title, author, language) VALUES(?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setInt(1, book.bookId());
            pstmt.setString(2, book.title());
            pstmt.setString(3, book.author());
            pstmt.setString(4, book.language());
            pstmt.executeUpdate();

            System.out.println("Metadatos insertados en SQLite para el libro: " + book.bookId());
        } catch (Exception e) {
            System.err.println("Error al insertar metadatos: " + e.getMessage());
        }
    }
}