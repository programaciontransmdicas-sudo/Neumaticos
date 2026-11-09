package com.transmdicas.neumaticos.modulo.model;

import java.time.*;
import java.util.*;

public class SalidaForm {
  private String token = UUID.randomUUID().toString();
  private String fecha = "";
  private String codigo = "";
  private Integer cantidad = 1;
  private String placa = "";
  private String observaciones = "";

  public String getToken() {
    return token;
  }

  public void setToken(String value) {
    this.token = value;
  }

  public String getFecha() {
    return fecha;
  }

  public void setFecha(String value) {
    this.fecha = value;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String value) {
    this.codigo = value;
  }

  public Integer getCantidad() {
    return cantidad;
  }

  public void setCantidad(Integer value) {
    this.cantidad = value;
  }

  public String getPlaca() {
    return placa;
  }

  public void setPlaca(String value) {
    this.placa = value;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public void setObservaciones(String value) {
    this.observaciones = value;
  }
}
