@echo off
REM Jalankan dari folder NotMeAI. Pastikan local.properties sudah berisi sdk.dir
if not exist gradlew.bat call gradle wrapper --gradle-version 8.7
if not exist notme.jks keytool -genkeypair -keystore notme.jks -alias notme -keyalg RSA -keysize 2048 -validity 10000 -storepass bubbleai123 -keypass bubbleai123 -dname "CN=NotMeAI"
call gradlew.bat assembleRelease
copy /Y app\build\outputs\apk\release\app-release.apk NotMeAI.apk
echo Selesai: NotMeAI.apk
