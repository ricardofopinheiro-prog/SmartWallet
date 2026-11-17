package pt.smartwallet.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class SmartWalletDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: SmartWalletDatabase? = null

        fun get(context: Context): SmartWalletDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartWalletDatabase::class.java,
                    "smartwallet.db"
                ).build().also { INSTANCE = it }
            }
    }
}
