package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AccountingRepository(db)

    // --- State Streams from Database ---
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryWithOwner>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partners: StateFlow<List<PartnerEntity>> = repository.allPartners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payrolls: StateFlow<List<PayrollRecordEntity>> = repository.allPayrolls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search/Filter states ---
    private val _selectedLedgerAccountId = MutableStateFlow<Long?>(null)
    val selectedLedgerAccountId: StateFlow<Long?> = _selectedLedgerAccountId.asStateFlow()

    init {
        // Seed default accounts initially if database is fresh
        seedDefaultAccountsIfNeeded()
    }

    private fun seedDefaultAccountsIfNeeded() {
        viewModelScope.launch {
            repository.checkAndSeed()
        }
    }

    fun selectLedgerAccount(id: Long?) {
        _selectedLedgerAccountId.value = id
    }

    // --- Account operations ---
    fun addAccount(code: String, nameAr: String, nameEn: String, type: String, parentId: Long?, allowPosting: Boolean) {
        viewModelScope.launch {
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
        }
    }

    // --- Journal Entry operations ---
    fun addManualJournalEntry(description: String, date: Long, entryNo: String, draftLines: List<Pair<Long, Pair<Double, Double>>>) {
        viewModelScope.launch {
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
        }
    }

    fun postJournalEntry(id: Long) {
        viewModelScope.launch {
            repository.postJournalEntry(id)
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteJournalEntry(id)
        }
    }

    // --- Partner operations ---
    fun addPartner(name: String, type: String, phone: String, email: String, limit: Double) {
        viewModelScope.launch {
            repository.addPartner(PartnerEntity(name = name, type = type, phone = phone, email = email, creditLimit = limit))
        }
    }

    // --- Product operations ---
    fun addProduct(code: String, name: String, price: Double, cost: Double, minStock: Double, type: String) {
        viewModelScope.launch {
            repository.addProduct(ProductEntity(code = code, name = name, price = price, cost = cost, minStock = minStock, type = type))
        }
    }

    // --- Inventory adjustments ---
    fun adjustStock(productId: Long, qtyChange: Double, description: String) {
        viewModelScope.launch {
            repository.recordStockAdjustment(productId, qtyChange, description, System.currentTimeMillis())
        }
    }

    // --- Invoicing integrations with automatic double entries ---
    fun recordSaleInvoice(customerId: Long, productId: Long, quantity: Double, price: Double, cashAccountId: Long) {
        viewModelScope.launch {
            repository.addSalesInvoice(customerId, productId, quantity, price, cashAccountId, System.currentTimeMillis())
        }
    }

    fun recordPurchaseInvoice(supplierId: Long, productId: Long, quantity: Double, cost: Double, paymentAccountId: Long) {
        viewModelScope.launch {
            repository.addPurchaseInvoice(supplierId, productId, quantity, cost, paymentAccountId, System.currentTimeMillis())
        }
    }

    // --- Employee & payroll ---
    fun addEmployee(code: String, name: String, department: String, phone: String, salary: Double) {
        viewModelScope.launch {
            repository.addEmployee(EmployeeEntity(code = code, name = name, department = department, phone = phone, basicSalary = salary))
        }
    }

    fun generatePayroll(month: String, allowances: Double, deductions: Double) {
        viewModelScope.launch {
            repository.generatePayroll(month, allowances, deductions)
        }
    }

    fun payPayrollSalary(record: PayrollRecordEntity, cashAccountId: Long) {
        viewModelScope.launch {
            repository.processPayrollPayment(record, System.currentTimeMillis(), cashAccountId)
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
