package com.transmdicas.neumaticos.modulo.repository;

import com.transmdicas.neumaticos.modulo.model.*;
import java.sql.*;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentoRepository {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  public DocumentoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    this.named = new NamedParameterJdbcTemplate(jdbc);
  }

  public Optional<DocumentoForm> buscar(Long id) {
    var lista =
        jdbc.query("SELECT * FROM neumaticos.documentos WHERE id=?", (r, n) -> cabecera(r), id);
    if (lista.isEmpty()) return Optional.empty();
    var d = lista.get(0);
    d.getFilas().clear();
    d.getDespues().clear();
    jdbc.query(
        "SELECT * FROM neumaticos.documento_filas WHERE documento_id=? ORDER BY seccion,indice",
        (RowCallbackHandler)
            r -> {
              var fila = fila(r);
              if ("PRINCIPAL".equals(r.getString("seccion"))) d.getFilas().add(fila);
              else d.getDespues().add(fila);
            },
        id);
    return Optional.of(d);
  }

  public Optional<Long> porToken(String token) {
    return jdbc
        .queryForList("SELECT id FROM neumaticos.documentos WHERE token=?", Long.class, token)
        .stream()
        .findFirst();
  }

  public Long guardar(DocumentoForm d) {
    MapSqlParameterSource p = params(d);
    if (d.getId() == null) {
      var key = new GeneratedKeyHolder();
      named.update(
          """
INSERT INTO neumaticos.documentos (token,tipo,estado,placa,operador,kilometraje,fecha,ot,inicio,fin,marca_unidad,operacion,ruta,semirremolque,rotaciones,observaciones,responsable1,responsable2,responsable3,responsable4)
VALUES (:token,:tipo,:estado,:placa,:operador,:kilometraje,:fecha,:ot,:inicio,:fin,:marcaUnidad,:operacion,:ruta,:semirremolque,:rotaciones,:observaciones,:responsable1,:responsable2,:responsable3,:responsable4)
""",
          p,
          key,
          new String[] {"id"});
      d.setId(Objects.requireNonNull(key.getKey()).longValue());
      d.setVersion(0);
    } else {
      int n =
          named.update(
              """
UPDATE neumaticos.documentos SET estado=:estado,placa=:placa,operador=:operador,kilometraje=:kilometraje,fecha=:fecha,ot=:ot,inicio=:inicio,fin=:fin,
marca_unidad=:marcaUnidad,operacion=:operacion,ruta=:ruta,semirremolque=:semirremolque,rotaciones=:rotaciones,observaciones=:observaciones,
responsable1=:responsable1,responsable2=:responsable2,responsable3=:responsable3,responsable4=:responsable4,
version=version+1,actualizado_en=CURRENT_TIMESTAMP WHERE id=:id AND version=:version AND estado='BORRADOR'
""",
              p);
      if (n != 1)
        throw new OptimisticLockingFailureException(
            "Otro usuario modificó este documento. Abre nuevamente el registro antes de guardar.");
      d.setVersion(d.getVersion() + 1);
      jdbc.update("DELETE FROM neumaticos.documento_filas WHERE documento_id=?", d.getId());
    }
    filas(d.getId(), "PRINCIPAL", d.getFilas());
    filas(d.getId(), "ROTACION", d.getDespues());
    return d.getId();
  }

  private void filas(Long id, String seccion, List<FilaDocumento> filas) {
    for (int i = 0; i < filas.size(); i++) {
      var r = filas.get(i);
      jdbc.update(
          """
INSERT INTO neumaticos.documento_filas(documento_id,seccion,indice,posicion,codigo,marca,diseno,medida,rn,r1,r2,presion,aro,condicion,sin_tapa,disposicion,codigo_nuevo,marca_nueva,diseno_nuevo,medida_nueva,n1,n2,cantidad,conformidad)
VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
""",
          id,
          seccion,
          i,
          r.getPosicion(),
          r.getCodigo(),
          r.getMarca(),
          r.getDiseno(),
          r.getMedida(),
          r.getRn(),
          r.getR1(),
          r.getR2(),
          r.getPresion(),
          r.getAro(),
          r.getCondicion(),
          r.getSinTapa(),
          r.getDisposicion(),
          r.getCodigoNuevo(),
          r.getMarcaNueva(),
          r.getDisenoNuevo(),
          r.getMedidaNueva(),
          r.getN1(),
          r.getN2(),
          r.getCantidad(),
          r.getConformidad());
    }
  }

  private MapSqlParameterSource params(DocumentoForm d) {
    return new MapSqlParameterSource()
        .addValue("id", d.getId())
        .addValue("version", d.getVersion())
        .addValue("token", d.getToken())
        .addValue("tipo", d.getTipo().name())
        .addValue("estado", d.getEstado())
        .addValue("placa", d.getPlaca())
        .addValue("operador", d.getOperador())
        .addValue("kilometraje", d.getKilometraje())
        .addValue("fecha", d.getFecha(), Types.DATE)
        .addValue("ot", d.getOt())
        .addValue("inicio", d.getInicio(), Types.TIME)
        .addValue("fin", d.getFin(), Types.TIME)
        .addValue("marcaUnidad", d.getMarcaUnidad())
        .addValue("operacion", d.getOperacion())
        .addValue("ruta", d.getRuta())
        .addValue("semirremolque", d.getSemirremolque())
        .addValue("rotaciones", d.isRotaciones())
        .addValue("observaciones", d.getObservaciones())
        .addValue("responsable1", d.getResponsable1())
        .addValue("responsable2", d.getResponsable2())
        .addValue("responsable3", d.getResponsable3())
        .addValue("responsable4", d.getResponsable4());
  }

  private DocumentoForm cabecera(ResultSet r) throws SQLException {
    var d = new DocumentoForm();
    d.setId(r.getLong("id"));
    d.setVersion(r.getLong("version"));
    d.setToken(r.getString("token"));
    d.setTipo(TipoDocumento.valueOf(r.getString("tipo")));
    d.setEstado(r.getString("estado"));
    d.setPlaca(r.getString("placa"));
    d.setOperador(r.getString("operador"));
    d.setKilometraje(r.getObject("kilometraje", Long.class));
    d.setFecha(r.getObject("fecha", java.time.LocalDate.class));
    d.setOt(r.getString("ot"));
    d.setInicio(r.getObject("inicio", java.time.LocalTime.class));
    d.setFin(r.getObject("fin", java.time.LocalTime.class));
    d.setMarcaUnidad(r.getString("marca_unidad"));
    d.setOperacion(r.getString("operacion"));
    d.setRuta(r.getString("ruta"));
    d.setSemirremolque(r.getString("semirremolque"));
    d.setRotaciones(r.getBoolean("rotaciones"));
    d.setObservaciones(r.getString("observaciones"));
    d.setResponsable1(r.getString("responsable1"));
    d.setResponsable2(r.getString("responsable2"));
    d.setResponsable3(r.getString("responsable3"));
    d.setResponsable4(r.getString("responsable4"));
    return d;
  }

  private FilaDocumento fila(ResultSet r) throws SQLException {
    var f = new FilaDocumento();
    f.setPosicion(r.getObject("posicion", Integer.class));
    f.setCodigo(r.getString("codigo"));
    f.setMarca(r.getString("marca"));
    f.setDiseno(r.getString("diseno"));
    f.setMedida(r.getString("medida"));
    f.setRn(r.getString("rn"));
    f.setR1(r.getBigDecimal("r1"));
    f.setR2(r.getBigDecimal("r2"));
    f.setPresion(r.getBigDecimal("presion"));
    f.setAro(r.getString("aro"));
    f.setCondicion(r.getString("condicion"));
    f.setSinTapa(r.getString("sin_tapa"));
    f.setDisposicion(r.getString("disposicion"));
    f.setCodigoNuevo(r.getString("codigo_nuevo"));
    f.setMarcaNueva(r.getString("marca_nueva"));
    f.setDisenoNuevo(r.getString("diseno_nuevo"));
    f.setMedidaNueva(r.getString("medida_nueva"));
    f.setN1(r.getBigDecimal("n1"));
    f.setN2(r.getBigDecimal("n2"));
    f.setCantidad(r.getObject("cantidad", Integer.class));
    f.setConformidad(r.getString("conformidad"));
    return f;
  }

  private String busqueda(String q) {
    String valor = q == null ? "" : q.strip();
    if (valor.matches("(?i)DOC-[0-9]+")) valor = valor.substring(4).replaceFirst("^0+(?!$)", "");
    for (var tipo : TipoDocumento.values())
      if (tipo.getTitulo().equalsIgnoreCase(valor)) valor = tipo.name();
    return "%" + CatalogoRepository.literal(valor) + "%";
  }

  private String filtro() {
    return " FROM neumaticos.documentos WHERE LOWER(placa || ' ' || operador || ' ' || tipo || ' '"
        + " || estado || ' ' || CAST(id AS VARCHAR)) LIKE ? ESCAPE '!'";
  }

  public long contar(String q) {
    return jdbc.queryForObject("SELECT COUNT(*)" + filtro(), Long.class, busqueda(q));
  }

  public List<ResumenDocumento> listar(String q, int pagina) {
    return jdbc.query(
        "SELECT id,tipo,estado,placa,operador,fecha,version"
            + filtro()
            + " ORDER BY id DESC LIMIT 25 OFFSET ?",
        (r, n) ->
            new ResumenDocumento(
                r.getLong("id"),
                TipoDocumento.valueOf(r.getString("tipo")),
                r.getString("estado"),
                r.getString("placa"),
                r.getString("operador"),
                r.getObject("fecha", java.time.LocalDate.class),
                r.getLong("version")),
        busqueda(q),
        pagina * 25);
  }

  public void eliminar(Long id, long version) {
    if (jdbc.update("DELETE FROM neumaticos.documentos WHERE id=? AND version=?", id, version) != 1)
      throw new OptimisticLockingFailureException("El registro cambió. Vuelve a abrirlo.");
  }

  public List<Map<String, Object>> resumen() {
    return jdbc.queryForList(
        "SELECT tipo,estado,COUNT(*) AS cantidad FROM neumaticos.documentos GROUP BY tipo,estado"
            + " ORDER BY tipo,estado");
  }

  public List<Long> todosIds() {
    return jdbc.queryForList("SELECT id FROM neumaticos.documentos ORDER BY id", Long.class);
  }
}
