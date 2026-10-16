# Do I Have

A small Android toolkit for inspecting this app's permissions and AppOps.

Release version **0.0.1**, debug version **0.0.1-debug**, Android 8.0+ (API 26+). Settings displays the build's versionName directly. The development build uses Android's debug signing key.

- **Permissions:** only shows permissions with a runtime request API or an implemented special-access settings action. Filter all/granted/not granted/unknown; confirmed grants have a checkmark on the right. Unreadable or ambiguous special-access states are shown as unknown. Rows show localized names, with bundled Chinese/English fallbacks when system labels are missing or contain only permission identifiers. Nothing requests access on launch.
- **App Ops:** inspect operation names and AOSP numeric IDs for this app's own UID, with the raw status on the right. Filter all five Android modes, unknown modes, or all modes using inline buttons that wrap to fit the screen. Tap an operation for its associated permission and details.
- **Settings:** follow the system language/theme or choose Simplified Chinese / English and light / dark mode. The interface uses neutral grays. Includes the project link and full MIT license.

State refreshes when the app resumes and after a permission result. A failed load can be tapped to retry. Inspecting state does not read contacts, capture audio/images, or invoke the underlying operations.

## Build

Use JDK 17+ (JDK 21 recommended), Android SDK Platform 37.0 and Build Tools 36.0.0. Set `ANDROID_HOME` or create an untracked `local.properties` with `sdk.dir`.

Application and test sources are Kotlin. The project uses AGP 9.1.1's built-in Kotlin support, with JVM target 17; no separate Kotlin Android plugin is required.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Gradle 9.3.1 and AGP 9.1.1 are pinned in the project. Release signing is not configured.

Build an unsigned release APK for signing separately:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease :app:lintRelease
```

The release application ID is `com.napreth.doihave`. The unsigned APK is written to `app/build/outputs/apk/release/app-release-unsigned.apk`; release builds disable debugging and do not use the debug signing key. Debug builds use `com.napreth.doihave.debug` so that testing can coexist with a separately signed release installation.

Install and run on a running AVD (always specify its serial if a phone is also connected):

```powershell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.napreth.doihave.debug/com.napreth.doihave.MainActivity
```

## Tests

JVM tests cover permission request routing and argument groups at SDK boundaries, removal of non-requestable permissions, readable English/Chinese permission names, exact AppOps mode filters, and validation of the shipped AOSP catalog (including camera #26 and unused slot #96). Exact-alarm regressions verify that capability availability cannot manufacture a permission grant, default/unreadable AppOps remain unknown, and authorization filters do not misclassify unknown states. Run them with `:app:testDebugUnitTest`; the HTML report is in `app/build/reports/tests/testDebugUnitTest/`.

Instrumented tests check permission/operation row reuse (including unknown states), combined search and state filtering, RTL wrapping, and all seven Ops state buttons on a narrow screen with large text. They also verify direct settings launches without visibility queries, fallback after missing/blocked activities, retention of supported special-access rows, manifest isolation, and separate exact-alarm grant/capability readings. Run against a running AVD:

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
.\gradlew.bat :app:connectedDebugAndroidTest
```

The report is in `app/build/reports/androidTests/connected/debug/`. AndroidX Test is used only in the test APK; it is not a production dependency.

Run the end-to-end UI smoke test on an AVD (this resets only Do I Have's test data):

```powershell
.\scripts\smoke-test.ps1 -Serial emulator-5554
```

The test checks launch behavior, removal of non-requestable entries and extra UI copy, navigation alignment, camera authorization and its checkmark, Ops number 26 and its trailing status, inline filter behavior, build version, language/theme changes, the full MIT license, and preference persistence. Screenshots and results are saved under the untracked `.local/smoke/` directory.

After installing the debug APK, reproduce the capability/grant distinction on an Android 12+ AVD:

```powershell
.\scripts\exact-alarm-regression-test.ps1 -Serial emulator-5554
```

This temporarily denies the debug app's exact-alarm App Op while giving that same debug app a battery exemption. It checks that the permission row remains ungranted while operation details separately show the available capability. Cleanup independently attempts every restore action and reports all restore failures while preserving any original test failure. The signed release app is not modified.

Run the cleanup fault-injection checks without an AVD:

```powershell
.\scripts\tests\test-cleanup.Tests.ps1
```

## Permission and operation catalog

The manifest declares 351 public permission constants from SDK 37.0 for diagnostics. `USE_EXACT_ALARM` is deliberately excluded because its automatic grant bypasses the user-controlled `SCHEDULE_EXACT_ALARM` flow. The UI includes only dangerous runtime permissions and implemented special-access actions supported by the device's Android version. Install-time, signature/privileged and obsolete permissions have no rows. Special-access rows are retained without querying activity visibility; tapping attempts the action, then falls back to app settings if Android rejects it. Android may suppress further dialogs after repeated denials, and restricted runtime permissions can still depend on installer or role policy. Background location and background sensor access use their separate settings flows.

The data model separately records the manifest permission grant, special-access authorization, capability availability and raw AppOps mode. For exact alarms, only explicit allowed/ignored/errored AppOps modes establish the special-access state; default or unreadable modes remain unknown. `canScheduleExactAlarms()` is recorded only as a capability check, since exemptions can make it true independently of the switch. Operation details display the manifest permission grant separately from special access and capability. No alarms are scheduled by the checks.

AppOps numbers come from AOSP's API 37.0 `AppOpInfo` table, including its unused slot. Unsupported operations are omitted on older Android versions. OEM-specific operation IDs are not inferred. Mode checks use public APIs and this app's own UID, without Root, Shizuku, shell privileges or hidden-API reflection. Status filters match the raw AppOps mode exactly; the associated permission's grant is shown separately in operation details. Modes are diagnostic information, not a guarantee that the underlying API is usable.

Regenerate the checked-in manifest and operation mapping with SDK sources installed:

```powershell
.\scripts\generate-platform-data.ps1
```

This broad permission declaration is intentional for a development utility. Installing a clean build also grants normal install-time permissions automatically, as Android specifies.

## License

[MIT](LICENSE), Copyright 2026 Napreth. The AOSP-derived operation catalog is attributed in [NOTICE](NOTICE); its Apache 2.0 license is included in the APK assets.

Project: https://github.com/Napreth/Do-I-Have (the repository may not exist yet).
