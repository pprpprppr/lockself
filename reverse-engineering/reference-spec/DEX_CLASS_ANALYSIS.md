# Legacy Password Keeper — DEX Class & Package Analysis Report

## 1. Package Organization & Deobfuscation Map

Legacy Password Keeper applies ProGuard/R8 bytecode obfuscation to internal proprietary packages while retaining Android framework component names. Reverse engineering analysis maps the obfuscated packages as follows:

| Obfuscated Package | Deobfuscated Role | Core Functional Responsibility | Evidence Classification |
| :--- | :--- | :--- | :--- |
| `P0` | `data.model` & `data.persistence` | Domain entities, enums, SQLCipher persistence, RecordManager, cryptography helpers, password strength evaluation | STATICALLY CONFIRMED |
| `I0` | `ui.viewholders` & `ui.adapters` | RecyclerView ViewHolders, item renderers, password reveal toggles, checklist item binders | STATICALLY CONFIRMED |
| `Q0` | `data.importexport` | 18-column CSV export/import engine, JSON custom fields serialization, legacy format parsers | STATICALLY CONFIRMED |
| `J0` | `security.biometrics` | AndroidKeyStore RSA-OAEP key generation, fingerprint crypto wrapping, password reminder counter | STATICALLY CONFIRMED |
| `D0` | `ui.common` | Shared fragments, base dialogs, custom styling attributes, progress bars | STATICALLY CONFIRMED |
| `H0` | `ui.dialogs` | Password generator popup dialogs, confirmation dialogs, progress indicators | STATICALLY CONFIRMED |
| `M0` | `ui.navigation` | Drawer category enums (`ALL`, `FAVOURITES`, `PASSWORDS`, `NOTES`, `LISTS`, `SETTINGS`), count badges | STATICALLY CONFIRMED |
| `N0` | `util.helpers` | Formatting utilities, timestamp converters, screen state detectors | STATICALLY CONFIRMED |
| `LN0` | `util.generator` | Multi-candidate password generation and candidate security comparison | STATICALLY CONFIRMED |

---

## 2. Core Class Inventory & Responsibilities

### Persistence & Data Management (`P0`)

#### `P0.p` — `RecordManager`
- **Role**: Primary singleton controller for SQLCipher database operations. [STATICALLY CONFIRMED]
- **Responsibilities**:
  - Dynamically loads native library: `System.loadLibrary("sqlcipher");` (`P0/p.java:1762`).
  - Opens/creates database: `SQLiteDatabase.openOrCreateDatabase(N("records.db"), P(cArrG), ...)` (`P0/p.java:1275`).
  - Key translation: `P(char[] cArr)` encodes passphrase to UTF-8 byte array (`P0/p.java:1770`).
  - Table initialization: `D0(SQLiteDatabase sQLiteDatabase)` executes schema DDL for `records`, `fields`, `subfields`, and `metadata` (`P0/p.java:1298-1301`).
  - Migration handler: `F0(int i5)` manages version migrations up to version 7 (`P0/p.java:1320-1360`).
  - CRUD operations: Full ACID transactions for querying, inserting, updating, and deleting records with cascading relationship enforcement.

#### `P0.m` — `Record`
- **Role**: In-memory domain representation of a vault record. [STATICALLY CONFIRMED]
- **Fields**:
  - `f2541a` (`long id`): Primary key.
  - `f2542b` (`boolean favorite`): Favorite flag.
  - `f2543c` (`P0.q type`): Enum record type (`PASSWORD`, `NOTE`, `CLIPBOARD`, `LIST`).
  - `f2544d` (`long lastModified`): UNIX epoch timestamp in seconds.
  - `f2545e` (`String uid`): Unique UUID string identifier.
  - `f2546f` (`List<P0.c> fields`): List of associated fields.
  - `f2547g` (`List<String> tags`): Assigned organization tags.

#### `P0.c` — `Field`
- **Role**: Individual attribute of a record. [STATICALLY CONFIRMED]
- **Fields**:
  - `f2397a` (`long id`): Primary key.
  - `f2398b` (`long recordId`): Foreign key referencing `records.id`.
  - `f2399c` (`P0.d type`): Field type enum.
  - `f2400d` (`String name`): Custom field label or null for standard fields.
  - `f2401e` (`String value`): Plaintext field string value.
  - `f2402f` (`List<P0.t> subfields`): Attached subfields.

#### `P0.t` — `Subfield` (Abstract Base)
- **Role**: Base polymorphic entity for extended field properties. [STATICALLY CONFIRMED]
- **Fields**: `id` (`long`), `type` (`P0.d`).
- **Abstract Methods**: `j(ContentValues)` for persistence, `i()` for cloning, `f(t)` for deep equality.

#### `P0.f` — `HistoricalPasswordSubfield`
- **Role**: Tracks previous password values and their retirement timestamps. [STATICALLY CONFIRMED via `P0/f.java`]
- **Fields**:
  - `f2418c` (`String password`): Stored in `subfields.tVal`.
  - `f2419d` (`long changedTimeSeconds`): Stored in `subfields.iVal`.

#### `P0.g` — `ListCheckBoxSubfield`
- **Role**: Item in a checklist record. [STATICALLY CONFIRMED via `P0/g.java`]
- **Fields**:
  - `f2420c` (`String itemText`): Stored in `subfields.tVal`.
  - `f2421d` (`boolean isChecked`): Stored in `subfields.boolVal`.
  - `f2422e` (`int orderIndex`): Stored in `subfields.iVal`.

#### `P0.i` — `PasswordSetTimeSubfield`
- **Role**: Records the exact timestamp when a credential's current password was assigned. [STATICALLY CONFIRMED via `P0/i.java`]
- **Fields**: `f2423c` (`long setTimeSeconds`): Stored in `subfields.iVal`.

---

## 3. Enumeration Definitions

### `P0.q` — `RecordType`
```java
public enum q {
    PASSWORD,  // 0: Credential record with username, password, site
    NOTE,      // 1: Freeform text note
    CLIPBOARD, // 2: Ephemeral copied item
    LIST       // 3: Interactive checklist
}
```
[STATICALLY CONFIRMED via `P0/q.java`]

### `P0.d` — `FieldType`
```java
public enum d {
    TITLE,                 // 0
    USERNAME,              // 1
    WEBSITE,               // 2
    PASSWORD,              // 3
    NOTE,                  // 4
    SECURITY_QUESTION,     // 5
    HISTORICAL_PASSWORD,   // 6
    ICON,                  // 7
    CUSTOM,                // 8
    PASSWORD_SET_TIME,     // 9
    TRUSTED_APPLICATIONS,  // 10
    LIST,                  // 11
    LIST_CHECK_BOX,        // 12
    TRUSTED_APP            // 13
}
```
[STATICALLY CONFIRMED via `P0/d.java`]

---

## 4. Algorithmic Modules

### Password Strength Scoring (`P0.k`)
- **Normalization**: Unicode NFC normalization via `Normalizer.normalize(str, Normalizer.Form.NFC)` (`P0/k.java:21`).
- **Scoring Engine**:
  - Base length score: `length * 4`.
  - Variety additions: Uppercase bonus, Lowercase bonus, Digit bonus (`digits * 3`), Symbol bonus (`symbols * 5`), Middle numbers/symbols bonus (`middleCount * 2`).
  - Combination bonus: `length >= 8` with at least 3 character classes adds `ruleBonus * 2`.
  - Penalties: Letters-only (`-length`), numbers-only (`-length`), consecutive character class penalties (`-consecutiveUpper`, `-consecutiveLower`, `-consecutiveDigits * 2`, `-consecutiveSymbols * 4`), sequential patterns 3+ (`-sequentialCount * 3`), repeated characters (`-repeatCount * 5`).
  - Bounds: Clamped strictly to `[0, 100]`.
  - Status classification: If `length < 6`, returns `REASON_TOO_SHORT`; otherwise `REASON_NONE`. [STATICALLY CONFIRMED via `P0/k.java:140-155`]

### Multi-Candidate Password Generator (`P0.l` & `LN0.c`)
- **Cryptographic Randomness**: Direct usage of `java.security.SecureRandom` (`P0/l.java:33`).
- **Bitmask Pool Selection**:
  - Bit 0 (`1`): Uppercase & Lowercase letters.
  - Bit 1 (`2`): Numbers (`0-9`).
  - Bit 2 (`4`): Symbols (`!@#$%^&*...`).
- **Selection Algorithm**: Generates multiple candidates meeting character composition criteria, analyzes candidates with `P0.k`, and selects the candidate with the highest evaluated security score. [STATICALLY CONFIRMED via `P0/l.java` and `LN0/c.java`]

---

## 5. Import/Export Engine (`Q0`)

### `Q0.a` — `CsvRecordExporter`
- **Output Columns**: 18 fixed columns (`url, username, password, extra, name, grouping, fav, customFields, lastModifiedTime, uid, usernameLabel, passwordLabel, websiteLabel, notesLabel, passwordSetDate, flags, imageIndex, dataVersion`). [STATICALLY CONFIRMED via `Q0/a.java:18`]
- **Escaping**: RFC 4180 standard escaping; quotes replaced with `""` and wrapped in quotes.
- **Checklist & Custom Fields**: Encoded as embedded JSON in column 7 (`customFields`):
  `{"cf": [{"chk_label": "item text", "chk_checked": true, "chk_order": 0, "cft": 28675}]}`. [STATICALLY CONFIRMED via `Q0/a.java:220` and `Q0/i.java`]

### `Q0.b` — `CsvRecordImporter`
- **Streaming Parser**: Byte-by-byte finite state machine (`NEXT_PART`, `IN_PART`, `DQUOTE`, `DQUOTE_QUOTE`) handling multiline text, commas inside quoted fields, and quote escapes. [STATICALLY CONFIRMED via `Q0/b.java`]
- **Format Compatibility**: Seamlessly imports CSV files with or without header rows, legacy Legacy Vault desktop backups, and timestamps in seconds or milliseconds. [STATICALLY CONFIRMED]
