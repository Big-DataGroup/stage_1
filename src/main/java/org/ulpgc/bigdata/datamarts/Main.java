package org.ulpgc.bigdata.datamarts;

public class Main {
    public static void main(String[] args) {
        // 1. Inicializar la base de datos de metadatos
        MetadataStore.initializeDatabase();

        // 2. Extraer datos y guardarlos (Reemplaza la ruta por tu archivo de prueba)
        String rutaCabecera = "C:\\Users\\...\\ruta\\al\\proyecto\\1342_header.txt";
        BookMetadata metadata = MetadataParser.parseHeader(1342, rutaCabecera);
        MetadataStore.insertMetadata(metadata);

        // 3. Inicializar y probar la capa de control
        ControlLayer.initializeControlFiles();
        ControlLayer.processNextStep();
    }
}