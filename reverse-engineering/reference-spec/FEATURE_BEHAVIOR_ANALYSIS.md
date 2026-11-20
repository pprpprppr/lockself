# BlackBerry Password Keeper — Feature Behavior Analysis Report

## 1. Feature-by-Feature Technical Breakdown

### A. Password Records
- **Fields & Order**: Title $\to$ Username $\to$ Password (with reveal toggle and strength meter) $\to$ Website URL $\to$ Notes $\to$ Custom Fields / Tags. [STATICALLY CONFIRMED via `view_record_details.xml` and `a.java`]
- **Password Visibility**: Masked by default (`InputType.TYPE_TEXT_VARIATION_PASSWORD`). Clicking eye icon toggles to `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` rendered in monospace `assets/Inconsolata-Bold.ttf`. [STATICALLY CONFIRMED]
- **Copy Action**: Clicking the copy icon copies the credential to the Android clipboard tagged with `"bb.pk"`, immediately triggering the clipboard auto-wipe countdown timer. [STATICALLY CONFIRMED via `PKApplication.java:596`]
- **Website Navigation**: Clicking the website field launches the URL in the system browser or Chrome Custom Tabs (`android.support.customtabs.action.CustomTabsService`). [STATICALLY CONFIRMED]

### B. Notes
- **Presentation**: Single card with bold title and multiline readable content. [STATICALLY CONFIRMED]
- **Interaction**: Tap to view full text; edit action allows editing header and content; search indexes both title and note content. [STATICALLY CONFIRMED]

### C. Checklists
- **Structure**: Title plus ordered sequence of checklist rows (`ListCheckBoxSubfield`). [STATICALLY CONFIRMED via `P0/g.java`]
- **Behavior**: Tapping a checkbox toggles its boolean state immediately and persists it to the database without requiring explicit "Save" button press. [STATICALLY CONFIRMED via `I0/j.java`]
- **Editing**: Adding new items, deleting existing items, and reordering items. [STATICALLY CONFIRMED]

### D. Password Generator
- **Controls**:
  - Length selection: 4 to 16 characters (Default: 12). [STATICALLY CONFIRMED via `pref_random_pwd_length_values`]
  - Character pool toggles: Letters (Uppercase & Lowercase), Numbers (`0-9`), Symbols (`!@#$%^&*...`). [STATICALLY CONFIRMED via `pref_random.xml`]
- **Algorithm (`P0.l` & `LN0.c`)**:
  - Uses `SecureRandom` to draw characters from the active pools.
  - Generates 3 candidate passwords.
  - Scores each candidate using the `P0.k` strength evaluation engine.
  - Selects and displays the candidate with the highest evaluated security score. [STATICALLY CONFIRMED via `P0/l.java` and `LN0/c.java`]

### E. Password Strength Indicator (`PasswordSecurityField`)
- **Visual Meter**: Continuous spectrum gradient track (`security_progress_bar.png` passing through Red $\to$ Orange $\to$ Yellow $\to$ Green) with an overlaid sliding notch (`progress_indicator`). [STATICALLY CONFIRMED via `PasswordSecurityField.java`]
- **Motion Animation**:
  - Sliding notch translation: `notchX = (score / 100.0f) * (trackWidth - notchWidth)`.
  - Animated smoothly with `ValueAnimator` and `AccelerateInterpolator(2.0f)` over `R.integer.config_shortAnimTime`. [STATICALLY CONFIRMED via `PasswordSecurityField.java:95-125`]
- **Weakness Warnings**: Displays `@string/chosen_password_too_short` if length < 6, or `@string/chosen_password_too_common` if password appears in dictionary/forbidden list. [STATICALLY CONFIRMED via `PasswordSecurityField.java:180-205`]

### F. Password History
- **Tracking Invariant**: Whenever a record's password field is modified and saved, the previous password and current timestamp are inserted into `subfields` as a `HistoricalPasswordSubfield` (`P0.f`). [STATICALLY CONFIRMED via `P0/m.java` and `P0/f.java`]
- **Display**: Accessed via `PasswordHistoryActivity` or overflow menu; displays list of previous passwords, date range during which they were active, and unmask toggles. [STATICALLY CONFIRMED via `I0/t.java`]

### G. Tags & Search
- **Search Capabilities**: Real-time filtering in `MainActivity` searching across title, username, website, and tags simultaneously. [STATICALLY CONFIRMED via `MainActivity.java`]
- **Tags Structure**: Multiple alphanumeric tags per record, selectable for one-tap filtering in the navigation drawer or search view. [STATICALLY CONFIRMED]

### H. Biometric Authentication
- **Enrollment Workflow**: User navigates to Settings $\to$ Security $\to$ Fingerprint $\to$ enters master password $\to$ system enrolls key in `AndroidKeyStore`. [STATICALLY CONFIRMED via `EnableFingerprintActivity.java`]
- **Password Reminder Counter**: Decrements a counter from 15 logins; every 15 logins, requires full master password entry to prevent password amnesia. [STATICALLY CONFIRMED via `J0/c.java:94`]

### I. Automatic Locking
- **Timeout Options**: 30 seconds, 1 minute, 2 minutes, 5 minutes, 10 minutes, 15 minutes (Default), 30 minutes, 1 hour, Never (`-1`). [STATICALLY CONFIRMED via `pref_lock_on_timeout_values`]
- **Triggers**:
  - Inactivity timeout expired.
  - App sent to background / minimize (`pref_lock_on_minimize` enabled by default).
  - Screen turned off (`pref_lock_on_screen_timeout` enabled by default).
  - User explicitly selects "Lock Vault" from navigation drawer. [STATICALLY CONFIRMED via `PKApplication.java` and `LockAlarmReceiver.java`]

### J. Clipboard Auto-Wipe
- **Timeout Options**: 30 seconds, 1 minute, 2 minutes, 5 minutes, 10 minutes (Default), 15 minutes, Never (`-1`). [STATICALLY CONFIRMED via `pref_clear_clipboard_timeout_values`]
- **Triggers**:
  - Countdown timer finishes: `ClipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))`.
  - App locked: Clears clipboard immediately if `pref_clear_clipboard_on_lock` is true. [STATICALLY CONFIRMED via `PKApplication.java`]

### K. Backup and Restore
- **BlackBerry CSV Format**: 18 standard columns; custom fields encoded as JSON in column 7.
- **Import Engine**: Handles quoted strings, commas within quotes, multiline values, and legacy UNIX timestamp formats. [STATICALLY CONFIRMED via `Q0/b.java`]
