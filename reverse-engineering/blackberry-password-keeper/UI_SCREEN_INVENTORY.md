# BlackBerry Password Keeper — UI Screen Inventory Report

## 1. Screen-by-Screen Inventory

| # | Screen Name | Class Name | Layout XML Reference | Key Visual & Interactive Components | Evidence Classification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **Initial Setup Screen** | `SetupActivity` | `res/layout/activity_setup.xml` | Welcome header, master password entry, password confirmation, strength indicator meter, "Create Password" button. | STATICALLY CONFIRMED |
| 2 | **Vault Lock / Unlock Screen** | `LockActivity` | `res/layout/activity_lock.xml` | Shield logo, vault locked indicator, master password input field with reveal eye, fingerprint icon button, "Unlock" button, numeric attempt countdown. | STATICALLY CONFIRMED |
| 3 | **Main Dashboard Screen** | `MainActivity` | `res/layout/activity_main.xml` | Top app bar with search icon, category title, hamburger drawer toggle, empty state illustration, RecyclerView list with 5dp colored category strips, multi-action Floating Action Button (FAB). | STATICALLY CONFIRMED |
| 4 | **Navigation Drawer** | `NavigationDrawerFragment` | `res/layout/fragment_navigation_drawer.xml` | Header with Password Keeper branding, category rows with numeric badges (`All Records`, `Favorites`, `Passwords`, `Notes`, `Lists`), divider, `Settings`, and `Lock Vault`. | STATICALLY CONFIRMED |
| 5 | **Record Detail Screen** | `ItemDetailActivity` | `res/layout/activity_item_detail.xml` | Up navigation arrow, category banner, title heading, username with copy button, masked password with reveal & copy buttons, website launcher link, notes block, checklist items, edit FAB. | STATICALLY CONFIRMED |
| 6 | **Add Record Screen** | `AddRecordActivity` | `res/layout/activity_add.xml` | Record type segmented picker (`Password`, `Note`, `List`), input fields for title/username/password/url/note, password generator trigger icon, save action checkmark in top bar. | STATICALLY CONFIRMED |
| 7 | **Edit Record Screen** | `EditRecordActivity` | `res/layout/activity_edit.xml` | Top bar with "Discard" (X) and "Save" (check), dynamic field list, add custom field button, tag selector chip group, delete record button. | STATICALLY CONFIRMED |
| 8 | **Password History Screen** | `PasswordHistoryActivity` | `res/layout/activity_password_history.xml` | List of retired passwords, active date range labels, masked dots, copy on long-press, individual reveal eye toggles. | STATICALLY CONFIRMED |
| 9 | **Password Generator Dialog** | `H0.g` / `H0.i` | `res/layout/dialog_password_generator.xml` | Generated password text box (Inconsolata font), length slider (4-16), checkboxes for Letters/Numbers/Symbols, "Regenerate" icon button, "Accept" button. | STATICALLY CONFIRMED |
| 10 | **Settings Hub Screen** | `SettingsActivity` | `res/xml/pref_headers.xml` | Categorized preference rows: General, Form Fill / Autofill, Cloud Backup, Import & Export, Copied Text, Lock, Security, Random Password, About. | STATICALLY CONFIRMED |
| 11 | **Lock Preferences** | `SettingsActivity$LockPreferenceFragment` | `res/xml/pref_lock.xml` | Checkbox: Lock on minimize; Checkbox: Lock on screen timeout; ListPreference: Lock on timeout (30s to 1h, Never). | STATICALLY CONFIRMED |
| 12 | **Copied Text Preferences** | `SettingsActivity$CopiedTextPreferenceFragment` | `res/xml/pref_copied_text.xml` | Checkbox: Allow clipboard copy; ListPreference: Clear clipboard timeout (30s to 15m, Never); Checkbox: Clear clipboard on lock. | STATICALLY CONFIRMED |
| 13 | **Security Preferences** | `SettingsActivity$SecurityPreferenceFragment` | `res/xml/pref_security.xml` | "Change Password" button row, "Password last changed" info display, Checkbox: Biometric unlock toggle, Checkbox: Password reminder toggle. | STATICALLY CONFIRMED |
| 14 | **Change Password Screen** | `ChangePasswordActivity` | `res/layout/activity_change_password.xml` | Current password input, new password input with strength meter, confirm password input, "Save" action. | STATICALLY CONFIRMED |
| 15 | **Export Screen** | `ExportActivity` | `res/layout/activity_export.xml` | Format selection (CSV / Encrypted), output folder picker, export password input, progress bar, completion notification. | STATICALLY CONFIRMED |
| 16 | **Import Screen** | `ImportActivity` | `res/layout/activity_import.xml` | File selection button, duplicate resolution picker (Keep Existing / Overwrite / Keep Both), import progress counter. | STATICALLY CONFIRMED |
| 17 | **Biometric Setup Screen** | `EnableFingerprintActivity` | `res/layout/activity_with_fragment.xml` | Informational fingerprint graphic, explanation text, master password verification step, system fingerprint confirmation prompt. | STATICALLY CONFIRMED |
| 18 | **What's New Screen** | `WhatsNewActivity` | `res/layout/activity_whats_new.xml` | Version banner, bulleted feature list, "Get Started" dismissal button. | STATICALLY CONFIRMED |
| 19 | **Open Source Licenses** | `OpenSourceActivity` | `res/layout/activity_open_source.xml` | WebView rendering open source notices and licenses. | STATICALLY CONFIRMED |

---

## 2. Visual Density, Typography & Dimensions

- **Typography**: Roboto Regular for labels; Inconsolata / Monospace for passwords and codes.
- **Card Spacing**: 8dp vertical card margins, 16dp horizontal screen padding.
- **Accent Strip**: 5dp width pinned to left edge of each record card (`#D12B58` for Passwords, `#00B0D4` for Notes, `#46A66C` for Lists).
- **Notch Indicator**: 3dp vertical height track with circular or rounded rectangular sliding notch.
