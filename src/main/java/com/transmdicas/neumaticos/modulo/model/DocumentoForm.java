package com.transmdicas.neumaticos.modulo.model;

import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;

public class DocumentoForm {
  private Long id;
  private long version = 0;
  private String token = UUID.randomUUID().toString();
  private TipoDocumento tipo = TipoDocumento.INSPECCION;
  private String estado = "BORRADOR";
  private String placa = "";
  private String operador = "";
  private Long kilometraje;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate fecha;

  private String ot = "";

  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  private LocalTime inicio;

  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  private LocalTime fin;

  private String marcaUnidad = "";
  private String operacion = "";
  private String ruta = "";
  private String semirremolque = "";
  private boolean rotaciones = false;
  private String observaciones = "";
  private String responsable1 = "";
  private String responsable2 = "";
  private String responsable3 = "";
  private String responsable4 = "";
  private List<FilaDocumento> filas = new ArrayList<>();
  private List<FilaDocumento> despues = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    this.id = value;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long value) {
    this.version = value;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String value) {
    this.token = value;
  }

  public TipoDocumento getTipo() {
    return tipo;
  }

  public void setTipo(TipoDocumento value) {
    this.tipo = value;
  }

  public String getEstado() {
    return estado;
  }

  public void setEstado(String value) {
    this.estado = value;
  }

  public String getPlaca() {
    return placa;
  }

  public void setPlaca(String value) {
    this.placa = value;
  }

  public String getOperador() {
    return operador;
  }

  public void setOperador(String value) {
    this.operador = value;
  }

  public Long getKilometraje() {
    return kilometraje;
  }

  public void setKilometraje(Long value) {
    this.kilometraje = value;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate value) {
    this.fecha = value;
  }

  public String getOt() {
    return ot;
  }

  public void setOt(String value) {
    this.ot = value;
  }

  public LocalTime getInicio() {
    return inicio;
  }

  public void setInicio(LocalTime value) {
    this.inicio = value;
  }

  public LocalTime getFin() {
    return fin;
  }

  public void setFin(LocalTime value) {
    this.fin = value;
  }

  public String getMarcaUnidad() {
    return marcaUnidad;
  }

  public void setMarcaUnidad(String value) {
    this.marcaUnidad = value;
  }

  public String getOperacion() {
    return operacion;
  }

  public void setOperacion(String value) {
    this.operacion = value;
  }

  public String getRuta() {
    return ruta;
  }

  public void setRuta(String value) {
    this.ruta = value;
  }

  public String getSemirremolque() {
    return semirremolque;
  }

  public void setSemirremolque(String value) {
    this.semirremolque = value;
  }

  public boolean isRotaciones() {
    return rotaciones;
  }

  public void setRotaciones(boolean value) {
    this.rotaciones = value;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public void setObservaciones(String value) {
    this.observaciones = value;
  }

  public String getResponsable1() {
    return responsable1;
  }

  public void setResponsable1(String value) {
    this.responsable1 = value;
  }

  public String getResponsable2() {
    return responsable2;
  }

  public void setResponsable2(String value) {
    this.responsable2 = value;
  }

  public String getResponsable3() {
    return responsable3;
  }

  public void setResponsable3(String value) {
    this.responsable3 = value;
  }

  public String getResponsable4() {
    return responsable4;
  }

  public void setResponsable4(String value) {
    this.responsable4 = value;
  }

  public List<FilaDocumento> getFilas() {
    return filas;
  }

  public void setFilas(List<FilaDocumento> value) {
    this.filas = value;
  }

  public List<FilaDocumento> getDespues() {
    return despues;
  }

  public void setDespues(List<FilaDocumento> value) {
    this.despues = value;
  }

  public static DocumentoForm nuevo(TipoDocumento tipo) {
    DocumentoForm f = new DocumentoForm();
    f.tipo = tipo;
    for (int i = 1; i <= (tipo == TipoDocumento.INSPECCION ? 10 : 2); i++) {
      FilaDocumento fila = new FilaDocumento();
      fila.setPosicion(i);
      f.filas.add(fila);
    }
    for (int i = 1; i <= 10; i++) {
      FilaDocumento fila = new FilaDocumento();
      fila.setPosicion(i);
      f.despues.add(fila);
    }
    return f;
  }

  public boolean isGenerado() {
    return "GENERADO".equals(estado);
  }

  public String getNumero() {
    return id == null ? "Sin guardar" : "DOC-" + String.format("%06d", id);
  }

  public long getFilasUsadas() {
    return filas.stream().filter(FilaDocumento::isUsada).count();
  }

  public List<String> getResponsables() {
    return List.of(responsable1, responsable2, responsable3, responsable4);
  }
}
