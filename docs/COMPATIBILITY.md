# Compatibility and verification

Dual Space is an actual BlackBox-based app container, not a collection of
shortcuts to the personal-profile apps. Each numbered space maps to one
virtual user ID. App data and virtual Google accounts belong to that user.
It is userspace virtualization, not the Android Enterprise security boundary.

## Google Play services

The space menu's **Google Play services** screen checks Services Framework,
Play services and Play Store separately. Setup imports them from the phone
in dependency order, includes Google Account Manager when present, checks
every installation, and rolls back only components added by a failed setup.
There are no Google APK download links or bundled Google binaries.

Automatic setup is enabled by default when cloning/importing apps. If it
fails, apps can still be installed and the actual error is shown. You can
import your own compatible APKs/APKS and use the component status screen.
"Installed" describes installation, not verified Google login or API behavior.

The engine contains account, service-binding, package, job, activity and
Google compatibility hooks. That does **not** guarantee all Google APIs work.
Hardware-backed Play Integrity, DRM and anti-virtualization checks are not
emulated. Google sign-in, FCM, Maps, Play Store downloads and individual apps
require physical-device testing with the installed Google package versions.
Unused upstream authentication proxies that fabricated accounts/tokens have
been removed; virtual account management uses the actual account service.

## Android and architecture

Minimum Android version is 7.0 (API 24). The project compiles against SDK 35,
but targets API 28 because the inherited virtualization engine relies on
legacy framework/package-parser behavior. This is a sideload APK build;
modern Android may show an older-target warning. It is not Play Store ready.
Changing targetSdk alone does not modernize the engine.

The upstream fork includes Android 14–16 compatibility patches, but support
is app/ROM-specific. No claim of universal Android 16 support is made.
Install **DualSpace-arm64.apk** for 64-bit apps on modern ARM phones.
Use **DualSpace-arm32.apk** only for 32-bit apps on phones with 32-bit runtime
support. Many recent phones cannot run 32-bit apps. Installing the other APK
replaces the same host app, so stop virtual apps before switching.
Split APKs and APKS bundles are handled by the inherited engine; complex
dynamic feature bundles still need app-specific validation.

## Device identity

Each space starts with a unique persisted default identity. Randomization
changes Device ID, Android ID, Bluetooth MAC, Wi-Fi MAC and serial for that
space; restoration returns to its original stored values. The single
server-process content provider prevents stale cross-process preference
reads. All apps in the selected space are stopped before changing values.

Android ID uses the settings-provider hooks. Device ID uses telephony hooks.
Serial uses Build/native property and device-identifier hooks. Wi-Fi MAC
uses the Wi-Fi API hook. Bluetooth MAC uses the Bluetooth manager binder
hook where the platform exposes that method. Native interface enumeration,
protected APIs and hardware attestation may return different values. The
physical phone's identifiers are never modified.

## Validation

GitHub Actions runs APK compilation, identity-format/collision/Luhn tests
and Android lint. There is no attached physical Android device in the build
environment. See DEVICE_TEST_PLAN.md for runtime acceptance tests. Build
success must not be treated as proof that every cloned app or Google login
works. Logs remain local; the upstream external uploader has been removed.
