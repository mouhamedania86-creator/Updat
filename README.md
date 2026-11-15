# MoviFlix 🎬

Netflix-style Android app built on the hitv.ltd API with full RSA+AES crypto protocol reverse-engineered from the original APK.

---

## Features
- **Hero banner** – auto-scrolling ViewPager2 with dot indicators
- **Content rows** – horizontal carousels per category, infinite scroll pagination
- **Search** – debounced live search with trending grid fallback
- **Detail screen** – poster, score, description, cast, season picker, episode list
- **Player** – ExoPlayer HLS streaming, quality switcher (LD/SD/HD/FHD), subtitle selector, brightness/volume swipe gestures, double-tap seek ±10s
- **Downloads** – offline HLS→TS segment download via WorkManager, Room DB tracking
- **Settings** – language, default quality, subtitle language, account info, logout
- **Auth** – email login/register via hitv API, guest mode
- **Multi-ABI** – arm64-v8a + armeabi-v7a + x86 + x86_64 (universal APK + per-ABI splits)

---

## Crypto Protocol (reverse-engineered)

Every API request is signed as follows:

```
1. Generate random 16-char alphanumeric AES key
2. RSA-PKCS1v1.5 encrypt the AES key with server public key → base64 → header "aeskey"
3. Sort all query params alphabetically, concat values + currentTimeMs → URL-encode
4. AES-ECB-PKCS5 encrypt the string with the AES key → base64 → MD5 → header "sign"
5. Encrypted responses: AES-ECB-PKCS5 decrypt the "data" field using same AES key
```

---

## Build

### Requirements
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Steps

```bash
git clone https://github.com/YOUR_USERNAME/MoviFlix.git
cd MoviFlix
./gradlew assembleDebug
```

APK outputs:
- `app/build/outputs/apk/debug/app-universal-debug.apk`  ← install on any device
- `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`  ← 64-bit (smaller)
- `app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk` ← 32-bit

### Install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

---

## Project Structure

```
app/src/main/java/com/moviflix/
├── MoviFlixApp.kt              Application class, DI wiring
├── api/
│   ├── ApiService.kt           All Retrofit endpoints
│   ├── HitvInterceptor.kt      Auto-sign every request
│   ├── HitvRepository.kt       Data layer, transparent decryption
│   └── NetworkModule.kt        OkHttp + Retrofit factory
├── crypto/
│   └── HitvCrypto.kt           RSA + AES engine (BouncyCastle)
├── models/
│   └── Models.kt               All Gson data classes + Room entity
├── service/
│   ├── DownloadDatabase.kt     Room DB + DAO
│   └── DownloadWorker.kt       WorkManager HLS downloader
├── ui/
│   ├── auth/LoginActivity.kt
│   ├── home/{MainActivity,HomeFragment}.kt
│   ├── detail/DetailFragment.kt
│   ├── search/SearchFragment.kt
│   ├── downloads/DownloadsFragment.kt
│   ├── player/PlayerActivity.kt
│   └── settings/SettingsFragment.kt
└── utils/SessionManager.kt     DataStore preferences
```

---

## Upload to GitHub

```bash
cd MoviFlix
git init
git add .
git commit -m "feat: initial MoviFlix release"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/MoviFlix.git
git push -u origin main
```
