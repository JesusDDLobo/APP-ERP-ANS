# Configuración Backend para Push Notifications - ANS-APP

## CONTEXTO DEL SISTEMA

La aplicación móvil Android (ANS-APP) ha implementado un sistema completo de notificaciones push usando Firebase Cloud Messaging (FCM). Cuando el usuario inicia sesión en la app móvil, el dispositivo obtiene automáticamente un token FCM único que debe ser registrado en el backend.

El objetivo es que **cuando se asigne una nueva tarea a un usuario, el backend envíe una notificación push al dispositivo de ese usuario** de forma que:
1. Aparezca una notificación visual en el dispositivo (incluso si la app está cerrada)
2. Se almacene en el historial local de la app
3. Se muestre un badge rojo con el contador en el icono de notificaciones

---

## FLUJO ESPERADO

1. **Usuario inicia sesión en la app móvil**
   - El dispositivo obtiene un token FCM de Google
   - La app envía ese token al backend en una petición POST
   - El backend lo almacena asociado a ese usuario

2. **Supervisor asigna una tarea en el sistema**
   - Se crea la asignación en la BD (relación usuario-tarea)
   - El backend detecta que se asignó una tarea a un usuario
   - El backend busca el token FCM de ese usuario
   - El backend envía un mensaje push a través de Firebase
   - El mensaje contiene: título, descripción e ID de la tarea

3. **Dispositivo recibe la notificación**
   - Aparece una notificación visual (incluso si app está cerrada)
   - Si la app está abierta, también se procesa internamente
   - Se almacena en la base de datos local del dispositivo
   - El badge de notificaciones se actualiza automáticamente

4. **Usuario interactúa con la notificación**
   - Toca la notificación en el centro de notificaciones
   - La app abre directamente la tarea asignada
   - La notificación se marca como leída

---

## COMPONENTES QUE NECESITAMOS DEL BACKEND

### 1. ALMACENAMIENTO DE TOKENS FCM
- La tabla `users` debe tener un campo para guardar el token FCM
- Debe guardar también la fecha/hora en que se actualizó el token
- Un usuario puede cambiar de dispositivo, así que el token debe ser actualizable

### 2. ENDPOINT PARA REGISTRAR TOKENS
- Un endpoint POST que reciba el token FCM desde la app móvil
- Solo usuarios autenticados pueden llamarlo (usar Sanctum)
- Debe validar y guardar el token en la tabla de usuarios

### 3. INTEGRACIÓN CON FIREBASE
- El backend necesita conectarse a Firebase usando credenciales de servicio
- Esto requiere descargar un JSON de credenciales desde Firebase Console
- Necesita una librería/SDK que permita enviar mensajes a través de Firebase

### 4. LÓGICA DE ENVÍO DE NOTIFICACIONES
- Cuando se asigne una tarea a un usuario, verificar si tiene token FCM
- Si tiene token, enviar un mensaje push con:
  - Título: "Nueva Tarea Asignada"
  - Mensaje: el nombre de la tarea
  - ID de la tarea (para que el móvil sepa a cuál abrir)
  - Tipo: "task_assigned" (para que el móvil lo procese correctamente)
- Manejar casos donde el envío falla (token expirado, usuario sin token, etc.)

### 5. (OPCIONAL) HISTORIAL DE NOTIFICACIONES
- Una tabla para guardar un registro de todas las notificaciones enviadas
- Esto permite que la app muestre "notificaciones antiguas" si lo necesita
- Útil también para auditoría y debugging

### 6. ENDPOINT PARA MARCAR COMO LEÍDAS (OPCIONAL)
- Si se desea sincronizar el estado de lectura desde el móvil al backend
- El móvil podría enviar un POST indicando que marcó una notificación como leída

---

## DECISIONES ARQUITECTÓNICAS QUE EL BACKEND DEBE TOMAR

1. **¿Dónde se envían las notificaciones?**
   - En el endpoint de asignación de tareas (síncrono)
   - En una cola de trabajo en background (asíncrono)
   - En un evento/listener que se dispara cuando se asigna

2. **¿Qué hacer si un token es inválido?**
   - Intentar de nuevo varias veces
   - Marcar el token como expirado
   - Eliminar el token de la BD

3. **¿Guardar historial de notificaciones?**
   - Solo los datos para auditoría
   - Información completa para que el móvil consulte
   - No guardar si no es necesario

4. **¿Controlar permisos de notificación?**
   - Algunos usuarios podrían no querer recibir notificaciones
   - Agregar un campo booleano en la tabla de usuarios para deshabilitar

5. **¿Manejar múltiples dispositivos por usuario?**
   - Un usuario podría tener tokens de varios dispositivos (ej: celular + tablet)
   - La arquitectura actual almacena UN SOLO token por usuario (se sobrescribe)
   - Si un usuario inicia sesión en otro dispositivo, el token anterior se invalida
   - Decidir si esto es aceptable o si necesitan soporte para múltiples dispositivos por usuario
   - Soporte para múltiples requerería crear una tabla separada de tokens con relación many-to-many

---

## DATOS DEL MENSAJE PUSH

El backend debe enviar por Firebase un mensaje con esta estructura:

```
{
  "type": "task_assigned",
  "title": "Nueva Tarea Asignada",
  "message": "Se te asignó: Nombre de la Tarea",
  "task_id": "7"
}
```

Esto es un mensaje de datos (data message), no de notificación. La app móvil se encarga de mostrar la notificación visual y guardarla localmente.

---

## CONSIDERACIONES DE PRODUCCIÓN

- Los tokens FCM pueden expirar después de cierto tiempo de inactividad
- Es recomendable que los tokens se re-registren periódicamente (cada vez que la app se abre)
- En producción, mantener las credenciales de Firebase en variables de entorno, nunca en el repositorio
- Si se esperan muchas notificaciones simultáneas, usar colas de trabajo para no bloquear las peticiones principales
- Agregar logging para poder debuggear fallos en el envío

---

## RESUMEN DE CAMBIOS NECESARIOS

✓ Agregar campo de FCM token en tabla users
✓ Crear endpoint para registrar tokens (POST /api/mobile/fcm-token)
✓ Configurar conexión a Firebase
✓ Integrar lógica de envío en el flujo de asignación de tareas
✓ Manejar errores y casos especiales
✓ (Opcional) Crear tabla de historial de notificaciones
✓ (Opcional) Crear endpoint para consultar notificaciones antiguas
✓ Agregar logging para debugging

El agente de backend tendrá el contexto suficiente para tomar las decisiones de implementación basadas en la arquitectura actual de ANS-ERP.

