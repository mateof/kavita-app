# Kavita App

A native Android client for reading books, magazines, and comics from [Kavita](https://www.kavitareader.com/) servers and generic OPDS catalogs.

Built with Kotlin, Jetpack Compose, and Material Design 3.

## Features

- **Multi-server support** — Connect to multiple Kavita servers and OPDS catalogs simultaneously
- **Advanced comic/manga reader** — 5 reading modes: LTR, RTL (manga), vertical, webtoon, and double-page spread
- **EPUB & PDF support** — Powered by Readium Kotlin Toolkit
- **Offline downloads** — Download chapters for offline reading with background sync
- **Reading progress sync** — Tracks your position across devices via server sync
- **Reading statistics** — Time spent reading, pages read, daily charts, and reading streaks
- **Server administration** — Scan libraries, refresh metadata, and view server info (admin users)
- **OPDS browser** — Browse and download from any OPDS 1.2 compatible catalog
- **Dynamic theming** — Material You with light, dark, and system-follow modes
- **Pinch-to-zoom** — 1x–5x zoom with double-tap toggle on comic pages

## Screenshots

*Coming soon*

## Requirements

- Android 8.0 (API 26) or higher
- A running [Kavita](https://www.kavitareader.com/) server (v0.8.x+) or any OPDS-compatible server

## Installation

### From source

1. Clone the repository:
   ```bash
   git clone https://github.com/mateof/kavita-app.git
   cd kavita-app
   ```

2. Open in [Android Studio](https://developer.android.com/studio) (Ladybug 2024.2.1+)

3. Build and run:
   ```bash
   ./gradlew installDebug
   ```

### APK

Download the latest APK from the [Releases](https://github.com/mateof/kavita-app/releases) page.

## Reader Modes

The comic/manga reader supports five reading directions:

| Mode | Description |
|---|---|
| **Left to Right** | Standard horizontal page turning |
| **Right to Left** | Manga-style reading (reversed horizontal pager) |
| **Vertical** | Page-by-page vertical swiping |
| **Webtoon** | Continuous vertical scroll without page gaps |
| **Double Page** | Two pages side by side (landscape) |

All modes support pinch-to-zoom (1x–5x), double-tap zoom toggle, and page preloading.

## Kavita API

The app communicates with Kavita servers via their REST API (v0.8.9.8):

- **Authentication**: JWT-based with automatic token refresh on 401
- **Series browsing**: Paginated series list with filter support (`POST /api/Series/v2`)
- **Reading**: Page-by-page image streaming (`GET /api/Reader/image`)
- **Progress tracking**: Bidirectional sync (`POST /api/Reader/progress`)
- **Administration**: Library scanning, metadata refresh, server info

Full API docs available at `http://your-server:5000/swagger`.

## Development

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed setup instructions, build commands, project conventions, and troubleshooting.

Quick start:

```bash
# Clone
git clone https://github.com/your-username/kavita-app.git
cd kavita-app

# Build
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Make your changes following the existing code conventions
4. Run `./gradlew test` and `./gradlew lint` to verify
5. Submit a pull request

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Kavita](https://www.kavitareader.com/) — The self-hosted reading server this app connects to
- [Readium](https://readium.org/) — Kotlin toolkit powering EPUB/PDF/CBZ reading
- [Material Design 3](https://m3.material.io/) — Google's design system for Android
