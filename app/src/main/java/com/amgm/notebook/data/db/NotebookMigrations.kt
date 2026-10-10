package com.amgm.notebook.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** DB マイグレーション定義。 */
object NotebookMigrations {

    /**
     * v1 -> v2:
     * - notes.auto_sync_title_h1（H1自動同期フラグ。既存ノートは ON）
     * - tag_property_definitions.link_target_tag_id（NoteLink の既定絞り込みタグ。外部キーなし）
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN auto_sync_title_h1 INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE tag_property_definitions ADD COLUMN link_target_tag_id TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
