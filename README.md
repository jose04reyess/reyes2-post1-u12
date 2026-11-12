# Catálogo de Productos — Dockerización con Perfiles y CI/CD en GitHub Actions

> **Estudiante:** Reyes  
> **Asignatura:** Programación Web — Unidad 12  
> **Tema:** Dockerización con Perfiles de Spring Boot y Pipeline CI/CD con GitHub Actions (R2-Lab)  
> **Stack Tecnológico:** Java 21 LTS, Spring Boot 3.2.5, Maven, Docker Multi-Stage, H2 / PostgreSQL, Spring Boot Actuator, GitHub Actions, JUnit 5, Mockito.

---

## 1. Descripción del Proyecto

Este proyecto implementa una API REST completa para la gestión de un catálogo de productos informáticos desarrollada con **Java 21** y **Spring Boot 3.2**. La solución aplica las mejores prácticas de la ingeniería de software y DevOps moderno:
- **Separación de entornos por perfiles:** Configuración desacoplada para desarrollo (`dev` con H2 en memoria y precarga automática de datos) y producción (`prod` con PostgreSQL y hardening de endpoints).
- **Dockerización optimizada (Multi-Stage Build):** Construcción en dos fases (builder con JDK 21 y runtime con JRE 21 alpine), reduciendo drásticamente la superficie de ataque y el tamaño final de la imagen, ejecutada bajo un usuario no root (`spring`).
- **Observabilidad con Spring Boot Actuator:** Endpoint de salud `/actuator/health` configurado para validaciones de disponibilidad en tiempo real.
- **Pipeline CI/CD automatizado:** Workflow de GitHub Actions con jobs encadenados de compilación/pruebas (con publicación de reportes Surefire) y construcción/smoke testing del contenedor Docker.

---

## 2. Estructura del Repositorio

```text
reyes2-post1-u12/
├── .dockerignore
├── .gitignore
├── Dockerfile
├── pom.xml
├── README.md
├── .github/
│   └── workflows/
│       └── ci.yml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── universidad/
    │   │           └── catalogo/
    │   │               ├── CatalogoApplication.java
    │   │               ├── config/
    │   │               │   └── DataSeeder.java
    │   │               ├── controller/
    │   │               │   └── ProductoController.java
    │   │               ├── model/
    │   │               │   └── Producto.java
    │   │               ├── repository/
    │   │               │   └── ProductoRepository.java
    │   │               └── service/
    │   │                   └── ProductoService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       └── application-prod.properties
    └── test/
        └── java/
            └── com/
                └── universidad/
                    └── catalogo/
                        ├── ApplicationContextTest.java
                        └── service/
                            └── ProductoServiceTest.java
```

---

## 3. Dominio del Catálogo de Productos

La arquitectura sigue el patrón por capas clásico y limpio de Spring:

| Componente | Paquete | Descripción |
|---|---|---|
| **CatalogoApplication** | `com.universidad.catalogo` | Clase principal que inicia el contexto de Spring Boot (`@SpringBootApplication`). |
| **Producto** | `...catalogo.model` | Entidad JPA (`@Entity`, `@Table(name = "productos")`) con validaciones declarativas (`@NotBlank`, `@NotNull`, `@Positive`). |
| **ProductoRepository** | `...catalogo.repository` | Interfaz Spring Data JPA (`JpaRepository<Producto, Long>`) con consulta derivada `findByCategoria(String categoria)`. |
| **ProductoService** | `...catalogo.service` | Capa transaccional de negocio con inyección por constructor de `ProductoRepository`. Métodos: `listarTodos()`, `buscarPorId(Long id)`, `crear(Producto p)`, `eliminar(Long id)`. |
| **ProductoController** | `...catalogo.controller` | Controlador REST (`@RestController`, `@RequestMapping("/api/productos")`) que expone los endpoints CRUD con códigos de estado HTTP apropiados (`200 OK`, `201 Created`, `204 No Content`, `404 Not Found`). |
| **DataSeeder** | `...catalogo.config` | `CommandLineRunner` condicionado a `@Profile("dev")`. Si la tabla se encuentra vacía al inicio, precarga automáticamente 3 registros de prueba. |

---

## 4. Gestión de Perfiles de Configuración

Spring Boot administra las variables del sistema mediante perfiles desacoplados:

### `application.properties` (Configuración Común)
Define el nombre del servicio, puerto parametrizable y perfil activo por defecto:
```properties
spring.application.name=mi-spring-app
server.port=${PORT:8080}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

### Perfil Desarrollo (`dev`) — `application-dev.properties`
- **Base de datos:** Base de datos en memoria H2 (`jdbc:h2:mem:devdb`).
- **DDL Hibernate:** `create-drop` para reiniciar el esquema limpiamente en cada arranque.
- **Actuator:** Exposición abierta (`*`) y detalles de salud siempre visibles (`show-details=always`).
- **DataSeeder:** Activo para sembrar productos automáticamente.

### Perfil Producción (`prod`) — `application-prod.properties`
- **Base de datos:** Conexión a motor PostgreSQL (`jdbc:postgresql://localhost:5432/appdb`).
- **DDL Hibernate:** `validate` (no altera tablas automáticamente).
- **Actuator:** Exposición restringida únicamente a `health,info` y detalles ocultos o restringidos (`show-details=when-authorized`).
- **DataSeeder:** Desactivado por seguridad.

---

## 5. Dockerización con Multi-Stage Build

El archivo `Dockerfile` implementa una estrategia de construcción multi-etapa para generar artefactos mínimos, seguros y eficientes:

```dockerfile
# Etapa 1: compilacion con Maven y JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# Etapa 2: imagen de ejecucion ligera (solo JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Ventajas de este Enfoque
1. **Caché eficiente de dependencias:** Copiar `pom.xml` antes del código fuente y ejecutar `mvn dependency:go-offline` permite reutilizar las capas cacheadas de Maven si solo cambia el código fuente.
2. **Reducción de tamaño (JRE vs JDK):** La imagen final descarta el compilador, herramientas de build y código fuente; utiliza exclusivamente el entorno de ejecución `eclipse-temurin:21-jre-alpine`.
3. **Seguridad (Principio de menor privilegio):** La aplicación no corre como `root`. Se crea un grupo y usuario de sistema `spring` sin privilegios administrativos.

---

## 6. Pipeline CI/CD con GitHub Actions (`.github/workflows/ci.yml`)

El pipeline se activa automáticamente en cada `push` o `pull_request` a la rama `main` y consta de dos jobs secuenciales:

```
[ Push / PR a main ]
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Job 1: build-and-test                                       │
│ 1. Checkout actions/checkout@v4                             │
│ 2. Configurar Eclipse Temurin JDK 21 + caché Maven          │
│ 3. mvn clean verify (Unit tests + Integration tests)        │
│ 4. actions/upload-artifact@v4 (publicar reportes surefire)  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼ (needs: build-and-test)
┌─────────────────────────────────────────────────────────────┐
│ Job 2: docker-build                                         │
│ 1. Checkout actions/checkout@v4                             │
│ 2. docker build -t mi-spring-app:${{ github.sha }} .        │
│ 3. docker run -d -p 8080:8080 en segundo plano               │
│ 4. Smoke Test: polling a /actuator/health buscando "UP"     │
│ 5. docker rm -f smoke-test (siempre ejecutado al final)     │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Pruebas Automatizadas

Se implementaron dos niveles de pruebas en `src/test/java/com/universidad/catalogo/`:

1. **`ApplicationContextTest.java` (Prueba de Integración / Contexto):**
   - Anotada con `@SpringBootTest` y `@ActiveProfiles("dev")`.
   - Método `@Test void contextLoads()`: valida que la configuración de Spring Boot, los repositorios JPA y la base de datos H2 se instancien de manera íntegra sin fallos de configuración.

2. **`ProductoServiceTest.java` (Pruebas Unitarias Aisladas):**
   - Utiliza **JUnit 5** y **Mockito** (`@ExtendWith(MockitoExtension.class)`).
   - Mockea `ProductoRepository` (`@Mock`) e inyecta en `ProductoService` (`@InjectMocks`).
   - Cobertura completa de operaciones:
     - `testCrearProducto`: Guarda y retorna entidad con ID.
     - `testListarTodos`: Retorna la colección completa de productos.
     - `testBuscarPorId_Existente`: Consulta producto por identificador.
     - `testBuscarPorId_NoExistente`: Maneja `Optional.empty()` ante IDs inexistentes.
     - `testEliminar_Existe`: Confirma borrado cuando el registro existe.
     - `testEliminar_NoExiste`: Evita borrados erróneos si el ID no existe.

---

## 8. Especificación de Endpoints REST

La API se expone bajo la ruta base `/api/productos`:

### 8.1. Listar Productos
- **Método:** `GET`
- **Ruta:** `/api/productos`
- **Respuesta Exitosa:** `200 OK`
```json
[
  {
    "id": 1,
    "nombre": "Laptop Lenovo ThinkPad",
    "precio": 1299.99,
    "categoria": "Computación"
  },
  {
    "id": 2,
    "nombre": "Mouse Inalámbrico Logitech",
    "precio": 29.99,
    "categoria": "Accesorios"
  },
  {
    "id": 3,
    "nombre": "Monitor Dell 27 4K",
    "precio": 399.99,
    "categoria": "Monitores"
  }
]
```

### 8.2. Obtener Producto por ID
- **Método:** `GET`
- **Ruta:** `/api/productos/{id}`
- **Respuestas:**
  - `200 OK` con el JSON del producto.
  - `404 Not Found` si el ID no existe.

### 8.3. Crear Producto
- **Método:** `POST`
- **Ruta:** `/api/productos`
- **Cabeceras:** `Content-Type: application/json`
- **Cuerpo (Request Body):**
```json
{
  "nombre": "Teclado Mecánico Keychron K2",
  "precio": 95.0,
  "categoria": "Periféricos"
}
```
- **Respuesta Exitosa:** `201 Created`
```json
{
  "id": 4,
  "nombre": "Teclado Mecánico Keychron K2",
  "precio": 95.0,
  "categoria": "Periféricos"
}
```

### 8.4. Eliminar Producto
- **Método:** `DELETE`
- **Ruta:** `/api/productos/{id}`
- **Respuestas:**
  - `204 No Content` si se eliminó exitosamente.
  - `404 Not Found` si el ID no existe.

### 8.5. Endpoint de Salud (Actuator)
- **Método:** `GET`
- **Ruta:** `/actuator/health`
- **Respuesta Exitosa:** `200 OK`
```json
{
  "status": "UP"
}
```

---

## 9. Guía de Ejecución Local y Despliegue con Docker

### 9.1. Ejecución con Maven Local

```bash
# Compilar y ejecutar pruebas automatizadas
mvn clean verify

# Iniciar la aplicación con el perfil de desarrollo (H2 + DataSeeder)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 9.2. Construcción y Ejecución en Docker

```bash
# Construir la imagen Docker localmente
docker build -t catalogo-app:latest .

# Ejecutar el contenedor mapeando el puerto 8080
docker run -d --name catalogo-prod -p 8080:8080 catalogo-app:latest

# Comprobar el estado del contenedor con Actuator
curl -i http://localhost:8080/actuator/health

# Ver logs del contenedor
docker logs -f catalogo-prod

# Detener y remover el contenedor
docker stop catalogo-prod && docker rm catalogo-prod
```

---

## 10. Checklist de Cumplimiento Rúbrica R2-Lab

| Criterio / Requisito | Estado | Detalle de Implementación |
|---|:---:|---|
| **Aplicación Base Spring Boot** | Cumplido | Arquitectura limpia en `com.universidad.catalogo`, entidad `Producto`, repositorio JPA con query derivada, servicio transaccional y controlador REST completo. |
| **DataSeeder para Pruebas** | Cumplido | Precarga condicional (`@Profile("dev")`) de 3 productos al iniciar si la tabla está vacía. |
| **Separación de Perfiles dev/prod** | Cumplido | `application.properties`, `application-dev.properties` (H2, logging DEBUG, Actuator abierto) y `application-prod.properties` (Postgres, logging INFO/WARN, Actuator restringido). |
| **Pruebas Automatizadas** | Cumplido | `ApplicationContextTest` (`@SpringBootTest`, `@ActiveProfiles("dev")`) y `ProductoServiceTest` (JUnit 5 + Mockito). |
| **Docker Multi-Stage Seguro** | Cumplido | `Dockerfile` con etapa de compilación JDK 21 Alpine y etapa de ejecución JRE 21 Alpine con usuario no root `spring`. |
| **Spring Boot Actuator** | Cumplido | Dependencia incluida, endpoint `/actuator/health` expuesto y verificado. |
| **Pipeline CI/CD en GitHub Actions** | Cumplido | Archivo `.github/workflows/ci.yml` con jobs `build-and-test` (reportes surefire) y `docker-build` con smoke test por polling a `/actuator/health`. |
| **Documentación Consolidada** | Cumplido | `README.md` exhaustivo con arquitectura, endpoints, comandos y justificación técnica. |
