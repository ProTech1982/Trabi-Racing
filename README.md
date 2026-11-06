# Trabi Racing
Android-Hochformat-Racer mit drei Fahrspuren, Neon-DDR-Kulisse, Baustellenhindernissen und lokalem Highscore.

## APK
GitHub → Actions → Android APK → neuester erfolgreicher Lauf → Artifact **Trabi-Racing-APK** herunterladen.

## Signierung
Benötigte Repository-Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
Bei Updates immer denselben Keystore und dieselbe applicationId verwenden. Die Versionsnummer muss steigen.

## Updates
Die App prüft die öffentliche GitHub-Releases-API. Ein Release mit Tag `v1.0.1` und APK-Asset ermöglicht einen Browserdownload. Bei privatem Repository ist eine anonyme Release-Prüfung nicht möglich; hierfür muss ein sicherer, authentifizierter Backenddienst ergänzt werden. Keine GitHub-Tokens in die APK einbetten.
