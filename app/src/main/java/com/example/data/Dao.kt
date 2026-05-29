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

data class SalesQuotationWithOwner(
    @Embedded val quotation: QuotationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quotationId"
    )
    val lines: List<QuotationLineEntity>
)

data class SalesInvoiceWithOwner(
    @Embedded val invoice: SalesInvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lines: List<SalesInvoiceLineEntity>
)

data class SalesReturnWithOwner(
    @Embedded val returnEntity: SalesReturnEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "returnId"
    )
    val lines: List<SalesReturnLineEntity>
)

@Dao
interface SalesDao {
    @Transaction
    @Query("SELECT * FROM sales_quotations ORDER BY date DESC")
    fun getAllQuotations(): Flow<List<SalesQuotationWithOwner>>

    @Transaction
    @Query("SELECT * FROM sales_quotations WHERE id = :id")
    suspend fun getQuotationById(id: Long): SalesQuotationWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(q: QuotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotationLines(lines: List<QuotationLineEntity>)

    @Update
    suspend fun updateQuotation(q: QuotationEntity)

    @Delete
    suspend fun deleteQuotation(q: QuotationEntity)

    @Transaction
    @Query("SELECT * FROM sales_invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<SalesInvoiceWithOwner>>

    @Transaction
    @Query("SELECT * FROM sales_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): SalesInvoiceWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SalesInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceLines(lines: List<SalesInvoiceLineEntity>)

    @Update
    suspend fun updateInvoice(invoice: SalesInvoiceEntity)

    @Update
    suspend fun updateInvoiceLines(lines: List<SalesInvoiceLineEntity>)

    @Delete
    suspend fun deleteInvoice(invoice: SalesInvoiceEntity)

    @Transaction
    @Query("SELECT * FROM sales_returns ORDER BY date DESC")
    fun getAllReturns(): Flow<List<SalesReturnWithOwner>>

    @Transaction
    @Query("SELECT * FROM sales_returns WHERE id = :id")
    suspend fun getReturnById(id: Long): SalesReturnWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(ret: SalesReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnLines(lines: List<SalesReturnLineEntity>)

    @Transaction
    suspend fun saveQuotation(q: QuotationEntity, lines: List<QuotationLineEntity>) {
        val qId = insertQuotation(q)
        val linesWithId = lines.map { it.copy(quotationId = qId) }
        insertQuotationLines(linesWithId)
    }

    @Transaction
    suspend fun saveSalesInvoice(invoice: SalesInvoiceEntity, lines: List<SalesInvoiceLineEntity>) {
        val invId = insertInvoice(invoice)
        val linesWithId = lines.map { it.copy(invoiceId = invId) }
        insertInvoiceLines(linesWithId)
    }

    @Transaction
    suspend fun saveSalesReturn(ret: SalesReturnEntity, lines: List<SalesReturnLineEntity>) {
        val rId = insertReturn(ret)
        val linesWithId = lines.map { it.copy(returnId = rId) }
        insertReturnLines(linesWithId)
    }
}

data class PurchaseOrderWithOwner(
    @Embedded val order: PurchaseOrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val lines: List<PurchaseOrderLineEntity>
)

data class PurchaseInvoiceWithOwner(
    @Embedded val invoice: PurchaseInvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lines: List<PurchaseInvoiceLineEntity>
)

data class PurchaseReturnWithOwner(
    @Embedded val returnEntity: PurchaseReturnEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "returnId"
    )
    val lines: List<PurchaseReturnLineEntity>
)

@Dao
interface PurchaseDao {
    @Transaction
    @Query("SELECT * FROM purchase_orders ORDER BY date DESC")
    fun getAllOrders(): Flow<List<PurchaseOrderWithOwner>>

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    suspend fun getOrderById(id: Long): PurchaseOrderWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PurchaseOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderLines(lines: List<PurchaseOrderLineEntity>)

    @Update
    suspend fun updateOrder(order: PurchaseOrderEntity)

    @Delete
    suspend fun deleteOrder(order: PurchaseOrderEntity)

    @Transaction
    @Query("SELECT * FROM purchase_invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<PurchaseInvoiceWithOwner>>

    @Transaction
    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): PurchaseInvoiceWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: PurchaseInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceLines(lines: List<PurchaseInvoiceLineEntity>)

    @Update
    suspend fun updateInvoice(invoice: PurchaseInvoiceEntity)

    @Update
    suspend fun updateInvoiceLines(lines: List<PurchaseInvoiceLineEntity>)

    @Delete
    suspend fun deleteInvoice(invoice: PurchaseInvoiceEntity)

    @Transaction
    @Query("SELECT * FROM purchase_returns ORDER BY date DESC")
    fun getAllReturns(): Flow<List<PurchaseReturnWithOwner>>

    @Transaction
    @Query("SELECT * FROM purchase_returns WHERE id = :id")
    suspend fun getReturnById(id: Long): PurchaseReturnWithOwner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(ret: PurchaseReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnLines(lines: List<PurchaseReturnLineEntity>)

    @Transaction
    suspend fun savePurchaseOrder(order: PurchaseOrderEntity, lines: List<PurchaseOrderLineEntity>) {
        val oId = insertOrder(order)
        val linesWithId = lines.map { it.copy(orderId = oId) }
        insertOrderLines(linesWithId)
    }

    @Transaction
    suspend fun savePurchaseInvoice(invoice: PurchaseInvoiceEntity, lines: List<PurchaseInvoiceLineEntity>) {
        val invId = insertInvoice(invoice)
        val linesWithId = lines.map { it.copy(invoiceId = invId) }
        insertInvoiceLines(linesWithId)
    }

    @Transaction
    suspend fun savePurchaseReturn(ret: PurchaseReturnEntity, lines: List<PurchaseReturnLineEntity>) {
        val rId = insertReturn(ret)
        val linesWithId = lines.map { it.copy(returnId = rId) }
        insertReturnLines(linesWithId)
    }
}

