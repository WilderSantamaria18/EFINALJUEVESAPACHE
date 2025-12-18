# 🍎 IPHONE STORE - Sistema ESB

## Proyecto de Servicios Web con Lógica de Negocio

---

## 📋 ¿QUÉ HACE ESTE PROYECTO?

Este proyecto es un **sistema de gestión de tienda de iPhones** que implementa una arquitectura **ESB (Enterprise Service Bus)** usando **Apache ActiveMQ** y **JMS (Java Message Service)**.

### Funcionalidad Principal

El sistema permite:

- ✅ **Validar clientes** consultando RENIEC (registro de personas)
- ✅ **Validar empresas** consultando RUC (registro de empresas)
- ✅ **Consultar productos** con cálculo de descuentos e IGV
- ✅ **Gestionar inventario** con alertas de stock bajo
- ✅ **Procesar ventas** con validación completa
- ✅ **Consultar empleados** con cálculo de bonificaciones

---

## 🏗️ ARQUITECTURA DEL SISTEMA

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTE                                 │
│                    (Consola / Bonita)                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ORQUESTADOR ESB                              │
│              (Coordina todos los servicios)                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   APACHE ACTIVEMQ                               │
│              (Broker de mensajes JMS)                           │
│                                                                 │
│   Colas: RENIEC, RUC, PRODUCTO, VENTA, INVENTARIO, EMPLEADO    │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
   ┌────────────┐      ┌────────────┐      ┌────────────┐
   │  RENIEC    │      │    RUC     │      │  PRODUCTO  │
   │  Service   │      │  Service   │      │  Service   │
   └────────────┘      └────────────┘      └────────────┘
   ┌────────────┐      ┌────────────┐      ┌────────────┐
   │ INVENTARIO │      │   VENTA    │      │  EMPLEADO  │
   │  Service   │      │  Service   │      │  Service   │
   └────────────┘      └────────────┘      └────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BASE DE DATOS MySQL                          │
│                     (iphone_store_ESB)                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✅ PUNTO 1: LOS 6 SERVICIOS WEB CON LÓGICA DE NEGOCIO

### Requisitos del Punto 1:

- ✅ **6 servicios mínimo**
- ✅ **Lógica de negocio (procesos)**, no solo CRUD

---

### 🔹 SERVICIO 1: ReniecService (Validación de Personas)

**Ubicación:** `src/main/java/com/iphone/store/services/ReniecService.java`

**Lógica de Negocio Implementada:**
| Proceso | Descripción |
|---------|-------------|
| Validación de DNI | Verifica que tenga 8 dígitos numéricos |
| Cálculo de Edad | Calcula automáticamente la edad basada en fecha de nacimiento |
| Verificación Mayoría de Edad | Determina si es mayor de 18 años |
| Clasificación por Grupo Etario | MENOR_DE_EDAD, ADULTO_JOVEN, ADULTO, ADULTO_MAYOR |

**Entrada:** DNI (8 dígitos)

```
12345678
```

**Salida JSON:**

```json
{
  "exito": true,
  "dni": "12345678",
  "nombreCompleto": "Juan Carlos Perez Lopez",
  "fechaNacimiento": "1985-03-15",
  "edad": 39,
  "esMayorEdad": true,
  "estadoCivil": "ADULTO"
}
```

---

### 🔹 SERVICIO 2: RucService (Validación de Empresas)

**Ubicación:** `src/main/java/com/iphone/store/services/RucService.java`

**Lógica de Negocio Implementada:**
| Proceso | Descripción |
|---------|-------------|
| Validación de RUC | Verifica 11 dígitos y prefijo válido (10, 20, 15, 17) |
| Tipo de Contribuyente | Determina si es Persona Natural, Jurídica o Gobierno |
| Nivel de Riesgo | Calcula ALTO, MEDIO o BAJO según estado y datos |
| Verificación de Facturación | Determina si puede emitir facturas |

**Entrada:** RUC (11 dígitos)

```
20123456789
```

**Salida JSON:**

```json
{
  "exito": true,
  "ruc": "20123456789",
  "razonSocial": "COMERCIAL TECH SAC",
  "tipoContribuyente": "PERSONA_JURIDICA",
  "esActivo": true,
  "puedeFacturar": true,
  "nivelRiesgo": "BAJO"
}
```

---

### 🔹 SERVICIO 3: ProductoService (Gestión de Productos)

**Ubicación:** `src/main/java/com/iphone/store/services/ProductoService.java`

**Lógica de Negocio Implementada:**
| Proceso | Descripción |
|---------|-------------|
| Verificación de Disponibilidad | Compara stock vs cantidad solicitada |
| Descuento por Cantidad | 5% (5-9 unid), 10% (10-19 unid), 15% (20+ unid) |
| Cálculo de IGV | Aplica 18% de IGV al subtotal |
| Estado de Stock | AGOTADO, BAJO, NORMAL, ALTO |

**Entrada:** codigo,cantidad

```
IPHONE14,5
```

**Salida JSON:**

```json
{
  "exito": true,
  "codigo": "IPHONE14",
  "nombre": "iPhone 14 256GB",
  "precioOriginal": 3999.0,
  "stock": 30,
  "cantidadSolicitada": 5,
  "disponible": true,
  "porcentajeDescuento": 5.0,
  "precioConDescuento": 3799.05,
  "subtotal": 18995.25,
  "igv": 3419.15,
  "total": 22414.4,
  "estadoStock": "NORMAL"
}
```

---

### 🔹 SERVICIO 4: InventarioService (Control de Inventario)

**Ubicación:** `src/main/java/com/iphone/store/services/InventarioService.java`

**Lógica de Negocio Implementada:**
| Proceso | Descripción |
|---------|-------------|
| Alerta de Stock Mínimo | Detecta si stock <= 10 unidades |
| Prioridad de Reposición | URGENTE (0), ALTA (1-5), NORMAL (6-10), BAJA (>10) |
| Cálculo de Reposición | Sugiere cantidad para llegar a 50 unidades |
| Valor de Inventario | Calcula valor monetario del stock |
| Alerta de Actualización | Detecta si no se actualizó en >30 días |

**Entrada:** código de producto

```
IPHONE15
```

**Salida JSON:**

```json
{
  "exito": true,
  "codigoProducto": "IPHONE15",
  "cantidad": 20,
  "ubicacion": "ALMACEN-B",
  "diasSinActualizar": 408,
  "requiereReposicion": false,
  "cantidadReposicionSugerida": 30,
  "prioridadReposicion": "BAJA",
  "estadoInventario": "NORMAL",
  "valorInventario": 109980.0,
  "requiereRevision": true,
  "alertaActualizacion": "Inventario sin actualizar por 408 dias"
}
```

---

### 🔹 SERVICIO 5: VentaService (Proceso de Ventas)

**Ubicación:** `src/main/java/com/iphone/store/services/VentaService.java`

**Lógica de Negocio Implementada:**
| Proceso | Descripción |
|---------|-------------|
| Validación de Cliente | Verifica que el DNI exista en RENIEC |
| Verificación de Stock | Confirma disponibilidad del producto |
| Cálculo de Descuentos | Por volumen de compra (3%, 5%, 10%) |
| Registro de Venta | Inserta en base de datos |
| Actualización de Stock | Descuenta del inventario automáticamente |

**Entrada:** dniCliente,codigoProducto,cantidad

```
12345678,IPHONE14,2
```

**Salida JSON:**

```json
{
  "exito": true,
  "idVenta": 4,
  "fecha": "2024-12-14",
  "cliente": "Juan Carlos Perez",
  "dniCliente": "12345678",
  "producto": "iPhone 14 256GB",
  "cantidad": 2,
  "precioUnitario": 3999.0,
  "descuento": 0.0,
  "subtotal": 7998.0,
  "igv": 1439.64,
  "total": 9437.64,
  "estado": "COMPLETADO",
  "mensaje": "Venta registrada exitosamente"
}
```

---

### 🔹 SERVICIO 6: EmpleadoService (Gestión de Personal)

**Ubicación:** `src/main/java/com/iphone/store/services/EmpleadoService.java`

**Lógica de Negocio Implementada:**
| Proceso | Descripción |
|---------|-------------|
| Cálculo de Antigüedad | Años desde fecha de ingreso |
| Bonificación por Cargo | Gerente 20%, Vendedor 10%, Otros 5% |
| Bonificación por Antigüedad | +2% (1 año), +5% (3 años), +10% (5+ años) |
| Aportes | Calcula AFP (13%) y Salud (9%) |
| Elegibilidad para Ascenso | Si tiene 2+ años y no es gerente |
| Nivel de Empleado | PRACTICANTE, JUNIOR, PLENO, SENIOR |

**Entrada:** DNI del empleado

```
87654321
```

**Salida JSON:**

```json
{
  "exito": true,
  "dni": "87654321",
  "nombreCompleto": "Maria Elena Garcia Rojas",
  "cargo": "GERENTE",
  "fechaIngreso": "2022-06-01",
  "antiguedadAnios": 2,
  "salarioBase": 3500.0,
  "porcentajeBonificacion": 25.0,
  "bonificacion": 875.0,
  "salarioTotal": 4375.0,
  "aporteAFP": 568.75,
  "aporteSalud": 393.75,
  "salarioNeto": 3412.5,
  "nivelEmpleado": "JUNIOR",
  "categoriaEmpleado": "B",
  "elegibleAscenso": false
}
```

---

## 🚀 CÓMO COMPILAR Y EJECUTAR

### Prerrequisitos

1. **Java JDK 11+** instalado
2. **Maven** instalado
3. **MySQL** corriendo con la base de datos
4. **Apache ActiveMQ** instalado y corriendo

### Paso 1: Crear la Base de Datos

```sql
-- Ejecutar en MySQL
mysql -u root -p < database.sql
```

O ejecutar manualmente el contenido de `database.sql` en MySQL Workbench.

### Paso 2: Iniciar Apache ActiveMQ

```bash
# Windows
cd C:\activemq\bin
activemq.bat start

# Luego verificar en: http://localhost:8161/admin
# Usuario: admin, Password: admin
```

### Paso 3: Compilar el Proyecto

```bash
# En la carpeta del proyecto
cd "d:\5  CICLO\APLICACIONES DE SERVICIOS\EC3"

# Compilar con Maven
mvn clean compile

# Empaquetar (opcional)
mvn package
```

**Salida esperada:**

```
[INFO] BUILD SUCCESS
[INFO] Compiling 16 source files
```

### Paso 4: Iniciar los 6 Servicios

**Opción A: Usar el script batch**

```bash
.\iniciar-servicios.bat
```

**Opción B: Iniciar manualmente cada servicio**

```bash
# Terminal 1 - RENIEC
java -cp "target/classes;target/lib/*" com.iphone.store.services.ReniecService

# Terminal 2 - RUC
java -cp "target/classes;target/lib/*" com.iphone.store.services.RucService

# Terminal 3 - PRODUCTO
java -cp "target/classes;target/lib/*" com.iphone.store.services.ProductoService

# Terminal 4 - INVENTARIO
java -cp "target/classes;target/lib/*" com.iphone.store.services.InventarioService

# Terminal 5 - VENTA
java -cp "target/classes;target/lib/*" com.iphone.store.services.VentaService

# Terminal 6 - EMPLEADO
java -cp "target/classes;target/lib/*" com.iphone.store.services.EmpleadoService
```

### Paso 5: Iniciar el Orquestador

```bash
# En una nueva terminal
java -cp "target/classes;target/lib/*" com.iphone.store.orchestrator.ESBOrchestrator
```

**O usar el script:**

```bash
.\iniciar-orquestador.bat
```

---

## 🎮 CÓMO USAR EL SISTEMA

Al iniciar el orquestador verás este menú:

```
========================================
   ORQUESTADOR ESB - IPHONE STORE
========================================

======== MENU PRINCIPAL ========
1. Consultar RENIEC (DNI)
2. Consultar RUC (Empresa)
3. Consultar Producto
4. Consultar Venta
5. Consultar Inventario
6. Consultar Empleado
0. Salir
================================
Seleccione una opcion:
```

### Datos de Prueba

| Tipo     | Valor       | Descripción              |
| -------- | ----------- | ------------------------ |
| DNI      | 12345678    | Juan Carlos Perez Lopez  |
| DNI      | 87654321    | Maria Elena Garcia Rojas |
| RUC      | 20123456789 | COMERCIAL TECH SAC       |
| RUC      | 20987654321 | DISTRIBUIDORA PERU EIRL  |
| Producto | IPHONE13    | iPhone 13 128GB          |
| Producto | IPHONE14    | iPhone 14 256GB          |
| Producto | IPHONE15    | iPhone 15 Pro 512GB      |
| Producto | AIRPODS     | AirPods Pro 2da Gen      |

---

## 📁 ESTRUCTURA DEL PROYECTO

```
EC3/
├── pom.xml                         # Configuración Maven
├── database.sql                    # Script de base de datos
├── iniciar-servicios.bat           # Script para iniciar servicios
├── iniciar-orquestador.bat         # Script para iniciar orquestador
├── README.md                       # ESTE ARCHIVO
├── README-PROCESO-BONITA.md        # Documentación punto 2
├── README-FLUJOS-ESB.md            # Documentación punto 3
├── GUIA-ESB.md                     # Guía técnica del ESB
│
└── src/main/java/com/iphone/store/
    │
    ├── config/
    │   └── DatabaseConnection.java # Conexión a MySQL
    │
    ├── model/                      # Modelos de datos
    │   ├── Persona.java
    │   ├── Empresa.java
    │   ├── Producto.java
    │   ├── Inventario.java
    │   ├── Venta.java
    │   └── Empleado.java
    │
    ├── services/                   # LOS 6 SERVICIOS WEB
    │   ├── ReniecService.java      # Servicio 1
    │   ├── RucService.java         # Servicio 2
    │   ├── ProductoService.java    # Servicio 3
    │   ├── InventarioService.java  # Servicio 4
    │   ├── VentaService.java       # Servicio 5
    │   └── EmpleadoService.java    # Servicio 6
    │
    ├── orchestrator/
    │   └── ESBOrchestrator.java    # Orquestador central
    │
    └── flows/                      # Flujos ESB
        ├── FlujoVentaCompleta.java
        └── FlujoExcepcionNegocio.java
```

---

## 📝 RESUMEN DE CUMPLIMIENTO

| Requisito             | Estado | Implementación                                     |
| --------------------- | ------ | -------------------------------------------------- |
| 6 servicios mínimo    | ✅     | RENIEC, RUC, Producto, Inventario, Venta, Empleado |
| Lógica de negocio     | ✅     | Validaciones, cálculos, reglas implementadas       |
| No solo CRUD          | ✅     | Cada servicio tiene procesos complejos             |
| Compila correctamente | ✅     | `mvn clean compile` → BUILD SUCCESS                |

---

## 🔧 TECNOLOGÍAS UTILIZADAS

- **Java 11** - Lenguaje de programación
- **Maven** - Gestión de dependencias
- **Apache ActiveMQ** - Message Broker (JMS)
- **MySQL 8** - Base de datos
- **Gson** - Serialización JSON
- **JMS API** - Mensajería asíncrona
