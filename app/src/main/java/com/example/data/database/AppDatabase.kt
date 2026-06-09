package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.User
import com.example.data.model.AttendanceLog
import com.example.data.model.AttendanceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, AttendanceLog::class, AttendanceConfig::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun attendanceConfigDao(): AttendanceConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "micet_net_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(
                        database.userDao(),
                        database.attendanceConfigDao()
                    )
                }
            }
        }

        suspend fun populateDatabase(userDao: UserDao, configDao: AttendanceConfigDao) {
            // Check if database is empty
            if (userDao.getUserCount() == 0) {
                // Seed Default Admin
                userDao.insertUser(
                    User(
                        username = "admin",
                        password = "admin",
                        fullName = "Administrator Micet",
                        role = "ADMIN"
                    )
                )
                // Seed Default Employee
                userDao.insertUser(
                    User(
                        username = "karyawan",
                        password = "karyawan",
                        fullName = "Ahmad Karyawan",
                        role = "EMPLOYEE"
                    )
                )
                // Seed extra employees for testing
                userDao.insertUser(
                    User(
                        username = "budi",
                        password = "budi",
                        fullName = "Budi Hartono",
                        role = "EMPLOYEE"
                    )
                )

                // Seed Default Configuration
                configDao.insertConfig(AttendanceConfig())
            }
        }
    }
}
