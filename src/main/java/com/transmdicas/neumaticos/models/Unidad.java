package com.transmdicas.neumaticos.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unidades", schema="neumaticos")

public class Unidad{
   @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(nullable = false, unique = true, length = 10)
private String placa;

@Column(nullable = false, length = 100)
private String marca;

@Column(nullable = false, length = 150)
private String operacion;

@Column(nullable = false, length = 200)
private String ruta;
    protected Unidad() {
}
    
    public Unidad(
        String placa,
        String marca,
        String operacion,
        String ruta) {

    this.placa = placa;
    this.marca = marca;
    this.operacion = operacion;
    this.ruta = ruta;
}
    public String getPlaca(){
        return placa;
    }
    public String getMarca(){
        return marca;
    }
    public String getOperacion(){
        return operacion;
    }
    public String getRuta(){
        return ruta;
    }
}