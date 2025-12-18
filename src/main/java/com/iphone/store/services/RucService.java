package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.iphone.store.model.Empresa;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RucService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "RUC.REQUEST";
    private static final String RESPONSE_QUEUE = "RUC.RESPONSE";
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio RUC iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String ruc = textMessage.getText();
                        System.out.println("RUC recibio solicitud RUC: " + ruc);

                        String jsonResponse = procesarConsultaRUC(ruc);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("RUC envio respuesta para RUC: " + ruc);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarConsultaRUC(String ruc) {
        JsonObject resultado = new JsonObject();

        if (!validarFormatoRUC(ruc)) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "RUC invalido: debe tener 11 digitos numericos");
            return gson.toJson(resultado);
        }

        String tipoContribuyente = determinarTipoContribuyente(ruc);

        Empresa empresa = consultarRUC(ruc);

        if (empresa == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "RUC no encontrado en el sistema SUNAT");
            return gson.toJson(resultado);
        }

        boolean esActivo = "ACTIVO".equalsIgnoreCase(empresa.getEstado());
        boolean puedeFacturar = esActivo && empresa.getDireccion() != null && !empresa.getDireccion().isEmpty();
        String nivelRiesgo = calcularNivelRiesgo(empresa);

        resultado.addProperty("exito", true);
        resultado.addProperty("ruc", empresa.getRuc());
        resultado.addProperty("razonSocial", empresa.getRazonSocial());
        resultado.addProperty("estado", empresa.getEstado());
        resultado.addProperty("direccion", empresa.getDireccion());
        resultado.addProperty("telefono", empresa.getTelefono());
        resultado.addProperty("tipoContribuyente", tipoContribuyente);
        resultado.addProperty("esActivo", esActivo);
        resultado.addProperty("puedeFacturar", puedeFacturar);
        resultado.addProperty("nivelRiesgo", nivelRiesgo);

        return gson.toJson(resultado);
    }

    private boolean validarFormatoRUC(String ruc) {
        if (ruc == null || ruc.length() != 11) {
            return false;
        }
        for (char c : ruc.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        String prefijo = ruc.substring(0, 2);
        return prefijo.equals("10") || prefijo.equals("20") || prefijo.equals("15") || prefijo.equals("17");
    }

    private String determinarTipoContribuyente(String ruc) {
        String prefijo = ruc.substring(0, 2);
        switch (prefijo) {
            case "10":
                return "PERSONA_NATURAL";
            case "20":
                return "PERSONA_JURIDICA";
            case "15":
                return "GOBIERNO_CENTRAL";
            case "17":
                return "GOBIERNO_REGIONAL";
            default:
                return "DESCONOCIDO";
        }
    }

    private String calcularNivelRiesgo(Empresa empresa) {
        if (!"ACTIVO".equalsIgnoreCase(empresa.getEstado())) {
            return "ALTO";
        }
        if (empresa.getTelefono() == null || empresa.getTelefono().isEmpty()) {
            return "MEDIO";
        }
        if (empresa.getDireccion() == null || empresa.getDireccion().isEmpty()) {
            return "MEDIO";
        }
        return "BAJO";
    }

    private Empresa consultarRUC(String ruc) {
        Empresa empresa = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM ruc WHERE ruc = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, ruc);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                empresa = new Empresa();
                empresa.setRuc(rs.getString("ruc"));
                empresa.setRazonSocial(rs.getString("razon_social"));
                empresa.setEstado(rs.getString("estado"));
                empresa.setDireccion(rs.getString("direccion"));
                empresa.setTelefono(rs.getString("telefono"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empresa;
    }

    public static void main(String[] args) {
        new RucService().start();
    }
}
