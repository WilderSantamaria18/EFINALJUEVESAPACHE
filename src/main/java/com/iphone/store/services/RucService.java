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
import java.util.Scanner;

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
        RucService service = new RucService();
        boolean modoInteractivo = args.length > 0 && args[0].equals("--interactive");
        
        if (modoInteractivo) {
            Scanner scanner = null;
            try {
                scanner = new Scanner(System.in);
                
                while (true) {
                    System.out.println("\n--- SERVICIO RUC - LOGICA DE NEGOCIO ---");
                    System.out.println("1. Consultar RUC (validaciones + tipo contribuyente)");
                    System.out.println("2. Validar digito verificador RUC");
                    System.out.println("3. Determinar regimen tributario");
                    System.out.println("4. Calcular nivel de riesgo empresa");
                    System.out.println("5. Verificar si puede facturar");
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
                            System.out.print("\nIngrese RUC (11 digitos): ");
                            String ruc = scanner.nextLine();
                            String resultado = service.procesarConsultaRUC(ruc);
                            System.out.println("\nResultado:");
                            System.out.println(formatearJSON(resultado));
                            break;
                            
                        case 2:
                            System.out.print("\nIngrese RUC para validar: ");
                            String rucValidar = scanner.nextLine();
                            boolean valido = service.validarDigitoVerificador(rucValidar);
                            System.out.println("Digito verificador: " + (valido ? "VALIDO" : "INVALIDO"));
                            break;
                            
                        case 3:
                            System.out.print("\nIngrese RUC: ");
                            String rucRegimen = scanner.nextLine();
                            String tipo = service.determinarTipoContribuyente(rucRegimen);
                            String regimen = service.determinarRegimenTributario(rucRegimen);
                            System.out.println("Tipo contribuyente: " + tipo);
                            System.out.println("Regimen tributario sugerido: " + regimen);
                            break;
                            
                        case 4:
                            System.out.print("\nIngrese RUC: ");
                            String rucRiesgo = scanner.nextLine();
                            Empresa emp = service.consultarRUC(rucRiesgo);
                            if (emp != null) {
                                String riesgo = service.calcularNivelRiesgo(emp);
                                System.out.println("Razon Social: " + emp.getRazonSocial());
                                System.out.println("Estado: " + emp.getEstado());
                                System.out.println("Nivel de Riesgo: " + riesgo);
                            } else {
                                System.out.println("RUC no encontrado");
                            }
                            break;
                            
                        case 5:
                            System.out.print("\nIngrese RUC: ");
                            String rucFacturar = scanner.nextLine();
                            Empresa empresa = service.consultarRUC(rucFacturar);
                            if (empresa != null) {
                                boolean puede = service.puedeFacturar(empresa);
                                System.out.println("Razon Social: " + empresa.getRazonSocial());
                                System.out.println("Estado: " + empresa.getEstado());
                                System.out.println("Puede Facturar: " + (puede ? "SI" : "NO"));
                                if (!puede) {
                                    System.out.println("Motivo: " + service.obtenerMotivoNoFactura(empresa));
                                }
                            } else {
                                System.out.println("RUC no encontrado");
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
            new RucService().start();
        }
    }
    
    private static String formatearJSON(String json) {
        return json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }
    
    private boolean validarDigitoVerificador(String ruc) {
        if (ruc == null || ruc.length() != 11) return false;
        int[] factores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(ruc.charAt(i)) * factores[i];
        }
        int resto = suma % 11;
        int digitoEsperado = 11 - resto;
        if (digitoEsperado >= 10) digitoEsperado = digitoEsperado - 10;
        return Character.getNumericValue(ruc.charAt(10)) == digitoEsperado;
    }
    
    private String determinarRegimenTributario(String ruc) {
        String tipo = determinarTipoContribuyente(ruc);
        if ("PERSONA_NATURAL".equals(tipo)) {
            return "NUEVO RUS o REGIMEN ESPECIAL";
        } else if ("PERSONA_JURIDICA".equals(tipo)) {
            return "REGIMEN GENERAL o MYPE TRIBUTARIO";
        }
        return "REGIMEN GENERAL";
    }
    
    private boolean puedeFacturar(Empresa empresa) {
        return "ACTIVO".equalsIgnoreCase(empresa.getEstado()) 
               && empresa.getDireccion() != null 
               && !empresa.getDireccion().isEmpty();
    }
    
    private String obtenerMotivoNoFactura(Empresa empresa) {
        if (!"ACTIVO".equalsIgnoreCase(empresa.getEstado())) {
            return "Contribuyente no activo (estado: " + empresa.getEstado() + ")";
        }
        if (empresa.getDireccion() == null || empresa.getDireccion().isEmpty()) {
            return "No tiene direccion fiscal registrada";
        }
        return "Sin restricciones";
    }
}
