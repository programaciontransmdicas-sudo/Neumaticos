package com.transmdicas.neumaticos.modulo;

import static org.junit.jupiter.api.Assertions.*;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.*;
import com.transmdicas.neumaticos.modulo.service.*;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CambiosModuloTest {
  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired DocumentoService documentos;
  @Autowired IndicadorSalidasService indicadores;
  @Autowired MantenimientoCatalogoService catalogo;
  @Autowired MantenimientoCatalogoRepository catalogos;
  @Autowired DisposicionRepository disposiciones;
  HttpClient http;

  @BeforeEach
  void init() {
    jdbc.update("DELETE FROM neumaticos.salidas_almacen");
    jdbc.update("DELETE FROM neumaticos.documentos");
    jdbc.update("DELETE FROM neumaticos.disposiciones");
    jdbc.update("DELETE FROM neumaticos.catalogo_neumaticos");
    jdbc.update("DELETE FROM neumaticos.unidades");
    jdbc.update(
        "INSERT INTO neumaticos.unidades(placa,marca,operacion,ruta) VALUES"
            + " ('ABC123','Volvo','Operación','Ruta')");
    jdbc.update(
        "INSERT INTO"
            + " neumaticos.catalogo_neumaticos(codigo,marca,diseno,medida,reencauche1,reencauche2)"
            + " VALUES ('N001','Marca inicial','Modelo A','11R22.5','','')");
    http =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
  }

  HttpResponse<String> get(String ruta) throws Exception {
    return http.send(
        HttpRequest.newBuilder(
                URI.create(ruta.startsWith("http") ? ruta : "http://localhost:" + port + ruta))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  HttpResponse<String> post(String ruta, Map<String, String> d) throws Exception {
    String b =
        d.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(java.util.stream.Collectors.joining("&"));
    return http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(b))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  Map<String, String> inputs(String s) {
    Map<String, String> d = new HashMap<>();
    var tags = Pattern.compile("<input\\b[^>]*>").matcher(s);
    while (tags.find()) {
      var tag = tags.group();
      if (tag.contains("type=\"checkbox\"")) continue;
      var n = Pattern.compile("name=\"([^\"]+)\"").matcher(tag);
      var v = Pattern.compile("value=\"([^\"]*)\"").matcher(tag);
      if (n.find()) d.put(n.group(1), v.find() ? v.group(1).replace("&amp;", "&") : "");
    }
    return d;
  }

  DocumentoForm inspeccion() {
    var d = DocumentoForm.nuevo(TipoDocumento.INSPECCION);
    d.setPlaca("ABC123");
    d.setOperador("Operador");
    d.setKilometraje(1L);
    d.setFecha(LocalDate.of(2026, 1, 2));
    var f = d.getFilas().get(0);
    f.setCodigo("N001");
    f.setRn("N");
    f.setR1(new BigDecimal("6"));
    f.setR2(new BigDecimal("6"));
    f.setPresion(new BigDecimal("100"));
    f.setAro("C");
    f.setCondicion("Bueno");
    f.setSinTapa("NO");
    return d;
  }

  @Test
  void limitesDeAlertasSinHuecos() {
    assertEquals(EstadoRemanente.SIN_DATOS, EstadoRemanente.de(null));
    for (String v : List.of("0", "5.99", "6", "6.00"))
      assertEquals(EstadoRemanente.CAMBIO, EstadoRemanente.de(new BigDecimal(v)), v);
    for (String v : List.of("6.01", "6.5", "7", "8.99"))
      assertEquals(EstadoRemanente.ADVERTENCIA, EstadoRemanente.de(new BigDecimal(v)), v);
    for (String v : List.of("9", "9.01", "20"))
      assertEquals(EstadoRemanente.CONFORME, EstadoRemanente.de(new BigDecimal(v)), v);
    var f = new FilaDocumento();
    f.setR1(new BigDecimal("6"));
    f.setR2(new BigDecimal("7"));
    assertEquals(EstadoRemanente.ADVERTENCIA, f.getAlerta());
    f.setN1(new BigDecimal("8"));
    f.setN2(new BigDecimal("10"));
    assertEquals(EstadoRemanente.CONFORME, f.getAlertaNuevo());
  }

  @Test
  void inspeccionNoMuestraOtYDiagnosticaEnObservaciones() throws Exception {
    String pagina = get("/documentos").body();
    assertFalse(pagina.contains("name=\"ot\""));
    var d = inspeccion();
    d.setOt("OT-NO-DEBE-GUARDARSE");
    Long id = documentos.guardar(d, true);
    assertEquals("", documentos.abrir(id).getOt());
    jdbc.update("UPDATE neumaticos.documentos SET ot='ANTIGUA' WHERE id=?", id);
    String vista = get("/documentos/" + id + "/vista").body();
    assertFalse(vista.contains("<b>OT:</b>"));
    assertTrue(vista.contains("Remanente: Por revisar"));
    assertTrue(vista.contains("Presión baja"));
    assertFalse(vista.contains("remanente-alerta"));
  }

  @Test
  void tecnicoSoloArribaYDisposicionPersistente() throws Exception {
    var r = get("/documentos/nuevo?tipo=CAMBIO");
    String html = r.body();
    assertEquals(200, r.statusCode());
    assertEquals(1, html.split("name=\"responsable1\"", -1).length - 1);
    assertTrue(
        html.indexOf("name=\"responsable1\"") < html.indexOf("Neumáticos retirados y montados"));
    assertTrue(html.contains("Jefe de mantenimiento"));
    assertFalse(html.contains("Jefe de taller"));
    var d = inputs(html);
    d.put("responsable1", "Técnico nuevo");
    d.put("operador", "Operador conservado");
    d.put("nuevaDisposicion", "  Recuperación  externa ");
    d.put("accion", "agregarDisposicion");
    r = post("/documentos/procesar", d);
    assertEquals(200, r.statusCode(), r.body());
    assertEquals("Técnico nuevo", org.springframework.web.util.HtmlUtils.htmlUnescape(inputs(r.body()).get("responsable1")));
    assertTrue(r.body().contains("Operador conservado"));
    assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(r.body()).contains("RECUPERACIÓN EXTERNA"));
    disposiciones.agregar("recuperacion externa");
    assertEquals(1, disposiciones.listar().size());
    assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(get("/documentos/nuevo?tipo=CAMBIO").body()).contains("RECUPERACIÓN EXTERNA"));
  }

  void salida(String fecha, int cantidad, String marca, String modelo) {
    jdbc.update(
        "INSERT INTO"
            + " neumaticos.salidas_almacen(origen,referencia,fecha,codigo,marca,modelo,cantidad)"
            + " VALUES ('MANUAL',?,?,?,?,?,?)",
        UUID.randomUUID().toString(),
        LocalDate.parse(fecha),
        "N001",
        marca,
        modelo,
        cantidad);
  }

  @Test
  void agrupacionesSumandoCantidadConFiltrosYLimitesCalendario() {
    String[] f = {
      "2026-01-01",
      "2026-01-15",
      "2026-01-16",
      "2026-01-31",
      "2026-02-01",
      "2026-06-30",
      "2026-07-01",
      "2026-12-31"
    };
    int[] q = {2, 3, 4, 1, 5, 6, 7, 8};
    for (int i = 0; i < f.length; i++) salida(f[i], q[i], "Marca A", "Modelo A");
    salida("2025-12-31", 100, "Marca A", "Modelo A");
    salida("2027-01-01", 100, "Marca A", "Modelo A");
    salida("2026-01-01", 40, "Otra marca", "Modelo B");
    var mensual = indicadores.calcular(2026, PeriodoSalidas.MENSUAL, "marca a", "modelo a");
    assertEquals(36, mensual.unidades());
    assertEquals(8, mensual.movimientos());
    assertEquals(12, mensual.periodos().size());
    assertEquals(10, mensual.periodos().get(0).unidades());
    var qn = indicadores.calcular(2026, PeriodoSalidas.QUINCENAL, "Marca A", "");
    assertEquals(24, qn.periodos().size());
    assertEquals(5, qn.periodos().get(0).unidades());
    assertEquals(5, qn.periodos().get(1).unidades());
    assertEquals(LocalDate.of(2026, 1, 31), qn.periodos().get(1).hasta());
    var semestre = indicadores.calcular(2026, PeriodoSalidas.SEMESTRAL, "Marca A", "");
    assertEquals(21, semestre.periodos().get(0).unidades());
    assertEquals(15, semestre.periodos().get(1).unidades());
    assertEquals(76, indicadores.calcular(2026, PeriodoSalidas.MENSUAL, "", "").unidades());
  }

  @Test
  void quincenaBisiestaYPeriodosSinMovimientos() {
    salida("2024-02-29", 5, "M", "D");
    var q = indicadores.calcular(2024, PeriodoSalidas.QUINCENAL, "", "");
    assertEquals(LocalDate.of(2024, 2, 29), q.periodos().get(3).hasta());
    assertEquals(5, q.periodos().get(3).unidades());
    assertEquals(0, q.periodos().get(0).unidades());
    assertEquals(0, indicadores.calcular(2023, PeriodoSalidas.SEMESTRAL, "", "").unidades());
  }

  @Test
  void altaSalidasNoCuentaDocumentosYSnapshotMarca() {
    documentos.guardar(inspeccion(), true);
    assertEquals(0, indicadores.calcular(2026, PeriodoSalidas.MENSUAL, "", "").unidades());
    var f = new SalidaForm();
    f.setFecha("2026-11-01");
    f.setCodigo("N001");
    f.setCantidad(3);
    indicadores.registrar(f);
    indicadores.registrar(f);
    assertEquals(
        3, indicadores.calcular(2026, PeriodoSalidas.MENSUAL, "Marca inicial", "").unidades());
    var cat = catalogos.buscar(false, "N001").orElseThrow();
    cat.setMarca("Marca editada");
    catalogo.guardar(false, cat);
    assertEquals(
        3, indicadores.calcular(2026, PeriodoSalidas.MENSUAL, "Marca inicial", "").unidades());
    assertEquals(
        0, indicadores.calcular(2026, PeriodoSalidas.MENSUAL, "Marca editada", "").unidades());
  }

  @Test
  void altasEdicionesCatalogosVersionesEHistoria() {
    Long id = documentos.guardar(inspeccion(), true);
    var f = new CatalogoForm();
    f.setClave("N002");
    f.setMarca("Marca nueva");
    f.setModelo("Modelo nuevo");
    f.setMedida("12R22.5");
    catalogo.guardar(false, f);
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> catalogo.guardar(false, f));
    var a = catalogos.buscar(false, "N001").orElseThrow();
    var obsoleto = catalogos.buscar(false, "N001").orElseThrow();
    a.setMarca("Editada");
    a.setModelo("Diseño editado");
    catalogo.guardar(false, a);
    assertThrows(
        org.springframework.dao.OptimisticLockingFailureException.class,
        () -> catalogo.guardar(false, obsoleto));
    assertEquals("Marca inicial", documentos.abrir(id).getFilas().get(0).getMarca());
    var u = new CatalogoForm();
    u.setClave(" xyz987 ");
    u.setMarca("Hino");
    u.setOperacion("Industria");
    u.setRuta("Ruta nueva");
    catalogo.guardar(true, u);
    assertTrue(catalogos.buscar(true, "XYZ987").isPresent());
    u = catalogos.buscar(true, "XYZ987").orElseThrow();
    u.setSemirremolque("REM001");
    catalogo.guardar(true, u);
    assertEquals("REM001", catalogos.buscar(true, "XYZ987").orElseThrow().getSemirremolque());
  }

  @Test
  void pantallasCrudIndicadoresCsvYProteccionCsrf() throws Exception {
    for (String ruta :
        List.of(
            "/catalogos/neumaticos/nuevo",
            "/catalogos/neumaticos/editar?clave=N001",
            "/catalogos/flota/nuevo",
            "/catalogos/flota/editar?clave=ABC123",
            "/reportes/salidas?anio=2026&periodo=QUINCENAL",
            "/reportes/salidas?anio=2026&periodo=SEMESTRAL")) {
      var r = get(ruta);
      assertEquals(200, r.statusCode(), ruta + " " + r.body());
    }
    assertEquals(403, post("/catalogos/neumaticos/guardar", Map.of()).statusCode());
    assertEquals(403, post("/salidas/registrar", Map.of()).statusCode());
    var d = inputs(get("/catalogos/neumaticos/nuevo").body());
    d.put("clave", "N003");
    d.put("marca", "Nueva");
    d.put("modelo", "Diseño");
    d.put("medida", "11R22.5");
    var guardado = post("/catalogos/neumaticos/guardar", d);
    assertEquals(302, guardado.statusCode(), guardado.body());
    assertTrue(
        get(guardado.headers().firstValue("location").orElseThrow()).body().contains("N003"));
    salida("2026-01-01", 2, "Marca A", "Modelo A");
    var csv =
        get("/reportes/salidas.csv?anio=2026&periodo=MENSUAL&marca=Marca%20A&modelo=Modelo%20A");
    assertEquals(200, csv.statusCode());
    assertTrue(csv.body().contains("Marca A"));
    assertTrue(csv.body().contains("Modelo A"));
    assertEquals(400, get("/reportes/salidas?anio=1800").statusCode());
  }

  @Test
  void datosHistoricosCompletosYFechasValidas() throws Exception {
    try (var lector =
        new java.io.BufferedReader(
            new java.io.InputStreamReader(
                new org.springframework.core.io.ClassPathResource("catalogos/salidas-almacen.tsv")
                    .getInputStream(),
                StandardCharsets.UTF_8))) {
      var filas = lector.lines().skip(1).toList();
      assertEquals(2122, filas.size());
      long cantidad = 0;
      for (String linea : filas) {
        var f = linea.split("\t", -1);
        assertEquals(8, f.length);
        LocalDate.parse(f[1]);
        cantidad += Integer.parseInt(f[6]);
      }
      assertEquals(2122, cantidad);
    }
  }
}
