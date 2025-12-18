package com.iphone.store;

import com.iphone.store.flows.FlujoVentaCompleta;
import com.iphone.store.flows.FlujoExcepcionNegocio;
import java.util.Scanner;

/**
 * Clase principal para demostrar los 2 flujos ESB:
 * 1. Flujo de venta completa (integra servicios a+b)
 * 2. Flujo con manejo de excepciones (negocio y técnicas)
 * 
 * Requiere:
 * - ActiveMQ corriendo en tcp://localhost:61616
 * - Base de datos MySQL con las tablas necesarias
 * - Los 6 servicios REST levantados
 */
public class Main {
    
    public static void main(String[] args) {
        mostrarBienvenida();
        
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;
        
        while (continuar) {
            mostrarMenu();
            System.out.print("Seleccione una opción: ");
            
            try {
                int opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer
                System.out.println();
                
                switch (opcion) {
                    case 1:
                        ejecutarFlujoVentaCompleta(scanner);
                        break;
                    case 2:
                        ejecutarFlujoExcepciones(scanner);
                        break;
                    case 3:
                        demostrarColas();
                        break;
                    case 4:
                        verificarActiveMQ();
                        break;
                    case 0:
                        continuar = false;
                        System.out.println("✓ Saliendo del sistema ESB...");
                        break;
                    default:
                        System.out.println("✗ Opción inválida");
                }
                
                if (continuar) {
                    System.out.println("\nPresione ENTER para continuar...");
                    scanner.nextLine();
                }
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                scanner.nextLine(); // Limpiar buffer en caso de error
            }
        }
        
        scanner.close();
        System.out.println("Sistema finalizado.");
    }
    
    private static void mostrarBienvenida() {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                   SISTEMA ESB - IPHONE STORE                  ║");
        System.out.println("║                  Integración de Servicios Web                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static void mostrarMenu() {
        System.out.println("\n═══════════════ MENÚ PRINCIPAL ═══════════════");
        System.out.println("1. Ejecutar Flujo de Venta Completa (Integra servicios)");
        System.out.println("2. Ejecutar Flujo con Manejo de Excepciones");
        System.out.println("3. Demostrar Colas JMS/ActiveMQ");
        System.out.println("4. Verificar estado de ActiveMQ");
        System.out.println("0. Salir");
        System.out.println("═══════════════════════════════════════════════");
    }
    
    private static void ejecutarFlujoVentaCompleta(Scanner scanner) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║         FLUJO ESB 1: VENTA COMPLETA (Integración)            ║");
        System.out.println("║   Integra: RENIEC + RUC + Producto + Inventario + Venta      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        
        System.out.print("Ingrese DNI del cliente: ");
        String dni = scanner.nextLine();
        
        System.out.print("Ingrese código del producto: ");
        String codigoProducto = scanner.nextLine();
        
        System.out.print("Ingrese cantidad: ");
        int cantidad = scanner.nextInt();
        scanner.nextLine();
        
        System.out.println("\n→ Iniciando flujo de venta completa...\n");
        
        FlujoVentaCompleta flujo = new FlujoVentaCompleta();
        String resultado = flujo.ejecutarVenta(dni, codigoProducto, cantidad);
        
        System.out.println("\n═══ RESULTADO DEL FLUJO ═══");
        System.out.println(formatearJSON(resultado));
    }
    
    private static void ejecutarFlujoExcepciones(Scanner scanner) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║    FLUJO ESB 2: MANEJO DE EXCEPCIONES (Negocio y Técnicas)   ║");
        System.out.println("║          Usa colas JMS para registrar errores                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        
        FlujoExcepcionNegocio flujo = new FlujoExcepcionNegocio();
        
        System.out.println("=== PRUEBA 1: Validación de DNI (Excepción de Negocio) ===");
        System.out.print("Ingrese DNI a validar: ");
        String dni = scanner.nextLine();
        String resultadoDNI = flujo.validarDNI(dni);
        System.out.println("Resultado: " + formatearJSON(resultadoDNI));
        
        System.out.println("\n=== PRUEBA 2: Validación de Stock (Excepción de Negocio) ===");
        System.out.print("Ingrese cantidad solicitada: ");
        int cantidadSolicitada = scanner.nextInt();
        System.out.print("Ingrese stock disponible: ");
        int stockDisponible = scanner.nextInt();
        scanner.nextLine();
        String resultadoStock = flujo.validarCantidad(cantidadSolicitada, stockDisponible);
        System.out.println("Resultado: " + formatearJSON(resultadoStock));
        
        System.out.println("\n=== PRUEBA 3: Simulación de servicio (Excepción Técnica) ===");
        String resultadoTecnico = flujo.invocarServicioConReintentos("SERVICIO.PRUEBA.REQUEST", "SERVICIO.PRUEBA.RESPONSE", "datos_test");
        System.out.println("Resultado: " + formatearJSON(resultadoTecnico));
        
        flujo.cerrar();
    }
    
    private static void demostrarColas() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║               DEMOSTRACIÓN DE COLAS JMS/ActiveMQ              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("Colas configuradas en el sistema:");
        System.out.println("  • RENIEC.REQUEST / RENIEC.RESPONSE");
        System.out.println("  • RUC.REQUEST / RUC.RESPONSE");
        System.out.println("  • PRODUCTO.REQUEST / PRODUCTO.RESPONSE");
        System.out.println("  • INVENTARIO.REQUEST / INVENTARIO.RESPONSE");
        System.out.println("  • VENTA.REQUEST / VENTA.RESPONSE");
        System.out.println("  • EMPLEADO.REQUEST / EMPLEADO.RESPONSE");
        System.out.println("  • ERROR.QUEUE (para excepciones de negocio)");
        System.out.println("  • DEADLETTER.QUEUE (para excepciones técnicas)");
        
        System.out.println("\nCaracterísticas:");
        System.out.println("✓ Comunicación asíncrona entre servicios");
        System.out.println("✓ Reintentos automáticos (máx. 3)");
        System.out.println("✓ Dead Letter Queue para mensajes fallidos");
        System.out.println("✓ Persistencia de mensajes");
    }
    
    private static void verificarActiveMQ() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║            VERIFICACIÓN DE ACTIVEMQ                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("Para iniciar ActiveMQ:");
        System.out.println("1. Navegar a: <ACTIVEMQ_HOME>/bin");
        System.out.println("2. Ejecutar:");
        System.out.println("   Windows: activemq.bat start");
        System.out.println("   Linux/Mac: ./activemq start");
        System.out.println();
        System.out.println("Consola Web: http://localhost:8161/admin");
        System.out.println("Usuario: admin");
        System.out.println("Password: admin");
        System.out.println();
        System.out.println("Broker URL: tcp://localhost:61616");
        
        // Intentar conexión
        System.out.println("\n→ Intentando conectar a ActiveMQ...");
        try {
            org.apache.activemq.ActiveMQConnectionFactory factory = 
                new org.apache.activemq.ActiveMQConnectionFactory("tcp://localhost:61616");
            javax.jms.Connection conn = factory.createConnection();
            conn.start();
            System.out.println("✓ Conexión exitosa a ActiveMQ");
            conn.close();
        } catch (Exception e) {
            System.out.println("✗ No se pudo conectar a ActiveMQ: " + e.getMessage());
            System.out.println("  Asegúrese de que ActiveMQ esté corriendo.");
        }
    }
    
    private static String formatearJSON(String json) {
        if (json == null) return "null";
        // Formateo simple para mejor legibilidad
        return json.replace(",", ",\n  ")
                   .replace("{", "{\n  ")
                   .replace("}", "\n}");
    }
}
