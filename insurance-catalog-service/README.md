# Insurance Catalog Service

Un servicio REST reactivo para gestionar y consultar un catálogo de planes de seguros. Construido con **Spring Boot 3.5.10**, **WebFlux** y **R2DBC** para proporcionar operaciones asincrónicas y no bloqueantes.

---

## 📋 Descripción

**Insurance Catalog Service** es una aplicación backend que expone endpoints reactivos para consultar planes de seguros disponibles. Utiliza:

- **Spring WebFlux**: Para crear endpoints reactivos con soporte a Server-Sent Events (SSE)
- **R2DBC**: Para acceso reactivo a la base de datos
- **H2 Database**: Base de datos embebida en memoria
- **Project Reactor**: Para programación reactiva con `Flux` y `Mono`
- **Lombok**: Para reducir código boilerplate
- **Spring Validation**: Para validación de datos

---

## 🏗️ Arquitectura

```
Controller Layer
    ↓
Service Layer (Interface + Implementation)
    ↓
Repository Layer (R2DBC)
    ↓
Database (H2 In-Memory)
```

### Estructura de directorios

```
src/main/java/com/challenge/insurance_catalog_service/
├── controller/           # Endpoints REST
├── service/              # Lógica de negocio
│   ├── InsuranceCatalogService.java (interfaz)
│   └── impl/
│       └── InsuranceCatalogServiceImpl.java
├── dto/                  # Data Transfer Objects
├── entity/               # Entidades de dominio
├── repository/           # Acceso a datos (R2DBC)
├── exception/            # Manejo de excepciones
└── InsuranceCatalogServiceApplication.java
```

---

## 📦 Modelo de Datos

### Tabla: `insuranceplan`

| Campo      | Tipo      | Descripción                                      |
|-----------|----------|------------------------------------------------|
| `id`      | BIGINT   | Identificador único (auto-incrementado)        |
| `name`    | VARCHAR  | Nombre del plan de seguros                     |
| `base_price` | DOUBLE | Precio base del plan                           |
| `min_age` | INT      | Edad mínima permitida                          |
| `max_age` | INT      | Edad máxima permitida                          |
| `active`  | BOOLEAN  | Estado del plan (activo/inactivo)              |

---

## 🚀 Requisitos

- **Java 17** o superior
- **Maven 3.6+** o superior
- **Windows PowerShell** o terminal compatible

---

## ⚙️ Instalación y Ejecución

### 1. Clonar o descargar el repositorio
```bash
cd insurance-catalog-service
```

### 2. Compilar el proyecto
```bash
mvnw clean package
```

### 3. Ejecutar la aplicación
```bash
mvnw spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

---

## 📡 Endpoints

### 1. Obtener todos los planes de seguros

**Descripción:** Retorna todos los planes de seguros disponibles en el catálogo.

- **URL:** `/api/insurance-catalog`
- **Método:** `GET`
- **Tipo de respuesta:** `text/event-stream` (Server-Sent Events)
- **Código de éxito:** `200 OK`

#### Ejemplo con cURL

```bash
curl -X GET http://localhost:8080/api/insurance-catalog
```

#### Ejemplo con PowerShell

```powershell
$uri = "http://localhost:8080/api/insurance-catalog"
$response = Invoke-WebRequest -Uri $uri -Method Get
Write-Host $response.Content
```

#### Ejemplo con JavaScript/Fetch

```javascript
const eventSource = new EventSource('http://localhost:8080/api/insurance-catalog');

eventSource.onmessage = (event) => {
  const plan = JSON.parse(event.data);
  console.log('Plan recibido:', plan);
};

eventSource.onerror = (error) => {
  console.error('Error en la conexión:', error);
  eventSource.close();
};
```

#### Respuesta esperada (cada evento)

```json
{
  "namePlan": "Plan Básico",
  "pricePlan": 150.50,
  "scopeAge": "18-65",
  "active": true
}
```

---

### 2. Obtener planes de seguros activos

**Descripción:** Retorna únicamente los planes de seguros con estado activo.

- **URL:** `/api/insurance-catalog/active`
- **Método:** `GET`
- **Tipo de respuesta:** `text/event-stream` (Server-Sent Events)
- **Código de éxito:** `200 OK`
- **Código de error:** `404 Not Found` (si no hay planes activos)

#### Ejemplo con cURL

```bash
curl -X GET http://localhost:8080/api/insurance-catalog/active
```

#### Ejemplo con PowerShell

```powershell
$uri = "http://localhost:8080/api/insurance-catalog/active"
try {
  $response = Invoke-WebRequest -Uri $uri -Method Get
  Write-Host "Planes activos encontrados:"
  Write-Host $response.Content
} catch {
  Write-Host "Error: No se encontraron planes activos" -ForegroundColor Red
}
```

#### Ejemplo con JavaScript/Fetch

```javascript
const eventSource = new EventSource('http://localhost:8080/api/insurance-catalog/active');

eventSource.onmessage = (event) => {
  const plan = JSON.parse(event.data);
  console.log('Plan activo:', plan);
};

eventSource.onerror = (error) => {
  console.error('Error:', error);
  if (eventSource.readyState === EventSource.CLOSED) {
    console.log('Conexión cerrada. Posiblemente no hay planes activos.');
  }
  eventSource.close();
};
```

#### Respuesta esperada (cada evento)

```json
{
  "id": 1,
  "name": "Plan Premium",
  "basePrice": 350.00,
  "minAge": 18,
  "maxAge": 70,
  "active": true
}
```

---

## 🗄️ Base de Datos

### Datos iniciales (data.sql)

La aplicación carga automáticamente datos de ejemplo en la tabla `insuranceplan`:

```sql
INSERT INTO insuranceplan (name, base_price, min_age, max_age, active)
VALUES
  ('Plan Básico', 150.50, 18, 65, true),
  ('Plan Plus', 250.00, 18, 70, true),
  ('Plan Premium', 350.00, 25, 75, true),
  ('Plan Senior', 200.00, 60, 85, false);
```

### Acceso a la consola H2

Durante el desarrollo, puedes acceder a la consola H2 en:

- **URL:** `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:///insurancedb`
- **Usuario:** `sa`
- **Contraseña:** (vacío)

---

## 🔄 Flujo de Operación Reactivo

```
Cliente solicita /api/insurance-catalog
    ↓
Controller recibe la solicitud
    ↓
Service consulta el repositorio
    ↓
Repository ejecuta query R2DBC
    ↓
Base de datos retorna datos
    ↓
Flux mapea cada InsurancePlan a InsurancePlanResponse
    ↓
SSE envía eventos al cliente progresivamente
    ↓
Cliente recibe eventos en tiempo real
```

---

## 📋 Estructuras clave

### InsurancePlanResponse (DTO)

```java
public record InsurancePlanResponse(
    String namePlan,
    Double pricePlan,
    String scopeAge,      // Formato: "min-max" ej: "18-65"
    Boolean active
)
```

### InsurancePlan (Entity)

```java
public record InsurancePlan(
    Long id,
    String name,
    Double basePrice,
    Integer minAge,
    Integer maxAge,
    Boolean active
)
```

---

## 🛠️ Manejo de Errores

### PlanNotFoundException

Se lanza cuando se solicita planes activos pero no existen.

**Respuesta HTTP 404:**

```json
{
  "errorMessage": "there are not plan active",
  "status": 404,
  "timestamp": "2026-02-14T10:30:00.000+00:00"
}
```

---

## 📊 Ejemplo completo: Consumir ambos endpoints

### PowerShell (Ejemplo integrado)

```powershell
# Obtener todos los planes
Write-Host "=== Todos los planes ===" -ForegroundColor Cyan
Invoke-WebRequest -Uri "http://localhost:8080/api/insurance-catalog" | Select-Object -ExpandProperty Content

# Obtener planes activos
Write-Host "`n=== Planes activos ===" -ForegroundColor Cyan
try {
  Invoke-WebRequest -Uri "http://localhost:8080/api/insurance-catalog/active" | Select-Object -ExpandProperty Content
} catch {
  Write-Host "No hay planes activos o error en la solicitud" -ForegroundColor Yellow
}
```

### cURL (Script de ejemplo)

```bash
#!/bin/bash

echo "=== Todos los planes ==="
curl -s http://localhost:8080/api/insurance-catalog

echo -e "\n\n=== Planes activos ==="
curl -s http://localhost:8080/api/insurance-catalog/active
```

---

## 🧪 Testing

Ejecutar los tests unitarios:

```bash
mvnw test
```

Tests incluidos:
- `InsuranceCatalogServiceApplicationTests`: Test de contexto
- `InsuranceCatalogControllerTest`: Test de endpoints
- `InsuranceCatalogServiceTest`: Test de lógica de servicio

---

## 📝 Logging

La aplicación utiliza **SLF4J** con **Logback** para logging:

- **INFO**: Planes encontrados
- **ERROR**: Errores durante consultas

Ejemplo en logs:

```
INFO com.challenge.insurance_catalog_service.service.impl.InsuranceCatalogServiceImpl : Found plan: Plan Básico
INFO com.challenge.insurance_catalog_service.service.impl.InsuranceCatalogServiceImpl : Found plan: Plan Plus
INFO com.challenge.insurance_catalog_service.service.impl.InsuranceCatalogServiceImpl : Found plan: Plan Premium
```

---

## 🔐 Consideraciones de seguridad

- La consola H2 está habilitada solo para desarrollo (`/h2-console`)
- Se recomienda deshabitarla en producción
- Los endpoints no requieren autenticación en esta versión

---

## 📚 Documentación Javadoc

Cada clase cuenta con documentación Javadoc completa:

- `InsuranceCatalogController`: Endpoints REST
- `InsuranceCatalogServiceImpl`: Lógica de negocio
- `InsurancePlanRepository`: Acceso a datos

---

## 📞 Contacto y soporte

Para reportar bugs o sugerencias, contactar al equipo de desarrollo.

---

## 📄 Licencia

Este proyecto es parte de un desafío técnico.

---

**Última actualización:** 14 de febrero de 2026

