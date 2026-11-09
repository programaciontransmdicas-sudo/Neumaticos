package com.transmdicas.neumaticos.modulo.model;

import java.time.*;
import java.util.*;

public class CatalogoForm {
  private String original = "";
  private String clave = "";
  private long revision = 0;
  private String marca = "";
  private String modelo = "";
  private String medida = "";
  private String operacion = "";
  private String ruta = "";
  private String semirremolque = "";
  private String tipoUnidad = "";
  private String reencauche1 = "";
  private String reencauche2 = "";
  private boolean requiereRevision = false;

  public String getOriginal() {
    return original;
  }

  public void setOriginal(String value) {
    this.original = value;
  }

  public String getClave() {
    return clave;
  }

  public void setClave(String value) {
    this.clave = value;
  }

  public long getRevision() {
    return revision;
  }

  public void setRevision(long value) {
    this.revision = value;
  }

  public String getMarca() {
    return marca;
  }

  public void setMarca(String value) {
    this.marca = value;
  }

  public String getModelo() {
    return modelo;
  }

  public void setModelo(String value) {
    this.modelo = value;
  }

  public String getMedida() {
    return medida;
  }

  public void setMedida(String value) {
    this.medida = value;
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

  public String getTipoUnidad() {
    return tipoUnidad;
  }

  public void setTipoUnidad(String value) {
    this.tipoUnidad = value;
  }

  public String getReencauche1() {
    return reencauche1;
  }

  public void setReencauche1(String value) {
    this.reencauche1 = value;
  }

  public String getReencauche2() {
    return reencauche2;
  }

  public void setReencauche2(String value) {
    this.reencauche2 = value;
  }

  public boolean isRequiereRevision() {
    return requiereRevision;
  }

  public void setRequiereRevision(boolean value) {
    this.requiereRevision = value;
  }
}
