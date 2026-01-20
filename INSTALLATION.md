# Installation auf Ihr Xiaomi Pad 6

## Einfachste Methode: Android Studio verwenden

### 1. Android Studio installieren
Falls noch nicht installiert:
```bash
brew install --cask android-studio
```

### 2. Projekt öffnen
- Öffnen Sie Android Studio
- Klicken Sie auf "Open" und wählen Sie diesen Ordner: `/Users/andynope/Documents/pdfwriter`
- Warten Sie, bis Gradle synchronisiert ist (kann einige Minuten dauern)

### 3. Xiaomi Pad 6 vorbereiten
1. **Entwickleroptionen aktivieren:**
   - Gehen Sie zu: Einstellungen → Über das Tablet
   - Tippen Sie 7x auf "MIUI-Version" oder "Build-Nummer"

2. **USB-Debugging aktivieren:**
   - Gehen Sie zu: Einstellungen → Zusätzliche Einstellungen → Entwickleroptionen
   - Aktivieren Sie "USB-Debugging"

3. **Tablet mit Mac verbinden:**
   - Verbinden Sie das Pad 6 per USB-C mit Ihrem Mac
   - Tippen Sie auf "Zulassen" wenn die USB-Debugging-Erlaubnis erscheint

### 4. App installieren
- In Android Studio: Klicken Sie auf den grünen "Run" Button (▶️) in der Toolbar
- Wählen Sie Ihr Xiaomi Pad 6 aus der Geräteliste
- Die App wird automatisch kompiliert und installiert

## Alternative: APK manuell erstellen

Falls Sie Android Studio nicht verwenden möchten:

1. **Android SDK installieren:**
```bash
brew install --cask android-commandlinetools
```

2. **SDK-Location konfigurieren:**
Erstellen Sie die Datei `local.properties` mit:
```
sdk.dir=/Users/andynope/Library/Android/sdk
```

3. **APK erstellen:**
```bash
./gradlew assembleDebug
```

4. **APK auf Tablet übertragen:**
   - Die APK finden Sie unter: `app/build/outputs/apk/debug/app-debug.apk`
   - Per AirDrop, E-Mail oder USB auf das Tablet übertragen
   - Auf dem Tablet öffnen und installieren (erlauben Sie Installation aus unbekannten Quellen)

## Nach der Installation

Die App heißt "PDF Writer" und erscheint in Ihrer App-Liste. 

### Erste Schritte:
1. Öffnen Sie die App
2. Tippen Sie auf "PDF öffnen"
3. Wählen Sie eine PDF-Datei
4. Zeichnen Sie mit Ihrem Xiaomi Stift
5. Speichern Sie mit "PDF speichern"
