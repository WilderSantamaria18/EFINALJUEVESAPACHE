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
import java.util.Scanner;

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
        InventarioService service = new InventarioService();
        boolean modoInteractivo = args.length > 0 && args[0].equals("--interactive");
        
        if (modoInteractivo) {
            Scanner scanner = null;
            try {
                scanner = new Scanner(System.in);
                
                while (true) {
                    System.out.println("\n--- SERVICIO INVENTARIO - LOGICA DE NEGOCIO ---");
                    System.out.println("1. Consultar inventario (stock + alertas)");
                    System.out.println("2. Verificar si requiere reposicion");
                    System.out.println("3. Calcular cantidad a reponer");
                    System.out.println("4. Calcular valor del inventario");
                    System.out.println("5. Generar alerta de stock bajo");
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
                            String resultado = service.procesarConsultaInventario(codigo);
                            System.out.println("\nResultado:");
                            System.out.println(formatearJSON(resultado));
                            break;
                            
                        case 2:
                            System.out.print("\nIngrese codigo producto: ");
                            String cod2 = scanner.nextLine();
                            Inventario inv = service.consultarInventario(cod2);
                            if (inv != null) {
                                boolean requiere = inv.getCantidad() <= STOCK_MINIMO;
                                System.out.println("Producto: " + cod2);
                                System.out.println("Stock actual: " + inv.getCantidad());
                                System.out.println("Stock minimo: " + STOCK_MINIMO);
                                System.out.println("Requiere reposicion: " + (requiere ? "SI" : "NO"));
                                System.out.println("Prioridad: " + service.determinarPrioridadReposicion(inv.getCantidad()));
                            } else {
                                System.out.println("Producto no encontrado en inventario");
                            }
                            break;
                            
                        case 3:
                            System.out.print("\nIngrese stock actual: ");
                            int stockActual = scanner.nextInt();
                            int cantidadReponer = service.calcularCantidadReposicion(stockActual);
                            System.out.println("Stock actual: " + stockActual);
                            System.out.println("Stock objetivo: 50");
                            System.out.println("Cantidad a reponer: " + cantidadReponer);
                            break;
                            
                        case 4:
                            System.out.print("\nIngrese codigo producto: ");
                            String cod4 = scanner.nextLine();
                            Inventario inv4 = service.consultarInventario(cod4);
                            if (inv4 != null) {
                                double valor = service.calcularValorInventario(cod4, inv4.getCantidad());
                                System.out.println("Producto: " + cod4);
                                System.out.println("Cantidad: " + inv4.getCantidad());
                                System.out.println("Valor total inventario: S/. " + Math.round(valor * 100.0) / 100.0);
                            } else {
                                System.out.println("Producto no encontrado");
                            }
                            break;
                            
                        case 5:
                            System.out.print("\nIngrese codigo producto: ");
                            String cod5 = scanner.nextLine();
                            Inventario inv5 = service.consultarInventario(cod5);
                            if (inv5 != null) {
                                String estado = service.determinarEstadoInventario(inv5.getCantidad());
                                String prioridad = service.determinarPrioridadReposicion(inv5.getCantidad());
                                System.out.println("ALERTA DE INVENTARIO");
                                System.out.println("Producto: " + cod5);
                                System.out.println("Stock: " + inv5.getCantidad());
                                System.out.println("Estado: " + estado);
                                System.out.println("Prioridad: " + prioridad);
                                if ("CRITICO".equals(estado) || "AGOTADO".equals(estado)) {
                                    System.out.println("ACCION REQUERIDA: Reponer inmediatamente");
                                }
                            } else {
                                System.out.println("Producto no encontrado");
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
            new InventarioService().start();
        }
    }
    
    private static String formatearJSON(String json) {
        return json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }
}
