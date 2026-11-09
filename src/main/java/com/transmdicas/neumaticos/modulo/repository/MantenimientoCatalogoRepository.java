package com.transmdicas.neumaticos.modulo.repository;

import com.transmdicas.neumaticos.modulo.model.CatalogoForm;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MantenimientoCatalogoRepository {
  private final JdbcTemplate jdbc;

  public MantenimientoCatalogoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<CatalogoForm> buscar(boolean flota, String clave) {
    String sql =
        flota
            ? "SELECT * FROM neumaticos.unidades WHERE placa=?"
            : "SELECT * FROM neumaticos.catalogo_neumaticos WHERE codigo=?";
    return jdbc
        .query(
            sql,
            (r, n) -> {
              var f = new CatalogoForm();
              f.setOriginal(clave);
              f.setClave(clave);
              f.setRevision(r.getLong("revision"));
              f.setMarca(r.getString("marca"));
              f.setRequiereRevision(r.getBoolean("requiere_revision"));
              if (flota) {
                f.setModelo(r.getString("modelo"));
                f.setOperacion(r.getString("operacion"));
                f.setRuta(r.getString("ruta"));
                f.setSemirremolque(r.getString("semirremolque"));
                f.setTipoUnidad(r.getString("tipo"));
              } else {
                f.setModelo(r.getString("diseno"));
                f.setMedida(r.getString("medida"));
                f.setReencauche1(r.getString("reencauche1"));
                f.setReencauche2(r.getString("reencauche2"));
              }
              return f;
            },
            clave)
        .stream()
        .findFirst();
  }

  public void guardar(boolean flota, CatalogoForm f) {
    if (f.getOriginal().isBlank()) {
      if (flota)
        jdbc.update(
            "INSERT INTO"
                + " neumaticos.unidades(placa,marca,modelo,operacion,ruta,semirremolque,tipo,requiere_revision)"
                + " VALUES (?,?,?,?,?,?,?,?)",
            f.getClave(),
            f.getMarca(),
            f.getModelo(),
            f.getOperacion(),
            f.getRuta(),
            f.getSemirremolque(),
            f.getTipoUnidad(),
            f.isRequiereRevision());
      else
        jdbc.update(
            "INSERT INTO"
                + " neumaticos.catalogo_neumaticos(codigo,marca,diseno,medida,reencauche1,reencauche2,requiere_revision)"
                + " VALUES (?,?,?,?,?,?,?)",
            f.getClave(),
            f.getMarca(),
            f.getModelo(),
            f.getMedida(),
            f.getReencauche1(),
            f.getReencauche2(),
            f.isRequiereRevision());
    } else {
      int n =
          flota
              ? jdbc.update(
                  "UPDATE neumaticos.unidades SET"
                      + " marca=?,modelo=?,operacion=?,ruta=?,semirremolque=?,tipo=?,requiere_revision=?,revision=revision+1"
                      + " WHERE placa=? AND revision=?",
                  f.getMarca(),
                  f.getModelo(),
                  f.getOperacion(),
                  f.getRuta(),
                  f.getSemirremolque(),
                  f.getTipoUnidad(),
                  f.isRequiereRevision(),
                  f.getOriginal(),
                  f.getRevision())
              : jdbc.update(
                  "UPDATE neumaticos.catalogo_neumaticos SET"
                      + " marca=?,diseno=?,medida=?,reencauche1=?,reencauche2=?,requiere_revision=?,revision=revision+1"
                      + " WHERE codigo=? AND revision=?",
                  f.getMarca(),
                  f.getModelo(),
                  f.getMedida(),
                  f.getReencauche1(),
                  f.getReencauche2(),
                  f.isRequiereRevision(),
                  f.getOriginal(),
                  f.getRevision());
      if (n != 1)
        throw new OptimisticLockingFailureException(
            "El registro cambió en otra pestaña. Vuelve a abrirlo antes de guardar.");
    }
  }
}
