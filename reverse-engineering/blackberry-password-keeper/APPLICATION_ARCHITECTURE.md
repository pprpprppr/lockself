# BlackBerry Password Keeper — Application Architecture Report

## 1. Original BlackBerry Architecture Model

The original BlackBerry Password Keeper (`com.blackberry.passwordkeeper`) employs a traditional Android Activity/Fragment architectural pattern anchored by an Application-level controller:

```mermaid
flowchart TD
    subgraph UI_Layer ["UI / Presentation Layer"]
        MA[MainActivity]
        NDF[NavigationDrawerFragment]
        IDA[ItemDetailActivity]
        ERA[EditRecordActivity]
        LA[LockActivity]
        SA[SetupActivity]
        SA_Prefs[SettingsActivity]
    end

    subgraph App_Controller ["Application Controller & Lifecycle"]
        PKA[PKApplication]
        LAR[LockAlarmReceiver]
        CBS[Clipboard Listener]
    end

    subgraph Domain_Core ["Domain Core & Services"]
        RM[P0.p: RecordManager]
        PE[P0.k: Password Strength]
        PG[P0.l: Password Generator]
        IE[Q0.a / Q0.b: CSV Engine]
        FP[J0.c: Fingerprint Keystore]
    end

    subgraph Storage ["Encrypted Storage Layer"]
        SC[SQLCipher Native Engine]
        DB[(records.db)]
        KS[AndroidKeyStore]
        SP[SharedPreferences]
    end

    MA --> NDF
    MA --> IDA
    MA --> ERA
    MA --> LA
    MA --> SA_Prefs
    
    PKA --> LAR
    PKA --> CBS
    PKA --> RM
    
    LA --> RM
    IDA --> RM
    ERA --> RM
    ERA --> PE
    ERA --> PG
    SA_Prefs --> FP
    
    RM --> SC
    SC --> DB
    FP --> KS
    PKA --> SP
```

### Architectural Analysis:
1. **Activity-Centric Orchestration**: Screens are implemented as standalone Activities (`MainActivity`, `ItemDetailActivity`, `EditRecordActivity`, `LockActivity`) backed by legacy `android.app.Fragment` instances. [STATICALLY CONFIRMED]
2. **Centralized Singleton State**: `PKApplication` maintains application-wide session state, monitors foreground/background transitions via `ActivityLifecycleCallbacks`, and controls timeout alarms. [STATICALLY CONFIRMED via `PKApplication.java:580-600`]
3. **Synchronous/Worker Persistence**: `RecordManager` handles raw SQL statements, managing transactions directly on a SQLCipher database handle. [STATICALLY CONFIRMED via `P0/p.java`]

---

## 2. LockSelf Reconstructed Clean Architecture

To eliminate the architectural debt and deprecations of the original application (such as deprecated `android.app.Fragment`, legacy `FingerprintManager`, synchronous disk access on main thread, and embedded AdMob/telemetry SDKs), **LockSelf** implements modern Android Clean Architecture with Jetpack Compose:

```mermaid
flowchart TD
    subgraph Presentation_Layer ["Presentation Layer (Jetpack Compose + Material 3)"]
        VMS[VaultMainScreen]
        RDS[RecordDetailScreen]
        ERS[EditRecordScreen]
        LS[LockScreen]
        SS[SettingsScreen]
        PGD[PasswordGeneratorDialog]
        CSM[ContinuousStrengthMeter]
    end

    subgraph ViewModel_Layer ["State & ViewModel Layer (Kotlin Coroutines + StateFlow)"]
        VM_Main[VaultMainViewModel]
        VM_Detail[RecordDetailViewModel]
        VM_Edit[EditRecordViewModel]
        VM_Settings[SettingsViewModel]
    end

    subgraph Repository_Layer ["Repository Layer (Single Source of Truth)"]
        VR[VaultRepository]
        CSV_Parser[BlackBerryCsvParser]
    end

    subgraph Domain_Services ["Domain Logic & Utilities"]
        Crypto[VaultCryptoManager]
        Gen[PasswordGenerator]
        Meter[PasswordStrengthMeter]
        Clip[ClipboardHelper]
        Prefs[AppPreferences]
    end

    subgraph Data_Layer ["Data & Security Layer"]
        RoomDB[LockSelfDatabase]
        DAO[RecordDao]
        Keystore[Android KeyStore AES-GCM]
    end

    VMS --> VM_Main
    RDS --> VM_Detail
    ERS --> VM_Edit
    SS --> VM_Settings
    
    VM_Main --> VR
    VM_Detail --> VR
    VM_Edit --> VR
    VM_Edit --> Gen
    VM_Edit --> Meter
    VM_Settings --> VR
    VM_Settings --> Prefs
    
    VR --> DAO
    VR --> Crypto
    VR --> CSV_Parser
    Crypto --> Keystore
    DAO --> RoomDB
```

---

## 3. Layer Separation Guarantees

| Architecture Layer | Responsibilities in LockSelf | Technologies Used | Security & Privacy Guarantees |
| :--- | :--- | :--- | :--- |
| **Presentation (UI)** | Declarative rendering, screen transitions, responsive drawer, tactile user feedback | Jetpack Compose, Material 3, Navigation Compose | Pure declarative Composables; no direct database or cryptographic access. |
| **ViewModels** | UI state management, coroutine scoping, error state modeling, validation | AndroidX `ViewModel`, `StateFlow`, `asStateFlow()` | Unidirectional data flow (UDF); state preserved across configuration changes. |
| **Repository** | Single source of truth, orchestration between database and cryptographic services | Kotlin Coroutines, `Flow` | In-memory sensitive values zeroed upon vault locking. |
| **Domain Services** | Security scoring, candidate generation, clipboard auto-wipe, preference management | `SecureRandom`, `Normalizer`, `ClipboardManager` | Zero network operations; purely offline algorithmic execution. |
| **Database & DAO** | Encrypted object-relational mapping, versioned migrations, relational integrity | AndroidX Room, SQLite | Foreign keys with `CASCADE` delete; indexed queries; encrypted persistence. |
| **Cryptography** | Master key derivation, authenticated encryption, Keystore biometric wrapping | PBKDF2 with HMAC-SHA256, AES-256-GCM, Android Keystore | Unique 96-bit nonces per encryption; 128-bit authentication tags; hardware-backed keystore. |

---

## 4. Elimination of Legacy Anti-Patterns

1. **Deprecated Fragments**: Replaced deprecated `android.app.Fragment` with type-safe `NavHost` and Composable destinations (`NavRoutes`).
2. **Thread Blocking**: Replaced synchronous SQL queries on the UI thread with asynchronous Room `Flow` and `suspend` functions executing on `Dispatchers.IO`.
3. **Telemetry & Ads**: Stripped all references to `com.google.android.gms.ads` and `com.blackberry.ddt` analytics, ensuring a zero-footprint, 100% offline, privacy-first implementation.
