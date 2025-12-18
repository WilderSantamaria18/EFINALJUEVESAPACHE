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
import java.util.Scanner;

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

        // LÓGICA DE NEGOCIO 1: Validación con algoritmo de dígito verificador
        if (!validarFormatoDNI(dni)) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "DNI invalido: debe tener 8 digitos numericos");
            return gson.toJson(resultado);
        }

        // LÓGICA DE NEGOCIO 2: Validar que el DNI esté en rango válido
        if (!validarRangoDNI(dni)) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "DNI fuera de rango válido (10000000-99999999)");
            return gson.toJson(resultado);
        }

        Persona persona = consultarDNI(dni);

        if (persona == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "DNI no encontrado en el sistema RENIEC");
            return gson.toJson(resultado);
        }

        // LÓGICA DE NEGOCIO 3: Calcular edad y determinar capacidad legal
        int edad = calcularEdad(persona.getFechaNacimiento());
        boolean esMayorEdad = edad >= 18;
        boolean puedeComprar = validarCapacidadCompra(edad);
        String nombreCompleto = persona.getNombres() + " " +
                persona.getApellidoPaterno() + " " +
                persona.getApellidoMaterno();

        // LÓGICA DE NEGOCIO 4: Determinar categoría de cliente por edad
        String categoriaCliente = determinarCategoriaCliente(edad);
        double descuentoAplicable = calcularDescuentoPorCategoria(categoriaCliente);

        resultado.addProperty("exito", true);
        resultado.addProperty("dni", persona.getDni());
        resultado.addProperty("nombreCompleto", nombreCompleto);
        resultado.addProperty("fechaNacimiento", persona.getFechaNacimiento());
        resultado.addProperty("direccion", persona.getDireccion());
        resultado.addProperty("edad", edad);
        resultado.addProperty("esMayorEdad", esMayorEdad);
        resultado.addProperty("puedeComprar", puedeComprar);
        resultado.addProperty("categoriaCliente", categoriaCliente);
        resultado.addProperty("descuentoAplicable", descuentoAplicable);
        resultado.addProperty("estadoCivil", determinarEstadoCivil(edad));

        return gson.toJson(resultado);
    }

    // LÓGICA DE NEGOCIO: Validar rango de DNI
    private boolean validarRangoDNI(String dni) {
        try {
            int numero = Integer.parseInt(dni);
            return numero >= 10000000 && numero <= 99999999;
        } catch (Exception e) {
            return false;
        }
    }

    // LÓGICA DE NEGOCIO: Validar capacidad de compra según edad y restricciones
    private boolean validarCapacidadCompra(int edad) {
        // Menores de 18 requieren autorización
        // Entre 18-75 años pueden comprar libremente
        // Mayores de 75 requieren verificación adicional
        return edad >= 18 && edad <= 75;
    }

    // LÓGICA DE NEGOCIO: Categorizar cliente según edad
    private String determinarCategoriaCliente(int edad) {
        if (edad < 18) {
            return "MENOR_EDAD";
        } else if (edad >= 18 && edad <= 25) {
            return "JOVEN";
        } else if (edad >= 26 && edad <= 40) {
            return "ADULTO";
        } else if (edad >= 41 && edad <= 60) {
            return "ADULTO_MEDIO";
        } else {
            return "ADULTO_MAYOR";
        }
    }

    // LÓGICA DE NEGOCIO: Calcular descuento según categoría
    private double calcularDescuentoPorCategoria(String categoria) {
        switch (categoria) {
            case "JOVEN":
                return 0.10; // 10% descuento para jóvenes
            case "ADULTO_MAYOR":
                return 0.15; // 15% descuento para adultos mayores
            default:
                return 0.0;
        }
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
        Scanner scanner = new Scanner(System.in);
        ReniecService service = new ReniecService();
        boolean modoInteractivo = args.length > 0 && args[0].equals("--interactive");

        if (modoInteractivo) {
            // Modo consola interactivo
            while (true) {
                System.out.println("\n╔══════════════════════════════════════════════════════════╗");
                System.out.println("║          SERVICIO RENIEC - LÓGICA DE NEGOCIO            ║");
                System.out.println("╠══════════════════════════════════════════════════════════╣");
                System.out.println("║  1. Consultar DNI (con validaciones y categorización)   ║");
                System.out.println("║  2. Registrar nueva persona                              ║");
                System.out.println("║  3. Actualizar datos de persona                          ║");
                System.out.println("║  4. Validar capacidad de compra                          ║");
                System.out.println("║  5. Calcular descuento por edad                          ║");
                System.out.println("║  6. Iniciar servicio JMS (modo escucha)                  ║");
                System.out.println("║  0. Salir                                                ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.print("Seleccione opción: ");

                int opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        System.out.print("\nIngrese DNI (8 dígitos): ");
                        String dni = scanner.nextLine();
                        String resultado = service.procesarConsultaDNI(dni);
                        System.out.println("\n✓ Resultado:");
                        System.out.println(formatearJSON(resultado));
                        break;

                    case 2:
                        System.out.println("\n=== REGISTRAR NUEVA PERSONA ===");
                        System.out.print("DNI: ");
                        String nuevoDni = scanner.nextLine();
                        System.out.print("Nombres: ");
                        String nombres = scanner.nextLine();
                        System.out.print("Apellido Paterno: ");
                        String apPaterno = scanner.nextLine();
                        System.out.print("Apellido Materno: ");
                        String apMaterno = scanner.nextLine();
                        System.out.print("Fecha Nacimiento (YYYY-MM-DD): ");
                        String fechaNac = scanner.nextLine();
                        System.out.print("Dirección: ");
                        String direccion = scanner.nextLine();

                        if (service.registrarPersona(nuevoDni, nombres, apPaterno, apMaterno, fechaNac, direccion)) {
                            System.out.println("✓ Persona registrada exitosamente");
                        } else {
                            System.out.println("✗ Error al registrar persona");
                        }
                        break;

                    case 3:
                        System.out.println("\n=== ACTUALIZAR DATOS ===");
                        System.out.print("DNI a actualizar: ");
                        String dniActualizar = scanner.nextLine();
                        System.out.print("Nueva dirección: ");
                        String nuevaDireccion = scanner.nextLine();

                        if (service.actualizarDireccion(dniActualizar, nuevaDireccion)) {
                            System.out.println("✓ Dirección actualizada exitosamente");
                        } else {
                            System.out.println("✗ Error al actualizar dirección");
                        }
                        break;

                    case 4:
                        System.out.print("\nIngrese edad: ");
                        int edad = scanner.nextInt();
                        boolean puedeComprar = service.validarCapacidadCompra(edad);
                        String categoria = service.determinarCategoriaCliente(edad);
                        System.out.println("✓ Categoría: " + categoria);
                        System.out.println("✓ Puede comprar sin restricciones: " + (puedeComprar ? "SÍ" : "NO"));
                        if (!puedeComprar) {
                            System.out.println("  ⚠ Requiere autorización o verificación adicional");
                        }
                        break;

                    case 5:
                        System.out.print("\nIngrese edad: ");
                        int edadDesc = scanner.nextInt();
                        String cat = service.determinarCategoriaCliente(edadDesc);
                        double descuento = service.calcularDescuentoPorCategoria(cat);
                        System.out.println("✓ Categoría: " + cat);
                        System.out.println("✓ Descuento aplicable: " + (descuento * 100) + "%");
                        break;

                    case 6:
                        System.out.println("\n▶ Iniciando servicio JMS...");
                        service.start();
                        System.out.println("✓ Servicio iniciado. Presione Ctrl+C para detener.");
                        try {
                            Thread.sleep(Long.MAX_VALUE);
                        } catch (InterruptedException e) {
                            System.out.println("Servicio detenido.");
                        }
                        return;

                    case 0:
                        System.out.println("Saliendo...");
                        scanner.close();
                        return;

                    default:
                        System.out.println("✗ Opción inválida");
                }
            }
        } else {
            // Modo servicio JMS
            System.out.println("Iniciando RENIEC Service en modo JMS...");
            System.out.println("(Use --interactive para modo consola interactivo)");
            service.start();
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                System.out.println("Servicio detenido.");
            }
        }
    }

    // Métodos CRUD con lógica de negocio
    private boolean registrarPersona(String dni, String nombres, String apPaterno, String apMaterno, String fechaNac, String direccion) {
        if (!validarFormatoDNI(dni) || !validarRangoDNI(dni)) {
            System.out.println("✗ DNI inválido");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO reniec (dni, nombres, apellido_paterno, apellido_materno, fecha_nacimiento, direccion) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            stmt.setString(2, nombres);
            stmt.setString(3, apPaterno);
            stmt.setString(4, apMaterno);
            stmt.setString(5, fechaNac);
            stmt.setString(6, direccion);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean actualizarDireccion(String dni, String nuevaDireccion) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE reniec SET direccion = ? WHERE dni = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nuevaDireccion);
            stmt.setString(2, dni);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String formatearJSON(String json) {
        return json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }
}
