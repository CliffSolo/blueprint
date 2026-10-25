package com.cliffsolo.blueprint.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lesson_progress", primaryKeys = ["courseId", "lessonId"])
data class LessonProgress(
    val courseId: String,
    val lessonId: String,
    val completedAt: Long,
    val quizCorrect: Int,
    val quizTotal: Int,
)

@Entity(tableName = "course_state")
data class CourseState(
    @PrimaryKey val courseId: String,
    val lastLessonId: String?,
    val lastOpenedAt: Long,
)

/** One row per GitHub repo we've downloaded, so sync only re-downloads repos that changed. */
@Entity(tableName = "synced_repo")
data class SyncedRepo(
    @PrimaryKey val repo: String,
    val pushedAt: String,
    val installedAt: Long,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun allProgress(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE courseId = :courseId")
    fun progressFor(courseId: String): Flow<List<LessonProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: LessonProgress)

    @Query("SELECT * FROM course_state")
    fun courseStates(): Flow<List<CourseState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCourseState(state: CourseState)

    @Query("DELETE FROM lesson_progress WHERE courseId = :courseId")
    suspend fun resetCourse(courseId: String)
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM synced_repo")
    suspend fun all(): List<SyncedRepo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(repo: SyncedRepo)
}

@Database(entities = [LessonProgress::class, CourseState::class, SyncedRepo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun sync(): SyncDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "blueprint.db").build()
    }
}
