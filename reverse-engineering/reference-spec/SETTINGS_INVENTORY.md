# BlackBerry Password Keeper — Settings Inventory Report

## 1. Complete Preferences Specification

Recovered directly from the 12 decompiled preference resource definitions (`res/xml/pref_*.xml`) and resource arrays (`res/values/arrays.xml`):

### A. Lock Preferences (`res/xml/pref_lock.xml`)

| Preference Key | Title / Description | Type | Default Value | Available Option Values | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `pref_lock_on_minimize` | Lock on minimize | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |
| `pref_lock_on_screen_timeout` | Lock on screen timeout | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |
| `pref_lock_on_timeout` | Lock on timeout | `ListPreference` | `"900"` (15m) | `30` (30s), `60` (1m), `120` (2m), `300` (5m), `600` (10m), `900` (15m), `1800` (30m), `3600` (1h), `-1` (Never) | STATICALLY CONFIRMED |

### B. Copied Text Preferences (`res/xml/pref_copied_text.xml`)

| Preference Key | Title / Description | Type | Default Value | Available Option Values | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `pref_allow_clipboard_copy` | Allow clipboard copy | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |
| `pref_clear_clipboard_timeout` | Clear clipboard timeout | `ListPreference` | `"600"` (10m) | `30` (30s), `60` (1m), `120` (2m), `300` (5m), `600` (10m), `900` (15m), `-1` (Never) | STATICALLY CONFIRMED |
| `pref_clear_clipboard_on_lock` | Clear clipboard on lock | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |

### C. Security Preferences (`res/xml/pref_security.xml`)

| Preference Key | Title / Description | Type | Default Value | Available Option Values | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `change_password_button` | Change password | Action (`Intent`) | — | Launches `ChangePasswordActivity` | STATICALLY CONFIRMED |
| `pref_password_last_changed_time` | Password last changed | Read-only Summary | Computed | Formatted date string from database metadata | STATICALLY CONFIRMED |
| `pref_fingerprint_authentication` | Biometric unlock | `CheckBoxPreference` | `false` | `true`, `false` (launches enrollment) | STATICALLY CONFIRMED |
| `pref_fingerprint_password_reminder` | Biometric reminder | `CheckBoxPreference` | `true` | `true`, `false` (15-login countdown) | STATICALLY CONFIRMED |

### D. Random Password Preferences (`res/xml/pref_random.xml`)

| Preference Key | Title / Description | Type | Default Value | Available Option Values | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `pref_random_length` | Password length | `ListPreference` | `"12"` | `4`, `5`, `6`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `14`, `15`, `16` | STATICALLY CONFIRMED |
| `pref_random_letters` | Include letters | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |
| `pref_random_numbers` | Include numbers | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |
| `pref_random_symbols` | Include symbols | `CheckBoxPreference` | `true` | `true`, `false` | STATICALLY CONFIRMED |

### E. General & Theme Preferences (`res/xml/pref_general.xml`)

| Preference Key | Title / Description | Type | Default Value | Available Option Values | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `pref_dark_theme` | Dark theme | `CheckBoxPreference` | `false` | `true`, `false` | STATICALLY CONFIRMED |
| `navigation_drawer_last_view` | Last selected drawer filter | `Integer` | `0` (All) | `0` (All), `1` (Favorites), `2` (Passwords), `3` (Notes), `4` (Lists) | STATICALLY CONFIRMED |

---

## 2. LockSelf Persistence Mapping

LockSelf models these exact keys and types via `AppPreferences.kt` backed by encrypted Android preferences, preserving the exact default values, array entries, and lifecycle triggers.
