# LockSelf

> A privacy-first, offline native Android password manager, built with Kotlin and Jetpack Compose.

---

## Highlights

- **100% Offline & Private**: Zero external network permissions (`android.permission.INTERNET` omitted). Zero analytics, telemetry, or cloud tracking.
- **Hardware-Backed Encryption**: Vault records encrypted using AES-GCM-256 with keys derived via PBKDF2-HMAC-SHA256 and Android Keystore hardware-backed biometric authentication.
- **Core Experience & Visual Styling**:
  - **Category Visual Accents**: Signature 5dp vertical color strips (`#D12B58` Crimson for Passwords, `#00B0D4` Teal Cyan for Notes, `#46A66C` Fresh Green for Checklists).
  - **Continuous Spectrum Strength Meter**: Horizontal multi-color gradient track (`#FF0000` $\to$ `#01CC1D`) with real-time sliding notch indicator.
  - **3-Candidate Password Generator**: Generates 3 candidate passwords using `SecureRandom`, computes entropy/security scores, and returns the strongest candidate.
  - **Navigation Drawer Hierarchy**: Full modal drawer featuring record count badges across All, Favorites, Passwords, Notes, and Checklists.
  - **Auto-Lock & Wipe Timers**: Configurable timeout intervals (30s, 1m, 2m, 5m, 10m, 15m [default], 30m, 1h, Never), screen turn-off auto-lock, and instant clipboard wiping on lock.
  - **Bidirectional CSV Interoperability**: 18-column canonical CSV import/export with automatic 10-digit epoch-seconds timestamp normalization, file picker, and direct paste/share options.
- **Clean Architecture**: Strictly decoupled UI, ViewModel, Repository, Crypto, and Room database layers without heavyweight third-party frameworks.

---

## Architecture Overview

```
lockself-android/app/src/main/java/com/lockself/app/
├── ui/
│   ├── screens/                  # Pure Jetpack Compose UI Screens
│   │   ├── VaultMainScreen.kt    # Main list with Navigation Drawer & Category Tabs
│   │   ├── RecordDetailScreen.kt # Record view, password reveal/mask, history
│   │   ├── EditRecordScreen.kt   # Create/Edit credentials, notes, checklists, custom fields
│   │   ├── LockScreen.kt         # Master password auth & Biometric unlock
│   │   └── SettingsScreen.kt     # Timeout preferences, biometrics, backup & restore
│   ├── components/               # Reusable UI Components
│   │   ├── CommonComponents.kt   # Spectrum gradient meter with sliding notch, Badges
│   │   └── PasswordGeneratorDialog.kt
│   ├── navigation/               # Navigation Layer
│   │   ├── NavRoutes.kt          # Type-safe sealed routes
│   │   └── LockSelfNavGraph.kt   # Centralized NavHost controller
│   ├── viewmodel/                # ViewModels (StateFlows & Coroutines)
│   │   ├── VaultMainViewModel.kt # Filter, search, and category state
│   │   ├── RecordDetailViewModel.kt
│   │   ├── EditRecordViewModel.kt
│   │   └── ViewModelFactory.kt   # Clean DI-free ViewModelProvider Factory
│   └── theme/                    # Design System & Colors
│       ├── Color.kt              # Signature accent palette (#D12B58, #00B0D4, #46A66C)
│       └── Theme.kt
├── data/
│   ├── model/                    # Domain Models & Entities
│   │   ├── Entities.kt           # RecordEntity, RecordFieldEntity, ChecklistItemEntity
│   │   └── Enums.kt              # RecordType, FieldType
│   ├── database/                 # Database Layer (Room SQLite)
│   │   ├── LockSelfDatabase.kt
│   │   ├── RecordDao.kt
│   │   └── Converters.kt
│   ├── crypto/                   # Cryptographic Engine
│   │   └── VaultCryptoManager.kt # AES-GCM-256, PBKDF2, Keystore, Biometric wrapping
│   └── repository/               # Repository Layer
│       └── VaultRepository.kt    # Vault CRUD, JSON backup, and CSV export
└── util/
    ├── AppPreferences.kt         # Inactivity auto-lock & clipboard timeout persistence
    ├── ClipboardHelper.kt        # Auto-clearing clipboard management
    ├── PasswordGenerator.kt      # 3-candidate best-security selection algorithm
    ├── PasswordStrengthMeter.kt  # Real-time score & multi-level evaluator
    └── VaultCsvParser.kt         # 18-column bidirectional CSV parser/exporter
```

---

## Security Model

| Security Dimension | Specification |
|---|---|
| **Symmetric Cipher** | AES-256-GCM with unique 12-byte IV per encryption operation |
| **Key Derivation** | PBKDF2-HMAC-SHA256 with cryptographically secure 32-byte salt |
| **Biometric Key Storage** | Android Keystore (`AndroidKeyStore`) wrapping master key with `KeyGenParameterSpec` |
| **Memory Hygiene** | Decrypted credentials held only in ephemeral memory; instant wipe on lock/background |
| **Clipboard Defense** | Configurable automatic erasure timer (default: 10 min) + immediate clearing on lock |
| **Network Attack Surface** | Zero network access (`android.permission.INTERNET` not present in `AndroidManifest.xml`) |

---

## Build & Test Instructions

### Prerequisites
- JDK 21
- Android SDK (API 35, Build-tools 35.0.0+)

### Running Unit Tests
```bash
cd lockself-android
./gradlew testDebugUnitTest
```

### Assembling the Release APK
```bash
cd lockself-android
./gradlew assembleRelease
```
The resulting signed release APK will be generated at:
```
lockself-android/app/build/outputs/apk/release/app-release.apk
```

---

## License

Source code and assets are provided under the MIT License.
