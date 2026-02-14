# Quote Calculator Service

## 📋 Descripción General

**Quote Calculator Service** es un microservicio reactivo desarrollado con Spring Boot WebFlux que calcula cotizaciones de seguros personalizadas para prospectos de usuarios. El servicio evalúa la elegibilidad del usuario basándose en criterios específicos y devuelve opciones de planes de seguros disponibles con precios finales ajustados según factores de riesgo.

## 🎯 Funcionalidades Principales

- **Cálculo de Cotizaciones Reactivo**: Utiliza programación reactiva con Project Reactor para procesar solicitudes de forma asincrónica
- **Filtrado Inteligente**: Evalúa planes basados en edad, ingreso anual y factores de riesgo
- **Streaming en Tiempo Real**: Devuelve resultados mediante Server-Sent Events (SSE) para actualizaciones en tiempo real
- **Recargos de Riesgo**: Aplica incrementos automáticos en precios para fumadores (20% de recargo)
- **Manejo de Errores**: Respuestas detalladas cuando no hay planes disponibles

## 🔧 Requisitos Técnicos

- **Java**: 17 o superior
- **Spring Boot**: 3.5.10
- **Maven**: 3.6+
- **Insurance Catalog Service**: Disponible en `http://localhost:8080` (dependencia externa)

## 📦 Tecnologías Utilizadas

- **Spring Boot WebFlux**: Framework reactivo para aplicaciones web
- **Project Reactor**: Librería de programación reactiva
- **Spring Data R2DBC**: Acceso a datos reactivo
- **Lombok**: Reducción de código boilerplate
- **H2 Database**: Base de datos en memoria para desarrollo

## 🚀 Iniciar la Aplicación

### Opción 1: Maven

```bash
mvn spring-boot:run
```

### Opción 2: JAR Compilado

```bash
mvn clean package
java -jar target/quote-calculator-service-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en: **http://localhost:8081**

## 🔌 Endpoints de la API

### POST - Crear Cotización de Seguros

```http
POST http://localhost:8081/api/quote-calculator
Content-Type: application/json
```

#### Criterios de Elegibilidad

- ✅ Edad del usuario debe estar dentro del rango permitido del plan
- ✅ Ingreso anual debe ser mayor a 30,000
- ✅ Se aplica 20% de recargo si el usuario es fumador
- ❌ Si no cumple criterios: Error `PlanNotFoudForUser`

#### Estructura del Request

```json
{
  "dni": "12345678A",
  "fullname": "Juan García López",
  "occupation": "Ingeniero",
  "annualIncome": 45000.00,
  "isSmoker": true,
  "age": 35
}
```

#### Campos de Entrada

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `dni` | String | Documento de identidad nacional | "12345678A" |
| `fullname` | String | Nombre completo del usuario | "Juan García López" |
| `occupation` | String | Ocupación del usuario | "Ingeniero" |
| `annualIncome` | Double | Ingreso anual en unidades monetarias | 45000.00 |
| `isSmoker` | Boolean | Indicador de si el usuario es fumador | true/false |
| `age` | Integer | Edad del usuario en años | 35 |

#### Estructura de Response (Streaming SSE)

```json
{
  "fullname": "Juan García López",
  "planName": "Plan Premium Plus",
  "price": 120.00
}
```

#### Campos de Salida

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `fullname` | String | Nombre completo del usuario |
| `planName` | String | Nombre del plan de seguros |
| `price` | Double | Precio final incluyendo recargos aplicables |

---

## 📝 Ejemplos de Invocación

### Ejemplo 1: Usuario Elegible (Fumador)

**Request:**
```bash
curl -X POST http://localhost:8081/api/quote-calculator \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678A",
    "fullname": "Juan García López",
    "occupation": "Ingeniero",
    "annualIncome": 45000.00,
    "isSmoker": true,
    "age": 35
  }'
```

**Response (SSE Stream):**
```
data: {"fullname":"Juan García López","planName":"Plan Premium Plus","price":120.00}

data: {"fullname":"Juan García López","planName":"Plan Básico","price":72.00}

data: {"fullname":"Juan García López","planName":"Plan Integral","price":180.00}
```

*Nota: El precio incluye 20% de recargo por ser fumador*

---

### Ejemplo 2: Usuario No Fumador

**Request:**
```bash
curl -X POST http://localhost:8081/api/quote-calculator \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "87654321B",
    "fullname": "María Rodríguez Pérez",
    "occupation": "Médica",
    "annualIncome": 55000.00,
    "isSmoker": false,
    "age": 42
  }'
```

**Response (SSE Stream):**
```
data: {"fullname":"María Rodríguez Pérez","planName":"Plan Premium Plus","price":100.00}

data: {"fullname":"María Rodríguez Pérez","planName":"Plan Integral","price":150.00}
```

---

### Ejemplo 3: Usuario No Elegible (Ingreso Insuficiente)

**Request:**
```bash
curl -X POST http://localhost:8081/api/quote-calculator \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "11111111C",
    "fullname": "Carlos López Martínez",
    "occupation": "Estudiante",
    "annualIncome": 15000.00,
    "isSmoker": false,
    "age": 28
  }'
```

**Response (Error):**
```json
{
  "timestamp": "2024-02-14T10:30:45.123Z",
  "status": 400,
  "error": "No hay seguros para su perfil",
  "message": "No existen planes de seguros disponibles para el perfil del usuario"
}
```

---

### Ejemplo 4: Usuario Fuera de Rango de Edad

**Request:**
```bash
curl -X POST http://localhost:8081/api/quote-calculator \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "99999999D",
    "fullname": "Ana García Sánchez",
    "occupation": "Jubilada",
    "annualIncome": 35000.00,
    "isSmoker": false,
    "age": 75
  }'
```

**Response (Error):**
```json
{
  "timestamp": "2024-02-14T10:32:15.456Z",
  "status": 400,
  "error": "No hay seguros para su perfil",
  "message": "No existen planes de seguros disponibles para el perfil del usuario"
}
```

---

## 🔗 Integración con Insurance Catalog Service

Este servicio depende del **Insurance Catalog Service** que debe estar ejecutándose en `http://localhost:8080`. 

**Flujo de Integración:**

```
Quote Calculator Service
    ↓ (obtiene planes activos)
Insurance Catalog Service (http://localhost:8080)
    ↓ (filtra y aplica recargos)
Quote Calculator Service
    ↓ (devuelve cotizaciones)
Cliente (recibe SSE Stream)
```

## 📊 Arquitectura de Capas

```
┌─────────────────────────────────────┐
│   Controller Layer                  │
│   (QuoteCalculatorController)       │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Service Layer                     │
│   (QuoteCalculatorService)          │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Proxy Layer                       │
│   (InsuranceCatalogProxy)           │
└────────────────┬────────────────────┘
                 ↓
    External Insurance Catalog API
```

## 🧪 Testing

Ejecutar pruebas unitarias:

```bash
mvn test
```

Ejecutar pruebas específicas:

```bash
mvn test -Dtest=QuoteCalculatorControllerTest
mvn test -Dtest=QuoteCalculatorServiceTest
```

## 📋 Configuración

Editar `src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: quote-calculator-service

logging:
  level:
    com.challenge.quote_calculator_service: DEBUG

insurance-catalog-service:
  url: http://localhost:8080
```

## ⚙️ Estructura del Proyecto

```
quote-calculator-service/
├── src/
│   ├── main/
│   │   ├── java/com/challenge/quote_calculator_service/
│   │   │   ├── controller/          # Endpoints REST
│   │   │   ├── service/             # Lógica de negocio
│   │   │   ├── proxy/               # Integración con servicios externos
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── exception/           # Manejo de excepciones
│   │   │   ├── config/              # Configuraciones
│   │   │   └── QuoteCalculatorServiceApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/challenge/quote_calculator_service/
├── pom.xml
└── README.md
```

## 🐛 Manejo de Errores

El servicio implementa manejo centralizado de excepciones mediante `GlobalExceptionHandler`:

- **PlanNotFoudForUser**: Se lanza cuando no hay planes elegibles para el usuario
- **Status 400**: Criterios de elegibilidad no cumplidos
- **Status 500**: Errores internos del servidor

## 📝 Notas Importantes

- ⚠️ El servicio requiere que Insurance Catalog Service esté ejecutándose
- ⚠️ El ingreso mínimo requerido es 30,000
- ⚠️ Los fumadores reciben 20% de recargo automático en la prima
- ⚠️ Las respuestas se envían mediante SSE (Server-Sent Events)
- ⚠️ Todos los cálculos se realizan de forma reactiva y asincrónica

## 👨‍💻 Autores

Challenge Team

## 📄 Versión

0.0.1-SNAPSHOT

## 📞 Soporte

Para reportar problemas o sugerencias, contacte al equipo de desarrollo.

---

**Última actualización**: Febrero 2024

