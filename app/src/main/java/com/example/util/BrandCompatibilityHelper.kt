package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Membantu memastikan kompatibilitas pengingat MakanMinum di semua versi Android (12+)
 * dan berbagai antarmuka pabrikan (OEM) seperti Xiaomi/HyperOS, Samsung One UI,
 * Oppo/ColorOS, Vivo/Funtouch, Realme, Infinix/Tecno, Huawei, dsb.
 */
object BrandCompatibilityHelper {

    enum class PhoneBrand {
        XIAOMI,
        SAMSUNG,
        OPPO,
        VIVO,
        REALME,
        TRANSSION, // Infinix, Tecno, itel
        HUAWEI_HONOR,
        GENERIC
    }

    fun getDetectedBrand(): PhoneBrand {
        val manufacturer = runCatching { Build.MANUFACTURER?.lowercase() }.getOrNull().orEmpty()
        val brand = runCatching { Build.BRAND?.lowercase() }.getOrNull().orEmpty()
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                manufacturer.contains("redmi") || brand.contains("redmi") ||
                manufacturer.contains("poco") || brand.contains("poco") -> PhoneBrand.XIAOMI

            manufacturer.contains("samsung") || brand.contains("samsung") -> PhoneBrand.SAMSUNG

            manufacturer.contains("oppo") || brand.contains("oppo") -> PhoneBrand.OPPO

            manufacturer.contains("vivo") || brand.contains("vivo") ||
                manufacturer.contains("iqoo") || brand.contains("iqoo") -> PhoneBrand.VIVO

            manufacturer.contains("realme") || brand.contains("realme") -> PhoneBrand.REALME

            manufacturer.contains("infinix") || brand.contains("infinix") ||
                manufacturer.contains("tecno") || brand.contains("tecno") ||
                manufacturer.contains("itel") || brand.contains("itel") ||
                manufacturer.contains("transsion") || brand.contains("transsion") -> PhoneBrand.TRANSSION

            manufacturer.contains("huawei") || brand.contains("huawei") ||
                manufacturer.contains("honor") || brand.contains("honor") -> PhoneBrand.HUAWEI_HONOR

            else -> PhoneBrand.GENERIC
        }
    }

    fun getDeviceDisplayName(): String {
        val brand = runCatching { Build.BRAND?.replaceFirstChar { it.uppercase() } }.getOrNull()?.ifBlank { "Android" } ?: "Android"
        val model = runCatching { Build.MODEL }.getOrNull()?.ifBlank { "Device" } ?: "Device"
        val androidVer = runCatching { Build.VERSION.RELEASE }.getOrNull()?.ifBlank { "12+" } ?: "12+"
        return "$brand $model (Android $androidVer)"
    }

    fun getBrandNameLabel(): String {
        return when (getDetectedBrand()) {
            PhoneBrand.XIAOMI -> "Xiaomi / Redmi / POCO (MIUI / HyperOS)"
            PhoneBrand.SAMSUNG -> "Samsung (One UI)"
            PhoneBrand.OPPO -> "OPPO (ColorOS)"
            PhoneBrand.VIVO -> "Vivo / iQOO (Funtouch OS)"
            PhoneBrand.REALME -> "Realme (Realme UI)"
            PhoneBrand.TRANSSION -> "Infinix / Tecno (XOS / HiOS)"
            PhoneBrand.HUAWEI_HONOR -> "Huawei / Honor (EMUI / MagicOS)"
            PhoneBrand.GENERIC -> "Android Standar / Google Pixel / Motorola"
        }
    }

    fun getCompatibilityTips(): List<String> {
        return when (getDetectedBrand()) {
            PhoneBrand.XIAOMI -> listOf(
                "Aktifkan 'Mulai Otomatis' (Autostart) agar alarm tetap berbunyi saat aplikasi ditutup.",
                "Di 'Izin Lainnya', aktifkan 'Tampilkan di Layar Kunci' dan 'Tampilkan jendela pop-up saat di latar belakang'.",
                "Ubah 'Penghemat Baterai' ke 'Tidak ada pembatasan' (No restrictions) agar sistem MIUI/HyperOS tidak mematikan pengingat.",
                "Kunci aplikasi di layar 'Recent Apps' (tekan lama kartu aplikasi lalu ketuk ikon gembok)."
            )
            PhoneBrand.SAMSUNG -> listOf(
                "Buka 'Perawatan Perangkat' -> 'Baterai' -> 'Batas Penggunaan Latar Belakang'.",
                "Pastikan MakanMinum TIDAK masuk ke 'Aplikasi yang tidur' atau 'Aplikasi yang tidur lelap'.",
                "Tambahkan MakanMinum ke daftar 'Aplikasi yang tidak pernah tidur' (Never sleeping apps).",
                "Di menu Info Aplikasi -> Baterai, pilih opsi 'Tidak dibatasi' (Unrestricted)."
            )
            PhoneBrand.OPPO, PhoneBrand.REALME -> listOf(
                "Aktifkan 'Mulai otomatis' (Auto-launch) pada menu Manajemen Aplikasi -> Mulai Otomatis.",
                "Izinkan 'Aktivitas latar belakang' di pengaturan Penggunaan Baterai aplikasi MakanMinum.",
                "Nonaktifkan optimasi baterai agresif untuk MakanMinum agar alarm berdering tepat waktu.",
                "Kunci aplikasi di Recent Tasks dengan mengetuk titik tiga lalu pilih 'Kunci'."
            )
            PhoneBrand.VIVO -> listOf(
                "Buka 'iManager' -> 'Manajer Aplikasi' -> 'Mulai Otomatis' lalu aktifkan MakanMinum.",
                "Di Pengaturan Baterai, aktifkan 'Konsumsi daya latar belakang tinggi' untuk MakanMinum.",
                "Izinkan notifikasi dan alarm di layar kunci pada Pengaturan Notifikasi Vivo."
            )
            PhoneBrand.TRANSSION -> listOf(
                "Buka aplikasi 'Phone Master' bawaan HP -> 'Manajemen Mulai Otomatis' dan aktifkan MakanMinum.",
                "Pastikan fitur 'App Freeze' (Pembekuan Aplikasi) tidak mematikan MakanMinum.",
                "Atur konsumsi baterai aplikasi ke 'Tidak Terkelola' / 'Tidak Dibatasi'."
            )
            PhoneBrand.HUAWEI_HONOR -> listOf(
                "Buka Pengaturan -> Baterai -> 'Peluncuran Aplikasi' (App Launch).",
                "Cari MakanMinum, ubah dari 'Kelola otomatis' menjadi 'Kelola manual'.",
                "Aktifkan ketiga opsi: 'Mulai otomatis', 'Mulai sekunder', dan 'Jalankan di latar belakang'."
            )
            PhoneBrand.GENERIC -> listOf(
                "Izinkan 'Alarm & Pengingat Tepat Waktu' (SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM).",
                "Atur penggunaan baterai aplikasi ke 'Tidak dibatasi' (Unrestricted).",
                "Pastikan izin Notifikasi (POST_NOTIFICATIONS) aktif di Android 13+.",
                "Pastikan mode 'Jangan Ganggu' (DND) mengizinkan alarm berbunyi."
            )
        }
    }

    /**
     * Berusaha membuka halaman pengaturan khusus pabrikan (Autostart/Security Manager).
     * Jika tidak didukung oleh perangkat, secara aman jatuh tempo ke halaman Info Aplikasi.
     */
    fun openOemBackgroundSettings(context: Context): Boolean {
        val intents = getOemIntents(context)
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Lanjut coba intent alternatif
            }
        }

        // Fallback aman: Buka Pengaturan Info Aplikasi
        return runCatching {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        }.getOrDefault(false)
    }

    private fun getOemIntents(context: Context): List<Intent> {
        val list = mutableListOf<Intent>()

        when (getDetectedBrand()) {
            PhoneBrand.XIAOMI -> {
                list.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                list.add(Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
                list.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")))
            }
            PhoneBrand.SAMSUNG -> {
                list.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                list.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                list.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")))
            }
            PhoneBrand.OPPO, PhoneBrand.REALME -> {
                list.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                list.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                list.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
                list.add(Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelga设置.PowerConsumptionActivity")))
            }
            PhoneBrand.VIVO -> {
                list.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                list.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")))
                list.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))
            }
            PhoneBrand.TRANSSION -> {
                list.add(Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.settings.AutoRunSettingsActivity")))
                list.add(Intent().setComponent(ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.settings.AutoRunSettingsActivity")))
            }
            PhoneBrand.HUAWEI_HONOR -> {
                list.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                list.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
                list.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
            }
            PhoneBrand.GENERIC -> {}
        }

        // Tambahkan opsi standar abaikan optimasi baterai
        list.add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        return list
    }
}
