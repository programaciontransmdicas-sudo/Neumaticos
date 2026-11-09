package com.transmdicas.neumaticos.modulo.repository;

import java.text.Normalizer;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DisposicionRepository {
  private final JdbcTemplate jdbc;

  public DisposicionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> listar() {
    return jdbc.queryForList(
        "SELECT nombre FROM neumaticos.disposiciones ORDER BY nombre", String.class);
  }

  @Transactional
  public void agregar(String nombre) {
    if (nombre == null || nombre.isBlank() || nombre.strip().length() > 250)
      throw new IllegalArgumentException("Escribe una disposición de entre 1 y 250 caracteres.");
    String valor = nombre.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    String clave = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM neumaticos.disposiciones WHERE clave=?", Long.class, clave)
        == 0)
      jdbc.update("INSERT INTO neumaticos.disposiciones(clave,nombre) VALUES (?,?)", clave, valor);
  }
}
