package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["code"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val type: String, // ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSE
    val parentId: Long? = null,
    val allowPosting: Boolean = true,
    val isDefault: Boolean = false
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryNumber: String,
    val date: Long,
    val description: String,
    val status: String, // DRAFT, POSTED
    val createdBy: String = "User",
    val isClosing: Boolean = false
)

@Entity(
    tableName = "journal_entry_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("entryId"), Index("accountId")]
)
data class JournalEntryLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val accountId: Long,
    val debit: Double,
    val credit: Double,
    val description: String = ""
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["code"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val price: Double,
    val cost: Double,
    val stock: Double = 0.0,
    val minStock: Double = 2.0,
    val type: String = "PRODUCT" // PRODUCT, SERVICE
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val type: String, // PURCHASE, SALE, ADJUSTMENT_IN, ADJUSTMENT_OUT
    val quantity: Double,
    val unitCost: Double,
    val date: Long,
    val description: String
)

@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // CUSTOMER, SUPPLIER
    val phone: String = "",
    val email: String = "",
    val creditLimit: Double = 10000.0,
    val balance: Double = 0.0
)

@Entity(
    tableName = "employees",
    indices = [Index(value = ["code"], unique = true)]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val department: String,
    val phone: String = "",
    val basicSalary: Double,
    val isActive: Boolean = true
)

@Entity(
    tableName = "payroll_records",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("employeeId")]
)
data class PayrollRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val month: String, // e.g. "2026-05"
    val basic: Double,
    val allowance: Double,
    val deductions: Double,
    val net: Double,
    val status: String, // UNPAID, PAID
    val paymentDate: Long? = null
)
