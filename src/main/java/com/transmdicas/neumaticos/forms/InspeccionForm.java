package com.transmdicas.neumaticos.forms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InspeccionForm {

    private String placa;
    private String operador;
    private Long kilometraje;
    private LocalDate fecha = LocalDate.now();
    private String observaciones;

    private List<Fila> filas = new ArrayList<>();

    public InspeccionForm() {
        for (int i = 0; i < 10; i++) {
            filas.add(new Fila());
        }
    }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }

    public Long getKilometraje() { return kilometraje; }
    public void setKilometraje(Long kilometraje) {
        this.kilometraje = kilometraje;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<Fila> getFilas() { return filas; }
    public void setFilas(List<Fila> filas) { this.filas = filas; }

    public static class Fila {

        private String codigo;
        private String rn;
        private BigDecimal r1;
        private BigDecimal r2;
        private BigDecimal presion;
        private String aro;
        private String estado;
        private boolean sinTapa;

        public BigDecimal getPromedio() {
            if (r1 == null || r2 == null) {
                return null;
            }

            return r1.add(r2)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }

        public String getRn() { return rn; }
        public void setRn(String rn) { this.rn = rn; }

        public BigDecimal getR1() { return r1; }
        public void setR1(BigDecimal r1) { this.r1 = r1; }

        public BigDecimal getR2() { return r2; }
        public void setR2(BigDecimal r2) { this.r2 = r2; }

        public BigDecimal getPresion() { return presion; }
        public void setPresion(BigDecimal presion) {
            this.presion = presion;
        }

        public String getAro() { return aro; }
        public void setAro(String aro) { this.aro = aro; }

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }

        public boolean isSinTapa() { return sinTapa; }
        public void setSinTapa(boolean sinTapa) {
            this.sinTapa = sinTapa;
        }
    }
}