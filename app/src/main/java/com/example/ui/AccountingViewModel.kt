package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val container = com.example.core.di.ServiceContainer.getInstance()
    private val db = container.database
    private val repository = container.repository

    // --- State Streams from Database ---
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading accounts", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryWithOwner>> = repository.allEntries
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading entries", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading products", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouses: StateFlow<List<WarehouseEntity>> = repository.allWarehouses
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading warehouses", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.allMovements
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading stock movements", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partners: StateFlow<List<PartnerEntity>> = repository.allPartners
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading partners", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading employees", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payrolls: StateFlow<List<PayrollRecordEntity>> = repository.allPayrolls
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading payrolls", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    val quotations: StateFlow<List<SalesQuotationWithOwner>> = repository.allQuotations
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading quotations", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesInvoices: StateFlow<List<SalesInvoiceWithOwner>> = repository.allInvoices
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading sales invoices", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesReturns: StateFlow<List<SalesReturnWithOwner>> = repository.allReturns
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading sales returns", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search/Filter states ---
    private val _selectedLedgerAccountId = MutableStateFlow<Long?>(null)
    val selectedLedgerAccountId: StateFlow<Long?> = _selectedLedgerAccountId.asStateFlow()

    // --- Settings flows using SettingsManager ---
    private val settingsManager = container.settingsManager

    private val _companyProfile = MutableStateFlow(settingsManager.getCompanyProfile())
    val companyProfile: StateFlow<com.example.core.settings.CompanyProfile> = _companyProfile.asStateFlow()

    private val _fiscalPeriods = MutableStateFlow(settingsManager.getFiscalPeriods())
    val fiscalPeriods: StateFlow<List<com.example.core.settings.FiscalPeriod>> = _fiscalPeriods.asStateFlow()

    private val _numberSequences = MutableStateFlow(settingsManager.getNumberSequences())
    val numberSequences: StateFlow<List<com.example.core.settings.NumberSequence>> = _numberSequences.asStateFlow()

    private val _unitsOfMeasure = MutableStateFlow(settingsManager.getUnitsOfMeasure())
    val unitsOfMeasure: StateFlow<List<com.example.core.settings.UnitOfMeasure>> = _unitsOfMeasure.asStateFlow()

    private val _costCenters = MutableStateFlow(settingsManager.getCostCenters())
    val costCenters: StateFlow<List<com.example.core.settings.CostCenter>> = _costCenters.asStateFlow()

    private val _notificationToggles = MutableStateFlow(settingsManager.getNotificationToggles())
    val notificationToggles: StateFlow<Map<String, Boolean>> = _notificationToggles.asStateFlow()

    private val _auditLogs = MutableStateFlow(settingsManager.getAuditLogs())
    val auditLogs: StateFlow<List<com.example.core.settings.AuditLog>> = _auditLogs.asStateFlow()

    private val _syncSummary = MutableStateFlow(settingsManager.getSyncSummary())
    val syncSummary: StateFlow<com.example.core.settings.SettingsManager.SyncSummary> = _syncSummary.asStateFlow()

    fun updateCompanyProfile(profile: com.example.core.settings.CompanyProfile) {
        settingsManager.saveCompanyProfile(profile)
        _companyProfile.value = settingsManager.getCompanyProfile()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun updateFiscalPeriodStatus(id: String, newStatus: String) {
        settingsManager.updateFiscalPeriodStatus(id, newStatus)
        _fiscalPeriods.value = settingsManager.getFiscalPeriods()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun setLinkedAccountCode(key: String, code: String) {
        settingsManager.setLinkedAccountCode(key, code)
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun resetLinkedAccountsToDefaults() {
        settingsManager.resetToDefaults()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun updateSequence(type: String, newPrefix: String, newCurrent: Int) {
        settingsManager.updateSequence(type, newPrefix, newCurrent)
        _numberSequences.value = settingsManager.getNumberSequences()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun addUnitOfMeasure(name: String, symbol: String, baseUnitId: String?, factor: Double) {
        settingsManager.addUnitOfMeasure(name, symbol, baseUnitId, factor)
        _unitsOfMeasure.value = settingsManager.getUnitsOfMeasure()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun deleteUnitOfMeasure(id: String) {
        settingsManager.deleteUnitOfMeasure(id)
        _unitsOfMeasure.value = settingsManager.getUnitsOfMeasure()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun addCostCenter(code: String, name: String, parentId: String?) {
        settingsManager.addCostCenter(code, name, parentId)
        _costCenters.value = settingsManager.getCostCenters()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun deleteCostCenter(id: String) {
        settingsManager.deleteCostCenter(id)
        _costCenters.value = settingsManager.getCostCenters()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun updateNotificationToggle(key: String, enabled: Boolean) {
        settingsManager.updateNotificationToggle(key, enabled)
        _notificationToggles.value = settingsManager.getNotificationToggles()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun clearAuditLogs() {
        settingsManager.clearAuditLogs()
        _auditLogs.value = settingsManager.getAuditLogs()
    }

    fun forceSync(context: android.content.Context, onSyncFinished: () -> Unit = {}) {
        settingsManager.forceSync(context) {
            _syncSummary.value = settingsManager.getSyncSummary()
            _auditLogs.value = settingsManager.getAuditLogs()
            onSyncFinished()
        }
    }

    init {
        // Seed default accounts initially if database is fresh
        seedDefaultAccountsIfNeeded()
    }

    private fun seedDefaultAccountsIfNeeded() {
        viewModelScope.launch {
            repository.checkAndSeed()
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun handleException(t: Throwable) {
        android.util.Log.e("AccountingViewModel", "Operation error captured", t)
        _errorMessage.value = t.message ?: "حدث خطأ غير متوقع أثناء المعالجة"
    }

    fun selectLedgerAccount(id: Long?) {
        _selectedLedgerAccountId.value = id
    }

    // --- Account operations ---
    fun addAccount(code: String, nameAr: String, nameEn: String, type: String, parentId: Long?, allowPosting: Boolean) {
        viewModelScope.launch {
            try {
                repository.addAccount(
                    AccountEntity(
                        code = code,
                        nameAr = nameAr,
                        nameEn = nameEn,
                        type = type,
                        parentId = parentId,
                        allowPosting = allowPosting
                    )
                )
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(account)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Journal Entry operations ---
    fun addManualJournalEntry(description: String, date: Long, entryNo: String, draftLines: List<Pair<Long, Pair<Double, Double>>>) {
        viewModelScope.launch {
            try {
                val entryObj = JournalEntryEntity(
                    entryNumber = entryNo,
                    date = date,
                    description = description,
                    status = "DRAFT"
                )
                val linesObj = draftLines.map { (accId, debCred) ->
                    JournalEntryLineEntity(
                        entryId = 0,
                        accountId = accId,
                        debit = debCred.first,
                        credit = debCred.second,
                        description = description
                    )
                }
                repository.saveJournalEntry(entryObj, linesObj)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun postJournalEntry(id: Long) {
        viewModelScope.launch {
            try {
                repository.postJournalEntry(id)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun reverseJournalEntry(id: Long) {
        viewModelScope.launch {
            try {
                repository.reverseJournalEntry(id)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun closePeriod(periodId: String) {
        viewModelScope.launch {
            try {
                repository.closePeriod(periodId)
                // update local state of fiscal period list as it is stored in SharedPreferences
                _fiscalPeriods.value = settingsManager.getFiscalPeriods()
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteJournalEntry(id)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Partner operations ---
    fun addPartner(name: String, type: String, phone: String, email: String, limit: Double) {
        viewModelScope.launch {
            try {
                repository.addPartner(PartnerEntity(name = name, type = type, phone = phone, email = email, creditLimit = limit))
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Product operations ---
    fun addProduct(code: String, name: String, price: Double, cost: Double, minStock: Double, type: String) {
        viewModelScope.launch {
            try {
                repository.addProduct(ProductEntity(code = code, name = name, price = price, cost = cost, minStock = minStock, type = type))
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Inventory adjustments ---
    fun adjustStock(productId: Long, warehouseId: Long?, qtyChange: Double, description: String) {
        viewModelScope.launch {
            try {
                repository.recordStockAdjustment(productId, warehouseId, qtyChange, description, System.currentTimeMillis())
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // -- Warehouse Management --
    fun addWarehouse(name: String, code: String, location: String, manager: String, isDefault: Boolean) {
        viewModelScope.launch {
            try {
                val wh = WarehouseEntity(name = name, code = code, location = location, manager = manager, isDefault = isDefault)
                val id = repository.addWarehouse(wh)
                if (isDefault) {
                    repository.setDefaultWarehouse(id)
                }
            } catch (t: Throwable) { handleException(t) }
        }
    }

    fun updateWarehouse(warehouse: WarehouseEntity) {
        viewModelScope.launch {
            try {
                repository.updateWarehouse(warehouse)
                if (warehouse.isDefault) {
                    repository.setDefaultWarehouse(warehouse.id)
                }
            } catch (t: Throwable) { handleException(t) }
        }
    }

    fun deleteWarehouse(warehouse: WarehouseEntity) {
        viewModelScope.launch {
            try { repository.deleteWarehouse(warehouse) } catch (t: Throwable) { handleException(t) }
        }
    }

    fun setDefaultWarehouse(warehouseId: Long) {
        viewModelScope.launch {
            try { repository.setDefaultWarehouse(warehouseId) } catch (t: Throwable) { handleException(t) }
        }
    }

    fun transferStock(productId: Long, fromWarehouseId: Long, toWarehouseId: Long, quantity: Double, description: String) {
        viewModelScope.launch {
            try {
                repository.transferStock(productId, fromWarehouseId, toWarehouseId, quantity, description, System.currentTimeMillis())
            } catch (t: Throwable) { handleException(t) }
        }
    }

    // --- Invoicing integrations with automatic double entries ---
    fun recordSaleInvoice(customerId: Long, productId: Long, quantity: Double, price: Double, cashAccountId: Long) {
        viewModelScope.launch {
            try {
                repository.addSalesInvoice(customerId, productId, quantity, price, cashAccountId, System.currentTimeMillis())
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun recordPurchaseInvoice(supplierId: Long, productId: Long, quantity: Double, cost: Double, paymentAccountId: Long) {
        viewModelScope.launch {
            try {
                repository.addPurchaseInvoice(supplierId, productId, quantity, cost, paymentAccountId, System.currentTimeMillis())
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Quotations (عروض الأسعار) Operations ---
    fun createQuotation(customerId: Long, expiryDate: Long, items: List<Pair<Long, Pair<Double, Double>>>) {
        viewModelScope.launch {
            try {
                repository.createQuotation(customerId, expiryDate, items)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun convertQuotationToInvoice(quotationId: Long, cashAccountId: Long?, isCredit: Boolean) {
        viewModelScope.launch {
            try {
                repository.convertQuotationToInvoice(quotationId, cashAccountId, isCredit)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Enhanced Sales Invoicing Operations ---
    fun createSalesInvoiceDraft(
        customerId: Long,
        date: Long,
        dueDate: Long,
        isCredit: Boolean,
        paymentAccountId: Long?,
        lines: List<Pair<Long, Pair<Double, Double>>>
    ) {
        viewModelScope.launch {
            try {
                repository.createSalesInvoiceDraft(customerId, date, dueDate, isCredit, paymentAccountId, lines)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun confirmSalesInvoice(invoiceId: Long, paymentAccountId: Long?) {
        viewModelScope.launch {
            try {
                repository.confirmSalesInvoice(invoiceId, paymentAccountId)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun deleteSalesInvoice(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSalesInvoice(id)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun recordInvoicePayment(invoiceId: Long, amount: Double, paymentAccountId: Long) {
        viewModelScope.launch {
            try {
                repository.recordInvoicePayment(invoiceId, amount, paymentAccountId)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Sales Return Operations ---
    fun recordSalesReturn(invoiceId: Long, reason: String, itemsToReturn: List<Pair<Long, Double>>) {
        viewModelScope.launch {
            try {
                repository.recordSalesReturn(invoiceId, reason, itemsToReturn)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // --- Employee & payroll ---
    fun addEmployee(code: String, name: String, department: String, phone: String, salary: Double) {
        viewModelScope.launch {
            try {
                repository.addEmployee(EmployeeEntity(code = code, name = name, department = department, phone = phone, basicSalary = salary))
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun generatePayroll(month: String, allowances: Double, deductions: Double) {
        viewModelScope.launch {
            try {
                repository.generatePayroll(month, allowances, deductions)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    fun payPayrollSalary(record: PayrollRecordEntity, cashAccountId: Long) {
        viewModelScope.launch {
            try {
                repository.processPayrollPayment(record, System.currentTimeMillis(), cashAccountId)
            } catch (t: Throwable) {
                handleException(t)
            }
        }
    }

    // ==========================================
    // --- ENGINE: real-time Financial Analytics ---
    // ==========================================

    // Dynamic aggregated trial balance rows
    val trialBalance: StateFlow<TrialBalanceData> = combine(accounts, journalEntries) { accountList, entryList ->
        // Accumulate Debits / Credits for each account ID from all POSTED entries
        val debitMap = mutableMapOf<Long, Double>()
        val creditMap = mutableMapOf<Long, Double>()

        entryList.filter { it.entry.status == "POSTED" }.forEach { entryWithOwner ->
            entryWithOwner.lines.forEach { line ->
                debitMap[line.accountId] = (debitMap[line.accountId] ?: 0.0) + line.debit
                creditMap[line.accountId] = (creditMap[line.accountId] ?: 0.0) + line.credit
            }
        }

        val rows = accountList.map { acc ->
            val totalDebit = debitMap[acc.id] ?: 0.0
            val totalCredit = creditMap[acc.id] ?: 0.0

            // Normal balances: Assets & Expenses are naturally Debit. Liabilities, Equity & Revenues naturally Credit.
            val balance = when (acc.type) {
                "ASSETS", "EXPENSE" -> totalDebit - totalCredit
                else -> totalCredit - totalDebit
            }

            TrialBalanceRow(
                account = acc,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = balance
            )
        }

        val totalDebitSum = rows.sumOf { it.totalDebit }
        val totalCreditSum = rows.sumOf { it.totalCredit }

        TrialBalanceData(rows = rows, totalDebit = totalDebitSum, totalCredit = totalCreditSum)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrialBalanceData())

    // Ledger entries of currently selected account
    val accountLedger: StateFlow<List<LedgerTransaction>> = combine(selectedLedgerAccountId, journalEntries, accounts) { accountId, entryList, accountList ->
        if (accountId == null) return@combine emptyList<LedgerTransaction>()

        val selectedAccount = accountList.find { it.id == accountId } ?: return@combine emptyList()
        val isDebitBalanced = selectedAccount.type == "ASSETS" || selectedAccount.type == "EXPENSE"

        val transactions = mutableListOf<LedgerTransaction>()
        var runningBalance = 0.0

        // Filter and sort posted entries chronologically
        val postedEntries = entryList.filter { it.entry.status == "POSTED" }.sortedBy { it.entry.date }

        postedEntries.forEach { entryWithOwner ->
            entryWithOwner.lines.filter { it.accountId == accountId }.forEach { line ->
                if (isDebitBalanced) {
                    runningBalance += (line.debit - line.credit)
                } else {
                    runningBalance += (line.credit - line.debit)
                }

                transactions.add(
                    LedgerTransaction(
                        entryNumber = entryWithOwner.entry.entryNumber,
                        date = entryWithOwner.entry.date,
                        description = entryWithOwner.entry.description,
                        debit = line.debit,
                        credit = line.credit,
                        balanceAfter = runningBalance
                    )
                )
            }
        }
        transactions
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Income Statement: Revenues vs Expenses
    val incomeStatement: StateFlow<IncomeStatementData> = trialBalance.map { tbData ->
        val revenueRows = tbData.rows.filter { it.account.type == "REVENUE" && it.account.allowPosting }
        val expenseRows = tbData.rows.filter { it.account.type == "EXPENSE" && it.account.allowPosting }

        val totalRevenue = revenueRows.sumOf { it.netBalance }
        val totalExpense = expenseRows.sumOf { it.netBalance }
        val netIncome = totalRevenue - totalExpense

        IncomeStatementData(
            revenueAccounts = revenueRows,
            expenseAccounts = expenseRows,
            totalRevenue = totalRevenue,
            totalExpense = totalExpense,
            netIncome = netIncome
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncomeStatementData())

    // Balance Sheet: Assets = Liabilities + Equity + retained earnings (netIncome)
    val balanceSheet: StateFlow<BalanceSheetData> = combine(trialBalance, incomeStatement) { tbData, statementData ->
        val assetRows = tbData.rows.filter { it.account.type == "ASSETS" && it.account.allowPosting }
        val liabilityRows = tbData.rows.filter { it.account.type == "LIABILITIES" && it.account.allowPosting }
        val equityRows = tbData.rows.filter { it.account.type == "EQUITY" && it.account.allowPosting }

        val totalAssets = assetRows.sumOf { it.netBalance }
        val totalLiabilities = liabilityRows.sumOf { it.netBalance }
        val totalEquity = equityRows.sumOf { it.netBalance } + statementData.netIncome

        BalanceSheetData(
            assetAccounts = assetRows,
            liabilityAccounts = liabilityRows,
            equityAccounts = equityRows,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            totalEquity = totalEquity
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BalanceSheetData())

    private val _aiAnalysisResult = MutableStateFlow<String?>(null)
    val aiAnalysisResult: StateFlow<String?> = _aiAnalysisResult.asStateFlow()

    private val geminiService = GeminiService()

    fun analyzeFinancials() {
        viewModelScope.launch {
            _aiAnalysisResult.value = "جاري تحليل البيانات عبر Gemini... يرجى الانتظار"
            val incomeData = incomeStatement.value
            val tbData = trialBalance.value.rows.filter { it.totalDebit != 0.0 || it.totalCredit != 0.0 || it.netBalance != 0.0 }
            
            val incomeListForPrompt = mutableListOf<Pair<AccountEntity, Double>>()
            incomeData.revenueAccounts.forEach { incomeListForPrompt.add(Pair(it.account, it.netBalance)) }
            incomeData.expenseAccounts.forEach { incomeListForPrompt.add(Pair(it.account, it.netBalance)) }
            
            val result = geminiService.analyzeFinancials(incomeListForPrompt, tbData)
            _aiAnalysisResult.value = result
        }
    }

    // --- PURCHASE STREAMS ---
    val purchaseOrders: StateFlow<List<PurchaseOrderWithOwner>> = repository.allPurchaseOrders
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading purchase orders", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseInvoices: StateFlow<List<PurchaseInvoiceWithOwner>> = repository.allPurchaseInvoices
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading purchase invoices", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseReturns: StateFlow<List<PurchaseReturnWithOwner>> = repository.allPurchaseReturns
        .catch { t ->
            android.util.Log.e("AccountingViewModel", "Error loading purchase returns", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _supplierStatement = MutableStateFlow<List<SupplierStatementTx>>(emptyList())
    val supplierStatement: StateFlow<List<SupplierStatementTx>> = _supplierStatement.asStateFlow()

    fun loadSupplierStatement(supplierId: Long, fromDate: Long, toDate: Long) {
        viewModelScope.launch {
            try {
                _supplierStatement.value = repository.getSupplierStatement(supplierId, fromDate, toDate)
            } catch (e: Exception) {
                _supplierStatement.value = emptyList()
            }
        }
    }

    // --- PURCHASE OPERATIONS ---
    fun createPurchaseOrder(
        supplierId: Long,
        date: Long,
        lines: List<Pair<Long, Pair<Double, Double>>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.createPurchaseOrder(supplierId, date, lines)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }

    fun deletePurchaseOrder(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePurchaseOrder(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء الحذف")
            }
        }
    }

    fun confirmPurchaseOrder(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.confirmPurchaseOrder(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء التأكيد")
            }
        }
    }

    fun convertOrderToInvoice(
        orderId: Long,
        paymentAccountId: Long?,
        isCredit: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.convertOrderToInvoice(orderId, paymentAccountId, isCredit)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء تحويل الأمر")
            }
        }
    }

    fun createPurchaseInvoice(
        supplierId: Long,
        date: Long,
        dueDate: Long,
        isCredit: Boolean,
        paymentAccountId: Long?,
        lines: List<Pair<Long, Pair<Double, Double>>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.createPurchaseInvoiceDraft(supplierId, date, dueDate, isCredit, paymentAccountId, lines)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء حفظ الفاتورة")
            }
        }
    }

    fun deletePurchaseInvoice(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePurchaseInvoice(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء الحذف")
            }
        }
    }

    fun confirmPurchaseInvoice(
        invoiceId: Long,
        paymentAccountId: Long?,
        landedCosts: Double,
        landedAllocationMethod: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.confirmPurchaseInvoice(invoiceId, paymentAccountId, landedCosts, landedAllocationMethod)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء تأكيد الفاتورة")
            }
        }
    }

    fun recordSupplierPayment(
        invoiceId: Long,
        amount: Double,
        payAccountId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.recordSupplierPayment(invoiceId, amount, payAccountId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء تسجيل السداد")
            }
        }
    }

    fun createPurchaseReturn(
        invoiceId: Long,
        reason: String,
        itemsToReturn: List<Pair<Long, Double>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.createPurchaseReturn(invoiceId, reason, itemsToReturn)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء معالجة المرتجع")
            }
        }
    }
}

// Data wrappers for dashboard stats and reports
data class TrialBalanceRow(
    val account: AccountEntity,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double
)

data class TrialBalanceData(
    val rows: List<TrialBalanceRow> = emptyList(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0
)

data class LedgerTransaction(
    val entryNumber: String,
    val date: Long,
    val description: String,
    val debit: Double,
    val credit: Double,
    val balanceAfter: Double
)

data class IncomeStatementData(
    val revenueAccounts: List<TrialBalanceRow> = emptyList(),
    val expenseAccounts: List<TrialBalanceRow> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netIncome: Double = 0.0
)

data class BalanceSheetData(
    val assetAccounts: List<TrialBalanceRow> = emptyList(),
    val liabilityAccounts: List<TrialBalanceRow> = emptyList(),
    val equityAccounts: List<TrialBalanceRow> = emptyList(),
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val totalEquity: Double = 0.0
)
