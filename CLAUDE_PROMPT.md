# Prompt de continuacion para Claude

Copia y pega este prompt para retomar el desarrollo del proyecto en otra sesion de Claude. Puedes anadir al final las tareas concretas que quieras que haga.

---

## Prompt

```
Continua implementando el proyecto kavita-app Android en /Users/mateo.fuentes/Repos/Mios/kavita-app.

## Stack tecnologico

- Kotlin 2.1.0 (K2) + Jetpack Compose nativo
- Arquitectura multi-modulo clean (19 modulos Gradle)
- Build: AGP 8.7.3, Gradle 8.11.1, convention plugins en build-logic/
- DI: Hilt 2.53.1 con KSP
- Red: Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx.serialization 1.7.3
- BD local: Room 2.6.1 con Flow reactivo
- Preferencias: DataStore 1.1.1
- Imagenes: Coil 2.7.0 con cabecera JWT
- Paginacion: Paging 3 (3.3.5)
- Navegacion: Compose Navigation type-safe con @Serializable routes
- Background: WorkManager 2.10.0
- Lectura: Readium Kotlin Toolkit 3.1.2 (EPUB/PDF/CBZ)
- UI: Material Design 3 (BOM 2024.12.01) con colores dinamicos
- minSdk 26, targetSdk/compileSdk 35

## Modulos del proyecto

### Core
- core-model: Modelos de dominio (Server, Series, Volume, Chapter, ReadingProgress, Bookmark, ReadingStats, DownloadTask, UserPreferences, OpdsModels, AppResult) + 9 interfaces de repositorio
- core-common: Routes de navegacion compartidas (core-common/navigation/Routes.kt), ConnectivityMonitor
- core-network: 8 APIs Retrofit (Account, Series, Reader, Library, Book, Image, Stats, Server) + 15 DTOs + Mappers + AuthInterceptor JWT + ServerTokenStore + ActiveServerProvider + RetrofitFactory + NetworkModule
- core-database: Room con 8 entidades (Server, Series, Chapter, ReadingProgress, Bookmark, Download, ReadingSession, OpdsFeed) + 8 DAOs + DatabaseModule
- core-data: 9 implementaciones de repositorio (Server, Auth, Series, Reader, Library, Stats, Download, Opds, Admin) + UserPreferencesDataStore + DataModule (Hilt bindings)
- core-ui: KavitaTheme (claro/oscuro/dinamico) + 6 componentes (CoverImage, SeriesCard, ProgressBadge, LoadingIndicator, ErrorScreen, EmptyState) + KavitaImageLoaderModule
- core-readium: Placeholder para wrapper Readium

### Feature
- feature-auth: LoginScreen/ViewModel, ServerManagementScreen/ViewModel, AuthNavigation
- feature-home: HomeScreen/ViewModel (carruseles: continuar leyendo, a continuacion, recientes), HomeNavigation
- feature-library: LibraryScreen/ViewModel (grid paginado + filtros por biblioteca), SeriesDetailScreen/ViewModel (cabecera + volumenes acordeon + capitulos), navegaciones
- feature-search: SearchScreen/ViewModel (debounce 300ms, grid resultados), SearchNavigation
- feature-reader: ReaderScreen/ViewModel (auto-save 30s, deteccion formato, navegacion capitulos), ComicReader (5 modos: LTR, RTL manga, vertical, webtoon, doble pagina), ZoomablePageImage (pinch 1x-5x, doble-tap), WebtoonReader, DoublePageReader, ReaderControls (overlay superior/inferior, slider pagina, bookmark, brillo), BrightnessOverlay
- feature-downloads: DownloadsScreen/ViewModel, DownloadsNavigation
- feature-stats: StatsScreen/ViewModel (overview cards + grafico Canvas 30 dias), StatsNavigation
- feature-opds: OpdsBrowseScreen/ViewModel (navegacion jerarquica, busqueda), OpdsNavigation
- feature-admin: AdminScreen/ViewModel (info servidor, scan/refresh bibliotecas, verificacion JWT Admin), AdminNavigation
- feature-settings: SettingsScreen/ViewModel, MoreScreen (menu principal), SettingsNavigation

### Sync
- DownloadWorker: Foreground service, descarga paginas individuales, notificacion de progreso
- ProgressSyncWorker: Sincroniza progreso pendiente con el servidor

### App
- KavitaApplication (@HiltAndroidApp)
- MainActivity (single activity, edge-to-edge)
- KavitaApp (Scaffold + NavigationBar 5 tabs + StartupViewModel)
- StartupViewModel (auto-login, decide LoginRoute vs HomeRoute)
- KavitaNavHost (integra todas las *Navigation de cada feature)

## API Kavita v0.8.9.8

- Auth: POST /api/Account/login (JWT), POST /api/Account/refresh-token
- Validacion: GET /api/Server/server-info
- Series: POST /api/Series/v2 (paginado con filtros), GET /api/Series/{id}/volumes
- Lector: GET /api/Reader/image?chapterId&page, POST /api/Reader/progress, GET /api/Reader/chapter-info
- Libros: GET /api/Book/{chapterId}/book-page (EPUB/PDF)
- Imagenes: GET /api/Image/series-cover, volume-cover, chapter-cover
- Bibliotecas: GET /api/Library/libraries, POST /api/Library/scan, POST /api/Library/refresh-metadata
- Stats: GET /api/Stats/reading-count-by-day, GET /api/Stats/user/reading-history
- OPDS: parser XML basico (pendiente migrar a readium-opds)

## Patrones clave

- Rutas type-safe en core-common/navigation/Routes.kt (@Serializable)
- Cada feature expone fun NavGraphBuilder.featureScreen(navController)
- ViewModels: StateFlow<UiState> + SharedFlow<Event>
- Repositorios: interfaz en core-model, impl en core-data, binding en DataModule
- RetrofitFactory: crea instancias Retrofit por URL base (soporte multi-servidor)
- Auth: AuthInterceptor anade Bearer token, ServerTokenStore por serverId

## Estado actual

Las 10 fases de implementacion estan completas (~151 ficheros Kotlin).
El proyecto necesita abrirse en Android Studio para verificar compilacion y corregir posibles errores de imports.

## Tareas pendientes

- [x] Verificar compilacion en Android Studio y corregir errores
- [x] Integrar Readium para EPUB real (EpubNavigatorFragment en Compose via AndroidView)
- [x] Integrar Readium para PDF (PdfNavigatorFragment + PdfiumEngineProvider)
- [ ] Migrar parser OPDS a readium-opds
- [x] Tests unitarios (ViewModels, Repositories, Mappers)
- [x] Tests de UI (Compose testing)
- [ ] Cache de imagenes offline
- [ ] Racha de lectura y objetivo diario en estadisticas
- [ ] Colecciones y listas de lectura
- [ ] Gestion de usuarios en admin (/api/Users)
- [ ] Optimizar precarga de paginas en comic reader
- [ ] Ajustes tipografia EPUB (fuente, interlineado, tema sepia)
- [ ] Modo noche PDF (invertir colores con ColorMatrix)
- [ ] Internacionalizar strings (strings.xml)
- [x] CI/CD con GitHub Actions

## Idioma de la UI

Espanol de Espana (vosotros, ordenador, vale, etc.).
```

---

## Ejemplos de uso

### Para corregir errores de compilacion
```
[pegar prompt de arriba]

Abre el proyecto y compila con ./gradlew assembleDebug. Corrige todos los errores de compilacion que encuentres.
```

### Para implementar el lector EPUB
```
[pegar prompt de arriba]

Implementa el lector EPUB real usando Readium Kotlin Toolkit 3.1.2:
- EpubNavigatorFragment embebido en Compose via AndroidView
- Tabla de contenidos (ToC)
- Ajustes de tipografia: tamano fuente, familia (serif/sans/mono/dislexia), interlineado
- Temas de lectura: dia, noche, sepia
- Modos de scroll: horizontal (paginas) y vertical (scroll continuo)
```

### Para anadir tests
```
[pegar prompt de arriba]

Anade tests unitarios para:
- LoginViewModel (login exitoso, error de red, validacion de campos)
- SeriesRepositoryImpl (getSeries paginado, busqueda, mappers)
- ReaderViewModel (cambio de pagina, auto-save, navegacion capitulos)
```

### Para una tarea concreta
```
[pegar prompt de arriba]

[Describe aqui tu tarea concreta]
```
