package com.example.dailywidget.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dailywidget.data.db.dao.DailySentenceDao
import com.example.dailywidget.data.db.entity.DailySentenceEntity

/**
 * Room 데이터베이스
 * 버전 3: styleId, backgroundId 제거 (DataStore로 이관)
 */
@Database(
    entities = [DailySentenceEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailySentenceDao(): DailySentenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 마이그레이션 1→2: writer 컬럼 추가
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_sentences ADD COLUMN writer TEXT")
            }
        }

        /**
         * 마이그레이션 2→3: styleId, backgroundId 컬럼 제거
         * 위젯 설정은 DataStore로 이관
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 임시 테이블 생성 (styleId, backgroundId 없음)
                db.execSQL("""
                    CREATE TABLE daily_sentences_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        genre TEXT NOT NULL,
                        text TEXT NOT NULL,
                        source TEXT,
                        writer TEXT,
                        extra TEXT
                    )
                """.trimIndent())

                // 2. 기존 데이터 복사 (styleId, backgroundId 제외)
                db.execSQL("""
                    INSERT INTO daily_sentences_new (id, date, genre, text, source, writer, extra)
                    SELECT id, date, genre, text, source, writer, extra
                    FROM daily_sentences
                """.trimIndent())

                // 3. 기존 테이블 삭제
                db.execSQL("DROP TABLE daily_sentences")

                // 4. 새 테이블 이름 변경
                db.execSQL("ALTER TABLE daily_sentences_new RENAME TO daily_sentences")
            }
        }

        /**
         * 데이터베이스 싱글톤 인스턴스 반환
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_widget.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}