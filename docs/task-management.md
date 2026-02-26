# Documentación de Gestión de Tareas y Asignaciones (ANS-ERP)

## 1. Visión General

El módulo de gestión de tareas permite desglosar las **Sub-Órdenes de Trabajo** (Sub Work Orders) del proceso de Operaciones en unidades de trabajo más pequeñas y asignables llamadas **Tareas** (Tasks). Estas tareas tienen un ciclo de vida propio, integración profunda con Google Tasks y validaciones operativas específicas (como el mantenimiento pre-operacional de vehículos).

### Características Principales
*   **Vinculación Jerárquica:** Las tareas siempre pertenecen a una Sub-Orden de Trabajo.
*   **Integración Bidireccional con Google Tasks:** Sincronización automática tanto para el creador (seguimiento) como para los técnicos asignados (ejecución).
*   **Reportes de Avance:** Los técnicos pueden enviar texto y fotos (Google Drive) desde el móvil para reportar progreso.
*   **Cierre con Preguntas Dinámicas:** Configuración de cuestionarios obligatorios u opcionales para la finalización de tareas.
*   **Validación de Recursos:** Verificación automática de mantenimientos pre-operacionales al asignar vehículos.
*   **Prevención de Solapamientos:** Interfaz visual para asignar técnicos evitando conflictos de horario.

---

## 2. Reportes de Avance (Task Advances)

Los "Advances" permiten documentar el progreso de una tarea antes de su cierre.

### Funcionamiento
1.  **Registro:** El usuario (generalmente desde el móvil) envía un texto y, opcionalmente, archivos.
2.  **Almacenamiento de Archivos:** Las fotos/documentos se suben a una carpeta específica en Google Drive vinculada al Ticket. El sistema utiliza `TaskAdvanceService` para gestionar la creación del registro y la vinculación con Drive.
3.  **Visualización:** En el ERP Web, se pueden consultar los avances cronológicamente mediante el componente `TaskAdvancesSheet`.

---

## 3. Cierre de Tareas y Preguntas Dinámicas

Para estandarizar el cierre de tareas, se pueden configurar preguntas específicas.

### Configuración
- Los supervisores definen preguntas desde el ERP Web (`TaskQuestionConfig`).
- **Tipos de Pregunta:** `text` (respuesta abierta) o `file` (requiere subir evidencia).
- **Obligatoriedad:** Se puede marcar si una pregunta es requerida para poder cerrar la tarea.

### Flujo de Cierre
1.  **Consumo Móvil:** El APP móvil solicita las preguntas para el ticket (`GET /api/mobile/tasks/{ticket}/questions`).
2.  **Envío de Respuestas:** El APP envía las respuestas (`POST /api/mobile/tasks/{ticket}/complete`).
3.  **Validación:** El backend valida que se hayan respondido todas las preguntas obligatorias antes de marcar el ticket como "Terminado".

---

## 4. Integración Móvil (API)

Este apartado es crítico para el desarrollo del aplicativo móvil.

### Endpoints de Autenticación

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/mobile/login` | Inicio de sesión tradicional (email/password). |
| `POST` | `/api/mobile/google/login` | Inicio de sesión con Google (envía `code`). |
| `GET` | `/api/mobile/me` | Verifica el estado del usuario autenticado. |
| `POST` | `/api/mobile/logout` | Cierra la sesión activa. |
| `POST` | `/api/mobile/force-password-change` | Establece la contraseña inicial obligatoria. |

### Endpoints de Tareas Operativas

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/mobile/tasks/advance` | Envía un reporte de avance (Multipart: `ticket_id`, `content`, `files[]`). |
| `GET` | `/api/mobile/tasks/{ticket}/questions` | Obtiene la lista de preguntas de cierre configuradas para esa tarea. |
| `POST` | `/api/mobile/tasks/{ticket}/complete` | Envía las respuestas finales y cierra la tarea (`answers`: array de `{question_id, answer_text, file_index?}`). |

### Formato de Completado de Tarea
Para completar una tarea con archivos, se debe enviar un `Multipart Form Data`:
- `answers`: Un string JSON con el siguiente formato:
  ```json
  [
    { "question_id": 1, "answer_text": "Respuesta de texto" },
    { "question_id": 2, "answer_text": "Evidencia", "file_index": 0 }
  ]
  ```
- `files[]`: Los archivos físicos. El `file_index` en el JSON debe corresponder a la posición del archivo en el array `files[]`.

---

## 5. Flujo de Creación de Tareas

La creación de tareas es manejada principalmente por `App\Http\Controllers\TaskController@store`.

### Proceso de Creación
1.  **Validación de Datos:** Se requiere nombre, descripción y fechas opcionales.
2.  **Contexto Operativo:** Se verifica que la Sub-Orden de Trabajo exista y pertenezca al proceso de "Operaciones".
3.  **Validación de Vehículo (Si aplica):**
    *   Si la tarea implica el uso de un vehículo (`vehicle_id`), el sistema verifica en `VehicleMaintenance` si existe un registro de tipo `pre_task` creado el día actual.
    *   *Regla de Negocio:* No se puede crear la tarea si el vehículo no tiene su pre-operacional del día.
4.  **Generación del Ticket:**
    *   Se crea un nuevo `Ticket` con `content->type = 'task'`.
    *   **ID Público:** Se genera un consecutivo `TK-XXXX`.
    *   **Estado Inicial:** "En proceso".
5.  **Sincronización Google Tasks (Creador):**
    *   Se invoca `GoogleTasksService::syncCreatorTasks`.
    *   Crea dos tareas en la lista del creador: una para el inicio (`start_time`) y otra para el fin (`end_time`) de la tarea, permitiendo al supervisor visualizar el bloque de tiempo en su calendario/lista.

---

## 6. Asignación de Técnicos

---

## 7. Resiliencia y Funcionamiento Offline (App Móvil)

El aplicativo móvil está diseñado para funcionar en entornos de conectividad inestable (zonas rurales, sótanos, etc.) mediante una arquitectura **Offline-First**.

### Persistencia Local (Caché de Tareas)
La aplicación utiliza una base de datos local (**Room**) para almacenar la información de las tareas asignadas.
- **Sincronización Automática:** Cada vez que el usuario abre la lista de tareas con conexión, la app descarga los datos frescos y actualiza la base de datos local.
- **Modo Offline:** Si no hay conexión, la app carga automáticamente los datos desde el almacenamiento local, permitiendo al técnico consultar detalles, técnicos asignados y avances previos sin interrupciones.
- **Mapeo de Datos:** Los objetos complejos (preguntas, avances, usuarios) se serializan como JSON dentro de la base de datos local para mantener la integridad de la información.

### Gestión de Avances Offline
Los reportes de avance (texto y fotos) cuentan con un sistema de cola de subida:
1.  **Guardado Local:** Al crear un avance sin conexión, este se guarda en una tabla de "Pendientes" y las imágenes se comprimen y almacenan en el almacenamiento privado de la app.
2.  **Sincronización en Segundo Plano:** Se utiliza **WorkManager** para detectar cuándo el dispositivo recupera el acceso a internet. En ese momento, el sistema sube automáticamente todos los avances pendientes al servidor sin intervención del usuario.

### Resiliencia en la Carga de Datos
Para mitigar errores intermitentes del servidor (como truncado de JSON o "End of input"), la app implementa:
- **Auto-Reintento:** Si una petición de detalle de tarea falla, la app realiza hasta 3 intentos automáticos con un retraso de 1 segundo entre ellos.
- **Feedback Visual:** Se muestra una rueda de carga (`ProgressBar`) y se atenúa la interfaz durante los reintentos para informar al usuario que se está recuperando la información.

---

## 8. Visualización de Archivos y Multimedia

La aplicación soporta múltiples formatos de archivo para avances y evidencias:
- **Imágenes:** Visualización directa con zoom y caché optimizada mediante Glide.
- **Documentos (PDF, Word, Excel):**
    - **Archivos Remotos:** Se previsualizan mediante el visor de Google Docs integrado en un WebView.
    - **Archivos Locales:** Se abren utilizando aplicaciones externas del sistema mediante un `FileProvider` seguro, garantizando compatibilidad con cualquier visor instalado en el dispositivo.

---

## 9. Reglas de Edición y Seguridad

- **Ventana de Edición:** Los técnicos tienen un máximo de **10 minutos** para editar o eliminar un avance después de haberlo creado. Pasado este tiempo, el registro queda bloqueado para garantizar la trazabilidad.
- **Validación de Archivos:** Solo se permite la subida de formatos válidos (JPG, PNG, PDF, DOCX, XLSX, PPTX, RAR, ZIP) para evitar archivos corruptos o peligrosos en el servidor.
- **Compresión de Imágenes:** Todas las fotos tomadas desde la app se comprimen automáticamente antes del envío para reducir el consumo de datos y evitar errores de "Content Too Large" (413) en el servidor.


La asignación de personal se maneja a través de `App\Http\Controllers\TicketController@assignUsers`, reutilizando la lógica central de asignación de tickets pero con comportamientos específicos para tareas.

### Flujo de Asignación
1.  **Selección Visual:** Desde el frontend (`AssignToTaskSheet`), el supervisor ve un calendario con las tareas existentes de los técnicos para evitar solapamientos.
2.  **Procesamiento Backend:**
    *   Detecta usuarios agregados y removidos comparando con el estado actual.
    *   **Notificaciones:** Envía alertas internas a los usuarios afectados.
    *   **Recordatorios:** Si se define una fecha límite (`finish_date`), se programan recordatorios automáticos.
3.  **Sincronización Google Tasks (Asignados):**
    *   Se invoca `GoogleTasksService::syncAssignments`.
    *   Crea una tarea única en la lista de tareas de *cada técnico asignado*.
    *   Si el técnico es removido, `GoogleTasksService::removeAssignments` elimina la tarea de su lista de Google.

---

## 7. Integración con Google Tasks

El servicio `App\Services\GoogleTasksService` actúa como puente entre el ERP y la API de Google Tasks.

### Lista de Tareas
El sistema busca o crea automáticamente una lista de tareas llamada **"ANS Tickets"** en la cuenta de Google del usuario conectado.

### Tipos de Sincronización

| Tipo | Destinatario | Descripción | Método |
| :--- | :--- | :--- | :--- |
| **Tareas de Seguimiento** | Creador (Supervisor) | Crea hitos de "Inicio" y "Fin" para que el supervisor tenga visibilidad del cronograma. | `syncCreatorTasks` |
| **Tareas de Ejecución** | Asignado (Técnico) | Crea una tarea con la fecha límite o de fin, conteniendo enlace al ticket y detalles. | `syncAssignments` |

### Manejo de Tokens
El servicio utiliza `GoogleTokenRefreshService` para asegurar que siempre se use un token de acceso válido (`freshAccessToken`), renovándolo automáticamente si ha expirado antes de intentar contactar a la API de Google.

### Resolución de Problemas Comunes
> [!IMPORTANT]
> **Error 403: Insufficient Permission / ACCESS_TOKEN_SCOPE_INSUFFICIENT**
> Si el sistema arroja errores 403 al intentar sincronizar tareas, generalmente es porque el usuario vinculó su cuenta de Google antes de que se añadiera el alcance (scope) de `tasks`. 
> **Solución:** El usuario debe cerrar sesión en el ERP y volver a iniciar sesión mediante Google para que se le soliciten y otorguen los nuevos permisos necesarios para gestionar tareas.

---

### Backend
*   **`TaskController`:** CRUD de tareas, gestión de preguntas dinámicas.
*   **`Api\MobileTaskController`:** Endpoints optimizados para el consumo desde el aplicativo móvil.
    *   `GET /api/mobile/tasks`: Lista de tareas asignadas.
    *   `POST /api/mobile/tasks/{id}/advance`: Reporte de avance.
    *   `GET /api/mobile/tasks/{id}/questions`: Preguntas de cierre.
    *   `POST /api/mobile/tasks/{id}/complete`: Finalización de tarea.
*   **`TaskAdvanceService`:** Lógica de negocio para reportes de avance y carga de archivos a Drive.
*   **`TicketController`:** Lógica compartida de asignación de usuarios (`assignUsers`).
*   **`GoogleTasksService`:** Comunicación con la API de Google.

### Frontend (Android)
*   **`TasksActivity`:** Lista de tareas asignadas (RecyclerView).
*   **`TaskDetailActivity`:** Detalles de la tarea y acciones.
*   **`TaskAdvanceDialog`:** Interfaz para reportar avance.
*   **`TaskCompleteDialog`:** Interfaz dinámica para responder preguntas de cierre.
*   **`TaskRepository`:** Gestión de datos y llamadas a la API.

### Frontend
*   **`sub-work-orders-tasks.tsx`:** Vista principal, gestión de tareas y visualización de reportes/avances.
*   **`assign-to-task-sheet.tsx`:** Componente crítico que muestra la disponibilidad de los técnicos en un calendario interactivo antes de confirmar la asignación.
*   **`TaskQuestionConfig.tsx`:** Interfaz para definir las preguntas de cierre.

---

## 9. Estructura de Datos (Ticket Content)

El campo JSON `content` de un ticket de tipo tarea tiene la siguiente estructura esperada:

```json
{
    "type": "task",
    "name": "Nombre de la tarea",
    "description": "Detalles técnicos...",
    "sub_work_order_id": 123,
    "start_time": "2023-10-27T08:00:00",
    "end_time": "2023-10-27T12:00:00",
    "vehicle_id": 45, // Opcional
    "google_task_owner_user_id": 1, // ID del creador para sync
    "google_start_task_id": "...", // ID tarea Google (Inicio)
    "google_end_task_id": "..." // ID tarea Google (Fin)
}
```
