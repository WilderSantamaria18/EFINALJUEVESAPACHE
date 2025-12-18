package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.iphone.store.model.Producto;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProductoService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "PRODUCTO.REQUEST";
    private static final String RESPONSE_QUEUE = "PRODUCTO.RESPONSE";
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio PRODUCTO iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String solicitud = textMessage.getText();
                        System.out.println("PRODUCTO recibio solicitud: " + solicitud);

                        String jsonResponse = procesarSolicitudProducto(solicitud);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("PRODUCTO envio respuesta");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarSolicitudProducto(String solicitud) {
        JsonObject resultado = new JsonObject();

        String[] partes = solicitud.split(",");
        String codigo = partes[0];
        int cantidadSolicitada = partes.length > 1 ? Integer.parseInt(partes[1]) : 1;

        Producto producto = consultarProducto(codigo);

        if (producto == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Producto no encontrado: " + codigo);
            return gson.toJson(resultado);
        }

        boolean disponible = producto.getStock() >= cantidadSolicitada;
        double porcentajeDescuento = calcularDescuentoPorCantidad(cantidadSolicitada);
        double precioUnitarioConDescuento = producto.getPrecio() * (1 - porcentajeDescuento / 100);
        double subtotal = precioUnitarioConDescuento * cantidadSolicitada;
        double igv = subtotal * 0.18;
        double total = subtotal + igv;
        String estadoStock = determinarEstadoStock(producto.getStock());

        resultado.addProperty("exito", true);
        resultado.addProperty("codigo", producto.getCodigo());
        resultado.addProperty("nombre", producto.getNombre());
        resultado.addProperty("precioOriginal", producto.getPrecio());
        resultado.addProperty("stock", producto.getStock());
        resultado.addProperty("cantidadSolicitada", cantidadSolicitada);
        resultado.addProperty("disponible", disponible);
        resultado.addProperty("porcentajeDescuento", porcentajeDescuento);
        resultado.addProperty("precioConDescuento", Math.round(precioUnitarioConDescuento * 100.0) / 100.0);
        resultado.addProperty("subtotal", Math.round(subtotal * 100.0) / 100.0);
        resultado.addProperty("igv", Math.round(igv * 100.0) / 100.0);
        resultado.addProperty("total", Math.round(total * 100.0) / 100.0);
        resultado.addProperty("estadoStock", estadoStock);

        if (!disponible) {
            resultado.addProperty("mensaje", "Stock insuficiente. Disponible: " + producto.getStock());
        }

        return gson.toJson(resultado);
    }

    private double calcularDescuentoPorCantidad(int cantidad) {
        if (cantidad >= 20) {
            return 15.0;
        } else if (cantidad >= 10) {
            return 10.0;
        } else if (cantidad >= 5) {
            return 5.0;
        }
        return 0.0;
    }

    private String determinarEstadoStock(int stock) {
        if (stock <= 0) {
            return "AGOTADO";
        } else if (stock <= 10) {
            return "BAJO";
        } else if (stock <= 30) {
            return "NORMAL";
        } else {
            return "ALTO";
        }
    }

    private Producto consultarProducto(String codigo) {
        Producto producto = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM productos WHERE codigo = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                producto = new Producto();
                producto.setCodigo(rs.getString("codigo"));
                producto.setNombre(rs.getString("nombre"));
                producto.setPrecio(rs.getDouble("precio"));
                producto.setStock(rs.getInt("stock"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return producto;
    }

    public static void main(String[] args) {
        new ProductoService().start();
    }
}
