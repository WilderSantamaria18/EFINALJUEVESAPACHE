package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Scanner;

public class VentaService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "VENTA.REQUEST";
    private static final String RESPONSE_QUEUE = "VENTA.RESPONSE";
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio VENTA iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String solicitud = textMessage.getText();
                        System.out.println("VENTA recibio solicitud: " + solicitud);

                        String jsonResponse = procesarSolicitudVenta(solicitud);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("VENTA envio respuesta");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarSolicitudVenta(String solicitud) {
        JsonObject resultado = new JsonObject();

        String[] partes = solicitud.split(",");
        if (partes.length < 3) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Formato invalido. Use: dniCliente,codigoProducto,cantidad");
            return gson.toJson(resultado);
        }

        String dniCliente = partes[0];
        String codigoProducto = partes[1];
        int cantidad = Integer.parseInt(partes[2]);

        if (!validarClienteExiste(dniCliente)) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Cliente con DNI " + dniCliente + " no encontrado en RENIEC");
            return gson.toJson(resultado);
        }

        JsonObject infoProducto = consultarProductoDisponible(codigoProducto, cantidad);
        if (!infoProducto.get("disponible").getAsBoolean()) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Stock insuficiente para " + codigoProducto);
            resultado.addProperty("stockDisponible", infoProducto.get("stock").getAsInt());
            return gson.toJson(resultado);
        }

        double precioUnitario = infoProducto.get("precio").getAsDouble();
        double descuento = calcularDescuentoVenta(cantidad, precioUnitario);
        double subtotal = (precioUnitario * cantidad) - descuento;
        double igv = subtotal * 0.18;
        double total = subtotal + igv;

        int idVenta = registrarVenta(dniCliente, total);
        if (idVenta > 0) {
            actualizarStock(codigoProducto, cantidad);
        }

        String nombreCliente = obtenerNombreCliente(dniCliente);

        resultado.addProperty("exito", true);
        resultado.addProperty("idVenta", idVenta);
        resultado.addProperty("fecha", LocalDate.now().toString());
        resultado.addProperty("cliente", nombreCliente);
        resultado.addProperty("dniCliente", dniCliente);
        resultado.addProperty("producto", infoProducto.get("nombre").getAsString());
        resultado.addProperty("cantidad", cantidad);
        resultado.addProperty("precioUnitario", precioUnitario);
        resultado.addProperty("descuento", Math.round(descuento * 100.0) / 100.0);
        resultado.addProperty("subtotal", Math.round(subtotal * 100.0) / 100.0);
        resultado.addProperty("igv", Math.round(igv * 100.0) / 100.0);
        resultado.addProperty("total", Math.round(total * 100.0) / 100.0);
        resultado.addProperty("estado", "COMPLETADO");
        resultado.addProperty("mensaje", "Venta registrada exitosamente");

        return gson.toJson(resultado);
    }

    private boolean validarClienteExiste(String dni) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT dni FROM reniec WHERE dni = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String obtenerNombreCliente(String dni) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT nombres, apellido_paterno FROM reniec WHERE dni = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nombres") + " " + rs.getString("apellido_paterno");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Cliente";
    }

    private JsonObject consultarProductoDisponible(String codigo, int cantidadRequerida) {
        JsonObject info = new JsonObject();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM productos WHERE codigo = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int stock = rs.getInt("stock");
                info.addProperty("nombre", rs.getString("nombre"));
                info.addProperty("precio", rs.getDouble("precio"));
                info.addProperty("stock", stock);
                info.addProperty("disponible", stock >= cantidadRequerida);
            } else {
                info.addProperty("disponible", false);
                info.addProperty("stock", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            info.addProperty("disponible", false);
        }
        return info;
    }

    private double calcularDescuentoVenta(int cantidad, double precioUnitario) {
        double totalBruto = precioUnitario * cantidad;
        if (totalBruto >= 10000) {
            return totalBruto * 0.10;
        } else if (totalBruto >= 5000) {
            return totalBruto * 0.05;
        } else if (cantidad >= 3) {
            return totalBruto * 0.03;
        }
        return 0;
    }

    private int registrarVenta(String dniCliente, double total) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO ventas (fecha, dni_cliente, total, estado) VALUES (?, ?, ?, 'COMPLETADO')";
            PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, LocalDate.now().toString());
            stmt.setString(2, dniCliente);
            stmt.setDouble(3, total);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void actualizarStock(String codigoProducto, int cantidadVendida) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE productos SET stock = stock - ? WHERE codigo = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cantidadVendida);
            stmt.setString(2, codigoProducto);
            stmt.executeUpdate();

            String sqlInventario = "UPDATE inventario SET cantidad = cantidad - ?, fecha_actualizacion = ? WHERE codigo_producto = ?";
            PreparedStatement stmtInv = conn.prepareStatement(sqlInventario);
            stmtInv.setInt(1, cantidadVendida);
            stmtInv.setString(2, LocalDate.now().toString());
            stmtInv.setString(3, codigoProducto);
            stmtInv.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        VentaService service = new VentaService();
        boolean modoInteractivo = args.length > 0 && args[0].equals("--interactive");
        
        if (modoInteractivo) {
            Scanner scanner = null;
            try {
                scanner = new Scanner(System.in);
                
                while (true) {
                    System.out.println("\n--- SERVICIO VENTA - LOGICA DE NEGOCIO ---");
                    System.out.println("1. Procesar venta completa");
                    System.out.println("2. Calcular descuento por monto");
                    System.out.println("3. Simular cotizacion");
                    System.out.println("4. Calcular puntos de fidelidad");
                    System.out.println("5. Verificar historial cliente");
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
                            System.out.print("\nDNI Cliente: ");
                            String dni = scanner.nextLine();
                            System.out.print("Codigo Producto: ");
                            String codProd = scanner.nextLine();
                            System.out.print("Cantidad: ");
                            int cantidad = scanner.nextInt();
                            String resultado = service.procesarSolicitudVenta(dni + "," + codProd + "," + cantidad);
                            System.out.println("\nResultado venta:");
                            System.out.println(formatearJSON(resultado));
                            break;
                            
                        case 2:
                            System.out.print("\nIngrese monto total: ");
                            double monto = scanner.nextDouble();
                            double descuento = service.calcularDescuentoVenta(1, monto);
                            System.out.println("Monto: S/. " + monto);
                            System.out.println("Descuento: S/. " + Math.round(descuento * 100.0) / 100.0);
                            System.out.println("Monto final: S/. " + Math.round((monto - descuento) * 100.0) / 100.0);
                            System.out.println("\nReglas de descuento:");
                            System.out.println("- Mayor a S/.10000: 10%");
                            System.out.println("- Mayor a S/.5000: 5%");
                            System.out.println("- 3+ productos: 3%");
                            break;
                            
                        case 3:
                            System.out.print("\nPrecio unitario: ");
                            double precio = scanner.nextDouble();
                            System.out.print("Cantidad: ");
                            int cant = scanner.nextInt();
                            double subtotal = precio * cant;
                            double desc = service.calcularDescuentoVenta(cant, precio);
                            double base = subtotal - desc;
                            double igv = base * 0.18;
                            double total = base + igv;
                            System.out.println("\nCotizacion:");
                            System.out.println("Subtotal: S/. " + Math.round(subtotal * 100.0) / 100.0);
                            System.out.println("Descuento: S/. " + Math.round(desc * 100.0) / 100.0);
                            System.out.println("Base imponible: S/. " + Math.round(base * 100.0) / 100.0);
                            System.out.println("IGV (18%): S/. " + Math.round(igv * 100.0) / 100.0);
                            System.out.println("TOTAL: S/. " + Math.round(total * 100.0) / 100.0);
                            break;
                            
                        case 4:
                            System.out.print("\nIngrese monto de compra: ");
                            double montoCompra = scanner.nextDouble();
                            int puntos = (int) (montoCompra / 10);
                            System.out.println("Monto: S/. " + montoCompra);
                            System.out.println("Puntos ganados: " + puntos);
                            System.out.println("(1 punto por cada S/.10 de compra)");
                            if (puntos >= 100) {
                                System.out.println("Cliente VIP - Acceso a promociones exclusivas");
                            }
                            break;
                            
                        case 5:
                            System.out.print("\nIngrese DNI cliente: ");
                            String dniHist = scanner.nextLine();
                            if (service.validarClienteExiste(dniHist)) {
                                String nombre = service.obtenerNombreCliente(dniHist);
                                System.out.println("Cliente: " + nombre);
                                System.out.println("DNI: " + dniHist);
                                System.out.println("Estado: REGISTRADO EN RENIEC");
                            } else {
                                System.out.println("Cliente no encontrado en RENIEC");
                            }
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
            new VentaService().start();
        }
    }
    
    private static String formatearJSON(String json) {
        return json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }
}
