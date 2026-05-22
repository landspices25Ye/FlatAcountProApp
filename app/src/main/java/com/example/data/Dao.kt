package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Support builder syntax for instantiate in Kotlin safely
data class JournalEntryWithOwner(
    @Embedded val entry: JournalEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val lines: List<JournalEntryLineEntity>
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY code ASC")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE code = :code")
    suspend fun getAccountByCode(code: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountsCount(): Int
}

@Dao
interface JournalDao {
    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntriesWithLines(): Flow<List<JournalEntryWithOwner>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryWithLinesById(id: Long): JournalEntryWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<JournalEntryLineEntity>)

    @Query("DELETE FROM journal_entry_lines WHERE entryId = :entryId")
    suspend fun deleteLinesForEntry(entryId: Long)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Transaction
    suspend fun saveJournalEntry(entry: JournalEntryEntity, lines: List<JournalEntryLineEntity>) {
        val entryId = insertEntry(entry)
        val linesWithId = lines.map { it.copy(entryId = entryId) }
        insertLines(linesWithId)
    }

    @Transaction
    suspend fun updateJournalEntry(entry: JournalEntryEntity, lines: List<JournalEntryLineEntity>) {
        updateEntry(entry)
        deleteLinesForEntry(entry.id)
        val linesWithId = lines.map { it.copy(entryId = entry.id) }
        insertLines(linesWithId)
    }
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY code ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY code ASC")
    suspend fun getAllProductsSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY date DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY date DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>
}

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners ORDER BY name ASC")
    fun getAllPartners(): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partners WHERE id = :id")
    suspend fun getPartnerById(id: Long): PartnerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerEntity): Long

    @Update
    suspend fun updatePartner(partner: PartnerEntity)

    @Delete
    suspend fun deletePartner(partner: PartnerEntity)
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    @Query("SELECT * FROM payroll_records ORDER BY month DESC")
    fun getAllPayrollRecords(): Flow<List<PayrollRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollRecord(record: PayrollRecordEntity): Long

    @Update
    suspend fun updatePayrollRecord(record: PayrollRecordEntity)
}
