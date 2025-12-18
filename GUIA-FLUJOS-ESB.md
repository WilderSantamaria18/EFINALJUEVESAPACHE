# GUÍA DE EJECUCIÓN - FLUJOS ESB

## 📋 Resumen del Proyecto

Este proyecto implementa **2 flujos ESB** que integran 6 servicios web RESTful/SOAP con un proceso de negocio modelado en Bonita.

### ✅ Lo que YA tienes completo:

1. **6 Servicios Web** (punto a):
   - ReniecService.java (validación RENIEC)
   - RucService.java (validación SUNAT)
   - ProductoService.java (consulta productos)
   - InventarioService.java (verificar stock)
   - VentaService.java (registrar ventas)
   - EmpleadoService.java (validar empleados)

2. **Proceso de Negocio en Bonita** (punto b):
   - 25 tareas
   - 2 gateways AND (paralelos)
   - 1 inicio, 3 finales
   - Formularios con contratos/variables
   - ✅ Sin errores, listo para ejecutar

---

## 🚀 Flujos ESB (punto c)

### **Flujo 1: FlujoVentaCompleta.java**
**Integra los servicios (puntos a + b)**

Orquesta los 6 servicios en un flujo completo:
```
Cliente → RENIEC → Producto → Inventario → RUC → Venta → Comprobante
```

**Características:**
- Validación de cliente (RENIEC)
- Verificación de stock (Inventario)
- Validación de empresa (RUC)
- Registro de venta (Venta)
- Cálculo de precios e IGV
- Respuesta JSON estructurada

**Archivo:** `src/main/java/com/iphone/store/flows/FlujoVentaCompleta.java`

---

### **Flujo 2: FlujoExcepcionNegocio.java**
**Manejo de excepciones de negocio y técnicas**

Gestiona errores y usa **colas JMS/ActiveMQ**:

**Excepciones de Negocio:**
- Validación de DNI (formato, longitud)
- Validación de stock (disponibilidad)
- Validación de edad (mayor de edad)
- Validación de RUC (formato)

**Excepciones Técnicas:**
- Timeout de servicios
- Servicios no disponibles
- Errores de conexión
- Reintentos automáticos (máx. 3)

**Uso de Colas:**
- `ERROR.QUEUE` → errores de negocio
- `DEADLETTER.QUEUE` → errores técnicos irrecuperables
- Mensajes persistentes
- Reintentos automáticos

**Archivo:** `src/main/java/com/iphone/store/flows/FlujoExcepcionNegocio.java`

---

## 🔧 Pasos para Ejecutar

### 1. Iniciar ActiveMQ

**Opción A - Docker (recomendado, más rápido):**
```bash
docker run -d --name activemq -p 61616:61616 -p 8161:8161 rmohr/activemq
```

**Opción B - Ejecutar script incluido:**
```cmd
cd EC3
iniciar-activemq.bat
```

**Verificar:**
- Consola web: http://localhost:8161/admin
- Usuario: `admin` / Password: `admin`

---

### 2. Compilar el proyecto

```bash
cd EC3
mvn clean compile
```

---

### 3. Ejecutar los flujos ESB

```bash
mvn exec:java -Dexec.mainClass="com.iphone.store.Main"
```

**Menú interactivo:**
```
═══════════════ MENÚ PRINCIPAL ═══════════════
1. Ejecutar Flujo de Venta Completa (Integra servicios)
2. Ejecutar Flujo con Manejo de Excepciones
3. Demostrar Colas JMS/ActiveMQ
4. Verificar estado de ActiveMQ
0. Salir
```

---

### 4. Ejecutar el proceso en Bonita

1. Abrir Bonita Studio
2. Abrir diagrama: `MiDiagrama-1.0.proc`
3. Click **Run** (o F6)
4. Crear un nuevo caso
5. Completar los formularios hasta llegar a "Fin"

---

## 📊 Demostración de los Flujos

### Flujo 1 - Venta Completa

**Entrada:**
```
DNI: 12345678
Código Producto: IP13
Cantidad: 2
```

**Proceso:**
1. Valida cliente en RENIEC → ✓ Mayor de edad
2. Consulta producto → ✓ iPhone 13, S/3500
3. Verifica stock → ✓ Disponible
4. Calcula totales:
   - Subtotal: S/7000
   - IGV (18%): S/1260
   - Total: S/8260
5. Registra venta en BD
6. Genera comprobante

**Salida JSON:**
```json
{
  "exito": true,
  "ventaId": 123,
  "cliente": "Juan Perez",
  "producto": "iPhone 13",
  "cantidad": 2,
  "total": 8260.00,
  "mensaje": "Venta registrada exitosamente"
}
```

---

### Flujo 2 - Manejo de Excepciones

#### Caso 1: Excepción de Negocio (DNI inválido)

**Entrada:** `DNI: 123` (menos de 8 dígitos)

**Salida:**
```json
{
  "exito": false,
  "tipoError": "NEGOCIO",
  "codigo": "ERR_DNI_001",
  "mensaje": "DNI inválido: debe tener exactamente 8 dígitos"
}
```

**Cola JMS:** Mensaje enviado a `ERROR.QUEUE` para auditoría

---

#### Caso 2: Excepción Técnica (Servicio no disponible)

**Entrada:** Llamar servicio que no responde

**Proceso:**
1. Intento 1 → Timeout
2. Intento 2 → Timeout
3. Intento 3 → Timeout
4. Enviar a Dead Letter Queue

**Salida:**
```json
{
  "exito": false,
  "tipoError": "TECNICO",
  "codigo": "ERR_TIMEOUT",
  "mensaje": "Servicio no disponible después de 3 intentos",
  "intentos": 3
}
```

**Cola JMS:** Mensaje enviado a `DEADLETTER.QUEUE`

---

## 📁 Estructura del Proyecto

```
EC3/
├── src/main/java/com/iphone/store/
│   ├── Main.java                      ← PUNTO DE ENTRADA
│   ├── orchestrator/
│   │   └── ESBOrchestrator.java       ← Orquestador JMS
│   ├── flows/
│   │   ├── FlujoVentaCompleta.java    ← FLUJO 1 (integración)
│   │   └── FlujoExcepcionNegocio.java ← FLUJO 2 (excepciones + JMS)
│   ├── services/
│   │   ├── ReniecService.java
│   │   ├── RucService.java
│   │   ├── ProductoService.java
│   │   ├── InventarioService.java
│   │   ├── VentaService.java
│   │   └── EmpleadoService.java
│   ├── model/
│   │   ├── Persona.java
│   │   ├── Empresa.java
│   │   ├── Producto.java
│   │   ├── Inventario.java
│   │   ├── Venta.java
│   │   └── Empleado.java
│   └── config/
│       └── DatabaseConnection.java
├── iniciar-servicios.bat              ← Iniciar servicios REST
├── iniciar-activemq.bat               ← Iniciar ActiveMQ
└── pom.xml                            ← Dependencias Maven
```

---

## 🔍 Verificación de Colas en ActiveMQ

1. Abrir consola: http://localhost:8161/admin
2. Ir a **Queues**
3. Deberías ver:
   - `RENIEC.REQUEST` / `RENIEC.RESPONSE`
   - `ERROR.QUEUE` (mensajes de errores de negocio)
   - `DEADLETTER.QUEUE` (mensajes fallidos)
4. Click en una cola para ver mensajes pendientes

---

## ✅ Entregables Completos

### Punto (a) - 6 Servicios Web ✓
- ReniecService, RucService, ProductoService, InventarioService, VentaService, EmpleadoService
- Implementan lógica de negocio (validaciones, cálculos, consultas BD)
- REST con JSON

### Punto (b) - Proceso de Negocio ✓
- Bonita: 25 tareas, 2 gateways, 3 finales
- Formularios con variables/contratos
- Flujo básico + alternos

### Punto (c) - Flujos ESB ✓
- **Flujo 1:** Integra servicios a + b (FlujoVentaCompleta)
- **Flujo 2:** Manejo de excepciones (FlujoExcepcionNegocio)
- **Colas JMS:** ActiveMQ con ERROR.QUEUE y DEADLETTER.QUEUE

---

## 🎯 Demostración para el Profesor

1. **Mostrar ActiveMQ corriendo:**
   ```
   http://localhost:8161/admin → Queues tab
   ```

2. **Ejecutar Flujo 1 (Venta Completa):**
   ```bash
   mvn exec:java -Dexec.mainClass="com.iphone.store.Main"
   → Opción 1 → Ingresar datos
   ```

3. **Ejecutar Flujo 2 (Excepciones):**
   ```bash
   → Opción 2 → Ver errores capturados
   ```

4. **Mostrar colas con mensajes:**
   ```
   ActiveMQ → ERROR.QUEUE → Ver mensajes de errores de negocio
   ```

5. **Ejecutar proceso Bonita:**
   ```
   Bonita Studio → Run → Completar formularios hasta Fin
   ```

---

## 📝 Notas Importantes

- **ActiveMQ debe estar corriendo ANTES de ejecutar Main.java**
- Los servicios REST pueden ser simulados (no necesitan estar levantados para la demo)
- El flujo de excepciones funciona **sin servicios externos** (todo en memoria/colas)
- Bonita es independiente de los flujos ESB (puede demostrarse por separado)

---

## 🆘 Resolución de Problemas

**Error: "No se pudo conectar a ActiveMQ"**
```bash
# Verificar que ActiveMQ esté corriendo
netstat -an | findstr :61616

# Reiniciar ActiveMQ
docker restart activemq
```

**Error: "ERROR.QUEUE no existe"**
→ Las colas se crean automáticamente al enviar el primer mensaje. Normal.

**Error: "No se puede compilar"**
```bash
mvn clean install -U
```

---

## 🎓 Resumen para la Defensa

> "Implementé 2 flujos ESB en Java:
> 
> **Flujo 1** orquesta los 6 servicios REST (RENIEC, RUC, Producto, Inventario, Venta, Empleado) en un proceso de venta completa que valida cliente, verifica stock, calcula totales y registra la venta.
> 
> **Flujo 2** maneja excepciones de negocio (DNI inválido, sin stock) y técnicas (timeouts, servicios caídos) usando colas JMS con ActiveMQ. Los errores se envían a ERROR.QUEUE (negocio) y DEADLETTER.QUEUE (técnicas), con reintentos automáticos hasta 3 intentos.
> 
> Ambos flujos están integrados con el proceso de negocio modelado en Bonita que tiene 25 tareas, 2 gateways y múltiples eventos. El sistema usa ActiveMQ como ESB para comunicación asíncrona entre servicios."

---

✅ **TODO ESTÁ LISTO PARA EJECUTAR**
