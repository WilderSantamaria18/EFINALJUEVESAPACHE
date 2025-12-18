# GUÍA COMPLETA DEL ORQUESTADOR ESB

## 📋 ¿Qué es un ESB (Enterprise Service Bus)?

Un ESB es un patrón arquitectónico que facilita la comunicación entre diferentes servicios de una aplicación mediante un bus de mensajería central. Permite que los servicios se comuniquen de forma desacoplada, asíncrona y escalable.

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    ORQUESTADOR ESB                          │
│              (Coordina todos los servicios)                 │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   APACHE ACTIVEMQ                           │
│              (Message Broker - JMS)                         │
│                                                             │
│  Colas:                                                     │
│  ├─ RENIEC.REQUEST    / RENIEC.RESPONSE                    │
│  ├─ RUC.REQUEST       / RUC.RESPONSE                       │
│  ├─ PRODUCTO.REQUEST  / PRODUCTO.RESPONSE                  │
│  ├─ VENTA.REQUEST     / VENTA.RESPONSE                     │
│  ├─ INVENTARIO.REQUEST/ INVENTARIO.RESPONSE                │
│  └─ EMPLEADO.REQUEST  / EMPLEADO.RESPONSE                  │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  SERVICIOS (6 Servicios)                    │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│  │ RENIEC   │  │   RUC    │  │ PRODUCTO │                │
│  └──────────┘  └──────────┘  └──────────┘                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│  │  VENTA   │  │INVENTARIO│  │ EMPLEADO │                │
│  └──────────┘  └──────────┘  └──────────┘                │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    BASE DE DATOS MySQL                      │
│                     (iphone_store)                          │
│                                                             │
│  Tablas: reniec, ruc, productos, ventas,                   │
│          inventario, empleados                             │
└─────────────────────────────────────────────────────────────┘
```

## 🔄 Flujo de Comunicación

### Ejemplo: Consulta de DNI en RENIEC

1. **Usuario** ingresa DNI "12345678" en el orquestador
2. **Orquestador** crea un mensaje JMS y lo envía a la cola `RENIEC.REQUEST`
3. **ActiveMQ** almacena el mensaje en la cola
4. **Servicio RENIEC** (escuchando en `RENIEC.REQUEST`) recibe el mensaje
5. **Servicio RENIEC** consulta la base de datos MySQL
6. **Servicio RENIEC** crea respuesta JSON y la envía a `RENIEC.RESPONSE`
7. **Orquestador** recibe la respuesta desde `RENIEC.RESPONSE`
8. **Usuario** ve el resultado en la consola

```
Orquestador → [RENIEC.REQUEST] → ActiveMQ → Servicio RENIEC
                                               ↓
                                          MySQL DB
                                               ↓
Orquestador ← [RENIEC.RESPONSE] ← ActiveMQ ← Servicio RENIEC
```

## 📊 Colas JMS en ActiveMQ

Cada servicio utiliza **2 colas**:

| Servicio | Cola Request | Cola Response |
|----------|--------------|---------------|
| RENIEC | RENIEC.REQUEST | RENIEC.RESPONSE |
| RUC | RUC.REQUEST | RUC.RESPONSE |
| PRODUCTO | PRODUCTO.REQUEST | PRODUCTO.RESPONSE |
| VENTA | VENTA.REQUEST | VENTA.RESPONSE |
| INVENTARIO | INVENTARIO.REQUEST | INVENTARIO.RESPONSE |
| EMPLEADO | EMPLEADO.REQUEST | EMPLEADO.RESPONSE |

## 🖥️ Monitorear en ActiveMQ Web Console

### Acceder a la consola:
1. Abrir navegador: **http://localhost:8161/admin**
2. Usuario: `admin`
3. Password: `admin`

### Ver las colas:
1. Click en **"Queues"** en el menú superior
2. Verás una lista de todas las colas activas
3. Columnas importantes:
   - **Number Of Pending Messages**: Mensajes en espera
   - **Number Of Consumers**: Servicios escuchando
   - **Messages Enqueued**: Total de mensajes enviados
   - **Messages Dequeued**: Total de mensajes procesados

### Inspeccionar mensajes:
1. Click en el nombre de una cola (ej: `RENIEC.REQUEST`)
2. Verás los mensajes pendientes
3. Puedes ver el contenido de cada mensaje
4. Puedes eliminar o mover mensajes manualmente

## ✅ Ventajas del Orquestador ESB

### 1. **Desacoplamiento**
- Los servicios no se conocen entre sí
- Cada servicio es independiente
- Fácil de mantener y actualizar

### 2. **Escalabilidad**
- Agregar nuevos servicios sin modificar los existentes
- Múltiples instancias del mismo servicio pueden escuchar la misma cola
- Balanceo de carga automático

### 3. **Resiliencia**
- Si un servicio está caído, los mensajes se encolan
- Cuando el servicio se recupera, procesa los mensajes pendientes
- No se pierden solicitudes

### 4. **Asincronía**
- Comunicación no bloqueante
- El orquestador puede enviar múltiples solicitudes simultáneamente
- Mejor rendimiento

### 5. **Centralización**
- Un punto único (orquestador) coordina toda la lógica
- Fácil de entender y monitorear
- Logs centralizados

### 6. **Flexibilidad**
- Cambiar la implementación de un servicio sin afectar otros
- Agregar transformaciones de datos en el orquestador
- Implementar patrones de retry, circuit breaker, etc.

## 🎯 Casos de Uso del ESB

### En este proyecto:
- **Consulta RENIEC**: Validar identidad de clientes
- **Consulta RUC**: Validar proveedores
- **Consulta Producto**: Verificar disponibilidad
- **Consulta Venta**: Historial de ventas
- **Consulta Inventario**: Control de stock
- **Consulta Empleado**: Información de personal

### En producción real:
- Integración con servicios externos (bancos, APIs gubernamentales)
- Procesamiento de pagos
- Notificaciones (email, SMS, push)
- Sincronización entre sistemas legacy y modernos
- Auditoría y logging centralizado
- Transformación de formatos (XML ↔ JSON)

## 🔧 Configuración Técnica

### ActiveMQ
- **URL Broker**: tcp://localhost:61616
- **Puerto Web Console**: 8161
- **Protocolo**: JMS (Java Message Service)

### Base de Datos
- **Motor**: MySQL 8.0
- **Base de datos**: iphone_store
- **Host**: localhost:3306
- **Usuario**: root
- **Password**: root

### Servicios
- **Lenguaje**: Java 11+
- **Framework**: Apache ActiveMQ Client
- **Serialización**: Google Gson (JSON)

## 📝 Datos de Prueba

### DNI (8 dígitos):
- 12345678 - Juan Carlos Perez Lopez
- 87654321 - Maria Elena Garcia Rojas
- 11223344 - Pedro Luis Martinez Silva
- 44332211 - Ana Sofia Rodriguez Vargas

### RUC (11 dígitos):
- 20123456789 - COMERCIAL TECH SAC
- 20987654321 - DISTRIBUIDORA PERU EIRL
- 20111222333 - SERVICIOS GLOBALES SA
- 20444555666 - IMPORTACIONES DEL SUR SAC

### Productos:
- IPHONE13 - iPhone 13 128GB
- IPHONE14 - iPhone 14 256GB
- IPHONE15 - iPhone 15 Pro 512GB
- AIRPODS - AirPods Pro 2da Gen

### Ventas:
- ID: 1, 2, 3

## 🚀 Ejecución del Sistema

### 1. Iniciar ActiveMQ
```bash
cd C:\ruta\activemq\bin
activemq.bat start
```

### 2. Crear base de datos
```bash
mysql -u root -p < database.sql
```

### 3. Iniciar servicios
```bash
.\iniciar-servicios.bat
```
Se abrirán 6 ventanas con cada servicio

### 4. Iniciar orquestador
```bash
.\iniciar-orquestador.bat
```
o ejecutar desde VS Code (F5)

## 📚 Glosario

- **ESB**: Enterprise Service Bus - Patrón de arquitectura de integración
- **JMS**: Java Message Service - API estándar de mensajería en Java
- **ActiveMQ**: Broker de mensajes open source de Apache
- **Cola (Queue)**: Estructura FIFO para almacenar mensajes
- **Producer**: Componente que envía mensajes
- **Consumer**: Componente que recibe mensajes
- **Broker**: Intermediario que gestiona las colas y mensajes
- **Orquestador**: Componente que coordina la comunicación entre servicios
