package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "aod_lists")
data class AodList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "aod_tasks",
    foreignKeys = [
        ForeignKey(
            entity = AodList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class AodTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listId: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: Int = 1, // 0 = Low, 1 = Medium, 2 = High
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface AodDao {
    @Query("SELECT * FROM aod_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<AodList>>

    @Query("SELECT * FROM aod_tasks WHERE listId = :listId ORDER BY isCompleted ASC, priority DESC, createdAt ASC")
    fun getTasksForList(listId: Int): Flow<List<AodTask>>

    @Query("SELECT * FROM aod_lists WHERE isPinned = 1 LIMIT 1")
    fun getPinnedListFlow(): Flow<AodList?>

    @Query("SELECT * FROM aod_lists WHERE isPinned = 1 LIMIT 1")
    suspend fun getPinnedList(): AodList?

    @Query("SELECT * FROM aod_tasks ORDER BY isCompleted ASC, priority DESC, createdAt ASC")
    fun getAllTasks(): Flow<List<AodTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: AodList): Long

    @Update
    suspend fun updateList(list: AodList)

    @Delete
    suspend fun deleteList(list: AodList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AodTask): Long

    @Update
    suspend fun updateTask(task: AodTask)

    @Delete
    suspend fun deleteTask(task: AodTask)

    @Query("UPDATE aod_lists SET isPinned = 0")
    suspend fun clearAllPins()

    @Transaction
    suspend fun pinList(listId: Int) {
        clearAllPins()
        setPinStatus(listId, true)
    }

    @Query("UPDATE aod_lists SET isPinned = :isPinned WHERE id = :listId")
    suspend fun setPinStatus(listId: Int, isPinned: Boolean)
}

@Database(entities = [AodList::class, AodTask::class], version = 1, exportSchema = false)
abstract class AodDatabase : RoomDatabase() {
    abstract fun aodDao(): AodDao

    companion object {
        @Volatile
        private var INSTANCE: AodDatabase? = null

        fun getDatabase(context: Context): AodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AodDatabase::class.java,
                    "aod_widget_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
