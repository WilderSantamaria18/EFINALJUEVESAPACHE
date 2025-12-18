# GUIA ANYPOINT STUDIO - 2 FLUJOS ESB (INTEGRACION + EXCEPCIONES) CON JMS

Este documento reemplaza la guia anterior cuando tu integracion ESB se basa en JMS/ActiveMQ (no en HTTP directo a los servicios Java).

Objetivo del trabajo:
- Flujo 1: integrar el punto A (servicios con logica) con el punto B (proceso Bonita) a traves del ESB.
- Flujo 2: flujo alterno de manejo de excepcion de negocio o tecnica.
- En al menos un flujo usar colas JMS (ActiveMQ) (aqui se usa en ambos).


## 1) Prerrequisitos

1. ActiveMQ ejecutandose
   - Broker (JMS): tcp://localhost:61616
   - Consola web (opcional): http://localhost:8161

2. Servicios Java ejecutandose en modo JMS
   - Windows: ejecutar iniciar-servicios.bat
   - Ubuntu: ejecutar iniciar-servicios.sh

3. Colas (ya usadas por tu codigo Java)
   - RENIEC.REQUEST / RENIEC.RESPONSE
   - RUC.REQUEST / RUC.RESPONSE
   - PRODUCTO.REQUEST / PRODUCTO.RESPONSE
   - INVENTARIO.REQUEST / INVENTARIO.RESPONSE
   - VENTA.REQUEST / VENTA.RESPONSE
   - EMPLEADO.REQUEST / EMPLEADO.RESPONSE

4. Bonita listo (proceso del punto B)
   - Bonita debe poder consumir un endpoint HTTP (el ESB lo expone).


## 2) Instalar Anypoint Studio (Windows o Ubuntu)

Nota: no puedo instalarlo automaticamente desde aqui, pero estos pasos son los oficiales y funcionan.

1. Descarga
   - Ir a MuleSoft: https://www.mulesoft.com/lp/dl/studio
   - Descargar Anypoint Studio (Mule 4).

2. Java
   - Instalar JDK (recomendado JDK 11).
   - Configurar JAVA_HOME.

3. Instalar y abrir
   - Windows: descomprimir y ejecutar AnypointStudio.exe
   - Ubuntu: descomprimir, dar permisos y ejecutar AnypointStudio


## 3) PROYECTO UNICO CON 2 FLUJOS

En Anypoint Studio:
1. File -> New -> Mule Project
2. Project Name: iphone-store-esb-jms
3. Runtime: Mule 4.x


## 4) Configuracion: HTTP Listener + JMS (ActiveMQ)

Vas a necesitar dos conectores:
- HTTP (Listener)
- JMS (para ActiveMQ)

En Mule 4, agrega el JMS Connector desde Exchange o desde la paleta.

Crea Global Elements:

A) HTTP Listener Config
- Host: 0.0.0.0
- Port: 8091

B) JMS Config (ActiveMQ)
- Broker URL: tcp://localhost:61616

Si tu Studio no trae plantilla de ActiveMQ lista, usa JMS Generic y agrega dependencias (solo si te lo pide):
- org.apache.activemq:activemq-client (version compatible con tu ActiveMQ)


## 5) FLUJO 1: INTEGRACION (Bonita -> ESB -> Servicios JMS)

Idea:
- Bonita llama a un endpoint del ESB (HTTP POST).
- El ESB valida datos y orquesta llamadas JMS a:
  1) RENIEC (validar cliente)
  2) PRODUCTO (precio + descuentos + igv)
  3) INVENTARIO (stock/alertas)
  4) VENTA (registro/resultado final)
- El ESB retorna JSON a Bonita.


### 5.1 Endpoint de entrada
- HTTP Listener
  - Path: /api/venta-completa
  - Method: POST
  - Port: 8091

Ejemplo de request (desde Bonita o Postman):
```json
{
   "dniCliente": "12345678",
   "codigoProducto": "IPHONE14",
   "cantidad": 2
}
```


### 5.2 Contrato JMS (lo que esperan tus servicios)
Tus servicios Java reciben un TextMessage con este contenido:
- ReniecService: "<dni>" (ej: "12345678")
- ProductoService: "<codigo>,<cantidad>" (ej: "IPHONE14,2")
- InventarioService: "<codigo>" (ej: "IPHONE14")
- VentaService: "<dni>,<codigo>,<cantidad>" (ej: "12345678,IPHONE14,2")

Y responden en su RESPONSE con un JSON (string).

Importante:
- Tus servicios responden siempre a colas fijas *.RESPONSE.
- Para demo, ejecuta una solicitud a la vez (request-reply simple), porque no hay correlacion (JMSCorrelationID) implementada.


### 5.3 Pasos en el canvas (recomendado)
Dentro del flujo principal:
1) Transform Message: normaliza payload a variables
2) Validacion basica (Choice)
3) JMS Publish -> RENIEC.REQUEST (body: dni)
4) JMS Consume -> RENIEC.RESPONSE (timeout 5000)
5) Choice: si RENIEC exito=false, retornar error
6) JMS Publish -> PRODUCTO.REQUEST (body: codigo,cantidad)
7) JMS Consume -> PRODUCTO.RESPONSE
8) JMS Publish -> INVENTARIO.REQUEST (body: codigo)
9) JMS Consume -> INVENTARIO.RESPONSE
10) JMS Publish -> VENTA.REQUEST (body: dni,codigo,cantidad)
11) JMS Consume -> VENTA.RESPONSE
12) Transform Message: respuesta final


### 5.4 Esqueleto XML (copiar como referencia)
Nota: los nombres exactos de tags pueden variar segun el conector JMS que instales en tu Studio.
La idea es la misma: publicar a *.REQUEST y consumir de *.RESPONSE.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mule xmlns:http="http://www.mulesoft.org/schema/mule/http"
         xmlns:jms="http://www.mulesoft.org/schema/mule/jms"
         xmlns:ee="http://www.mulesoft.org/schema/mule/ee/core"
         xmlns="http://www.mulesoft.org/schema/mule/core"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
            http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
            http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd
            http://www.mulesoft.org/schema/mule/jms http://www.mulesoft.org/schema/mule/jms/current/mule-jms.xsd
            http://www.mulesoft.org/schema/mule/ee/core http://www.mulesoft.org/schema/mule/ee/core/current/mule-ee.xsd">

   <http:listener-config name="HTTP_8091" >
      <http:listener-connection host="0.0.0.0" port="8091" />
   </http:listener-config>

   <jms:config name="ActiveMQ_Config">
      <jms:active-mq-connection brokerUrl="tcp://localhost:61616" />
   </jms:config>

   <flow name="venta-completa-esb">
      <http:listener config-ref="HTTP_8091" path="/api/venta-completa" allowedMethods="POST" />

      <ee:transform>
         <ee:message>
            <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
payload]]></ee:set-payload>
         </ee:message>
         <ee:variables>
            <ee:set-variable variableName="dni" ><![CDATA[#[payload.dniCliente]]]></ee:set-variable>
            <ee:set-variable variableName="codigo" ><![CDATA[#[payload.codigoProducto]]]></ee:set-variable>
            <ee:set-variable variableName="cantidad" ><![CDATA[#[payload.cantidad as Number]]]></ee:set-variable>
         </ee:variables>
      </ee:transform>

      <choice>
         <when expression="#[isEmpty(vars.dni) or (sizeOf(vars.dni) != 8)]">
            <set-payload value='{"exito":false,"mensaje":"DNI invalido"}' mimeType="application/json" />
         </when>
         <when expression="#[vars.cantidad <= 0]">
            <set-payload value='{"exito":false,"mensaje":"Cantidad invalida"}' mimeType="application/json" />
         </when>
         <otherwise>
            <jms:publish config-ref="ActiveMQ_Config" destination="RENIEC.REQUEST" destinationType="QUEUE">
               <jms:message>
                  <jms:body><![CDATA[#[vars.dni]]]></jms:body>
               </jms:message>
            </jms:publish>
            <jms:consume config-ref="ActiveMQ_Config" destination="RENIEC.RESPONSE" destinationType="QUEUE" timeout="5000" />
            <set-variable variableName="reniecJson" value="#[payload]" />

            <jms:publish config-ref="ActiveMQ_Config" destination="PRODUCTO.REQUEST" destinationType="QUEUE">
               <jms:message>
                  <jms:body><![CDATA[#[vars.codigo ++ ',' ++ (vars.cantidad as String)]]]></jms:body>
               </jms:message>
            </jms:publish>
            <jms:consume config-ref="ActiveMQ_Config" destination="PRODUCTO.RESPONSE" destinationType="QUEUE" timeout="5000" />
            <set-variable variableName="productoJson" value="#[payload]" />

            <jms:publish config-ref="ActiveMQ_Config" destination="INVENTARIO.REQUEST" destinationType="QUEUE">
               <jms:message>
                  <jms:body><![CDATA[#[vars.codigo]]]></jms:body>
               </jms:message>
            </jms:publish>
            <jms:consume config-ref="ActiveMQ_Config" destination="INVENTARIO.RESPONSE" destinationType="QUEUE" timeout="5000" />
            <set-variable variableName="inventarioJson" value="#[payload]" />

            <jms:publish config-ref="ActiveMQ_Config" destination="VENTA.REQUEST" destinationType="QUEUE">
               <jms:message>
                  <jms:body><![CDATA[#[vars.dni ++ ',' ++ vars.codigo ++ ',' ++ (vars.cantidad as String)]]]></jms:body>
               </jms:message>
            </jms:publish>
            <jms:consume config-ref="ActiveMQ_Config" destination="VENTA.RESPONSE" destinationType="QUEUE" timeout="5000" />
            <set-variable variableName="ventaJson" value="#[payload]" />

            <ee:transform>
               <ee:message>
                  <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
{
   exito: true,
   mensaje: "Venta procesada via ESB",
   reniec: read(vars.reniecJson, "application/json"),
   producto: read(vars.productoJson, "application/json"),
   inventario: read(vars.inventarioJson, "application/json"),
   venta: read(vars.ventaJson, "application/json")
}]]></ee:set-payload>
               </ee:message>
            </ee:transform>
         </otherwise>
      </choice>
   </flow>

</mule>
```


### 5.5 Ejemplo de respuesta final
```json
{
   "exito": true,
   "mensaje": "Venta procesada via ESB",
   "reniec": { "...": "..." },
   "producto": { "...": "..." },
   "inventario": { "...": "..." },
   "venta": { "...": "..." }
}
```


## 6) FLUJO 2: EXCEPCIONES (NEGOCIO + TECNICO) CON COLAS JMS

Objetivo:
- Validar negocio y enviar errores de negocio a una cola ERROR.QUEUE
- Manejar errores tecnicos (timeout/conectividad) y enviar a DEADLETTER.QUEUE
- Responder al cliente (Bonita/Postman) con un JSON claro.

Colas a crear/usar en ActiveMQ:
- ERROR.QUEUE
- DEADLETTER.QUEUE


### 6.1 Entrada
- HTTP Listener
  - Path: /api/procesar-con-validacion
  - Port: 8092 (puede ser 8091 si prefieres el mismo listener, pero separarlo es mas claro)


### 6.2 Validaciones de negocio (Choice)
Reglas sugeridas:
- DNI invalido: null o longitud != 8
- Cantidad invalida: <= 0

Cuando falle:
- Transform Message: construir JSON de error NEGOCIO
- JMS Publish -> ERROR.QUEUE
- Transform Message: respuesta al cliente (exito=false)


### 6.3 Manejo tecnico (Try + Error Handler)
En el default (cuando las validaciones pasan):
- Try scope
  - JMS Publish -> RENIEC.REQUEST
  - JMS Consume -> RENIEC.RESPONSE con timeout

Error Handler (On Error Propagate) para:
- TIMEOUT / CONNECTIVITY (segun el tipo que te muestre Studio)

Accion del handler:
- Transform Message: JSON error TECNICO
- JMS Publish -> DEADLETTER.QUEUE
- Transform Message: respuesta al cliente (exito=false)


### 6.4 Esqueleto XML (flujo 2, referencia)
```xml
<flow name="procesar-con-validacion">
   <http:listener config-ref="HTTP_8092" path="/api/procesar-con-validacion" allowedMethods="POST" />

   <choice>
      <when expression="#[payload.dni == null or sizeOf(payload.dni) != 8]">
         <set-variable variableName="err" value='{"tipoError":"NEGOCIO","codigo":"ERR_DNI_001","mensaje":"DNI invalido","datos":#[payload]}' />
         <jms:publish config-ref="ActiveMQ_Config" destination="ERROR.QUEUE" destinationType="QUEUE">
            <jms:message>
               <jms:body><![CDATA[#[vars.err]]]></jms:body>
            </jms:message>
         </jms:publish>
         <set-payload value='{"exito":false,"mensaje":"DNI invalido"}' mimeType="application/json" />
      </when>

      <when expression="#[payload.cantidad <= 0]">
         <set-variable variableName="err" value='{"tipoError":"NEGOCIO","codigo":"ERR_CANT_001","mensaje":"Cantidad invalida","datos":#[payload]}' />
         <jms:publish config-ref="ActiveMQ_Config" destination="ERROR.QUEUE" destinationType="QUEUE">
            <jms:message>
               <jms:body><![CDATA[#[vars.err]]]></jms:body>
            </jms:message>
         </jms:publish>
         <set-payload value='{"exito":false,"mensaje":"Cantidad invalida"}' mimeType="application/json" />
      </when>

      <otherwise>
         <try>
            <jms:publish config-ref="ActiveMQ_Config" destination="RENIEC.REQUEST" destinationType="QUEUE">
               <jms:message>
                  <jms:body><![CDATA[#[payload.dni]]]></jms:body>
               </jms:message>
            </jms:publish>
            <jms:consume config-ref="ActiveMQ_Config" destination="RENIEC.RESPONSE" destinationType="QUEUE" timeout="3000" />

            <set-payload value='{"exito":true,"mensaje":"OK"}' mimeType="application/json" />
         <error-handler>
            <on-error-propagate>
               <set-variable variableName="err" value='{"tipoError":"TECNICO","codigo":"ERR_TIMEOUT","mensaje":"Timeout o conectividad"}' />
               <jms:publish config-ref="ActiveMQ_Config" destination="DEADLETTER.QUEUE" destinationType="QUEUE">
                  <jms:message>
                     <jms:body><![CDATA[#[vars.err]]]></jms:body>
                  </jms:message>
               </jms:publish>
               <set-payload value='{"exito":false,"mensaje":"Error tecnico"}' mimeType="application/json" />
            </on-error-propagate>
         </error-handler>
         </try>
      </otherwise>
   </choice>
</flow>
```


### 6.4 Flujo consumidor de colas de error (opcional pero recomendado)
Crea 2 flows adicionales:
- JMS Listener -> ERROR.QUEUE -> Logger / Write to file
- JMS Listener -> DEADLETTER.QUEUE -> Logger / Write to file

Esto demuestra el patron de "manejo asincrono de errores".


## 7) Integrar Bonita (Punto B) con el ESB (Punto C)

En Bonita, la forma mas directa es:
- En el proceso, agrega un conector HTTP (REST) que llame al ESB.
- URL: http://localhost:8091/api/venta-completa
- Method: POST
- Body: con variables del proceso (dniCliente, codigoProducto, cantidad)

Luego, Bonita recibe el JSON del ESB y decide el siguiente paso:
- Si exito=true -> continuar proceso
- Si exito=false -> ir a flujo alterno (tarea de revision / notificacion)


## 8) Prueba rapida sin Bonita (Postman / curl)

Flujo 1:
- POST http://localhost:8091/api/venta-completa

Flujo 2:
- POST http://localhost:8092/api/procesar-con-validacion


## 9) Checklist de demostracion (para sustentacion)

1. ActiveMQ arriba (61616) y se ven las colas.
2. Servicios Java arriba (cada uno escuchando su *.REQUEST).
3. Mule Flow 1 recibe HTTP y orquesta JMS (se ven mensajes en REQUEST/RESPONSE).
4. Mule Flow 2 manda errores de negocio a ERROR.QUEUE.
5. Mule Flow 2 manda errores tecnicos a DEADLETTER.QUEUE.
6. Bonita llama al ESB y toma decision segun exito.
