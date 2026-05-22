package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        PayrollRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val accountDao: AccountDao
    abstract val journalDao: JournalDao
    abstract val productDao: ProductDao
    abstract val partnerDao: PartnerDao
    abstract val employeeDao: EmployeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accounting_database"
                )
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
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
