# Device acceptance tests

## Native work profile (1.1.0)

Use a test profile with the default debug-signed APK. Configure stable signing
as described in README.md before keeping profile data across future builds.

1. Open ⋮ → Android work profile. Cancel system setup and verify no profile is
   created. Try again and approve Android's consent screens. Record ROM/API.
2. Open briefcase-badged Nimbus in the launcher's Work tab. Verify the native
   work-app screen appears and the virtual engine does not start there.
3. Enable Google components and copy the native report. Open work Play Store,
   sign in with a test account, and install a browser or messenger. Confirm
   its data is separate from the personal copy and numbered virtual spaces.
4. Install Swiggy from the work Play Store and record whether it accepts this
   profile. A successful APK build is not evidence of Swiggy compatibility.
5. Pause/resume the profile in Android and test Open Nimbus in work profile
   from the personal app. Test the manual Work-tab fallback when paused.
6. Test a device with an existing work profile: creating another must be
   unavailable; the app must not delete or take ownership of that profile.
7. With a stable key configured, build/install a subsequent version and verify
   work apps/accounts persist. Inspect the merged manifest's provisioning
   receiver/activity protection and confirm only managed-profile mode is used.

## Numbered virtual spaces

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
   On Android 16 / Play services 26.34.36, retry the reported Cronet Dynamite
   manifest mismatch with 1.0.2. Verify the installed virtual package is
   `com.google.android.gms`, not `com.google.android.gms.dynamite_cronetdynamite`.
   Copy a new report if the explicit base-manifest fallback fails.
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
