package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.DocumentoForm;
import com.transmdicas.neumaticos.modulo.model.FilaDocumento;
import com.transmdicas.neumaticos.modulo.model.ObservacionesNeumaticos;
import com.transmdicas.neumaticos.modulo.model.TipoDocumento;
import com.transmdicas.neumaticos.modulo.service.DocumentoService;
import com.transmdicas.neumaticos.util.ConversorPdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class CambioExcelController {

    private final DocumentoService documentoService;

    public CambioExcelController(
            DocumentoService documentoService
    ) {
        this.documentoService = documentoService;
    }

    @GetMapping("/documentos/{id}/excel-cambio-auditado")
    public ResponseEntity<byte[]> descargar(
            @PathVariable("id") Long id
    ) throws IOException {

        DocumentoForm documento = documentoService.abrir(id);

        if (documento.getTipo() != TipoDocumento.CAMBIO) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "El documento no es un cambio de neumáticos."
            );
        }

        validarPosiciones(documento.getFilas());

        ClassPathResource plantilla = new ClassPathResource(
                "formatos/auditados/cambio.xlsm"
        );

        try (
                InputStream entrada = plantilla.getInputStream();
                Workbook libro = WorkbookFactory.create(entrada);
                ByteArrayOutputStream salida = new ByteArrayOutputStream()
        ) {
            Sheet hoja = libro.getSheet("MNT-F-76 AQP-01");

            if (hoja == null) {
                throw new IOException(
                        "No se encontró la hoja MNT-F-76 AQP-01."
                );
            }

           // Seleccionar la hoja auditada que estamos llenando.
int indiceHoja = libro.getSheetIndex(hoja);

libro.setActiveSheet(indiceHoja);
libro.setSelectedTab(indiceHoja);

// Conservar solo la hoja que se usará para este PDF.
// Esto modifica la copia cargada en memoria, no la plantilla original.
for (int i = libro.getNumberOfSheets() - 1; i >= 0; i--) {
    if (!libro.getSheetName(i).equals("MNT-F-76 AQP-01")) {
        libro.removeSheetAt(i);
    }
}

// La hoja conservada ahora ocupa la posición 0.
libro.setSheetHidden(0, false);
libro.setActiveSheet(0);
libro.setSelectedTab(0);

// Dar espacio al nombre del técnico en la fila 17.
hoja.addMergedRegion(
        new CellRangeAddress(16, 16, 0, 21)
);

// Dar más altura a las dos filas que ocupa el operador.
hoja.getRow(12).setHeightInPoints(18);
hoja.getRow(13).setHeightInPoints(18);


llenarHoja(hoja, documento);

// Centrar horizontalmente al imprimir.
hoja.setHorizontallyCenter(true);

// Usar el mismo margen a izquierda y derecha.
// Los valores están en pulgadas.
hoja.setMargin(Sheet.LeftMargin, 0.3);
hoja.setMargin(Sheet.RightMargin, 0.3);

// Ajustar el ancho del formato a una página.
hoja.setFitToPage(true);
hoja.getPrintSetup().setFitWidth((short) 1);
hoja.getPrintSetup().setFitHeight((short) 0);

libro.write(salida);

            byte[] pdf = ConversorPdf.convertir(
        salida.toByteArray(),
        "xlsm"
);

            return ResponseEntity.ok()
        .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=cambio-" + id + ".pdf"
        )
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
        }
    }

    private void validarPosiciones(List<FilaDocumento> filas) {

        Set<Integer> usadas = new HashSet<>();

        for (FilaDocumento fila : filas) {

            if (!fila.isUsada()) {
                continue;
            }

            Integer posicion = fila.getPosicion();

            if (
                    posicion == null
                    || posicion < 1
                    || posicion > 12
                    || !usadas.add(posicion)
            ) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "El formato auditado admite posiciones únicas "
                        + "del 1 al 12. Revisa las posiciones del documento."
                );
            }
        }
    }

    private void llenarHoja(
            Sheet hoja,
            DocumentoForm documento
    ) {
        Map<Short, CellStyle> estilos = new HashMap<>();

        // Datos generales.
        escribirTexto(hoja, 10, 4, documento.getPlaca(), estilos);
        escribirTexto(hoja, 10, 12, documento.getOt(), estilos);

        escribirTexto(hoja, 12, 4, documento.getOperador(), estilos);
        escribirTexto(hoja, 12, 12, documento.getOperacion(), estilos);

        escribirTexto(
                hoja, 14, 4,
                documento.getKilometraje(),
                estilos
        );

        String fecha = documento.getFecha() == null
                ? ""
                : documento.getFecha().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );

        escribirTexto(hoja, 14, 12, fecha, estilos);

        escribirTexto(
                hoja, 10, 19,
                documento.getInicio(),
                estilos
        );

        escribirTexto(
                hoja, 13, 19,
                documento.getFin(),
                estilos
        );

        // Nombre del técnico en la parte superior.
        escribirTexto(
                hoja, 16, 0,
                "TÉCNICO DE NEUMÁTICOS: "
                        + valor(documento.getResponsable1()),
                estilos
        );

        hoja.getRow(16).setHeightInPoints(18);

        // Las filas de la plantilla se ubican según la posición
        // de cada neumático.
        for (FilaDocumento fila : documento.getFilas()) {

            if (!fila.isUsada()) {
                continue;
            }

            int posicion = fila.getPosicion();

            // Sección: neumáticos retirados.
            int retirada = 21 + (posicion - 1) * 2;

            escribirTexto(
                    hoja, retirada, 1,
                    fila.getCodigo(), estilos
            );

            escribirTexto(
                    hoja, retirada, 4,
                    fila.getMarca(), estilos
            );

            escribirTexto(
                    hoja, retirada, 7,
                    fila.getDiseno(), estilos
            );

            escribirTexto(
                    hoja, retirada, 10,
                    fila.getMedida(), estilos
            );

            escribirNumero(
                    hoja, retirada, 13,
                    fila.getPromedio(), estilos
            );

            escribirTexto(
                    hoja, retirada, 16,
                    fila.getDisposicion(), estilos
            );

            // Sección: neumáticos montados.
            int montada = 50 + (posicion - 1) * 2;

            escribirTexto(
                    hoja, montada, 1,
                    fila.getCodigoNuevo(), estilos
            );

            escribirTexto(
                    hoja, montada, 4,
                    fila.getMarcaNueva(), estilos
            );

            escribirTexto(
                    hoja, montada, 9,
                    fila.getDisenoNuevo(), estilos
            );

            escribirTexto(
                    hoja, montada, 13,
                    fila.getMedidaNueva(), estilos
            );

            escribirNumero(
                    hoja, montada, 16,
                    fila.getPromedioNuevo(), estilos
            );
        }

        llenarObservaciones(hoja, documento, estilos);

        // Nombres en los espacios de conformidad.
        escribirTexto(
                hoja, 82, 1,
                "Nombre: " + valor(documento.getResponsable1()),
                estilos
        );

        escribirTexto(
                hoja, 82, 8,
                "Nombre: " + valor(documento.getResponsable2()),
                estilos
        );

        escribirTexto(
                hoja, 82, 15,
                "Nombre: " + valor(documento.getResponsable3()),
                estilos
        );

        escribirTexto(
                hoja, 90, 15,
                "JEFE DE MANTENIMIENTO",
                estilos
        );
    }

    private void llenarObservaciones(
        Sheet hoja,
        DocumentoForm documento,
        Map<Short, CellStyle> estilos
) {
    // Escribe la observación manual en el espacio original
    // de la plantilla auditada.
    escribirTexto(
            hoja,
            77,
            0,
            documento.getObservaciones(),
            estilos
    );
}

    private String valor(Object dato) {
        return dato == null ? "" : dato.toString();
    }

    private Cell obtenerCelda(
            Sheet hoja,
            int fila,
            int columna,
            Map<Short, CellStyle> estilos
    ) {
        Row renglon = hoja.getRow(fila);

        if (renglon == null) {
            renglon = hoja.createRow(fila);
        }

        Cell celda = renglon.getCell(columna);

        if (celda == null) {
            celda = renglon.createCell(columna);
        }

        CellStyle original = celda.getCellStyle();
        CellStyle estilo = estilos.get(original.getIndex());

        if (estilo == null) {

            Workbook libro = hoja.getWorkbook();

            estilo = libro.createCellStyle();
            estilo.cloneStyleFrom(original);

            Font fuenteOriginal = libro.getFontAt(
                    original.getFontIndex()
            );

            Font fuente = libro.createFont();

            fuente.setFontName("Arial");
            fuente.setFontHeightInPoints((short) 10);
            fuente.setBold(fuenteOriginal.getBold());
            fuente.setItalic(fuenteOriginal.getItalic());
            fuente.setColor(fuenteOriginal.getColor());
            fuente.setUnderline(fuenteOriginal.getUnderline());

            estilo.setFont(fuente);
            estilo.setWrapText(true);

            estilos.put(original.getIndex(), estilo);
        }

        celda.setCellStyle(estilo);

        return celda;
    }

    private void escribirTexto(
            Sheet hoja,
            int fila,
            int columna,
            Object dato,
            Map<Short, CellStyle> estilos
    ) {
        Cell celda = obtenerCelda(
                hoja, fila, columna, estilos
        );

        celda.setCellValue(valor(dato));
    }

    private void escribirNumero(
            Sheet hoja,
            int fila,
            int columna,
            BigDecimal dato,
            Map<Short, CellStyle> estilos
    ) {
        Cell celda = obtenerCelda(
                hoja, fila, columna, estilos
        );

        if (dato == null) {
            celda.setBlank();
        } else {
            celda.setCellValue(dato.doubleValue());
        }
    }
}