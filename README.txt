PERSIAPAN (sekali saja)
1. Install JDK 17 dan Gradle 8.7 (gradle.org), lalu Android command-line tools.
2. Di cmd:
   sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
3. Set ANDROID_HOME ke folder SDK, mis. C:\Users\NAMA\AppData\Local\Android\Sdk

BUILD (cmd, di folder NotMeAI)
   echo sdk.dir=C:\\Users\\NAMA\\AppData\\Local\\Android\\Sdk> local.properties
   gradle wrapper --gradle-version 8.7
   gradlew.bat assembleDebug

HASIL: app\build\outputs\apk\debug\app-debug.apk
INSTALL: adb install -r app\build\outputs\apk\debug\app-debug.apk

CARA CEPAT (APK siap share)
   1. Buat local.properties berisi sdk.dir=...
   2. Jalankan: build.bat
   3. Hasil: NotMeAI.apk di folder proyek (sudah ditandatangani)
   Ganti password keystore lewat env var KS_PASS bila perlu, dan jangan bagikan notme.jks.

TANPA INSTALL APA PUN (build di GitHub)
   Upload proyek ke repo GitHub -> tab Actions -> tunggu build selesai -> download artifact "NotMeAI-apk".
