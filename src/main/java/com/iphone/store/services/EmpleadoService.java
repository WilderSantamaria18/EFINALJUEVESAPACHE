package com.iphone.store.services;

import com.iphone.store.config.DatabaseConnection;
import com.iphone.store.model.Empleado;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;
import java.util.Scanner;

public class EmpleadoService {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String REQUEST_QUEUE = "EMPLEADO.REQUEST";
    private static final String RESPONSE_QUEUE = "EMPLEADO.RESPONSE";
    private Gson gson = new Gson();

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination requestQueue = session.createQueue(REQUEST_QUEUE);
            MessageConsumer consumer = session.createConsumer(requestQueue);

            System.out.println("Servicio EMPLEADO iniciado y escuchando...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String dni = textMessage.getText();
                        System.out.println("EMPLEADO recibio solicitud DNI: " + dni);

                        String jsonResponse = procesarConsultaEmpleado(dni);

                        Destination replyQueue = session.createQueue(RESPONSE_QUEUE);
                        MessageProducer producer = session.createProducer(replyQueue);
                        TextMessage response = session.createTextMessage(jsonResponse);
                        producer.send(response);
                        producer.close();

                        System.out.println("EMPLEADO envio respuesta para DNI: " + dni);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String procesarConsultaEmpleado(String dni) {
        JsonObject resultado = new JsonObject();

        Empleado empleado = consultarEmpleado(dni);

        if (empleado == null) {
            resultado.addProperty("exito", false);
            resultado.addProperty("mensaje", "Empleado con DNI " + dni + " no encontrado");
            return gson.toJson(resultado);
        }

        int antiguedad = calcularAntiguedad(empleado.getFechaIngreso());
        double porcentajeBonificacion = calcularPorcentajeBonificacion(empleado.getCargo(), antiguedad);
        double bonificacion = empleado.getSalario() * (porcentajeBonificacion / 100);
        double salarioTotal = empleado.getSalario() + bonificacion;
        String nivelEmpleado = determinarNivelEmpleado(antiguedad);
        String categoriaEmpleado = determinarCategoria(empleado.getSalario());
        boolean elegibleAscenso = antiguedad >= 2 && !"GERENTE".equalsIgnoreCase(empleado.getCargo());
        double aporteAFP = salarioTotal * 0.13;
        double aporteSalud = salarioTotal * 0.09;
        double salarioNeto = salarioTotal - aporteAFP - aporteSalud;

        resultado.addProperty("exito", true);
        resultado.addProperty("id", empleado.getId());
        resultado.addProperty("dni", empleado.getDni());
        resultado.addProperty("nombreCompleto", empleado.getNombreCompleto());
        resultado.addProperty("cargo", empleado.getCargo());
        resultado.addProperty("fechaIngreso", empleado.getFechaIngreso());
        resultado.addProperty("antiguedadAnios", antiguedad);
        resultado.addProperty("salarioBase", empleado.getSalario());
        resultado.addProperty("porcentajeBonificacion", porcentajeBonificacion);
        resultado.addProperty("bonificacion", Math.round(bonificacion * 100.0) / 100.0);
        resultado.addProperty("salarioTotal", Math.round(salarioTotal * 100.0) / 100.0);
        resultado.addProperty("aporteAFP", Math.round(aporteAFP * 100.0) / 100.0);
        resultado.addProperty("aporteSalud", Math.round(aporteSalud * 100.0) / 100.0);
        resultado.addProperty("salarioNeto", Math.round(salarioNeto * 100.0) / 100.0);
        resultado.addProperty("nivelEmpleado", nivelEmpleado);
        resultado.addProperty("categoriaEmpleado", categoriaEmpleado);
        resultado.addProperty("elegibleAscenso", elegibleAscenso);

        return gson.toJson(resultado);
    }

    private int calcularAntiguedad(String fechaIngreso) {
        try {
            LocalDate fecha = LocalDate.parse(fechaIngreso);
            LocalDate hoy = LocalDate.now();
            return Period.between(fecha, hoy).getYears();
        } catch (Exception e) {
            return 0;
        }
    }

    private double calcularPorcentajeBonificacion(String cargo, int antiguedad) {
        double bonificacionBase = 0;

        if ("GERENTE".equalsIgnoreCase(cargo)) {
            bonificacionBase = 20;
        } else if ("VENDEDOR".equalsIgnoreCase(cargo)) {
            bonificacionBase = 10;
        } else {
            bonificacionBase = 5;
        }

        if (antiguedad >= 5) {
            bonificacionBase += 10;
        } else if (antiguedad >= 3) {
            bonificacionBase += 5;
        } else if (antiguedad >= 1) {
            bonificacionBase += 2;
        }

        return bonificacionBase;
    }

    private String determinarNivelEmpleado(int antiguedad) {
        if (antiguedad >= 10) {
            return "SENIOR";
        } else if (antiguedad >= 5) {
            return "PLENO";
        } else if (antiguedad >= 2) {
            return "JUNIOR";
        } else {
            return "PRACTICANTE";
        }
    }

    private String determinarCategoria(double salario) {
        if (salario >= 5000) {
            return "A";
        } else if (salario >= 3000) {
            return "B";
        } else if (salario >= 1500) {
            return "C";
        } else {
            return "D";
        }
    }

    private Empleado consultarEmpleado(String dni) {
        Empleado empleado = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM empleados WHERE dni = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                empleado = new Empleado();
                empleado.setId(rs.getInt("id"));
                empleado.setDni(rs.getString("dni"));
                empleado.setNombreCompleto(rs.getString("nombre_completo"));
                empleado.setCargo(rs.getString("cargo"));
                empleado.setSalario(rs.getDouble("salario"));
                empleado.setFechaIngreso(rs.getString("fecha_ingreso"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empleado;
    }

    public static void main(String[] args) {
        EmpleadoService service = new EmpleadoService();
        boolean modoInteractivo = args.length > 0 && args[0].equals("--interactive");
        
        if (modoInteractivo) {
            Scanner scanner = null;
            try {
                scanner = new Scanner(System.in);
                
                while (true) {
                    System.out.println("\n--- SERVICIO EMPLEADO - LOGICA DE NEGOCIO ---");
                    System.out.println("1. Consultar empleado (salario + bonificaciones)");
                    System.out.println("2. Calcular comision por venta");
                    System.out.println("3. Calcular bonificacion por antiguedad");
                    System.out.println("4. Calcular salario neto");
                    System.out.println("5. Verificar elegibilidad ascenso");
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
                            System.out.print("\nIngrese DNI empleado: ");
                            String dni = scanner.nextLine();
                            String resultado = service.procesarConsultaEmpleado(dni);
                            System.out.println("\nResultado:");
                            System.out.println(formatearJSON(resultado));
                            break;
                            
                        case 2:
                            System.out.print("\nIngrese monto de venta: ");
                            double montoVenta = scanner.nextDouble();
                            System.out.print("Ingrese cargo (VENDEDOR/GERENTE): ");
                            scanner.nextLine();
                            String cargo = scanner.nextLine();
                            double comision = service.calcularComision(montoVenta, cargo);
                            System.out.println("Monto venta: S/. " + montoVenta);
                            System.out.println("Cargo: " + cargo);
                            System.out.println("Comision: S/. " + Math.round(comision * 100.0) / 100.0);
                            System.out.println("\nTasas de comision:");
                            System.out.println("- Vendedor: 2% de la venta");
                            System.out.println("- Gerente: 1% de la venta");
                            break;
                            
                        case 3:
                            System.out.print("\nIngrese salario base: ");
                            double salario = scanner.nextDouble();
                            System.out.print("Ingrese antiguedad (anios): ");
                            int antiguedad = scanner.nextInt();
                            System.out.print("Ingrese cargo: ");
                            scanner.nextLine();
                            String cargoB = scanner.nextLine();
                            double pctBono = service.calcularPorcentajeBonificacion(cargoB, antiguedad);
                            double bono = salario * (pctBono / 100);
                            System.out.println("Salario base: S/. " + salario);
                            System.out.println("Antiguedad: " + antiguedad + " anios");
                            System.out.println("Porcentaje bonificacion: " + pctBono + "%");
                            System.out.println("Bonificacion: S/. " + Math.round(bono * 100.0) / 100.0);
                            break;
                            
                        case 4:
                            System.out.print("\nIngrese salario bruto: ");
                            double salarioBruto = scanner.nextDouble();
                            double aporteAFP = salarioBruto * 0.13;
                            double aporteSalud = salarioBruto * 0.09;
                            double salarioNeto = salarioBruto - aporteAFP - aporteSalud;
                            System.out.println("Salario bruto: S/. " + salarioBruto);
                            System.out.println("Aporte AFP (13%): S/. " + Math.round(aporteAFP * 100.0) / 100.0);
                            System.out.println("Aporte Salud (9%): S/. " + Math.round(aporteSalud * 100.0) / 100.0);
                            System.out.println("Salario neto: S/. " + Math.round(salarioNeto * 100.0) / 100.0);
                            break;
                            
                        case 5:
                            System.out.print("\nIngrese antiguedad (anios): ");
                            int ant = scanner.nextInt();
                            System.out.print("Ingrese cargo actual: ");
                            scanner.nextLine();
                            String cargoAct = scanner.nextLine();
                            boolean elegible = ant >= 2 && !"GERENTE".equalsIgnoreCase(cargoAct);
                            String nivel = service.determinarNivelEmpleado(ant);
                            System.out.println("Antiguedad: " + ant + " anios");
                            System.out.println("Cargo: " + cargoAct);
                            System.out.println("Nivel: " + nivel);
                            System.out.println("Elegible para ascenso: " + (elegible ? "SI" : "NO"));
                            if (!elegible && "GERENTE".equalsIgnoreCase(cargoAct)) {
                                System.out.println("Motivo: Ya tiene cargo maximo");
                            } else if (!elegible) {
                                System.out.println("Motivo: Requiere minimo 2 anios de antiguedad");
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
            new EmpleadoService().start();
        }
    }
    
    private static String formatearJSON(String json) {
        return json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }
    
    private double calcularComision(double montoVenta, String cargo) {
        if ("VENDEDOR".equalsIgnoreCase(cargo)) {
            return montoVenta * 0.02;
        } else if ("GERENTE".equalsIgnoreCase(cargo)) {
            return montoVenta * 0.01;
        }
        return 0;
    }
}
