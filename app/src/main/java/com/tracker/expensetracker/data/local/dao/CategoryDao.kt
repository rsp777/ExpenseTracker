package com.tracker.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, displayName ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query(
        """
        UPDATE categories SET displayName = :displayName, colorHex = :colorHex, iconName = :iconName
        WHERE id = :id AND isDefault = 0
        """
    )
    suspend fun updateCategory(id: String, displayName: String, colorHex: Long, iconName: String): Int

    @Query("UPDATE categories SET isArchived = :archived WHERE id = :id AND isDefault = 0")
    suspend fun setCategoryArchived(id: String, archived: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
