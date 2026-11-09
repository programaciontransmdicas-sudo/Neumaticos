package com.transmdicas.neumaticos.modulo.repository;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OperadorRepository {
  private final JdbcTemplate jdbc;

  public OperadorRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> buscar(String texto) {
    return jdbc.queryForList(
        "SELECT nombre FROM neumaticos.operadores WHERE busqueda LIKE ? ESCAPE '!'"
            + " ORDER BY nombre LIMIT 25",
        String.class,
        "%" + CatalogoRepository.literal(normalizar(texto)) + "%");
  }

  public static String normalizar(String nombre) {
    return Normalizer.normalize(nombre.strip(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }
}
