package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MealReminderEntity::class, WaterLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MakanMinumDatabase : RoomDatabase() {
    abstract fun mealReminderDao(): MealReminderDao
    abstract fun waterLogDao(): WaterLogDao

    companion object {
        @Volatile
        private var INSTANCE: MakanMinumDatabase? = null

        fun getInstance(context: Context): MakanMinumDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MakanMinumDatabase::class.java,
                    "makan_minum.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val DEFAULT_MEAL_REMINDERS = listOf(
            MealReminderEntity(
                nama = "Sarapan",
                jam = 7,
                menit = 0,
                hariAktif = MealReminderEntity.ALL_DAYS_MASK,
                aktif = true
            ),
            MealReminderEntity(
                nama = "Makan Siang",
                jam = 12,
                menit = 0,
                hariAktif = MealReminderEntity.ALL_DAYS_MASK,
                aktif = true
            ),
            MealReminderEntity(
                nama = "Makan Malam",
                jam = 19,
                menit = 0,
                hariAktif = MealReminderEntity.ALL_DAYS_MASK,
                aktif = true
            )
        )
    }
}
