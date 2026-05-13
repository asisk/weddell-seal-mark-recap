# Testing — WeddellSealMarkRecap

This file lives under `app/src/test/resources/` so it appears with the **unit test** source set (`weddellseal.markrecap (test)`) in Android Studio’s **Android** project view. Instrumented tests are documented here too; they live under `app/src/androidTest/`.

How to run automated tests for the `:app` module.

## Prerequisites

- **JDK 17** (matches the project toolchain in `app/build.gradle.kts`).
- **Android SDK** (via Android Studio or `ANDROID_HOME`).
- **Emulator or physical device** (API 29+) for **instrumented** tests only.

From a terminal, Gradle must resolve a Java runtime. If you see *Unable to locate a Java Runtime*, set for example:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
```

Adjust the path if your JDK is installed elsewhere (e.g. Temurin under `~/Library/Java/JavaVirtualMachines/`).

## JVM unit tests (`src/test`)

These run on the JVM and do **not** require a device. Some use **Robolectric** for Android APIs (e.g. Room with `ApplicationProvider`).

```bash
cd AndroidStudioProjects/WeddellSealMarkRecap
./gradlew :app:testDebugUnitTest
```

Useful variants:

```bash
# Run a single test class (example)
./gradlew :app:testDebugUnitTest --tests "weddellseal.markrecap.domain.tagretag.data.SealTest"
```

Reports: `app/build/reports/tests/testDebugUnitTest/index.html`

## Instrumented tests (`src/androidTest`)

These install a test APK on a **running emulator or USB-connected device** and execute UI / integration tests (e.g. Compose smoke tests, Room on device).

1. Start an emulator or connect a device with USB debugging enabled.
2. Run:

```bash
cd AndroidStudioProjects/WeddellSealMarkRecap
./gradlew :app:connectedDebugAndroidTest
```

Reports: `app/build/reports/androidTests/connected/index.html` (path may vary slightly by AGP version).

## Android Studio

- **Unit tests:** Right-click `app/src/test/...` or a test class → **Run** / **Debug**.
- **Instrumented tests:** Right-click `app/src/androidTest/...` → choose a device → **Run**.

If you do not see **resources** under `(test)`, switch the project tree to **Project** view; the file path is always `app/src/test/resources/README_TESTING.md`.

## Where tests live

| Location | Role |
|----------|------|
| `app/src/test/java/...` | JVM unit tests (domain, ViewModels with fakes/MockK, Robolectric + Room, etc.) |
| `app/src/androidTest/java/...` | Instrumented tests (`AndroidJUnit4`, Compose UI tests, device-backed checks) |

## CI / headless

For CI, use the same Gradle tasks on a machine (or container) with the Android SDK; instrumented tests typically need an **emulator** (e.g. Gradle Managed Device, Firebase Test Lab, or a self-hosted AVD).
