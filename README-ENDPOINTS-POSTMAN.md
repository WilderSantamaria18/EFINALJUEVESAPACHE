# 📡 Endpoints ESB - Pruebas con Postman

## 🚀 Configuración Rápida

**URL Base:** `http://localhost:8091`
**Header:** `Content-Type: application/json`
**Método:** `POST` (para todos)

---

## 1️⃣ Endpoint: Venta Completa

**URL:** `POST http://localhost:8091/venta-completa`

**Descripción:** Flujo completo que integra RENIEC + PRODUCTO + INVENTARIO + VENTA

### ✅ Prueba Exitosa

```json
{
    "dniCliente": "12345678",
    "codigoProducto": "IPHONE14",
    "cantidad": 2
}
```

### ❌ Prueba Error - DNI Inválido

```json
{
    "dniCliente": "123",
    "codigoProducto": "IPHONE14",
    "cantidad": 1
}
```

### ❌ Prueba Error - Stock Insuficiente

```json
{
    "dniCliente": "12345678",
    "codigoProducto": "IPHONE14",
    "cantidad": 999999
}
```

---

## 2️⃣ Endpoint: Validar Cliente

**URL:** `POST http://localhost:8091/validar-cliente`

**Descripción:** Valida DNI con servicio RENIEC

### ✅ Prueba Exitosa

```json
{
    "dni": "12345678"
}
```

### ❌ Prueba Error

```json
{
    "dni": "123"
}
```

---

## 3️⃣ Endpoint: Consultar Producto

**URL:** `POST http://localhost:8091/consultar-producto`

**Descripción:** Consulta producto y verifica stock disponible

### ✅ Prueba Exitosa - iPhone 14

```json
{
    "codigo": "IPHONE14",
    "cantidad": 2
}
```

### ✅ Prueba Exitosa - iPhone 15 Pro Max

```json
{
    "codigo": "IPHONE15PM",
    "cantidad": 1
}
```

### ❌ Prueba Error - Producto No Existe

```json
{
    "codigo": "NOEXISTE",
    "cantidad": 1
}
```

---

## 4️⃣ Endpoint: Verificar Stock

**URL:** `POST http://localhost:8091/verificar-stock`

**Descripción:** Consulta stock actual en inventario

### ✅ Prueba Exitosa - iPhone 14

```json
{
    "codigo": "IPHONE14"
}
```

### ✅ Prueba Exitosa - AirPods

```json
{
    "codigo": "AIRPODS"
}
```

### ❌ Prueba Error

```json
{
    "codigo": "NOEXISTE"
}
```

---

## 5️⃣ Endpoint: Consultar RUC

**URL:** `POST http://localhost:8091/consultar-ruc`

**Descripción:** Valida RUC de empresa (SUNAT)

### ✅ Prueba Exitosa

```json
{
    "ruc": "20123456789"
}
```

### ❌ Prueba Error - RUC Inválido

```json
{
    "ruc": "123"
}
```

---

## 📋 Checklist de Pruebas

### Endpoint 1: Venta Completa
- [ ] Venta exitosa iPhone 14
- [ ] Error: DNI inválido
- [ ] Error: Stock insuficiente

### Endpoint 2: Validar Cliente
- [ ] DNI válido (8 dígitos)
- [ ] DNI inválido (menos de 8)

### Endpoint 3: Consultar Producto
- [ ] iPhone 14 disponible
- [ ] iPhone 15 Pro Max disponible
- [ ] Producto no existe

### Endpoint 4: Verificar Stock
- [ ] Stock iPhone 14
- [ ] Stock AirPods
- [ ] Producto no en inventario

### Endpoint 5: Consultar RUC
- [ ] RUC válido (11 dígitos)
- [ ] RUC inválido (menos de 11)

---

## 🔧 Pasos para Probar

1. **Inicia ActiveMQ:**
   ```powershell
   cd "D:\apache-activemq-5.17.3\bin"
   .\activemq start
   ```

2. **Inicia Servicios Java:**
   ```powershell
   cd "d:\5  CICLO\APLICACIONES DE SERVICIOS\EFINALMODELADO\EC3"
   .\iniciar-servicios.bat
   ```

3. **Inicia Anypoint Studio:**
   - Run As → Mule Application
   - Espera: "Started app 'iphone-store-esb-jms'"

4. **Abre Postman:**
   - Crea nuevo request
   - Método: POST
   - Copia una URL de arriba
   - Headers: `Content-Type: application/json`
   - Body: raw → JSON (copia un ejemplo)
   - Click **Send**

---

## 🎯 Productos Disponibles

Códigos válidos para probar:
- `IPHONE14` - iPhone 14
- `IPHONE13` - iPhone 13
- `IPHONE15PM` - iPhone 15 Pro Max
- `AIRPODS` - AirPods Pro
- `WATCH` - Apple Watch

---

## ⚡ Respuestas Rápidas

**200 OK + exito: true** ✅ = Prueba exitosa
**200 OK + exito: false** ⚠️ = Error de negocio (DNI inválido, sin stock, etc.)
**500 Internal Server Error** ❌ = Error técnico (servicio caído, timeout, etc.)

---

**🚀 ¡Listo para probar!**
