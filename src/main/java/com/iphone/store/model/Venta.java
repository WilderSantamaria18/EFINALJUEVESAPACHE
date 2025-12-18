package com.iphone.store.model;

public class Venta {
    private int id;
    private String fecha;
    private String dniCliente;
    private double total;
    private String estado;

    public Venta() {}

    public Venta(int id, String fecha, String dniCliente, double total, String estado) {
        this.id = id;
        this.fecha = fecha;
        this.dniCliente = dniCliente;
        this.total = total;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getDniCliente() {
        return dniCliente;
    }

    public void setDniCliente(String dniCliente) {
        this.dniCliente = dniCliente;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
