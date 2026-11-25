# Legacy Password Keeper — APK Metadata Report

## 1. Executive Identification

- **Application Label**: Legacy Password Keeper
- **Package Name**: `com.legacy.passwordkeeper`
- **Version Name**: `2.2643.6`
- **Version Code**: `142825012`
- **Target SDK**: `36` (Android 16 preview)
- **Minimum SDK**: `28` (Android 9.0 Pie)
- **Compile SDK**: `36`
- **Platform Build Version**: `16` (Code `36`)
- **Binary File Path**: `/home/deb/locksafe/Password Keeper.apk`
- **File Size**: `25,059,775` bytes (~23.90 MB)
- **SHA-256 Checksum**: `24e6b63c9efb4abb9e711c624647a6b517be5df3b6294335c1e5585d0c3a4258`
- **MD5 Checksum**: `58622c422472e389e17b35ea4ffc562c`
- **SHA-1 Checksum**: `b794f4a3bfec69a531b4eefcaef1dc2b6045618b`

---

## 2. Evidence Classification & Source Mapping

| Attribute | Value | Evidence Classification | Source Reference |
| :--- | :--- | :--- | :--- |
| File Path | `/home/deb/locksafe/Password Keeper.apk` | DIRECTLY OBSERVED | Filesystem check (`ls -l`) |
| File Size | `25,059,775` bytes | DIRECTLY OBSERVED | `ls -l` command |
| SHA-256 Digest | `24e6b63c9efb4abb9e711c624647a6b517be5df3b6294335c1e5585d0c3a4258` | DIRECTLY OBSERVED | `sha256sum` utility output |
| Application Name | `Password Keeper` | STATICALLY CONFIRMED | `aapt dump badging`, `res/values/strings.xml` (`app_name`) |
| Package Name | `com.legacy.passwordkeeper` | STATICALLY CONFIRMED | `aapt dump badging`, `AndroidManifest.xml` package attribute |
| Version Code | `142825012` | STATICALLY CONFIRMED | `aapt dump badging`, `AndroidManifest.xml` versionCode attribute |
| Version Name | `2.2643.6` | STATICALLY CONFIRMED | `aapt dump badging`, `AndroidManifest.xml` versionName attribute |
| Min SDK | `28` | STATICALLY CONFIRMED | `AndroidManifest.xml` `uses-sdk` element |
| Target SDK | `36` | STATICALLY CONFIRMED | `AndroidManifest.xml` `uses-sdk` element |
| Application Class | `com.legacy.passwordkeeper.PKApplication` | STATICALLY CONFIRMED | `AndroidManifest.xml` application `android:name` |
| Main Activity | `com.legacy.passwordkeeper.MainActivity` | STATICALLY CONFIRMED | `AndroidManifest.xml` launcher activity declaration |

---

## 3. Build & Signature Metadata

- **Android Gradle Plugin (AGP)**: `8.6.1` [STATICALLY CONFIRMED via `META-INF/com/android/build/gradle/app-metadata.properties`]
- **App Metadata Version**: `1.1` [STATICALLY CONFIRMED via `META-INF/com/android/build/gradle/app-metadata.properties`]
- **Source Control Repository Revision**: `ccc32b23273d90d6cb7a8d725f8aee1ddf109f66` [STATICALLY CONFIRMED via `META-INF/version-control-info.textproto`]
- **VCS System**: `GIT` [STATICALLY CONFIRMED via `META-INF/version-control-info.textproto`]

### Signing Certificate
- **Signer**: `CN=Legacy Vault Limited` [STATICALLY CONFIRMED via `keytool -printcert -jarfile`]
- **Issuer**: `CN=Legacy Vault Limited` [STATICALLY CONFIRMED]
- **Serial Number**: `b2c4d6efe664ea95dcb83b744a5ee8b5` [STATICALLY CONFIRMED]
- **Validity Period**: `Thu Mar 05 14:19:06 EST 2015` to `Sat Mar 05 14:19:06 EST 2050` [STATICALLY CONFIRMED]
- **Signature Algorithm**: `SHA256withRSA` (2048-bit RSA key) [STATICALLY CONFIRMED]
- **Certificate SHA-1**: `49:7A:EE:E5:8A:F2:A7:45:30:CE:1F:9B:25:6D:46:54:AA:41:30:20` [STATICALLY CONFIRMED]
- **Certificate SHA-256**: `2C:57:98:5A:7B:46:76:56:17:A0:29:CC:6A:FA:E8:61:B0:E5:EA:AC:3F:9C:AA:0B:29:F4:1C:49:EB:AF:37:8B` [STATICALLY CONFIRMED]
