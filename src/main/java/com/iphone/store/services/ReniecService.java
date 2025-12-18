package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.iphone.store.model.Persona;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class ReniecService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "RENIEC.REQUEST";
    private static final String RESPONSE_QUEUE = "RENIEC.RESPONSE";
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio RENIEC iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String dni = textMessage.getText();
                        System.out.println("RENIEC recibio solicitud DNI: " + dni);

                        String jsonResponse = procesarConsultaDNI(dni);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("RENIEC envio respuesta para DNI: " + dni);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarConsultaDNI(String dni) {
        JsonObject resultado = new JsonObject();

        if (!validarFormatoDNI(dni)) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "DNI invalido: debe tener 8 digitos numericos");
            return gson.toJson(resultado);
        }

        Persona persona = consultarDNI(dni);

        if (persona == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "DNI no encontrado en el sistema RENIEC");
            return gson.toJson(resultado);
        }

        int edad = calcularEdad(persona.getFechaNacimiento());
        boolean esMayorEdad = edad >= 18;
        String nombreCompleto = persona.getNombres() + " " +
                persona.getApellidoPaterno() + " " +
                persona.getApellidoMaterno();

        resultado.addProperty("exito", true);
        resultado.addProperty("dni", persona.getDni());
        resultado.addProperty("nombreCompleto", nombreCompleto);
        resultado.addProperty("fechaNacimiento", persona.getFechaNacimiento());
        resultado.addProperty("direccion", persona.getDireccion());
        resultado.addProperty("edad", edad);
        resultado.addProperty("esMayorEdad", esMayorEdad);
        resultado.addProperty("estadoCivil", determinarEstadoCivil(edad));

        return gson.toJson(resultado);
    }

    private boolean validarFormatoDNI(String dni) {
        if (dni == null || dni.length() != 8) {
            return false;
        }
        for (char c : dni.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private int calcularEdad(String fechaNacimiento) {
        try {
            LocalDate fechaNac = LocalDate.parse(fechaNacimiento);
            LocalDate hoy = LocalDate.now();
            return Period.between(fechaNac, hoy).getYears();
        } catch (Exception e) {
            return 0;
        }
    }

    private String determinarEstadoCivil(int edad) {
        if (edad < 18) {
            return "MENOR_DE_EDAD";
        } else if (edad < 30) {
            return "ADULTO_JOVEN";
        } else if (edad < 60) {
            return "ADULTO";
        } else {
            return "ADULTO_MAYOR";
        }
    }

    private Persona consultarDNI(String dni) {
        Persona persona = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM reniec WHERE dni = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                persona = new Persona();
                persona.setDni(rs.getString("dni"));
                persona.setNombres(rs.getString("nombres"));
                persona.setApellidoPaterno(rs.getString("apellido_paterno"));
                persona.setApellidoMaterno(rs.getString("apellido_materno"));
                persona.setFechaNacimiento(rs.getString("fecha_nacimiento"));
                persona.setDireccion(rs.getString("direccion"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return persona;
    }

    public static void main(String[] args) {
        new ReniecService().start();
    }
}
