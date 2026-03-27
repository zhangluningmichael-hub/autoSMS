package com.example.autosms.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.autosms.model.SmsRule
import com.example.autosms.database.dao.SmsRuleDao

@Database(
    entities = [SmsRule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsRuleDao(): SmsRuleDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosms_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}