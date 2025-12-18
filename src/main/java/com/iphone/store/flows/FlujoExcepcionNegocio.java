package com.iphone.store.flows;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class FlujoExcepcionNegocio {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String ERROR_QUEUE = "ERROR.QUEUE";
    private static final String DEADLETTER_QUEUE = "DEADLETTER.QUEUE";
    private static final int MAX_REINTENTOS = 3;

    private Connection connection;
    private Session session;
    private Gson gson = new Gson();

    public FlujoExcepcionNegocio() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (Exception e) {
            System.err.println("Error al inicializar conexión: " + e.getMessage());
        }
    }

    public String validarDNI(String dni) {
        JsonObject resultado = new JsonObject();

        if (dni == null || dni.length() != 8) {
            String error = "DNI inválido: debe tener exactamente 8 dígitos";
            registrarExcepcionNegocio("VALIDACION_DNI", dni, error);
            resultado.addProperty("exito", false);
            resultado.addProperty("tipoError", "NEGOCIO");
            resultado.addProperty("codigo", "ERR_DNI_001");
            resultado.addProperty("mensaje", error);
            return gson.toJson(resultado);
        }

        for (char c : dni.toCharArray()) {
            if (!Character.isDigit(c)) {
                String error = "DNI inválido: solo debe contener números";
                registrarExcepcionNegocio("VALIDACION_DNI", dni, error);
                resultado.addProperty("exito", false);
                resultado.addProperty("tipoError", "NEGOCIO");
                resultado.addProperty("codigo", "ERR_DNI_002");
                resultado.addProperty("mensaje", error);
                return gson.toJson(resultado);
            }
        }

        resultado.addProperty("exito", true);
        resultado.addProperty("mensaje", "DNI válido");
        return gson.toJson(resultado);
    }

    public String validarRUC(String ruc) {
        JsonObject resultado = new JsonObject();

        if (ruc == null || ruc.length() != 11) {
            String error = "RUC inválido: debe tener exactamente 11 dígitos";
            registrarExcepcionNegocio("VALIDACION_RUC", ruc, error);
            resultado.addProperty("exito", false);
            resultado.addProperty("tipoError", "NEGOCIO");
            resultado.addProperty("codigo", "ERR_RUC_001");
            resultado.addProperty("mensaje", error);
            return gson.toJson(resultado);
        }

        for (char c : ruc.toCharArray()) {
            if (!Character.isDigit(c)) {
                String error = "RUC inválido: solo debe contener números";
                registrarExcepcionNegocio("VALIDACION_RUC", ruc, error);
                resultado.addProperty("exito", false);
                resultado.addProperty("tipoError", "NEGOCIO");
                resultado.addProperty("codigo", "ERR_RUC_003");
                resultado.addProperty("mensaje", error);
                return gson.toJson(resultado);
            }
        }

        String prefijo = ruc.substring(0, 2);
        if (!prefijo.equals("10") && !prefijo.equals("20") &&
                !prefijo.equals("15") && !prefijo.equals("17")) {
            String error = "RUC inválido: prefijo no corresponde a tipo de contribuyente válido";
            registrarExcepcionNegocio("VALIDACION_RUC", ruc, error);
            resultado.addProperty("exito", false);
            resultado.addProperty("tipoError", "NEGOCIO");
            resultado.addProperty("codigo", "ERR_RUC_002");
            resultado.addProperty("mensaje", error);
            return gson.toJson(resultado);
        }

        resultado.addProperty("exito", true);
        resultado.addProperty("mensaje", "RUC válido");
        return gson.toJson(resultado);
    }

    public String validarCantidad(int cantidad, int stockDisponible) {
        JsonObject resultado = new JsonObject();

        if (cantidad <= 0) {
            String error = "Cantidad inválida: debe ser mayor a 0";
            registrarExcepcionNegocio("VALIDACION_CANTIDAD", String.valueOf(cantidad), error);
            resultado.addProperty("exito", false);
            resultado.addProperty("tipoError", "NEGOCIO");
            resultado.addProperty("codigo", "ERR_CANT_001");
            resultado.addProperty("mensaje", error);
            return gson.toJson(resultado);
        }

        if (cantidad > stockDisponible) {
            String error = "Stock insuficiente. Disponible: " + stockDisponible + ", Solicitado: " + cantidad;
            registrarExcepcionNegocio("VALIDACION_STOCK", String.valueOf(cantidad), error);
            resultado.addProperty("exito", false);
            resultado.addProperty("tipoError", "NEGOCIO");
            resultado.addProperty("codigo", "ERR_STOCK_001");
            resultado.addProperty("mensaje", error);
            resultado.addProperty("stockDisponible", stockDisponible);
            return gson.toJson(resultado);
        }

        resultado.addProperty("exito", true);
        resultado.addProperty("mensaje", "Cantidad válida");
        return gson.toJson(resultado);
    }

    public String invocarServicioConReintentos(String requestQueue, String responseQueue, String mensaje) {
        int intentos = 0;
        Exception ultimaExcepcion = null;

        while (intentos < MAX_REINTENTOS) {
            try {
                intentos++;
                System.out.println("Intento " + intentos + " de " + MAX_REINTENTOS);

                return invocarServicio(requestQueue, responseQueue, mensaje);

            } catch (Exception e) {
                ultimaExcepcion = e;

                if (intentos < MAX_REINTENTOS) {
                    int tiempoEspera = (int) Math.pow(2, intentos - 1) * 1000;
                    System.out.println("Error en intento " + intentos + ". Reintentando en " + tiempoEspera + "ms...");

                    try {
                        Thread.sleep(tiempoEspera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return aplicarFallback(requestQueue, mensaje, ultimaExcepcion);
    }

    private String invocarServicio(String requestQueue, String responseQueue, String mensaje) throws JMSException {
        Destination request = session.createQueue(requestQueue);
        MessageProducer producer = session.createProducer(request);
        TextMessage message = session.createTextMessage(mensaje);
        producer.send(message);
        producer.close();

        Destination response = session.createQueue(responseQueue);
        MessageConsumer consumer = session.createConsumer(response);
        Message responseMessage = consumer.receive(5000);
        consumer.close();

        if (responseMessage == null) {
            throw new JMSException("Timeout: Servicio no respondió en tiempo esperado");
        }

        if (responseMessage instanceof TextMessage) {
            return ((TextMessage) responseMessage).getText();
        }

        throw new JMSException("Respuesta inválida del servicio");
    }

    private String aplicarFallback(String servicio, String mensaje, Exception excepcion) {
        JsonObject resultado = new JsonObject();

        registrarExcepcionTecnica(servicio, mensaje, excepcion);

        resultado.addProperty("exito", false);
        resultado.addProperty("tipoError", "TECNICO");
        resultado.addProperty("codigo", "ERR_TECNICO_001");
        resultado.addProperty("mensaje", "Servicio temporalmente no disponible");
        resultado.addProperty("servicio", servicio);
        resultado.addProperty("accion", "Por favor, intente nuevamente más tarde");
        resultado.addProperty("ticketSoporte", generarTicketSoporte());

        return gson.toJson(resultado);
    }

    private void registrarExcepcionNegocio(String tipo, String datos, String mensaje) {
        try {
            JsonObject errorLog = new JsonObject();
            errorLog.addProperty("timestamp", System.currentTimeMillis());
            errorLog.addProperty("tipoError", "NEGOCIO");
            errorLog.addProperty("categoria", tipo);
            errorLog.addProperty("datos", datos != null ? datos : "null");
            errorLog.addProperty("mensaje", mensaje);

            enviarACola(ERROR_QUEUE, gson.toJson(errorLog));
            System.out.println("[LOG NEGOCIO] " + tipo + ": " + mensaje);

        } catch (Exception e) {
            System.err.println("Error al registrar excepción de negocio: " + e.getMessage());
        }
    }

    private void registrarExcepcionTecnica(String servicio, String mensaje, Exception excepcion) {
        try {
            JsonObject errorLog = new JsonObject();
            errorLog.addProperty("timestamp", System.currentTimeMillis());
            errorLog.addProperty("tipoError", "TECNICO");
            errorLog.addProperty("servicio", servicio);
            errorLog.addProperty("mensajeOriginal", mensaje);
            errorLog.addProperty("excepcion", excepcion.getClass().getName());
            errorLog.addProperty("detalleExcepcion", excepcion.getMessage());
            errorLog.addProperty("intentosRealizados", MAX_REINTENTOS);

            enviarACola(DEADLETTER_QUEUE, gson.toJson(errorLog));
            System.out.println("[LOG TECNICO] Servicio: " + servicio + " - Error: " + excepcion.getMessage());

        } catch (Exception e) {
            System.err.println("Error al registrar excepción técnica: " + e.getMessage());
        }
    }

    private void enviarACola(String nombreCola, String mensaje) throws JMSException {
        Destination cola = session.createQueue(nombreCola);
        MessageProducer producer = session.createProducer(cola);
        TextMessage message = session.createTextMessage(mensaje);
        producer.send(message);
        producer.close();
    }

    private String generarTicketSoporte() {
        return "TKT-" + System.currentTimeMillis();
    }

    public void cerrar() {
        try {
            if (session != null)
                session.close();
            if (connection != null)
                connection.close();
        } catch (Exception e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        FlujoExcepcionNegocio flujo = new FlujoExcepcionNegocio();

        System.out.println("========================================");
        System.out.println("   FLUJO ESB: MANEJO DE EXCEPCIONES");
        System.out.println("========================================\n");

        System.out.println("=== PRUEBAS DE EXCEPCIONES DE NEGOCIO ===\n");

        System.out.println("Prueba 1 - DNI corto:");
        System.out.println(flujo.validarDNI("1234567"));

        System.out.println("\nPrueba 2 - DNI con letras:");
        System.out.println(flujo.validarDNI("1234A678"));

        System.out.println("\nPrueba 3 - RUC con prefijo inválido:");
        System.out.println(flujo.validarRUC("99999999999"));

        System.out.println("\nPrueba 4 - Cantidad cero:");
        System.out.println(flujo.validarCantidad(0, 10));

        System.out.println("\nPrueba 5 - Stock insuficiente:");
        System.out.println(flujo.validarCantidad(100, 10));

        System.out.println("\n=== PRUEBA DE EXCEPCIÓN TÉCNICA ===\n");

        System.out.println("Prueba 6 - Servicio no disponible:");
        System.out.println(flujo.invocarServicioConReintentos(
                "SERVICIO.INEXISTENTE.REQUEST",
                "SERVICIO.INEXISTENTE.RESPONSE",
                "test"));

        System.out.println("\n========================================");
        System.out.println("PRUEBAS COMPLETADAS");
        System.out.println("========================================");

        flujo.cerrar();
    }
}
