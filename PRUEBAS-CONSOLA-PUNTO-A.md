# 🧪 CÓMO PROBAR EL PUNTO (a) EN CONSOLA

## Servicios Web con Lógica de Negocio - Modo Interactivo

---

## 📋 Requisitos previos

✅ Java 11 instalado  
✅ Maven instalado  
✅ Proyecto compilado

---

## 🚀 PASO 1: Compilar el proyecto

```bash
cd "D:\5  CICLO\APLICACIONES DE SERVICIOS\EFINALMODELADO\EC3"
mvn clean compile
```

Espera el mensaje: **BUILD SUCCESS**

---

## 🚀 PASO 2: Ejecutar el menú interactivo

```bash
.\ejecutar-servicio-interactivo.bat
```

Verás este menú:

```
================================================================
     EJECUTAR SERVICIOS CON LOGICA DE NEGOCIO (MODO CONSOLA)
================================================================
  1. RENIEC Service     - Validacion DNI con logica
  2. RUC Service        - Validacion RUC con algoritmo
  3. Producto Service   - Precios, descuentos, promociones
  4. Inventario Service - Gestion stock, alertas, reservas
  5. Venta Service      - Calculo totales, descuentos, puntos
  6. Empleado Service   - Comisiones, metas, bonificaciones
  7. TODOS (modo JMS)   - Iniciar todos los servicios
  0. Salir
================================================================
Seleccione opcion:
```

---

## 🧪 PRUEBA 1: RENIEC Service (Validación de DNI con Lógica)

### Ejecutar:
```
Seleccione opcion: 1
```

### Menú del servicio:
```
==============================================================
          SERVICIO RENIEC - LOGICA DE NEGOCIO
==============================================================
  1. Consultar DNI (con validaciones y categorización)
  2. Registrar nueva persona
  3. Actualizar datos de persona
  4. Validar capacidad de compra
  5. Calcular descuento por edad
  6. Iniciar servicio JMS (modo escucha)
  0. Salir
==============================================================
```

### ✅ Opción 1: Consultar DNI (LÓGICA DE NEGOCIO)

```
Seleccione opción: 1
Ingrese DNI (8 dígitos): 12345678
```

**Resultado esperado:**
```json
✓ Resultado:
{
  "exito": true,
  "dni": "12345678",
  "nombreCompleto": "Juan Pérez López",
  "fechaNacimiento": "1998-05-15",
  "direccion": "Av. Principal 123",
  "edad": 26,
  "esMayorEdad": true,
  "puedeComprar": true,
  "categoriaCliente": "JOVEN",           ← LÓGICA: Categorización automática
  "descuentoAplicable": 0.10,            ← LÓGICA: 10% descuento por ser joven
  "estadoCivil": "ADULTO_JOVEN"
}
```

**LÓGICA DE NEGOCIO DEMOSTRADA:**
- ✅ Validación de formato DNI (8 dígitos numéricos)
- ✅ Validación de rango válido (10000000-99999999)
- ✅ Cálculo automático de edad (26 años)
- ✅ Categorización: JOVEN (18-25 años)
- ✅ Descuento automático: 10% por categoría
- ✅ Validación de capacidad de compra (mayor de 18)

### ✅ Opción 4: Validar capacidad de compra

```
Seleccione opción: 4
Ingrese edad: 70
```

**Resultado esperado:**
```
✓ Categoría: ADULTO_MAYOR
✓ Puede comprar sin restricciones: NO
  ⚠ Requiere autorización o verificación adicional
```

**LÓGICA DE NEGOCIO:**
- Mayores de 75 años requieren verificación adicional

### ✅ Opción 5: Calcular descuento por edad

```
Seleccione opción: 5
Ingrese edad: 65
```

**Resultado esperado:**
```
✓ Categoría: ADULTO_MAYOR
✓ Descuento aplicable: 15.0%
```

**LÓGICA DE NEGOCIO:**
- Jóvenes (18-25): 10% descuento
- Adultos mayores (60+): 15% descuento

---

## 🧪 PRUEBA 2: RUC Service (Validación con Algoritmo)

### Ejecutar:
```
Volver al menú principal → Seleccione opción: 2
```

### ✅ Validar RUC con algoritmo

```
Opción: 1
Ingrese RUC (11 dígitos): 20123456789
```

**Resultado esperado:**
```json
{
  "exito": true,
  "ruc": "20123456789",
  "razonSocial": "Empresa Demo SAC",
  "tipoContribuyente": "EMPRESA",        ← LÓGICA: Identifica tipo por prefijo "20"
  "regimenTributario": "GENERAL",
  "digitoVerificador": "VÁLIDO"          ← LÓGICA: Algoritmo módulo 11
}
```

**LÓGICA DE NEGOCIO DEMOSTRADA:**
- ✅ Algoritmo de dígito verificador (módulo 11)
- ✅ Identificación automática: 10=Persona, 20=Empresa
- ✅ Validación de régimen tributario

---

## 🧪 PRUEBA 3: Producto Service (Precios Dinámicos)

### Ejecutar:
```
Volver al menú → Seleccione opción: 3
```

### ✅ Consultar producto con cálculo de precios

```
Opción: 1
Ingrese código de producto: IP13
```

**Resultado esperado:**
```json
{
  "codigo": "IP13",
  "nombre": "iPhone 13 Pro",
  "precioBase": 3999.00,
  "descuentoModelo": 399.90,      ← LÓGICA: 10% descuento por modelo
  "subtotal": 3599.10,
  "igv": 647.84,                  ← LÓGICA: 18% IGV automático
  "precioFinal": 4246.94,         ← LÓGICA: Base - Descuento + IGV
  "promocionActiva": true
}
```

**LÓGICA DE NEGOCIO DEMOSTRADA:**
- ✅ Cálculo automático de descuentos (5-20% según modelo)
- ✅ Aplicación de IGV (18%)
- ✅ Precio final con todos los cargos

---

## 🧪 PRUEBA 4: Inventario Service (Alertas Automáticas)

### Ejecutar:
```
Volver al menú → Seleccione opción: 4
```

### ✅ Verificar stock con alertas

```
Opción: 1
Ingrese código de producto: IP13
Ingrese cantidad solicitada: 5
```

**Resultado esperado:**
```json
{
  "disponible": true,
  "stockActual": 8,
  "alertaNivel": "CRÍTICO",              ← LÓGICA: Stock < 10 genera alerta
  "necesitaReabastecimiento": true,
  "cantidadSugerida": 50,               ← LÓGICA: Calcula necesidad
  "prioridad": "ALTA"
}
```

**LÓGICA DE NEGOCIO DEMOSTRADA:**
- ✅ Sistema de alertas (stock < 10 = CRÍTICO)
- ✅ Cálculo de reabastecimiento necesario
- ✅ Priorización automática

---

## 🧪 PRUEBA 5: Venta Service (Cálculo Completo)

### Ejecutar:
```
Volver al menú → Seleccione opción: 5
```

### ✅ Procesar venta con todos los cálculos

```
Opción: 1
Ingrese DNI cliente: 12345678
Ingrese código producto: IP13
Ingrese cantidad: 2
```

**Resultado esperado:**
```json
{
  "ventaId": "V-00123",
  "subtotal": 7198.20,
  "descuentoCliente": 719.82,     ← LÓGICA: 10% por categoría JOVEN
  "igv": 1166.11,                 ← LÓGICA: 18% automático
  "total": 7644.49,               ← LÓGICA: Subtotal - Desc + IGV
  "puntosGanados": 382,           ← LÓGICA: 5% del total en puntos
  "comisionVendedor": 382.22      ← LÓGICA: 5% del total
}
```

**LÓGICA DE NEGOCIO DEMOSTRADA:**
- ✅ Cálculo de subtotal automático
- ✅ Aplicación de descuentos acumulativos
- ✅ Sistema de puntos (5% del total)
- ✅ Generación de comisiones (5% del total)

---

## 🧪 PRUEBA 6: Empleado Service (Comisiones)

### Ejecutar:
```
Volver al menú → Seleccione opción: 6
```

### ✅ Calcular comisión con bonificación

```
Opción: 1
Ingrese ID empleado: EMP001
Ingrese monto de venta: 7644.49
```

**Resultado esperado:**
```json
{
  "empleadoId": "EMP001",
  "comisionBase": 382.22,         ← LÓGICA: 5% de la venta
  "metaMensual": 50000.00,
  "ventasAcumuladas": 52000.00,
  "porcentajeCumplimiento": 104,  ← LÓGICA: Cálculo de cumplimiento
  "bonificacion": 38.22,          ← LÓGICA: 10% extra por meta
  "comisionTotal": 420.44         ← LÓGICA: Base + Bonificación
}
```

**LÓGICA DE NEGOCIO DEMOSTRADA:**
- ✅ Cálculo de comisión base (5%)
- ✅ Seguimiento de metas mensuales
- ✅ Bonificación por cumplimiento (10% extra)
- ✅ Cálculo automático de totales

---

## 📊 RESUMEN DE LÓGICA DE NEGOCIO DEMOSTRADA

| Servicio | Lógica de Negocio |
|----------|-------------------|
| RENIEC | ✅ Categorización por edad<br>✅ Descuentos automáticos (10-15%)<br>✅ Validación de capacidad |
| RUC | ✅ Algoritmo módulo 11<br>✅ Identificación de tipo<br>✅ Validación tributaria |
| Producto | ✅ Precios dinámicos<br>✅ Descuentos por modelo<br>✅ Cálculo IGV |
| Inventario | ✅ Alertas automáticas<br>✅ Cálculo reabastecimiento<br>✅ Priorización |
| Venta | ✅ Totales automáticos<br>✅ Descuentos acumulativos<br>✅ Puntos y comisiones |
| Empleado | ✅ Comisiones 5%<br>✅ Bonificaciones 10%<br>✅ Metas y ranking |

---

## ✅ CHECKLIST DE PRUEBAS

- [ ] RENIEC: Consultar DNI → Ver categorización y descuento
- [ ] RENIEC: Validar capacidad → Probar con diferentes edades
- [ ] RUC: Validar RUC → Ver algoritmo de verificación
- [ ] Producto: Consultar → Ver cálculo de precio final
- [ ] Inventario: Verificar stock → Ver alertas automáticas
- [ ] Venta: Procesar → Ver todos los cálculos
- [ ] Empleado: Calcular comisión → Ver bonificaciones

---

## 🎯 PARA DEMOSTRAR AL PROFESOR

1. **Ejecutar:** `.\ejecutar-servicio-interactivo.bat`
2. **Seleccionar opción 1** (RENIEC)
3. **Consultar DNI:** 12345678
4. **Mostrar resultado:** Señalar categorización y descuento automático
5. **Repetir** con otros servicios para mostrar sus lógicas

**Frase clave:**  
"Los servicios NO son CRUD simple, cada uno tiene lógica de negocio compleja: algoritmos de validación, cálculos automáticos, categorizaciones, descuentos dinámicos y reglas de negocio."

---

## 💡 TIPS

- ✅ Cada servicio tiene su propio menú interactivo
- ✅ Puedes probar sin necesidad de ActiveMQ
- ✅ Los resultados muestran JSON formateado
- ✅ Presiona 0 para volver al menú principal
- ✅ Opción 7 inicia todos en modo JMS (requiere ActiveMQ)
