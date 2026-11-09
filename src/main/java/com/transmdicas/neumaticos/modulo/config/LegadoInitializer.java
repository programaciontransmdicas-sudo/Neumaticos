package com.transmdicas.neumaticos.modulo.config;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.DocumentoRepository;
import java.sql.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "modulo.legado.importar", havingValue = "true", matchIfMissing = true)
@Order(20)
public class LegadoInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final DocumentoRepository repo;

  public LegadoInitializer(JdbcTemplate jdbc, DocumentoRepository repo) {
    this.jdbc = jdbc;
    this.repo = repo;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) throws Exception {
    if (!existe("inspecciones") || !existe("inspeccion_detalles")) return;
    for (var fila : jdbc.queryForList("SELECT * FROM neumaticos.inspecciones ORDER BY id")) {
      long original = ((Number) fila.get("id")).longValue();
      String token = "legado-inspeccion:" + original;
      if (repo.porToken(token).isPresent()
          || jdbc.queryForObject(
                  "SELECT COUNT(*) FROM neumaticos.importaciones_modulo WHERE clave=?",
                  Long.class,
                  token)
              > 0) continue;
      var d = DocumentoForm.nuevo(TipoDocumento.INSPECCION);
      d.setToken(token);
      d.setPlaca(texto(fila.get("placa")));
      d.setMarcaUnidad(texto(fila.get("marca")));
      d.setOperacion(texto(fila.get("operacion")));
      d.setRuta(texto(fila.get("ruta")));
      d.setOperador(texto(fila.get("operador")));
      d.setObservaciones(texto(fila.get("observaciones")));
      if (fila.get("kilometraje") != null)
        d.setKilometraje(((Number) fila.get("kilometraje")).longValue());
      if (fila.get("fecha") instanceof java.sql.Date fecha) d.setFecha(fecha.toLocalDate());
      for (var f :
          jdbc.queryForList(
              "SELECT * FROM neumaticos.inspeccion_detalles WHERE inspeccion_id=? ORDER BY"
                  + " posicion",
              original)) {
        int pos = ((Number) f.get("posicion")).intValue();
        if (pos < 1 || pos > 10) continue;
        var r = d.getFilas().get(pos - 1);
        r.setCodigo(texto(f.get("codigo")));
        r.setRn(texto(f.get("rn")));
        r.setR1((java.math.BigDecimal) f.get("r1"));
        r.setR2((java.math.BigDecimal) f.get("r2"));
        r.setPresion((java.math.BigDecimal) f.get("presion"));
        r.setAro(texto(f.get("aro")));
        r.setCondicion(texto(f.get("estado")));
        r.setSinTapa(Boolean.TRUE.equals(f.get("sin_tapa")) ? "SI" : "NO");
        jdbc.query(
            "SELECT marca,diseno,medida FROM neumaticos.catalogo_neumaticos WHERE codigo=?",
            (org.springframework.jdbc.core.RowCallbackHandler)
                t -> {
                  r.setMarca(t.getString("marca"));
                  r.setDiseno(t.getString("diseno"));
                  r.setMedida(t.getString("medida"));
                },
            r.getCodigo());
      }
      repo.guardar(d);
      jdbc.update("INSERT INTO neumaticos.importaciones_modulo(clave) VALUES (?)", token);
    }
  }

  private boolean existe(String tabla) throws SQLException {
    return Boolean.TRUE.equals(
        jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<Boolean>)
                c -> {
                  try (var r =
                      c.getMetaData()
                          .getTables(null, "neumaticos", tabla, new String[] {"TABLE"})) {
                    return r.next();
                  }
                }));
  }

  private String texto(Object v) {
    return v == null ? "" : v.toString();
  }
}
