# Build and UAT Instructions

## What was hardened in this pass
- Added the Kotlin Compose compiler plugin required for Kotlin 2.x Compose builds.
- Verified project module structure and package/manifest consistency.
- Reviewed Room/KAPT/serialization dependency alignment.
- Retained local-only storage and no-backend architecture.

## Build locally in Android Studio
1. Extract this archive.
2. Open the root `CompetitivePhysique` project in Android Studio.
3. Allow Gradle sync to complete.
4. Select an Android device or emulator running Android 8.0+.
5. Run the `app` configuration.
6. If Android Studio reports a compiler error, copy the full error text and send it back for the next hardening iteration.

## Expected build artifact
After a successful debug build:
`app/build/outputs/apk/debug/app-debug.apk`

## Important honesty note
This environment does not have Gradle, an Android SDK, or an Android emulator installed, so a real APK compilation and installation cannot be executed here. The source has therefore been statically hardened, but physical build verification must happen in Android Studio or a CI environment.
