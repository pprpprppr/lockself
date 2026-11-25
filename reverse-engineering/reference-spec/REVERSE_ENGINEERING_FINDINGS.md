# Legacy Password Keeper — Reverse Engineering Master Findings Report

## 1. Executive Summary

This report establishes the complete reverse engineering analysis of the official **Legacy Password Keeper APK** (`com.legacy.passwordkeeper` v2.2643.6, build 142825012) using the `morluto/rea` reverse engineering framework, JADX decompiler, AAPT binary inspector, and Android SDK tools.

The application binary was verified in place without modification:
- **Location**: `/home/deb/locksafe/Password Keeper.apk` [DIRECTLY OBSERVED]
- **File Size**: `25,059,775` bytes [DIRECTLY OBSERVED]
- **SHA-256 Digest**: `24e6b63c9efb4abb9e711c624647a6b517be5df3b6294335c1e5585d0c3a4258` [DIRECTLY OBSERVED]
- **Certificate**: Signed by `CN=Legacy Vault Limited` with SHA-256 `2C:57:98:5A:7B:46:76:56:17:A0:29:CC:6A:FA:E8:61:B0:E5:EA:AC:3F:9C:AA:0B:29:F4:1C:49:EB:AF:37:8B` [STATICALLY CONFIRMED]

---

## 2. Master Findings Matrix with Evidence Classifications

| # | Domain | Core Finding | Class / Method / Resource Reference | Classification | Evidence Details |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **Binary** | Single DEX archive containing 7,362 classes; native libraries restricted solely to SQLCipher 4.x for `arm64-v8a` and `armeabi-v7a`. | `classes.dex`, `lib/arm64-v8a/libsqlcipher.so` | **STATICALLY CONFIRMED** | Verified via archive inspection (`unzip -l`) and JADX decompiler loading manifest. |
| 2 | **Database** | 4 SQLite tables: `records`, `fields`, `subfields`, and `metadata` managed by SQLCipher under database filename `records.db`. Schema version 7. | `P0.p.D0(SQLiteDatabase)` (`P0/p.java:1298`) | **STATICALLY CONFIRMED** | Extracted exact SQL DDL strings and migration switch blocks (`P0.p.F0`). |
| 3 | **Model** | Polymorphic record system supporting 4 record types (`PASSWORD`, `NOTE`, `CLIPBOARD`, `LIST`) and 14 field types. | `P0.q`, `P0.d` | **STATICALLY CONFIRMED** | Extracted Java enum constants and field mapping logic. |
| 4 | **Subfields** | Specialized subfields handle historical password logging (`HistoricalPasswordSubfield`), interactive checklists (`ListCheckBoxSubfield`), and timestamp audits (`PasswordSetTimeSubfield`). | `P0.f`, `P0.g`, `P0.i` | **STATICALLY CONFIRMED** | Column mappings `tVal` (String), `boolVal` (Boolean), and `iVal` (Long/Int) confirmed from constructors and `ContentValues` mappers. |
| 5 | **Strength** | Continuous gradient strength evaluation using Unicode NFC normalization, character class scoring, and sequence penalties clamped to `[0, 100]`. | `P0.k.c(char[])`, `PasswordSecurityField.java` | **STATICALLY CONFIRMED** | Complete scoring formula and sliding notch calculation `notchX = (score / 100f) * trackWidth` verified. |
| 6 | **Generator** | Multi-candidate CSPRNG password generation selecting the highest scoring candidate among 3 generated candidates. | `P0.l.d(int, int)`, `LN0.c.B` | **STATICALLY CONFIRMED** | Uses `java.security.SecureRandom` with bitmask selection for Letters, Numbers, and Symbols. |
| 7 | **Biometrics** | Android KeyStore RSA 2048-bit key (`"fingerprint_key"`) with OAEP-SHA256 wrapping master password in SharedPreferences. | `J0.c.d(Context, String)` | **STATICALLY CONFIRMED** | KeyStore alias, parameter specs, and 15-login reminder counter verified. |
| 8 | **Lifecycle** | Background inactivity tracking and screen timeout locking enforced via `AlarmManager` and `LockAlarmReceiver`. | `PKApplication.java:585`, `LockAlarmReceiver.java` | **STATICALLY CONFIRMED** | Auto-lock timeouts: 30s, 1m, 2m, 5m, 10m, 15m (default), 30m, 1h, Never. |
| 9 | **Clipboard** | Clipboard writes tagged as `"bb.pk"`; auto-cleared upon timer expiry (30s to 15m, 10m default) or vault locking. | `PKApplication.java:596`, `pref_copied_text.xml` | **STATICALLY CONFIRMED** | Verified `OnPrimaryClipChangedListener` and `ClipData` clearing mechanisms. |
| 10 | **Import/Export** | 18-column bidirectional CSV format with custom fields serialized as embedded JSON array in column 7. | `Q0.a.n(P0.m)`, `Q0.b.m(String)` | **STATICALLY CONFIRMED** | Column headers and JSON parsing state machine fully analyzed. |
| 11 | **UI Styling** | High-density card layout with signature 5dp left vertical accent strips: CrimsonRed (`#D12B58`) for Passwords, Cyan (`#00B0D4`) for Notes, EmeraldGreen (`#46A66C`) for Checklists. | `res/values/colors.xml`, `view_record_details.xml` | **STATICALLY CONFIRMED** | Color hex values and layout dimension attributes verified from compiled resources. |
| 12 | **Telemetry/Ads** | Inclusion of Google AdMob advertising SDK (`MobileAdsInitProvider`) and Legacy Vault DDT telemetry (`SEND_TELEMETRY_EVENTS`). | `AndroidManifest.xml:45` | **STATICALLY CONFIRMED** | Identified external SDK dependencies; documented for removal in LockSelf. |

---

## 3. Reconstruction Blueprint for LockSelf

Based on these findings, the reconstructed native Android application **LockSelf** is engineered to achieve:
1. **100% Visual and Behavioral Fidelity**: Recreates the navigation drawer categories (`All`, `Favorites`, `Passwords`, `Notes`, `Lists`, `Settings`, `Lock Vault`), continuous gradient strength meter with sliding notch, multi-candidate password generator, and record workflows.
2. **Hardened Security**: Replaces legacy SQLCipher string handling with AES-256-GCM authenticated encryption and PBKDF2 (600,000 rounds); modernizes biometrics with AndroidX `BiometricPrompt` and hardware-backed Android KeyStore.
3. **Zero Network, Zero Telemetry**: Omits all advertising SDKs, tracking libraries, and network permissions, ensuring a completely offline, private vault.
4. **Clean Layered Architecture**: Strict separation of UI screens, ViewModels, Repository, Database DAOs, and Cryptographic providers using Kotlin and Jetpack Compose.
