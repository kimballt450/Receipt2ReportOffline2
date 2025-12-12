# Receipt2Report Offline — GitHub Cloud Build (No local installs)

This project builds an Android APK using GitHub Actions (works on locked-down work laptops).

## Upload to GitHub (important)
Upload the *contents* of this folder to your repo root so you see **app/** at the top level:
- app/
- .github/
- build.gradle.kts
- settings.gradle.kts
- gradle.properties

## Build APK
1. Go to Actions
2. Click Build Android APK
3. Click Run workflow
4. Download artifact: Receipt2ReportOffline-APK
5. Inside is: app-debug.apk

## Install on Android
Copy app-debug.apk to your phone, open it, and allow Install unknown apps.
After install, the app works offline.
