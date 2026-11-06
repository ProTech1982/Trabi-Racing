# Trabi Racing

Native Android-Arcade im Hochformat: drei Fahrspuren, DDR-Baustellenbaken, Neon-Nachtkulisse
mit Fernsehturm und ein Trabant 601 von hinten. Kein WebView, kein HTML – die Grafik wird
direkt auf einer `SurfaceView` gezeichnet, die Chiptune-Musik per `AudioTrack` im Code erzeugt.

## Spielprinzip
- Antippen links/rechts oder die Pfeiltasten wechseln die Spur
- Die Straße wird mit der Zeit immer schneller
- Punkte für gefahrene Strecke und sauber passierte Hindernisse
- Eine Kollision beendet die Fahrt, ebenso das gewählte Zeitlimit (1, 2 oder 5 Minuten)
- Spielstand speichern/laden, Top-10-Bestenliste, Musik an/aus

## Aufbau
| Datei | Aufgabe |
|---|---|
| `MainActivity.java` | Menü, Dialoge, Fensterränder, Lebenszyklus |
| `GameView.java` | Spielschleife, Perspektive, Rendering, Eingaben |
| `Chiptune.java` | Melodie, Bass und Hi-Hat als PCM-Synthese |
| `Store.java` | Einstellungen, Spielstand, Highscore |

## APK
GitHub → Releases → neuester Build → Asset `Trabi-Racing.apk`.
Alternativ Actions → letzter grüner Lauf → Artefakt `Trabi-Racing-APK`.

## Signierung
Repository-Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
Bei PKCS12-Keystores sind Keystore- und Schlüsselpasswort identisch.
Immer denselben Keystore behalten, sonst lassen sich Updates nicht über eine
bereits installierte Version legen. Der `versionCode` kommt aus der Build-Nummer.

## Offene Anforderungen

Festgehalten als Arbeitsliste für die nächsten Ausbaustufen.

### Update-Funktion
Nach dem Vorbild von `ProTech1982/meine-termine` und `ProTech1982/unsere-tage`:

- Menüpunkt **„Nach Updates suchen"** sowie optional eine stille Prüfung beim App-Start
- Abfrage der öffentlichen GitHub-Releases-API dieses Repositories
- Vergleich der installierten Version (`versionCode` / `versionName`) mit dem neuesten Release
- Bei neuerer Version: Hinweis mit Änderungstext und Download des APK-Assets
- Voraussetzungen: `INTERNET`-Berechtigung wieder aufnehmen (wurde beim Umbau entfernt),
  Repository muss öffentlich bleiben
- **Kein** GitHub-Token in der APK einbetten

### Menüstruktur
Soll-Zustand des Hauptmenüs:

| Punkt | Status |
|---|---|
| Neues Spiel | vorhanden, heißt derzeit „Spiel starten" |
| Weiterfahren | vorhanden (nur bei laufender Partie) |
| Spiel speichern | vorhanden |
| Spiel laden | vorhanden |
| Spielstand löschen | vorhanden |
| Highscore – Top 10 | vorhanden |
| **Ton an/aus** | **fehlt** – getrennter Schalter für Effekte |
| Musik an/aus | vorhanden (Chiptune) |
| Nach Updates suchen | fehlt |
| Zeitlimit 1 / 2 / 5 Minuten | vorhanden |
| Beenden | vorhanden |

### Ton getrennt von Musik
`Ton an/aus` soll nur die Effekte schalten, `Musik an/aus` nur die Chiptune-Melodie.
Beide Schalter werden in `Store` getrennt abgelegt. Noch zu ergänzende Effekte:
Motorgeräusch, Spurwechsel, sauber passiertes Hindernis, Kollision.
