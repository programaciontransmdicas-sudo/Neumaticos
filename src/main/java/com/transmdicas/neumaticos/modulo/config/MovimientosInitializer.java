package com.transmdicas.neumaticos.modulo.config;

import com.transmdicas.neumaticos.modulo.repository.DisposicionRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(30)
@ConditionalOnProperty(
    name = "modulo.catalogos.importar",
    havingValue = "true",
    matchIfMissing = true)
public class MovimientosInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final DisposicionRepository disposiciones;

  public MovimientosInitializer(JdbcTemplate jdbc, DisposicionRepository disposiciones) {
    this.jdbc = jdbc;
    this.disposiciones = disposiciones;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) throws Exception {
    try (var lector = lector("catalogos/disposiciones.txt")) {
      for (String opcion : lector.lines().toList())
        if (!opcion.isBlank()) disposiciones.agregar(opcion);
    }
    String clave = "salidas-almacen-excel-2026-v1";
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM neumaticos.importaciones_modulo WHERE clave=?", Long.class, clave)
        > 0) return;
    try (var lector = lector("catalogos/salidas-almacen.tsv")) {
      List<String[]> filas = lector.lines().skip(1).map(l -> l.split("\t", -1)).toList();
      for (var f : filas)
        if (f.length != 8)
          throw new IOException("Registro de salida inválido en la fila del Excel");
      jdbc.batchUpdate(
          "INSERT INTO"
              + " neumaticos.salidas_almacen(origen,referencia,fecha,codigo,marca,modelo,medida,cantidad,placa)"
              + " VALUES ('EXCEL',?,?,?,?,?,?,?,?) ON CONFLICT (referencia) DO NOTHING",
          filas,
          500,
          (ps, f) -> {
            ps.setString(1, "MANT-F-68:" + f[0]);
            ps.setObject(2, LocalDate.parse(f[1]));
            ps.setString(3, f[2]);
            ps.setString(4, f[3]);
            ps.setString(5, f[4]);
            ps.setString(6, f[5]);
            ps.setInt(7, Integer.parseInt(f[6]));
            ps.setString(8, f[7]);
          });
    }
    jdbc.update(
        "INSERT INTO neumaticos.importaciones_modulo(clave) VALUES (?) ON CONFLICT DO NOTHING",
        clave);
  }

  private BufferedReader lector(String ruta) throws IOException {
    return new BufferedReader(
        new InputStreamReader(
            new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8));
  }
}
