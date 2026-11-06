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
