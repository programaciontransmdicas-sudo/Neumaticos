package com.transmdicas.neumaticos.modulo.repository;

import com.transmdicas.neumaticos.modulo.model.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogoRepository {
  private final JdbcTemplate jdbc;

  public CatalogoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final org.springframework.jdbc.core.RowMapper<UnidadCatalogo> unidad =
      (r, n) ->
          new UnidadCatalogo(
              r.getString("placa"),
              r.getString("marca"),
              r.getString("operacion"),
              r.getString("ruta"),
              r.getString("semirremolque"),
              r.getString("modelo"),
              r.getString("tipo"),
              r.getBoolean("requiere_revision"));
  private final org.springframework.jdbc.core.RowMapper<NeumaticoCatalogo> neumatico =
      (r, n) ->
          new NeumaticoCatalogo(
              r.getString("codigo"),
              r.getString("marca"),
              r.getString("diseno"),
              r.getString("medida"),
              r.getString("reencauche1"),
              r.getString("reencauche2"),
              r.getBoolean("requiere_revision"));

  public Optional<UnidadCatalogo> unidad(String placa) {
    return jdbc.query("SELECT * FROM neumaticos.unidades WHERE placa=?", unidad, placa).stream()
        .findFirst();
  }

  public Optional<NeumaticoCatalogo> neumatico(String codigo) {
    return jdbc
        .query("SELECT * FROM neumaticos.catalogo_neumaticos WHERE codigo=?", neumatico, codigo)
        .stream()
        .findFirst();
  }

  public List<UnidadCatalogo> flota(String q, int pagina) {
    String s = "%" + literal(q) + "%";
    return jdbc.query(
        "SELECT * FROM neumaticos.unidades WHERE LOWER(placa || ' ' || marca || ' ' || operacion ||"
            + " ' ' || ruta) LIKE ? ESCAPE '!' ORDER BY placa LIMIT 25 OFFSET ?",
        unidad,
        s,
        pagina * 25);
  }

  public List<NeumaticoCatalogo> neumaticos(String q, int pagina) {
    String s = "%" + literal(q) + "%";
    return jdbc.query(
        "SELECT * FROM neumaticos.catalogo_neumaticos WHERE LOWER(codigo || ' ' || marca || ' ' ||"
            + " diseno || ' ' || medida) LIKE ? ESCAPE '!' ORDER BY codigo LIMIT 25 OFFSET ?",
        neumatico,
        s,
        pagina * 25);
  }

  public long contar(boolean flota, String q) {
    String tabla = flota ? "unidades" : "catalogo_neumaticos";
    String campos =
        flota
            ? "placa || ' ' || marca || ' ' || operacion || ' ' || ruta"
            : "codigo || ' ' || marca || ' ' || diseno || ' ' || medida";
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM neumaticos."
            + tabla
            + " WHERE LOWER("
            + campos
            + ") LIKE ? ESCAPE '!'",
        Long.class,
        "%" + literal(q) + "%");
  }

  public Map<String, NeumaticoCatalogo> porCodigos(Collection<String> codigos) {
    if (codigos.isEmpty()) return Map.of();
    var named = new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc);
    Map<String, NeumaticoCatalogo> mapa = new HashMap<>();
    for (var n :
        named.query(
            "SELECT * FROM neumaticos.catalogo_neumaticos WHERE codigo IN (:codigos)",
            Map.of("codigos", codigos),
            neumatico)) mapa.put(n.codigo(), n);
    return mapa;
  }

  public List<String> placas() {
    return jdbc.queryForList("SELECT placa FROM neumaticos.unidades ORDER BY placa", String.class);
  }

  public static String literal(String s) {
    return s == null
        ? ""
        : s.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
