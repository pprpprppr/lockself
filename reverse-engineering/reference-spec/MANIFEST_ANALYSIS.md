# BlackBerry Password Keeper — Manifest Analysis Report

## 1. Application-Level Attributes

| Attribute | Declared Value | Architectural Purpose | Evidence Classification |
| :--- | :--- | :--- | :--- |
| `package` | `com.blackberry.passwordkeeper` | Unique Android package identifier | STATICALLY CONFIRMED |
| `android:name` | `com.blackberry.passwordkeeper.PKApplication` | Custom application singleton lifecycle | STATICALLY CONFIRMED |
| `android:theme` | `@style/PKTheme` | Root styling theme | STATICALLY CONFIRMED |
| `android:allowBackup` | `false` | Explicitly disables Android Cloud ADB auto-backup | STATICALLY CONFIRMED |
| `android:appCategory` | `productivity` | Play Store categorization | STATICALLY CONFIRMED |
| `android:extractNativeLibs` | `false` | Memory-mapped native execution from APK | STATICALLY CONFIRMED |
| `android:supportsRtl` | `true` | Bidirectional layout mirroring for RTL languages | STATICALLY CONFIRMED |

---

## 2. Permissions Analysis

| Declared Permission | Protection Level | Purpose in Original Application | Security Assessment & LockSelf Modernization | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal | Used for Google AdMob (`MobileAdsInitProvider`), telemetry, cloud backup, and web links | **ELIMINATED IN LOCKSELF**: Unnecessary for a secure offline vault; removing eliminates remote attack vectors. | STATICALLY CONFIRMED |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Checks network availability for AdMob and Google Drive synchronization | **ELIMINATED IN LOCKSELF**: Zero network connectivity required. | STATICALLY CONFIRMED |
| `com.google.android.gms.permission.AD_ID` | Normal | Advertising tracking identifier for Google AdMob SDK | **ELIMINATED IN LOCKSELF**: Privacy violation; ad-free architecture. | STATICALLY CONFIRMED |
| `android.permission.ACCESS_ADSERVICES_AD_ID` | Normal | Privacy Sandbox advertising API | **ELIMINATED IN LOCKSELF**: Excluded. | STATICALLY CONFIRMED |
| `android.permission.ACCESS_ADSERVICES_ATTRIBUTION` | Normal | Ad attribution conversion tracking | **ELIMINATED IN LOCKSELF**: Excluded. | STATICALLY CONFIRMED |
| `android.permission.ACCESS_ADSERVICES_TOPICS` | Normal | Ad targeting interest topics | **ELIMINATED IN LOCKSELF**: Excluded. | STATICALLY CONFIRMED |
| `com.blackberry.ddt.permission.SEND_TELEMETRY_EVENTS` | Signature/Privileged | BlackBerry device analytics telemetry | **ELIMINATED IN LOCKSELF**: Zero telemetry policy. | STATICALLY CONFIRMED |
| `com.blackberry.pim.permission.INTERNAL` | Signature | BlackBerry Enterprise PIM interoperability | **ELIMINATED IN LOCKSELF**: Proprietary enterprise dependency. | STATICALLY CONFIRMED |
| `android.permission.USE_FINGERPRINT` | Normal (Deprecated) | Legacy Android 6.0-8.1 fingerprint authentication API | **MODERNIZED IN LOCKSELF**: Replaced with modern AndroidX `BiometricPrompt` and `BiometricManager`. | STATICALLY CONFIRMED |
| `android.permission.VIBRATE` | Normal | Haptic feedback on invalid attempts and toggle events | Preserved for tactile user feedback. | STATICALLY CONFIRMED |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Clears stale clipboard and registers lock timer on reboot | Handled via Android WorkManager / Keystore lifecycle. | STATICALLY CONFIRMED |
| `android.permission.READ_EXTERNAL_STORAGE` | Dangerous (Legacy) | File picker for backup restore | **MODERNIZED IN LOCKSELF**: Replaced with Storage Access Framework (`ActivityResultContracts.OpenDocument`). | STATICALLY CONFIRMED |
| `android.permission.WRITE_EXTERNAL_STORAGE` | Dangerous (Legacy) | Saving CSV backup exports | **MODERNIZED IN LOCKSELF**: Replaced with SAF (`ActivityResultContracts.CreateDocument`). | STATICALLY CONFIRMED |
| `android.permission.WAKE_LOCK` | Normal | Prevents CPU sleep during backup operations | Not needed for local transactions. | STATICALLY CONFIRMED |
| `android.permission.FOREGROUND_SERVICE` | Normal | Background clipboard monitor / WorkManager | Standard foreground service constraint. | STATICALLY CONFIRMED |
| `android.permission.REORDER_TASKS` | Normal | Moving task to back on auto-lock minimize | Handled via standard activity lifecycle finish/minimize. | STATICALLY CONFIRMED |

---

## 3. Component Inventory: Activities (35 Total)

### Core User-Facing Activities
1. `com.blackberry.passwordkeeper.MainActivity`
   - **Exported**: `true` [STATICALLY CONFIRMED]
   - **Intent Filters**: `android.intent.action.MAIN` (`category.LAUNCHER`), `android.intent.action.SEARCH`, `com.google.android.gms.actions.SEARCH_ACTION`.
   - **Launch Mode**: `singleTop`.
   - **Function**: Primary dashboard, category list, drawer host, record filtering, fab actions.
2. `com.blackberry.passwordkeeper.ItemDetailActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Record inspection, password unmasking, checklist toggles, copy actions.
3. `com.blackberry.passwordkeeper.EditRecordActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Editing and updating credentials, notes, and checklist items.
4. `com.blackberry.passwordkeeper.AddRecordActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Dedicated creation flow for new records.
5. `com.blackberry.passwordkeeper.LockActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Vault unlock gate; PIN/password verification and biometric trigger.
6. `com.blackberry.passwordkeeper.SetupActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Initial vault creation wizard and master password setup.
7. `com.blackberry.passwordkeeper.ChangePasswordActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Master password modification and database re-encryption.
8. `com.blackberry.passwordkeeper.PasswordHistoryActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Inspects previously used passwords with timestamps.
9. `com.blackberry.passwordkeeper.settings.SettingsActivity`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Multi-category settings screen.
10. `com.blackberry.passwordkeeper.importexport.ExportActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: Initiates CSV/encrypted file export.
11. `com.blackberry.passwordkeeper.importexport.ImportActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: File selection and parsing for record restoration.
12. `com.blackberry.passwordkeeper.fingerprint.EnableFingerprintActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: Biometric enrollment wizard.
13. `com.blackberry.passwordkeeper.WhatsNewActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: Version feature overview dialog.
14. `com.blackberry.passwordkeeper.OpenSourceActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: Legal license attribution notice viewer.
15. `com.blackberry.passwordkeeper.RateAndReviewActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: Play Store rating prompt.
16. `com.blackberry.passwordkeeper.SelectRecordActivity` & `SelectRecordIfNeededActivity`
    - **Exported**: `false` [STATICALLY CONFIRMED]
    - **Function**: Picker interface for autofill services.

### Third-Party / Advertising Activities
- `com.google.android.gms.ads.AdActivity`
- `com.google.android.gms.ads.OutOfContextTestingActivity`
- `com.google.android.gms.ads.NotificationHandlerActivity`
- `com.google.android.gms.auth.api.signin.internal.SignInHubActivity`
- `com.google.android.gms.common.api.GoogleApiActivity`

---

## 4. Component Inventory: Services (10 Total)

1. `com.blackberry.passwordkeeper.autofill.PKAutofillService`
   - **Permission**: `android.permission.BIND_AUTOFILL_SERVICE` [STATICALLY CONFIRMED]
   - **Exported**: `true` [STATICALLY CONFIRMED]
   - **Function**: Android Autofill Framework integration.
2. `com.blackberry.passwordkeeper.formfill.ImeService`
   - **Permission**: `android.permission.BIND_INPUT_METHOD` [STATICALLY CONFIRMED]
   - **Exported**: `true` [STATICALLY CONFIRMED]
   - **Function**: Custom software keyboard provider for pasting credentials into restricted fields.
3. `com.blackberry.passwordkeeper.ClipboardService`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Manages clipboard monitoring and timed auto-clearing.
4. `com.blackberry.passwordkeeper.DummyCloudSyncService`
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Stub sync adapter service.
5. WorkManager & Room Services:
   - `androidx.work.impl.background.systemalarm.SystemAlarmService`
   - `androidx.work.impl.background.systemjob.SystemJobService`
   - `androidx.work.impl.foreground.SystemForegroundService`
   - `androidx.room.MultiInstanceInvalidationService`
6. `com.google.android.gms.ads.AdService` [STATICALLY CONFIRMED]

---

## 5. Component Inventory: Broadcast Receivers & Content Providers

### Receivers
1. `com.blackberry.passwordkeeper.LockAlarmReceiver`: Wakes on system alarm triggers to lock vault and wipe clipboard. [STATICALLY CONFIRMED]
2. `com.blackberry.profile.ProfileUnifiedChangeReceiver`: Handles work profile toggle events. [STATICALLY CONFIRMED]
3. Android WorkManager constraint proxies (`BatteryChargingProxy`, `BatteryNotLowProxy`, `StorageNotLowProxy`, `NetworkStateProxy`, `RescheduleReceiver`). [STATICALLY CONFIRMED]

### Content Providers
1. `com.blackberry.arch.backup.BackupProvider`:
   - **Authorities**: `com.blackberry.passwordkeeper.backup` [STATICALLY CONFIRMED]
   - **Exported**: `false` [STATICALLY CONFIRMED]
   - **Function**: Inter-process backup staging content provider.
2. `com.google.android.gms.ads.MobileAdsInitProvider`: Automatic AdMob SDK initialization. [STATICALLY CONFIRMED]
3. `androidx.startup.InitializationProvider`: Jetpack App Startup initializer. [STATICALLY CONFIRMED]
