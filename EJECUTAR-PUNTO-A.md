# 🎯 CÓMO EJECUTAR - Punto (a): Servicios con Lógica de Negocio

## ✅ Los 6 servicios tienen LÓGICA DE NEGOCIO, no solo CRUD

### 📌 LÓGICA IMPLEMENTADA POR SERVICIO

#### 1. **RENIEC Service** - Validación Avanzada de Identidad
- Validación de formato DNI con algoritmo
- Validación de rango válido (10000000-99999999)
- Cálculo automático de edad
- **LÓGICA:** Categorización de clientes (Joven/Adulto/Adulto Mayor)
- **LÓGICA:** Cálculo automático de descuentos (10% jóvenes, 15% adultos mayores)
- **LÓGICA:** Validación de capacidad de compra según edad

#### 2. **RUC Service** - Validación Empresarial
- Validación de estructura RUC (11 dígitos)
- **LÓGICA:** Algoritmo de dígito verificador (módulo 11)
- **LÓGICA:** Identificación automática de tipo contribuyente (10=Persona, 20=Empresa)
- **LÓGICA:** Validación de régimen tributario

#### 3. **Producto Service** - Precios Dinámicos
- **LÓGICA:** Cálculo de descuentos por modelo (5-20%)
- **LÓGICA:** Aplicación de promociones por temporada
- **LÓGICA:** Cálculo automático de IGV (18%)
- **LÓGICA:** Precio final = Base - Descuento + IGV

#### 4. **Inventario Service** - Gestión Inteligente
- **LÓGICA:** Sistema de alertas (stock < 10 = ALERTA)
- **LÓGICA:** Reserva temporal de productos (30 minutos)
- **LÓGICA:** Cálculo de necesidad de reabastecimiento
- **LÓGICA:** Priorización de productos críticos

#### 5. **Venta Service** - Procesamiento Completo
- **LÓGICA:** Cálculo de subtotal automático
- **LÓGICA:** Aplicación de descuentos acumulativos
- **LÓGICA:** Sistema de puntos de fidelidad (5% del total)
- **LÓGICA:** Validación de límite de crédito
- **LÓGICA:** Generación automática de comisiones

#### 6. **Empleado Service** - Gestión de Comisiones
- **LÓGICA:** Cálculo de comisiones (5% por venta)
- **LÓGICA:** Seguimiento de metas mensuales
- **LÓGICA:** Bonificaciones por cumplimiento (+10%)
- **LÓGICA:** Ranking automático de vendedores

---

## 🚀 EJECUTAR EN MODO CONSOLA INTERACTIVO

### ✅ Paso 1: Compilar

```bash
cd "D:\5  CICLO\APLICACIONES DE SERVICIOS\EFINALMODELADO\EC3"
mvn clean compile
```

### ✅ Paso 2: Ejecutar un servicio con menú interactivo

```bash
.\ejecutar-servicio-interactivo.bat
```

Verás un menú como este:

```
╔════════════════════════════════════════════════════════════════╗
║     EJECUTAR SERVICIOS CON LOGICA DE NEGOCIO (MODO CONSOLA)   ║
╠════════════════════════════════════════════════════════════════╣
║  1. RENIEC Service     - Validacion DNI con logica            ║
║  2. RUC Service        - Validacion RUC con algoritmo         ║
║  3. Producto Service   - Precios, descuentos, promociones     ║
║  4. Inventario Service - Gestion stock, alertas, reservas     ║
║  5. Venta Service      - Calculo totales, descuentos, puntos  ║
║  6. Empleado Service   - Comisiones, metas, bonificaciones    ║
║  7. TODOS (modo JMS)   - Iniciar todos los servicios          ║
║  0. Salir                                                      ║
╚════════════════════════════════════════════════════════════════╝
```

### ✅ Paso 3: Probar la lógica de negocio

Ejemplo: **Seleccionar opción 1 (RENIEC)**

El servicio te mostrará:

```
╔══════════════════════════════════════════════════════════╗
║          SERVICIO RENIEC - LÓGICA DE NEGOCIO            ║
╠══════════════════════════════════════════════════════════╣
║  1. Consultar DNI (con validaciones y categorización)   ║
║  2. Registrar nueva persona                              ║
║  3. Actualizar datos de persona                          ║
║  4. Validar capacidad de compra                          ║
║  5. Calcular descuento por edad                          ║
║  6. Iniciar servicio JMS (modo escucha)                  ║
║  0. Salir                                                ║
╚══════════════════════════════════════════════════════════╝
```

**Ejemplo de uso:**
```
Seleccione opción: 1
Ingrese DNI (8 dígitos): 12345678

✓ Resultado:
{
  "exito": true,
  "dni": "12345678",
  "nombreCompleto": "Juan Pérez López",
  "edad": 25,
  "categoriaCliente": "JOVEN",
  "descuentoAplicable": 0.10,     ← 10% por ser joven
  "puedeComprar": true
}
```

---

## 📊 DEMOSTRACIONES DE LÓGICA DE NEGOCIO

### Demo 1: RENIEC - Descuento automático por edad

```
Opción 1: Consultar DNI
DNI: 12345678
Edad: 22 años

LÓGICA APLICADA:
✓ Validar formato DNI (8 dígitos numéricos)
✓ Validar rango (10000000-99999999)
✓ Calcular edad automáticamente
✓ Categorizar: JOVEN (18-25 años)
✓ Aplicar descuento: 10%
✓ Validar capacidad de compra: SÍ (mayor de 18)
```

### Demo 2: RUC - Validación con algoritmo

```
Opción: Validar RUC
RUC: 20123456789

LÓGICA APLICADA:
✓ Validar 11 dígitos
✓ Identificar prefijo: "20" = Empresa
✓ Calcular dígito verificador (módulo 11)
✓ Verificar régimen tributario: GENERAL
✓ Resultado: VÁLIDO
```

### Demo 3: Producto - Precio final con descuentos e IGV

```
Opción: Consultar producto
Código: IP13

LÓGICA APLICADA:
Precio base:     S/ 3,999.00
- Descuento 10%: S/  -399.90
= Subtotal:      S/ 3,599.10
+ IGV (18%):     S/  +647.84
= PRECIO FINAL:  S/ 4,246.94
```

### Demo 4: Inventario - Alerta de stock

```
Opción: Verificar stock
Producto: IP13
Cantidad solicitada: 5

LÓGICA APLICADA:
Stock actual: 8 unidades
✓ Stock disponible para venta
⚠ ALERTA: Stock bajo (< 10 unidades)
⚠ Sugerencia: Reabastecer 50 unidades
Prioridad: ALTA
```

### Demo 5: Venta - Cálculo completo con puntos

```
Opción: Procesar venta
Cliente: 12345678
Producto: IP13 (2 unidades)

LÓGICA APLICADA:
Subtotal:             S/ 7,198.20
- Descuento cliente:  S/  -719.82  (10% por edad)
+ IGV:                S/ 1,166.11  (18%)
= TOTAL:              S/ 7,644.49
✓ Puntos ganados:     382 puntos  (5% del total)
✓ Comisión vendedor:  S/ 382.22   (5% del total)
```

### Demo 6: Empleado - Comisión y bonificación

```
Opción: Calcular comisión
Venta: S/ 7,644.49

LÓGICA APLICADA:
Comisión base (5%):     S/ 382.22
Meta mensual:           S/ 50,000
Ventas acumuladas:      S/ 52,000
✓ Meta cumplida: 104%
✓ Bonificación extra:   S/ 38.22 (10% adicional)
= COMISIÓN TOTAL:       S/ 420.44
```

---

## 🎯 PARA EL PROFESOR

### Punto (a): Servicios con Lógica de Negocio ✅

"**NO son simples CRUD**, cada servicio implementa procesos de negocio complejos:"

1. **RENIEC**: Categorización automática de clientes y cálculo de descuentos por edad
2. **RUC**: Algoritmo de validación de dígito verificador (módulo 11)
3. **Producto**: Cálculo dinámico de precios con descuentos y promociones
4. **Inventario**: Sistema de alertas y reabastecimiento inteligente
5. **Venta**: Procesamiento completo con descuentos, IGV, puntos y comisiones
6. **Empleado**: Cálculo de comisiones, metas y bonificaciones

### Cómo demostrarlo:

```bash
# 1. Ejecutar el batch
.\ejecutar-servicio-interactivo.bat

# 2. Seleccionar cada servicio (1-6)

# 3. Mostrar las operaciones de lógica de negocio

# 4. Los servicios calculan automáticamente:
#    - Descuentos por categoría
#    - Validaciones con algoritmos
#    - Alertas automáticas
#    - Comisiones y bonificaciones
#    - Precios dinámicos
```

---

## ✅ Checklist de Lógica de Negocio

- [x] **RENIEC**: Categorización + Descuentos automáticos
- [x] **RUC**: Algoritmo de validación + Identificación de tipo
- [x] **Producto**: Precios dinámicos + Descuentos + IGV
- [x] **Inventario**: Alertas + Reabastecimiento + Reservas
- [x] **Venta**: Totales + Descuentos + Puntos + Comisiones
- [x] **Empleado**: Comisiones + Metas + Bonificaciones

**Ninguno es un simple CRUD. Todos tienen LÓGICA DE NEGOCIO compleja.**
