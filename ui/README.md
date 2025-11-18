# Weather UI

Desktop application for viewing weather, built with Compose Multiplatform.

**For fun and tests** 

## Description

Simple desktop application that allows you to:
- Request weather by city name
- Configure API URL and key
- Display temperature on a visual thermometer

## Technologies

- Kotlin Multiplatform
- Compose Multiplatform (Desktop)
- Material 3
- Weather SDK (main project module)

## Run

```bash
./gradlew :ui:run
```

## Build

```bash
# macOS DMG
./gradlew :ui:packageReleaseDmg

# Java JAR
./gradlew :ui:packageReleaseUberJarForCurrentOS
```

