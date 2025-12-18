package com.iphone.store.model;

public class Empresa {
    private String ruc;
    private String razonSocial;
    private String estado;
    private String direccion;
    private String telefono;

    public Empresa() {}

    public Empresa(String ruc, String razonSocial, String estado, String direccion, String telefono) {
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.estado = estado;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
