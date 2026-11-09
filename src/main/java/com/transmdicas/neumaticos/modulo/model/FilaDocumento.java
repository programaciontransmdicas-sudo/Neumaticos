package com.transmdicas.neumaticos.modulo.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class FilaDocumento {
  private Integer posicion;
  private String codigo = "";
  private String marca = "";
  private String diseno = "";
  private String medida = "";
  private String rn = "";
  private BigDecimal r1;
  private BigDecimal r2;
  private BigDecimal presion;
  private String aro = "";
  private String condicion = "";
  private String sinTapa = "";
  private String disposicion = "";
  private String codigoNuevo = "";
  private String marcaNueva = "";
  private String disenoNuevo = "";
  private String medidaNueva = "";
  private BigDecimal n1;
  private BigDecimal n2;
  private Integer cantidad;
  private String conformidad = "";

  public Integer getPosicion() {
    return posicion;
  }

  public void setPosicion(Integer value) {
    this.posicion = value;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String value) {
    this.codigo = value;
  }

  public String getMarca() {
    return marca;
  }

  public void setMarca(String value) {
    this.marca = value;
  }

  public String getDiseno() {
    return diseno;
  }

  public void setDiseno(String value) {
    this.diseno = value;
  }

  public String getMedida() {
    return medida;
  }

  public void setMedida(String value) {
    this.medida = value;
  }

  public String getRn() {
    return rn;
  }

  public void setRn(String value) {
    this.rn = value;
  }

  public BigDecimal getR1() {
    return r1;
  }

  public void setR1(BigDecimal value) {
    this.r1 = value;
  }

  public BigDecimal getR2() {
    return r2;
  }

  public void setR2(BigDecimal value) {
    this.r2 = value;
  }

  public BigDecimal getPresion() {
    return presion;
  }

  public void setPresion(BigDecimal value) {
    this.presion = value;
  }

  public String getAro() {
    return aro;
  }

  public void setAro(String value) {
    this.aro = value;
  }

  public String getCondicion() {
    return condicion;
  }

  public void setCondicion(String value) {
    this.condicion = value;
  }

  public String getSinTapa() {
    return sinTapa;
  }

  public void setSinTapa(String value) {
    this.sinTapa = value;
  }

  public String getDisposicion() {
    return disposicion;
  }

  public void setDisposicion(String value) {
    this.disposicion = value;
  }

  public String getCodigoNuevo() {
    return codigoNuevo;
  }

  public void setCodigoNuevo(String value) {
    this.codigoNuevo = value;
  }

  public String getMarcaNueva() {
    return marcaNueva;
  }

  public void setMarcaNueva(String value) {
    this.marcaNueva = value;
  }

  public String getDisenoNuevo() {
    return disenoNuevo;
  }

  public void setDisenoNuevo(String value) {
    this.disenoNuevo = value;
  }

  public String getMedidaNueva() {
    return medidaNueva;
  }

  public void setMedidaNueva(String value) {
    this.medidaNueva = value;
  }

  public BigDecimal getN1() {
    return n1;
  }

  public void setN1(BigDecimal value) {
    this.n1 = value;
  }

  public BigDecimal getN2() {
    return n2;
  }

  public void setN2(BigDecimal value) {
    this.n2 = value;
  }

  public Integer getCantidad() {
    return cantidad;
  }

  public void setCantidad(Integer value) {
    this.cantidad = value;
  }

  public String getConformidad() {
    return conformidad;
  }

  public void setConformidad(String value) {
    this.conformidad = value;
  }

  public BigDecimal getPromedio() {
    return promedio(r1, r2);
  }

  public BigDecimal getPromedioNuevo() {
    return promedio(n1, n2);
  }

  private BigDecimal promedio(BigDecimal a, BigDecimal b) {
    return a == null || b == null
        ? null
        : a.add(b).divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
  }

  public boolean isUsada() {
    return !codigo.isBlank()
        || !codigoNuevo.isBlank()
        || !rn.isBlank()
        || r1 != null
        || r2 != null
        || presion != null
        || !aro.isBlank()
        || !condicion.isBlank()
        || !sinTapa.isBlank()
        || !disposicion.isBlank()
        || n1 != null
        || n2 != null
        || cantidad != null
        || !conformidad.isBlank();
  }

  public EstadoRemanente getAlerta() {
    return EstadoRemanente.de(getPromedio());
  }

  public EstadoRemanente getAlertaNuevo() {
    return EstadoRemanente.de(getPromedioNuevo());
  }
}
