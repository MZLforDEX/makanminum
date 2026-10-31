# MakanMinum — Aplikasi Pengingat Makan & Pencatat Air Harian

Aplikasi Android ringan, sepenuhnya offline, dan tanpa akun yang membantu pengguna menjaga jadwal makan teratur serta mencapai target hidrasi harian melalui pengingat tepat waktu dan pencatatan satu ketukan.

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan produk berikut telah dikonfirmasi berdasarkan preferensi Anda serta spesifikasi PRD versi 1.0:

- **Identitas & Visual (Terkonfirmasi)**: Menggunakan nama **MakanMinum** dengan identitas visual bertema **Biru Segar (Hidrasi/Air)** dan **Oranye Hangat (Nutrisi/Makan)** yang mendukung mode Terang, Gelap, dan Ikuti Sistem.
- **Bahasa Antarmuka (Terkonfirmasi)**: Seluruh teks, label, pesan pengingat, dan panduan izin disajikan dalam **Bahasa Indonesia** yang ringkas, ramah, dan mudah dipahami.
- **Cakupan Fitur Versi 1.0 (Terkonfirmasi)**: Berfokus murni pada pengingat jadwal makan dan pencatatan air putih sesuai PRD tanpa fitur tambahan di luar lingkup agar aplikasi tetap cepat, fokus, dan memiliki ukuran APK yang sangat ringan (≤ 6 MB).

---

## 1. Overview & Core Concept

- **What It Does**:
  - Menyediakan dasbor progres minum harian interaktif dengan pencatatan cepat (`+100 ml`, `+250 ml`, `+500 ml`, serta input jumlah kustom) dan fitur batalkan/hapus catatan terakhir.
  - Mengalkulasi target minum harian otomatis berdasarkan berat badan (`33 ml × kg` dalam rentang rekomendasi 30–35 ml/kg) dengan opsi penyesuaian manual kapan saja.
  - Mengelola jadwal pengingat makan (bawaan: Sarapan `07:00`, Makan Siang `12:00`, Makan Malam `19:00`, ditambah jadwal kustom atau camilan) lengkap dengan pemilihan hari aktif (Senin–Minggu).
  - Menjalankan pengingat minum berkala secara otomatis hanya di antara jam bangun dan jam tidur pengguna, serta otomatis berhenti untuk hari tersebut ketika target minum harian sudah tercapai (opsi ini dapat diatur).
  - Menampilkan notifikasi interaktif dengan tombol aksi langsung (**"Sudah makan"**, **"Minum 250 ml"**, dan **"Tunda 10 menit"**) tanpa perlu membuka aplikasi.
  - Menyajikan riwayat hidrasi 7 hari terakhir melalui grafik batang berbanding garis target harian serta rincian riwayat waktu minum per hari.
- **Target Audience / Persona**:
  - Pelajar, mahasiswa, pekerja kantoran, maupun siapa saja yang sering melewatkan waktu makan atau lupa minum air karena kesibukan dan menginginkan alat bantu yang instan, bebas iklan, dan menjaga privasi data sepenuhnya di perangkat.
- **Key Value**:
  - Pencatatan selesai dalam 1–2 ketukan (di dalam aplikasi maupun langsung dari notifikasi), bekerja 100% tanpa internet, hemat baterai dengan penjadwalan alarm sistem, dan tetap andal meski perangkat di-restart atau mengalami perubahan zona waktu.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Onboarding Cepat (2 Langkah pada Penggunaan Pertama)**:
     - *Langkah 1 (Profil & Target)*: Pengguna memasukkan berat badan (otomatis menghitung rekomendasi target ml harian, yang juga bisa diubah manual) serta mengatur jam bangun, jam tidur, dan interval pengingat minum.
     - *Langkah 2 (Izin & Keandalan Pengingat)*: Permintaan izin notifikasi (`POST_NOTIFICATIONS`), pengecekan izin alarm tepat waktu (`SCHEDULE_EXACT_ALARM`), serta panduan singkat penghemat baterai dengan tombol selesai untuk masuk ke Beranda.
  2. **Beranda (Tab 1)**:
     - Menampilkan kartu indikator lingkaran (radial progress gauge) besar yang memperlihatkan jumlah ml air yang diminum hari ini, persentase pencapaian, dan sisa ml menuju target.
     - Baris tombol tambah cepat (`+100 ml`, `+250 ml`, `+500 ml`, `Kustom`) serta tombol **"Batalkan catatan terakhir"** apabila sudah ada catatan hari ini.
     - Kartu **"Makan Berikutnya"** dengan aksen oranye hangat yang menampilkan nama jadwal makan terdekat hari ini, jam, hitung mundur singkat, dan tombol konfirmasi cepat.
     - Peringatan kontekstual jika izin alarm tepat waktu atau notifikasi belum aktif.
  3. **Jadwal Makan (Tab 2)**:
     - Menampilkan daftar seluruh pengingat makan (bawaan maupun tambahan) dengan sakelar aktif/nonaktif, informasi jam, serta lencana hari aktif (`Sen` s.d. `Min`).
     - Tombol tambah (`FAB`) dan dialog pengeditan untuk menambah/mengubah nama waktu makan, jam & menit, pemilihan hari aktif, atau menghapus jadwal.
  4. **Riwayat (Tab 3)**:
     - Grafik batang interaktif **7 Hari Terakhir** menggunakan Canvas dengan garis putus-putus indikator target harian dan penanda warna khusus saat target harian tercapai.
     - Pemilih hari di antara 7 hari terakhir untuk melihat daftar lengkap catatan minum per hari (jam pencatatan, jumlah ml, dan tombol hapus per item).
  5. **Pengaturan (Tab 4)**:
     - Pengaturan berat badan & kalkulator target otomatis/manual (disertai catatan penyangkalan bahwa angka merupakan perkiraan umum, bukan saran medis).
     - Pengaturan jam bangun, jam tidur, interval pengingat minum (30 menit, 1 jam, 1.5 jam, 2 jam, 3 jam, 4 jam), dan sakelar **"Hentikan pengingat saat target tercapai"**.
     - Pilihan tema tampilan (**Terang**, **Gelap**, **Ikuti Sistem**).
     - Kartu diagnostik & pintasan langsung ke Pengaturan Izin Notifikasi, Pengaturan Alarm & Pengingat Sistem, serta Pengaturan Optimasi Baterai perangkat.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Fresh Vitality & Warm Nourishment** — antarmuka modern berbasis Material Design 3 yang memadukan kesegaran air dengan kehangatan nutrisi secara bersih, kontras tinggi, dan bebas distraksi.
  - *Color Palette & Mood*:
    - `--primary`: **Ocean Cyan / Fresh Blue** (`#0284C7` pada mode terang, `#38BDF8` pada mode gelap) untuk progres hidrasi, grafik air, dan aksi utama minum.
    - `--secondary` / `--accent`: **Warm Tangerine Orange** (`#EA580C` pada mode terang, `#FB923C` pada mode gelap) khusus untuk kartu jadwal makan, ikon kuliner, dan penanda waktu makan.
    - `--background` & `--surface`: Latar putih sejuk kebiruan (`#F8FAFC`) dengan kartu berlapis lembut (`#FFFFFF` / `#F0F9FF`) pada mode terang, serta biru malam pekat (`#0B1120` / `#1E293B`) pada mode gelap.
  - *Typography & Hierarchy*:
    - Menggunakan pasangan font lokal Google Fonts (**Plus Jakarta Sans** untuk judul, angka metrik ml, dan header yang tegas namun ramah; dipadukan dengan keterbacaan tinggi pada teks isi dan label).
  - *Component Styling & Layout*:
    - Semua elemen interaktif mematuhi ukuran sentuh minimum `48dp × 48dp`, memiliki sudut membulat harmonis (`16dp`–`24dp`), serta menghormati `WindowInsets` (status bar dan navigation bar edge-to-edge).
- **Interactive Feedback & Motion**:
  - Animasi pengisian lingkaran progres air menggunakan *spring physics* yang halus saat pengguna menekan tombol `+100/+250/+500 ml`.
  - Umpan balik visual berupa *Snackbar* konfirmasi singkat setiap kali air tercatat, catatan terakhir dibatalkan, atau jadwal makan diperbarui.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Penjadwalan Pengingat Hemat Baterai dengan AlarmManager**
  - *Chosen Approach*: Menggunakan `AlarmManager` (`setExactAndAllowWhileIdle` bila izin tersedia, dengan *fallback* otomatis ke `setAndAllowWhileIdle` bila izin exact alarm ditolak/dicabut) yang dipadukan dengan `BroadcastReceiver`.
  - *Why*: Menjamin notifikasi makan dan minum muncul tepat waktu meskipun aplikasi ditutup, tanpa menguras baterai dengan *foreground service* permanen.
  - *Alternatives Considered*: `WorkManager` periodik (tidak menjamin ketepatan menit untuk jam makan) atau *Foreground Service* terus-menerus (boros baterai dan mengganggu bilah notifikasi).
- **Decision 2: Penyimpanan 100% Lokal & Ramping (Room + Preferences DataStore)**
  - *Chosen Approach*: Menggunakan database **Room** untuk entitas terstruktur (`MealReminder` dan `WaterLog`) serta **Preferences DataStore** untuk preferensi pengguna (`Settings`), sambil menonaktifkan dependensi cloud/jaringan yang tidak terpakai pada konfigurasi build.
  - *Why*: Memenuhi syarat mutlak PRD untuk aplikasi offline tanpa akun, cold start cepat (< 2 detik), privasi penuh di perangkat, dan menjaga ukuran rilis tetap di bawah 6 MB.
  - *Alternatives Considered*: Penyimpanan berbasis cloud (membutuhkan akun dan koneksi internet, bertentangan dengan tujuan PRD).

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ Onboarding  │  │ Beranda Tab  │  │ Jadwal Makan │  │ Riwayat &      │  │
│  │ Flow        │  │ (Hidrasi &   │  │ Tab (CRUD &  │  │ Pengaturan Tab │  │
│  │ (2 Langkah) │  │  Next Meal)  │  │  Hari Aktif) │  │ (Grafik 7 Hari)│  │
│  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘  └───────┬────────┘  │
└─────────┼────────────────┼─────────────────┼──────────────────┼───────────┘
          │                │                 │                  │
          ▼                ▼                 ▼                  ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    ViewModel Layer (StateFlow + MVVM)                     │
│                MakanMinumViewModel (UiState & Action Handlers)            │
└─────────────────┬───────────────────────────────────────┬─────────────────┘
                  │                                       │
                  ▼                                       ▼
┌──────────────────────────────────────┐ ┌──────────────────────────────────┐
│       Data & Repository Layer        │ │   Reminder & Notification Engine │
│  ┌────────────────────────────────┐  │ │  ┌────────────────────────────┐  │
│  │ MakanMinumRepository           │  │ │  │ ReminderScheduler          │  │
│  └───────┬────────────────┬───────┘  │ │  │ (Exact / Fallback Alarms)  │  │
│          │                │          │ │  └─────────────┬──────────────┘  │
│          ▼                ▼          │ │                │                 │
│  ┌──────────────┐ ┌───────────────┐  │ │                ▼                 │
│  │ Room DB      │ │ DataStore     │  │ │  ┌────────────────────────────┐  │
│  │ - MealDao    │ │ - UserPrefs   │  │ │  │ BroadcastReceivers         │  │
│  │ - WaterDao   │ │   (Berat, Jam │  │ │  │ - AlarmTriggerReceiver     │  │
│  │ (3 Default   │ │    Bangun/    │  │ │  │ - NotificationActionRcvr   │  │
│  │  Meals Seed) │ │    Tidur, dll)│  │ │  │   (Minum 250ml/Sudah/Tunda)│  │
│  └──────────────┘ └───────────────┘  │ │  │ - Boot & TimeChangeRcvr    │  │
└──────────────────────────────────────┘ └──────────────────────────────────┘
```

- **Data Model & State**:
  - **`MealReminderEntity` (Room)**:
    - `id: Long` (Primary Key, auto-generate)
    - `nama: String` (contoh: "Sarapan", "Makan Siang", "Makan Malam")
    - `jam: Int` (`0–23`), `menit: Int` (`0–59`)
    - `hariAktif: Int` (Bitmask 7-bit untuk Senin s.d. Minggu; default `127` / setiap hari)
    - `aktif: Boolean`
  - **`WaterLogEntity` (Room)**:
    - `id: Long` (Primary Key, auto-generate)
    - `tanggal: String` (Format `yyyy-MM-dd` waktu lokal untuk pengelompokan harian & reset otomatis pukul `00:00`)
    - `waktu: Long` (Epoch milliseconds untuk pencatatan jam & pengurutan)
    - `jumlahMl: Int` (Jumlah asupan dalam mililiter)
  - **`UserSettings` (Preferences DataStore)**:
    - `onboardingSelesai: Boolean`, `beratBadanKg: Int` (default `60`), `targetMl: Int` (default `2000`), `jamBangunMenit: Int` (default `06:00`), `jamTidurMenit: Int` (default `22:00`), `intervalMenit: Int` (default `90`), `tema: ThemeMode` (`SISTEM`, `TERANG`, `GELAP`), `stopSetelahTarget: Boolean` (default `true`), serta `tanggalMakan TerakhirDikonfirmasi` untuk status kartu makan di Beranda.

- **Interactive Component & State Mapping**:
  - **Pencatatan & Pembatalan Minum**: Menekan tombol `+100/+250/+500 ml` atau menyimpan dialog input kustom akan memanggil `repository.addWaterLog(ml)`, menyimpan catatan dengan tanggal lokal saat ini (`LocalDate.now()`), memperbarui progres radial secara reaktif via `Flow`, dan mengevaluasi ulang apakah pengingat minum berikutnya hari ini perlu dihentikan (jika `totalHariIni >= targetMl` dan `stopSetelahTarget == true`). Menekan **"Batalkan catatan terakhir"** menghapus entri terbaru pada tanggal hari ini.
  - **Aksi Langsung dari Notifikasi**:
    - Saat notifikasi minum muncul, menekan **"Minum 250 ml"** akan memicu `NotificationActionReceiver` menggunakan `goAsync()` untuk mencatat `250 ml` langsung ke database Room di latar belakang, menutup notifikasi, dan menjadwalkan pengingat berikutnya (atau menghentikannya untuk hari itu bila target tercapai).
    - Saat notifikasi makan muncul, menekan **"Sudah makan"** akan menutup notifikasi dan menandai jadwal makan tersebut sudah selesai untuk hari ini.
    - Menekan **"Tunda 10 menit"** pada notifikasi makan maupun minum akan menjadwalkan alarm sekali jalan tepat `+10 menit` dari waktu saat ini.
  - **Ketahanan Pasca-Restart & Perubahan Waktu**: `BootAndTimeReceiver` mendengarkan `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, dan `MY_PACKAGE_REPLACED` untuk membaca ulang seluruh jadwal makan aktif serta pengaturan interval minum dari Room/DataStore dan menjadwalkan ulang seluruh alarm secara akurat.
