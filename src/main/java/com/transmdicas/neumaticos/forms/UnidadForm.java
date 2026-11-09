package com.transmdicas.neumaticos.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UnidadForm {

    @NotBlank(message = "Ingresa la placa.")
    @Size(max = 10, message = "La placa admite hasta 10 caracteres.")
    private String placa;

    @NotBlank(message = "Ingresa la marca.")
    @Size(max = 100, message = "La marca admite hasta 100 caracteres.")
    private String marca;

    @NotBlank(message = "Ingresa la operación.")
    @Size(max = 150, message = "La operación admite hasta 150 caracteres.")
    private String operacion;

    @NotBlank(message = "Ingresa la ruta.")
    @Size(max = 200, message = "La ruta admite hasta 200 caracteres.")
    private String ruta;

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }
}