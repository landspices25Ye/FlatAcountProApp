package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Database(
    entities = [
        AccountEntity::class,
        JournalEntryEntity::class,
        JournalEntryLineEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        PartnerEntity::class,
        EmployeeEntity::class,
        PayrollRecordEntity::class,
        QuotationEntity::class,
        QuotationLineEntity::class,
        SalesInvoiceEntity::class,
        SalesInvoiceLineEntity::class,
        SalesReturnEntity::class,
        SalesReturnLineEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderLineEntity::class,
        PurchaseInvoiceEntity::class,
        PurchaseInvoiceLineEntity::class,
        PurchaseReturnEntity::class,
        PurchaseReturnLineEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val accountDao: AccountDao
    abstract val journalDao: JournalDao
    abstract val productDao: ProductDao
    abstract val partnerDao: PartnerDao
    abstract val employeeDao: EmployeeDao
    abstract val salesDao: SalesDao
    abstract val purchaseDao: PurchaseDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: employees
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `employees` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `code` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `department` TEXT NOT NULL, 
                        `phone` TEXT NOT NULL, 
                        `basicSalary` REAL NOT NULL, 
                        `isActive` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_employees_code` ON `employees` (`code`)")

                // Table: payroll_records
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payroll_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `employeeId` INTEGER NOT NULL, 
                        `month` TEXT NOT NULL, 
                        `basic` REAL NOT NULL, 
                        `allowance` REAL NOT NULL, 
                        `deductions` REAL NOT NULL, 
                        `net` REAL NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `paymentDate` INTEGER, 
                        FOREIGN KEY(`employeeId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payroll_records_employeeId` ON `payroll_records` (`employeeId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: sales_quotations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_quotations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `quotationNumber` TEXT NOT NULL, 
                        `customerId` INTEGER NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `expiryDate` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `subtotal` REAL NOT NULL, 
                        `vat` REAL NOT NULL, 
                        `grandTotal` REAL NOT NULL
                    )
                """.trimIndent())

                // Table: sales_quotation_lines
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_quotation_lines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `quotationId` INTEGER NOT NULL, 
                        `productId` INTEGER NOT NULL, 
                        `quantity` REAL NOT NULL, 
                        `price` REAL NOT NULL, 
                        `discount` REAL NOT NULL, 
                        FOREIGN KEY(`quotationId`) REFERENCES `sales_quotations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_quotation_lines_quotationId` ON `sales_quotation_lines` (`quotationId`)")

                // Table: sales_invoices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_invoices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `invoiceNumber` TEXT NOT NULL, 
                        `customerId` INTEGER NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `dueDate` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `isCredit` INTEGER NOT NULL, 
                        `subtotal` REAL NOT NULL, 
                        `vat` REAL NOT NULL, 
                        `grandTotal` REAL NOT NULL, 
                        `paidAmount` REAL NOT NULL, 
                        `paymentAccountOrCashId` INTEGER, 
                        `associatedJournalEntryId` INTEGER
                    )
                """.trimIndent())

                // Table: sales_invoice_lines
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_invoice_lines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `invoiceId` INTEGER NOT NULL, 
                        `productId` INTEGER NOT NULL, 
                        `quantity` REAL NOT NULL, 
                        `price` REAL NOT NULL, 
                        `discount` REAL NOT NULL, 
                        `returnedQuantity` REAL NOT NULL, 
                        FOREIGN KEY(`invoiceId`) REFERENCES `sales_invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoice_lines_invoiceId` ON `sales_invoice_lines` (`invoiceId`)")

                // Table: sales_returns
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_returns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `returnNumber` TEXT NOT NULL, 
                        `invoiceId` INTEGER NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `reason` TEXT NOT NULL, 
                        `refundAmount` REAL NOT NULL, 
                        `associatedJournalEntryId` INTEGER, 
                        FOREIGN KEY(`invoiceId`) REFERENCES `sales_invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_returns_invoiceId` ON `sales_returns` (`invoiceId`)")

                // Table: sales_return_lines
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_return_lines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `returnId` INTEGER NOT NULL, 
                        `productId` INTEGER NOT NULL, 
                        `quantity` REAL NOT NULL, 
                        `price` REAL NOT NULL, 
                        FOREIGN KEY(`returnId`) REFERENCES `sales_returns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_return_lines_returnId` ON `sales_return_lines` (`returnId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accounting_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetDatabase(context: Context) {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error closing DB", e)
                }
                INSTANCE = null
                context.deleteDatabase("accounting_database")
            }
        }

        suspend fun seedDefaultAccounts(accountDao: AccountDao) = withContext(Dispatchers.IO) {
            try {
                // Check if already populated
                if (accountDao.getAccountsCount() > 0) return@withContext

                // Category Roots (allowPosting = false)
                val assetsId = accountDao.insertAccount(
                    AccountEntity(code = "1000", nameAr = "الأصول", nameEn = "Assets", type = "ASSETS", allowPosting = false)
                )
                val liabilitiesId = accountDao.insertAccount(
                    AccountEntity(code = "2000", nameAr = "الخصوم (الالتزامات)", nameEn = "Liabilities", type = "LIABILITIES", allowPosting = false)
                )
                val equityId = accountDao.insertAccount(
                    AccountEntity(code = "3000", nameAr = "حقوق الملكية", nameEn = "Equity", type = "EQUITY", allowPosting = false)
                )
                val revenueId = accountDao.insertAccount(
                    AccountEntity(code = "4000", nameAr = "الإيرادات", nameEn = "Revenue", type = "REVENUE", allowPosting = false)
                )
                val expensesId = accountDao.insertAccount(
                    AccountEntity(code = "5000", nameAr = "المصروفات", nameEn = "Expenses", type = "EXPENSE", allowPosting = false)
                )

                // Child Accounts (allowPosting = true)
                accountDao.insertAccount(
                    AccountEntity(code = "1101", nameAr = "الصندوق (النقدية)", nameEn = "Cash on Hand", type = "ASSETS", parentId = assetsId)
                )
                accountDao.insertAccount(
                    AccountEntity(code = "1102", nameAr = "البنك", nameEn = "Bank (Cash at Bank)", type = "ASSETS", parentId = assetsId)
                )
                accountDao.insertAccount(
                    AccountEntity(code = "1201", nameAr = "الذمم المدينة (العملاء)", nameEn = "Accounts Receivable", type = "ASSETS", parentId = assetsId)
                )
                accountDao.insertAccount(
                    AccountEntity(code = "1301", nameAr = "المخزون السلعي", nameEn = "Inventory", type = "ASSETS", parentId = assetsId)
                )

                accountDao.insertAccount(
                    AccountEntity(code = "2101", nameAr = "الذمم الدائنة (الموردين)", nameEn = "Accounts Payable", type = "LIABILITIES", parentId = liabilitiesId)
                )

                accountDao.insertAccount(
                    AccountEntity(code = "3101", nameAr = "رأس المال المدفوع", nameEn = "Capital", type = "EQUITY", parentId = equityId)
                )

                accountDao.insertAccount(
                    AccountEntity(code = "4101", nameAr = "إيرادات المبيعات", nameEn = "Sales Revenue", type = "REVENUE", parentId = revenueId)
                )

                accountDao.insertAccount(
                    AccountEntity(code = "5101", nameAr = "تكلفة البضاعة المباعة", nameEn = "Cost of Goods Sold (COGS)", type = "EXPENSE", parentId = expensesId)
                )
                accountDao.insertAccount(
                    AccountEntity(code = "5201", nameAr = "مصروف الرواتب والأجور", nameEn = "Salaries & Wages Expense", type = "EXPENSE", parentId = expensesId)
                )
                accountDao.insertAccount(
                    AccountEntity(code = "5301", nameAr = "مصاريف إيجار ومرافق", nameEn = "Rent & Utilities", type = "EXPENSE", parentId = expensesId)
                )
                accountDao.insertAccount(
                    AccountEntity(code = "5401", nameAr = "فروقات جرد المخزون", nameEn = "Stock Variance Adjustment", type = "EXPENSE", parentId = expensesId)
                )
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Failed to seed default accounts", e)
            }
        }
    }
}
