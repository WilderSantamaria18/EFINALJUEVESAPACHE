package com.iphone.store.flows;

import com.iphone.store.orchestrator.ESBOrchestrator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FlujoVentaCompleta {
    private ESBOrchestrator orchestrator;
    private Gson gson = new Gson();

    public FlujoVentaCompleta() {
        this.orchestrator = new ESBOrchestrator();
    }

    public String ejecutarVenta(String dniCliente, String codigoProducto, int cantidad) {
        JsonObject resultado = new JsonObject();

        try {
            System.out.println("=== PASO 1: Validando cliente en RENIEC ===");
            String respuestaReniec = orchestrator.consultarReniec(dniCliente);

            if (respuestaReniec == null || respuestaReniec.equals("null")) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "VALIDACION_CLIENTE");
                resultado.addProperty("mensaje", "Servicio RENIEC no disponible");
                return gson.toJson(resultado);
            }

            JsonObject clienteJson = JsonParser.parseString(respuestaReniec).getAsJsonObject();

            if (!clienteJson.has("exito") || !clienteJson.get("exito").getAsBoolean()) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "VALIDACION_CLIENTE");
                resultado.addProperty("mensaje",
                        clienteJson.has("mensaje") ? clienteJson.get("mensaje").getAsString() : "Cliente no válido");
                return gson.toJson(resultado);
            }

            if (clienteJson.has("esMayorEdad") && !clienteJson.get("esMayorEdad").getAsBoolean()) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "VALIDACION_EDAD");
                resultado.addProperty("mensaje", "Cliente debe ser mayor de edad");
                return gson.toJson(resultado);
            }

            String nombreCliente = clienteJson.has("nombreCompleto") ? clienteJson.get("nombreCompleto").getAsString()
                    : "Cliente";
            System.out.println("Cliente validado: " + nombreCliente);

            System.out.println("=== PASO 2: Consultando producto ===");
            String solicitudProducto = codigoProducto + "," + cantidad;
            String respuestaProducto = orchestrator.consultarProducto(solicitudProducto);

            if (respuestaProducto == null || respuestaProducto.equals("null")) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "CONSULTA_PRODUCTO");
                resultado.addProperty("mensaje", "Servicio PRODUCTO no disponible");
                return gson.toJson(resultado);
            }

            JsonObject productoJson = JsonParser.parseString(respuestaProducto).getAsJsonObject();

            if (!productoJson.has("exito") || !productoJson.get("exito").getAsBoolean()) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "CONSULTA_PRODUCTO");
                resultado.addProperty("mensaje", productoJson.has("mensaje") ? productoJson.get("mensaje").getAsString()
                        : "Producto no encontrado");
                return gson.toJson(resultado);
            }
            System.out.println("Producto encontrado: " + productoJson.get("nombre").getAsString());

            System.out.println("=== PASO 3: Verificando inventario ===");
            String respuestaInventario = orchestrator.consultarInventario(codigoProducto);

            if (respuestaInventario == null || respuestaInventario.equals("null")) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "CONSULTA_INVENTARIO");
                resultado.addProperty("mensaje", "Servicio INVENTARIO no disponible");
                return gson.toJson(resultado);
            }

            JsonObject inventarioJson = JsonParser.parseString(respuestaInventario).getAsJsonObject();

            if (!inventarioJson.has("exito") || !inventarioJson.get("exito").getAsBoolean()) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "CONSULTA_INVENTARIO");
                resultado.addProperty("mensaje",
                        inventarioJson.has("mensaje") ? inventarioJson.get("mensaje").getAsString()
                                : "Error en inventario");
                return gson.toJson(resultado);
            }

            if (productoJson.has("disponible") && !productoJson.get("disponible").getAsBoolean()) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "VERIFICACION_STOCK");
                resultado.addProperty("mensaje", "Stock insuficiente");
                resultado.addProperty("stockDisponible", inventarioJson.get("cantidad").getAsInt());
                return gson.toJson(resultado);
            }
            System.out.println("Stock disponible: " + inventarioJson.get("cantidad").getAsInt());

            System.out.println("=== PASO 4: Procesando venta ===");
            String solicitudVenta = dniCliente + "," + codigoProducto + "," + cantidad;
            String respuestaVenta = orchestrator.consultarVenta(solicitudVenta);

            if (respuestaVenta == null || respuestaVenta.equals("null")) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "REGISTRO_VENTA");
                resultado.addProperty("mensaje", "Servicio VENTA no disponible");
                return gson.toJson(resultado);
            }

            JsonObject ventaJson = JsonParser.parseString(respuestaVenta).getAsJsonObject();

            if (!ventaJson.has("exito") || !ventaJson.get("exito").getAsBoolean()) {
                resultado.addProperty("exito", false);
                resultado.addProperty("paso", "REGISTRO_VENTA");
                resultado.addProperty("mensaje",
                        ventaJson.has("mensaje") ? ventaJson.get("mensaje").getAsString() : "Error al registrar venta");
                return gson.toJson(resultado);
            }

            resultado.addProperty("exito", true);
            resultado.addProperty("mensaje", "Venta procesada exitosamente");
            resultado.addProperty("idVenta", ventaJson.get("idVenta").getAsInt());
            resultado.addProperty("cliente", nombreCliente);
            resultado.addProperty("producto", productoJson.get("nombre").getAsString());
            resultado.addProperty("cantidad", cantidad);
            resultado.addProperty("total", ventaJson.get("total").getAsDouble());

            System.out.println("=== VENTA COMPLETADA EXITOSAMENTE ===");

        } catch (Exception e) {
            resultado.addProperty("exito", false);
            resultado.addProperty("paso", "ERROR_TECNICO");
            resultado.addProperty("mensaje", "Error técnico: " + e.getMessage());
            e.printStackTrace();
        }

        return gson.toJson(resultado);
    }

    public void cerrar() {
        orchestrator.cerrar();
    }

    public static void main(String[] args) {
        FlujoVentaCompleta flujo = new FlujoVentaCompleta();

        System.out.println("========================================");
        System.out.println("   FLUJO ESB: VENTA COMPLETA");
        System.out.println("========================================\n");

        String resultado = flujo.ejecutarVenta("12345678", "IPHONE14", 2);

        System.out.println("\n========================================");
        System.out.println("RESULTADO FINAL:");
        System.out.println("========================================");
        System.out.println(resultado);

        flujo.cerrar();
    }
}
