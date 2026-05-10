# CLAUDE.md

Briefing for future Claude sessions on this repo. Read this first.

## What this is

Android POS app for **Sunmi W1401** (T1-family landscape Android terminal),
inspired by Ocha POS. Single-module Kotlin / Jetpack Compose project. Local-only
(Room/SQLite). Toggles between **Restaurant** (tables/kitchen) and **Retail**
modes.

Branch: `claude/android-pos-printer-app-ihLwW` (always develop here per the
session's git instructions; the user wants commits + pushes on this branch).

## Tech stack (already wired)

- Kotlin 2.0.21, AGP 8.5.2, Gradle 8.10.2 (wrapper committed)
- Min SDK 24, target SDK 34, JDK 17
- Jetpack Compose + Material 3
- Hilt (KAPT) + Room (KSP) + Navigation Compose + Coroutines/Flow
- ZXing core for QR. **No** other 3rd-party libs; Star and BT printing both
  use raw ESC/POS over TCP / Bluetooth SPP — no SDK dependency.

Plugins / versions live in `gradle/libs.versions.toml`.

## Sandbox build limitation

This dev container has **JDK 21 + Gradle 8.14 but NO Android SDK**, and Google's
Maven (`dl.google.com`) is **blocked (HTTP 403)**, so AGP can't download.
Don't waste cycles trying `./gradlew assembleDebug` here — it will always fail
on plugin resolution. The user builds in Android Studio on their own machine.

If you need to validate Kotlin syntax, use `kotlinc -script` on individual files
or just careful reading. Maven Central reachable for non-Google deps if needed.

## Project layout

```
app/src/main/java/com/postest/app/
├── PosApplication.kt, MainActivity.kt
├── auth/            PinHasher (PBKDF2), SessionManager (roles, perms, idle)
├── data/
│   ├── db/          AppDatabase, Daos, DatabaseSeeder (seeds default Owner
│   │                PIN=0000, sample menu, 6 tables, default SettingRow)
│   ├── entity/      Entities.kt — all entities + enums in one file
│   └── repo/        Catalog, Order, Shift, User, Audit, Settings, Report
├── di/AppModule.kt  Hilt DB + DAO providers (singleton scope)
├── print/
│   ├── PrinterService.kt   sealed PrinterTarget {Lan|Bt|None}, dispatch
│   ├── StarLanPrinter.kt   raw TCP:9100, ESC/POS raster (GS v 0)
│   ├── BtEscPosPrinter.kt  Android BluetoothSocket + SPP UUID
│   ├── EscPos              internal object in StarLanPrinter.kt — shared
│   ├── ReceiptRenderer.kt  Bitmap raster; receipt + kitchen + X/Z report
│   ├── PromptPayQr.kt      EMVCo TLV + CRC-16/CCITT-FALSE; ZXing render
│   └── TestBitmap.kt
├── ui/
│   ├── PosNavGraph.kt      Routes object + NavHost
│   ├── theme/Theme.kt
│   ├── common/Common.kt    PinPad
│   ├── auth/               LoginScreen, ManagerOverrideDialog
│   ├── pos/                PosScreen + PosViewModel (catalog grid + cart)
│   ├── tables/             TablesScreen (restaurant)
│   ├── checkout/           CheckoutScreen + CheckoutViewModel (cash/PromptPay)
│   ├── menu/MenuAdminScreen.kt   categories + products CRUD
│   ├── settings/SettingsScreen.kt
│   ├── shift/ShiftScreen.kt      open/close, X/Z reports, history
│   ├── report/ReportScreen.kt    today summary
│   ├── users/UsersAdminScreen.kt OWNER only
│   └── audit/AuditLogScreen.kt   OWNER only
└── util/Money.kt
```

## Domain model (Room)

`Category`, `Product`, `OrderTable`, `PosOrder`, `OrderItem`, `Payment`,
`Shift`, `User`, `AuditLog`, `SettingRow` — all in `data/entity/Entities.kt`.

Enums: `PosMode {RETAIL, RESTAURANT}`, `OrderStatus {OPEN, PAID, VOID}`,
`TableStatus {FREE, OCCUPIED}`, `PaymentMethod {CASH, PROMPTPAY}`,
`UserRole {OWNER, MANAGER, CASHIER}`.

Money is stored as **`Long` cents** (THB satang). Use `util/Money.kt` for
formatting/parsing — never roll your own.

Order/Payment carry `shiftId` + `userId` so reports filter by both.
Only one OPEN shift at a time (gates checkout in PosScreen).

## Permissions matrix

In `SessionManager.permissionsFor(role)`:
- **OWNER**: all
- **MANAGER**: TAKE_ORDER, OPEN/CLOSE_SHIFT, VOID, REFUND, PRICE_OVERRIDE,
  EDIT_MENU, EDIT_SETTINGS, VIEW_REPORTS
- **CASHIER**: TAKE_ORDER, OPEN_SHIFT — restricted actions go through
  `ManagerOverrideDialog`; both user IDs recorded in `AuditLog`.

## Conventions

- All money in cents (Long). Format with `Money.fmt(cents)`.
- All printer output rendered as **monochrome bitmap** (Thai glyphs). Don't
  attempt text-mode ESC/POS — it breaks Thai.
- Bilingual fields: `nameEn` + `nameTh` on Category/Product. Receipts switch
  by `SettingRow.language`.
- `PrinterTarget` chosen via extension `SettingRow.receiptPrinterTarget()` /
  `kitchenPrinterTarget()` in `print/PrinterService.kt`.
- ViewModels are `@HiltViewModel`. UI state is exposed via `StateFlow`, not
  Compose `mutableStateOf`, except for transient form state.

## Pending / known follow-ups (not yet implemented)

- **CSV export UI** for reports — repo + report exist, no UI button.
- **Idle auto-lock timer** — `SessionManager.isIdleExpired()` exists but
  isn't wired to a timer in `MainActivity`. To wire: launch a coroutine that
  ticks every 30s, checks `isIdleExpired(settings.idleLockMinutes)`, and
  `signOut()` if true.
- **Default-PIN warning banner** — string `default_owner_warning` exists, no
  banner shown. Add to PosScreen top bar when current user is the default
  Owner with PIN still `0000`.
- **Switch-user button** — only available indirectly via process restart.
  Add a top-bar action calling `session.signOut()` and routing to Login.
- **Saved-state Tables→POS handoff** uses `previousBackStackEntry?.savedStateHandle`
  in `PosScreen.kt`. The PosVM's `tableId` isn't yet derived from the resumed
  order — minor: when resuming, query `orderRepo.byId(id).tableId` and call
  `vm.setTable(...)`.
- **Migration to Icons.AutoMirrored.Filled.ArrowBack** — current uses
  deprecated `Icons.Filled.ArrowBack` (warning, not error).
- **Reports filtering by shift / date range** — currently only "today".
- **PromptPay merchant tag** — uses sub-tag `01` for both mobile and NID.
  If you encounter a banking app that rejects, switch NID to `02`.
- **No runtime permission requests** — Bluetooth printing requires user to
  grant `BLUETOOTH_CONNECT` via Android settings. Add a runtime request flow
  in `SettingsScreen` if needed.

## Plan file

The accepted plan lives at:
`/root/.claude/plans/create-android-app-that-floofy-steele.md`

It still reflects the agreed scope (Restaurant+Retail toggle, Core MVP,
local-only, Thai+English, shift management, multi-user roles).

## Conversation history summary

User asked for an Android POS for Sunmi W1401 that:
- prints to Star TSP100III over LAN and to a Bluetooth printer
- works like Ocha POS
- and (separately) wants fresh Android flashed onto the W1401

After clarifying scope they chose: Restaurant+Retail toggle, Core MVP,
local-only, Thai+English. Then asked to **add shift management**, then
**add multi-user roles**. Plan was approved and the initial implementation
was committed and pushed. README explains the Star ESC/POS-mode setup and
gives a frank addendum: flashing vanilla AOSP on a Sunmi W14xx is not
practically feasible (locked bootloader, no AOSP/GSI release, no community
ROM); recommended path is SunmiOS factory-reset + kiosk-launcher mode.

## Git etiquette for this repo

- Always develop on `claude/android-pos-printer-app-ihLwW`.
- Push with `git push -u origin <branch>`. Retry network failures with
  exponential backoff per session instructions.
- Don't open a PR unless the user explicitly asks.
- GitHub MCP is scoped to `readsher/pos-test` only.
