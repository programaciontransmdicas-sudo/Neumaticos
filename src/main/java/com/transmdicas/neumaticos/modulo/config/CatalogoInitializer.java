package com.transmdicas.neumaticos.modulo.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(10)
@ConditionalOnProperty(
    name = "modulo.catalogos.importar",
    havingValue = "true",
    matchIfMissing = true)
public class CatalogoInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public CatalogoInitializer(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) throws Exception {
    String clave = "excel-2026-prototipo-v1";
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM neumaticos.importaciones_modulo WHERE clave=?", Long.class, clave)
        > 0) return;
    importar(
        "catalogos/flota.tsv",
        "INSERT INTO neumaticos.unidades(placa,marca,operacion,ruta,semirremolque,modelo,tipo)"
            + " VALUES (?,?,?,?,?,?,?) ON CONFLICT (placa) DO NOTHING",
        7);
    importar(
        "catalogos/neumaticos.tsv",
        "INSERT INTO"
            + " neumaticos.catalogo_neumaticos(codigo,marca,diseno,medida,reencauche1,reencauche2)"
            + " VALUES (?,?,?,?,?,?) ON CONFLICT (codigo) DO NOTHING",
        6);
    try (var lector =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource("catalogos/revisar.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
      for (String codigo : lector.lines().toList())
        if (!codigo.isBlank())
          jdbc.update(
              "UPDATE neumaticos.catalogo_neumaticos SET requiere_revision=TRUE WHERE codigo=?",
              codigo);
    }
    try (var lector =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource("catalogos/placas-revisar.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
      for (String placa : lector.lines().toList())
        if (!placa.isBlank())
          jdbc.update("UPDATE neumaticos.unidades SET requiere_revision=TRUE WHERE placa=?", placa);
    }
    jdbc.update(
        "INSERT INTO neumaticos.importaciones_modulo(clave) VALUES (?) ON CONFLICT DO NOTHING",
        clave);
  }

  private void importar(String recurso, String sql, int columnas) throws IOException {
    try (var lector =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource(recurso).getInputStream(), StandardCharsets.UTF_8))) {
      List<String[]> datos = lector.lines().skip(1).map(l -> l.split("\t", -1)).toList();
      for (var fila : datos)
        if (fila.length != columnas) throw new IOException("Catálogo inválido: " + recurso);
      jdbc.batchUpdate(
          sql,
          datos,
          500,
          (ps, fila) -> {
            for (int i = 0; i < columnas; i++) ps.setString(i + 1, fila[i]);
          });
    }
  }
}
