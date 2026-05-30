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
    val salesDao = db.salesDao
    val purchaseDao = db.purchaseDao

    // --- Flows ---
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allEntries: Flow<List<JournalEntryWithOwner>> = journalDao.getAllEntriesWithLines()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allWarehouses: Flow<List<WarehouseEntity>> = productDao.getAllWarehouses()
    val allPartners: Flow<List<PartnerEntity>> = partnerDao.getAllPartners()
    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    val allPayrolls: Flow<List<PayrollRecordEntity>> = employeeDao.getAllPayrollRecords()
    val allMovements: Flow<List<StockMovementEntity>> = productDao.getAllMovements()
    val allQuotations: Flow<List<SalesQuotationWithOwner>> = salesDao.getAllQuotations()
    val allInvoices: Flow<List<SalesInvoiceWithOwner>> = salesDao.getAllInvoices()
    val allReturns: Flow<List<SalesReturnWithOwner>> = salesDao.getAllReturns()
    val allPurchaseOrders: Flow<List<PurchaseOrderWithOwner>> = purchaseDao.getAllOrders()
    val allPurchaseInvoices: Flow<List<PurchaseInvoiceWithOwner>> = purchaseDao.getAllInvoices()
    val allPurchaseReturns: Flow<List<PurchaseReturnWithOwner>> = purchaseDao.getAllReturns()

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
        if (account.code.isBlank()) {
            throw com.example.core.errors.BusinessRuleError("رمز الحساب لا يمكن أن يكون فارغاً")
        }
        if (!account.code.all { it.isDigit() }) {
            throw com.example.core.errors.BusinessRuleError("رمز الحساب يجب أن يحتوي على أرقام فقط")
        }
        
        val firstDigit = account.code.first()
        val expectedType = when (firstDigit) {
            '1' -> "ASSETS"
            '2' -> "LIABILITIES"
            '3' -> "EQUITY"
            '4' -> "REVENUE"
            '5' -> "EXPENSE"
            else -> null
        }
        if (expectedType == null) {
            throw com.example.core.errors.BusinessRuleError("يجب أن يبدأ رمز الحساب بأرقام الفئات القياسية (1: أصول، 2: خصوم، 3: حقوق ملكية، 4: إيرادات، 5: مصروفات)")
        }
        if (account.type != expectedType) {
            throw com.example.core.errors.BusinessRuleError("نوع الحساب المحدد لا يطابق رمز الحساب. الرمز الذي يبدأ بـ $firstDigit يجب أن يكون من نوع $expectedType")
        }

        val existing = accountDao.getAccountByCode(account.code)
        if (existing != null) {
            throw com.example.core.errors.BusinessRuleError("رمز الحساب [${account.code}] مستخدم بالفعل!")
        }

        if (account.parentId != null) {
            val parent = accountDao.getAccountById(account.parentId)
                ?: throw com.example.core.errors.BusinessRuleError("الحساب الأب المحدد غير موجود")
            if (parent.type != account.type) {
                throw com.example.core.errors.BusinessRuleError("يجب أن يكون الحساب الأب من نفس نوع تصنيف الحساب الأساسي")
            }
            if (parent.allowPosting) {
                throw com.example.core.errors.BusinessRuleError("لا يمكن تفرع حساب من حساب مسموح بالترحيل المباشر عليه")
            }
        }

        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        if (account.isDefault) {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف حساب افتراضي معزز في النظام")
        }
        
        val linesCount = db.openHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM journal_entry_lines WHERE accountId = ${account.id}"
        ).simpleQueryForLong()
        
        if (linesCount > 0) {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف الحساب [${account.code}] لارتباطه بقيود تجارية مسجلة بالدفاتر")
        }
        
        val childrenCount = db.openHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM accounts WHERE parentId = ${account.id}"
        ).simpleQueryForLong()
        
        if (childrenCount > 0) {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف هذا الحساب لوجود تفريعات وحسابات تابعة له في الشجرة")
        }

        accountDao.deleteAccount(account)
    }

    // --- Journal Entry Transactions ---
    suspend fun saveJournalEntry(entry: JournalEntryEntity, lines: List<JournalEntryLineEntity>) {
        if (lines.isEmpty()) {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حفظ قيد فارغ بدون بنود")
        }
        val sumDebits = lines.sumOf { it.debit }
        val sumCredits = lines.sumOf { it.credit }
        if (Math.abs(sumDebits - sumCredits) > 0.001) {
            throw com.example.core.errors.BusinessRuleError("القيد غير متوازن مالياً: إجمالي المدين ($sumDebits) لا يساوي إجمالي الدائن ($sumCredits)")
        }
        journalDao.saveJournalEntry(entry, lines)
    }

    private fun getPeriodIdFromTimestamp(timestamp: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(timestamp))
    }

    suspend fun postJournalEntry(entryId: Long) {
        val entryWithLines = journalDao.getEntryWithLinesById(entryId) ?: return
        if (entryWithLines.entry.status == "POSTED") return

        val periodId = getPeriodIdFromTimestamp(entryWithLines.entry.date)
        val periods = com.example.core.di.ServiceContainer.getInstance().settingsManager.getFiscalPeriods()
        val period = periods.find { it.id == periodId }
        if (period == null) {
            throw com.example.core.errors.BusinessRuleError("لا توجد فترة مالية معروفة لهذا التاريخ: $periodId")
        }
        if (period.status != "OPEN") {
            throw com.example.core.errors.BusinessRuleError("الفترة المالية $periodId مغلقة أو مقفلة، لا يمكن الترحيل إليها")
        }

        val updatedEntry = entryWithLines.entry.copy(status = "POSTED")
        journalDao.updateEntry(updatedEntry)
    }

    suspend fun reverseJournalEntry(entryId: Long) {
        val entryWithLines = journalDao.getEntryWithLinesById(entryId) 
            ?: throw com.example.core.errors.BusinessRuleError("القيد المطلوب غير موجود")
        
        if (entryWithLines.entry.status != "POSTED") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن عكس قيد غير مرحل")
        }
        
        val currentTime = System.currentTimeMillis()
        val currentPeriodId = getPeriodIdFromTimestamp(currentTime)
        val periods = com.example.core.di.ServiceContainer.getInstance().settingsManager.getFiscalPeriods()
        val currentPeriod = periods.find { it.id == currentPeriodId }
        if (currentPeriod == null || currentPeriod.status != "OPEN") {
            throw com.example.core.errors.BusinessRuleError("الفترة المالية الحالية ($currentPeriodId) مغلقة أو مقفلة، لا يمكن إجراء العكس المالي بها")
        }

        val reverseNo = "JV-REV-${entryWithLines.entry.entryNumber}"
        val reverseEntry = JournalEntryEntity(
            entryNumber = reverseNo,
            date = currentTime,
            description = "عكس القيد المحاسبي رقم: ${entryWithLines.entry.entryNumber} - ${entryWithLines.entry.description}",
            status = "POSTED",
            isClosing = false
        )

        val reverseLines = entryWithLines.lines.map { line ->
            JournalEntryLineEntity(
                entryId = 0,
                accountId = line.accountId,
                debit = line.credit,
                credit = line.debit,
                description = "قيد عكسي للقيد ${entryWithLines.entry.entryNumber}"
            )
        }

        journalDao.saveJournalEntry(reverseEntry, reverseLines)
    }

    suspend fun closePeriod(periodId: String) {
        val periods = com.example.core.di.ServiceContainer.getInstance().settingsManager.getFiscalPeriods()
        val period = periods.find { it.id == periodId } 
            ?: throw com.example.core.errors.BusinessRuleError("الفترة المالية المطلوبة غير موجودة")
        
        if (period.status != "OPEN") {
            throw com.example.core.errors.BusinessRuleError("الفترة المالية ليست مفتوحة لكي يتم إغلاقها")
        }

        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val date = sdf.parse(periodId) 
            ?: throw com.example.core.errors.BusinessRuleError("تنسيق معرف الفترة غير صالح")
        
        val cal = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        
        cal.add(java.util.Calendar.MONTH, 1)
        val endTime = cal.timeInMillis

        val draftsCount = db.openHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM journal_entries WHERE status = 'DRAFT' AND date >= $startTime AND date < $endTime"
        ).simpleQueryForLong()
        
        if (draftsCount > 0) {
            throw com.example.core.errors.BusinessRuleError("لا يمكن إغلاق الفترة المالية لوجود قيود مسودة غير مرحلة بها ($draftsCount قيود)")
        }

        val capitalAcc = accountDao.getAccountByCode("3101") 
            ?: throw com.example.core.errors.BusinessRuleError("حساب رأس المال/الأرباح المحتجزة (3101) غير معرف لتجلي صافي النتيجة")
        
        val queryStr = """
            SELECT l.accountId, a.type, SUM(l.debit) as deb, SUM(l.credit) as cred
            FROM journal_entry_lines l
            JOIN journal_entries e ON l.entryId = e.id
            JOIN accounts a ON l.accountId = a.id
            WHERE e.status = 'POSTED' AND e.date >= $startTime AND e.date < $endTime
            AND a.type IN ('REVENUE', 'EXPENSE')
            GROUP BY l.accountId, a.type
        """.trimIndent()
        val cursor = db.openHelper.readableDatabase.query(queryStr)

        val closingLines = mutableListOf<JournalEntryLineEntity>()
        var netIncome = 0.0

        if (cursor.moveToFirst()) {
            do {
                val accId = cursor.getLong(0)
                val type = cursor.getString(1)
                val deb = cursor.getDouble(2)
                val cred = cursor.getDouble(3)

                if (type == "REVENUE") {
                    val balance = cred - deb
                    if (balance != 0.0) {
                        netIncome += balance
                        closingLines.add(
                            JournalEntryLineEntity(
                                entryId = 0,
                                accountId = accId,
                                debit = balance,
                                credit = 0.0,
                                description = "قيد إغلاق إيرادات للفترة $periodId"
                            )
                        )
                    }
                } else if (type == "EXPENSE") {
                    val balance = deb - cred
                    if (balance != 0.0) {
                        netIncome -= balance
                        closingLines.add(
                            JournalEntryLineEntity(
                                entryId = 0,
                                accountId = accId,
                                debit = 0.0,
                                credit = balance,
                                description = "قيد إغلاق مصروفات للفترة $periodId"
                            )
                        )
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (closingLines.isNotEmpty()) {
            if (netIncome > 0.0) {
                closingLines.add(
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = capitalAcc.id,
                        debit = 0.0,
                        credit = netIncome,
                        description = "ترحيل صافي الأرباح المحققة للفترة $periodId"
                    )
                )
            } else if (netIncome < 0.0) {
                closingLines.add(
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = capitalAcc.id,
                        debit = Math.abs(netIncome),
                        credit = 0.0,
                        description = "ترحيل صافي الخسائر المحققة للفترة $periodId"
                    )
                )
            }

            val closingNo = "CL-ENT-$periodId"
            val closingEntry = JournalEntryEntity(
                entryNumber = closingNo,
                date = endTime - 1000,
                description = "قيد الإقفال السنوي والدروي المالي للفترة $periodId",
                status = "POSTED",
                isClosing = true
            )
            journalDao.saveJournalEntry(closingEntry, closingLines)
        }

        com.example.core.di.ServiceContainer.getInstance().settingsManager.updateFiscalPeriodStatus(periodId, "CLOSED")
    }

    suspend fun deleteJournalEntry(id: Long) {
        val entryWithLines = journalDao.getEntryWithLinesById(id)
        if (entryWithLines != null && entryWithLines.entry.status == "POSTED") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف قيد تم ترحيله بالفعل إلى الدفتر العام!")
        }
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
    // -- Warehouse Management --
    suspend fun addWarehouse(warehouse: WarehouseEntity): Long {
        return productDao.insertWarehouse(warehouse)
    }

    suspend fun updateWarehouse(warehouse: WarehouseEntity) {
        productDao.updateWarehouse(warehouse)
    }

    suspend fun deleteWarehouse(warehouse: WarehouseEntity) {
        productDao.deleteWarehouse(warehouse)
    }

    suspend fun setDefaultWarehouse(warehouseId: Long) {
        productDao.clearDefaultWarehouses()
        val warehouse = productDao.getWarehouseById(warehouseId)
        if (warehouse != null) {
            productDao.updateWarehouse(warehouse.copy(isDefault = true))
        }
    }

    suspend fun transferStock(
        productId: Long,
        fromWarehouseId: Long,
        toWarehouseId: Long,
        quantity: Double,
        description: String,
        timestamp: Long
    ) {
        val product = productDao.getProductById(productId) ?: return
        if (quantity <= 0) return

        // Out movement from source
        productDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                warehouseId = fromWarehouseId,
                type = "TRANSFER_OUT",
                quantity = -quantity,
                unitCost = product.cost,
                date = timestamp,
                description = description
            )
        )
        // In movement to destination
        productDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                warehouseId = toWarehouseId,
                type = "TRANSFER_IN",
                quantity = quantity,
                unitCost = product.cost,
                date = timestamp,
                description = description
            )
        )
    }

    suspend fun recordStockAdjustment(
        productId: Long,
        warehouseId: Long?,
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
                warehouseId = warehouseId,
                type = mvType,
                quantity = qtyChange,
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

    private fun formatAmount(amount: Double): String {
        return String.format(java.util.Locale.US, "%,.2f", amount)
    }

    // --- Quotations Business Rules ---
    suspend fun createQuotation(customerId: Long, expiryDate: Long, items: List<Pair<Long, Pair<Double, Double>>>) {
        val count = db.openHelper.readableDatabase.compileStatement("SELECT COUNT(*) FROM sales_quotations").simpleQueryForLong()
        val qNo = "SQ-${System.currentTimeMillis() % 100000}-${count + 1}"
        
        var subtotal = 0.0
        val lineEntities = items.map { (productId, qtyAndPrice) ->
            val (qty, price) = qtyAndPrice
            subtotal += qty * price
            QuotationLineEntity(
                quotationId = 0,
                productId = productId,
                quantity = qty,
                price = price,
                discount = 0.0
            )
        }
        val vat = subtotal * 0.15
        val grandTotal = subtotal + vat

        val quotation = QuotationEntity(
            quotationNumber = qNo,
            customerId = customerId,
            date = System.currentTimeMillis(),
            expiryDate = expiryDate,
            status = "DRAFT",
            subtotal = subtotal,
            vat = vat,
            grandTotal = grandTotal
        )
        salesDao.saveQuotation(quotation, lineEntities)
    }

    suspend fun convertQuotationToInvoice(quotationId: Long, cashAccountId: Long?, isCredit: Boolean) {
        val qWithOwner = salesDao.getQuotationById(quotationId) 
            ?: throw com.example.core.errors.BusinessRuleError("عرض السعر المحدد غير موجود")
        
        if (qWithOwner.quotation.status == "CONVERTED") {
            throw com.example.core.errors.BusinessRuleError("عرض السعر تم تحويله مسبقاً بفواتير سارية")
        }

        val invoiceNo = "SQ-INV-${System.currentTimeMillis() % 100000}"
        
        val invoice = SalesInvoiceEntity(
            invoiceNumber = invoiceNo,
            customerId = qWithOwner.quotation.customerId,
            date = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (7 * 24 * 3600 * 1000), // 1 week due default
            status = "DRAFT",
            isCredit = isCredit,
            subtotal = qWithOwner.quotation.subtotal,
            vat = qWithOwner.quotation.vat,
            grandTotal = qWithOwner.quotation.grandTotal,
            paidAmount = 0.0,
            paymentAccountOrCashId = cashAccountId
        )

        val invoiceLines = qWithOwner.lines.map { qLine ->
            SalesInvoiceLineEntity(
                invoiceId = 0,
                productId = qLine.productId,
                quantity = qLine.quantity,
                price = qLine.price,
                discount = qLine.discount,
                returnedQuantity = 0.0
            )
        }

        salesDao.saveSalesInvoice(invoice, invoiceLines)
        salesDao.updateQuotation(qWithOwner.quotation.copy(status = "CONVERTED"))
    }

    // --- Sales Invoice Workflows ---
    suspend fun createSalesInvoiceDraft(
        customerId: Long,
        date: Long,
        dueDate: Long,
        isCredit: Boolean,
        paymentAccountId: Long?,
        lines: List<Pair<Long, Pair<Double, Double>>>
    ) {
        val count = db.openHelper.readableDatabase.compileStatement("SELECT COUNT(*) FROM sales_invoices").simpleQueryForLong()
        val invNo = "SI-${System.currentTimeMillis() % 100000}-${count + 1}"

        var subtotal = 0.0
        val lineEntities = lines.map { (productId, qtyAndPrice) ->
            val (qty, price) = qtyAndPrice
            subtotal += qty * price
            SalesInvoiceLineEntity(
                invoiceId = 0,
                productId = productId,
                quantity = qty,
                price = price,
                discount = 0.0,
                returnedQuantity = 0.0
            )
        }
        val vat = subtotal * 0.15
        val grandTotal = subtotal + vat

        val invoice = SalesInvoiceEntity(
            invoiceNumber = invNo,
            customerId = customerId,
            date = date,
            dueDate = dueDate,
            status = "DRAFT",
            isCredit = isCredit,
            subtotal = subtotal,
            vat = vat,
            grandTotal = grandTotal,
            paidAmount = 0.0,
            paymentAccountOrCashId = paymentAccountId
        )

        salesDao.saveSalesInvoice(invoice, lineEntities)
    }

    suspend fun deleteSalesInvoice(id: Long) {
        val invoiceWithOwner = salesDao.getInvoiceById(id) ?: return
        if (invoiceWithOwner.invoice.status != "DRAFT") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف فاتورة مبيعات مؤكدة أو مسددة")
        }
        salesDao.deleteInvoice(invoiceWithOwner.invoice)
    }

    suspend fun confirmSalesInvoice(invoiceId: Long, paymentAccountId: Long?) {
        val invoiceWithOwner = salesDao.getInvoiceById(invoiceId) 
            ?: throw com.example.core.errors.BusinessRuleError("الفاتورة غير موجودة")
        val invoice = invoiceWithOwner.invoice
        if (invoice.status == "POSTED") return

        val customer = partnerDao.getPartnerById(invoice.customerId)
            ?: throw com.example.core.errors.BusinessRuleError("العميل غير معرف بالنظام")

        // 1. Credit Limit Check for Credit Invoices
        if (invoice.isCredit) {
            val availableCredit = customer.creditLimit - customer.balance
            if (invoice.grandTotal > availableCredit) {
                throw com.example.core.errors.BusinessRuleError("تجاوز الحد الائتماني للعميل! الحد الائتماني المتاح: ${formatAmount(availableCredit)} ر.س، قيمة الفاتورة المعتمدة: ${formatAmount(invoice.grandTotal)} ر.س")
            }
        }

        // 2. Stock and Product checks
        for (line in invoiceWithOwner.lines) {
            val product = productDao.getProductById(line.productId)
                ?: throw com.example.core.errors.BusinessRuleError("المنتج غير موجود")
            if (product.type == "PRODUCT") {
                if (product.stock < line.quantity) {
                    throw com.example.core.errors.BusinessRuleError("الكمية غير متوفرة في المخزن للمنتج (${product.name}). المتوفر: ${product.stock}، المطلوب: ${line.quantity}")
                }
            }
        }

        // 3. Process operations: Stock decrease and movements
        for (line in invoiceWithOwner.lines) {
            val product = productDao.getProductById(line.productId) ?: continue
            if (product.type == "PRODUCT") {
                productDao.updateProduct(product.copy(stock = product.stock - line.quantity))
                productDao.insertStockMovement(
                    StockMovementEntity(
                        productId = product.id,
                        type = "SALE",
                        quantity = line.quantity,
                        unitCost = product.cost,
                        date = invoice.date,
                        description = "مخرج مبيعات للفاتورة رقم: ${invoice.invoiceNumber}"
                    )
                )
            }
        }

        // 4. Create Accounting double entry with 15% VAT
        val recAcc = accountDao.getAccountByCode("1201") ?: throw com.example.core.errors.BusinessRuleError("حساب الذمم المدينة (المستلمة) غير محدد")
        val revAcc = accountDao.getAccountByCode("4101") ?: throw com.example.core.errors.BusinessRuleError("حساب إيرادات المبيعات غير محدد")
        val vatAcc = accountDao.getAccountByCode("2201") ?: throw com.example.core.errors.BusinessRuleError("حساب ضريبة مخرجات غير محدد")
        
        val cogsAcc = accountDao.getAccountByCode("5101")
        val invAcc = accountDao.getAccountByCode("1301")

        val entryNo = "SI-CONF-${invoice.invoiceNumber}"
        val entry = JournalEntryEntity(
            entryNumber = entryNo,
            date = invoice.date,
            description = "مبيعات للعميل: ${customer.name} - فاتورة رقم: ${invoice.invoiceNumber}",
            status = "POSTED"
        )

        val debitAccountId = if (invoice.isCredit) recAcc.id else (paymentAccountId ?: throw com.example.core.errors.BusinessRuleError("يجب تحديد صندوق أو بنك لتحصيل المبيعات النقدية"))
        
        val journalLines = mutableListOf(
            JournalEntryLineEntity(entryId = 0, accountId = debitAccountId, debit = invoice.grandTotal, credit = 0.0, description = "إجمالي قيمة فاتورة مبيعات"),
            JournalEntryLineEntity(entryId = 0, accountId = revAcc.id, debit = 0.0, credit = invoice.subtotal, description = "صافي إيراد مبيعات مستقل")
        )

        if (invoice.vat > 0.0) {
            journalLines.add(
                JournalEntryLineEntity(entryId = 0, accountId = vatAcc.id, debit = 0.0, credit = invoice.vat, description = "ضريبة مخرجات (15٪)")
            )
        }

        // Auto calculate COGS from actual products
        var totalCogs = 0.0
        for (line in invoiceWithOwner.lines) {
            val product = productDao.getProductById(line.productId) ?: continue
            if (product.type == "PRODUCT") {
                totalCogs += line.quantity * product.cost
            }
        }

        if (totalCogs > 0.0 && cogsAcc != null && invAcc != null) {
            journalLines.add(JournalEntryLineEntity(entryId = 0, accountId = cogsAcc.id, debit = totalCogs, credit = 0.0, description = "تكلفة البضاعة المباعة"))
            journalLines.add(JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = 0.0, credit = totalCogs, description = "صرف مخزني تلقائي بموجب الفاتورة"))
        }

        // Save entry
        val jEntryId = journalDao.insertEntry(entry)
        val journalLinesWithId = journalLines.map { it.copy(entryId = jEntryId) }
        journalDao.insertLines(journalLinesWithId)

        // 5. Update customer balance if credit
        if (invoice.isCredit) {
            partnerDao.updatePartner(customer.copy(balance = customer.balance + invoice.grandTotal))
        }

        // 6. Update invoice status to POSTED
        salesDao.updateInvoice(
            invoice.copy(
                status = "POSTED",
                associatedJournalEntryId = jEntryId,
                paidAmount = if (!invoice.isCredit) invoice.grandTotal else 0.0
            )
        )
    }

    suspend fun recordInvoicePayment(invoiceId: Long, amount: Double, paymentAccountId: Long) {
        val invoiceWithOwner = salesDao.getInvoiceById(invoiceId)
            ?: throw com.example.core.errors.BusinessRuleError("الفاتورة غير موجودة")
        val invoice = invoiceWithOwner.invoice
        if (invoice.status != "POSTED") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن سداد قيمة فاتورة غير مؤكدة ومرحلة")
        }

        val remaining = invoice.grandTotal - invoice.paidAmount
        if (amount > remaining) {
            throw com.example.core.errors.BusinessRuleError("المبلغ المدخل لم يتبق منه سوى ${formatAmount(remaining)} ر.س")
        }

        val customer = partnerDao.getPartnerById(invoice.customerId)
            ?: throw com.example.core.errors.BusinessRuleError("العميل غير موجود")

        val recAcc = accountDao.getAccountByCode("1201") 
            ?: throw com.example.core.errors.BusinessRuleError("حساب الذمم المدينة غير محدد")

        val entryNo = "PV-REC-${System.currentTimeMillis() % 100000}"
        val entry = JournalEntryEntity(
            entryNumber = entryNo,
            date = System.currentTimeMillis(),
            description = "تحصيل دفعة مالية للعميل ${customer.name} تحت حساب الفاتورة ${invoice.invoiceNumber}",
            status = "POSTED"
        )

        val jLines = listOf(
            JournalEntryLineEntity(entryId = 0, accountId = paymentAccountId, debit = amount, credit = 0.0, description = "تحصيل نقدي بالصندوق أو البنك"),
            JournalEntryLineEntity(entryId = 0, accountId = recAcc.id, debit = 0.0, credit = amount, description = "تخفيض ذمم العميل المستحقة للذات")
        )

        val jEntryId = journalDao.insertEntry(entry)
        val jLinesWithId = jLines.map { it.copy(entryId = jEntryId) }
        journalDao.insertLines(jLinesWithId)

        partnerDao.updatePartner(customer.copy(balance = Math.max(0.0, customer.balance - amount)))
        salesDao.updateInvoice(invoice.copy(paidAmount = invoice.paidAmount + amount))
    }

    // --- Sales Returns ---
    suspend fun recordSalesReturn(invoiceId: Long, reason: String, itemsToReturn: List<Pair<Long, Double>>) {
        val invoiceWithOwner = salesDao.getInvoiceById(invoiceId)
            ?: throw com.example.core.errors.BusinessRuleError("الفاتورة غير موجودة")
        val invoice = invoiceWithOwner.invoice

        if (invoice.status != "POSTED" && invoice.status != "REFUNDED") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن إجراء مرتجع إلا على فاتورة مباعة ومرحلة بالقسم المالي")
        }

        val customer = partnerDao.getPartnerById(invoice.customerId)
            ?: throw com.example.core.errors.BusinessRuleError("العميل غير موجود")

        val returnNo = "SR-${System.currentTimeMillis() % 100000}"
        
        var subtotalReturned = 0.0
        var totalCogsReturned = 0.0

        val returnLines = mutableListOf<SalesReturnLineEntity>()
        val updatedInvoiceLines = invoiceWithOwner.lines.toMutableList()

        for ((productId, returnQty) in itemsToReturn) {
            if (returnQty <= 0) continue
            val invLine = updatedInvoiceLines.find { it.productId == productId }
                ?: throw com.example.core.errors.BusinessRuleError("المنتج المعاد غير مدرج بهذه الفاتورة")
            
            val maxAllowed = invLine.quantity - invLine.returnedQuantity
            if (returnQty > maxAllowed) {
                throw com.example.core.errors.BusinessRuleError("الكمية المرتجعة متجاوزة المبيعات الفعلية المتبقية. المسموح به: $maxAllowed")
            }

            subtotalReturned += returnQty * invLine.price
            val product = productDao.getProductById(productId)
            if (product != null && product.type == "PRODUCT") {
                totalCogsReturned += returnQty * product.cost
                // Adjust stock movement & stock in database
                productDao.updateProduct(product.copy(stock = product.stock + returnQty))
                productDao.insertStockMovement(
                    StockMovementEntity(
                        productId = productId,
                        type = "ADJUSTMENT_IN",
                        quantity = returnQty,
                        unitCost = product.cost,
                        date = System.currentTimeMillis(),
                        description = "بضاعة مرتجعة بالصنف بموجب مرتجع رقم $returnNo"
                    )
                )
            }

            returnLines.add(
                SalesReturnLineEntity(
                    returnId = 0,
                    productId = productId,
                    quantity = returnQty,
                    price = invLine.price
                )
            )

            // Update returnedQuantity for this invoice line
            salesDao.updateInvoiceLines(listOf(invLine.copy(returnedQuantity = invLine.returnedQuantity + returnQty)))
        }

        val vatReturned = subtotalReturned * 0.15
        val grandTotalReturned = subtotalReturned + vatReturned

        val salesReturn = SalesReturnEntity(
            returnNumber = returnNo,
            invoiceId = invoiceId,
            date = System.currentTimeMillis(),
            reason = reason,
            refundAmount = grandTotalReturned
        )

        // double accounts entries reverse
        val recAcc = accountDao.getAccountByCode("1201") ?: throw com.example.core.errors.BusinessRuleError("حساب ذمم العملاء غير محدد")
        val revAcc = accountDao.getAccountByCode("4101") ?: throw com.example.core.errors.BusinessRuleError("حساب إيراد مبيعات غير محدد")
        val vatAcc = accountDao.getAccountByCode("2201") ?: throw com.example.core.errors.BusinessRuleError("حساب ضريبة مخرجات غير محدد")
        val cogsAcc = accountDao.getAccountByCode("5101")
        val invAcc = accountDao.getAccountByCode("1301")

        val entry = JournalEntryEntity(
            entryNumber = "SR-JV-${returnNo}",
            date = System.currentTimeMillis(),
            description = "مرتجعات مبيعات بموجب الفاتورة ${invoice.invoiceNumber}. العميل ${customer.name}",
            status = "POSTED"
        )

        // Opposite entries
        val debitAccount = if (invoice.isCredit) recAcc.id else (invoice.paymentAccountOrCashId ?: recAcc.id)
        val journalLines = mutableListOf(
            JournalEntryLineEntity(entryId = 0, accountId = revAcc.id, debit = subtotalReturned, credit = 0.0, description = "رد قيمة صافي الإيراد المباع"),
            JournalEntryLineEntity(entryId = 0, accountId = debitAccount, debit = 0.0, credit = grandTotalReturned, description = "رد القيمة المستحقة للعميل أو السداد المالي")
        )

        if (vatReturned > 0.0) {
            journalLines.add(
                JournalEntryLineEntity(entryId = 0, accountId = vatAcc.id, debit = vatReturned, credit = 0.0, description = "تخفيض قيمة ضريبة المخرجات للذود")
            )
        }

        if (totalCogsReturned > 0.0 && cogsAcc != null && invAcc != null) {
            journalLines.add(JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = totalCogsReturned, credit = 0.0, description = "إعادة توريد بضاعة مرتجعة إلى المخازن"))
            journalLines.add(JournalEntryLineEntity(entryId = 0, accountId = cogsAcc.id, debit = 0.0, credit = totalCogsReturned, description = "تخفيض تكلفة COGS بالمرتجعات"))
        }

        val jEntryId = journalDao.insertEntry(entry)
        val journalLinesWithId = journalLines.map { it.copy(entryId = jEntryId) }
        journalDao.insertLines(journalLinesWithId)

        // Update salesReturn with actual journal entry
        val finalReturnWithJournal = salesReturn.copy(associatedJournalEntryId = jEntryId)
        salesDao.saveSalesReturn(finalReturnWithJournal, returnLines)

        // Adjust customer balance if it was credit
        if (invoice.isCredit) {
            partnerDao.updatePartner(customer.copy(balance = Math.max(0.0, customer.balance - grandTotalReturned)))
        }

        // Determine if fully or partially refunded
        val allLines = salesDao.getInvoiceById(invoiceId)?.lines ?: emptyList()
        val isFullRefund = allLines.all { it.quantity == it.returnedQuantity }
        if (isFullRefund) {
            salesDao.updateInvoice(invoice.copy(status = "REFUNDED"))
        }
    }

    // ==========================================
    // --- PART 6: PURCHASES MODULE WORKFLOWS ---
    // ==========================================

    suspend fun getSuppliersWithBalances(): List<PartnerEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        partnerDao.getAllPartners().first().filter { it.type == "SUPPLIER" }
    }

    suspend fun createPurchaseOrder(
        supplierId: Long,
        date: Long,
        lines: List<Pair<Long, Pair<Double, Double>>>
    ) {
        val count = db.openHelper.readableDatabase.compileStatement("SELECT COUNT(*) FROM purchase_orders").simpleQueryForLong()
        val poNo = "PO-${System.currentTimeMillis() % 100000}-${count + 1}"

        var subtotal = 0.0
        val lineEntities = lines.map { (productId, qtyAndPrice) ->
            val (qty, price) = qtyAndPrice
            subtotal += qty * price
            PurchaseOrderLineEntity(
                orderId = 0,
                productId = productId,
                quantity = qty,
                price = price,
                discount = 0.0
            )
        }
        val vat = subtotal * 0.15
        val grandTotal = subtotal + vat

        val order = PurchaseOrderEntity(
            orderNumber = poNo,
            supplierId = supplierId,
            date = date,
            status = "DRAFT",
            subtotal = subtotal,
            vat = vat,
            grandTotal = grandTotal
        )
        purchaseDao.savePurchaseOrder(order, lineEntities)
    }

    suspend fun deletePurchaseOrder(id: Long) {
        val oWithOwner = purchaseDao.getOrderById(id) ?: return
        if (oWithOwner.order.status != "DRAFT") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف أمر شراء غير مسودة")
        }
        purchaseDao.deleteOrder(oWithOwner.order)
    }

    suspend fun confirmPurchaseOrder(id: Long) {
        val oWithOwner = purchaseDao.getOrderById(id) ?: return
        if (oWithOwner.order.status != "DRAFT") return
        purchaseDao.updateOrder(oWithOwner.order.copy(status = "CONFIRMED"))
    }

    suspend fun convertOrderToInvoice(orderId: Long, paymentAccountId: Long?, isCredit: Boolean) {
        val oWithOwner = purchaseDao.getOrderById(orderId) 
            ?: throw com.example.core.errors.BusinessRuleError("أمر الشراء المحدد غير موجود")
        
        if (oWithOwner.order.status == "CONVERTED") {
            throw com.example.core.errors.BusinessRuleError("أمر الشراء تم تحويله مسبقاً بفواتير سارية")
        }

        val invoiceNo = "PO-INV-${System.currentTimeMillis() % 100000}"
        
        val invoice = PurchaseInvoiceEntity(
            invoiceNumber = invoiceNo,
            supplierId = oWithOwner.order.supplierId,
            date = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (7 * 24 * 3600 * 1000), // 1 week due default
            status = "DRAFT",
            isCredit = isCredit,
            subtotal = oWithOwner.order.subtotal,
            vat = oWithOwner.order.vat,
            grandTotal = oWithOwner.order.grandTotal,
            paidAmount = 0.0,
            paymentAccountOrCashId = paymentAccountId
        )

        val invoiceLines = oWithOwner.lines.map { oLine ->
            PurchaseInvoiceLineEntity(
                invoiceId = 0,
                productId = oLine.productId,
                quantity = oLine.quantity,
                price = oLine.price,
                discount = oLine.discount,
                landedCostShare = 0.0,
                returnedQuantity = 0.0
            )
        }

        purchaseDao.savePurchaseInvoice(invoice, invoiceLines)
        purchaseDao.updateOrder(oWithOwner.order.copy(status = "CONVERTED"))
    }

    suspend fun createPurchaseInvoiceDraft(
        supplierId: Long,
        date: Long,
        dueDate: Long,
        isCredit: Boolean,
        paymentAccountId: Long?,
        lines: List<Pair<Long, Pair<Double, Double>>>
    ) {
        val count = db.openHelper.readableDatabase.compileStatement("SELECT COUNT(*) FROM purchase_invoices").simpleQueryForLong()
        val invNo = "PI-${System.currentTimeMillis() % 100000}-${count + 1}"

        var subtotal = 0.0
        val lineEntities = lines.map { (productId, qtyAndPrice) ->
            val (qty, price) = qtyAndPrice
            subtotal += qty * price
            PurchaseInvoiceLineEntity(
                invoiceId = 0,
                productId = productId,
                quantity = qty,
                price = price,
                discount = 0.0,
                landedCostShare = 0.0,
                returnedQuantity = 0.0
            )
        }
        val vat = subtotal * 0.15
        val grandTotal = subtotal + vat

        val invoice = PurchaseInvoiceEntity(
            invoiceNumber = invNo,
            supplierId = supplierId,
            date = date,
            dueDate = dueDate,
            status = "DRAFT",
            isCredit = isCredit,
            subtotal = subtotal,
            vat = vat,
            grandTotal = grandTotal,
            paidAmount = 0.0,
            paymentAccountOrCashId = paymentAccountId
        )

        purchaseDao.savePurchaseInvoice(invoice, lineEntities)
    }

    suspend fun deletePurchaseInvoice(id: Long) {
        val invoiceWithOwner = purchaseDao.getInvoiceById(id) ?: return
        if (invoiceWithOwner.invoice.status != "DRAFT") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن حذف فاتورة مشتريات مؤكدة أو مسددة")
        }
        purchaseDao.deleteInvoice(invoiceWithOwner.invoice)
    }

    suspend fun confirmPurchaseInvoice(
        invoiceId: Long,
        paymentAccountId: Long?,
        landedCosts: Double = 0.0,
        landedAllocationMethod: String = "NONE"
    ) {
        val invoiceWithOwner = purchaseDao.getInvoiceById(invoiceId) 
            ?: throw com.example.core.errors.BusinessRuleError("الفاتورة غير موجودة")
        val invoice = invoiceWithOwner.invoice
        if (invoice.status == "POSTED") return

        val supplier = partnerDao.getPartnerById(invoice.supplierId)
            ?: throw com.example.core.errors.BusinessRuleError("المورد غير معرف في النظام")

        // 1. Allocate landed costs if any
        val lines = invoiceWithOwner.lines
        var updatedLines = lines
        if (landedCosts > 0.0 && landedAllocationMethod != "NONE") {
            val totalQty = lines.sumOf { it.quantity }
            val totalVal = lines.sumOf { it.quantity * it.price }

            updatedLines = lines.map { line ->
                val share = when (landedAllocationMethod) {
                    "VALUE" -> if (totalVal > 0.0) ((line.quantity * line.price) / totalVal) * landedCosts else 0.0
                    "QUANTITY" -> if (totalQty > 0.0) (line.quantity / totalQty) * landedCosts else 0.0
                    else -> 0.0
                }
                line.copy(landedCostShare = share)
            }
        }

        // 2. Process stock insertion and update cost (WAC - Weighted Average Cost)
        for (line in updatedLines) {
            val product = productDao.getProductById(line.productId)
                ?: throw com.example.core.errors.BusinessRuleError("المنتج غير موجود")
            
            if (product.type == "PRODUCT") {
                val lineUnitLanded = if (line.quantity > 0.0) line.landedCostShare / line.quantity else 0.0
                val unitActualCost = line.price + lineUnitLanded

                val oldStock = product.stock
                val oldCost = product.cost
                val newStock = oldStock + line.quantity
                val newCost = if (newStock > 0.0) {
                    ((oldStock * oldCost) + (line.quantity * unitActualCost)) / newStock
                } else {
                    unitActualCost
                }

                productDao.updateProduct(
                    product.copy(
                        stock = newStock,
                        cost = newCost
                    )
                )

                productDao.insertStockMovement(
                    StockMovementEntity(
                        productId = product.id,
                        type = "PURCHASE",
                        quantity = line.quantity,
                        unitCost = unitActualCost,
                        date = invoice.date,
                        description = "وارد مشتريات للفاتورة رقم: ${invoice.invoiceNumber}"
                    )
                )
            }
        }

        // 3. Financial Bookkeeping
        val invAcc = accountDao.getAccountByCode("1301") ?: throw com.example.core.errors.BusinessRuleError("حساب المخزون السلعي (1301) غير محدد")
        val vatInputAcc = accountDao.getAccountByCode("1401") ?: throw com.example.core.errors.BusinessRuleError("حساب ضريبة مدخلات مشتريات (1401) غير محدد")
        val apAcc = accountDao.getAccountByCode("2101") ?: throw com.example.core.errors.BusinessRuleError("حساب الذمم الدائنة الموردين (2101) غير محدد")

        val entryNo = "PI-CONF-${invoice.invoiceNumber}"
        val entry = JournalEntryEntity(
            entryNumber = entryNo,
            date = invoice.date,
            description = "مشتريات من المورد: ${supplier.name} - فاتورة رقم: ${invoice.invoiceNumber}",
            status = "POSTED"
        )

        // Debit inventory for the net subtotal + landed costs
        val journalLines = mutableListOf(
            JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = invoice.subtotal + landedCosts, credit = 0.0, description = "توريد مخزني للمشتريات ومصاريف الشحن الموزعة"),
            JournalEntryLineEntity(entryId = 0, accountId = vatInputAcc.id, debit = invoice.vat, credit = 0.0, description = "ضريبة القيمة المضافة مدخلات (15٪)")
        )

        if (invoice.isCredit) {
            // Credit AP (Accounts Payable)
            journalLines.add(
                JournalEntryLineEntity(entryId = 0, accountId = apAcc.id, debit = 0.0, credit = invoice.grandTotal, description = "استحقاق للمورد شامل الضريبة")
            )
            // Credit shipping payer (Cash/Bank) for landed costs
            if (landedCosts > 0.0) {
                val shippingCreditAcc = paymentAccountId ?: apAcc.id
                journalLines.add(
                    JournalEntryLineEntity(entryId = 0, accountId = shippingCreditAcc, debit = 0.0, credit = landedCosts, description = "سداد تكاليف إضافية (شحن/جمارك) للفاتورة")
                )
            }
        } else {
            // Credit Cash or Bank
            val creditAcc = paymentAccountId ?: throw com.example.core.errors.BusinessRuleError("يجب تحديد صندوق أو بنك لسداد المشتريات النقدية")
            journalLines.add(
                JournalEntryLineEntity(entryId = 0, accountId = creditAcc, debit = 0.0, credit = invoice.grandTotal + landedCosts, description = "سداد قيمة مشتريات نقدية ومصاريف شحن")
            )
        }

        val jEntryId = journalDao.insertEntry(entry)
        val journalLinesWithId = journalLines.map { it.copy(entryId = jEntryId) }
        journalDao.insertLines(journalLinesWithId)

        // 4. Update supplier balance if Credit
        if (invoice.isCredit) {
            partnerDao.updatePartner(supplier.copy(balance = supplier.balance + invoice.grandTotal))
        }

        // 5. Save invoice database record updates
        purchaseDao.updateInvoice(
            invoice.copy(
                status = "POSTED",
                associatedJournalEntryId = jEntryId,
                landedCostsAllocated = landedCosts,
                landedAllocationMethod = landedAllocationMethod
            )
        )
        purchaseDao.updateInvoiceLines(updatedLines)
    }

    suspend fun recordSupplierPayment(
        invoiceId: Long,
        amount: Double,
        payAccountId: Long
    ) {
        val invoiceWithOwner = purchaseDao.getInvoiceById(invoiceId)
            ?: throw com.example.core.errors.BusinessRuleError("الفاتورة غير موجودة")
        val invoice = invoiceWithOwner.invoice

        if (invoice.status != "POSTED") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن سداد قيمة فاتورة غير مؤكدة ومرحلة")
        }

        val remaining = invoice.grandTotal - invoice.paidAmount
        if (amount > remaining + 0.01) {
            throw com.example.core.errors.BusinessRuleError("المبلغ المدخل متجاوز المتبقي. المسموح به: ${remaining} ر.س")
        }

        val supplier = partnerDao.getPartnerById(invoice.supplierId)
            ?: throw com.example.core.errors.BusinessRuleError("المورد غير موجود")

        val apAcc = accountDao.getAccountByCode("2101") ?: throw com.example.core.errors.BusinessRuleError("حساب الذمم الدائنة الموردين غير محدد")

        val pNo = "PI-PAY-${System.currentTimeMillis() % 100000}"
        val entry = JournalEntryEntity(
            entryNumber = pNo,
            date = System.currentTimeMillis(),
            description = "سداد دفعة للمورد ${supplier.name} بموجب فاتورة المشتريات رقم ${invoice.invoiceNumber}",
            status = "POSTED"
        )

        val jLines = listOf(
            JournalEntryLineEntity(entryId = 0, accountId = apAcc.id, debit = amount, credit = 0.0, description = "تخفيض الذمم الدائنة للمورد"),
            JournalEntryLineEntity(entryId = 0, accountId = payAccountId, debit = 0.0, credit = amount, description = "سداد نقدي من البنك/الصندوق للمورد")
        )

        val jEntryId = journalDao.insertEntry(entry)
        val jLinesWithId = jLines.map { it.copy(entryId = jEntryId) }
        journalDao.insertLines(jLinesWithId)

        partnerDao.updatePartner(supplier.copy(balance = Math.max(0.0, supplier.balance - amount)))
        purchaseDao.updateInvoice(invoice.copy(paidAmount = invoice.paidAmount + amount))
    }

    suspend fun createPurchaseReturn(
        invoiceId: Long,
        reason: String,
        itemsToReturn: List<Pair<Long, Double>>
    ) {
        val invoiceWithOwner = purchaseDao.getInvoiceById(invoiceId)
            ?: throw com.example.core.errors.BusinessRuleError("الفاتورة غير موجودة")
        val invoice = invoiceWithOwner.invoice

        if (invoice.status != "POSTED") {
            throw com.example.core.errors.BusinessRuleError("لا يمكن إجراء مرتجع إلا على فاتورة مشتراة ومؤكدة محاسبياً")
        }

        val supplier = partnerDao.getPartnerById(invoice.supplierId)
            ?: throw com.example.core.errors.BusinessRuleError("المورد غير موجود")

        val returnNo = "PR-RET-${System.currentTimeMillis() % 100000}"
        var subtotalReturned = 0.0
        val returnLines = mutableListOf<PurchaseReturnLineEntity>()

        for ((productId, returnQty) in itemsToReturn) {
            val invLine = invoiceWithOwner.lines.find { it.productId == productId }
                ?: throw com.example.core.errors.BusinessRuleError("الصنف المعاد غير مدرج بهذه الفاتورة")
            
            val maxAllowed = invLine.quantity - invLine.returnedQuantity
            if (returnQty > maxAllowed) {
                throw com.example.core.errors.BusinessRuleError("الكمية المرتجعة متجاوزة المشتريات الفعلية المتبقية. المسموح به: $maxAllowed")
            }

            val product = productDao.getProductById(productId)
                ?: throw com.example.core.errors.BusinessRuleError("المنتج غير موجود")

            subtotalReturned += returnQty * invLine.price

            // Adjust inventory count (reduce stock on purchase return)
            if (product.type == "PRODUCT") {
                val newStock = Math.max(0.0, product.stock - returnQty)
                productDao.updateProduct(product.copy(stock = newStock))
                productDao.insertStockMovement(
                    StockMovementEntity(
                        productId = product.id,
                        type = "PURCHASE_RETURN",
                        quantity = returnQty,
                        unitCost = product.cost,
                        date = System.currentTimeMillis(),
                        description = "بضاعة مرتجعة للمورد بموجب مرتجع رقم $returnNo"
                    )
                )
            }

            returnLines.add(
                PurchaseReturnLineEntity(
                    returnId = 0,
                    productId = productId,
                    quantity = returnQty,
                    price = invLine.price
                )
            )

            purchaseDao.updateInvoiceLines(listOf(invLine.copy(returnedQuantity = invLine.returnedQuantity + returnQty)))
        }

        val vatReturned = subtotalReturned * 0.15
        val grandTotalReturned = subtotalReturned + vatReturned

        val purchaseReturn = PurchaseReturnEntity(
            returnNumber = returnNo,
            invoiceId = invoiceId,
            date = System.currentTimeMillis(),
            reason = reason,
            refundAmount = grandTotalReturned
        )

        // 1. Double entries reverse
        val apAcc = accountDao.getAccountByCode("2101") ?: throw com.example.core.errors.BusinessRuleError("حساب ذمم الموردين غير حدد")
        val invAcc = accountDao.getAccountByCode("1301") ?: throw com.example.core.errors.BusinessRuleError("حساب المخزون السلعي غير حدد")
        val vatInputAcc = accountDao.getAccountByCode("1401") ?: throw com.example.core.errors.BusinessRuleError("حساب الضريبة غير حدد")

        val entry = JournalEntryEntity(
            entryNumber = "PR-JV-$returnNo",
            date = System.currentTimeMillis(),
            description = "مرتجع مشتريات بموجب الفاتورة رقم ${invoice.invoiceNumber}. المورد ${supplier.name}",
            status = "POSTED"
        )

        val debitAccount = if (invoice.isCredit) apAcc.id else (invoice.paymentAccountOrCashId ?: apAcc.id)
        val journalLines = mutableListOf(
            JournalEntryLineEntity(entryId = 0, accountId = debitAccount, debit = grandTotalReturned, credit = 0.0, description = "تخفيض مستحقات المورد بالقيمة المرتجعة"),
            JournalEntryLineEntity(entryId = 0, accountId = invAcc.id, debit = 0.0, credit = subtotalReturned, description = "صرف بضاعة من المخازن بموجب مرتجع المشتريات"),
            JournalEntryLineEntity(entryId = 0, accountId = vatInputAcc.id, debit = 0.0, credit = vatReturned, description = "عكس/تخفيض ضريبة القيمة المضافة لمدخلات المرتجع")
        )

        val jEntryId = journalDao.insertEntry(entry)
        val journalLinesWithId = journalLines.map { it.copy(entryId = jEntryId) }
        journalDao.insertLines(journalLinesWithId)

        val finalReturnWithJournal = purchaseReturn.copy(associatedJournalEntryId = jEntryId)
        purchaseDao.savePurchaseReturn(finalReturnWithJournal, returnLines)

        // 2. Adjust supplier balance
        if (invoice.isCredit) {
            partnerDao.updatePartner(supplier.copy(balance = Math.max(0.0, supplier.balance - grandTotalReturned)))
        }

        // Determine if fully or partially refunded
        val allLines = purchaseDao.getInvoiceById(invoiceId)?.lines ?: emptyList()
        val isFullRefund = allLines.all { it.quantity == it.returnedQuantity }
        if (isFullRefund) {
            purchaseDao.updateInvoice(invoice.copy(status = "REFUNDED"))
        }
    }

    suspend fun getSupplierStatement(supplierId: Long, fromDate: Long, toDate: Long): List<SupplierStatementTx> {
        val invoices = purchaseDao.getAllInvoices().first().filter { it.invoice.supplierId == supplierId && it.invoice.status == "POSTED" }
        val returns = purchaseDao.getAllReturns().first().filter { 
            val inv = purchaseDao.getInvoiceById(it.returnEntity.invoiceId)?.invoice
            inv?.supplierId == supplierId
        }

        val txs = mutableListOf<SupplierStatementTx>()
        for (invWithOwner in invoices) {
            val inv = invWithOwner.invoice
            if (inv.date in fromDate..toDate) {
                txs.add(
                    SupplierStatementTx(
                        date = inv.date,
                        type = "فاتورة مشتريات",
                        reference = inv.invoiceNumber,
                        debit = 0.0,
                        credit = inv.grandTotal
                    )
                )
                if (inv.paidAmount > 0.0) {
                    txs.add(
                        SupplierStatementTx(
                            date = inv.date + 1000, 
                            type = "سداد دفعة",
                            reference = "PAY-${inv.invoiceNumber}",
                            debit = inv.paidAmount,
                            credit = 0.0
                        )
                    )
                }
            }
        }

        for (retWithOwner in returns) {
            val ret = retWithOwner.returnEntity
            if (ret.date in fromDate..toDate) {
                txs.add(
                    SupplierStatementTx(
                        date = ret.date,
                        type = "مرتجع مشتريات",
                        reference = ret.returnNumber,
                        debit = ret.refundAmount,
                        credit = 0.0
                    )
                )
            }
        }

        txs.sortBy { it.date }
        var bal = 0.0
        return txs.map { 
            bal = bal + it.credit - it.debit
            it.copy(balanceAfter = bal)
        }
    }
}

data class SupplierStatementTx(
    val date: Long,
    val type: String,
    val reference: String,
    val debit: Double,
    val credit: Double,
    var balanceAfter: Double = 0.0
)

