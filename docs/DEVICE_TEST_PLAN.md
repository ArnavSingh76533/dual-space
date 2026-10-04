# Device acceptance tests

Record Android version/ROM, CPU ABI, Dual Space commit and Google package
versions. Test first with a nonessential account.

1. Install the APK matching your apps' architecture. Open Dual Space and
   create two spaces with +. Restart the host and verify both spaces persist.
2. Clone the same browser/messenger into both spaces. Sign in separately.
   Verify cookies/accounts/files differ from each other and the original app.
3. Import a standalone APK through the file picker. Repeat with an APKS
   bundle and an installed app containing splitSourceDirs.
4. Open Google Play services in each space. Verify all three components are
   installed; open Play Store and test Google login independently. Exercise
   an app requiring Google APIs, notifications, and an OAuth login flow.
   Record actual errors instead of treating installed packages as success.
   If setup fails, copy the report from its error dialog. Restart Engine and
   retry; verify completed components stay installed and setup resumes. Check
   both spaces: installation on the phone alone must not mark either complete.
5. Use an identifier test app in both spaces. Record Android ID, telephony
   device ID, Build serial, Bluetooth address and Wi-Fi address. Randomize
   Space 1; verify its apps stop and see new values after relaunch. Verify
   Space 2 is unchanged. Restore defaults and check the original values.
6. Rename apps/spaces, save a note, hide/unhide an app, search by package
   and name, create a shortcut, and scan a package/Play Store QR code.
7. Clear one app's data. Verify only its selected-space login resets.
   Uninstall all apps in one space; verify the original phone app and other
   spaces are unchanged. Delete the now-empty space.
8. Restart Engine with clones running. Verify the host returns and installed
   app data persists. Test background behavior with battery optimization on
   and off, granting only permissions needed by the apps under test.
