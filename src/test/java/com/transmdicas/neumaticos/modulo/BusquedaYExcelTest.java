package com.transmdicas.neumaticos.modulo;

import static org.junit.jupiter.api.Assertions.*;
import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.OperadorRepository;
import com.transmdicas.neumaticos.modulo.service.DocumentoService;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BusquedaYExcelTest {
  @Value("${local.server.port}") int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired DocumentoService documentos;
  final HttpClient http = HttpClient.newHttpClient();

  HttpResponse<String> get(String ruta) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta)).GET().build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  @Test
  void buscaCatalogosSinModificarElDocumento() throws Exception {
    jdbc.update("DELETE FROM neumaticos.operadores");
    jdbc.update("INSERT INTO neumaticos.operadores(busqueda,nombre) VALUES (?,?)",
        OperadorRepository.normalizar("PÉREZ, José"), "PÉREZ, José");
    assertTrue(get("/api/catalogos/operadores?q=perez").body().contains("PÉREZ, José"));
    assertEquals("[]", get("/api/catalogos/operadores?q=%25").body());
    assertEquals("[]", get("/api/catalogos/operadores?q=%27").body());
    assertEquals(200, get("/api/catalogos/flota?q=ABC").statusCode());
    var r = get("/api/catalogos/neumaticos?q=N");
    assertEquals(200, r.statusCode());
    assertTrue(r.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
    var editor = get("/documentos/nuevo?tipo=INSPECCION");
    assertEquals(200, editor.statusCode());
    assertTrue(editor.body().contains("data-busqueda=\"operadores\""));
    assertTrue(editor.body().contains("/js/editor.js"));
    assertTrue(editor.body().contains("data-rotaciones"));
    assertFalse(editor.body().contains("remanente-alerta"));
  }

  @Test
  void descargaLosTresFormatosYConservaObservaciones() throws Exception {
    for (var tipo : TipoDocumento.values()) {
      var d = DocumentoForm.nuevo(tipo);
      d.setOperador("Operador de prueba");
      d.setObservaciones("Nota manual & revisión");
      Long id = documentos.guardar(d, false);
      var vista = get("/documentos/" + id + "/vista");
      assertEquals(200, vista.statusCode());
      assertTrue(vista.body().contains("/documentos/" + id + "/excel"));
      var r = http.send(HttpRequest.newBuilder(
          URI.create("http://localhost:" + port + "/documentos/" + id + "/excel")).GET().build(),
          HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(200, r.statusCode());
      assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          r.headers().firstValue("Content-Type").orElse(""));
      Map<String,String> xml = new HashMap<>();
      try (var zip = new ZipInputStream(new ByteArrayInputStream(r.body()))) {
        for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
          if (e.getName().endsWith(".xml")) {
            xml.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
          }
        }
      }
      String hoja = xml.get("xl/worksheets/sheet1.xml");
      assertTrue(hoja.contains(tipo.getPapel()));
      assertTrue(hoja.contains("Nota manual &amp; revisión"));
      assertTrue(hoja.contains("orientation=\"portrait\""));
      if (tipo != TipoDocumento.REQUERIMIENTO) {
        assertTrue(hoja.contains("Neumático 12"));
        assertTrue(hoja.contains("OBSERVACIONES"));
      }
    }
  }
}
