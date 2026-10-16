# GitHub Actions Workflow — Kompilace APK pro HTMLPack

Vytvoření automatizovaného GitHub Actions CI/CD workflow (`.github/workflows/build-apk.yml`), který automaticky zkompiluje projekt do Android APK balíčku, uloží jej jako stažitelný artefakt běhu a při vytvoření git tagu automaticky vytvoří GitHub Release.

---

## Uživatelské volby a potvrzená rozhodnutí

> [!IMPORTANT]
> Na základě vašich odpovědí konfigurujeme workflow s těmito parametry:

- **Spouštěcí události (Triggery)**:
  - Automatické spuštění při každém `push` do větví `main` a `master`.
  - Ruční spuštění na vyžádání (`workflow_dispatch`) přímo z webového rozhraní GitHub Actions.
  - Spuštění při vytvoření a odeslání verzovacího tagu (`tags: ['v*']`).
- **Cíl kompilace**: Sestavení instalačního **Debug APK** (`./gradlew assembleDebug`), které je plně podepsané ladicím klíčem a lze jej okamžitě nainstalovat na jakékoliv zařízení se zapnutým povolením neznámých zdrojů bez nutnosti generovat produkční certifikáty.
- **Uložení a distribuce**:
  - Každý běh nahraje hotový APK soubor jako **GitHub Actions Artifact** (`actions/upload-artifact@v4`).
  - Pokud je workflow spuštěn verzovacím tagem (např. `v1.0.0`), automaticky vytvoří **GitHub Release** (`softprops/action-gh-release@v2`) s přiloženým APK souborem.

---

## 1. Architektura CI/CD workflow

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions Triggers                  │
│   • Push to main/master   • workflow_dispatch   • Git tag   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Job: build-apk                         │
│  1. Checkout kódu (actions/checkout@v4)                     │
│  2. Nastavení Java 17/21 (actions/setup-java@v4)            │
│  3. Setup Gradle s mezipamětí (gradle/actions/setup-gradle) │
│  4. Generování / ověření debug.keystore                     │
│  5. Příprava .env souboru z .env.example                    │
│  6. Spuštění kompilace: ./gradlew assembleDebug             │
│  7. Upload artefaktu HTMLPack-debug.apk                     │
│  8. (Při tagu) Vytvoření GitHub Release s APK               │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Klíčové kroky implementace

1. **Gradle Wrapper Script (`gradlew` a `gradlew.bat`)**:
   - V repozitáři je již nakonfigurována verze Gradle `gradle-9.3.1-bin.zip` v `gradle/wrapper/gradle-wrapper.properties`.
   - Přidáme standardní skript `gradlew` (s oprávněním pro spuštění `chmod +x gradlew`) a `gradlew.bat`, aby GitHub runner mohl Gradle spouštět bez závislosti na globální instalaci.
2. **Ošetření debug.keystore na CI**:
   - V `app/build.gradle.kts` je definován `storeFile = file("${rootDir}/debug.keystore")`.
   - Ve workflow zajistíme vytvoření platného `debug.keystore` pomocí standardního nástroje `keytool` před spuštěním kompilace, pokud v kořenu chybí.
3. **Workflow soubor `.github/workflows/build-apk.yml`**:
   - Moderní a bezpečné akce (`actions/checkout@v4`, `actions/setup-java@v4`, `actions/upload-artifact@v4`, `softprops/action-gh-release@v2`).
   - Nastavená oprávnění `permissions: contents: write` pro možnost publikovat GitHub Release při pushnutí tagu.
   - Přejmenování výstupního APK na přehledný název, např. `HTMLPack-debug.apk`.
