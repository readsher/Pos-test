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

---

## Install — step-by-step (no coding experience needed)

Read this part if you've never built an Android app. There are two halves:
**(A) make an APK file**, then **(B) install it on your Sunmi**. The APK is a
single `.apk` file — that's the installer for Android.

### (A) Get the APK file

You only need to do this once. Pick **one** option below.

#### Option 1 — Ask a developer to do it (easiest)

Send a developer this repo's link and ask them to:

1. Open the project in Android Studio.
2. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. Send you back the `app-debug.apk` file from `app/build/outputs/apk/debug/`.

That's it for you. Save the `.apk` somewhere on your computer.

#### Option 2 — Do it yourself with Android Studio

Allow about an hour the first time, mostly downloading.

1. **Install Android Studio**
   - Go to https://developer.android.com/studio and download for your OS (Windows / macOS / Linux).
   - Run the installer, accept defaults, click **Next** until done.
   - When it opens for the first time it will download "Android SDK" — say yes and let it finish (can take 20–40 minutes).

2. **Open the project**
   - Download this repo as a ZIP (top of GitHub page → green **Code** button → **Download ZIP**), then unzip it. *Or* clone with `git clone <this-repo-url>`.
   - In Android Studio: **File → Open** → pick the unzipped `Pos-test` folder → click **OK**.
   - Android Studio will say "Gradle sync" at the bottom. Wait for the progress bar to finish (5–15 minutes the first time, it's downloading more dependencies). If it asks to upgrade or trust the project, click **Trust Project** / **OK**.

3. **Build the APK**
   - Top menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
   - Wait for "BUILD SUCCESSFUL" (1–3 minutes).
   - A small popup at the bottom-right says **APK(s) generated successfully**. Click **locate** in that popup — it opens the folder containing `app-debug.apk`. Copy that file to a USB stick or somewhere easy to find.

If a build error comes up about "JDK" or "SDK location": **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**, set **Gradle JDK** to **17** (download it from that screen if missing). Re-try **Build APK(s)**.

### (B) Install the APK on the Sunmi W1401

You need the W1401 powered on, an **Owner / admin PIN** for it (the SunmiOS lock screen, not this app's PIN), and either a **USB-A → USB-A or USB-C cable** that fits the W1401, or a **USB stick**.

#### Easy way — USB stick

1. Copy `app-debug.apk` onto a USB stick.
2. Plug the stick into the W1401's USB port.
3. On the W1401, open the **Files** app (it's pre-installed on SunmiOS). Browse to the USB stick.
4. Tap `app-debug.apk`. Android will say **"For your security, your phone is not allowed to install unknown apps from this source."** — tap **Settings**, turn on **Allow from this source**, press back.
5. Tap the APK again → **Install** → **Open** when it finishes.

#### Alternative — over USB cable from a PC

This is the "adb" path. Skip if the USB-stick way worked.

1. On the W1401: **Settings → About → Build number** — tap it 7 times until it says "You are now a developer".
2. **Settings → System → Developer options** → turn on **USB debugging**.
3. On your PC, install **platform-tools** from https://developer.android.com/tools/releases/platform-tools (or it's bundled with Android Studio at `Sdk/platform-tools/`).
4. Connect the W1401 to the PC with the USB cable. The W1401 shows a popup **"Allow USB debugging?"** — tap **Allow**.
5. On the PC, open a terminal in the `platform-tools` folder and run:
   ```
   adb devices
   ```
   You should see the W1401 listed. If it says "unauthorized", re-tap **Allow** on the device.
6. Run:
   ```
   adb install path/to/app-debug.apk
   ```
   Should say **Success**.

### (C) First-run setup — what to do once it's installed

1. Open **PosTest** from the W1401's app drawer.
2. The login screen shows one user: **Owner**. Tap it.
3. Enter PIN **`0000`** and ✓.
4. **CHANGE THE PIN NOW.** Top-right gear icon → scroll to bottom → **Audit log / Users** isn't visible until you go to **Users** screen → tap **Owner** row → **Reset PIN** → enter a new 4–6 digit PIN.
5. **Settings → Shop name / Address / Tax % / PromptPay ID** — fill in your shop's info.
6. **Settings → Mode** — choose **Retail** or **Restaurant**.
7. **Settings → Language** — choose **English** or **ไทย**.

### (D) Connect the receipt printer

#### Star TSP100III over Wi-Fi / LAN

1. **One-time printer setup** (needs a Windows PC):
   - Connect the TSP100III to the same router as the W1401 with an Ethernet cable.
   - Install Star **futurePRNT** on the PC (free from Star Micronics' website).
   - Open **Star Configuration Utility** → find the printer → **Emulation** → switch to **ESC/POS Mode** → **Apply** → reboot the printer (turn it off, wait 5s, turn it on).
   - In the same utility, give the printer a **fixed (static) IP address** so it doesn't change. Write it down — e.g. `192.168.1.50`.
2. **In the app**: gear icon → **Settings → Printers**.
3. In **Star LAN printer IP**, type the IP from step 1 (e.g. `192.168.1.50`).
4. Set **Receipt printer** to **Star LAN**.
5. Tap **Test receipt printer**. The Star should print a small test slip. If nothing happens, double-check the IP, that the printer's green light is on, and that the W1401 is on the same Wi-Fi.

#### Bluetooth thermal printer

1. On the W1401: **Android Settings → Bluetooth** → make sure it's on.
2. Turn the printer on, hold its pairing button until its LED blinks (varies by model, see the printer's manual).
3. On the W1401, in the Bluetooth list tap the printer's name → **Pair**. If it asks for a PIN, common defaults are `0000` or `1234`.
4. Open **PosTest → Settings → Printers**. Your paired printer appears under "Bluetooth printer". Tap its radio button.
5. Set **Receipt printer** (or **Kitchen printer**) to **Bluetooth**.
6. Tap **Test receipt printer**. The printer should print a test slip.

If "No paired Bluetooth devices found" shows in the app even though it's paired in Android Settings, the app might not have permission. Go to **Android Settings → Apps → PosTest → Permissions → Nearby devices → Allow**, then re-open the app.

### (E) Open a shift and make your first sale

1. Top-right of the POS screen → tap **Open shift**.
2. Type the cash you have in the drawer to start (e.g. `1000` for ฿1000) → **Open shift**.
3. Back on the POS screen — tap a few products on the left, watch the cart fill on the right.
4. Tap **Checkout** → **Cash** → type the amount the customer gave you → **Pay & print**.
5. The receipt should print on your configured printer.

### (F) End-of-day — close the shift

1. Top-right → **Shift open** chip → tap it.
2. Count the cash in the drawer. Type that number into **Counted cash**.
3. **Close shift & print Z-report**. The Z-report shows your total sales by payment method and how much cash you should have vs how much you counted.

---

## Build (technical)

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
