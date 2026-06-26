# Simple-Chinese-Sentence-Finder

Simple Chinese Sentence Finder sekarang berisi:

- Program CLI Python lama di `src/main.py`.
- Project Android native offline di folder `app/`.
- Dataset Android di `app/src/main/assets/sentence.csv`.
- Rencana migrasi di `docs/android-app-plan.md`.

## Android App

Aplikasi Android dibuat dengan Java + XML layout agar bisa dibuka langsung di Android Studio tanpa dependency Kotlin tambahan.

Fitur Android v1:

- Pencarian contoh kalimat Mandarin dari dataset lokal.
- Mode Simplified dan Traditional.
- Transliteration pinyin memakai ICU bawaan Android.
- Hasil maksimal 10 kalimat.
- Tampilan Hanzi, pinyin, dan terjemahan Inggris.
- Bisa berjalan tanpa internet setelah APK terpasang.


## Cara Menjalankan di Android Studio

1. Buka folder repo ini di Android Studio.
2. Tunggu Gradle sync selesai.
3. Jalankan app ke emulator atau perangkat Android fisik.
4. Masukkan kosakata Hanzi, pilih mode aksara, lalu tekan `Cari`.

## Build APK

Dari Android Studio:

1. Buka menu `Build`.
2. Pilih `Build Bundle(s) / APK(s)`.
3. Pilih `Build APK(s)`.

Jika Gradle wrapper sudah dibuat oleh Android Studio atau Gradle sudah tersedia di terminal:

```powershell
.\gradlew.bat assembleDebug
```

atau:

```bash
./gradlew assembleDebug
```

Output APK debug ada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Catatan Toolchain

Project Android ini memakai:

- Android Gradle Plugin `8.5.2`
- Compile SDK `35`
- Minimum SDK `26`

Jika Android Studio meminta update versi Gradle atau SDK, ikuti saran Android Studio selama `minSdk` tetap `26` atau lebih tinggi. Minimum SDK 26 dibutuhkan karena aplikasi memakai ICU Android untuk konversi aksara dan pinyin.
