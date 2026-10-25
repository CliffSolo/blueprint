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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/** One row per downloaded course folder; [sha] is its git tree SHA, so sync only re-downloads folders that changed. */
@Entity(tableName = "synced_course")
data class SyncedCourse(
    @PrimaryKey val dir: String,
    val sha: String,
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
    @Query("SELECT * FROM synced_course")
    suspend fun all(): List<SyncedCourse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(course: SyncedCourse)
}

@Database(entities = [LessonProgress::class, CourseState::class, SyncedCourse::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun sync(): SyncDao

    companion object {
        /** 0.3.0: courses moved from one repo each into one repo's courses/ folder. Keeps lesson progress. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS synced_repo")
                db.execSQL("CREATE TABLE IF NOT EXISTS synced_course (dir TEXT NOT NULL, sha TEXT NOT NULL, installedAt INTEGER NOT NULL, PRIMARY KEY(dir))")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "blueprint.db").addMigrations(MIGRATION_1_2).build()
    }
}
