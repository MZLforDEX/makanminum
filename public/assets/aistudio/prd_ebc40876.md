# PRD: Aplikasi Pengingat Makan & Minum (Android)

| | |
|---|---|
| **Nama sementara** | MakanMinum (working title) |
| **Platform** | Android (min SDK 26 / Android 8.0, target SDK terbaru) |
| **Versi dokumen** | 1.0 |
| **Status** | Draft |

---

## 1. Ringkasan

Aplikasi Android ringan, offline, dan tanpa akun yang membantu pengguna menjaga pola makan teratur dan memenuhi kebutuhan minum harian lewat pengingat tepat waktu serta pencatatan asupan air yang cepat.

## 2. Latar Belakang & Masalah

- Banyak orang melewatkan jam makan atau lupa minum karena kesibukan.
- Aplikasi sejenis umumnya berat, penuh iklan, atau memaksa membuat akun.
- Pengguna butuh alat sederhana: diingatkan, mencatat dengan satu ketukan, lalu melihat progres.

## 3. Tujuan & Non-Tujuan

### Tujuan
1. Mengingatkan pengguna pada jam makan yang dipilih sendiri.
2. Membantu pengguna mencapai target minum harian.
3. Pencatatan minum selesai dalam 1-2 ketukan.
4. Aplikasi tetap ringan: APK ≤ 6 MB, tanpa koneksi internet.

### Non-Tujuan (di luar versi 1.0)
- Hitung kalori, nutrisi, atau rekomendasi menu.
- Akun, login, sinkronisasi cloud.
- Iklan dan pembelian dalam aplikasi.
- Integrasi wearable / Google Fit.

## 4. Target Pengguna

| Persona | Kebutuhan |
|---|---|
| Pelajar / mahasiswa | Sering lupa makan dan minum saat belajar |
| Pekerja kantoran | Duduk lama, butuh pengingat minum berkala |
| Siapa pun yang ingin pola hidup lebih teratur | Alat sederhana tanpa fitur berlebihan |

## 5. User Stories

| ID | Sebagai pengguna, saya ingin... | Agar... |
|---|---|---|
| US-1 | mengatur jam sarapan, makan siang, makan malam, dan camilan | diingatkan tepat waktu |
| US-2 | menentukan hari aktif untuk tiap pengingat makan | pengingat sesuai rutinitas saya |
| US-3 | menetapkan target minum harian (ml) | punya tujuan jelas |
| US-4 | menghitung target otomatis dari berat badan | tidak perlu menebak |
| US-5 | mencatat minum dengan tombol cepat (+100/+250/+500 ml) | pencatatan tidak merepotkan |
| US-6 | dapat pengingat minum berkala di antara jam bangun dan tidur | tidak lupa minum |
| US-7 | mencatat dari notifikasi tanpa membuka aplikasi | lebih cepat |
| US-8 | menunda pengingat 10 menit | saat sedang sibuk |
| US-9 | melihat progres hari ini dan riwayat 7 hari | memantau kebiasaan |
| US-10 | pengingat tetap aktif setelah HP restart | tidak perlu atur ulang |

## 6. Kebutuhan Fungsional

### 6.1 Pengingat Makan
- **FR-1.1** Pengguna dapat menambah, mengubah, menghapus, dan mengaktifkan/menonaktifkan pengingat makan.
- **FR-1.2** Setiap pengingat memiliki: nama, jam, menit, hari aktif (Sen-Min).
- **FR-1.3** Aplikasi menyediakan 3 pengingat bawaan: Sarapan 07:00, Makan Siang 12:00, Makan Malam 19:00 (dapat diubah/dihapus).
- **FR-1.4** Notifikasi muncul tepat waktu, dengan aksi "Sudah makan" dan "Tunda 10 menit".

### 6.2 Pencatatan & Target Minum
- **FR-2.1** Pengguna dapat mengisi berat badan; target minum default dihitung sekitar 30-35 ml × kg dan dapat diubah manual.
- **FR-2.2** Tombol cepat tambah +100, +250, +500 ml dan input jumlah kustom.
- **FR-2.3** Pengguna dapat menghapus/membatalkan catatan terakhir.
- **FR-2.4** Progres harian ditampilkan sebagai lingkaran/bar (ml tercatat dari target, persen).
- **FR-2.5** Progres reset otomatis saat pergantian hari (00:00 waktu lokal).

### 6.3 Pengingat Minum
- **FR-3.1** Pengguna mengatur jam bangun, jam tidur, dan interval (30 menit - 4 jam).
- **FR-3.2** Pengingat hanya muncul di antara jam bangun dan jam tidur.
- **FR-3.3** Notifikasi memiliki aksi "Minum 250 ml" dan "Tunda 10 menit".
- **FR-3.4** Pengingat dihentikan untuk hari itu jika target sudah tercapai (dapat dimatikan di pengaturan).

### 6.4 Riwayat
- **FR-4.1** Grafik batang total minum 7 hari terakhir dengan garis target.
- **FR-4.2** Daftar catatan minum per hari (waktu dan jumlah).

### 6.5 Pengaturan
- **FR-5.1** Berat badan, target ml, jam bangun/tidur, interval minum.
- **FR-5.2** Mode tema: terang, gelap, ikuti sistem.
- **FR-5.3** Pintasan ke pengaturan izin notifikasi, alarm, dan penghemat baterai.

## 7. Kebutuhan Non-Fungsional

| Kategori | Persyaratan |
|---|---|
| Ukuran | APK/AAB ≤ 6 MB setelah R8/shrink |
| Performa | Cold start < 2 detik pada perangkat menengah |
| Baterai | Tanpa foreground service permanen; gunakan alarm terjadwal |
| Offline | Seluruh fitur berjalan tanpa internet |
| Privasi | Data hanya di perangkat; tidak ada pelacakan/analitik pihak ketiga |
| Keandalan | Pengingat tetap aktif setelah restart, ganti zona waktu, dan pembaruan aplikasi |
| Aksesibilitas | Ukuran sentuh ≥ 48dp, kontras cukup, dukungan TalkBack dasar |
| Bahasa | Indonesia (utama), Inggris (opsional) |

## 8. Alur & Layar

1. **Onboarding singkat (1-2 layar):** isi berat badan, jam bangun/tidur, minta izin notifikasi dan alarm.
2. **Beranda:** progres minum, tombol tambah cepat, kartu "Makan berikutnya".
3. **Jadwal Makan:** daftar pengingat + tombol tambah.
4. **Riwayat:** grafik 7 hari dan daftar catatan.
5. **Pengaturan.**

Navigasi: bottom navigation 4 tab (Beranda, Makan, Riwayat, Pengaturan).

## 9. Model Data

**MealReminder**
| Field | Tipe |
|---|---|
| id | Long (PK) |
| nama | String |
| jam | Int (0-23) |
| menit | Int (0-59) |
| hariAktif | Bitmask / String |
| aktif | Boolean |

**WaterLog**
| Field | Tipe |
|---|---|
| id | Long (PK) |
| tanggal | String (yyyy-MM-dd) |
| waktu | Long (epoch ms) |
| jumlahMl | Int |

**Settings (DataStore):** `beratBadan`, `targetMl`, `jamBangun`, `jamTidur`, `intervalMenit`, `tema`, `stopSetelahTarget`.

## 10. Arsitektur & Teknologi

- **Bahasa:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arsitektur:** MVVM, satu Activity
- **Database:** Room
- **Pengaturan:** DataStore
- **Pengingat:** AlarmManager (exact alarm) + BroadcastReceiver
- **Pasca-boot:** `BOOT_COMPLETED` receiver untuk menjadwal ulang alarm
- **Build:** R8/ProGuard, resource shrinking, tanpa library besar

## 11. Izin yang Dibutuhkan

| Izin | Keterangan |
|---|---|
| `POST_NOTIFICATIONS` | Wajib di Android 13+ |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Alarm tepat waktu di Android 12+ (cek status sebelum dipakai) |
| `RECEIVE_BOOT_COMPLETED` | Menjadwal ulang setelah restart |

## 12. Kriteria Penerimaan (Ringkas)

- [ ] Notifikasi makan muncul ±1 menit dari jam yang ditetapkan, termasuk setelah aplikasi ditutup.
- [ ] Alarm tetap berfungsi setelah HP restart.
- [ ] Tombol "Minum 250 ml" di notifikasi menambah catatan dan memperbarui progres.
- [ ] Pengingat minum tidak muncul di luar jam bangun-tidur.
- [ ] Progres reset di pergantian hari dan riwayat tersimpan benar.
- [ ] Berjalan di Android 8 hingga versi terbaru tanpa crash.
- [ ] Ukuran rilis ≤ 6 MB.

## 13. Metrik Keberhasilan

| Metrik | Target awal |
|---|---|
| Rasio notifikasi terkirim tepat waktu | ≥ 95% (uji internal) |
| Crash-free sessions | ≥ 99% |
| Waktu mencatat satu kali minum | ≤ 3 detik |
| Retensi pengguna hari ke-7 | ≥ 30% (jika dirilis publik) |

> Catatan: metrik retensi hanya bisa diukur bila memakai analitik. Karena versi 1.0 sengaja tanpa pelacakan, gunakan data statistik dasar Play Console.

## 14. Rencana Rilis

| Fase | Cakupan | Estimasi |
|---|---|---|
| Fase 1 | Setup proyek, desain, skema database | 2-3 hari |
| Fase 2 | Fitur minum (beranda, catat, target) | 4-5 hari |
| Fase 3 | Pengingat makan & minum, notifikasi, boot receiver | 5-6 hari |
| Fase 4 | Riwayat dan pengaturan | 3-4 hari |
| Fase 5 | Pengujian lintas versi dan merek HP | 3-4 hari |
| Fase 6 | Polesan, ikon, optimasi ukuran, rilis | 2-3 hari |

**Total:** ± 3-4 minggu (paruh waktu).

## 15. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Penghemat baterai vendor (Xiaomi, Oppo, Vivo, Samsung) mematikan alarm | Pengingat tidak muncul | Panduan izin latar belakang di onboarding dan pengaturan |
| Izin exact alarm ditolak/dicabut | Pengingat terlambat | Deteksi status izin, tampilkan peringatan, fallback ke alarm tidak tepat |
| Perubahan zona waktu/jam sistem | Jadwal bergeser | Dengarkan `TIME_SET` / `TIMEZONE_CHANGED` dan jadwalkan ulang |
| Target minum dianggap saran medis | Salah paham | Tambahkan catatan bahwa angka hanya perkiraan umum |

## 16. Pertanyaan Terbuka

1. Nama final dan identitas visual aplikasi?
2. Apakah versi 1.0 dirilis ke Play Store atau cukup untuk penggunaan pribadi/tugas?
3. Perlukah dukungan bahasa Inggris sejak awal?
4. Apakah perlu pengingat tambahan (misalnya vitamin/obat) di versi berikutnya?

## 17. Pengembangan Lanjutan (Backlog)

- Widget layar utama untuk catat minum 1 ketukan.
- Ekspor riwayat ke CSV.
- Pengingat adaptif berdasarkan progres.
- Pencatatan jenis minuman.
- Cadangan dan pemulihan data lokal.
