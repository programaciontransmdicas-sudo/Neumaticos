package com.transmdicas.neumaticos.modulo.exportacion;

import com.transmdicas.neumaticos.modulo.model.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Genera los tres formatos como libros XLSX con celdas y valores editables. */
public final class DocumentoExcel {
  private static final int COLUMNAS = 48;
  private static final int NORMAL = 0;
  private static final int AZUL = 1;
  private static final int ENCABEZADO = 2;
  private static final int TITULO = 3;
  private static final int LOGO = 4;
  private static final int OBSERVACIONES = 5;
  private static final int TEXTO = 6;

  private DocumentoExcel() {}

  public static boolean admite(TipoDocumento tipo) {
    return tipo != null;
  }

  public static void escribir(DocumentoForm d, OutputStream destino) throws IOException {
    if (!admite(d.getTipo())) throw new IllegalArgumentException("Selecciona un formato.");
    Hoja h = new Hoja();
    cabecera(h, d);
    switch (d.getTipo()) {
      case INSPECCION -> {
        inspeccion(h, "INSPECCIÓN DE UNIDAD", d.getFilas());
        inspeccion(h, "ROTACIONES / CAMBIOS", d.isRotaciones() ? d.getDespues() : List.of());
      }
      case CAMBIO -> {
        cambio(h, "NEUMÁTICOS RETIRADOS", d.getFilas(), false);
        cambio(h, "NEUMÁTICOS MONTADOS", d.getFilas(), true);
      }
      case REQUERIMIENTO -> requerimiento(h, d.getFilas());
    }
    if (d.getTipo() != TipoDocumento.REQUERIMIENTO) {
      h.combinada(OBSERVACIONES, 18, "OBSERVACIONES");
      for (var seccion : ObservacionesNeumaticos.secciones(d)) {
        if (!seccion.titulo().isEmpty()) h.combinada(ENCABEZADO, 14, seccion.titulo());
        for (var bloque : seccion.bloques()) {
          List<Celda> encabezados = new ArrayList<>(), textos = new ArrayList<>();
          for (var celda : bloque) {
            encabezados.add(new Celda(4, celda.encabezado(), ENCABEZADO));
            textos.add(new Celda(4, celda.texto(), TEXTO));
          }
          h.fila(30, encabezados);
h.fila(110, textos);
        }
      }
      notas(h, d.getObservaciones());
    } else if (!d.getObservaciones().isBlank()) {
      h.combinada(AZUL, 16, "OBSERVACIONES");
      notas(h, d.getObservaciones());
    }
    firmas(h, d);
    escribirPaquete(h, destino);
  }

  private static void cabecera(Hoja h, DocumentoForm d) {
    h.fila(14, List.of(c(10, "TRANSMDICAS", LOGO), c(26, "FORMATO", AZUL),
        c(12, d.getTipo().getCodigoImpreso(), ENCABEZADO)));
    h.fila(18, List.of(c(10, "", LOGO), c(26, d.getTipo().getPapel(), TITULO),
        c(6, "Versión", ENCABEZADO), c(6, d.getTipo().getVersion(), NORMAL)));
    h.fila(14, List.of(c(10, "", LOGO), c(26, "", TITULO),
        c(6, "Registro", ENCABEZADO), c(6, d.getNumero(), NORMAL)));
    h.fila(14, List.of(c(10, "", LOGO), c(26, "", TITULO),
        c(6, "Estado", ENCABEZADO), c(6, d.getEstado(), NORMAL)));
    h.combinar(1, 1, 4, 10);
    h.combinar(2, 11, 4, 36);
    h.fila(7, List.of(c(48, "", TEXTO)));
    datos(h, "PLACA", d.getPlaca(), d.getTipo() == TipoDocumento.INSPECCION ? "MARCA" : "OT",
        d.getTipo() == TipoDocumento.INSPECCION ? d.getMarcaUnidad() : d.getOt());
    datos(h, "OPERADOR", d.getOperador(), "OPERACIÓN", d.getOperacion());
    datos(h, "KILOMETRAJE", d.getKilometraje(), "FECHA",
        d.getFecha() == null ? "" : d.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    datos(h, "RUTA / CLIENTE", d.getRuta(), "SEMIRREMOLQUE", d.getSemirremolque());
    if (d.getTipo() == TipoDocumento.CAMBIO) {
      datos(h, "TÉCNICO", d.getResponsable1(), "MARCA", d.getMarcaUnidad());
      datos(h, "HORA INICIO", d.getInicio(), "HORA FIN", d.getFin());
    }
    h.fila(7, List.of(c(48, "", TEXTO)));
  }

  private static void datos(Hoja h, String a, Object va, String b, Object vb) {
    h.fila(17, List.of(c(6, a, ENCABEZADO), c(18, va, NORMAL),
        c(6, b, ENCABEZADO), c(18, vb, NORMAL)));
  }

  private static void inspeccion(Hoja h, String titulo, List<FilaDocumento> filas) {
    h.combinada(AZUL, 17, titulo);
    int[] anchos = {12, 2, 4, 4, 4, 2, 4, 5, 3, 2, 3, 3};
    h.fila(26, celdas(ENCABEZADO, anchos, "", "POS.", "MEDIDA", "MARCA", "DISEÑO", "R/N",
        "R1 / R2\n(mm)", "CÓDIGO\nINTERNO", "PRESIÓN\n(PSI)", "ARO\nC/M", "ESTADO",
        "VÁLVULA\nSIN TAPA"));
    int principio = h.numero + 1;
    for (int posicion = 1; posicion <= 10; posicion++) {
      FilaDocumento f = porPosicion(filas, posicion);
      int fila = h.numero + 1;
      h.fila(10, celdas(NORMAL, anchos, "", posicion,
          f == null ? "" : f.getMedida(), f == null ? "" : f.getMarca(),
          f == null ? "" : f.getDiseno(), f == null ? "" : f.getRn(),
          f == null ? null : f.getR1(), f == null ? "" : f.getCodigo(),
          f == null ? null : f.getPresion(), f == null ? "" : f.getAro(),
          f == null ? "" : f.getCondicion(), f == null ? "" : f.getSinTapa()));
      h.fila(10, celdas(NORMAL, anchos, "", "", "", "", "", "",
          f == null ? null : f.getR2(), "", "", "", "", ""));
      int columna = 1;
      for (int i = 0; i < anchos.length; i++) {
        if (i != 0 && i != 6) h.combinar(fila, columna, fila + 1, columna + anchos[i] - 1);
        columna += anchos[i];
      }
    }
    h.combinar(principio, 1, h.numero, 12);
    h.mapas.add(new int[] {principio - 1, h.numero});
  }

  private static FilaDocumento porPosicion(List<FilaDocumento> filas, int posicion) {
    return filas.stream().filter(f -> f.getPosicion() != null && f.getPosicion() == posicion)
        .findFirst().orElse(null);
  }

  private static void cambio(Hoja h, String titulo, List<FilaDocumento> filas, boolean montado) {
    h.combinada(AZUL, 17, titulo);
    int[] anchos = montado ? new int[] {3, 8, 8, 9, 8, 6, 6}
        : new int[] {3, 7, 7, 8, 7, 5, 5, 6};
    Object[] nombres = montado
        ? new Object[] {"POS.", "CÓDIGO", "MARCA", "DISEÑO", "MEDIDA", "R3 (mm)", "R4 (mm)"}
        : new Object[] {"POS.", "CÓDIGO", "MARCA", "DISEÑO", "MEDIDA", "R1 (mm)", "R2 (mm)", "DISPOSICIÓN"};
    h.fila(20, celdas(ENCABEZADO, anchos, nombres));
    for (int i = 0; i < Math.max(12, filas.size()); i++) {
      FilaDocumento f = i < filas.size() ? filas.get(i) : null;
      if (f == null || !f.isUsada()) {
        h.fila(13, celdas(NORMAL, anchos, f == null ? "" : f.getPosicion()));
      } else if (montado) {
        h.fila(13, celdas(NORMAL, anchos, f.getPosicion(), f.getCodigoNuevo(),
            f.getMarcaNueva(), f.getDisenoNuevo(), f.getMedidaNueva(), f.getN1(), f.getN2()));
      } else {
        h.fila(13, celdas(NORMAL, anchos, f.getPosicion(), f.getCodigo(), f.getMarca(),
            f.getDiseno(), f.getMedida(), f.getR1(), f.getR2(), f.getDisposicion()));
      }
    }
  }

  private static void requerimiento(Hoja h, List<FilaDocumento> filas) {
    int[] anchos = {3, 7, 8, 8, 7, 3, 5, 7};
    h.fila(22, celdas(ENCABEZADO, anchos, "ÍTEM", "CÓDIGO", "MARCA", "DISEÑO",
        "MEDIDA", "R/N", "CANTIDAD", "CONFORMIDAD"));
    for (int i = 0; i < Math.max(20, filas.size()); i++) {
      FilaDocumento f = i < filas.size() ? filas.get(i) : null;
      h.fila(20, celdas(NORMAL, anchos, i + 1, f == null ? "" : f.getCodigo(),
          f == null ? "" : f.getMarca(), f == null ? "" : f.getDiseno(),
          f == null ? "" : f.getMedida(), f == null ? "" : f.getRn(),
          f == null ? null : f.getCantidad(), f == null ? "" : f.getConformidad()));
    }
  }

  private static void notas(Hoja h, String texto) {
    // Divide las notas extensas para que Excel y la impresión muestren todo el contenido.
    List<String> renglones = new ArrayList<>();
    for (String linea : texto.split("\\R", -1)) {
      if (linea.isEmpty()) renglones.add("");
      else for (int inicio = 0; inicio < linea.length();) {
        int fin = linea.offsetByCodePoints(inicio, Math.min(105, linea.codePointCount(inicio, linea.length())));
        renglones.add(linea.substring(inicio, fin));
        inicio = fin;
      }
    }
    for (int i = 0; i < Math.max(3, renglones.size()); i++) {
      h.combinada(TEXTO, 16, i < renglones.size() ? renglones.get(i) : "");
    }
  }

  private static void firmas(Hoja h, DocumentoForm d) {
    h.fila(8, List.of(c(48, "", TEXTO)));
    if (d.getTipo() == TipoDocumento.REQUERIMIENTO) {
      h.combinada(AZUL, 17, "FIRMA Y CONFORMIDAD");
    }
    List<String> roles = d.getTipo().getRoles(), nombres = d.getResponsables();
    int ancho = COLUMNAS / roles.size();
    List<Celda> espacios = new ArrayList<>(), responsables = new ArrayList<>(), cargos = new ArrayList<>();
    for (int i = 0; i < roles.size(); i++) {
      espacios.add(c(ancho, "", NORMAL));
      responsables.add(c(ancho, nombres.get(i), NORMAL));
      cargos.add(c(ancho, roles.get(i), ENCABEZADO));
    }
    h.fila(44, espacios);
    h.fila(19, responsables);
    h.fila(24, cargos);
    h.combinada(TEXTO, 14, d.getNumero() + " · CONTROL MANTENIMIENTO");
  }

  private record Celda(int ancho, Object valor, int estilo) {}

  private static Celda c(int ancho, Object valor, int estilo) {
    return new Celda(ancho, valor, estilo);
  }

  private static List<Celda> celdas(int estilo, int[] anchos, Object... valores) {
    List<Celda> celdas = new ArrayList<>();
    for (int i = 0; i < anchos.length; i++) {
      celdas.add(c(anchos[i], i < valores.length ? valores[i] : null, estilo));
    }
    return celdas;
  }

  private static String columna(int n) {
    StringBuilder nombre = new StringBuilder();
    while (n > 0) {
      nombre.insert(0, (char) ('A' + (n - 1) % 26));
      n = (n - 1) / 26;
    }
    return nombre.toString();
  }

  private static String xml(Object valor) {
    return valor.toString()
        .replaceAll("[^\\x{9}\\x{A}\\x{D}\\x{20}-\\x{D7FF}\\x{E000}-\\x{FFFD}\\x{10000}-\\x{10FFFF}]", "")
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static final class Hoja {
    private final StringBuilder filas = new StringBuilder();
    private final List<int[]> combinadas = new ArrayList<>();
    private final List<int[]> mapas = new ArrayList<>();
    private int numero;

    void combinada(int estilo, int alto, String texto) {
      fila(alto, List.of(c(48, texto, estilo)));
    }

    void combinar(int arriba, int izquierda, int abajo, int derecha) {
      combinadas.removeIf(m -> m[0] >= arriba && m[1] >= izquierda && m[2] <= abajo && m[3] <= derecha);
      combinadas.add(new int[] {arriba, izquierda, abajo, derecha});
    }

    void fila(int alto, List<Celda> celdas) {
      numero++;
      filas.append("<row r=\"").append(numero).append("\" ht=\"").append(alto).append("\" customHeight=\"1\">");
      int columna = 1;
      for (Celda celda : celdas) {
        for (int n = 0; n < celda.ancho(); n++) {
          Object valor = n == 0 ? celda.valor() : null;
          filas.append("<c r=\"").append(columna(columna + n)).append(numero)
              .append("\" s=\"").append(celda.estilo()).append('"');
          if (valor == null || "".equals(valor)) filas.append("/>");
          else if (valor instanceof Number) filas.append("><v>").append(valor).append("</v></c>");
          else filas.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
              .append(xml(valor)).append("</t></is></c>");
        }
        if (celda.ancho() > 1) combinar(numero, columna, numero, columna + celda.ancho() - 1);
        columna += celda.ancho();
      }
      if (columna != COLUMNAS + 1) throw new IllegalArgumentException("Anchos de columnas inválidos.");
      filas.append("</row>");
    }

    String contenido() {
      StringBuilder celdas = new StringBuilder();
      for (int[] m : combinadas) {
        celdas.append("<mergeCell ref=\"").append(columna(m[1])).append(m[0]).append(':')
            .append(columna(m[3])).append(m[2]).append("\"/>");
      }
      return """
    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
    <sheetViews><sheetView workbookViewId="0" showGridLines="0"/></sheetViews>
    <cols><col min="1" max="48" width="2.6" customWidth="1"/></cols>
    """
    + "<sheetData>" + filas + "</sheetData>"
    + "<mergeCells count=\"" + combinadas.size() + "\">"
    + celdas
    + "</mergeCells>"
    + """
    <pageMargins left="0.2" right="0.2" top="0.25" bottom="0.25" header="0.1" footer="0.1"/>
    <pageSetup paperSize="9" orientation="portrait" fitToWidth="1" fitToHeight="0"/>
    """
    + (mapas.isEmpty() ? "" : "<drawing r:id=\"rId1\"/>")
    + "</worksheet>";
    }

    String dibujos() {
      StringBuilder dibujos = new StringBuilder("""
          <xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
          """);
      for (int i = 0; i < mapas.size(); i++) {
        int[] p = mapas.get(i);
        dibujos.append("<xdr:twoCellAnchor><xdr:from><xdr:col>1</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>")
            .append(p[0]).append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>")
            .append("<xdr:to><xdr:col>11</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>")
            .append(p[1]).append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>")
            .append("<xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(i + 1)
            .append("\" name=\"Posiciones de neumáticos ").append(i + 1)
            .append("\"/><xdr:cNvPicPr/></xdr:nvPicPr>")
            .append("<xdr:blipFill><a:blip xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" r:embed=\"rId1\"/>")
            .append("<a:stretch><a:fillRect/></a:stretch></xdr:blipFill>")
            .append("<xdr:spPr><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr>")
            .append("</xdr:pic><xdr:clientData/></xdr:twoCellAnchor>");
      }
      return dibujos + "</xdr:wsDr>";
    }
  }

  private static void entrada(ZipOutputStream zip, String nombre, String texto) throws IOException {
    zip.putNextEntry(new ZipEntry(nombre));
    zip.write(texto.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private static void escribirPaquete(Hoja hoja, OutputStream destino) throws IOException {
    try (var zip = new ZipOutputStream(destino, StandardCharsets.UTF_8)) {
      entrada(zip, "[Content_Types].xml", """
          <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Default Extension="png" ContentType="image/png"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
          """ + (hoja.mapas.isEmpty() ? "" : """
          <Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>
          """) + "</Types>");
      entrada(zip, "_rels/.rels", """
          <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
          </Relationships>
          """);
      entrada(zip, "xl/workbook.xml", """
          <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets><sheet name="Formato de neumáticos" sheetId="1" r:id="rId1"/></sheets>
          <definedNames><definedName name="_xlnm.Print_Area" localSheetId="0">'Formato de neumáticos'!$A$1:$AV$
          """.stripTrailing() + hoja.numero + "</definedName></definedNames></workbook>");
      entrada(zip, "xl/_rels/workbook.xml.rels", """
          <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
          </Relationships>
          """);
      entrada(zip, "xl/styles.xml", ESTILOS);
      entrada(zip, "xl/worksheets/sheet1.xml", hoja.contenido());
      if (!hoja.mapas.isEmpty()) {
        entrada(zip, "xl/worksheets/_rels/sheet1.xml.rels", """
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/>
            </Relationships>
            """);
        entrada(zip, "xl/drawings/drawing1.xml", hoja.dibujos());
        entrada(zip, "xl/drawings/_rels/drawing1.xml.rels", """
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/posiciones.png"/>
            </Relationships>
            """);
        zip.putNextEntry(new ZipEntry("xl/media/posiciones.png"));
        try (var mapa = DocumentoExcel.class.getResourceAsStream("/formatos/posiciones.png")) {
          if (mapa == null) throw new IOException("Falta el esquema de posiciones del formato.");
          mapa.transferTo(zip);
        }
        zip.closeEntry();
      }
    }
  }

  private static final String ESTILOS = """
      <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      <fonts count="5">
      <font><sz val="9"/><name val="Arial"/></font>
<font><sz val="9"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
<font><b/><sz val="9"/><color rgb="FF465782"/><name val="Arial"/></font>
<font><b/><sz val="10"/><color rgb="FF465782"/><name val="Arial"/></font>
<font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
      </fonts>
      <fills count="5">
      <fill><patternFill patternType="none"/></fill>
      <fill><patternFill patternType="gray125"/></fill>
      <fill><patternFill patternType="solid"><fgColor rgb="FF7D87B9"/><bgColor indexed="64"/></patternFill></fill>
      <fill><patternFill patternType="solid"><fgColor rgb="FFF0EDF4"/><bgColor indexed="64"/></patternFill></fill>
      <fill><patternFill patternType="solid"><fgColor rgb="FF0070B7"/><bgColor indexed="64"/></patternFill></fill>
      </fills>
      <borders count="2">
      <border><left/><right/><top/><bottom/><diagonal/></border>
      <border><left style="thin"><color rgb="FF7183A1"/></left><right style="thin"><color rgb="FF7183A1"/></right><top style="thin"><color rgb="FF7183A1"/></top><bottom style="thin"><color rgb="FF7183A1"/></bottom><diagonal/></border>
      </borders>
      <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
      <cellXfs count="7">
      <xf fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
      <xf fontId="1" fillId="2" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
      <xf fontId="2" fillId="3" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
      <xf fontId="3" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
      <xf fontId="4" fillId="2" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
      <xf fontId="1" fillId="4" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
      <xf fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
      </cellXfs>
      <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
      </styleSheet>
      """;
}
