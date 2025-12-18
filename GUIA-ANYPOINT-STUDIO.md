# GUÍA ANYPOINT STUDIO - FLUJOS ESB

## 📋 Prerrequisitos

1. ✅ Anypoint Studio instalado
2. ✅ ActiveMQ corriendo (puerto 61616)
3. ✅ Tus 6 servicios Java funcionando
4. ✅ Bonita con el proceso listo

---

## 🚀 PROYECTO 1: Flujo de Integración (Punto c.i)

Este flujo integra los 6 servicios web (punto a) con el proceso de negocio (punto b).

### Paso 1: Crear nuevo proyecto Mule

1. Abrir **Anypoint Studio**
2. `File` → `New` → `Mule Project`
3. **Project Name:** `iphone-store-integration-flow`
4. **Runtime:** Mule Server 4.x (el que tengas)
5. Click **Finish**

### Paso 2: Configurar HTTP Listener (entrada)

1. En el **Package Explorer**, abrir `src/main/mule/iphone-store-integration-flow.xml`
2. Desde **Mule Palette**, arrastrar:
   - **HTTP** → **Listener** al canvas
3. Click en el **Listener**, configurar:
   - **Path:** `/venta-completa`
   - **Allowed methods:** `POST`
   - **General** → **Connector configuration:** Click `+`
     - **Host:** `0.0.0.0`
     - **Port:** `8091`
     - Click **OK**

### Paso 3: Agregar Transform (parsear entrada)

1. Arrastrar **Transform Message** después del Listener
2. En el editor DataWeave, poner:

```dataweave
%dw 2.0
output application/json
---
{
    dniCliente: payload.dniCliente,
    codigoProducto: payload.codigoProducto,
    cantidad: payload.cantidad
}
```

### Paso 4: Llamar al servicio RENIEC (Validación)

1. Arrastrar **HTTP** → **Request** después del Transform
2. Configurar:
   - **Method:** `POST`
   - **URL:** `http://localhost:8081/reniec/validar`
   - **Connector configuration:** Click `+`
     - **Host:** `localhost`
     - **Port:** `8081`
     - Click **OK**
3. En **Body**, seleccionar:
   ```json
   {"dni": #[payload.dniCliente]}
   ```

### Paso 5: Validar respuesta RENIEC (Choice)

1. Arrastrar **Choice** después del HTTP Request
2. En **When** (condición):
   - **Expression:** `#[payload.exito == true]`
3. Si **true** → continuar flujo
4. Si **false** → ir a manejo de error

### Paso 6: Llamar al servicio Producto

1. Dentro del **When** (true), arrastrar **HTTP Request**
2. Configurar:
   - **Method:** `POST`
   - **URL:** `http://localhost:8082/producto/consultar`
   - **Body:**
   ```json
   {"codigo": #[vars.codigoProducto]}
   ```

### Paso 7: Llamar al servicio Inventario

1. Arrastrar otro **HTTP Request**
2. Configurar:
   - **Method:** `POST`
   - **URL:** `http://localhost:8083/inventario/verificar`
   - **Body:**
   ```json
   {
     "codigoProducto": #[vars.codigoProducto],
     "cantidad": #[vars.cantidad]
   }
   ```

### Paso 8: Calcular totales y registrar venta

1. Arrastrar **Set Variable** → Nombre: `subtotal`, Value: `#[payload.precio * vars.cantidad]`
2. Arrastrar **Set Variable** → Nombre: `igv`, Value: `#[vars.subtotal * 0.18]`
3. Arrastrar **Set Variable** → Nombre: `total`, Value: `#[vars.subtotal + vars.igv]`
4. Arrastrar **HTTP Request** para VentaService:
   - **URL:** `http://localhost:8084/venta/registrar`
   - **Body:**
   ```json
   {
     "dniCliente": #[vars.dniCliente],
     "codigoProducto": #[vars.codigoProducto],
     "cantidad": #[vars.cantidad],
     "total": #[vars.total]
   }
   ```

### Paso 9: Respuesta final

1. Arrastrar **Transform Message** al final
2. Código DataWeave:
```dataweave
%dw 2.0
output application/json
---
{
    exito: true,
    mensaje: "Venta procesada exitosamente",
    ventaId: payload.id,
    total: vars.total
}
```

### Paso 10: Manejo de errores (Default en Choice)

1. En el **Default** del Choice, arrastrar **Transform Message**
2. Código:
```dataweave
%dw 2.0
output application/json
---
{
    exito: false,
    mensaje: "Error en la validación del cliente"
}
```

---

## 🚀 PROYECTO 2: Flujo con Excepciones y Colas JMS (Punto c.ii + c.iii)

Este flujo maneja excepciones y usa **ActiveMQ (colas JMS)**.

### Paso 1: Crear nuevo proyecto

1. `File` → `New` → `Mule Project`
2. **Project Name:** `iphone-store-exception-flow`
3. Click **Finish**

### Paso 2: Instalar ActiveMQ Connector

1. Click derecho en el proyecto → `Manage Dependencies`
2. Buscar: `ActiveMQ Connector`
3. O agregar manualmente al `pom.xml`:

```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule-jms-connector</artifactId>
    <version>1.8.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Paso 3: Configurar ActiveMQ Connection

1. Abrir el XML del flujo
2. Click **Global Elements** (abajo)
3. Click **Create** → Buscar **JMS Config**
4. Configurar:
   - **Name:** `ActiveMQ_Config`
   - **Provider:** `Active MQ`
   - **Broker URL:** `tcp://localhost:61616`
   - Click **OK**

### Paso 4: Crear flujo de entrada HTTP

1. Arrastrar **HTTP Listener**
   - **Path:** `/procesar-con-validacion`
   - **Port:** `8092`

### Paso 5: Agregar validaciones de negocio

1. Arrastrar **Choice** después del Listener
2. **When 1:** DNI inválido
   - **Expression:** `#[payload.dni == null or sizeOf(payload.dni) != 8]`
   - Acción: Enviar a cola de errores

3. **When 2:** Cantidad inválida
   - **Expression:** `#[payload.cantidad <= 0]`
   - Acción: Enviar a cola de errores

4. **Default:** Procesamiento exitoso

### Paso 6: Enviar a cola de errores (Excepciones de Negocio)

En **When 1** (DNI inválido):

1. Arrastrar **Transform Message**:
```dataweave
%dw 2.0
output application/json
---
{
    tipoError: "NEGOCIO",
    codigo: "ERR_DNI_001",
    mensaje: "DNI inválido: debe tener 8 dígitos",
    timestamp: now(),
    datos: payload
}
```

2. Arrastrar **JMS** → **Publish**
   - **Destination:** `ERROR.QUEUE`
   - **Destination type:** `QUEUE`
   - **Config:** Seleccionar `ActiveMQ_Config`

### Paso 7: Intentar llamar servicio (con reintentos)

En **Default** (validación OK):

1. Arrastrar **Try** scope
2. Dentro del Try, arrastrar **HTTP Request**
   - **URL:** `http://localhost:8081/reniec/validar`
   - Configurar **Reconnection**:
     - **Count:** `3`
     - **Frequency:** `2000` (ms)

### Paso 8: Catch de excepciones técnicas

1. En el **Try**, agregar **Error Handler** → **On Error Propagate**
2. **Type:** `HTTP:TIMEOUT, HTTP:CONNECTIVITY`
3. Dentro del Error Handler:

   a. **Transform Message**:
   ```dataweave
   %dw 2.0
   output application/json
   ---
   {
       tipoError: "TECNICO",
       codigo: "ERR_TIMEOUT",
       mensaje: "Servicio no disponible después de 3 intentos",
       intentos: 3,
       timestamp: now()
   }
   ```

   b. **JMS Publish**
      - **Destination:** `DEADLETTER.QUEUE`
      - **Config:** `ActiveMQ_Config`

### Paso 9: Respuesta exitosa

Después del **Try**, agregar **Transform Message**:
```dataweave
%dw 2.0
output application/json
---
{
    exito: true,
    mensaje: "Procesamiento completado",
    resultado: payload
}
```

### Paso 10: Consumer de colas (opcional - para ver mensajes)

Crear un flujo adicional:

1. Arrastrar **JMS** → **Listener** al canvas
   - **Destination:** `ERROR.QUEUE`
   - **Config:** `ActiveMQ_Config`

2. Arrastrar **Logger**
   - **Message:** `Error recibido: #[payload]`

---

## 📁 Estructura de archivos Anypoint

```
iphone-store-integration-flow/
├── src/main/mule/
│   └── iphone-store-integration-flow.xml
├── src/main/resources/
│   └── application.properties
└── pom.xml

iphone-store-exception-flow/
├── src/main/mule/
│   └── iphone-store-exception-flow.xml
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

---

## ⚙️ Configuración application.properties

Para cada proyecto, crear `src/main/resources/application.properties`:

```properties
# HTTP
http.port=8091  # o 8092 para el segundo flujo

# ActiveMQ
activemq.brokerUrl=tcp://localhost:61616
activemq.username=admin
activemq.password=admin

# Servicios Java
reniec.url=http://localhost:8081/reniec
producto.url=http://localhost:8082/producto
inventario.url=http://localhost:8083/inventario
venta.url=http://localhost:8084/venta
```

---

## 🧪 Probar los flujos

### Flujo 1 - Integración

```bash
curl -X POST http://localhost:8091/venta-completa \
  -H "Content-Type: application/json" \
  -d '{
    "dniCliente": "12345678",
    "codigoProducto": "IP13",
    "cantidad": 2
  }'
```

### Flujo 2 - Excepciones

```bash
# Caso 1: DNI inválido (debe enviar a ERROR.QUEUE)
curl -X POST http://localhost:8092/procesar-con-validacion \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "123",
    "codigoProducto": "IP13",
    "cantidad": 2
  }'

# Caso 2: Procesamiento exitoso
curl -X POST http://localhost:8092/procesar-con-validacion \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678",
    "codigoProducto": "IP13",
    "cantidad": 2
  }'
```

---

## 🔍 Verificar colas ActiveMQ

1. Abrir: http://localhost:8161/admin
2. Usuario: `admin` / Password: `admin`
3. Click **Queues**
4. Verificar:
   - `ERROR.QUEUE` (errores de negocio)
   - `DEADLETTER.QUEUE` (errores técnicos)
5. Click en una cola → **Browse** para ver mensajes

---

## ✅ Checklist de entrega

- [ ] Proyecto 1: `iphone-store-integration-flow` creado
- [ ] Proyecto 2: `iphone-store-exception-flow` creado
- [ ] ActiveMQ configurado en ambos
- [ ] Flujo 1 llama a los 6 servicios
- [ ] Flujo 2 usa colas JMS (ERROR.QUEUE, DEADLETTER.QUEUE)
- [ ] Ambos flujos probados con curl
- [ ] Colas verificadas en consola ActiveMQ

---

## 🎯 Resumen para el profesor

**Implementé 2 flujos ESB en Anypoint Studio:**

1. **Flujo de Integración** (puerto 8091):
   - Orquesta los 6 servicios REST Java
   - Valida cliente (RENIEC)
   - Consulta producto
   - Verifica stock
   - Calcula totales
   - Registra venta
   - Integra con proceso de Bonita

2. **Flujo de Excepciones** (puerto 8092):
   - Valida reglas de negocio (DNI, cantidad)
   - Maneja errores técnicos (timeouts, servicios caídos)
   - **Usa ActiveMQ/JMS:**
     - ERROR.QUEUE para errores de negocio
     - DEADLETTER.QUEUE para errores técnicos
   - Reintentos automáticos (3 veces)

**Tecnologías:**
- Mule 4
- ActiveMQ (colas JMS)
- HTTP connectors
- DataWeave transformations
- Error handling

---

## 📝 Próximos pasos

1. Seguir esta guía paso a paso en Anypoint Studio
2. Probar ambos flujos con curl
3. Verificar mensajes en ActiveMQ
4. Exportar proyectos para entrega (.jar deployables)
