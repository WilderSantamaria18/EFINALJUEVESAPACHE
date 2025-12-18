# 🚀 INICIO RÁPIDO - ANYPOINT STUDIO

## Opción A: Importar XMLs directamente

### 1. Crear proyecto vacío
```
File → New → Mule Project
Project Name: iphone-store-integration-flow
Runtime: Mule Server 4.x
Finish
```

### 2. Copiar XML
1. Abrir `src/main/mule/iphone-store-integration-flow.xml`
2. Borrar todo el contenido
3. Copiar todo el contenido de `anypoint-templates/iphone-store-integration-flow.xml`
4. Pegar y guardar

### 3. Verificar dependencias en pom.xml
Asegurar que existan:
```xml
<dependency>
    <groupId>org.mule.connectors</groupId>
    <artifactId>mule-http-connector</artifactId>
    <version>1.7.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### 4. Repetir para el segundo flujo
```
Project Name: iphone-store-exception-flow
XML: anypoint-templates/iphone-store-exception-flow.xml
```

Agregar dependencia JMS:
```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule-jms-connector</artifactId>
    <version>1.8.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

---

## Opción B: Construir visualmente (recomendado para aprender)

Seguir la guía completa en `GUIA-ANYPOINT-STUDIO.md`

---

## ⚙️ Configuración rápida

### 1. Iniciar ActiveMQ
```powershell
docker run -d --name activemq -p 61616:61616 -p 8161:8161 rmohr/activemq
```

### 2. Iniciar tus servicios Java
```powershell
cd "D:\5  CICLO\APLICACIONES DE SERVICIOS\EFINALMODELADO\EC3"
.\iniciar-servicios.bat
```

### 3. Ejecutar flujos Anypoint
- Click derecho en el proyecto
- Run As → Mule Application
- Esperar "DEPLOYED" en consola

---

## 🧪 Probar

### Flujo 1 - Integración
```bash
curl -X POST http://localhost:8091/venta-completa ^
  -H "Content-Type: application/json" ^
  -d "{\"dniCliente\":\"12345678\",\"codigoProducto\":\"IP13\",\"cantidad\":2}"
```

### Flujo 2 - Excepciones
```bash
# Error de negocio (DNI inválido → ERROR.QUEUE)
curl -X POST http://localhost:8092/procesar-con-validacion ^
  -H "Content-Type: application/json" ^
  -d "{\"dni\":\"123\",\"codigoProducto\":\"IP13\",\"cantidad\":2}"

# Procesamiento OK
curl -X POST http://localhost:8092/procesar-con-validacion ^
  -H "Content-Type: application/json" ^
  -d "{\"dni\":\"12345678\",\"codigoProducto\":\"IP13\",\"cantidad\":2}"
```

### Verificar colas
- Abrir: http://localhost:8161/admin (admin/admin)
- Queues → ERROR.QUEUE y DEADLETTER.QUEUE

---

## ✅ Entregables

1. **Screenshot** del flujo 1 en Anypoint Studio (vista visual)
2. **Screenshot** del flujo 2 en Anypoint Studio
3. **Screenshot** de ActiveMQ mostrando las colas con mensajes
4. **Video/GIF** ejecutando curl y viendo resultado
5. **Exportar proyectos**: Right-click → Export → Anypoint Studio Project to Mule Deployable Archive

---

## 📊 Comparación con tu implementación Java

| Característica | Java + ActiveMQ | Anypoint Studio |
|----------------|-----------------|-----------------|
| Orquestación | ✅ Código Java | ✅ Visual + XML |
| JMS/Colas | ✅ ActiveMQ | ✅ ActiveMQ |
| Servicios REST | ✅ HTTP client | ✅ HTTP connector |
| Reintentos | ✅ Manual | ✅ Built-in |
| Transformaciones | ✅ Gson | ✅ DataWeave |
| Monitoreo | ⚠️ Logs | ✅ Anypoint Monitoring |

**Ambas soluciones son válidas y profesionales.**

---

## 🎯 Tips para la presentación

"Implementé los flujos ESB usando Anypoint Studio, la plataforma líder de MuleSoft:

1. **Flujo de Integración**: Orquesta los 6 servicios REST usando HTTP connectors y DataWeave para transformaciones

2. **Flujo de Excepciones**: Maneja errores de negocio y técnicos enviándolos a colas JMS diferentes:
   - ERROR.QUEUE para validaciones de negocio
   - DEADLETTER.QUEUE para fallos técnicos con 3 reintentos

3. **Ventajas de Anypoint**:
   - Diseño visual del flujo
   - Monitoreo en tiempo real
   - Reintentos automáticos
   - DataWeave para transformaciones complejas
   - Fácil despliegue en CloudHub"

---

## 🆘 Solución de problemas

### Error: "Cannot resolve dependency"
→ Right-click proyecto → Mule → Update Project Dependencies

### Error: "Port 8091 already in use"
→ Cambiar puerto en HTTP Listener config

### ActiveMQ no conecta
→ Verificar: `docker ps` y que broker URL sea `tcp://localhost:61616`

### Servicios Java no responden
→ Iniciar con `iniciar-servicios.bat` primero
