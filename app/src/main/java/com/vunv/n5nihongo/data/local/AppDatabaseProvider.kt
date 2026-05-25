package com.vunv.n5nihongo.data.local

import android.content.Context
import androidx.room.Room

object AppDatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        val currentInstance = instance
        if (currentInstance != null) {
            return currentInstance
        }

        return synchronized(this) {
            val existingInstance = instance
            if (existingInstance != null) {
                existingInstance
            } else {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "n5_nihongo.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                instance = newInstance
                newInstance
            }
        }
    }
}
