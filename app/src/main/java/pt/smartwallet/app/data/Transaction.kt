package pt.smartwallet.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val type: String, // INCOME ou EXPENSE
    val category: String,
    val dateMillis: Long = System.currentTimeMillis()
)
