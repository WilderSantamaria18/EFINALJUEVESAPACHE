package com.iphone.store.model;

public class Empleado {
    private int id;
    private String dni;
    private String nombreCompleto;
    private String cargo;
    private double salario;
    private String fechaIngreso;

    public Empleado() {}

    public Empleado(int id, String dni, String nombreCompleto, String cargo, double salario, String fechaIngreso) {
        this.id = id;
        this.dni = dni;
        this.nombreCompleto = nombreCompleto;
        this.cargo = cargo;
        this.salario = salario;
        this.fechaIngreso = fechaIngreso;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public double getSalario() {
        return salario;
    }

    public void setSalario(double salario) {
        this.salario = salario;
    }

    public String getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(String fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
}
