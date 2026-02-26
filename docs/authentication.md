# Documentación del Sistema de Autenticación (ANS-ERP)

## Visión General

El sistema de autenticación de ANS-ERP utiliza un enfoque **híbrido con prioridad en Google (Google-First)**. Esto significa que la fuente de verdad para la identidad de los usuarios es el directorio de Google Workspace de la organización.

*   **Primer Acceso**: Obligatoriamente a través de Google OAuth.
*   **Accesos Subsecuentes**: Pueden realizarse vía Google OAuth o mediante credenciales locales (correo/contraseña), siempre que la cuenta ya esté vinculada.
*   **Sincronización**: Los tokens de acceso a servicios de Google (Drive, Calendar) se actualizan en cada inicio de sesión vía Google y se almacenan para su uso en segundo plano.

---

## 1. Flujos de Autenticación

### 1.1. Inicio de Sesión con Google (Principal)

Este es el método principal y obligatorio para el registro y primer acceso de cualquier usuario.

*   **Ruta**: `/auth/redirect` -> `/auth/callback`
*   **Controlador**: `App\Http\Controllers\Auth\AuthenticatedSessionController`

**Proceso:**
1.  El usuario es redirigido a Google con scopes para `email`, `profile`, `drive`, `calendar`, `chat`. Se fuerza `access_type=offline` para obtener `refresh_token`.
2.  Al retornar (`handleGoogleCallback`), el sistema:
    *   Consulta la API corporativa (`api.grupoans.com.co`) para enriquecer el perfil (cargo, área).
    *   Busca al usuario en la tabla `users` por `email`.
    *   **Create/Update**: Si no existe, lo crea. Si existe, actualiza sus datos y tokens.
    *   **Tokens**: Almacena `google_access_token` y `google_refresh_token` en la base de datos.
    *   **Marcador de vínculo**: Persistimos `google_linked_at` (fecha del primer OAuth exitoso) para habilitar el acceso local incluso si Google está caído.
    *   **Bandera de Contraseña**: Si el usuario no tiene contraseña local (`password` es null), establece `must_change_password = true`.

### 1.2. Configuración de Contraseña (Obligatorio)

Garantiza que todos los usuarios tengan un método de acceso de respaldo local.

*   **Trigger**: Bandera `must_change_password` en `true` al iniciar sesión.
*   **Frontend**: `resources/js/components/ui/ForcePasswordChangeSheet.tsx` (vía `Dashboard`).
*   **Endpoint**: `POST /user/force-password-change`

**Proceso:**
1.  El Dashboard detecta la bandera y bloquea la interfaz con un modal.
2.  El usuario define una contraseña segura.
3.  El backend guarda el hash (`Hash::make`) y establece `must_change_password = false`.

### 1.3. Inicio de Sesión Local (Secundario)

Permite el acceso tradicional para usuarios ya vinculados.

*   **Ruta**: `POST /login`
*   **Controlador**: `AuthenticatedSessionController@store`

**Reglas de Validación:**
El sistema rechaza el login local si:
1.  El usuario no existe.
2.  El usuario nunca completó Google OAuth (`google_linked_at` es `null`).
3.  La contraseña es incorrecta.
4.  Tiene pendiente el cambio de contraseña (`must_change_password`).

Si el usuario ya estaba vinculado pero Google no responde o los tokens caducaron, el login local se permite igualmente. Al finalizar la sesión, el backend intenta refrescar los tokens:

* **Éxito**: se limpian los contadores de fallo y no se muestra ningún aviso.
* **Fallo**: se marca el estado `google_services_status = down`, se dispara un Sheet emergente (vía `GoogleServicesStatusSheet`) avisando que Drive/Calendar/Chat estarán temporalmente no disponibles y se agenda un Job para reintentar.

El frontend sólo se limita a mostrar/ocultar los Sheets según el estado calculado por el backend.

### 1.4. Recuperación de Contraseña

El sistema permite restablecer la contraseña tanto desde fuera (login) como desde dentro de la aplicación (perfil de usuario).

*   **Rutas**: `/forgot-password` (POST), `/reset-password` (GET/POST).
*   **Controladores**: `PasswordResetLinkController`, `NewPasswordController`, `PasswordResetController`.

**Escenarios:**

1.  **Desde Login (Externo)**:
    *   El usuario ingresa su correo en el `ForgotPasswordSheet` (variante `default`).
    *   Se envía el enlace de recuperación estándar.

2.  **Desde Perfil de Usuario (Interno)**:
    *   Accesible desde el Header o el Directorio de Usuarios (`UserDetailSheet`).
    *   Usa `ForgotPasswordSheet` en variante `confirm`.
    *   No solicita el correo (usa el del usuario autenticado o seleccionado) y pide confirmación antes de enviar el enlace.

3.  **Restablecimiento (Click en el enlace)**:
    *   Ruta `GET /reset-password`.
    *   **Seguridad**: Si el usuario tiene una sesión activa al hacer clic en el enlace, el sistema **fuerza el cierre de sesión** (`Auth::logout`) e invalida la sesión antes de mostrar el formulario. Esto evita conflictos con la sesión actual y asegura que el cambio de contraseña se procese en un contexto limpio (guest).
    *   El formulario envía la nueva contraseña a `POST /reset-password` (ruta `password.store`), que actualiza el modelo `User` y limpia la bandera `must_change_password`.

---

## 2. Integración con Servicios de Google (Drive)

La autenticación no solo sirve para el acceso al sistema, sino para autorizar operaciones en Drive.

*   **Almacenamiento**: Los tokens OAuth se guardan en la tabla `users`.
*   **Clientes y servicios auxiliares**:
    * `App\Services\GoogleDriveClient`, `GoogleCalendarService`, `GoogleChatService` usan los tokens almacenados.
    * `App\Services\GoogleTokenRefreshService` centraliza el refresco inmediato y programado.
*   **Uso**:
    *   Cuando el sistema necesita subir/editar archivos, instancia el cliente de Drive.
    *   Si está configurado en modo `oauth`, recupera los tokens del usuario de la base de datos.
    *   Si el token ha expirado, usa el `google_refresh_token` almacenado para obtener uno nuevo automáticamente sin intervención del usuario.
    *   Si los tokens no están disponibles, se mantienen bloqueadas las operaciones en segundo plano y se informa al usuario mediante un Sheet hasta que el Job programado vuelva a obtener credenciales válidas.
*   **Reintentos automáticos**: El comando `google:tokens:refresh-pending` (agendado cada 5 minutos) encola el Job `AttemptGoogleTokenRefresh` para cualquier usuario con fallos recientes. Cuando un reintento tiene éxito, se envía un nuevo Sheet avisando que los servicios se han restaurado.

---

## 3. Estructura de Base de Datos

### Tabla `users`

Columnas críticas para el funcionamiento de la autenticación:

| Columna | Tipo | Descripción |
| :--- | :--- | :--- |
| `email` | `string` | Identificador único y principal. |
| `password` | `string` | Hash de la contraseña local. Puede ser `null` antes del primer setup. |
| `google_id` | `string` | ID único del usuario en Google. |
| `google_access_token` | `text` | Token de corta duración para API. |
| `google_refresh_token` | `text` | Token de larga duración para renovar acceso. |
| `google_token_expires_in` | `int` | Timestamp de expiración del token. |
| `google_linked_at` | `timestamp` | Fecha del primer vínculo exitoso mediante Google OAuth. |
| `google_services_status` | `string` | `ok` o `down` según la última evaluación de Google. |
| `google_services_message` | `text` | Mensaje que se muestra en el Sheet emergente. |
| `google_status_requires_ack` | `boolean` | Controla si el Sheet debe mostrarse (pendiente de confirmación del usuario). |
| `google_refresh_failure_count` | `tinyint` | Contador de reintentos fallidos para generar alertas. |
| `google_status_changed_at` | `timestamp` | Última transición de estado. |
| `google_tokens_checked_at` | `timestamp` | Última vez que se intentó refrescar el token. |
| `must_change_password` | `boolean` | Controla la aparición del modal de configuración de contraseña. |

---

## 4. Archivos Clave

### Backend
*   `routes/web.php`: Definición de rutas de autenticación y callbacks.
*   `routes/auth.php`: Rutas estándar de recuperación de contraseña.
*   `app/Http/Controllers/Auth/AuthenticatedSessionController.php`: Lógica central de login (Google y Local).
*   `app/Services/GoogleTokenRefreshService.php`: Refresco automático e informadores de estado.
*   `app/Jobs/AttemptGoogleTokenRefresh.php` y `app/Console/Commands/RefreshUserGoogleTokens.php`: Reintentos en background.
*   `app/Services/GoogleDriveClient.php`: Consumidor de los tokens almacenados.

### Frontend
*   `resources/js/pages/auth/login.tsx`: Pantalla de inicio de sesión.
*   `resources/js/pages/dashboard.tsx`: Punto de montaje para la validación de `must_change_password`.
*   `resources/js/components/ui/ForcePasswordChangeSheet.tsx`: UI para establecer contraseña.
*   `resources/js/components/ui/GoogleServicesStatusSheet.tsx`: Mensajes emergentes sobre la disponibilidad de Google.
*   `resources/js/components/ui/sheets/user-detail-sheet.tsx`: Ficha de detalle de usuario con opción de reset.
*   `resources/js/components/ui/sheets/forgot-password-sheet.tsx`: Componente compartido para solicitar reset (modos default/confirm).
---

## 5. Aplicativo Móvil (Android)

El aplicativo móvil extiende el sistema de autenticación híbrido para dispositivos Android, utilizando Retrofit para la comunicación con el backend y persistencia segura local.

### 5.1. Flujos de Autenticación Móvil

*   **Google Sign-In**: Implementado mediante Google Play Services. La app obtiene un `serverAuthCode` que es enviado al backend para su intercambio por tokens JWT.
*   **Login Local**: Permite el acceso con correo y contraseña.
*   **Gestión de Sesión**: Los tokens (`access_token`, `refresh_token`) y banderas de estado se almacenan localmente de forma segura.

### 5.2. Persistencia y Seguridad

Se utiliza la clase `AuthSessionManager` para gestionar la persistencia. Los datos se guardan en `SecurePreferences` (SharedPreferences encriptadas):
*   `access_token`: Token de acceso para las peticiones API.
*   `must_change_password`: Bandera que fuerza la aparición del diálogo de configuración de contraseña.
*   `google_services_status`: Estado actual de los servicios de Google del usuario.
*   `google_status_requires_ack`: Indica si el usuario debe confirmar que ha leído un aviso de error de Google.

### 5.3. Interfaz de Usuario (UI) y Reactividad

*   **MainActivity**: Actúa como observador de los estados de sesión. Si detecta `must_change_password`, muestra un banner superior y bloquea la navegación hasta que se complete el `PasswordSetupDialog`.
*   **AuthActivity**: Pantalla inicial de autenticación que gestiona tanto el flujo de Google como el formulario local.
*   **Logout**: El proceso de cierre de sesión es atómico y asíncrono. Asegura:
    1.  Cierre de sesión en el cliente de Google (`signOut()`).
    2.  Petición al backend para invalidar el token.
    3.  Limpieza total de `SecurePreferences`.
    4.  Redirección inmediata a `AuthActivity` limpiando el stack de actividades.

### 5.4. Endpoints de la API Móvil

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/mobile/auth/google` | Intercambia el `serverAuthCode` de Google por sesión. |
| `POST` | `/api/mobile/auth/login` | Inicio de sesión local. |
| `POST` | `/api/mobile/auth/password-setup` | Configuración inicial de contraseña (cuando `must_change_password` es true). |
| `POST` | `/api/mobile/auth/logout` | Invalida el token en el servidor. |
| `POST` | `/api/mobile/auth/status` | Confirma la lectura de avisos de estado de Google. |
| `POST` | `/api/mobile/auth/forgot-password` | Solicita enlace de recuperación. |

### 5.5. Archivos Clave (Mobile)

*   `com.example.app_ans.auth.session.AuthSessionManager`: Manejo de SharedPreferences.
*   `com.example.app_ans.auth.repository.AuthRepository`: Orquestador de peticiones y persistencia.
*   `com.example.app_ans.auth.network.AuthApi`: Definición de endpoints de Retrofit.
*   `com.example.app_ans.auth.ui.AuthViewModel`: Lógica de negocio y reactividad para la UI.
