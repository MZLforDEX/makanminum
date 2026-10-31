package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealReminderDao {
    @Query("SELECT * FROM meal_reminders ORDER BY jam ASC, menit ASC, id ASC")
    fun observeAllMeals(): Flow<List<MealReminderEntity>>

    @Query("SELECT * FROM meal_reminders ORDER BY jam ASC, menit ASC, id ASC")
    suspend fun getAllMealsSync(): List<MealReminderEntity>

    @Query("SELECT * FROM meal_reminders WHERE id = :id LIMIT 1")
    suspend fun getMealById(id: Long): MealReminderEntity?

    @Query("SELECT COUNT(*) FROM meal_reminders")
    suspend fun countMeals(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealReminderEntity>)

    @Update
    suspend fun updateMeal(meal: MealReminderEntity)

    @Delete
    suspend fun deleteMeal(meal: MealReminderEntity)

    @Query("DELETE FROM meal_reminders WHERE id = :id")
    suspend fun deleteMealById(id: Long)
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE tanggal = :tanggal ORDER BY waktu DESC, id DESC")
    fun observeLogsForDate(tanggal: String): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE tanggal >= :startDate ORDER BY waktu DESC, id DESC")
    fun observeLogsSince(startDate: String): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(jumlahMl), 0) FROM water_logs WHERE tanggal = :tanggal")
    suspend fun getTotalWaterForDateSync(tanggal: String): Int

    @Query("SELECT * FROM water_logs WHERE tanggal = :tanggal ORDER BY waktu DESC, id DESC LIMIT 1")
    suspend fun getLatestWaterLogForDate(tanggal: String): WaterLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLogEntity): Long

    @Delete
    suspend fun deleteWaterLog(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLogById(id: Long)
}
