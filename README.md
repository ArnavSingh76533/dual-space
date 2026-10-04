# Nimbus

Android app container matching the multi-space layout and controls in the
provided reference video. Built from source with GitHub Actions.

## Download

Open [Actions](https://github.com/ArnavSingh76533/dual-space/actions), choose
the latest successful **Build Android APK** run, and download the
**Nimbus-APK** artifact. Extract the ZIP and install `Nimbus-arm64.apk`
on most modern phones. `Nimbus-arm32.apk` is for 32-bit apps on supported
phones. Both are debug-signed sideload builds, with SHA-256 checksums.

## Features

| Reference feature | Implementation |
| --- | --- |
| Numbered space cards and + button | Separate virtual users; persistent app data |
| Installed Apps / System Apps | Alphabetical icon grids, search, multiple selection |
| Install App | Clone installed packages; import APK and APKS with system picker |
| Note | Persistent note per space |
| Reset Device / Device identity | Randomize or restore five per-space identifiers |
| Uninstall all apps | Removes apps/data only from the selected space |
| Settings | Automatic Google setup, background engine, permissions/storage/battery |
| Show hidden apps | Long-press Hide/Unhide and global visibility control |
| Restart Engine | Stop clones and restart host/core processes |
| Membership & subscription / ad icon | All features free; no ads or billing |
| Share | Android share sheet and repository QR |
| Search / QR toolbar | Search all spaces; scan app package or Play Store link |
| Google Play services | Per-space setup/repair and component status |
| Android work profile (1.1.0) | Optional OS-managed second installation; work Play Store and app launcher |

App long-press also provides Rename, Force stop, Clear data, Uninstall and
Create shortcut. Space menus provide Rename and Delete space.

The Android package is `com.arnav.nimbus`. This is a separate installation
from the earlier `com.arnav.dualspace` build; its spaces do not migrate
automatically. Renaming does not conceal virtualization or provide Play
Integrity certification.

## Start

1. Install the appropriate APK, open Nimbus and tap +.
2. Select apps, then tap Clone. Google setup runs by default when the phone
   has Google Play services; failure details are shown.
3. Open a space's menu → Google Play services to check/repair its components
   and open its Play Store. Long-press an app for management controls.
4. Grant needed permissions in Settings. Some clones need Storage access
   and relaxed battery optimization for background activity.

Google API/login compatibility varies by app, Google version and ROM.
Installed components are not a guarantee of working Google sign-in or
Play Integrity. Android 14–16 needs device testing. Read
[compatibility details](docs/COMPATIBILITY.md) and the
[device test plan](docs/DEVICE_TEST_PLAN.md).

## Google setup troubleshooting (1.0.1)

The installer now returns the actual failure instead of swallowing an
exception and reporting success. Parser errors include their cause and the
installation stage. Google setup checks the selected space, never the phone's
installation state, and reconnects once when the package service dies.
Completed dependencies are kept so a retry can resume.

If setup fails, open the space menu → Google Play services → Set up / repair.
Use **Copy report** on the error dialog, or **More → Copy setup report** on the
status dialog. The report includes Android/engine architecture, Google package
versions, APK readability, space installation state and the last setup error.
It does not include account credentials, tokens or device identifiers and is
only copied locally. An installed status does not verify Google login or API
behavior; those still need testing on the phone.

Version **1.0.2** addresses the reported Android 16 parse mismatch where Play
services was parsed as `com.google.android.gms.dynamite_cronetdynamite`.
Installed-app requests now pass the requested package name to the server;
parsing validates it and retries the explicitly selected base manifest on a
mismatch. The same parser is used when the phone's app is updated. Package
names and signatures of Google APKs are not altered. The actual Android 16
resource parsing fallback and Google login still require a phone retry.

## Build locally

JDK 21, Android SDK 35 / build-tools 35.0.0, and NDK 29.0.13846066:

```sh
chmod +x gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The project intentionally targets API 28 for legacy virtualization APIs.
It is intended for sideloading, not publication to Google Play.

## Native work-profile mode (1.1.0, experimental)

Open **⋮ → Android work profile → Create work profile**. Android displays
its consent/setup screens and installs Nimbus as the owner of a new work
profile. Open the briefcase-badged Nimbus app in the launcher's Work tab,
enable Google components when needed, and install apps from that profile's
Play Store. Apps run normally under Android, with their original APKs and
signatures. Google services are enabled as actual system apps in the profile;
the virtual Google installer and identity hooks are not used there.

The numbered virtual spaces remain available in the personal-profile app.
Work-profile mode is one additional Android profile, subject to ROM/device
policy. It does not import existing virtual-space data. If Android refuses
profile creation, the app reports that and does not remove an existing
profile. Provisioning, Google sign-in and Swiggy acceptance still require
physical-device testing. No claim of reproducing Clone Master's internal
engine or making apps undetectable is made.

**Signing matters for a profile owner.** Without signing secrets, Actions
uses a temporary debug key; a later build may require removing the test work
profile, erasing its data. Use a test profile for these APKs. For persistent
use, configure these repository Actions secrets before creating the profile:

- `NIMBUS_KEYSTORE_BASE64`: your private JKS/PKCS12 keystore encoded as base64.
- `NIMBUS_STORE_PASSWORD`: keystore password.
- `NIMBUS_KEY_ALIAS`: signing-key alias.
- `NIMBUS_KEY_PASSWORD`: key password.

Keep the same key and alias for all updates. Never commit the keystore or
passwords. The workflow loads it privately from secrets and signs the debug
APK; the APK is still an experimental, debuggable sideload build.

Implementation uses Android's public [work-profile provisioning APIs](https://developer.android.com/work/dpc/build-dpc).
It does not request whole-device ownership, password-reset/wipe policies or
automatic permission grants. Android's system consent screens remain enabled.

## Credits

Apache-licensed BlackBox engine, based on
[Black00Z/Blacks-BlackBox](https://github.com/Black00Z/Blacks-BlackBox)
at commit `40282a7bf4500948cfd598fc67e6e63114b26dd9`.
Engine source is vendored for reproducible builds. See [NOTICE](NOTICE)
and [LICENSE](LICENSE). Google APKs are not redistributed. No analytics,
ad SDKs, subscriptions, or external log uploader are added.
