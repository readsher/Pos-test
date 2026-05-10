# PosTest — Android POS for Sunmi W1401

A small Android POS app inspired by Ocha POS (Thailand). Built for landscape
14" terminals like the Sunmi W1401 (T1-family), but runs on any Android 7+
device.

## What it does

- **Two modes** (toggle in Settings):
  - **Restaurant** — tables, open tabs, "send to kitchen" tickets
  - **Retail / quick-service** — straight cart → checkout
- **Printing**:
  - **Star TSP100III over LAN** — raw TCP to port 9100, ESC/POS raster (printer must be in ESC/POS emulation mode — see below)
  - **Bluetooth ESC/POS** — generic 58/80 mm printers via SPP
- **Payment**: Cash (with tendered → change calc) and **PromptPay QR** (generated locally, EMVCo-compliant)
- **Shift management**: open shift with float, X-report (mid-shift), Z-report (printed on close), expected-vs-counted cash with over/short
- **Multi-user roles**: OWNER / MANAGER / CASHIER with per-user PIN login (PBKDF2-hashed). Manager-override prompt for restricted actions; everything written to an audit log
- **Reports**: today by hour / category / payment method / user
- **i18n**: Thai + English, switchable at runtime
- **Local-only**: SQLite (Room). No cloud.

## Build

Requirements:
- Android Studio Hedgehog or newer (or AGP 8.5.x via CLI)
- JDK 17
- Android SDK 34, build-tools 34.x

```bash
# Open in Android Studio and sync, OR build from CLI:
./gradlew :app:assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

Sideload onto the W1401:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## First run

- A default user **Owner** is created with PIN `0000`. **Change this immediately** in Settings → Users → Reset PIN.
- Sample categories, products, and 6 tables are seeded so you have something to play with.

## Star TSP100III setup

The TSP100III ships in **StarLine** mode by default. This app sends standard ESC/POS raster commands, so the printer needs to be in **ESC/POS emulation**:

1. Install Star **futurePRNT** on a Windows PC and connect to the printer.
2. *Star Configuration Utility* → **Emulation** → switch to **ESC/POS Mode**.
3. Save & reboot the printer.
4. In the app: Settings → enter the printer IP (assign a static IP first via futurePRNT) → Receipt printer = **Star LAN** → tap **Test print**.

> Want to use the official StarPRNT SDK instead? Drop `stario.aar` and `starioextension.aar` into `app/libs/`, uncomment the `files(...)` deps in `app/build.gradle.kts`, and replace the body of `StarLanPrinter.printBitmap` with the SDK's `StarIOPort` + `StarIoExt.appendBitmap` calls. The bitmap input is already produced by `ReceiptRenderer`.

## Bluetooth printer setup

1. Pair the printer in Android **Settings → Bluetooth** first.
2. In this app: Settings → tap the paired device under "Bluetooth printer".
3. Receipt printer = **Bluetooth** → **Test print**.

## PromptPay

1. Settings → enter your PromptPay ID (mobile number, citizen ID, or 15-digit eWallet ID).
2. At checkout pick **PromptPay** → QR is generated for the exact total.

## Project layout

```
app/src/main/java/com/postest/app/
├── PosApplication.kt, MainActivity.kt
├── auth/        — SessionManager, PinHasher
├── data/
│   ├── db/      — Room: AppDatabase, DAOs, DatabaseSeeder
│   ├── entity/  — Category, Product, OrderTable, PosOrder, OrderItem,
│   │              Payment, Shift, User, AuditLog, SettingRow
│   └── repo/    — Catalog, Order, Shift, User, Audit, Settings, Report
├── di/          — Hilt module(s)
├── print/       — PrinterService, StarLanPrinter, BtEscPosPrinter,
│                  ReceiptRenderer, PromptPayQr, EscPos
├── ui/
│   ├── auth/    — Login, ManagerOverrideDialog
│   ├── pos/     — Main POS screen + ViewModel
│   ├── tables/  — Tables grid (restaurant)
│   ├── checkout/— Cash + PromptPay flow
│   ├── menu/    — Menu admin (categories + products)
│   ├── settings/— App + printer settings
│   ├── shift/   — Open/close, X/Z reports, history
│   ├── report/  — Sales reports
│   ├── users/   — User admin (OWNER only)
│   └── audit/   — Audit log viewer (OWNER only)
└── util/        — Money formatter
```

## Roles & permissions

| Role | Permissions |
|---|---|
| **OWNER** | Everything: users, audit log, all settings, void/refund/override, reports, menu, shift |
| **MANAGER** | Open/close shift, void/refund, price override, edit menu/settings, reports |
| **CASHIER** | Take orders, checkout, open shift. All restricted actions need manager-override PIN — captured in audit log |

Auto-lock returns to the user picker after the configured idle minutes (default 5). Switch user is always available without closing the open shift.

## Flashing fresh Android on the Sunmi W1401

Asked for, but a heads-up before you spend time: **this is generally not feasible** on Sunmi devices.

- Sunmi ships **SunmiOS** (a customised Android build with their launcher and vendor services). The bootloader is **locked** on retail units. Sunmi does not publish AOSP / GSI images for the W14xx series, and there is no community LineageOS port.
- **Realistic options**, in order:
  1. **Factory reset** in SunmiOS recovery (Power + Vol-Up combo, varies by build) → clean SunmiOS → set this app as the **kiosk launcher** (Sunmi has a built-in "Custom launcher" setting). This is what most deployments do.
  2. **Contact the Sunmi distributor** and request a clean firmware flash via Sunmi's tooling (`SUNMI Assistant` / OTA). They sometimes provide an "international" image without preinstalled regional apps.
  3. **Unlocking the bootloader** to flash custom AOSP/GSI is **not officially supported**, risks bricking the device, and voids warranty. Not recommended.

If you genuinely need vanilla AOSP / Play Services certification, a generic Android tablet + external printer is usually the better answer.

## Limitations / not in this version

- No cloud sync, no multi-device
- No inventory tracking with low-stock alerts
- No customer CRM / loyalty
- No promotion / discount rules engine
- No card-terminal integration
- No QR-menu for customers
- No e-receipt / e-mail
- Reports CSV export is wired in the data layer but no UI button yet

The data model and `PrinterService` / repository abstractions leave room for these — they're easy follow-ups.

## Verification checklist

When you have the W1401 + printers in front of you:

- [ ] Install APK; sign in as Owner; change the PIN
- [ ] Settings → enter Star printer IP → **Test print**
- [ ] Pair BT printer in Android settings → pick it in app → **Test print**
- [ ] Switch app language to Thai → confirm strings flip
- [ ] Toggle Restaurant mode → Tables tab appears → open T1 → add items → **Send to kitchen** → kitchen ticket prints
- [ ] Open a shift with ฿1000 float → make a few sales (cash + PromptPay) → **Print X-report** → close shift with counted cash → **Z-report** prints with over/short
- [ ] Add a CASHIER user → log in as them → try to delete a product → manager-override prompt appears → approve → action recorded in Audit log
