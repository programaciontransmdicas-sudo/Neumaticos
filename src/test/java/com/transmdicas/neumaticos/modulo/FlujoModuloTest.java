package com.transmdicas.neumaticos.modulo;

import static org.junit.jupiter.api.Assertions.*;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.*;
import com.transmdicas.neumaticos.modulo.service.*;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.BeanPropertyBindingResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FlujoModuloTest {
  @Test
  void catalogosConservanColumnasYRegistrosDelExcel() throws Exception {
    String[] nombres = {"flota", "neumaticos"};
    int[] columnas = {7, 6};
    int[] registros = {189, 9028};
    for (int i = 0; i < nombres.length; i++) {
      var recurso =
          new org.springframework.core.io.ClassPathResource("catalogos/" + nombres[i] + ".tsv");
      try (var lector =
          new java.io.BufferedReader(
              new java.io.InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
        var filas = lector.lines().skip(1).toList();
        assertEquals(registros[i], filas.size());
        for (String fila : filas) assertEquals(columnas[i], fila.split("\t", -1).length);
      }
    }
  }

  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired DocumentoService service;
  @Autowired DocumentoRepository repo;
  HttpClient http;

  @BeforeEach
  void init() {
    jdbc.update("DELETE FROM neumaticos.documentos");
    jdbc.update("DELETE FROM neumaticos.catalogo_neumaticos");
    jdbc.update("DELETE FROM neumaticos.unidades");
    jdbc.update(
        "INSERT INTO neumaticos.unidades(placa,marca,operacion,ruta) VALUES"
            + " ('ABC123','Volvo','Operación real','Ruta real')");
    for (String codigo : List.of("N001", "N002"))
      jdbc.update(
          "INSERT INTO"
              + " neumaticos.catalogo_neumaticos(codigo,marca,diseno,medida,reencauche1,reencauche2)"
              + " VALUES (?,'Marca real','Diseño real','11R22.5','','')",
          codigo);
    http =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
  }

  HttpResponse<String> get(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(
                URI.create(path.startsWith("http://") ? path : "http://localhost:" + port + path))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  HttpResponse<String> post(String path, Map<String, String> data) throws Exception {
    String body =
        data.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(java.util.stream.Collectors.joining("&"));
    return http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  Map<String, String> inputs(String html) {
    Map<String, String> m = new LinkedHashMap<>();
    var tags = Pattern.compile("<input\\b[^>]*>").matcher(html);
    while (tags.find()) {
      String t = tags.group();
      if (t.contains("type=\"checkbox\"") || t.contains("name=\"_rotaciones\"")) continue;
      var n = Pattern.compile("name=\"([^\"]+)\"").matcher(t);
      var v = Pattern.compile("value=\"([^\"]*)\"").matcher(t);
      if (n.find()) m.put(n.group(1), v.find() ? v.group(1).replace("&amp;", "&") : "");
    }
    return m;
  }

  DocumentoForm valid(TipoDocumento tipo) {
    var d = DocumentoForm.nuevo(tipo);
    d.setPlaca(" abc123 ");
    d.setOperador("Operador prueba");
    d.setKilometraje(100L);
    d.setFecha(LocalDate.of(2026, 11, 2));
    d.setOt("OT-1");
    d.setInicio(java.time.LocalTime.of(8, 0));
    d.setFin(java.time.LocalTime.of(9, 0));
    var f = d.getFilas().get(0);
    f.setCodigo("N001");
    f.setRn("N");
    f.setR1(new BigDecimal("8"));
    f.setR2(new BigDecimal("10"));
    f.setPresion(new BigDecimal("100"));
    f.setAro("C");
    f.setCondicion("Bueno");
    f.setSinTapa("NO");
    if (tipo == TipoDocumento.CAMBIO) {
      f.setCodigoNuevo("N002");
      f.setN1(new BigDecimal("15"));
      f.setN2(new BigDecimal("17"));
      f.setDisposicion("Evaluación");
    }
    if (tipo == TipoDocumento.REQUERIMIENTO) f.setCantidad(2);
    return d;
  }

  @Test
  void paginasYFormulariosRenderizan() throws Exception {
    for (String p :
        List.of(
            "/documentos",
            "/documentos/nuevo?tipo=CAMBIO",
            "/documentos/nuevo?tipo=REQUERIMIENTO",
            "/registros",
            "/catalogos/flota",
            "/catalogos/neumaticos",
            "/reportes",
            "/reglas")) {
      var r = get(p);
      assertEquals(200, r.statusCode(), p + " " + r.body());
      if (p.startsWith("/documentos")) {
        assertTrue(r.body().contains("/js/editor.js"));
      } else {
        assertFalse(r.body().contains("<script"));
      }
    }
  }

  @Test
  void guardarReabrirActualizarYBloquearVersionObsoleta() {
    var d = valid(TipoDocumento.INSPECCION);
    Long id = service.guardar(d, false);
    assertEquals(1, repo.contar(d.getNumero()));
    var a = service.abrir(id);
    var b = service.abrir(id);
    assertEquals("ABC123", a.getPlaca());
    assertEquals("Marca real", a.getFilas().get(0).getMarca());
    assertEquals(new BigDecimal("9.00"), a.getFilas().get(0).getPromedio());
    a.setObservaciones("Actualizado");
    service.guardar(a, false);
    assertEquals(1, repo.contar(""));
    assertThrows(
        org.springframework.dao.OptimisticLockingFailureException.class,
        () -> service.guardar(b, false));
    assertEquals("Actualizado", service.abrir(id).getObservaciones());
    assertEquals(
        20,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM neumaticos.documento_filas WHERE documento_id=?",
            Integer.class,
            id));
  }

  @Test
  void generacionDeLosTresFormatosConservaDatosHistoricos() throws Exception {
    for (var tipo : TipoDocumento.values()) {
      var d = valid(tipo);
      Long id = service.guardar(d, true);
      assertTrue(service.abrir(id).isGenerado());
      assertEquals(200, get("/documentos/" + id + "/vista").statusCode());
      assertThrows(IllegalArgumentException.class, () -> service.guardar(service.abrir(id), false));
    }
    jdbc.update("UPDATE neumaticos.catalogo_neumaticos SET marca='Modificada' WHERE codigo='N001'");
    for (Long id : repo.todosIds())
      assertEquals("Marca real", service.abrir(id).getFilas().get(0).getMarca());
  }

  @Test
  void validacionesYPromedios() {
    var d = valid(TipoDocumento.INSPECCION);
    d.getFilas().get(1).setCodigo("N001");
    var e = new BeanPropertyBindingResult(d, "documento");
    service.enriquecer(d);
    service.validar(d, e, true);
    assertTrue(e.hasErrors());
    var vacio = DocumentoForm.nuevo(TipoDocumento.INSPECCION);
    assertNotNull(service.guardar(vacio, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.guardar(DocumentoForm.nuevo(TipoDocumento.INSPECCION), true));
    jdbc.update(
        "UPDATE neumaticos.catalogo_neumaticos SET requiere_revision=TRUE WHERE codigo='N001'");
    assertThrows(
        IllegalArgumentException.class,
        () -> service.guardar(valid(TipoDocumento.INSPECCION), true));
  }

  @Test
  void placaContradictoriaPermiteBorradorPeroImpideGenerar() {
    jdbc.update("UPDATE neumaticos.unidades SET requiere_revision=TRUE WHERE placa='ABC123'");
    assertNotNull(service.guardar(valid(TipoDocumento.INSPECCION), false));
    var error =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.guardar(valid(TipoDocumento.INSPECCION), true));
    assertTrue(error.getMessage().contains("placa"));
  }

  @Test
  void formularioHttpCsrfActualizacionYPreviewSinPerdida() throws Exception {
    assertEquals(403, post("/documentos/procesar", Map.of("accion", "guardar")).statusCode());
    var d = inputs(get("/documentos").body());
    d.put("placa", "ABC123");
    d.put("operador", "Operador HTTP");
    d.put("filas[0].codigo", "N001");
    d.put("filas[0].r1", "8");
    d.put("filas[0].r2", "10");
    d.put("marcaUnidad", "INVENTADA");
    d.put("accion", "actualizar");
    var r = post("/documentos/procesar", d);
    assertEquals(200, r.statusCode(), r.body());
    assertTrue(r.body().contains("Volvo"));
    assertTrue(r.body().contains("9.00"));
    assertFalse(r.body().contains("INVENTADA"));
    d = inputs(r.body());
    d.put("accion", "vista");
    r = post("/documentos/procesar", d);
    assertEquals(200, r.statusCode(), r.body());
    assertTrue(r.body().contains("BORRADOR"));
    var vuelta = inputs(r.body());
    vuelta.put("accion", "actualizar");
    r = post("/documentos/procesar", vuelta);
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("Operador HTTP"));
    d = inputs(r.body());
    d.put("accion", "guardar");
    r = post("/documentos/procesar", d);
    assertEquals(302, r.statusCode(), r.body());
    assertEquals(1, repo.contar(""));
    assertEquals(200, get(r.headers().firstValue("location").orElseThrow()).statusCode());
  }

  @Test
  void rotacionesYFilasDinamicas() throws Exception {
    var d = inputs(get("/documentos").body());
    d.put("rotaciones", "true");
    d.put("despues[0].codigo", "N002");
    d.put("accion", "actualizar");
    var r = post("/documentos/procesar", d);
    assertEquals(200, r.statusCode(), r.body());
    assertEquals(1, r.body().split("name=\"despues\\[0\\].codigo\"", -1).length - 1);
    d = inputs(r.body());
    d.put("rotaciones", "true");
    d.put("accion", "guardar");
    assertEquals(302, post("/documentos/procesar", d).statusCode());
    assertEquals("N002", service.abrir(repo.todosIds().get(0)).getDespues().get(0).getCodigo());
    d = inputs(get("/documentos/nuevo?tipo=CAMBIO").body());
    d.put("accion", "agregar");
    r = post("/documentos/procesar", d);
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("name=\"filas[2].codigo\""));
    d = inputs(r.body());
    d.put("quitar", "1");
    d.put("accion", "actualizar");
    r = post("/documentos/procesar", d);
    assertEquals(200, r.statusCode());
    assertFalse(r.body().contains("name=\"filas[2].codigo\""));
  }

  @Test
  void exportacionEliminacionEIdempotencia() throws Exception {
    var d = valid(TipoDocumento.REQUERIMIENTO);
    d.setObservaciones("=SUM(1+1)");
    Long id = service.guardar(d, false);
    var copia = valid(TipoDocumento.REQUERIMIENTO);
    copia.setToken(d.getToken());
    assertEquals(id, service.guardar(copia, false));
    assertEquals(1, repo.contar(""));
    var csv = get("/registros/exportar.csv");
    assertEquals(200, csv.statusCode());
    assertTrue(csv.body().contains("'=SUM(1+1)"));
    var form = inputs(get("/documentos/" + id + "/eliminar").body());
    assertEquals(302, post("/documentos/" + id + "/eliminar", form).statusCode());
    assertEquals(0, repo.contar(""));
    assertEquals(
        0, jdbc.queryForObject("SELECT COUNT(*) FROM neumaticos.documento_filas", Integer.class));
  }

  @Test
  void falloEnDetalleRevierteCabecera() {
    jdbc.execute(
        "ALTER TABLE neumaticos.documento_filas ADD CONSTRAINT prueba_rollback CHECK (r1 IS NULL OR"
            + " r1 < 5)");
    try {
      assertThrows(
          org.springframework.dao.DataAccessException.class,
          () -> service.guardar(valid(TipoDocumento.INSPECCION), false));
      assertEquals(0, repo.contar(""));
      assertEquals(
          0, jdbc.queryForObject("SELECT COUNT(*) FROM neumaticos.documento_filas", Integer.class));
    } finally {
      jdbc.execute("ALTER TABLE neumaticos.documento_filas DROP CONSTRAINT prueba_rollback");
    }
  }
}
