# AgroAPI - Llontop Vivas

**Autor:** Josué Llontop Vivas  
**Curso:** Desarrollo de Aplicaciones Móviles - Unidad 2  
**Caso:** AgroSierra

Servicio REST de lotes y labores agrícolas con Kotlin, Spring Boot, Spring Security JWT y persistencia H2 mediante Spring Data JPA.

## 1. Requisitos y ejecución

- Java 17 o superior.
- Conexión a Internet para descargar Maven y las dependencias la primera vez.

Abre PowerShell dentro de la carpeta del proyecto y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

También puedes iniciarlo con Docker Desktop:

```powershell
docker compose up --build
```

Dirección del servicio: `http://localhost:8080/api/v1`. En el emulador Android, la dirección base será `http://10.0.2.2:8080/api/v1`.

Para ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd test
```

## 2. Estructura de paquetes

Todos los paquetes están dentro de `src/main/kotlin/pe/upeu/agrosierra/`.

| Carpeta | Responsabilidad |
|---|---|
| `config/` | Configuración JWT, filtro de seguridad, respuestas de error y datos semilla |
| `seguridad/` | Inicio de sesión y consulta del usuario actual |
| `dominio/` | Entidades Usuario, Rol, Lote y Labor; enumeración TipoLabor |
| `repositorio/` | Consultas y persistencia con JPA |
| `servicio/` | Reglas de negocio RN-01 a RN-04 y transacciones |
| `web/` | Endpoints REST |
| `web/dto/` | Peticiones, respuestas y paginación |

Las pruebas se encuentran en `src/test/kotlin/pe/upeu/agrosierra/`.

## 3. Contrato del API

Todas las rutas están bajo `/api/v1`.

| Método | Ruta | Privilegio | Respuesta correcta |
|---|---|---|---|
| POST | `/auth/login` | Público | 200 |
| GET | `/lotes?pagina=0&tamanio=20&valle=&estado=` | `LOTE_LEER` | 200 |
| GET | `/lotes/{id}` | `LOTE_LEER` | 200 |
| GET | `/lotes/{id}/labores?pagina=0&tamanio=20` | `LABOR_LEER` | 200 |
| POST | `/labores` | `LABOR_CREAR` | 201 |
| PUT | `/labores/{id}` | `LABOR_EDITAR` | 200 |
| DELETE | `/labores/{id}` | `LABOR_ELIMINAR` | 204 sin cuerpo |
| POST | `/lotes/{id}/cerrar-campania` | `CAMPANIA_CERRAR` | 200 |
| GET | `/usuarios/me` | Autenticado | 200 |

`estado` acepta `ABIERTA` o `CERRADA`. Los listados incluyen `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas` y `ultima`. Los tipos de labor válidos son `RIEGO`, `FUMIGACION` y `COSECHA`.

Para crear una labor se envía:

```json
{
  "loteId": 4,
  "tipo": "RIEGO",
  "fecha": "2026-10-27",
  "cantidad": 1.5,
  "observacion": "Riego programado"
}
```

Los errores utilizan los campos `timestamp`, `status`, `error`, `message`, `path` y `validationErrors`. Se aplican los códigos 400, 401, 403, 404, 409 y 500 según el motivo del fallo.

## 4. Roles y privilegios

| Privilegio | TECNICO | JEFE_CAMPO | ADMINISTRADOR |
|---|---|---|---|
| `LOTE_LEER` | Sí | Sí | Sí |
| `LABOR_LEER` | Sí | Sí | Sí |
| `LABOR_CREAR` | Sí | Sí | No |
| `LABOR_EDITAR` | Solo propias hasta 24 horas | Todas | No |
| `LABOR_ELIMINAR` | No | Sí | No |
| `CAMPANIA_CERRAR` | No | Sí | No |
| `USUARIO_GESTIONAR` | No | No | Sí |

Los privilegios se asignan al rol en la base de datos. El filtro de seguridad valida el JWT y Spring Security aplica `hasAuthority` sobre las rutas protegidas. El contrato proporcionado no define endpoints para gestionar usuarios; el privilegio `USUARIO_GESTIONAR` queda modelado para esa funcionalidad.

## 5. Reglas de negocio

- **RN-01:** no registrar ni corregir labores si la campaña está cerrada (409).
- **RN-02:** el técnico solo corrige sus propias labores dentro de las primeras 24 horas (403).
- **RN-03:** solo se cierra una campaña con al menos una labor COSECHA (409).
- **RN-04:** en una FUMIGACION, la cantidad no supera `hectareas × dosisMaximaPorHectarea` (400 con mensaje en `validationErrors.cantidad`).

Estas reglas se implementan en la capa `servicio`, separadas de los controladores.

## 6. Persistencia, seguridad y datos iniciales

Se utiliza una base H2 persistente en archivo; las entidades se gestionan con JPA. El inicio de sesión compara las contraseñas cifradas con BCrypt. El JWT está firmado y vence a los 60 minutos, sin renovación. Para producción se configura una clave propia en la variable `JWT_SECRET_BASE64`.

Usuarios de prueba:

| Usuario | Contraseña | Rol |
|---|---|---|
| `tecnico1` | `Agro2026*` | TECNICO |
| `jefe1` | `Agro2026*` | JEFE_CAMPO |
| `admin` | `Agro2026*` | ADMINISTRADOR |

Los datos semilla incluyen 8 lotes en tres valles, dos campañas cerradas y 20 labores. Se proporciona una colección de Postman en `coleccion/AgroAPI-Llontop-Vivas.postman_collection.json` y pruebas de integración para las operaciones y las reglas del servicio.
