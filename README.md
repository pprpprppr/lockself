# LockSelf

> A privacy-first, offline native Android password manager and faithful clean-room reconstruction of the legendary BlackBerry Password Keeper experience, built with Kotlin and Jetpack Compose.

---

## Highlights

- **100% Offline & Private**: Zero external network permissions (`android.permission.INTERNET` omitted). Zero analytics, telemetry, or cloud tracking.
- **Hardware-Backed Encryption**: Vault records encrypted using AES-GCM-256 with keys derived via PBKDF2-HMAC-SHA256 (100,000 iterations) and Android Keystore hardware-backed biometric authentication.
- **BlackBerry Password Keeper Parity**:
  - **Category Visual Accents**: Signature 5dp vertical color strips (`#D12B58` Berry Crimson for Passwords, `#00B0D4` Teal Cyan for Notes, `#46A66C` Fresh Green for Checklists).
  - **Continuous Spectrum Strength Meter**: Horizontal multi-color gradient track (`#FF0000` $\to$ `#01CC1D`) with real-time sliding notch indicator based on BlackBerry's `PasswordSecurityField`.
  - **3-Candidate Password Generator**: Implements BlackBerry's `LN0/c.B` algorithm—generates 3 candidate passwords using `SecureRandom`, computes entropy/security scores, and returns the strongest candidate.
  - **Navigation Drawer Hierarchy**: Full modal drawer featuring record count badges across All, Favorites, Passwords, Notes, and Checklists.
  - **Auto-Lock & Wipe Timers**: Authentic timeout intervals (30s, 1m, 2m, 5m, 10m, 15m [default], 30m, 1h, Never), screen turn-off auto-lock, and instant clipboard wiping on lock.
  - **Bidirectional CSV Interoperability**: 18-column canonical BlackBerry Password Keeper CSV import/export with automatic 10-digit epoch-seconds timestamp normalization.
- **Clean Architecture**: 12 strictly decoupled layers without heavyweight third-party frameworks.
- **Companion Web / PWA Client**: Standalone client-side WebCrypto implementation located in [`web-app/`](web-app/).

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
│       ├── Color.kt              # BlackBerry extracted palette (#D12B58, #00B0D4, #46A66C)
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
│       └── VaultRepository.kt    # Vault CRUD, JSON backup, and BlackBerry CSV export
└── util/
    ├── AppPreferences.kt         # BlackBerry auto-lock & clipboard timeout persistence
    ├── ClipboardHelper.kt        # Auto-clearing clipboard management
    ├── PasswordGenerator.kt      # 3-candidate best-security selection algorithm (LN0/c.B)
    ├── PasswordStrengthMeter.kt  # Real-time score & multi-level evaluator
    └── BlackBerryCsvParser.kt    # 18-column bidirectional CSV parser/exporter
```

---

## Security Model

| Security Dimension | Specification |
|---|---|
| **Symmetric Cipher** | AES-256-GCM with unique 12-byte IV per encryption operation |
| **Key Derivation** | PBKDF2-HMAC-SHA256 with 100,000 iterations and 16-byte cryptographically secure salt |
| **Biometric Key Storage** | Android Keystore (`AndroidKeyStore`) wrapping master key with `KeyGenParameterSpec` user authentication requirements |
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

## Reverse Engineering Documentation

Detailed reverse-engineering specifications, schema comparisons, and disassembly findings are documented in [`reverse-engineering/blackberry-password-keeper/`](reverse-engineering/blackberry-password-keeper/):
- [APK Structure & Metadata](reverse-engineering/blackberry-password-keeper/APK_METADATA.md)
- [Application Architecture](reverse-engineering/blackberry-password-keeper/APPLICATION_ARCHITECTURE.md)
- [Database Schema & Migration Mapping](reverse-engineering/blackberry-password-keeper/DATABASE_SCHEMA.md)
- [Encryption & Cryptographic Tracing](reverse-engineering/blackberry-password-keeper/ENCRYPTION_ANALYSIS.md)
- [Feature Parity Matrix](reverse-engineering/blackberry-password-keeper/FEATURE_PARITY_MATRIX.md)
- [Reverse Engineering Findings](reverse-engineering/blackberry-password-keeper/REVERSE_ENGINEERING_FINDINGS.md)

---

## License

Independent clean-room implementation. Reconstructed assets and source code are provided under the MIT License.
