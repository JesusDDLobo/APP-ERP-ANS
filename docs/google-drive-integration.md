# Documentación de Integración con Google Drive (ANS-ERP)

## 1. Visión General

El sistema ANS-ERP integra Google Drive no solo como un sistema de almacenamiento de archivos (Storage), sino como una extensión de la estructura organizacional de la empresa. La integración permite replicar la jerarquía de datos del ERP (Clientes, Proyectos, Vehículos) en carpetas de Drive, gestionando permisos de acceso de forma dinámica.

### Características Principales
*   **Modo Híbrido de Autenticación:** Capacidad de operar actuando como el usuario logueado (Impersonation) o como el sistema (Service Account).
*   **Espejo de Estructura:** Cada entidad importante en el ERP tiene una contraparte (carpeta) en Drive.
*   **Gestión de Permisos:** Asignación automática de permisos de lectura/escritura a los involucrados en un ticket o proyecto.

---

## 2. Arquitectura de Autenticación

El núcleo de la integración es `App\Services\GoogleDriveService`, el cual decide dinámicamente qué credenciales utilizar para comunicarse con la API de Google.

### A. Modo "Impersonation" (Prioritario)
Se utiliza cuando un usuario humano está autenticado y tiene tokens de Google válidos (`google_access_token`, `google_refresh_token`).
*   **Objetivo:** Que en el historial de versiones de Drive aparezca "Modificado por [Nombre Usuario]" en lugar de una cuenta genérica.
*   **Requisito:** El usuario debe haber vinculado su cuenta (ver `docs/authentication.md`).

### B. Modo "Service Account" (Respaldo/Background)
Se utiliza en tareas programadas (Jobs), cuando el usuario no ha vinculado su cuenta, o cuando los tokens del usuario han expirado y no se pueden refrescar.
*   **Objetivo:** Garantizar la continuidad operativa del sistema.
*   **Credenciales:** Archivo JSON de Service Account configurado en `.env`.

### Roles y permisos JIT (lectura vs. edición)
- El cliente (OAuth/Service Account) mantiene scopes de lectura/escritura para permitir creación y edición cuando corresponde.
- Para vistas y previsualizaciones, `DocumentViewerAccessService` concede permisos JIT con rol `reader` (controlado por `drive.permissions.default_role`, default `reader`). Así se minimizan privilegios en flujos de solo lectura.
- Para flujos que crean/modifican (p. ej. `DocumentDriveService` al subir documentos, versionar o generar desde plantillas), se otorgan roles de escritura explícitos según la lógica del módulo. No se debe forzar `reader` en esos flujos.
- Recomendación: mantener `DRIVE_PERMISSIONS_DEFAULT_ROLE=reader` en `.env` y otorgar `writer` o `fileOrganizer` solo en rutas de negocio que realmente necesitan modificar archivos.

---

## 3. Estructura de Carpetas

El sistema organiza los archivos siguiendo una jerarquía estricta definida por constantes y lógica de negocio.

### Jerarquía General
`RAÍZ (GOOGLE_DRIVE_ROOT_FOLDER_ID)`
├── 📂 **Propuestas**
│   └── 📂 **[ID Propuesta]** (Ej. PC-0010)
│       ├── 📄 [Documentos Base de la Propuesta]
│       └── 📂 **[ID Ticket Derivado]** (Ej. DI-0004, LO-0023)
│           ├── 📂 **Solicitud** (Archivos de entrada/requerimiento)
│           └── 📂 **Respuesta** (Entregables y documentos de trabajo)
│
├── 📂 **Logística**
│   └── 📂 **Vehículos**
│       └── 📂 **[Placa Vehículo]**
│           ├── 📂 Documentos (SOAT, Tecno)
│           └── 📂 Mantenimientos
│
└── 📂 **[Otros Procesos]**

---

## 4. Componentes Clave

### `App\Services\GoogleDriveService`
Clase principal que envuelve la lógica de negocio.

#### Métodos Principales
*   `ensureFolderExists($name, $parentId)`: Busca una carpeta por nombre dentro de un padre específico; si no existe, la crea. Retorna el ID de la carpeta.
*   `createFolder($name, $parentId, $description)`: Crea una carpeta explícitamente y asigna metadatos (color, descripción).
*   `grantPermissions($fileId, $email, $role)`: Otorga acceso ('reader' o 'writer') a un email específico sobre un archivo o carpeta.
*   `uploadFile($path, $file, $name)`: Sube archivos manteniendo la referencia al padre.
*   `getClient()`: Método interno que resuelve si usar el cliente OAuth del usuario o el cliente de Service Account.

### `App\Providers\GoogleDriveServiceProvider`
Registra el adaptador de Flysystem para Google Drive. Permite usar la fachada de Laravel de forma transparente:
```php
// Ejemplo de uso
Storage::disk('google')->put('archivo.pdf', $content);
```

### Integración en Modelos (Traits)
Los modelos principales (`Client`, `Vehicle`, `Ticket`, `Project`) suelen tener un campo `google_drive_folder_id`.
*   **Al crear:** El sistema guarda el ID de la carpeta de Drive en la base de datos local.
*   **Al consultar:** Se usa este ID para generar enlaces directos o subir archivos sin tener que buscar la carpeta por nombre nuevamente.

---

## 5. Flujos de Trabajo

### A. Creación de Entidad (Ej. Nuevo Vehículo)
1.  El usuario envía el formulario de creación.
2.  El controlador llama a `GoogleDriveService`.
3.  El servicio verifica/crea la carpeta "Logística" -> "Vehículos".
4.  Crea la carpeta con la **Placa** del vehículo.
5.  Crea subcarpetas estándar (Documentos, Fotos).
6.  Retorna el ID de la carpeta de la Placa, que se guarda en `vehicles.google_drive_folder_id`.

### B. Subcarpetas de Ticket: Solicitud y Respuesta

La estructura de carpetas para tickets se adapta según si es el ticket raíz (Propuesta) o un ticket derivado (Solicitud a otra área).

#### 1. Ticket Raíz (Propuesta)
*   **Ruta:** `Propuestas/{proposal_public_id}/`
*   **Contenido:** Documentos principales de la propuesta.
*   **Comportamiento:** No se crean subcarpetas `Solicitud` ni `Respuesta`. Los archivos se almacenan directamente en la raíz del ticket.

#### 2. Ticket Derivado (Solicitud Interna)
Para solicitudes generadas desde una propuesta hacia otras áreas (Diseño, Logística, etc.), se crea una carpeta anidada:

*   **Ruta:** `Propuestas/{proposal_public_id}/{ticket_public_id}/`
*   **Estructura Interna:**
    *   📂 **Solicitud**: Contiene los archivos adjuntos al crear el ticket (requerimientos).
    *   📂 **Respuesta**: Contiene los archivos adjuntos en la respuesta y documentos de trabajo generados.

#### Implementación Técnica
El método `DocumentDriveService::storeTicketDocument` gestiona esta lógica mediante el atributo `category`:

*   `category = "request"`:
    *   Crea/Usa la subcarpeta **Solicitud** dentro de la carpeta del ticket específico.
    *   Usado al crear tickets derivados (`TicketController@store`).
*   `category = "response"`:
    *   Crea/Usa la subcarpeta **Respuesta** dentro de la carpeta del ticket específico.
    *   Usado al responder tickets (`TicketController@storeSubmition`) o versionar (`TicketController@versionate`).
*   `category = null` (o no definido):
    *   Usa la carpeta base del ticket.
    *   Usado para los documentos base de la propuesta (`PropositionsController@store`).

Adicionalmente, los **documentos de trabajo** (Google Docs/Sheets/Slides) generados desde plantillas se consideran automáticamente parte de la respuesta y se ubican en la carpeta **Respuesta**.

### C. Subida de Archivos
1.  Frontend envía archivo a un endpoint (ej. `/tickets/{id}/files`).
2.  Backend recupera el `google_drive_folder_id` del ticket.
3.  Usa `DocumentDriveService::storeTicketDocument` pasando, cuando aplique, `category="request"` o `category="response"` para dirigir el archivo a la subcarpeta adecuada.
4.  Registra el archivo en la tabla `files` con la URL de visualización de Drive.

---

## 6. Configuración (.env)

Variables críticas para el funcionamiento:

```ini
FILESYSTEM_DISK=google

# Credenciales de la App en Google Cloud Console
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=...

# ID de la carpeta raíz donde se creará toda la estructura
GOOGLE_DRIVE_ROOT_FOLDER_ID=...

# (Opcional) ID de carpeta Team Drive si se usa
GOOGLE_DRIVE_TEAM_DRIVE_ID=...
```

## 7. Manejo de Errores y Resiliencia

El sistema implementa mecanismos para manejar fallos en la API de Google:

*   **Expiración de Tokens:** Si el token del usuario expira, el sistema intenta refrescarlo automáticamente. Si falla, degrada a Service Account (si la operación lo permite) o lanza una excepción controlada.
*   **Servicio No Disponible:** En caso de caída total de Google, las operaciones de lectura locales siguen funcionando, pero las subidas/creaciones se ponen en cola o muestran un error amigable al usuario (ver [`docs/authentication.md`](docs/authentication.md) para detalles sobre el modo "Offline").
