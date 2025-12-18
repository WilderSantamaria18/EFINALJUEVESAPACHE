package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.iphone.store.model.Inventario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class InventarioService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "INVENTARIO.REQUEST";
    private static final String RESPONSE_QUEUE = "INVENTARIO.RESPONSE";
    private static final int STOCK_MINIMO = 10;
    private static final int DIAS_ALERTA_ACTUALIZACION = 30;
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio INVENTARIO iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String codigoProducto = textMessage.getText();
                        System.out.println("INVENTARIO recibio solicitud codigo: " + codigoProducto);

                        String jsonResponse = procesarConsultaInventario(codigoProducto);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("INVENTARIO envio respuesta para codigo: " + codigoProducto);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarConsultaInventario(String codigoProducto) {
        JsonObject resultado = new JsonObject();

        Inventario inventario = consultarInventario(codigoProducto);

        if (inventario == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Producto no encontrado en inventario: " + codigoProducto);
            return gson.toJson(resultado);
        }

        boolean requiereReposicion = inventario.getCantidad() <= STOCK_MINIMO;
        int cantidadReposicionSugerida = calcularCantidadReposicion(inventario.getCantidad());
        long diasSinActualizar = calcularDiasSinActualizar(inventario.getFechaActualizacion());
        boolean requiereRevision = diasSinActualizar > DIAS_ALERTA_ACTUALIZACION;
        String prioridadReposicion = determinarPrioridadReposicion(inventario.getCantidad());
        String estadoInventario = determinarEstadoInventario(inventario.getCantidad());
        double valorInventario = calcularValorInventario(codigoProducto, inventario.getCantidad());

        resultado.addProperty("exito", true);
        resultado.addProperty("codigoProducto", inventario.getCodigoProducto());
        resultado.addProperty("cantidad", inventario.getCantidad());
        resultado.addProperty("ubicacion", inventario.getUbicacion());
        resultado.addProperty("fechaActualizacion", inventario.getFechaActualizacion());
        resultado.addProperty("diasSinActualizar", diasSinActualizar);
        resultado.addProperty("requiereReposicion", requiereReposicion);
        resultado.addProperty("cantidadReposicionSugerida", cantidadReposicionSugerida);
        resultado.addProperty("prioridadReposicion", prioridadReposicion);
        resultado.addProperty("estadoInventario", estadoInventario);
        resultado.addProperty("valorInventario", Math.round(valorInventario * 100.0) / 100.0);
        resultado.addProperty("requiereRevision", requiereRevision);

        if (requiereReposicion) {
            resultado.addProperty("alerta", "Stock bajo. Reponer " + cantidadReposicionSugerida + " unidades");
        }
        if (requiereRevision) {
            resultado.addProperty("alertaActualizacion",
                    "Inventario sin actualizar por " + diasSinActualizar + " dias");
        }

        return gson.toJson(resultado);
    }

    private int calcularCantidadReposicion(int cantidadActual) {
        int objetivo = 50;
        return Math.max(0, objetivo - cantidadActual);
    }

    private long calcularDiasSinActualizar(String fechaActualizacion) {
        try {
            LocalDate fecha = LocalDate.parse(fechaActualizacion);
            LocalDate hoy = LocalDate.now();
            return ChronoUnit.DAYS.between(fecha, hoy);
        } catch (Exception e) {
            return 0;
        }
    }

    private String determinarPrioridadReposicion(int cantidad) {
        if (cantidad <= 0) {
            return "URGENTE";
        } else if (cantidad <= 5) {
            return "ALTA";
        } else if (cantidad <= STOCK_MINIMO) {
            return "NORMAL";
        } else {
            return "BAJA";
        }
    }

    private String determinarEstadoInventario(int cantidad) {
        if (cantidad <= 0) {
            return "AGOTADO";
        } else if (cantidad <= 5) {
            return "CRITICO";
        } else if (cantidad <= STOCK_MINIMO) {
            return "BAJO";
        } else if (cantidad <= 30) {
            return "NORMAL";
        } else {
            return "OPTIMO";
        }
    }

    private double calcularValorInventario(String codigoProducto, int cantidad) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT precio FROM productos WHERE codigo = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, codigoProducto);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("precio") * cantidad;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Inventario consultarInventario(String codigoProducto) {
        Inventario inventario = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM inventario WHERE codigo_producto = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, codigoProducto);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                inventario = new Inventario();
                inventario.setId(rs.getInt("id"));
                inventario.setCodigoProducto(rs.getString("codigo_producto"));
                inventario.setCantidad(rs.getInt("cantidad"));
                inventario.setUbicacion(rs.getString("ubicacion"));
                inventario.setFechaActualizacion(rs.getString("fecha_actualizacion"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inventario;
    }

    public static void main(String[] args) {
        new InventarioService().start();
    }
}
