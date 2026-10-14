package com.amgm.notebook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NotePropertyEntity
import com.amgm.notebook.data.db.entity.NoteTagEntity
import com.amgm.notebook.data.db.entity.TagEntity
import com.amgm.notebook.data.db.entity.TagParentEntity
import com.amgm.notebook.data.db.entity.TagPropertyDefinitionEntity

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        TagParentEntity::class,
        NoteTagEntity::class,
        TagPropertyDefinitionEntity::class,
        NotePropertyEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NotebookDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "notebook.db"

        /** 端末内ファイル `notebook.db` に保存するDB。 */
        fun create(context: Context): NotebookDatabase =
            Room.databaseBuilder(context.applicationContext, NotebookDatabase::class.java, DATABASE_NAME)
                .build()

        /** テスト用のインメモリDB。 */
        fun inMemory(context: Context): NotebookDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, NotebookDatabase::class.java)
                .build()
    }
}
