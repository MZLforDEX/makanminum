# MakanMinum — Aplikasi Pengingat Makan & Pencatat Air Harian (Android)

Aplikasi Android ringan, sepenuhnya **offline**, dan **tanpa akun** yang membantu pengguna menjaga pola makan teratur serta memenuhi kebutuhan minum air harian lewat pengingat tepat waktu dan pencatatan satu ketukan.

---

## Paket Instalasi (.APK)

- **Lokasi File APK di Repositori**: [`releases/MakanMinum-v1.0.apk`](releases/MakanMinum-v1.0.apk)
- **Ukuran & Kompatibilitas**: Ringan, mendukung Android 7.0+ / Android 8.0+ hingga versi Android terbaru.

---

## Fitur Utama (Sesuai PRD v1.0)

1. **Pengingat Makan Tepat Waktu (`AlarmManager` + `NotificationManager`)**:
   - Menyediakan 3 jadwal makan bawaan (**Sarapan 07:00**, **Makan Siang 12:00**, **Makan Malam 19:00**) yang dapat diubah, dinonaktifkan, atau dihapus.
   - Tambah jadwal makan atau camilan kustom beserta pilihan hari aktif (**Senin – Minggu**).
   - Notifikasi prioritas tinggi dilengkapi tombol aksi langsung **"Sudah makan"** dan **"Tunda 10 menit"**.
2. **Pencatatan & Target Minum Harian**:
   - Kalkulasi target minum otomatis berdasarkan berat badan (`~33 ml × kg` dalam rentang rekomendasi `30–35 ml × kg`) atau pengaturan manual.
   - Pencatatan 1 ketukan (`+100 ml`, `+250 ml`, `+500 ml`), input jumlah kustom, serta fitur **Batal** untuk menghapus catatan terakhir.
   - Indikator progres lingkaran (radial gauge) dan reset otomatis setiap pergantian hari (`00:00` waktu lokal).
3. **Pengingat Minum Berkala**:
   - Hanya aktif di antara **Jam Bangun** dan **Jam Tidur** yang diatur pengguna dengan interval pilihan (`30 menit` hingga `4 jam`).
   - Notifikasi dilengkapi aksi langsung **"Minum 250 ml"** (mencatat tanpa membuka aplikasi) dan **"Tunda 10 menit"**.
   - Otomatis menghentikan pengingat untuk hari tersebut setelah target harian tercapai (dapat diatur di Pengaturan).
4. **Riwayat 7 Hari & Pengaturan Lengkap**:
   - Grafik batang 7 hari terakhir dengan garis target harian dan daftar rincian waktu minum per hari.
   - Dukungan mode tema **Terang**, **Gelap**, dan **Ikuti Sistem**.
   - Tetap aktif setelah perangkat di-*restart* (`BOOT_COMPLETED`) atau berganti zona waktu (`TIME_SET` / `TIMEZONE_CHANGED`).

---

## Teknologi & Arsitektur

- **Bahasa**: Kotlin
- **Antarmuka (UI)**: Jetpack Compose + Material Design 3 (Tema Biru Segar & Oranye Hangat, Font Lokal *Plus Jakarta Sans*)
- **Arsitektur**: MVVM (`ViewModel` + `StateFlow` + `Repository`)
- **Penyimpanan Lokal**: Room Database (`MealReminderEntity`, `WaterLogEntity`) & Jetpack Preferences DataStore (`UserSettings`)
- **Sistem Pengingat**: `AlarmManager` (`setExactAndAllowWhileIdle` dengan *fallback* aman) + `BroadcastReceiver` + `NotificationManager`
