package com.transmdicas.neumaticos.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class ConversorPdf {

    private static final String LIBRE_OFFICE = System.getenv().getOrDefault(
            "LIBREOFFICE_PATH",
            System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows")
                    ? "C:\\Program Files\\LibreOffice\\program\\soffice.exe"
                    : "/usr/bin/libreoffice"
    );

    public static byte[] convertir(
            byte[] excel,
            String extension
    ) throws IOException {

        if (!extension.equals("xlsx") && !extension.equals("xlsm")) {
            throw new IllegalArgumentException(
                    "La extensión debe ser xlsx o xlsm."
            );
        }

        Path directorio = Files.createTempDirectory("neumaticos-pdf-");
        Path archivoExcel = directorio.resolve("documento." + extension);
        Path archivoPdf = directorio.resolve("documento.pdf");
        Path perfil = directorio.resolve("perfil-libreoffice");
        Path registro = directorio.resolve("conversion.log");

        Process proceso = null;

        try {
            Files.write(archivoExcel, excel);

            /*
             * Se usa un perfil temporal para que la conversión
             * no dependa de otra ventana abierta de LibreOffice.
             */
            String perfilUrl = perfil.toUri().toASCIIString();

            ProcessBuilder constructor = new ProcessBuilder(
                    LIBRE_OFFICE,
                    "-env:UserInstallation=" + perfilUrl,
                    "--headless",
                    "--convert-to",
                    "pdf:calc_pdf_Export",
                    "--outdir",
                    directorio.toString(),
                    archivoExcel.toString()
            );

            constructor.redirectErrorStream(true);
            constructor.redirectOutput(registro.toFile());

            proceso = constructor.start();

            boolean termino;

            try {
                termino = proceso.waitFor(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IOException(
                        "La conversión a PDF fue interrumpida.",
                        e
                );
            }

            if (!termino) {
                proceso.destroyForcibly();

                throw new IOException(
                        "La conversión a PDF tardó demasiado."
                );
            }

            if (proceso.exitValue() != 0 || !Files.exists(archivoPdf)) {
                throw new IOException(
                        "LibreOffice no pudo generar el PDF."
                );
            }

            return Files.readAllBytes(archivoPdf);

        } finally {
            if (proceso != null && proceso.isAlive()) {
                proceso.destroyForcibly();
            }

            /*
             * Se intenta limpiar los archivos temporales.
             * Si Windows aún tiene alguno abierto, no se interrumpe
             * la descarga por ese motivo.
             */
            try (var archivos = Files.walk(directorio)) {
                archivos
                        .sorted(Comparator.reverseOrder())
                        .forEach(ruta -> {
                            try {
                                Files.deleteIfExists(ruta);
                            } catch (IOException ignored) {
                                // Windows puede liberar el archivo después.
                            }
                        });
            } catch (IOException ignored) {
                // No impedir la respuesta si falla la limpieza.
            }
        }
    }
}