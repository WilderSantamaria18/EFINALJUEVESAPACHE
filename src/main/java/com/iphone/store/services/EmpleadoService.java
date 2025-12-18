package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.iphone.store.model.Empleado;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;

public class EmpleadoService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "EMPLEADO.REQUEST";
    private static final String RESPONSE_QUEUE = "EMPLEADO.RESPONSE";
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio EMPLEADO iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String dni = textMessage.getText();
                        System.out.println("EMPLEADO recibio solicitud DNI: " + dni);

                        String jsonResponse = procesarConsultaEmpleado(dni);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("EMPLEADO envio respuesta para DNI: " + dni);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarConsultaEmpleado(String dni) {
        JsonObject resultado = new JsonObject();

        Empleado empleado = consultarEmpleado(dni);

        if (empleado == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Empleado con DNI " + dni + " no encontrado");
            return gson.toJson(resultado);
        }

        int antiguedad = calcularAntiguedad(empleado.getFechaIngreso());
        double porcentajeBonificacion = calcularPorcentajeBonificacion(empleado.getCargo(), antiguedad);
        double bonificacion = empleado.getSalario() * (porcentajeBonificacion / 100);
        double salarioTotal = empleado.getSalario() + bonificacion;
        String nivelEmpleado = determinarNivelEmpleado(antiguedad);
        String categoriaEmpleado = determinarCategoria(empleado.getSalario());
        boolean elegibleAscenso = antiguedad >= 2 && !"GERENTE".equalsIgnoreCase(empleado.getCargo());
        double aporteAFP = salarioTotal * 0.13;
        double aporteSalud = salarioTotal * 0.09;
        double salarioNeto = salarioTotal - aporteAFP - aporteSalud;

        resultado.addProperty("exito", true);
        resultado.addProperty("id", empleado.getId());
        resultado.addProperty("dni", empleado.getDni());
        resultado.addProperty("nombreCompleto", empleado.getNombreCompleto());
        resultado.addProperty("cargo", empleado.getCargo());
        resultado.addProperty("fechaIngreso", empleado.getFechaIngreso());
        resultado.addProperty("antiguedadAnios", antiguedad);
        resultado.addProperty("salarioBase", empleado.getSalario());
        resultado.addProperty("porcentajeBonificacion", porcentajeBonificacion);
        resultado.addProperty("bonificacion", Math.round(bonificacion * 100.0) / 100.0);
        resultado.addProperty("salarioTotal", Math.round(salarioTotal * 100.0) / 100.0);
        resultado.addProperty("aporteAFP", Math.round(aporteAFP * 100.0) / 100.0);
        resultado.addProperty("aporteSalud", Math.round(aporteSalud * 100.0) / 100.0);
        resultado.addProperty("salarioNeto", Math.round(salarioNeto * 100.0) / 100.0);
        resultado.addProperty("nivelEmpleado", nivelEmpleado);
        resultado.addProperty("categoriaEmpleado", categoriaEmpleado);
        resultado.addProperty("elegibleAscenso", elegibleAscenso);

        return gson.toJson(resultado);
    }

    private int calcularAntiguedad(String fechaIngreso) {
        try {
            LocalDate fecha = LocalDate.parse(fechaIngreso);
            LocalDate hoy = LocalDate.now();
            return Period.between(fecha, hoy).getYears();
        } catch (Exception e) {
            return 0;
        }
    }

    private double calcularPorcentajeBonificacion(String cargo, int antiguedad) {
        double bonificacionBase = 0;

        if ("GERENTE".equalsIgnoreCase(cargo)) {
            bonificacionBase = 20;
        } else if ("VENDEDOR".equalsIgnoreCase(cargo)) {
            bonificacionBase = 10;
        } else {
            bonificacionBase = 5;
        }

        if (antiguedad >= 5) {
            bonificacionBase += 10;
        } else if (antiguedad >= 3) {
            bonificacionBase += 5;
        } else if (antiguedad >= 1) {
            bonificacionBase += 2;
        }

        return bonificacionBase;
    }

    private String determinarNivelEmpleado(int antiguedad) {
        if (antiguedad >= 10) {
            return "SENIOR";
        } else if (antiguedad >= 5) {
            return "PLENO";
        } else if (antiguedad >= 2) {
            return "JUNIOR";
        } else {
            return "PRACTICANTE";
        }
    }

    private String determinarCategoria(double salario) {
        if (salario >= 5000) {
            return "A";
        } else if (salario >= 3000) {
            return "B";
        } else if (salario >= 1500) {
            return "C";
        } else {
            return "D";
        }
    }

    private Empleado consultarEmpleado(String dni) {
        Empleado empleado = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM empleados WHERE dni = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                empleado = new Empleado();
                empleado.setId(rs.getInt("id"));
                empleado.setDni(rs.getString("dni"));
                empleado.setNombreCompleto(rs.getString("nombre_completo"));
                empleado.setCargo(rs.getString("cargo"));
                empleado.setSalario(rs.getDouble("salario"));
                empleado.setFechaIngreso(rs.getString("fecha_ingreso"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empleado;
    }

    public static void main(String[] args) {
        new EmpleadoService().start();
    }
}
