package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.DocumentoForm;
import com.transmdicas.neumaticos.modulo.model.FilaDocumento;
import com.transmdicas.neumaticos.modulo.model.ObservacionesNeumaticos;
import com.transmdicas.neumaticos.modulo.model.TipoDocumento;
import com.transmdicas.neumaticos.modulo.service.DocumentoService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.transmdicas.neumaticos.util.ConversorPdf;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class InspeccionExcelController {
    private final DocumentoService documentoService;

    public InspeccionExcelController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping("/documentos/{id}/excel-inspeccion-auditado")
    public ResponseEntity<byte[]> descargar(@PathVariable("id") Long id) throws IOException {
        DocumentoForm documento = documentoService.abrir(id);

        if (documento.getTipo() != TipoDocumento.INSPECCION) {
            throw new ResponseStatusException(NOT_FOUND, "El documento no es una inspección.");
        }

        ClassPathResource archivo = new ClassPathResource(
                "formatos/auditados/inspeccion.xlsm"
        );

        try (
                InputStream entrada = archivo.getInputStream();
                Workbook libro = WorkbookFactory.create(entrada);
                ByteArrayOutputStream salida = new ByteArrayOutputStream()
        ) {
            Sheet hoja = libro.getSheet("V4");

            if (hoja == null) {
                throw new IOException("No se encontró la hoja V4 en el formato auditado.");
            }

            // Conservar solo la hoja que contiene el formato de inspección.
for (int i = libro.getNumberOfSheets() - 1; i >= 0; i--) {
    if (!libro.getSheetName(i).equals("V4")) {
        libro.removeSheetAt(i);
    }
}

libro.setSheetHidden(0, false);
libro.setActiveSheet(0);
libro.setSelectedTab(0);

llenarHoja(hoja, documento);

// La parte superior de la hoja corresponde al remolcador.
// La parte inferior pertenece al formato de semirremolcador.
libro.setPrintArea(0, "$A$1:$R$58");

hoja.setHorizontallyCenter(true);
hoja.setMargin(Sheet.LeftMargin, 0.3);
hoja.setMargin(Sheet.RightMargin, 0.3);
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
                "attachment; filename=inspeccion-" + id + ".pdf"
        )
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
        }
    }

    private void llenarHoja(Sheet hoja, DocumentoForm documento) {
        // Datos generales del formato MNT-F-37.
        escribir(hoja, 1, 14, documento.getNumero());         // O2
        escribir(hoja, 8, 2, documento.getPlaca());           // C9
        escribir(hoja, 8, 7, documento.getKilometraje());     // H9
        escribir(hoja, 8, 13, documento.getOperador());       // N9
        escribir(hoja, 9, 2, documento.getMarcaUnidad());    // C10
        escribir(hoja, 9, 7, documento.getOperacion());      // H10

        if (documento.getFecha() != null) {
            String fecha = documento.getFecha().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );
            escribir(hoja, 9, 13, fecha);                    // N10
        }

        // La parte inferior corresponde a otro formato, el MNT-F-38.
        // Se limpia el número que venía de ejemplo en la plantilla.
        escribir(hoja, 59, 14, "");                          // O60

        int[] filasIniciales = {16, 17, 19, 20, 21, 22, 24, 25, 26, 27};
        int[] filasRotacion = {33, 34, 36, 37, 38, 39, 41, 42, 43, 44};

        llenarNeumaticos(hoja, documento.getFilas(), filasIniciales);

        if (documento.isRotaciones()) {
            llenarNeumaticos(hoja, documento.getDespues(), filasRotacion);
        }

        // Los nombres se colocan en los espacios de firma.
        escribir(hoja, 52, 1, documento.getResponsable1());   // B53
        escribir(hoja, 52, 7, documento.getResponsable2());   // H53
        escribir(hoja, 52, 13, documento.getResponsable3()); // N53

        

        if (documento.getObservaciones() != null) {
            escribir(hoja, 49, 1, documento.getObservaciones()); // B50
        }
    }

    private void llenarNeumaticos(
            Sheet hoja,
            List<FilaDocumento> lista,
            int[] filasExcel) {

        for (int i = 0; i < filasExcel.length; i++) {
            int posicion = i + 1;
            FilaDocumento neumatico = null;

            for (FilaDocumento fila : lista) {
                if (Objects.equals(fila.getPosicion(), posicion)) {
                    neumatico = fila;
                    break;
                }
            }

            if (neumatico == null || !neumatico.isUsada()) {
                continue;
            }

            // Las posiciones del arreglo son números visibles en Excel.
            int fila = filasExcel[i] - 1;

            escribir(hoja, fila, 6, neumatico.getMedida());       // G
            escribir(hoja, fila, 7, neumatico.getMarca());        // H
            escribir(hoja, fila, 9, neumatico.getDiseno());       // J
            escribir(hoja, fila, 11, neumatico.getRn());          // L
            escribir(hoja, fila, 12, neumatico.getPromedio());    // M
            escribir(hoja, fila, 13, neumatico.getCodigo());      // N
            escribir(hoja, fila, 14, neumatico.getPresion());     // O
            escribir(hoja, fila, 15, neumatico.getAro());         // P
            escribir(hoja, fila, 16, neumatico.getCondicion());   // Q
            escribir(hoja, fila, 17, neumatico.getSinTapa());     // R
        }
    }

    private void llenarObservaciones(Sheet hoja, DocumentoForm documento) {
        // En la plantilla original las filas 47, 48 y 49 están combinadas.
        String[] anteriores = {"B47:R47", "B48:R48", "B49:R49"};

        for (int i = hoja.getNumMergedRegions() - 1; i >= 0; i--) {
            String rango = hoja.getMergedRegion(i).formatAsString();

            for (String anterior : anteriores) {
                if (anterior.equals(rango)) {
                    hoja.removeMergedRegion(i);
                    break;
                }
            }
        }

        int[] primera = {1, 2, 3, 4, 5, 7, 9, 11, 13, 14, 15, 17};
        int[] ultima  = {1, 2, 3, 4, 6, 8, 10, 12, 13, 14, 16, 17};

        CellStyle estiloTitulo = crearEstiloObservacion(hoja, true);
        CellStyle estiloTexto = crearEstiloObservacion(hoja, false);

        // Se aumenta el alto de esta zona para que los diagnósticos se lean.
        hoja.getRow(46).setHeightInPoints(28);
        hoja.getRow(47).setHeightInPoints(45);
        hoja.getRow(48).setHeightInPoints(45);

        List<ObservacionesNeumaticos.Seccion> secciones =
                ObservacionesNeumaticos.secciones(documento);

        for (int i = 0; i < 12; i++) {
            CellRangeAddress titulo = new CellRangeAddress(
                    46, 46, primera[i], ultima[i]
            );
            CellRangeAddress detalle = new CellRangeAddress(
                    47, 48, primera[i], ultima[i]
            );

            if (primera[i] != ultima[i]) {
                hoja.addMergedRegion(titulo);
            }

            hoja.addMergedRegion(detalle);

            Cell cabecera = obtenerCelda(hoja, 46, primera[i]);
            cabecera.setCellValue(
                    String.format(Locale.ROOT, "Neumático %02d", i + 1)
            );
            cabecera.setCellStyle(estiloTitulo);

            StringBuilder texto = new StringBuilder();

            for (ObservacionesNeumaticos.Seccion seccion : secciones) {
                String diagnostico = seccion.bloques().get(0).get(i).texto();

                if (diagnostico == null || diagnostico.isBlank()) {
                    continue;
                }

                if (!texto.isEmpty()) {
                    texto.append("\n");
                }

                if (secciones.size() > 1) {
                    texto.append(seccion.titulo()).append(":\n");
                }

                texto.append(diagnostico);
            }

            Cell resultado = obtenerCelda(hoja, 47, primera[i]);
            resultado.setCellValue(texto.toString());
            resultado.setCellStyle(estiloTexto);

            bordes(hoja, titulo);
            bordes(hoja, detalle);
        }
    }

    private CellStyle crearEstiloObservacion(Sheet hoja, boolean titulo) {
        Workbook libro = hoja.getWorkbook();
        CellStyle estilo = libro.createCellStyle();
        estilo.cloneStyleFrom(obtenerCelda(hoja, 46, 1).getCellStyle());

        Font fuente = libro.createFont();
        fuente.setFontName("Arial");
        fuente.setFontHeightInPoints((short) 10);
        fuente.setBold(titulo);

        estilo.setFont(fuente);
        estilo.setWrapText(true);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(
                titulo ? VerticalAlignment.CENTER : VerticalAlignment.TOP
        );

        return estilo;
    }

    private void bordes(Sheet hoja, CellRangeAddress rango) {
        RegionUtil.setBorderTop(BorderStyle.THIN, rango, hoja);
        RegionUtil.setBorderBottom(BorderStyle.THIN, rango, hoja);
        RegionUtil.setBorderLeft(BorderStyle.THIN, rango, hoja);
        RegionUtil.setBorderRight(BorderStyle.THIN, rango, hoja);
    }

    private void escribir(Sheet hoja, int numeroFila, int numeroColumna, Object valor) {
        Cell celda = obtenerCelda(hoja, numeroFila, numeroColumna);
        aplicarArialDiez(celda);

        if (valor == null) {
            celda.setBlank();
        } else if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
        } else {
            celda.setCellValue(valor.toString());
        }
    }

    private void aplicarArialDiez(Cell celda) {
        Workbook libro = celda.getSheet().getWorkbook();
        CellStyle original = celda.getCellStyle();
        CellStyle copia = libro.createCellStyle();
        copia.cloneStyleFrom(original);

        Font fuenteOriginal = libro.getFontAt(original.getFontIndex());
        Font arial = libro.createFont();
        arial.setFontName("Arial");
        arial.setFontHeightInPoints((short) 10);
        arial.setBold(fuenteOriginal.getBold());
        arial.setItalic(fuenteOriginal.getItalic());
        arial.setUnderline(fuenteOriginal.getUnderline());
        arial.setColor(fuenteOriginal.getColor());

        copia.setFont(arial);
        celda.setCellStyle(copia);
    }

    private Cell obtenerCelda(Sheet hoja, int numeroFila, int numeroColumna) {
        Row fila = hoja.getRow(numeroFila);

        if (fila == null) {
            fila = hoja.createRow(numeroFila);
        }

        Cell celda = fila.getCell(numeroColumna);

        if (celda == null) {
            celda = fila.createCell(numeroColumna);
        }

        return celda;
    }
}
