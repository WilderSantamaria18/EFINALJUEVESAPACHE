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
import java.util.Scanner;

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
        ProductoService service = new ProductoService();
        boolean modoInteractivo = args.length > 0 && args[0].equals("--interactive");
        
        if (modoInteractivo) {
            Scanner scanner = null;
            try {
                scanner = new Scanner(System.in);
                
                while (true) {
                    System.out.println("\n--- SERVICIO PRODUCTO - LOGICA DE NEGOCIO ---");
                    System.out.println("1. Consultar producto (precio + disponibilidad)");
                    System.out.println("2. Calcular precio con descuento por cantidad");
                    System.out.println("3. Calcular precio final con IGV");
                    System.out.println("4. Simular cotizacion completa");
                    System.out.println("5. Verificar promociones activas");
                    System.out.println("0. Salir");
                    System.out.print("Opcion: ");
                    
                    if (!scanner.hasNextInt()) {
                        System.out.println("Error: Ingrese un numero");
                        scanner.nextLine();
                        continue;
                    }
                    
                    int opcion = scanner.nextInt();
                    scanner.nextLine();
                    
                    switch (opcion) {
                        case 1:
                            System.out.print("\nIngrese codigo producto: ");
                            String codigo = scanner.nextLine();
                            Producto prod = service.consultarProducto(codigo);
                            if (prod != null) {
                                System.out.println("Codigo: " + prod.getCodigo());
                                System.out.println("Nombre: " + prod.getNombre());
                                System.out.println("Precio: S/. " + prod.getPrecio());
                                System.out.println("Stock: " + prod.getStock());
                                System.out.println("Estado Stock: " + service.determinarEstadoStock(prod.getStock()));
                            } else {
                                System.out.println("Producto no encontrado");
                            }
                            break;
                            
                        case 2:
                            System.out.print("\nIngrese codigo producto: ");
                            String cod2 = scanner.nextLine();
                            Producto p2 = service.consultarProducto(cod2);
                            if (p2 != null) {
                                System.out.print("Ingrese cantidad: ");
                                int cant = scanner.nextInt();
                                double descPct = service.calcularDescuentoPorCantidad(cant);
                                double precioDesc = p2.getPrecio() * (1 - descPct / 100);
                                System.out.println("Precio unitario: S/. " + p2.getPrecio());
                                System.out.println("Descuento aplicado: " + descPct + "%");
                                System.out.println("Precio con descuento: S/. " + Math.round(precioDesc * 100.0) / 100.0);
                                System.out.println("Subtotal: S/. " + Math.round(precioDesc * cant * 100.0) / 100.0);
                            } else {
                                System.out.println("Producto no encontrado");
                            }
                            break;
                            
                        case 3:
                            System.out.print("\nIngrese precio base: ");
                            double precioBase = scanner.nextDouble();
                            double igv = precioBase * 0.18;
                            double total = precioBase + igv;
                            System.out.println("Precio base: S/. " + precioBase);
                            System.out.println("IGV (18%): S/. " + Math.round(igv * 100.0) / 100.0);
                            System.out.println("Precio final: S/. " + Math.round(total * 100.0) / 100.0);
                            break;
                            
                        case 4:
                            System.out.print("\nIngrese codigo producto: ");
                            String cod4 = scanner.nextLine();
                            Producto p4 = service.consultarProducto(cod4);
                            if (p4 != null) {
                                System.out.print("Ingrese cantidad: ");
                                int cant4 = scanner.nextInt();
                                String resultado = service.procesarSolicitudProducto(cod4 + "," + cant4);
                                System.out.println("\nCotizacion completa:");
                                System.out.println(formatearJSON(resultado));
                            } else {
                                System.out.println("Producto no encontrado");
                            }
                            break;
                            
                        case 5:
                            System.out.println("\nPromociones activas:");
                            System.out.println("- Compra 5+ unidades: 5% descuento");
                            System.out.println("- Compra 10+ unidades: 10% descuento");
                            System.out.println("- Compra 20+ unidades: 15% descuento");
                            System.out.println("- Compra mayor a S/.5000: 5% adicional");
                            System.out.println("- Compra mayor a S/.10000: 10% adicional");
                            break;
                            
                        case 0:
                            System.out.println("Saliendo...");
                            return;
                            
                        default:
                            System.out.println("Opcion invalida");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                if (scanner != null) scanner.close();
            }
        } else {
            new ProductoService().start();
        }
    }
    
    private static String formatearJSON(String json) {
        return json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }
}
