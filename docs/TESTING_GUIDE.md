# Guía de Testing para ANS-APP

## Descripción General

Esta aplicación incluye tests a múltiples niveles siguiendo estándares de la industria:
- **Unit Tests**: Pruebas de lógica individual (Robolectric + Mockito)
- **Instrumented Tests**: Pruebas en emulador (Espresso + AndroidJUnit4)
- **Integration Tests**: Pruebas de flujos completos

---

## Estructura de Tests

```
app/src/
├── test/                          # Unit Tests (JVM local)
│   └── java/.../
│       ├── FCMServiceTest.java
│       └── ...
│
└── androidTest/                   # Instrumented Tests (Emulator/Device)
    └── java/.../
        ├── NotificationButtonWidgetTest.java
        ├── MainActivityTest.java
        └── ...
```

---

## Dependencias de Testing

### Unit Tests (test/)
- **JUnit 4**: Framework base para testing
- **Mockito**: Creación de mocks y stubs
- **Robolectric**: Emulación de Android runtime en JVM

### Instrumented Tests (androidTest/)
- **AndroidJUnit4**: Runner para tests en Android
- **Espresso**: Testing de UI
- **AndroidX Test**: Utilidades de testing

---

## Ejecutar Tests desde Android Studio

### 1. Unit Tests (Local JVM)
```bash
# Opción 1: Desde Android Studio
- Click derecho en carpeta "test"
- Select "Run Tests in 'test'"

# Opción 2: Desde terminal
./gradlew test
```

### 2. Instrumented Tests (Emulator)
```bash
# Opción 1: Desde Android Studio
- Emulator debe estar ejecutándose
- Click derecho en carpeta "androidTest"
- Select "Run Android Tests"

# Opción 2: Desde terminal
./gradlew connectedAndroidTest
```

### 3. Todos los Tests
```bash
./gradlew test connectedAndroidTest
```

---

## Tests Creados

### 1. FCMServiceTest (Unit Test)
**Archivo**: `app/src/test/java/.../FCMServiceTest.java`

**Propósito**: Probar la lógica de recepción de notificaciones FCM

**Casos de Prueba**:
- ✓ Mensaje con ticket_id válido
- ✓ Mensaje sin ticket_id (manejo de error)
- ✓ Refresh de token FCM

**Ejecución**:
```bash
./gradlew testDebugUnitTest --tests "*FCMServiceTest*"
```

---

### 2. NotificationButtonWidgetTest (Instrumented Test)
**Archivo**: `app/src/androidTest/java/.../NotificationButtonWidgetTest.java`

**Propósito**: Probar widget de botón de notificaciones

**Casos de Prueba**:
- ✓ Inicialización correcta del widget
- ✓ Badge oculto cuando count = 0
- ✓ Badge visible cuando count > 0
- ✓ Badge muestra "99" cuando count > 99
- ✓ Inflación correcta del layout

**Ejecución**:
```bash
./gradlew connectedAndroidTest --tests "*NotificationButtonWidgetTest*"
```

---

### 3. MainActivityTest (Instrumented Test - Espresso)
**Archivo**: `app/src/androidTest/java/.../MainActivityTest.java`

**Propósito**: Probar interacciones UI en MainActivity

**Casos de Prueba**:
- ✓ Botón de notificación visible
- ✓ Botón de notificación clickeable
- ✓ Layout principal carga correctamente

**Ejecución**:
```bash
./gradlew connectedAndroidTest --tests "*MainActivityTest*"
```

---

## Cobertura de Código

Para generar reporte de cobertura:

```bash
./gradlew jacocoTestReport
```

El reporte se generará en: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

---

## Mejores Prácticas Implementadas

### 1. Separación de Concerns
- Unit tests para lógica pura (FCMService)
- Instrumented tests para UI (MainActivity)
- Mocks para dependencias externas

### 2. Naming Convention
```java
// Método de test: testWhatIsBeingTested_GivenWhat_ExpectWhat
testBadgeUpdateWithZeroCount()       // Claro y descriptivo
testOnMessageReceivedWithValidTicketId()
```

### 3. Setup/Teardown
```java
@Before
public void setUp() {
    // Preparar antes de cada test
}

@After
public void tearDown() {
    // Limpiar después de cada test
}
```

### 4. Assertions
Usar assertions claros:
```java
assertTrue("Badge should be hidden", condition);
assertNotNull("Widget should exist", widget);
assertEquals("Expected value", actual, expected);
```

---

## CI/CD Integration

Para integración continua, agregar en tu pipeline:

```yaml
# Ejemplo GitHub Actions (.github/workflows/android.yml)
- name: Run Unit Tests
  run: ./gradlew test
  
- name: Run Instrumented Tests
  run: ./gradlew connectedAndroidTest
  
- name: Generate Coverage Report
  run: ./gradlew jacocoTestReport
```

---

## Troubleshooting

### Error: "Emulator not found"
```bash
# Asegúrate de tener el emulator ejecutándose
./gradlew connectedAndroidTest
```

### Error: "Test found but not executed"
```bash
# Verifica que la clase esté en el paquete correcto
# androidTest debe estar en: app/src/androidTest/java/com/example/app_ans/
```

### Error: "RobolectricTestRunner not found"
```bash
# Ejecuta: ./gradlew clean build
# Espera a que descargue dependencias
```

---

## Próximos Pasos

1. **Ampliar cobertura**: Agregar más tests para TaskViewModel, NotificationViewModel
2. **Tests de integración**: Pruebas de flujo completo (FCM → DB → UI)
3. **Performance tests**: Medir tiempo de renderizado del widget
4. **Screenshot tests**: Comparación visual de layouts

---

## Referencias

- [Android Testing Guide](https://developer.android.com/training/testing)
- [Espresso Testing](https://developer.android.com/training/testing/espresso)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core)
- [Robolectric](http://robolectric.org/)

---

**Última Actualización**: Enero 8, 2026
