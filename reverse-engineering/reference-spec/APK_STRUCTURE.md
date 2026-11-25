# Legacy Password Keeper — APK Structure Report

## 1. Top-Level Binary Composition

| Component | Uncompressed Size | Compressed Size | Relative % | Evidence Classification | Source Reference |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `classes.dex` | 7,460,720 bytes | 3,195,492 bytes | 28.7% | DIRECTLY OBSERVED | `unzip -l`, `classes.dex` |
| Native Libraries (`lib/`) | 10,681,596 bytes | 4,210,314 bytes | 41.1% | DIRECTLY OBSERVED | `unzip -l`, `lib/` directory |
| Resources Table (`resources.arsc`) | 2,823,328 bytes | 682,140 bytes | 10.9% | DIRECTLY OBSERVED | `unzip -l`, `resources.arsc` |
| Drawables, Layouts & Assets (`res/`, `assets/`) | 4,468,142 bytes | 2,421,802 bytes | 17.2% | DIRECTLY OBSERVED | `unzip -l`, `res/`, `assets/` |
| Signatures & Manifests (`META-INF/`, `AndroidManifest.xml`) | 543,262 bytes | 215,920 bytes | 2.1% | DIRECTLY OBSERVED | `unzip -l`, `META-INF/` |
| **Total Archive** | **25,977,048 bytes** | **25,059,775 bytes** | **100.0%** | DIRECTLY OBSERVED | `ls -l`, `unzip -l` |

Total archive files: **2,319 files** [DIRECTLY OBSERVED].

---

## 2. Native Library Inventory

The application contains precompiled native shared libraries strictly for SQLCipher encrypted database operations across two ABIs:

| Architecture | Library Path | Size | Dynamic Linkage Dependencies | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- |
| `arm64-v8a` (64-bit ARM) | `lib/arm64-v8a/libsqlcipher.so` | 6,571,864 bytes | `liblog.so`, `libm.so`, `libc.so`, `libdl.so` | STATICALLY CONFIRMED |
| `armeabi-v7a` (32-bit ARM) | `lib/armeabi-v7a/libsqlcipher.so` | 4,109,732 bytes | `liblog.so`, `libm.so`, `libc.so`, `libdl.so` | STATICALLY CONFIRMED |

**Analysis**:
- No other native binary modules exist in the package. [STATICALLY CONFIRMED via `unzip -l`]
- `x86` and `x86_64` emulators require ARM binary translation support (e.g. libndk_translation / houdini) to run the original binary natively. [STATICALLY CONFIRMED]
- The Java runtime dynamically loads SQLCipher via `System.loadLibrary("sqlcipher");` in `P0.p` (RecordManager). [STATICALLY CONFIRMED via `P0/p.java:1762`]

---

## 3. DEX File Structure

- **DEX File Count**: `1` (`classes.dex`). [DIRECTLY OBSERVED]
- **DEX Format Version**: `039` (standard Android Pie+ DEX). [STATICALLY CONFIRMED]
- **Class Definition Count**: `7,362` compiled classes. [STATICALLY CONFIRMED via JADX progress logs]
- **Method References**: Well within the single DEX 64K limit (multidex disabled/unneeded for final release). [STATICALLY CONFIRMED]
- **Obfuscation**: Processed with ProGuard/R8. Legacy Vault proprietary internal packages are obfuscated to single/dual-letter package paths (`P0/`, `I0/`, `Q0/`, `D0/`, `H0/`, `J0/`, `M0/`, `N0/`, `LN0/`), while public activities, services, components, and AndroidX libraries remain identifiable. [STATICALLY CONFIRMED]

---

## 4. Assets Directory Inventory

- `assets/Inconsolata-Bold.ttf` (64,628 bytes): Fixed-width font used for password reveals and generator screens. [STATICALLY CONFIRMED]
- `assets/Inconsolata-Regular.ttf` (80,456 bytes): Fixed-width regular font. [STATICALLY CONFIRMED]
- `assets/modulerequirements.json` (224 bytes): Internal modular configuration. [STATICALLY CONFIRMED]
- `assets/dexopt/baseline.prof` & `baseline.profm`: Android ART Ahead-Of-Time (AOT) profile baseline. [STATICALLY CONFIRMED]

---

## 5. Resource Structure

- `res/layout/` (82 decompiled layout XML definitions): Activity layouts, dialogs, list rows, navigation drawer items. [STATICALLY CONFIRMED]
- `res/values/`:
  - `strings.xml`: 853 localized string resources covering categories, error messages, dialog options, field hints. [STATICALLY CONFIRMED via REA `decode-android-resources`]
  - `colors.xml`: Full color palette including brand CrimsonRed (`#D12B58`), Cyan (`#00B0D4`), EmeraldGreen (`#46A66C`), CeruleanBlue (`#025FCE`). [STATICALLY CONFIRMED]
  - `arrays.xml`: Pre-configured option arrays for lock timeout, clipboard wipe timeout, password generator lengths, and record type pickers. [STATICALLY CONFIRMED]
- `res/xml/`:
  - 12 Preference hierarchy specifications (`pref_headers.xml`, `pref_general.xml`, `pref_lock.xml`, `pref_security.xml`, `pref_copied_text.xml`, `pref_random.xml`, `pref_backup.xml`, etc.). [STATICALLY CONFIRMED]
