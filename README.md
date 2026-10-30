# AgroAPI — Llontop Vivas

Backend REST del caso **AgroSierra**, desarrollado con Kotlin, Spring Boot, Spring Security/JWT, Spring Data JPA y base relacional H2 persistida en archivo.

- **Autor:** Josué Llontop Vivas · Desarrollo de Aplicaciones Móviles, Unidad 2.
- **Cliente Android:** [AgroCampo-Llontop-Vivas](https://github.com/JosueLV/AgroCampo-Llontop-Vivas).
- Ramas `feature/*` y `sc-*-llontop` → `develop` → `main`; entrega: etiqueta `v1.0-unidad2`.

## 1. Requisitos y ejecución

Java **17 o superior**, puerto **8080** libre y conexión a Internet la primera vez para resolver dependencias. En PowerShell, dentro del directorio del backend:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Alternativa con Docker Desktop:

```powershell
docker compose up --build
```

- URL en el PC y Postman: **`http://localhost:8080/api/v1`**.
- URL desde un emulador Android (servidor en el PC): **`http://10.0.2.2:8080/api/v1`**.
- Para consultar un endpoint protegido sin token, es normal recibir **401**: significa que el backend sí está respondiendo.

El archivo `.env.example` documenta los ajustes de entorno. Para despliegues no académicos, configura un `JWT_SECRET_BASE64` propio y HTTPS.

## 2. Estructura de paquetes

Código fuente: `src/main/kotlin/pe/upeu/agrosierra/`.

| Paquete | Responsabilidad |
|---|---|
| `config/` | Spring Security, validación JWT, respuesta uniforme de errores, datos semilla |
| `seguridad/` | `POST /auth/login` y usuario autenticado |
| `dominio/` | `Usuario`, `Rol`, `Lote`, `Labor`, `TipoLabor` |
| `repositorio/` | Consultas y persistencia mediante Spring Data JPA |
| `servicio/` | Orquestación, reglas RN-01 a RN-04 y transacciones |
| `web/` | Rutas HTTP del servicio |
| `web/dto/` | Peticiones, respuestas, validación y paginación |

Pruebas de integración: `src/test/kotlin/pe/upeu/agrosierra/`; colección Postman: `coleccion/AgroAPI-Llontop-Vivas.postman_collection.json`.

## 3. Contrato REST v1

Todos los endpoints usan prefijo `/api/v1`.

| Método | Ruta | Privilegio necesario | Código correcto |
|---|---|---|---|
| POST | `/auth/login` | Público | 200 |
| GET | `/lotes?pagina=0&tamanio=20&valle=&estado=` | `LOTE_LEER` | 200 |
| GET | `/lotes/{id}` | `LOTE_LEER` | 200 |
| GET | `/lotes/{id}/labores?pagina=0&tamanio=20` | `LABOR_LEER` | 200 |
| POST | `/labores` | `LABOR_CREAR` | 201 |
| PUT | `/labores/{id}` | `LABOR_EDITAR` | 200 |
| DELETE | `/labores/{id}` | `LABOR_ELIMINAR` | 204, sin cuerpo |
| POST | `/lotes/{id}/cerrar-campania` | `CAMPANIA_CERRAR` | 200 |
| GET | `/usuarios/me` | Autenticado | 200 |

Todos los endpoints protegidos usan **`Authorization: Bearer <jwt>`**. El JWT está firmado, incluye identificación y privilegios y vence a la hora (sin refresh token). El login verifica el hash de contraseña con BCrypt. La autorización usa `hasAuthority` y los privilegios almacenados en roles; no depende de un nombre de rol hardcodeado en el controlador.

**Ejemplo de alta de labor:**

```json
{
  "loteId": 4,
  "tipo": "RIEGO",
  "fecha": "2026-10-27",
  "cantidad": 1.5,
  "cantidadJornales": 2,
  "observacion": "Riego de parcela"
}
```

Tipos válidos: `RIEGO`, `FUMIGACION`, `COSECHA`. Para editar se envían los mismos campos salvo `loteId`. `cantidad` debe ser positiva, `cantidadJornales` un entero ≥1, `observacion` máximo 500 caracteres.

**Paginación:** `{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":0,"totalPaginas":0,"ultima":true}` y filtros `valle`, `estado=ABIERTA|CERRADA`.

**Error uniforme:**

```json
{
  "timestamp": "2026-10-27T09:14:22",
  "status": 400,
  "error": "Bad Request",
  "message": "Dosis de fumigación excedida",
  "path": "/api/v1/labores",
  "validationErrors": {
    "cantidad": "Máximo permitido para el lote: 2.0"
  }
}
```

Se responden `400` (validación con campos), `401` (sin token, token alterado, vencido o credenciales falsas), `403` (sin permiso), `404` (no existe), `409` (conflicto de negocio) y `500` (error interno).

## 4. Privilegios por rol

| Privilegio | TECNICO | JEFE_CAMPO | ADMINISTRADOR | SUPERVISOR_CALIDAD (SC-B) |
|---|---|---|---|---|
| `LOTE_LEER` y `LABOR_LEER` | Sí | Sí | Sí | Sí |
| `LABOR_CREAR` | Sí | Sí | No | No |
| `LABOR_EDITAR` | Propias, antes de 24 h | Todas | No | Todas |
| `LABOR_ELIMINAR` | No | Sí | No | No |
| `CAMPANIA_CERRAR` | No | Sí | No | No |
| `USUARIO_GESTIONAR` | No | No | Sí | No |

El contrato no define endpoints de administración de usuarios; el permiso `USUARIO_GESTIONAR` queda modelado. El supervisor de calidad es un rol persistido, sin excepciones `if (rol == ...)` en controladores.

## 5. Reglas de negocio

| Regla | Aplicación | Error |
|---|---|---|
| RN-01 | No crear ni corregir labores en campaña cerrada | 409 |
| RN-02 | Un técnico solo modifica sus propias labores durante las primeras 24 h | 403 |
| RN-03 | No cerrar campaña sin al menos una labor `COSECHA` | 409 |
| RN-04 | En `FUMIGACION`, `cantidad` no supera `hectareas × dosisMaximaPorHectarea` | 400 con `validationErrors.cantidad` |

Estas validaciones se implementan en `AgroServicio` y se comprueban en pruebas de integración, independientemente de los botones de Android.

## 6. Persistencia y datos de prueba

H2 persistente, entidades JPA con usuarios/roles/permisos, lotes y labores. El arranque inicial crea **8 lotes de 3 valles**, **20 labores** y dos campañas cerradas; el seed es idempotente. El campo `cantidadJornales` de la solicitud **SC-C** se guarda y se devuelve en DTO y datos iniciales.

| Usuario | Contraseña | Rol |
|---|---|---|
| `tecnico1` | `Agro2026*` | TECNICO |
| `jefe1` | `Agro2026*` | JEFE_CAMPO |
| `admin` | `Agro2026*` | ADMINISTRADOR |
| `supervisor1` | `Agro2026*` | SUPERVISOR_CALIDAD |

Las credenciales son **solo de práctica**. El usuario supervisor se incorpora también a bases anteriores si falta.

## 7. Pruebas y evidencias finales

Importa en Postman `coleccion/AgroAPI-Llontop-Vivas.postman_collection.json`. Ejecuta primero los logins, que guardan las variables de JWT. La colección incluye ejemplos de `401` sin token y con token inválido, `403` por privilegios, `400` por fumigación fuera de dosis, `409` de campañas y el **204 sin cuerpo** de DELETE. Se muestran pruebas para el supervisor de calidad.

```powershell
.\mvnw.cmd test
git log --graph --oneline --all
git log --date=short --pretty="%h %ad %s"
```

Las capturas de pruebas, operaciones y gráfica Git se incorporan al PDF de evidencias, por temas. No se incluyen preguntas de defensa en este README.
