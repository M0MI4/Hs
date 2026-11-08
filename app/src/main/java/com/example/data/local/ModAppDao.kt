package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModAppDao {

    @Query("SELECT * FROM mod_apps ORDER BY downloadsCount DESC")
    fun getAllApps(): Flow<List<ModAppEntity>>

    @Query("SELECT * FROM mod_apps WHERE category = :category ORDER BY downloadsCount DESC")
    fun getAppsByCategory(category: String): Flow<List<ModAppEntity>>

    @Query("SELECT * FROM mod_apps WHERE id = :id LIMIT 1")
    suspend fun getAppById(id: String): ModAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<ModAppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ModAppEntity)

    @Query("DELETE FROM mod_apps WHERE id = :id")
    suspend fun deleteAppById(id: String)

    @Query("DELETE FROM mod_apps")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM mod_apps")
    suspend fun getCount(): Int
}
