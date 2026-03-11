# Kavita App - Guia de desarrollo

Cliente Android nativo para leer libros, revistas y comics desde servidores [Kavita](https://www.kavitareader.com/) y catalogos OPDS genericos.

## Requisitos previos

| Herramienta | Version minima | Notas |
|---|---|---|
| **Android Studio** | Ladybug (2024.2.1) o superior | Necesario para compilar y ejecutar |
| **JDK** | 17 | Incluido en Android Studio |
| **Android SDK** | compileSdk 35 (Android 15) | Instalar desde SDK Manager |
| **Gradle** | 8.11.1 | Gestionado por el wrapper (`./gradlew`) |
| **Kotlin** | 2.1.0 (K2) | Configurado en el proyecto |
| **Dispositivo/Emulador** | minSdk 26 (Android 8.0) | API 26 o superior |

### Instalacion de Android SDK

Desde Android Studio: **Settings > Languages & Frameworks > Android SDK**

Componentes necesarios:
- Android SDK Platform 35
- Android SDK Build-Tools 35.x
- Android SDK Command-line Tools
- Android Emulator (opcional, si no tienes dispositivo fisico)

## Primeros pasos

### 1. Clonar el repositorio

```bash
git clone <url-del-repo> kavita-app
cd kavita-app
```

### 2. Abrir en Android Studio

```
File > Open > seleccionar la carpeta kavita-app
```

Android Studio detectara automaticamente el proyecto Gradle y comenzara la sincronizacion.

### 3. Sincronizar Gradle

Si la sincronizacion no arranca sola:

```bash
./gradlew --refresh-dependencies
```

O desde Android Studio: **File > Sync Project with Gradle Files**

### 4. Compilar

```bash
# Debug
./gradlew assembleDebug

# Release (requiere configurar firma)
./gradlew assembleRelease
```

El APK de debug se genera en:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Ejecutar en dispositivo/emulador

```bash
# Instalar y ejecutar
./gradlew installDebug

# O desde Android Studio: boton Run (Shift+F10)
```

## Arquitectura del proyecto

### Estructura multi-modulo

```
kavita-app/
├── app/                          # Entry point, navegacion, DI graph
├── build-logic/convention/       # Plugins de convencion Gradle
├── core/
│   ├── core-model/               # Modelos de dominio + interfaces de repositorio (Kotlin puro)
│   ├── core-common/              # Utilidades, rutas de navegacion compartidas
│   ├── core-network/             # Retrofit APIs, DTOs, interceptores JWT, mappers
│   ├── core-database/            # Room DB, DAOs, entidades
│   ├── core-data/                # Implementaciones de repositorios, DataStore
│   ├── core-ui/                  # Theme MD3, componentes Compose compartidos, Coil
│   └── core-readium/             # Wrapper Readium (en desarrollo)
├── feature/
│   ├── feature-auth/             # Login, gestion de servidores
│   ├── feature-home/             # Inicio: continuar leyendo, recientes
│   ├── feature-library/          # Navegador biblioteca, detalle serie
│   ├── feature-search/           # Busqueda con debounce
│   ├── feature-reader/           # Lector de comics/manga (5 modos), EPUB, PDF
│   ├── feature-downloads/        # Gestor de descargas
│   ├── feature-stats/            # Estadisticas de lectura
│   ├── feature-opds/             # Navegador catalogos OPDS
│   ├── feature-admin/            # Administracion servidor Kavita
│   └── feature-settings/        # Ajustes, pantalla "Mas"
└── sync/                         # Workers: descargas, sync progreso
```

### Grafo de dependencias

```
app
 ├── feature-* (todos)
 │    ├── core-model
 │    ├── core-data
 │    ├── core-ui
 │    └── core-common
 ├── core-data
 │    ├── core-model
 │    ├── core-network
 │    ├── core-database
 │    └── core-common
 └── sync
      ├── core-network
      └── core-database
```

### Plugins de convencion

En `build-logic/convention/` hay 6 plugins que evitan duplicacion en los `build.gradle.kts`:

| Plugin | ID | Uso |
|---|---|---|
| `AndroidLibraryConventionPlugin` | `kavita.android.library` | Modulos Android library (compileSdk, minSdk, Java 17) |
| `AndroidComposeConventionPlugin` | `kavita.android.compose` | Habilita Compose, anade BOM + Material3 |
| `AndroidFeatureConventionPlugin` | `kavita.android.feature` | Aplica library+compose+hilt, anade dependencias core |
| `AndroidHiltConventionPlugin` | `kavita.android.hilt` | Aplica KSP + Hilt |
| `AndroidRoomConventionPlugin` | `kavita.android.room` | Aplica Room + KSP, configura esquemas |
| `KotlinLibraryConventionPlugin` | `kavita.kotlin.library` | Modulo Kotlin puro (JVM, Java 17) |

## Stack tecnologico

| Componente | Tecnologia | Version |
|---|---|---|
| Lenguaje | Kotlin (K2) | 2.1.0 |
| UI | Jetpack Compose | BOM 2024.12.01 |
| Diseno | Material Design 3 | Con colores dinamicos |
| DI | Hilt + KSP | 2.53.1 |
| Red | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Serializacion | kotlinx.serialization | 1.7.3 |
| BD local | Room | 2.6.1 |
| Preferencias | DataStore | 1.1.1 |
| Imagenes | Coil | 2.7.0 |
| Paginacion | Paging 3 | 3.3.5 |
| Navegacion | Compose Navigation (type-safe) | 2.8.5 |
| Tareas en segundo plano | WorkManager | 2.10.0 |
| Lectura | Readium Kotlin Toolkit | 3.1.2 |
| Build | AGP + Gradle | 8.7.3 / 8.11.1 |

## Compilacion y comandos Gradle

### Comandos basicos

```bash
# Compilar debug
./gradlew assembleDebug

# Compilar release
./gradlew assembleRelease

# Instalar en dispositivo conectado
./gradlew installDebug

# Limpiar build
./gradlew clean

# Limpiar y compilar
./gradlew clean assembleDebug
```

### Verificacion de codigo

```bash
# Comprobar que compila sin errores
./gradlew compileDebugKotlin

# Lint
./gradlew lint

# Lint solo del modulo app
./gradlew :app:lint
```

### Tests

```bash
# Tests unitarios (todos los modulos)
./gradlew test

# Tests unitarios de un modulo concreto
./gradlew :core:core-data:test
./gradlew :feature:feature-auth:test

# Tests de instrumentacion (requiere dispositivo/emulador)
./gradlew connectedAndroidTest
```

### Informacion del proyecto

```bash
# Ver todas las tareas disponibles
./gradlew tasks

# Ver dependencias del modulo app
./gradlew :app:dependencies

# Ver dependencias de un modulo core
./gradlew :core:core-network:dependencies
```

## Configuracion del servidor Kavita para desarrollo

### 1. Levantar un servidor Kavita local

La forma mas rapida es con Docker:

```bash
docker run -d \
  --name kavita \
  -p 5000:5000 \
  -v /ruta/a/tus/libros:/manga \
  -v /ruta/kavita/config:/kavita/config \
  jvmilazz0/kavita:latest
```

Accede a `http://localhost:5000` para completar la configuracion inicial.

### 2. Conectar la app al servidor

- Si usas un emulador Android: la IP del host es `10.0.2.2`, asi que la URL seria `http://10.0.2.2:5000`
- Si usas un dispositivo fisico en la misma red: usa la IP local de tu ordenador (ej. `http://192.168.1.100:5000`)

### 3. API de referencia

La app usa la API REST de Kavita v0.8.9.8. Endpoints principales:

| Endpoint | Metodo | Descripcion |
|---|---|---|
| `/api/Account/login` | POST | Login con usuario/contrasena, devuelve JWT |
| `/api/Account/refresh-token` | POST | Refrescar token expirado |
| `/api/Server/server-info` | GET | Info del servidor (validacion de URL) |
| `/api/Series/v2` | POST | Listar series con filtros |
| `/api/Series/{id}/volumes` | GET | Volumenes de una serie |
| `/api/Reader/image` | GET | Imagen de una pagina (comic) |
| `/api/Reader/progress` | POST | Guardar progreso de lectura |
| `/api/Book/{id}/book-page` | GET | Pagina de EPUB/PDF |
| `/api/Image/series-cover` | GET | Portada de serie |
| `/api/Library/libraries` | GET | Listar bibliotecas |
| `/api/Stats/reading-count-by-day` | GET | Estadisticas de lectura |

Swagger completo: `http://tu-servidor:5000/swagger`

## Estructura de la base de datos

Room database (`kavita.db`) con 8 entidades:

| Entidad | Tabla | Descripcion |
|---|---|---|
| `ServerEntity` | `servers` | Servidores configurados (Kavita/OPDS) |
| `SeriesEntity` | `series` | Series cacheadas localmente |
| `ChapterEntity` | `chapters` | Capitulos cacheados |
| `ReadingProgressEntity` | `reading_progress` | Progreso de lectura (local + sync) |
| `BookmarkEntity` | `bookmarks` | Marcadores de pagina |
| `DownloadEntity` | `downloads` | Estado de descargas |
| `ReadingSessionEntity` | `reading_sessions` | Sesiones de lectura (estadisticas) |
| `OpdsFeedEntity` | `opds_feeds` | Feeds OPDS guardados |

Los esquemas de Room se exportan a `core/core-database/schemas/` para migraciones.

## Inyeccion de dependencias (Hilt)

### Modulos principales

| Modulo Hilt | Ubicacion | Proporciona |
|---|---|---|
| `NetworkModule` | `core-network/di/` | `Json`, `OkHttpClient` |
| `DatabaseModule` | `core-database/di/` | `KavitaDatabase`, todos los DAOs |
| `DataModule` | `core-data/di/` | Bindings interfaz -> implementacion de 9 repositorios |
| `KavitaImageLoaderModule` | `core-ui/image/` | `ImageLoader` de Coil con auth |

### Patron de repositorios

1. **Interfaz** en `core-model/repository/` (modulo Kotlin puro, sin dependencias Android)
2. **Implementacion** en `core-data/repository/` (acceso a red + BD)
3. **Binding** en `DataModule` (Hilt `@Binds`)

## Navegacion

Rutas type-safe definidas en `core/core-common/src/main/kotlin/com/kavita/core/common/navigation/Routes.kt`:

```kotlin
@Serializable data object HomeRoute
@Serializable data object LoginRoute
@Serializable data class SeriesDetailRoute(val seriesId: Int, val serverId: Long)
@Serializable data class ReaderRoute(val chapterId: Int, val seriesId: Int, val volumeId: Int, val format: String)
// ... etc.
```

Cada feature expone una funcion de extension `NavGraphBuilder.featureScreen(navController)` en su fichero `*Navigation.kt`.

## Lector de comics/manga

El lector soporta 5 modos de lectura:

| Modo | Clase | Descripcion |
|---|---|---|
| LTR | `HorizontalPager(reverseLayout=false)` | Izquierda a derecha |
| RTL (Manga) | `HorizontalPager(reverseLayout=true)` | Derecha a izquierda |
| Vertical | `VerticalPager` | Paso de pagina vertical |
| Webtoon | `LazyColumn` | Scroll continuo sin separacion |
| Doble pagina | `HorizontalPager` + `Row` | Dos paginas lado a lado |

Funcionalidades:
- **Zoom**: Pinch-to-zoom 1x-5x, doble-tap para zoom/reset
- **Controles overlay**: Tap en zona central para mostrar/ocultar
- **Auto-guardado**: Progreso cada 30 segundos
- **Brillo**: Capa negra semitransparente controlada por slider
- **Precarga**: 2 paginas adyacentes (`beyondViewportPageCount = 2`)

## Descargas y modo offline

- **DownloadWorker**: Foreground service con notificacion de progreso
- **Almacenamiento**: `files/downloads/{serverId}/{seriesId}/{chapterId}/`
- **Sync**: `ProgressSyncWorker` sincroniza progreso pendiente al volver online
- Paginas descargadas como ficheros individuales `page_0.jpg`, `page_1.jpg`, etc.

## Convenios de codigo

### Estructura de un feature module

```
feature-xxx/
└── src/main/kotlin/com/kavita/feature/xxx/
    ├── XxxScreen.kt           # Composable de la pantalla
    ├── XxxViewModel.kt        # ViewModel con UiState + Events
    └── XxxNavigation.kt       # Extension NavGraphBuilder
```

### Patrones

- **UiState**: `data class XxxUiState(...)` con `MutableStateFlow` en el ViewModel
- **Eventos**: `sealed interface XxxEvent` con `SharedFlow` para navegacion/acciones one-shot
- **Coleccion de estado**: `collectAsStateWithLifecycle()` en los Composables
- **Errores**: Snackbar o `ErrorScreen` composable
- **Carga**: `LoadingIndicator` composable compartido

### Idioma de la UI

La interfaz esta en **espanol de Espana** (vosotros, ordenador, etc.). Actualmente los strings estan hardcoded; en el futuro se moveran a `strings.xml` para internacionalizacion.

## Generar APK firmado (release)

### 1. Crear keystore

```bash
keytool -genkey -v -keystore kavita-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kavita
```

### 2. Configurar firma en `app/build.gradle.kts`

Anadir dentro del bloque `android {}`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../kavita-release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        keyAlias = "kavita"
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... resto de config
    }
}
```

### 3. Compilar

```bash
KEYSTORE_PASSWORD=tu_password KEY_PASSWORD=tu_password ./gradlew assembleRelease
KEYSTORE_PASSWORD=C0rm3. KEY_PASSWORD=C0rm3. ./gradlew assembleRelease
```

APK firmado en: `app/build/outputs/apk/release/app-release.apk`

## Solucion de problemas comunes

### "SDK location not found"

Crear fichero `local.properties` en la raiz:
```properties
sdk.dir=/ruta/a/tu/Android/Sdk
```

En macOS suele ser: `sdk.dir=/Users/tu-usuario/Library/Android/sdk`

### Error de memoria en Gradle

Aumentar memoria en `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### "Unresolved reference" tras sincronizar

1. **File > Invalidate Caches > Invalidate and Restart**
2. Si persiste: `./gradlew clean` y volver a compilar

### Problemas con KSP/Hilt

```bash
./gradlew clean
./gradlew kspDebugKotlin
```

### El emulador no conecta al servidor local

- Usar `10.0.2.2` en lugar de `localhost` o `127.0.0.1`
- Verificar que el servidor Kavita esta escuchando en `0.0.0.0` (no solo `localhost`)

## Tareas pendientes

- [ ] Integrar Readium Kotlin Toolkit para lector EPUB real
- [ ] Integrar Readium PdfNavigatorFragment para PDF
- [ ] Migrar parser OPDS de regex XML a readium-opds
- [ ] Anadir tests unitarios (ViewModels, Repositories, Mappers)
- [ ] Anadir tests de UI con Compose Testing
- [ ] Internacionalizar strings (strings.xml)
- [ ] Implementar cache de imagenes offline
- [ ] Racha de lectura y objetivo diario en estadisticas
- [ ] Soporte para colecciones y listas de lectura
- [ ] Gestion de usuarios en panel de administracion
- [ ] CI/CD con GitHub Actions
