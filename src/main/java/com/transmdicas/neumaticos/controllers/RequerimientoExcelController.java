package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.DocumentoForm;
import com.transmdicas.neumaticos.modulo.model.FilaDocumento;
import com.transmdicas.neumaticos.modulo.model.TipoDocumento;
import com.transmdicas.neumaticos.modulo.service.DocumentoService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class RequerimientoExcelController {

    private final DocumentoService documentoService;

    public RequerimientoExcelController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping("/documentos/{id}/excel-auditado")
    public ResponseEntity<byte[]> descargarRequerimiento(
            @PathVariable("id") Long id) throws IOException {

        DocumentoForm documento = documentoService.abrir(id);

        if (documento.getTipo() != TipoDocumento.REQUERIMIENTO) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "Este enlace corresponde a un requerimiento."
            );
        }

        ClassPathResource archivo = new ClassPathResource(
                "formatos/auditados/requerimiento.xlsx"
        );

        try (
                InputStream entrada = archivo.getInputStream();
                Workbook libro = WorkbookFactory.create(entrada);
                ByteArrayOutputStream salida = new ByteArrayOutputStream()
        ) {
            Sheet hoja = libro.getSheet("V004");

            if (hoja == null) {
                throw new IOException(
                        "No se encontró la hoja V004 en el formato."
                );
            }

            libro.setActiveSheet(libro.getSheetIndex(hoja));

            // Datos generales.
            escribirTexto(hoja, 7, 4, documento.getPlaca());      // E8
            escribirTexto(hoja, 7, 9, documento.getOt());         // J8
            escribirTexto(hoja, 8, 4, documento.getOperador());   // E9
            escribirTexto(hoja, 8, 9, documento.getOperacion());  // J9

            escribirNumero(
                    hoja,
                    9,
                    4,
                    documento.getKilometraje()
            );                                                    // E10

            if (documento.getFecha() != null) {
                String fecha = documento.getFecha().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );

                escribirTexto(hoja, 9, 9, fecha);                  // J10
            }

            // El formato tiene 20 filas para neumáticos.
            int cantidad = Math.min(documento.getFilas().size(), 20);

            for (int i = 0; i < cantidad; i++) {
                FilaDocumento neumatico = documento.getFilas().get(i);

                if (!neumatico.isUsada()) {
                    continue;
                }

                // En Excel comienza en la fila 13.
                int filaExcel = 12 + i;

                escribirTexto(hoja, filaExcel, 2, neumatico.getCodigo()); // C
                escribirTexto(hoja, filaExcel, 4, neumatico.getMarca());  // E
                escribirTexto(hoja, filaExcel, 6, neumatico.getDiseno()); // G

                if ("R".equals(neumatico.getRn())) {
                    escribirTexto(hoja, filaExcel, 9, "X");       // J
                }

                if ("N".equals(neumatico.getRn())) {
                    escribirTexto(hoja, filaExcel, 10, "X");      // K
                }

                escribirNumero(
                        hoja,
                        filaExcel,
                        11,
                        neumatico.getCantidad()
                );                                                // L
            }
            // Nombres en la sección FIRMA Y CONFORMIDAD.
escribirTexto(
        hoja, 37, 1,
        "Nombre: " + documento.getResponsable1()
); // B38 - Logística

escribirTexto(
        hoja, 37, 4,
        "Nombre: " + documento.getResponsable2()
); // E38 - Técnico de neumáticos

escribirTexto(
        hoja, 37, 7,
        "Nombre: " + documento.getResponsable3()
); // H38 - Inspector de neumáticos

escribirTexto(
        hoja, 37, 11,
        "Nombre: " + documento.getResponsable4()
); // L38 - Autoriza

libro.write(salida);

return ResponseEntity.ok()
        .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=requerimiento-" + id + ".xlsx"
        )
                    .contentType(
                            MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                    )
                    .body(salida.toByteArray());
        }
    }

    private void escribirTexto(
            Sheet hoja,
            int numeroFila,
            int numeroColumna,
            String valor) {

        Cell celda = obtenerCelda(hoja, numeroFila, numeroColumna);
        celda.setCellValue(valor == null ? "" : valor);
    }

    private void escribirNumero(
            Sheet hoja,
            int numeroFila,
            int numeroColumna,
            Number valor) {

        if (valor == null) {
            return;
        }

        Cell celda = obtenerCelda(hoja, numeroFila, numeroColumna);
        celda.setCellValue(valor.doubleValue());
    }

    private Cell obtenerCelda(
        Sheet hoja,
        int numeroFila,
        int numeroColumna) {

    Row fila = hoja.getRow(numeroFila);

    if (fila == null) {
        fila = hoja.createRow(numeroFila);
    }

    Cell celda = fila.getCell(numeroColumna);

    if (celda == null) {
        celda = fila.createCell(numeroColumna);
    }

    // Se copia el estilo para conservar bordes, relleno y alineación.
    Workbook libro = hoja.getWorkbook();
    CellStyle estiloOriginal = celda.getCellStyle();

    CellStyle estiloArial = libro.createCellStyle();
    estiloArial.cloneStyleFrom(estiloOriginal);

    // Se conservan algunas características de la fuente original.
    Font fuenteOriginal = libro.getFontAt(
            estiloOriginal.getFontIndex()
    );

    Font fuenteArial = libro.createFont();
    fuenteArial.setFontName("Arial");
    fuenteArial.setFontHeightInPoints((short) 10);
    fuenteArial.setBold(fuenteOriginal.getBold());
    fuenteArial.setItalic(fuenteOriginal.getItalic());
    fuenteArial.setUnderline(fuenteOriginal.getUnderline());
    fuenteArial.setColor(fuenteOriginal.getColor());

    estiloArial.setFont(fuenteArial);
    celda.setCellStyle(estiloArial);

    return celda;
}
}