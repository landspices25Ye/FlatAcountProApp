package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class AccountingRepository(private val db: AppDatabase) {

    // --- DAOs references ---
    val accountDao = db.accountDao
    val journalDao = db.journalDao
    val productDao = db.productDao
    val partnerDao = db.partnerDao
    val employeeDao = db.employeeDao

    // --- Flows ---
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allEntries: Flow<List<JournalEntryWithOwner>> = journalDao.getAllEntriesWithLines()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allPartners: Flow<List<PartnerEntity>> = partnerDao.getAllPartners()
    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    val allPayrolls: Flow<List<PayrollRecordEntity>> = employeeDao.getAllPayrollRecords()
    val allMovements: Flow<List<StockMovementEntity>> = productDao.getAllMovements()

    // --- Seed helper in case it didn't trigger ---
    suspend fun checkAndSeed() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            AppDatabase.seedDefaultAccounts(accountDao)
            // Ensure VAT Output account (Liabilities) exists
            val vatOutput = accountDao.getAccountByCode("2201")
            if (vatOutput == null) {
                val liabilities = accountDao.getAccountByCode("2000")
                accountDao.insertAccount(
                    AccountEntity(
                        code = "2201",
                        nameAr = "ضريبة القيمة المضافة المستحقة (مخرجات)",
                        nameEn = "VAT Output Tax",
                        type = "LIABILITIES",
                        parentId = liabilities?.id
                    )
                )
            }
            // Ensure VAT Input account (Assets) exists
            val vatInput = accountDao.getAccountByCode("1401")
            if (vatInput == null) {
                val assets = accountDao.getAccountByCode("1000")
                accountDao.insertAccount(
                    AccountEntity(
                        code = "1401",
                        nameAr = "ضريبة القيمة المضافة المستردة (مدخلات)",
                        nameEn = "VAT Input Tax",
                        type = "ASSETS",
                        parentId = assets?.id
                    )
                )
            }
            // Ensure Zakat Provision account exists
            val zakatProvision = accountDao.getAccountByCode("2301")
            if (zakatProvision == null) {
                val liabilities = accountDao.getAccountByCode("2000")
                accountDao.insertAccount(
                    AccountEntity(
                        code = "2301",
                        nameAr = "مخصص الزكاة الشرعية المستحقة",
                        nameEn = "Provision for Zakat & Taxes",
                        type = "LIABILITIES",
                        parentId = liabilities?.id
                    )
                )
            }
        } catch (t: Throwable) {
            android.util.Log.e("AccountingRepository", "Failed to dynamically seed VAT/Zakat accounts or initialize DB", t)
        }
    }

    // --- Accounts Transactions ---
    suspend fun addAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    // --- Journal Entry Transactions ---
    suspend fun saveJournalEntry(entry: JournalEntryEntity, lines: List<JournalEntryLineEntity>) {
        journalDao.saveJournalEntry(entry, lines)
    }

    suspend fun postJournalEntry(entryId: Long) {
        val entryWithLines = journalDao.getEntryWithLinesById(entryId) ?: return
        if (entryWithLines.entry.status == "POSTED") return

        // Mark posted
        val updatedEntry = entryWithLines.entry.copy(status = "POSTED")
        journalDao.updateEntry(updatedEntry)
    }

    suspend fun deleteJournalEntry(id: Long) {
        journalDao.deleteEntry(id)
    }

    // --- Inventory/Products Transactions & Auto Cost Accounting ---
    suspend fun addProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    // Records manual adjustment, updates stock and creates a journal entry
    suspend fun recordStockAdjustment(
        productId: Long,
        qtyChange: Double, // can be positive or negative
        description: String,
        timestamp: Long
    ) {
        val product = productDao.getProductById(productId) ?: return
        val currentStock = product.stock
        val newStock = currentStock + qtyChange

        val updatedProduct = product.copy(stock = if (newStock < 0) 0.0 else newStock)
        productDao.updateProduct(updatedProduct)

        // Insert movement
        val mvType = if (qtyChange >= 0) "ADJUSTMENT_IN" else "ADJUSTMENT_OUT"
        productDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                type = mvType,
                quantity = Math.abs(qtyChange),
                unitCost = product.cost,
                date = timestamp,
                description = description
            )
        )

        // Generate accounting double entry for adjustment
        // Find general inventory account (1301) and stock variance adjustment account (5401)
        val invAcc = accountDao.getAccountByCode("1301")
        val varAcc = accountDao.getAccountByCode("5401")
        if (invAcc != null && varAcc != null) {
            val totalCostChange = Math.abs(qtyChange) * product.cost
            if (totalCostChange > 0) {
                val entryNo = "JV-ADJ-" + (timestamp % 100000)
                val status = "POSTED" // Auto post adjustment entries
                val entry = JournalEntryEntity(
                    entryNumber = entryNo,
                    date = timestamp,
                    description = "$description - $mvType",
                    status = status
                )

                val lines = if (qtyChange >= 0) {
                    // Increase inventory: Debit Inventory (1301), Credit Adjustment (5401)
                    listOf(
                        JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = totalCostChange, credit = 0.0, description = description),
                        JournalEntryLineEntity(entryId = 0, accountId = varAcc.id, debit = 0.0, credit = totalCostChange, description = description)
                    )
                } else {
                    // Decrease inventory: Debit Adjustment (5401), Credit Inventory (1301)
                    listOf(
                        JournalEntryLineEntity(entryId = 0, accountId = varAcc.id, debit = totalCostChange, credit = 0.0, description = description),
                        JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = 0.0, credit = totalCostChange, description = description)
                    )
                }
                saveJournalEntry(entry, lines)
            }
        }
    }

    // --- Partners Transactions ---
    suspend fun addPartner(partner: PartnerEntity): Long {
        return partnerDao.insertPartner(partner)
    }

    suspend fun updatePartner(partner: PartnerEntity) {
        partnerDao.updatePartner(partner)
    }

    suspend fun deletePartner(partner: PartnerEntity) {
        partnerDao.deletePartner(partner)
    }

    // --- HR/Employees Transactions ---
    suspend fun addEmployee(employee: EmployeeEntity): Long {
        return employeeDao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: EmployeeEntity) {
        employeeDao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        employeeDao.deleteEmployee(employee)
    }

    // --- Payroll records & Auto Payment posting ---
    suspend fun generatePayroll(month: String, allowancePct: Double = 0.0, deductionPct: Double = 0.0) {
        val employees = employeeDao.getAllEmployees().first()
        // Loop active employees
        employees.filter { it.isActive }.forEach { emp ->
            val allowance = emp.basicSalary * (allowancePct / 100.0)
            val deduction = emp.basicSalary * (deductionPct / 100.0)
            val net = emp.basicSalary + allowance - deduction

            employeeDao.insertPayrollRecord(
                PayrollRecordEntity(
                    employeeId = emp.id,
                    month = month, basic = emp.basicSalary, allowance = allowance, deductions = deduction, net = net, status = "UNPAID"
                )
            )
        }
    }

    suspend fun processPayrollPayment(record: PayrollRecordEntity, paymentDate: Long, cashAccountId: Long) {
        // Find employee salaries expense account (5201)
        val salExpAcc = accountDao.getAccountByCode("5201") ?: return
        
        // Update status of payroll to PAID
        val paidRecord = record.copy(status = "PAID", paymentDate = paymentDate)
        employeeDao.updatePayrollRecord(paidRecord)

        // Create journal entry: Debit Salary Expense (5201) and Credit Selected Cash/Bank Account (cashAccountId)
        val emp = employeeDao.getEmployeeById(record.employeeId)
        val empName = emp?.name ?: "Mofaza"
        val entryNo = "PV-PAY-" + (paymentDate % 100000)
        val entry = JournalEntryEntity(
            entryNumber = entryNo,
            date = paymentDate,
            description = "صرف راتب الموظف/ $empName لشهر: ${record.month}",
            status = "POSTED"
        )
        val lines = listOf(
            JournalEntryLineEntity(entryId = 0, accountId = salExpAcc.id, debit = record.net, credit = 0.0, description = "صرف الرواتب"),
            JournalEntryLineEntity(entryId = 0, accountId = cashAccountId, debit = 0.0, credit = record.net, description = "صرف رواتب")
        )
        saveJournalEntry(entry, lines)
    }

    // --- Advanced Sales & Purchases Workflows with Auto Journal Entries & Stock tracking ---
    suspend fun addSalesInvoice(
        customerId: Long,
        productId: Long,
        quantity: Double,
        price: Double, // custom override price if any
        cashAccountId: Long, // e.g., Cash or Bank to Credit
        timestamp: Long,
        description: String = ""
    ) {
        val product = productDao.getProductById(productId) ?: return
        val customer = partnerDao.getPartnerById(customerId) ?: return

        // 1. Update product inventory stock
        val newStock = product.stock - quantity
        productDao.updateProduct(product.copy(stock = if (newStock < 0) 0.0 else newStock))

        // 2. Add Stock movement
        productDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                type = "SALE",
                quantity = quantity,
                unitCost = product.cost,
                date = timestamp,
                description = "فاتورة مبيعات للعميل ${customer.name}"
            )
        )

        // 3. Create Balanced Accounting Entry with VAT 15%!
        val revenueAcc = accountDao.getAccountByCode("4101") ?: return
        val invAcc = accountDao.getAccountByCode("1301") ?: return
        val cogsAcc = accountDao.getAccountByCode("5101") ?: return
        val vatOutputAcc = accountDao.getAccountByCode("2201")

        val saleAmount = quantity * price
        val vatAmount = saleAmount * 0.15
        val totalAmountCollected = saleAmount + vatAmount
        val costAmount = quantity * product.cost

        val entryNo = "SI-INV-${timestamp % 100000}"
        val entry = JournalEntryEntity(
            entryNumber = entryNo,
            date = timestamp,
            description = "مبيعات إلى ${customer.name} - منتج: ${product.name} (شامل ضريبة 15٪)",
            status = "POSTED"
        )

        val lines = mutableListOf(
            // Debit Cash/Bank (cashAccountId) -> Total Amount Collected
            JournalEntryLineEntity(entryId = 0, accountId = cashAccountId, debit = totalAmountCollected, credit = 0.0, description = "تحصيل قيمة مبيعات شامل الضريبة"),
            // Credit Sales Revenue (revenueAcc.id) -> Base Sale Amount
            JournalEntryLineEntity(entryId = 0, accountId = revenueAcc.id, debit = 0.0, credit = saleAmount, description = "إيراد مبيعات مستقل")
        )

        // Credit VAT Output Tax -> 15% VAT Liability
        if (vatOutputAcc != null && vatAmount > 0.0) {
            lines.add(JournalEntryLineEntity(entryId = 0, accountId = vatOutputAcc.id, debit = 0.0, credit = vatAmount, description = "ضريبة القيمة المضافة مخرجات (15٪)"))
        }

        // Insert COGS lines if it's a PRODUCT with actual inventory cost tracking
        if (product.type == "PRODUCT" && costAmount > 0.0) {
            lines.add(JournalEntryLineEntity(entryId = 0, accountId = cogsAcc.id, debit = costAmount, credit = 0.0, description = "تكلفة البضاعة المباعة"))
            lines.add(JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = 0.0, credit = costAmount, description = "صرف بضاعة من المخزن"))
        }

        saveJournalEntry(entry, lines)
    }

    suspend fun addPurchaseInvoice(
        supplierId: Long,
        productId: Long,
        quantity: Double,
        cost: Double, // New buying cost
        paymentAccountId: Long, // e.g., Bank or Cash to credit payment
        timestamp: Long,
        description: String = ""
    ) {
        val product = productDao.getProductById(productId) ?: return
        val supplier = partnerDao.getPartnerById(supplierId) ?: return

        // Calculate New Moving Weighted Average Cost
        val oldQty = product.stock
        val oldCost = product.cost
        val totalQty = oldQty + quantity
        val newAvgCost = if (totalQty > 0) {
            ((oldQty * oldCost) + (quantity * cost)) / totalQty
        } else {
            cost
        }

        // 1. Update product stock & cost
        productDao.updateProduct(
            product.copy(
                stock = totalQty,
                cost = newAvgCost
            )
        )

        // 2. Add stock movement
        productDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                type = "PURCHASE",
                quantity = quantity,
                unitCost = cost,
                date = timestamp,
                description = "فاتورة مشتريات من المورد ${supplier.name}"
            )
        )

        // 3. Create Balanced Accounting Entry with VAT 15%!
        val invAcc = accountDao.getAccountByCode("1301") ?: return
        val vatInputAcc = accountDao.getAccountByCode("1401")

        val purchaseAmount = quantity * cost
        val vatAmount = purchaseAmount * 0.15
        val totalPaidAmount = purchaseAmount + vatAmount

        val entryNo = "PI-INV-${timestamp % 100000}"
        val entry = JournalEntryEntity(
            entryNumber = entryNo,
            date = timestamp,
            description = "مشتريات من ${supplier.name} - منتج: ${product.name} (شامل ضريبة 15٪)",
            status = "POSTED"
        )

        val lines = mutableListOf(
            // Debit Inventory (invAcc.id)
            JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = purchaseAmount, credit = 0.0, description = "توريد بضاعة للمخزن"),
            // Credit Cash/Bank (paymentAccountId)
            JournalEntryLineEntity(entryId = 0, accountId = paymentAccountId, debit = 0.0, credit = totalPaidAmount, description = "سداد قيمة مشتريات شامل الضريبة")
        )

        // Debit VAT Input Tax (asset)
        if (vatInputAcc != null && vatAmount > 0.0) {
            lines.add(JournalEntryLineEntity(entryId = 0, accountId = vatInputAcc.id, debit = vatAmount, credit = 0.0, description = "ضريبة القيمة المضافة مدخلات (15٪)"))
        }

        saveJournalEntry(entry, lines)
    }
}
