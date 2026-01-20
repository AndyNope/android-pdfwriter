# PDF Writer - Android PDF Annotation App

Eine einfache Android-App zum Annotieren von PDFs mit Stift-Unterstützung, speziell für das Xiaomi Pad 6.

## Features

- ✅ PDF-Dateien öffnen und anzeigen
- ✅ Mit dem Stift auf PDFs zeichnen
- ✅ Druckempfindlichkeit für natürliches Schreibgefühl
- ✅ Stiftgröße anpassbar
- ✅ Zeichnungen löschen
- ✅ Annotierte PDFs speichern

## Technische Details

- **Sprache**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **PDF-Bibliothek**: AndroidPdfViewer (barteksc)

## Installation

### Voraussetzungen
- Android Studio (neueste Version empfohlen)
- Android SDK 34
- Gradle 8.2+

### Build-Schritte

1. Projekt in Android Studio öffnen
2. Gradle sync ausführen (geschieht automatisch)
3. App auf Gerät oder Emulator installieren:
   ```bash
   ./gradlew installDebug
   ```

### APK erstellen

Debug-APK:
```bash
./gradlew assembleDebug
```

Release-APK (signiert):
```bash
./gradlew assembleRelease
```

## Verwendung

1. **PDF öffnen**: Tippen Sie auf "PDF öffnen" und wählen Sie eine PDF-Datei aus
2. **Zeichnen**: Verwenden Sie Ihren Stift, um auf dem PDF zu zeichnen
   - Die App unterstützt Druckempfindlichkeit
   - Verwenden Sie den Schieberegler für die Stiftgröße
3. **Löschen**: Tippen Sie auf "Zeichnung löschen", um alle Annotationen zu entfernen
4. **Speichern**: Tippen Sie auf "PDF speichern", um das annotierte PDF zu speichern
   - Die Datei wird im Documents-Ordner gespeichert

## Berechtigungen

Die App benötigt folgende Berechtigungen:
- `READ_EXTERNAL_STORAGE`: Zum Öffnen von PDF-Dateien
- `WRITE_EXTERNAL_STORAGE`: Zum Speichern annotierter PDFs (nur Android 9 und älter)

## Projektstruktur

```
app/
├── src/main/
│   ├── java/com/pdfwriter/app/
│   │   ├── MainActivity.kt          # Haupt-Activity mit PDF-Viewer
│   │   └── DrawingView.kt           # Custom View für Zeichnungen
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml    # UI Layout
│   │   ├── values/
│   │   │   ├── strings.xml          # String-Ressourcen
│   │   │   ├── colors.xml           # Farben
│   │   │   └── themes.xml           # App-Theme
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Anpassungen

### Stiftfarbe ändern
In [DrawingView.kt](app/src/main/java/com/pdfwriter/app/DrawingView.kt):
```kotlin
drawingView.penColor = Color.BLUE // oder Color.RED, Color.GREEN, etc.
```

### Standard-Stiftgröße ändern
In [MainActivity.kt](app/src/main/java/com/pdfwriter/app/MainActivity.kt):
```kotlin
binding.drawingView.penSize = 10f // Standard ist 5f
```

## Bekannte Einschränkungen

- Die App speichert nur eine flache PDF-Datei (keine Ebenen)
- Keine Radiergummi-Funktion (nur komplettes Löschen)
- Keine Farbauswahl-UI (nur programmatisch änderbar)
- Mehrseitige PDFs werden als einzelne scrollbare Ansicht angezeigt

## Zukünftige Verbesserungen

- [ ] Radiergummi-Funktion
- [ ] Farbauswahl-Dialog
- [ ] Rückgängig/Wiederherstellen
- [ ] Verschiedene Stift-Typen (Marker, Textmarker)
- [ ] Seiten-Navigation für mehrseitige PDFs
- [ ] Cloud-Synchronisation

## Lizenz

Dieses Projekt ist frei verfügbar für persönliche und kommerzielle Nutzung.

## Support

Bei Problemen oder Fragen erstellen Sie bitte ein Issue im Repository.
