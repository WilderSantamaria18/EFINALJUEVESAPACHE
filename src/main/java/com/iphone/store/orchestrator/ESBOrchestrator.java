package com.iphone.store.orchestrator;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.util.Scanner;

public class ESBOrchestrator {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private javax.jms.Connection connection;
    private Session session;

    public ESBOrchestrator() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            System.out.println("Orquestador ESB iniciado correctamente");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String consultarReniec(String dni) {
        return enviarSolicitud("RENIEC.REQUEST", "RENIEC.RESPONSE", dni);
    }

    public String consultarRuc(String ruc) {
        return enviarSolicitud("RUC.REQUEST", "RUC.RESPONSE", ruc);
    }

    public String consultarProducto(String codigo) {
        return enviarSolicitud("PRODUCTO.REQUEST", "PRODUCTO.RESPONSE", codigo);
    }

    public String consultarVenta(String id) {
        return enviarSolicitud("VENTA.REQUEST", "VENTA.RESPONSE", id);
    }

    public String consultarInventario(String codigoProducto) {
        return enviarSolicitud("INVENTARIO.REQUEST", "INVENTARIO.RESPONSE", codigoProducto);
    }

    public String consultarEmpleado(String dni) {
        return enviarSolicitud("EMPLEADO.REQUEST", "EMPLEADO.RESPONSE", dni);
    }

    private String enviarSolicitud(String requestQueue, String responseQueue, String mensaje) {
        try {
            Destination request = session.createQueue(requestQueue);
            MessageProducer producer = session.createProducer(request);
            TextMessage message = session.createTextMessage(mensaje);
            producer.send(message);
            producer.close();

            System.out.println("Orquestador envio solicitud a " + requestQueue + ": " + mensaje);

            Destination response = session.createQueue(responseQueue);
            MessageConsumer consumer = session.createConsumer(response);
            Message responseMessage = consumer.receive(5000);
            consumer.close();

            if (responseMessage instanceof TextMessage) {
                String respuesta = ((TextMessage) responseMessage).getText();
                System.out.println("Orquestador recibio respuesta de " + responseQueue);
                return respuesta;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void cerrar() {
        try {
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ESBOrchestrator orchestrator = new ESBOrchestrator();
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;

        mostrarBienvenida();

        while (continuar) {
            mostrarMenu();
            System.out.print("Seleccione una opcion: ");
            
            try {
                int opcion = scanner.nextInt();
                scanner.nextLine();
                System.out.println();

                switch (opcion) {
                    case 1:
                        System.out.print("Ingrese DNI (8 digitos): ");
                        String dni = scanner.nextLine();
                        System.out.println("\n--- Consultando RENIEC ---");
                        String personaJson = orchestrator.consultarReniec(dni);
                        mostrarResultado(personaJson);
                        break;

                    case 2:
                        System.out.print("Ingrese RUC (11 digitos): ");
                        String ruc = scanner.nextLine();
                        System.out.println("\n--- Consultando RUC ---");
                        String empresaJson = orchestrator.consultarRuc(ruc);
                        mostrarResultado(empresaJson);
                        break;

                    case 3:
                        System.out.print("Ingrese codigo de producto: ");
                        String codigo = scanner.nextLine();
                        System.out.println("\n--- Consultando Producto ---");
                        String productoJson = orchestrator.consultarProducto(codigo);
                        mostrarResultado(productoJson);
                        break;

                    case 4:
                        System.out.print("Ingrese ID de venta: ");
                        String idVenta = scanner.nextLine();
                        System.out.println("\n--- Consultando Venta ---");
                        String ventaJson = orchestrator.consultarVenta(idVenta);
                        mostrarResultado(ventaJson);
                        break;

                    case 5:
                        System.out.print("Ingrese codigo de producto: ");
                        String codigoProd = scanner.nextLine();
                        System.out.println("\n--- Consultando Inventario ---");
                        String inventarioJson = orchestrator.consultarInventario(codigoProd);
                        mostrarResultado(inventarioJson);
                        break;

                    case 6:
                        System.out.print("Ingrese DNI del empleado: ");
                        String dniEmp = scanner.nextLine();
                        System.out.println("\n--- Consultando Empleado ---");
                        String empleadoJson = orchestrator.consultarEmpleado(dniEmp);
                        mostrarResultado(empleadoJson);
                        break;

                    

                    case 0:
                        continuar = false;
                        System.out.println("Cerrando orquestador...");
                        break;

                    default:
                        System.out.println("Opcion invalida. Intente nuevamente.");
                }

                if (continuar && opcion != 7) {
                    System.out.println("\nPresione ENTER para continuar...");
                    scanner.nextLine();
                }

            } catch (Exception e) {
                System.out.println("Error: Ingrese un numero valido");
                scanner.nextLine();
            }
        }

        orchestrator.cerrar();
        scanner.close();
        System.out.println("\nOrquestador finalizado");
    }

    private static void mostrarBienvenida() {
        System.out.println("========================================");
        System.out.println("   ORQUESTADOR ESB - IPHONE STORE");
        System.out.println("========================================");
        System.out.println("Sistema de integracion de servicios");
        System.out.println("usando Apache ActiveMQ y JMS");
        System.out.println("========================================\n");
    }

    private static void mostrarMenu() {
        System.out.println("\n======== MENU PRINCIPAL ========");
        System.out.println("1. Consultar RENIEC (DNI)");
        System.out.println("2. Consultar RUC (Empresa)");
        System.out.println("3. Consultar Producto");
        System.out.println("4. Consultar Venta");
        System.out.println("5. Consultar Inventario");
        System.out.println("6. Consultar Empleado");
    
        System.out.println("0. Salir");
        System.out.println("================================");
    }

    private static void mostrarResultado(String json) {
        if (json != null && !json.equals("null")) {
            System.out.println("\nRespuesta JSON:");
            System.out.println(json);
        } else {
            System.out.println("\nNo se encontraron datos o el servicio no respondio");
            System.out.println("Verifique que el servicio este activo");
        }
    }

    
}
