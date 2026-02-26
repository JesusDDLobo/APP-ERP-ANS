# Documentación de Manejo de Datos, Tablas y Filtros (ANS-ERP)

## Visión General

El sistema ANS-ERP ha migrado de una arquitectura de "Carga Total" (Client-Side Rendering) a una arquitectura robusta de **Paginación y Filtrado en el Servidor (Server-Side Pagination)**.

*   **Actualmente**: El frontend solicita solo la página actual (ej. 10 registros) y delegamos al motor de base de datos (MySQL) la responsabilidad de filtrar, ordenar y paginar.

---

## 1. Flujo de Datos

El flujo de información sigue el patrón de "Single Source of Truth" basado en la URL.

1.  **Estado en URL**: Los filtros activos (búsqueda, página, ordenamiento) se reflejan en los *Query Parameters* de la URL (ej. `?page=2&search=juan&sort=name&direction=asc`).
2.  **Solicitud (Inertia)**: Al cambiar un filtro, el frontend hace una petición `router.get()` preservando el estado, lo que recarga solo los datos (`props`) necesarios.
3.  **Procesamiento (Backend)**: El controlador captura los parámetros, construye la consulta SQL dinámica y retorna un objeto `LengthAwarePaginator`.
4.  **Renderizado**: La vista recibe `data` (array reducido) y `meta` (información de paginación) para renderizar la tabla.

---

## 2. Implementación Backend

### 2.1. Patrón de Consulta (Query Builder)

Para mantener los controladores limpios, se utiliza un método auxiliar (ej. `makeClientQuery`) o Scopes que centralizan la lógica de filtrado.

**Ejemplo (`ClientsController.php`):**

```php
protected function makeClientQuery(Request $request): array
{
    // 1. Captura y saneamiento de parámetros
    $search = trim((string) $request->input('search', ''));
    $perPage = (int) $request->input('perPage', 10);
    $sort = (string) $request->input('sort', 'name');
    $direction = $request->input('direction') === 'desc' ? 'desc' : 'asc';

    // 2. Construcción del Query Builder
    $query = Client::query()
        ->select(['id', 'name', 'email', ...]) // Select optimizado
        ->when($search, function ($q) use ($search) {
            // Lógica de búsqueda avanzada (OR where)
            $q->where('name', 'like', "%$search%")
              ->orWhere('email', 'like', "%$search%");
        })
        ->orderBy($sort, $direction);

    // 3. Retorno del Builder y los filtros aplicados (para devolver al front)
    return [$query, compact('search', 'perPage', 'sort', 'direction')];
}
```

### 2.2. Endpoints

*   **Index (`GET /clients`)**:
    *   Usa `$query->paginate($perPage)->withQueryString()`.
    *   Retorna una vista Inertia con `clients` (paginado) y `filters` (estado actual).
*   **Exportación (`GET /clients/export`)**:
    *   Endpoint JSON dedicado para descargas masivas.
    *   Reutiliza la misma lógica de `makeClientQuery` pero usa `$query->limit(5000)->get()` en lugar de paginar.
    *   Evita sobrecargar la vista principal con datos que no se están mostrando.

### 2.3. Infraestructura Compartida (`ServerTables`)

Para evitar duplicar lógica de filtros/listados en cada módulo, ahora contamos con un paquete genérico ubicado en `app/Support/ServerTables`:

*   **`ServerTableFilters`**: Reemplaza los DTOs personalizados. Recibe la `Request` + configuración declarada en cada tabla y retorna `search`, `perPage`, `sort`, `direction` sanos.
*   **`TableDefinition`**: Describe el comportamiento de la tabla (relaciones a cargar, columnas buscables, mapa de ordenamiento, query base, límites de exportación y transformadores).
*   **`TableContext`**: Transporta información del proceso/usuario/director para aplicar restricciones comunes (ej. limitar a asignaciones).
*   **`ProcessTicketTableService`**: Servicio único que aplica filtros, búsqueda, ordenamiento, paginación y exportación usando la definición previa.

Cada módulo solo necesita declarar su propia definición. Ejemplo simplificado (`PropositionTableDefinition`):

```php
public static function make(): TableDefinition
{
    return new TableDefinition(
        name: 'propositions',
        filterOptions: [
            'per_page' => ['default' => 10, 'allowed' => [5, 10, 20, 50, 100]],
            'sort' => ['default' => 'created_at', 'allowed' => ['created_at', 'public_id']],
            'direction' => ['default' => 'desc', 'allowed' => ['asc', 'desc']],
        ],
        relations: ['sender', 'receiver', 'state', 'process', 'documents'],
        searchColumns: ['public_id', 'content', 'sender.name', 'receiver.name', 'state.value'],
        sortMap: [
            'created_at' => 'created_at',
            'public_id' => 'public_id',
        ],
        rowTransformer: [self::class, 'mapTicket'],
        restrictToAssignments: false,
        baseQuery: fn ($query) => $query->whereNull('previous_request'),
        collectionTransformer: [self::class, 'transformCollection'],
    );
}
```

El controlador queda reducido a obtener filtros + contexto y delegar todo al servicio:

```php
$definition = PropositionTableDefinition::make();
$filters = ServerTableFilters::fromRequest($request, $definition->filterOptions);
$context = TableContext::make(PropositionTableDefinition::processId(), Auth::id(), true);

return Inertia::render('comercial/propositions', [
    'propositions' => $this->tableService->paginate($definition, $filters, $context),
    'filters' => $filters->toArray(),
]);
```

### 2.4. Tickets por Proceso (Diseño, Logística, Infraestructura, Jurídico)

Los módulos que comparten la tabla genérica `TicketsTable` fueron migrados al mismo stack:

* **Diseño**: `DesignTableDefinition` restringe los listados al personal asignado (salvo directores) y expone al frontend un `LengthAwarePaginator` junto con los filtros activos.
* **Logística, Infraestructura, Jurídico**: cada módulo declara su `XTableDefinition` (ubicado en `app/Support/<Module>`), pero delega la construcción del objeto a `ProcessTicketDefinitionFactory` y reutiliza el `ProcessTicketRowTransformer` común en `app/Support/ProcessTickets`.
* Todos los controladores (`DesignController`, `LogisticsController`, `InfrastructureController`, `LegalController`) ahora utilizan el mismo `ProcessTicketTableService` tanto para `index` como para `export`.
* Cada módulo tiene un endpoint `GET <modulo>/export` registrado en `routes/web.php`, lo que permite que el frontend descargue "todos los registros" aplicando exactamente los mismos filtros/sorts de la página actual.

En el frontend, las vistas `resources/js/pages/design`, `logistics`, `infrastructure` y `legal` mantienen su barra de búsqueda y ordenamiento, pero toda la lógica se reduce a sincronizar los *query params* vía `router.get`. `TicketsTable` recibe ahora `sortKey`, `sortDirection` y `fetchAllItems`, por lo que exportar respeta el mismo pipeline de filtros sin necesidad de copiar lógica en el cliente.

---

## 3. Implementación Frontend

### 3.1. Gestión del Estado (Páginas)

Las páginas (ej. `Clients.tsx`) actúan como orquestadores. No filtran datos localmente, sino que sincronizan la UI con el Backend.

*   **Hooks**: `useCallback` y `useEffect` para manejar el *debounce* de la búsqueda.
*   **Router**: Uso de `router.get` con `preserveState: true` para una experiencia tipo SPA fluida.

```typescript
const submitFilters = (overrides) => {
    router.get('/clients', { ...currentFilters, ...overrides }, {
        preserveState: true,
        preserveScroll: true,
        replace: true // Evita llenar el historial del navegador
    });
};
```

### 3.2. Componente `DataTableWithExport`

Este componente ha sido refactorizado para soportar dos modos:

1.  **Modo Cliente (Legacy)**: Recibe todos los datos, ordena y pagina en memoria.
2.  **Modo Servidor (Nuevo)**:
    *   Recibe `sortState` y `onSortChange`: Delega el ordenamiento al padre (Backend).
    *   Recibe `fetchAllItems`: Una función asíncrona (Promesa) para descargar todos los datos solo cuando el usuario pide "Exportar Todo".

### 3.3. Componentes Auxiliares

*   **`PaginationControls`**: Renderiza los botones de navegación basándose en `meta` (total, current_page, last_page) provisto por Laravel.
*   **`SearchBar`**: Ahora es un componente controlado (recibe `value`) para mantenerse sincronizado con la URL si se recarga la página.

---

## 4. Estrategia de Exportación a Excel

Para evitar problemas de memoria en el navegador al exportar grandes volúmenes:

1.  **Exportar Página Actual**: Usa los datos que ya están en pantalla (`paginatedItems`). Es instantáneo.
2.  **Exportar Todo**:
    *   El frontend llama a `fetchAllItems` (que apunta a `/clients/export`).
    *   El backend ejecuta la consulta con los **mismos filtros activos** pero sin paginación (con un límite seguro, ej. 5000).
    *   El frontend recibe el JSON, genera el Excel con `exceljs` y lo descarga.
    *   Esto mantiene la carga inicial de la página ligera.

---

## 5. Guía para Migrar Nuevos Módulos

1.  **Backend**:
    *   Crear `XTableDefinition` con:
        *   `filterOptions` para perPage/sort/direction.
        *   `relations`, `searchColumns`, `sortMap` y `rowTransformer` con las columnas específicas del módulo.
        *   `baseQuery` opcional (ej. "solo tickets raíz"), `collectionTransformer` si se requiere lógica multi-registro.
    *   En el controlador, usar `ServerTableFilters::fromRequest` + `ProcessTicketTableService` para `index` y `export`.
    *   Definir `TableContext::make($processId, $userId, $isDirector)` para compartir reglas de permisos.
2.  **Frontend**:
    *   Mantener los filtros en la URL y reutilizar `DataTableWithExport` con `sortState`, `onSortChange` y `fetchAllItems`.
    *   Solo renderizar las columnas que exponga el backend en su `rowTransformer`.

Con este enfoque, agregar columnas únicas (ej. `deadline` en Diseño) es tan simple como actualizarlas en su `TableDefinition`, sin tocar el servicio compartido ni otros módulos.
