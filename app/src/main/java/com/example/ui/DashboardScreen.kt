package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

enum class ActiveTab(val titleAr: String, val icon: ImageVector) {
    OVERVIEW("الرئيسية", Icons.Default.Home),
    ACCOUNTS("الحسابات (دليل)", Icons.AutoMirrored.Filled.List),
    JOURNAL("القيود اليومية", Icons.Default.Edit),
    TRIAL_BALANCE("دفتر الميزان", Icons.Default.CheckCircle),
    INVOICING("الفواتير", Icons.Default.ShoppingCart),
    INVENTORY("المخازن", Icons.Default.Build),
    HR_PAYROLL("شؤون الموظفين", Icons.Default.AccountBox),
    REPORTS("التقارير المالية", Icons.Default.Menu)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ActiveTab.OVERVIEW) }

    // Collect Reactive State Flows
    val accountList by viewModel.accounts.collectAsStateWithLifecycle()
    val journalList by viewModel.journalEntries.collectAsStateWithLifecycle()
    val productList by viewModel.products.collectAsStateWithLifecycle()
    val partnerList by viewModel.partners.collectAsStateWithLifecycle()
    val employeeList by viewModel.employees.collectAsStateWithLifecycle()
    val payrollList by viewModel.payrolls.collectAsStateWithLifecycle()
    val stockMvList by viewModel.stockMovements.collectAsStateWithLifecycle()

    val trialData by viewModel.trialBalance.collectAsStateWithLifecycle()
    val ledgerData by viewModel.accountLedger.collectAsStateWithLifecycle()
    val incomeData by viewModel.incomeStatement.collectAsStateWithLifecycle()
    val balanceSheetData by viewModel.balanceSheet.collectAsStateWithLifecycle()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsStateWithLifecycle()

    val selectedLedgerId by viewModel.selectedLedgerAccountId.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "المحاسب المالي ERP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نظام إدارة الحسابات المتكامل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ActiveTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationDrawerItem(
                        icon = { Icon(tab.icon, contentDescription = tab.titleAr, modifier = Modifier.size(20.dp)) },
                        label = { Text(tab.titleAr, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        selected = isSelected,
                        onClick = {
                            currentTab = tab
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة الجانبية",
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المحاسب المالي المحترف ERP",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "UTC: 2026",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .navigationBarsPadding() // Avoid overlapping with device navigation keys
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    ActiveTab.values().forEach { tab ->
                        FilterChip(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            label = { Text(tab.titleAr) },
                            leadingIcon = { Icon(tab.icon, contentDescription = tab.titleAr, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    ActiveTab.OVERVIEW -> OverviewTab(
                        trialData = trialData,
                        journalList = journalList,
                        incomeData = incomeData,
                        onNavigateToJournal = { currentTab = ActiveTab.JOURNAL }
                    )
                    ActiveTab.ACCOUNTS -> AccountsTab(
                        accounts = accountList,
                        onAddAccount = { code, nameAr, nameEn, type, parent ->
                            viewModel.addAccount(code, nameAr, nameEn, type, parent, true)
                        }
                    )
                    ActiveTab.JOURNAL -> JournalTab(
                        accounts = accountList,
                        entries = journalList,
                        onAddEntry = { desc, date, no, lines ->
                            viewModel.addManualJournalEntry(desc, date, no, lines)
                        },
                        onPostEntry = { id -> viewModel.postJournalEntry(id) },
                        onDeleteEntry = { id -> viewModel.deleteJournalEntry(id) }
                    )
                    ActiveTab.TRIAL_BALANCE -> TrialBalanceAndLedgerTab(
                        trialData = trialData,
                        ledgerData = ledgerData,
                        accounts = accountList,
                        selectedLedgerId = selectedLedgerId,
                        onSelectLedger = { id -> viewModel.selectLedgerAccount(id) }
                    )
                    ActiveTab.INVOICING -> InvoicingTab(
                        partners = partnerList,
                        products = productList,
                        accounts = accountList,
                        onAddPartner = { name, type, phone, email, limit ->
                            viewModel.addPartner(name, type, phone, email, limit)
                        },
                        onAddProduct = { code, name, price, cost, minStock, type ->
                            viewModel.addProduct(code, name, price, cost, minStock, type)
                        },
                        onRecordSale = { cust, prod, qty, price, cashAcc ->
                            viewModel.recordSaleInvoice(cust, prod, qty, price, cashAcc)
                        },
                        onRecordPurchase = { supp, prod, qty, cost, pAcc ->
                            viewModel.recordPurchaseInvoice(supp, prod, qty, cost, pAcc)
                        }
                    )
                    ActiveTab.INVENTORY -> InventoryTab(
                        products = productList,
                        movements = stockMvList,
                        onAdjustStock = { prodId, qty, desc ->
                            viewModel.adjustStock(prodId, qty, desc)
                        }
                    )
                    ActiveTab.HR_PAYROLL -> HrPayrollTab(
                        employees = employeeList,
                        payrolls = payrollList,
                        accounts = accountList,
                        onAddEmployee = { code, name, dept, phone, salary ->
                            viewModel.addEmployee(code, name, dept, phone, salary)
                        },
                        onGeneratePayroll = { month, allowance, deduction ->
                            viewModel.generatePayroll(month, allowance, deduction)
                        },
                        onPaySalary = { record, cashAcc ->
                            viewModel.payPayrollSalary(record, cashAcc)
                        }
                    )
                    ActiveTab.REPORTS -> ReportsTab(
                        incomeData = incomeData,
                        balanceSheetData = balanceSheetData,
                        aiAnalysisResult = aiAnalysisResult,
                        onAnalyze = { viewModel.analyzeFinancials() }
                    )
                }
            }
        }
    }
}
}

// ----------------------------------------------------
// TAB 1: OVERVIEW (الرئيسية)
// ----------------------------------------------------
@Composable
fun OverviewTab(
    trialData: TrialBalanceData,
    journalList: List<JournalEntryWithOwner>,
    incomeData: IncomeStatementData,
    onNavigateToJournal: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "لوحة المراقبة المالية العامة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Analytical Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Revenues Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("إجمالي الإيرادات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${formatAmount(incomeData.totalRevenue)} ﷼",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Net Profit Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (incomeData.netIncome >= 0) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("صافي الأرباح", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${formatAmount(incomeData.netIncome)} ﷼",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (incomeData.netIncome >= 0) Color(0xFF137333) else Color(0xFFC5221F)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المركز التجاري والتحقق المحاسبي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي التوازن بالدفتر العام", fontSize = 12.sp, color = Color.Gray)
                            Text("${formatAmount(trialData.totalDebit)} ﷼", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        if (trialData.totalDebit == trialData.totalCredit && trialData.totalDebit > 0) {
                            Surface(
                                color = Color(0xFFE6F4EA),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    "متوازن ✓",
                                    color = Color(0xFF137333),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFEF7E0),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    "بانتظار القيود",
                                    color = Color(0xFFB06000),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent transactions list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("آخر قيود اليومية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = onNavigateToJournal) {
                    Text("عرض الكل")
                }
            }
        }

        if (journalList.isEmpty()) {
            item {
                Text(
                    "لا توجد قيود مسجلة حالياً.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(journalList.take(5)) { entryWithOwner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(entryWithOwner.entry.entryNumber, fontWeight = FontWeight.Bold)
                            Text(entryWithOwner.entry.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(formatDate(entryWithOwner.entry.date), fontSize = 10.sp, color = Color.LightGray)
                        }
                        Surface(
                            color = if (entryWithOwner.entry.status == "POSTED") Color(0xFFC2E7FF) else Color(0xFFF1F3F4),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (entryWithOwner.entry.status == "POSTED") "مرحَّل" else "مسودة",
                                color = if (entryWithOwner.entry.status == "POSTED") Color(0xFF004A77) else Color(0xFF3C4043),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: CHARTS OF ACCOUNTS (دليل الحسابات)
// ----------------------------------------------------
@Composable
fun AccountsTab(
    accounts: List<AccountEntity>,
    onAddAccount: (String, String, String, String, Long?) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    var codeInput by remember { mutableStateOf("") }
    var nameArInput by remember { mutableStateOf("") }
    var nameEnInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ASSETS") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }

    val types = listOf(
        "ASSETS" to "الأصول (Assets)",
        "LIABILITIES" to "الخصوم (Liabilities)",
        "EQUITY" to "حقوق الملكية (Equity)",
        "REVENUE" to "الإيرادات (Revenue)",
        "EXPENSE" to "المصروفات (Expenses)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("شجرة دليل الحسابات الموحد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = { showAddDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة")
                    Text("إضافة حساب")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Display sorted Chart Of Accounts tree inside unified list
        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Render root parents and childs hierarchically
                val roots = accounts.filter { it.parentId == null }
                roots.forEach { parent ->
                    item {
                        AccountRow(account = parent, indent = 0)
                    }
                    val children = accounts.filter { it.parentId == parent.id }
                    items(children) { child ->
                        AccountRow(account = child, indent = 1)
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("فتح حساب جديد في الشجرة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            label = { Text("رمز الحساب (الكود، مثال: 1103)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nameArInput,
                            onValueChange = { nameArInput = it },
                            label = { Text("الاسم باللغة العربية") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nameEnInput,
                            onValueChange = { nameEnInput = it },
                            label = { Text("الاسم باللغة الإنجليزية") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Type selector dropdown selection
                        Text("نوع تصنيف الحساب الأساسي :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Column {
                            types.forEach { (typeKey, typeLabel) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedType = typeKey }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(selected = selectedType == typeKey, onClick = { selectedType = typeKey })
                                    Text(typeLabel, fontSize = 13.sp)
                                }
                            }
                        }

                        // Parent selector inside dialog
                        Text("الحساب الأب الاسترشادي :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val parentOptions = accounts.filter { !it.allowPosting }
                        var expandedParents by remember { mutableStateOf(false) }
                        val parentLabel = parentOptions.find { it.id == selectedParentId }?.let { "${it.code} - ${it.nameAr}" } ?: "بدون (حساب رئيسي)"

                        Box {
                            OutlinedButton(
                                onClick = { expandedParents = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(parentLabel)
                            }
                            DropdownMenu(
                                expanded = expandedParents,
                                onDismissRequest = { expandedParents = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون (حساب رئيسي أصل)") },
                                    onClick = { selectedParentId = null; expandedParents = false }
                                )
                                parentOptions.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.code} - ${p.nameAr}") },
                                        onClick = { selectedParentId = p.id; expandedParents = false }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (codeInput.isNotBlank() && nameArInput.isNotBlank()) {
                                onAddAccount(codeInput, nameArInput, nameEnInput, selectedType, selectedParentId)
                                showAddDialog = false
                                // Clear inputs
                                codeInput = ""
                                nameArInput = ""
                                nameEnInput = ""
                                selectedParentId = null
                            }
                        }
                    ) {
                        Text("تأكيد الحفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
                }
            )
        }
    }
}

@Composable
fun AccountRow(account: AccountEntity, indent: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 16).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.allowPosting) MaterialTheme.colorScheme.surface else Color(0xFFF1F5F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (account.allowPosting) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (account.allowPosting) Icons.Default.Menu else Icons.AutoMirrored.Filled.List,
                    contentDescription = "",
                    tint = if (account.allowPosting) TealAccent else PrimarySlate,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "${account.code} / ${account.nameAr}",
                        fontWeight = if (account.allowPosting) FontWeight.Normal else FontWeight.Bold,
                        fontSize = if (account.allowPosting) 14.sp else 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(account.nameEn, fontSize = 11.sp, color = Color.Gray)
                }
            }
            // Badge type Ar
            Surface(
                color = when (account.type) {
                    "ASSETS" -> Color(0xFFE8F0FE)
                    "LIABILITIES" -> Color(0xFFFCE8E6)
                    "EQUITY" -> Color(0xFFFEF7E0)
                    "REVENUE" -> Color(0xFFE6F4EA)
                    else -> Color(0xFFF1F3F4)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                val label = when (account.type) {
                    "ASSETS" -> "أصول"
                    "LIABILITIES" -> "خصوم"
                    "EQUITY" -> "حقوق"
                    "REVENUE" -> "إيراد"
                    else -> "مصروف"
                }
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (account.type) {
                        "ASSETS" -> Color(0xFF1967D2)
                        "LIABILITIES" -> Color(0xFFC5221F)
                        "EQUITY" -> Color(0xFFB06000)
                        "REVENUE" -> Color(0xFF137333)
                        else -> Color(0xFF3C4043)
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// TAB 3: JOURNAL ENTRIES (قيود اليومية والتحكم بالتوازن)
// ----------------------------------------------------
@Composable
fun JournalTab(
    accounts: List<AccountEntity>,
    entries: List<JournalEntryWithOwner>,
    onAddEntry: (String, Long, String, List<Pair<Long, Pair<Double, Double>>>) -> Unit,
    onPostEntry: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    var entryNoInput by remember { mutableStateOf("JV-${System.currentTimeMillis() % 1000}") }
    var descInput by remember { mutableStateOf("") }
    val postingAccounts = accounts.filter { it.allowPosting }

    // Multi-line editor state inside standard manual Journal Entry Dialog
    val draftLines = remember { mutableStateListOf<Pair<Long, Pair<Double, Double>>>() }

    var lineAccountId by remember { mutableStateOf<Long?>(null) }
    var debitInput by remember { mutableStateOf("") }
    var creditInput by remember { mutableStateOf("") }

    // Live balanced computations calculation
    val liveSumDebits = draftLines.sumOf { it.second.first }
    val liveSumCredits = draftLines.sumOf { it.second.second }
    val isBalanced = liveSumDebits == liveSumCredits && draftLines.size >= 2 && liveSumDebits > 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("القيود اليومية والتحكم في التوازن", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = {
                draftLines.clear()
                showAddDialog = true
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "")
                    Text("إنشاء قيد يدوي قيد توازن")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا قيود محاسبية مسجلة. اضغط 'إنشاء قيد' للبدء.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entryAndLines ->
                    JournalEntryWithOwnerCard(
                        entryAndLines = entryAndLines,
                        onPost = onPostEntry,
                        onDelete = onDeleteEntry,
                        accounts = accounts
                    )
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("تسجيل قيد محاسبي double-entry متوازن", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = entryNoInput,
                            onValueChange = { entryNoInput = it },
                            label = { Text("رقم القيد تلقائي") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("البيان العام للقيد (مثال: إيداع رأس مال)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider()

                        // Add lines block
                        Text("البنود المرحلية الحالية (${draftLines.size} أسطر):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // Render added draft lines
                        Box(modifier = Modifier.heightIn(max = 120.dp)) {
                            LazyColumn {
                                items(draftLines) { (accId, debCred) ->
                                    val accLabel = accounts.find { it.id == accId }?.nameAr ?: "Null"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(accLabel, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                        Text("مدين: ${debCred.first} | دائن: ${debCred.second}", fontSize = 11.sp, modifier = Modifier.weight(2f))
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف سطر",
                                            tint = Color.Red,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { draftLines.remove(Pair(accId, debCred)) }
                                        )
                                    }
                                }
                            }
                        }

                        // Add line fields
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var expandedAccountsDropdown by remember { mutableStateOf(false) }
                            val currentAccLabel = postingAccounts.find { it.id == lineAccountId }?.let { "${it.code} - ${it.nameAr}" } ?: "اختر الحساب"

                            Box(modifier = Modifier.weight(1.5f)) {
                                OutlinedButton(onClick = { expandedAccountsDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(currentAccLabel, maxLines = 1, fontSize = 11.sp)
                                }
                                DropdownMenu(expanded = expandedAccountsDropdown, onDismissRequest = { expandedAccountsDropdown = false }) {
                                    postingAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text("${acc.code} ${acc.nameAr}") },
                                            onClick = {
                                                lineAccountId = acc.id
                                                expandedAccountsDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = debitInput,
                                onValueChange = { debitInput = it },
                                label = { Text("مدين", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = creditInput,
                                onValueChange = { creditInput = it },
                                label = { Text("دائن", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Button(
                                onClick = {
                                    val finalAccId = lineAccountId
                                    val debValue = debitInput.toDoubleOrNull() ?: 0.0
                                    val credValue = creditInput.toDoubleOrNull() ?: 0.0
                                    if (finalAccId != null && (debValue > 0 || credValue > 0)) {
                                        draftLines.add(Pair(finalAccId, Pair(debValue, credValue)))
                                        lineAccountId = null
                                        debitInput = ""
                                        creditInput = ""
                                    }
                                },
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Text("+")
                            }
                        }

                        // Live balanced computation indicator
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("إجمالي المدين: ${formatAmount(liveSumDebits)} ﷼", fontSize = 11.sp, color = if (isBalanced) Color(0xFF137333) else Color.Red)
                                Text("إجمالي الدائن: ${formatAmount(liveSumCredits)} ﷼", fontSize = 11.sp, color = if (isBalanced) Color(0xFF137333) else Color.Red)
                            }

                            Surface(
                                color = if (isBalanced) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (isBalanced) "متوازن" else "غير متوازن",
                                    color = if (isBalanced) Color(0xFF137333) else Color(0xFFC5221F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isBalanced) {
                                onAddEntry(descInput, System.currentTimeMillis(), entryNoInput, draftLines.toList())
                                showAddDialog = false
                            }
                        },
                        enabled = isBalanced
                    ) {
                        Text("حفظ ترحيل مسودة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
                }
            )
        }
    }
}

@Composable
fun JournalEntryWithOwnerCard(
    entryAndLines: JournalEntryWithOwner,
    onPost: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    accounts: List<AccountEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(entryAndLines.entry.entryNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(entryAndLines.entry.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(formatDate(entryAndLines.entry.date), fontSize = 11.sp, color = Color.LightGray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entryAndLines.entry.status == "DRAFT") {
                        Button(
                            onClick = { onPost(entryAndLines.entry.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                        ) {
                            Text("ترحيل")
                        }
                        IconButton(onClick = { onDelete(entryAndLines.entry.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف قيد", tint = Color.Red)
                        }
                    } else {
                        Surface(
                            color = Color(0xFFE6F4EA),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "تم الترحيل للدفاتر العام",
                                color = Color(0xFF137333),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Sub levels listing line details
            entryAndLines.lines.forEach { line ->
                val accountLabel = accounts.find { it.id == line.accountId }?.nameAr ?: "Unknown"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(accountLabel, modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (line.debit > 0) "مدين: ${formatAmount(line.debit)}" else "دائن: ${formatAmount(line.credit)}",
                        modifier = Modifier.weight(1.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        color = if (line.debit > 0) TealAccent else AmberGold
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: TRIAL BALANCE & GENERAL LEDGER (دفتر الأستاذ والميزان)
// ----------------------------------------------------
@Composable
fun TrialBalanceAndLedgerTab(
    trialData: TrialBalanceData,
    ledgerData: List<LedgerTransaction>,
    accounts: List<AccountEntity>,
    selectedLedgerId: Long?,
    onSelectLedger: (Long?) -> Unit
) {
    var modeToggle by remember { mutableStateOf(0) } // 0: Trial Balance, 1: General Ledger

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mode Selector Toggle Tab Card
        TabRow(selectedTabIndex = modeToggle, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = modeToggle == 0, onClick = { modeToggle = 0 }, text = { Text("ميزان المراجعة بالمجاميع والأرصدة") })
            Tab(selected = modeToggle == 1, onClick = { modeToggle = 1 }, text = { Text("دفتر الأستاذ العام للحساب") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (modeToggle == 0) {
            // Render Trial Balance with aggregates validation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الحساب", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                Text("مدين مجموع", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
                Text("دائن مجموع", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
                Text("صافي الرصيد", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(trialData.rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${row.account.code} / ${row.account.nameAr}", modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text(formatAmount(row.totalDebit), modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
                        Text(formatAmount(row.totalCredit), modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
                        Text(
                            "${formatAmount(Math.abs(row.netBalance))} ${if (row.netBalance >= 0) "مدين" else "دائن"}",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                            color = if (row.netBalance >= 0) TealAccent else AmberGold
                        )
                    }
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Totals Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (trialData.totalDebit == trialData.totalCredit) Color(0xFFE6F4EA) else Color(0xFFFEEFC3))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المجاميع المتوازنة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${formatAmount(trialData.totalDebit)} ﷼", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${formatAmount(trialData.totalCredit)} ﷼", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

        } else {
            // Render General Ledger list
            val postingAccounts = accounts.filter { it.allowPosting }
            var dropdownMenuExpanded by remember { mutableStateOf(false) }
            val selectorLabel = postingAccounts.find { it.id == selectedLedgerId }?.let { "${it.code} / ${it.nameAr}" } ?: "اختر حساباً لمعاينة الدفتر"

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { dropdownMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectorLabel)
                }
                DropdownMenu(expanded = dropdownMenuExpanded, onDismissRequest = { dropdownMenuExpanded = false }) {
                    postingAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.code} ${acc.nameAr}") },
                            onClick = {
                                onSelectLedger(acc.id)
                                dropdownMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedLedgerId == null) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("بانتظار تحديد واختيار حساب من شجرة الدفاتر.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else if (ledgerData.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("لا توجد قيود مكشوفة أو مرحَّلة لهذا الحساب المحدد حالياً.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تاريخ / سند", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), fontSize = 11.sp)
                    Text("البيان", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                    Text("مدين", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), fontSize = 11.sp, textAlign = TextAlign.End)
                    Text("دائن", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), fontSize = 11.sp, textAlign = TextAlign.End)
                    Text("الرصيد", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ledgerData) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(item.entryNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(formatDate(item.date), fontSize = 9.sp, color = Color.Gray)
                            }
                            Text(item.description, modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                            Text(formatAmount(item.debit), modifier = Modifier.weight(0.9f), fontSize = 11.sp, textAlign = TextAlign.End)
                            Text(formatAmount(item.credit), modifier = Modifier.weight(0.9f), fontSize = 11.sp, textAlign = TextAlign.End)
                            Text(formatAmount(item.balanceAfter), modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 5: INVOICING & SALES/PURCHASES (الفواتير)
// ----------------------------------------------------
@Composable
fun InvoicingTab(
    partners: List<PartnerEntity>,
    products: List<ProductEntity>,
    accounts: List<AccountEntity>,
    onAddPartner: (String, String, String, String, Double) -> Unit,
    onAddProduct: (String, String, Double, Double, Double, String) -> Unit,
    onRecordSale: (Long, Long, Double, Double, Long) -> Unit,
    onRecordPurchase: (Long, Long, Double, Double, Long) -> Unit
) {
    var invoicingMode by remember { mutableStateOf(0) } // 0: Sales Invoice, 1: Purchase Invoice, 2: Contacts Setup, 3: Products Add

    // Local lists
    val customers = partners.filter { it.type == "CUSTOMER" }
    val suppliers = partners.filter { it.type == "SUPPLIER" }
    val cashBankAccounts = accounts.filter { it.allowPosting && (it.code.startsWith("1101") || it.code.startsWith("1102")) }

    // Forms entry elements state variables
    var partnerArName by remember { mutableStateOf("") }
    var partnerType by remember { mutableStateOf("CUSTOMER") }
    var partnerPhone by remember { mutableStateOf("") }
    var partnerEmail by remember { mutableStateOf("") }
    var creditLimitValue by remember { mutableStateOf("10000.0") }

    var productCode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCost by remember { mutableStateOf("") }
    var minStockVal by remember { mutableStateOf("2.0") }

    // Active sale form state variables
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var selectedSaleProductId by remember { mutableStateOf<Long?>(null) }
    var saleQtyInput by remember { mutableStateOf("1") }
    var salePriceOverride by remember { mutableStateOf("") }
    var saleFinancialAccountId by remember { mutableStateOf<Long?>(null) }

    // Active purchase form state variables
    var selectedSupplierId by remember { mutableStateOf<Long?>(null) }
    var selectedPurchaseProductId by remember { mutableStateOf<Long?>(null) }
    var purchaseQtyInput by remember { mutableStateOf("1") }
    var purchaseCostInput by remember { mutableStateOf("") }
    var purchasePaymentAccountId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScrollableTabRow(selectedTabIndex = invoicingMode, edgePadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = invoicingMode == 0, onClick = { invoicingMode = 0 }, text = { Text("فاتورة مبيعات") })
                Tab(selected = invoicingMode == 1, onClick = { invoicingMode = 1 }, text = { Text("فاتورة مشتريات") })
                Tab(selected = invoicingMode == 2, onClick = { invoicingMode = 2 }, text = { Text("العملاء والموردين") })
                Tab(selected = invoicingMode == 3, onClick = { invoicingMode = 3 }, text = { Text("إعداد المنتجات") })
            }
        }

        when (invoicingMode) {
            0 -> {
                // Sales invoice form code block
                item {
                    Text("إصدار فاتورة مبيعات متكاملة القيود", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("ستخفف المخزون تلقائياً، وتثبت الإيراد والمدفوع والـ COGS مزدوجاً!", fontSize = 11.sp, color = Color.Gray)
                }

                item {
                    // Customer select dropdown
                    var expandedCustDropdown by remember { mutableStateOf(false) }
                    val custLabel = customers.find { it.id == selectedCustomerId }?.name ?: "اختر العميل"

                    Box {
                        OutlinedButton(onClick = { expandedCustDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(custLabel)
                        }
                        DropdownMenu(expanded = expandedCustDropdown, onDismissRequest = { expandedCustDropdown = false }) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = { Text(cust.name) },
                                    onClick = {
                                        selectedCustomerId = cust.id
                                        expandedCustDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Product select
                    var expandedProdDropdown by remember { mutableStateOf(false) }
                    val pName = products.find { it.id == selectedSaleProductId }?.let { "${it.name} (المخزون المتوفر: ${it.stock})" } ?: "اختر المنتج"

                    Box {
                        OutlinedButton(onClick = { expandedProdDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(pName)
                        }
                        DropdownMenu(expanded = expandedProdDropdown, onDismissRequest = { expandedProdDropdown = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.stock} متوفر)") },
                                    onClick = {
                                        selectedSaleProductId = p.id
                                        salePriceOverride = p.price.toString()
                                        expandedProdDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = saleQtyInput,
                        onValueChange = { saleQtyInput = it },
                        label = { Text("الكمية المباعة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = salePriceOverride,
                        onValueChange = { salePriceOverride = it },
                        label = { Text("سعر البيع المعتمد للوحدة (﷼)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    val q = saleQtyInput.toDoubleOrNull() ?: 0.0
                    val pr = salePriceOverride.toDoubleOrNull() ?: 0.0
                    val subtotal = q * pr
                    val vat = subtotal * 0.15
                    val grandTotal = subtotal + vat

                    if (subtotal > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ملخص مالبة الفاتورة (ضريبة القيمة المضافة المبسطة):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المجموع الفرعي (غير شامل الضريبة):", fontSize = 11.sp)
                                    Text("${formatAmount(subtotal)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ضريبة القيمة المضافة (15٪):", fontSize = 11.sp)
                                    Text("${formatAmount(vat)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الإجمالي المستحق (شامل الضريبة):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${formatAmount(grandTotal)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                item {
                    // Financial Account cash/Bank Destination
                    var expandedCashDropdown by remember { mutableStateOf(false) }
                    val cashLabel = cashBankAccounts.find { it.id == saleFinancialAccountId }?.let { "${it.code} - ${it.nameAr}" } ?: "اختر حساب التحصيل (صندوق / بنك)"

                    Box {
                        OutlinedButton(onClick = { expandedCashDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(cashLabel)
                        }
                        DropdownMenu(expanded = expandedCashDropdown, onDismissRequest = { expandedCashDropdown = false }) {
                            cashBankAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.code} ${acc.nameAr}") },
                                    onClick = {
                                        saleFinancialAccountId = acc.id
                                        expandedCashDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cust = selectedCustomerId
                            val prod = selectedSaleProductId
                            val q = saleQtyInput.toDoubleOrNull() ?: 1.0
                            val pr = salePriceOverride.toDoubleOrNull() ?: 0.0
                            val acc = saleFinancialAccountId
                            if (cust != null && prod != null && acc != null) {
                                onRecordSale(cust, prod, q, pr, acc)
                                // clear
                                selectedCustomerId = null
                                selectedSaleProductId = null
                                saleQtyInput = "1"
                                salePriceOverride = ""
                                saleFinancialAccountId = null
                            }
                        },
                        enabled = selectedCustomerId != null && selectedSaleProductId != null && saleFinancialAccountId != null
                    ) {
                        Text("حفظ وترحيل فاتورة المبيعات")
                    }
                }
            }

            1 -> {
                // Purchase Invoice template Form
                item {
                    Text("توريد شراء وحساب تكلفة المتوسط المرجح", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("ستقوم تلقائياً بزيادة المخزون وحساب متوسط التكلفة وحفظ القيد المزدوج!", fontSize = 11.sp, color = Color.Gray)
                }

                item {
                    var expandedSuppDropdown by remember { mutableStateOf(false) }
                    val suppLabel = suppliers.find { it.id == selectedSupplierId }?.name ?: "اختر المورد"

                    Box {
                        OutlinedButton(onClick = { expandedSuppDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(suppLabel)
                        }
                        DropdownMenu(expanded = expandedSuppDropdown, onDismissRequest = { expandedSuppDropdown = false }) {
                            suppliers.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        selectedSupplierId = p.id
                                        expandedSuppDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    var expandedProdByPurchDropdown by remember { mutableStateOf(false) }
                    val prLabel = products.find { it.id == selectedPurchaseProductId }?.name ?: "اختر المنتج المورد"

                    Box {
                        OutlinedButton(onClick = { expandedProdByPurchDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(prLabel)
                        }
                        DropdownMenu(expanded = expandedProdByPurchDropdown, onDismissRequest = { expandedProdByPurchDropdown = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        selectedPurchaseProductId = p.id
                                        purchaseCostInput = p.cost.toString()
                                        expandedProdByPurchDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = purchaseQtyInput,
                        onValueChange = { purchaseQtyInput = it },
                        label = { Text("الكمية الموردة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = purchaseCostInput,
                        onValueChange = { purchaseCostInput = it },
                        label = { Text("تكلفة الشراء للوحدة (﷼)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    val q = purchaseQtyInput.toDoubleOrNull() ?: 0.0
                    val cost = purchaseCostInput.toDoubleOrNull() ?: 0.0
                    val subtotal = q * cost
                    val vat = subtotal * 0.15
                    val grandTotal = subtotal + vat

                    if (subtotal > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ملخص مالية فاتورة المشتريات (مع الاسترداد الضريبي):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المجموع الفرعي (غير شامل الضريبة):", fontSize = 11.sp)
                                    Text("${formatAmount(subtotal)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ضريبة المدخلات القابلة للاسترداد (15٪):", fontSize = 11.sp)
                                    Text("${formatAmount(vat)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي المدفوعات المستحق (شامل الضريبة):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${formatAmount(grandTotal)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }

                item {
                    var expandedPAccDropdown by remember { mutableStateOf(false) }
                    val pAccLabel = cashBankAccounts.find { it.id == purchasePaymentAccountId }?.let { "${it.code} - ${it.nameAr}" } ?: "اختر حساب سداد القيمة (صندوق / بنك)"

                    Box {
                        OutlinedButton(onClick = { expandedPAccDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(pAccLabel)
                        }
                        DropdownMenu(expanded = expandedPAccDropdown, onDismissRequest = { expandedPAccDropdown = false }) {
                            cashBankAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.code} ${acc.nameAr}") },
                                    onClick = {
                                        purchasePaymentAccountId = acc.id
                                        expandedPAccDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val supp = selectedSupplierId
                            val prod = selectedPurchaseProductId
                            val q = purchaseQtyInput.toDoubleOrNull() ?: 1.0
                            val c = purchaseCostInput.toDoubleOrNull() ?: 0.0
                            val acc = purchasePaymentAccountId
                            if (supp != null && prod != null && acc != null) {
                                onRecordPurchase(supp, prod, q, c, acc)
                                // clear
                                selectedSupplierId = null
                                selectedPurchaseProductId = null
                                purchaseQtyInput = "1"
                                purchaseCostInput = ""
                                purchasePaymentAccountId = null
                            }
                        },
                        enabled = selectedSupplierId != null && selectedPurchaseProductId != null && purchasePaymentAccountId != null
                    ) {
                        Text("حفظ وترحيل فاتورة المشتريات")
                    }
                }
            }

            2 -> {
                // Partner Customer/Supplier listing and Add Form
                item {
                    Text("إدارة جهات الاتصال التجارية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة جهة اتصال تجارية", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            OutlinedTextField(value = partnerArName, onValueChange = { partnerArName = it }, label = { Text("الاسم الكامل بالبلد") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    RadioButton(selected = partnerType == "CUSTOMER", onClick = { partnerType = "CUSTOMER" })
                                    Text("عميل")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    RadioButton(selected = partnerType == "SUPPLIER", onClick = { partnerType = "SUPPLIER" })
                                    Text("مورد")
                                }
                            }
                            OutlinedTextField(value = partnerPhone, onValueChange = { partnerPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = partnerEmail, onValueChange = { partnerEmail = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (partnerArName.isNotBlank()) {
                                        onAddPartner(partnerArName, partnerType, partnerPhone, partnerEmail, creditLimitValue.toDoubleOrNull() ?: 10000.0)
                                        // Clear
                                        partnerArName = ""
                                        partnerPhone = ""
                                        partnerEmail = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إدخال جهة اتصال")
                            }
                        }
                    }
                }

                item {
                    Text("جهات الاتصال التجارية المسجلة :", fontWeight = FontWeight.Bold)
                }

                if (partners.isEmpty()) {
                    item { Text("لا يوجد شركاء تجاريين حالياً.", color = Color.Gray) }
                } else {
                    items(partners) { p ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text("هاتف: ${p.phone} | بريد: ${p.email}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Surface(
                                    color = if (p.type == "CUSTOMER") Color(0xFFE8F0FE) else Color(0xFFFEF7E0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        if (p.type == "CUSTOMER") "عميل" else "مورد",
                                        color = if (p.type == "CUSTOMER") Color(0xFF1967D2) else Color(0xFFB06000),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Products add and configuration template Block
                item {
                    Text("إدارة وتعريف المنتجات والخدمات السلعية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("بطاقة مادة / منتج جديد", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            OutlinedTextField(value = productCode, onValueChange = { productCode = it }, label = { Text("رمز المادة (الكود، مثال: P001)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = productPrice, onValueChange = { productPrice = it }, label = { Text("سعر البيع الافتراضي للمستهلك (﷼)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = productCost, onValueChange = { productCost = it }, label = { Text("تكلفة الشراء للوحدة (﷼)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                            Button(
                                onClick = {
                                    if (productCode.isNotBlank() && productName.isNotBlank()) {
                                        onAddProduct(productCode, productName, productPrice.toDoubleOrNull() ?: 0.0, productCost.toDoubleOrNull() ?: 0.0, minStockVal.toDoubleOrNull() ?: 2.0, "PRODUCT")
                                        // clean
                                        productCode = ""
                                        productName = ""
                                        productPrice = ""
                                        productCost = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة لكتالوج المواد")
                            }
                        }
                    }
                }

                item {
                    Text("الأصناف والمواد المتاحة :", fontWeight = FontWeight.Bold)
                }

                if (products.isEmpty()) {
                    item { Text("لا توجد أصناف مسجلة بالدليل حالياً.", color = Color.Gray) }
                } else {
                    items(products) { p ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${p.code} - ${p.name}", fontWeight = FontWeight.Bold)
                                    Text("سعر البيع: ${p.price} | تكلفة الشراء: ${p.cost}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Surface(
                                    color = Color(0xFFE6F4EA),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "مخزون: ${p.stock}",
                                        color = Color(0xFF137333),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 6: INVENTORY & MOVEMENTS (المخازن والتسوية الجردية)
// ----------------------------------------------------
@Composable
fun InventoryTab(
    products: List<ProductEntity>,
    movements: List<StockMovementEntity>,
    onAdjustStock: (Long, Double, String) -> Unit
) {
    var invViewMode by remember { mutableStateOf(0) } // 0: Stock levels, 1: Inventory Adjustments, 2: Stock movements history

    // adjustment form variables
    var selectedAdjustProductId by remember { mutableStateOf<Long?>(null) }
    var adjustQtyInput by remember { mutableStateOf("") }
    var adjustDescInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = invViewMode, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = invViewMode == 0, onClick = { invViewMode = 0 }, text = { Text("المخزون الفعلي المتاح") })
            Tab(selected = invViewMode == 1, onClick = { invViewMode = 1 }, text = { Text("تسوية جردية") })
            Tab(selected = invViewMode == 2, onClick = { invViewMode = 2 }, text = { Text("سجل حركات المخزن") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (invViewMode) {
            0 -> {
                Text("الأرصدة الحالية للمخازن الفردية والمجمعة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد منتجات معرفة. يرجى إضافتها من الفواتير -> إعداد المنتجات.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(products) { p ->
                            val lowStock = p.stock <= p.minStock
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${p.code} - ${p.name}", fontWeight = FontWeight.Bold)
                                        Text("متوسط التكلفة: ${formatAmount(p.cost)} ﷼ | القيمة الكلية: ${formatAmount(p.stock * p.cost)} ﷼", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${p.stock} وحدة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (lowStock) RoseCrimson else TealAccent
                                        )
                                        if (lowStock) {
                                            Text("مخزون منخفض!", fontSize = 10.sp, color = RoseCrimson, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Adjustment Form manual triggers
                Text("تسجيل تسوية جردية مخزنية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("ستقوم تلقائياً بتوليد قيد تساو ضائعات/فروقات للمخزون!", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                var expandedProdAdjustDdown by remember { mutableStateOf(false) }
                val prodLabel = products.find { it.id == selectedAdjustProductId }?.name ?: "اختر المنتج للتسوية"

                Box {
                    OutlinedButton(onClick = { expandedProdAdjustDdown = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(prodLabel)
                    }
                    DropdownMenu(expanded = expandedProdAdjustDdown, onDismissRequest = { expandedProdAdjustDdown = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedAdjustProductId = p.id
                                    expandedProdAdjustDdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = adjustQtyInput,
                    onValueChange = { adjustQtyInput = it },
                    label = { Text("قيمة التغيير (مثال: 5 للزيادة، -3 للعجز)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = adjustDescInput,
                    onValueChange = { adjustDescInput = it },
                    label = { Text("سبب التسوية (مثال: تلف جزء من الشحنة أو فروق جرد)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val pId = selectedAdjustProductId
                        val qM = adjustQtyInput.toDoubleOrNull() ?: 0.0
                        if (pId != null && qM != 0.0) {
                            onAdjustStock(pId, qM, adjustDescInput)
                            // clear
                            selectedAdjustProductId = null
                            adjustQtyInput = ""
                            adjustDescInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedAdjustProductId != null && adjustQtyInput.isNotEmpty()
                ) {
                    Text("ترحيل التسوية المخزنية ومزدوجة القيد")
                }
            }

            2 -> {
                Text("سجل حركات الوارد والصادر التفصيلي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (movements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد حركات مخزنية مسجلة حتى الآن.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(movements) { mv ->
                            val productLabel = products.find { it.id == mv.productId }?.name ?: "Unknown"
                            val isIncoming = mv.type == "PURCHASE" || mv.type == "ADJUSTMENT_IN"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(productLabel, fontWeight = FontWeight.Bold)
                                        Text("${mv.type} | التكلفة بالوحدة: ${formatAmount(mv.unitCost)} ﷼", fontSize = 11.sp, color = Color.Gray)
                                        Text("السبب: ${mv.description}", fontSize = 11.sp, color = Color.DarkGray)
                                        Text(formatDate(mv.date), fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Text(
                                        "${if (isIncoming) "+" else "-"}${mv.quantity} %s".format("وحدة"),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncoming) TealAccent else RoseCrimson,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 7: HR & PAYROLL (الموارد البشرية والرواتب وصرف النقد المالي)
// ----------------------------------------------------
@Composable
fun HrPayrollTab(
    employees: List<EmployeeEntity>,
    payrolls: List<PayrollRecordEntity>,
    accounts: List<AccountEntity>,
    onAddEmployee: (String, String, String, String, Double) -> Unit,
    onGeneratePayroll: (String, Double, Double) -> Unit,
    onPaySalary: (PayrollRecordEntity, Long) -> Unit
) {
    var hrViewToggle by remember { mutableStateOf(0) } // 0: Employees List, 1: Payroll Records

    // Add employee variables
    var empCode by remember { mutableStateOf("") }
    var empNameAr by remember { mutableStateOf("") }
    var empDeptAr by remember { mutableStateOf("") }
    var empPhone by remember { mutableStateOf("") }
    var empSalary by remember { mutableStateOf("") }

    // Generate Payroll variables
    var showGenerateDialog by remember { mutableStateOf(false) }
    var payrollMonth by remember { mutableStateOf("2026-05") }
    var allowancePct by remember { mutableStateOf("0") }
    var deductionPct by remember { mutableStateOf("0") }

    // Selected cash pay bank account reference
    val cashBankAccounts = accounts.filter { it.allowPosting && (it.code.startsWith("1101") || it.code.startsWith("1102")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = hrViewToggle, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = hrViewToggle == 0, onClick = { hrViewToggle = 0 }, text = { Text("الموظفين والملفات") })
            Tab(selected = hrViewToggle == 1, onClick = { hrViewToggle = 1 }, text = { Text("مسيرات كشوف الرواتب") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (hrViewToggle) {
            0 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("إدخال ملف موظف جديد", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                OutlinedTextField(value = empCode, onValueChange = { empCode = it }, label = { Text("كود الموظف (مثال: EMP01)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = empNameAr, onValueChange = { empNameAr = it }, label = { Text("الاسم الكامل للموظف") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = empDeptAr, onValueChange = { empDeptAr = it }, label = { Text("القسم الإداري") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = empPhone, onValueChange = { empPhone = it }, label = { Text("رقم الجوال") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = empSalary, onValueChange = { empSalary = it }, label = { Text("الراتب الأساسي الشهري (﷼)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                                Button(
                                    onClick = {
                                        if (empCode.isNotBlank() && empNameAr.isNotBlank()) {
                                            onAddEmployee(empCode, empNameAr, empDeptAr, empPhone, empSalary.toDoubleOrNull() ?: 3000.0)
                                            // clear
                                            empCode = ""
                                            empNameAr = ""
                                            empDeptAr = ""
                                            empPhone = ""
                                            empSalary = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ وتسجيل الموظف")
                                }
                            }
                        }
                    }

                    item { Text("المفات العمالية الحالية :", fontWeight = FontWeight.Bold) }

                    if (employees.isEmpty()) {
                        item { Text("لا يوجد موظفين مسجلين حالياً.", color = Color.Gray) }
                    } else {
                        items(employees) { emp ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${emp.code} - ${emp.name}", fontWeight = FontWeight.Bold)
                                        Text("القسم: ${emp.department} | جوال: ${emp.phone}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("${formatAmount(emp.basicSalary)} ﷼", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Payroll view with generate trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مسيرات الرواتب الشهرية المصدرة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Button(onClick = { showGenerateDialog = true }) {
                        Text("إنتاج المسير")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (payrolls.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد مسيرات رواتب مصدرة بعد.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(payrolls) { record ->
                            val empLabel = employees.find { it.id == record.employeeId }?.name ?: "Unknown"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(empLabel, fontWeight = FontWeight.Bold)
                                            Text("الشهر: ${record.month}", fontSize = 11.sp, color = Color.Gray)
                                            Text("أساسي: ${record.basic} | بدلات: +${record.allowance} | خصم: -${record.deductions}", fontSize = 11.sp, color = Color.LightGray)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${formatAmount(record.net)} ﷼", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)

                                            Spacer(modifier = Modifier.height(4.dp))

                                            if (record.status == "UNPAID") {
                                                // Dropdown selector custom for direct pay salary triggering
                                                var payExpandedDropdown by remember { mutableStateOf(false) }

                                                Box {
                                                    Button(
                                                        onClick = { payExpandedDropdown = true },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("صرف الراتب", fontSize = 10.sp)
                                                    }

                                                    DropdownMenu(expanded = payExpandedDropdown, onDismissRequest = { payExpandedDropdown = false }) {
                                                        DropdownMenuItem(
                                                            text = { Text("اختر خزنة سداد الصرف") },
                                                            onClick = {},
                                                            enabled = false
                                                        )
                                                        cashBankAccounts.forEach { acc ->
                                                            DropdownMenuItem(
                                                                text = { Text("${acc.code} ${acc.nameAr}") },
                                                                onClick = {
                                                                    onPaySalary(record, acc.id)
                                                                    payExpandedDropdown = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    color = Color(0xFFE6F4EA),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "تم الصرف بالكامل ✓",
                                                        color = Color(0xFF137333),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showGenerateDialog) {
                    AlertDialog(
                        onDismissRequest = { showGenerateDialog = false },
                        title = { Text("إنتاج مسير كشوفات الرواتب شهرياً", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = payrollMonth, onValueChange = { payrollMonth = it }, label = { Text("الشهر القياسي المستند (مثال: 2026-05)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = allowancePct, onValueChange = { allowancePct = it }, label = { Text("نسبة البدلات الإضافية الموحدة للموظفين (%)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = deductionPct, onValueChange = { deductionPct = it }, label = { Text("نسبة الخصومات الموحدة (%)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val al = allowancePct.toDoubleOrNull() ?: 0.0
                                    val de = deductionPct.toDoubleOrNull() ?: 0.0
                                    onGeneratePayroll(payrollMonth, al, de)
                                    showGenerateDialog = false
                                }
                            ) {
                                Text("إنشاء وتوليد الرواتب")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGenerateDialog = false }) { Text("إلغاء") }
                        }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 8: FINANCIAL REPORTS (الميزانية وقائمة الدخل)
// ----------------------------------------------------
@Composable
fun ReportsTab(
    incomeData: IncomeStatementData,
    balanceSheetData: BalanceSheetData,
    aiAnalysisResult: String?,
    onAnalyze: () -> Unit
) {
    var reportsModeToggle by remember { mutableStateOf(0) } // 0: Income, 1: Balance, 2: VAT & Zakat, 3: AI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScrollableTabRow(selectedTabIndex = reportsModeToggle, edgePadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = reportsModeToggle == 0, onClick = { reportsModeToggle = 0 }, text = { Text("قائمة الدخل", fontSize = 11.sp) })
            Tab(selected = reportsModeToggle == 1, onClick = { reportsModeToggle = 1 }, text = { Text("الميزانية", fontSize = 11.sp) })
            Tab(selected = reportsModeToggle == 2, onClick = { reportsModeToggle = 2 }, text = { Text("الزكاة والضرائب", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) })
            Tab(selected = reportsModeToggle == 3, onClick = { reportsModeToggle = 3 }, text = { Text("التحليل الذكي", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reportsModeToggle == 0) {
            // Income Statement: Revenues - Expenses = Net Profit
            Text("قائمة الدخل (الأرباح والخسائر) للفترة الجارية 2026", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Section 1: Revenue
                item {
                    Text("1. الإيرادات التشغيلية وأنشطة المبيعات", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                }

                if (incomeData.revenueAccounts.isEmpty()) {
                    item { Text("لا توجد إيرادات مسجلة بالدفاتر.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp)) }
                } else {
                    items(incomeData.revenueAccounts) { rev ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${rev.account.code} - ${rev.account.nameAr}", fontSize = 12.sp)
                            Text("${formatAmount(rev.netBalance)} ﷼", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي الإيرادات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${formatAmount(incomeData.totalRevenue)} ﷼", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Section 2: Expense
                item {
                    Text("2. المصروفات وتكلفة البضاعة والرواتب", fontWeight = FontWeight.Bold, color = RoseCrimson, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                }

                if (incomeData.expenseAccounts.isEmpty()) {
                    item { Text("لا توجد مصروفات مسجلة بالدفاتر.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp)) }
                } else {
                    items(incomeData.expenseAccounts) { exp ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exp.account.code} - ${exp.account.nameAr}", fontSize = 12.sp)
                            Text("${formatAmount(exp.netBalance)} ﷼", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المصروفات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${formatAmount(incomeData.totalExpense)} ﷼", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Profit Line
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (incomeData.netIncome >= 0) Color(0xFFE6F4EA) else Color(0xFFFCE8E6))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("صافي الربح / الخسارة", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text("${formatAmount(incomeData.netIncome)} ﷼", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (incomeData.netIncome >= 0) Color(0xFF137333) else Color(0xFFC5221F))
                    }
                }
            }
        } else if (reportsModeToggle == 1) {
            // Balance Sheet: Assets = Liabilities + Equity
            Text("الميزانية العمومية ومسرد المركز المالي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Assets
                item {
                    Text("الأصول وقيمة الممتلكات (Assets)", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                }

                if (balanceSheetData.assetAccounts.isEmpty()) {
                    item { Text("لا توجد أصول مسجلة.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp)) }
                } else {
                    items(balanceSheetData.assetAccounts) { asset ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${asset.account.code} - ${asset.account.nameAr}", fontSize = 12.sp)
                            Text("${formatAmount(asset.netBalance)} ﷼", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F0FE)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي الأصول", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${formatAmount(balanceSheetData.totalAssets)} ﷼", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Liabilities
                item {
                    Text("الخصوم والالتزامات للغير (Liabilities)", fontWeight = FontWeight.Bold, color = RoseCrimson, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                }

                if (balanceSheetData.liabilityAccounts.isEmpty()) {
                    item { Text("لا توجد التزامات مسجلة.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp)) }
                } else {
                    items(balanceSheetData.liabilityAccounts) { liability ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${liability.account.code} - ${liability.account.nameAr}", fontSize = 12.sp)
                            Text("${formatAmount(liability.netBalance)} ﷼", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي الخصوم والذمم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${formatAmount(balanceSheetData.totalLiabilities)} ﷼", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Equity
                item {
                    Text("حقوق الملكية ورأس المال والأرباح المحتجزة", fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                }

                if (balanceSheetData.equityAccounts.isEmpty()) {
                    item { Text("لا توجد حقوق مسجلة.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp)) }
                } else {
                    items(balanceSheetData.equityAccounts) { eq ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${eq.account.code} - ${eq.account.nameAr}", fontSize = 12.sp)
                            Text("${formatAmount(eq.netBalance)} ﷼", fontSize = 12.sp)
                        }
                    }
                }

                // Retained Profit line
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("أرباح (خسائر) العام الجاري", fontSize = 12.sp)
                        Text("${formatAmount(incomeData.netIncome)} ﷼", fontSize = 12.sp)
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF7E0)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي حقوق الملكية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${formatAmount(balanceSheetData.totalEquity)} ﷼", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Check Equation verification block
                val balanceVerified = Math.abs(balanceSheetData.totalAssets - (balanceSheetData.totalLiabilities + balanceSheetData.totalEquity)) < 0.01
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (balanceVerified) Color(0xFFE6F4EA) else Color(0xFFFEEFC3))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("توازن الميزانية (أصول = خصوم + حقوق)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("أصول: ${formatAmount(balanceSheetData.totalAssets)} ﷼ | خصوم وحقوق: ${formatAmount(balanceSheetData.totalLiabilities + balanceSheetData.totalEquity)} ﷼", fontSize = 10.sp, color = Color.Gray)
                        }
                        Surface(
                            color = if (balanceVerified) Color(0xFF137333) else Color(0xFFB06000),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (balanceVerified) "متطابق ✓" else "قيد العمل",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else if (reportsModeToggle == 2) {
            // Zakat & VAT tax report (Saudi Arabia localization)
            val vatOutput = Math.abs(balanceSheetData.liabilityAccounts.find { it.account.code == "2201" }?.netBalance ?: 0.0)
            val vatInput = Math.abs(balanceSheetData.assetAccounts.find { it.account.code == "1401" }?.netBalance ?: 0.0)
            val netVatPayable = vatOutput - vatInput

            val capital = Math.abs(balanceSheetData.equityAccounts.find { it.account.code == "3101" }?.netBalance ?: 0.0)
            val zakatProvision = Math.abs(balanceSheetData.liabilityAccounts.find { it.account.code == "2301" }?.netBalance ?: 0.0)
            val zakatBase = Math.max(0.0, capital + incomeData.netIncome + zakatProvision)
            val zakatAmt = zakatBase * 0.025

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("تقرير الزكاة وضريبة القيمة المضافة (ZATCA KSA)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("الإقرارات والتقديرات المحاسبية للفترة الجارية 2026", fontSize = 11.sp, color = Color.Gray)
                }

                // 1. VAT Section
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ضريبة القيمة المضافة (VAT - 15٪)", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 14.sp)
                                Surface(
                                    color = if (netVatPayable >= 0) Color(0xFFFCE8E6) else Color(0xFFE6F4EA),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        if (netVatPayable >= 0) "مستحق السداد" else "رصيد دائن مسترد",
                                        color = if (netVatPayable >= 0) Color(0xFFC5221F) else Color(0xFF137333),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ضريبة المخرجات (المبيعات) - 2201:", fontSize = 12.sp)
                                Text("${formatAmount(vatOutput)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ضريبة المدخلات (المشتريات) - 1401:", fontSize = 12.sp)
                                Text("${formatAmount(vatInput)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("صافي ضريبة القيمة المضافة للإقرار الكلي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${formatAmount(Math.abs(netVatPayable))} ﷼", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (netVatPayable >= 0) RoseCrimson else TealAccent)
                            }
                        }
                    }
                }

                // 2. Zakat Section
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("تقديرات الزكاة الشرعية (2.5٪)", fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("محسوبة طبقاً للمتطلبات التنظيمية لهيئة الزكاة والضريبة والجمارك.", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("رأس المال المدرج والمثبت - 3101:", fontSize = 12.sp)
                                Text("${formatAmount(capital)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("صافي ربح الفترة الخاضع لأوعية الاستقطاع:", fontSize = 12.sp)
                                Text("${formatAmount(incomeData.netIncome)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مخصصات زكوية وضريبية قائمة - 2301:", fontSize = 12.sp)
                                Text("${formatAmount(zakatProvision)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الوعاء الزكوي التقريبي الكلي للفترة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${formatAmount(zakatBase)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الزكاة الشرعية المستحقة المتوقعة (2.5٪):", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${formatAmount(zakatAmt)} ﷼", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AmberGold)
                            }
                        }
                    }
                }
            }
        } else if (reportsModeToggle == 3) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text("المساعد المحاسبي الذكي", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("مدعوم بتقنية Gemini لتحليل الأداء المالي بلمسة واحدة.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بدء التحليل المالي", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (aiAnalysisResult != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("نتيجة التحليل والاستنتاجات:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(aiAnalysisResult, fontSize = 14.sp, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Formatting Utilities
// ----------------------------------------------------
fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return format.format(date)
}
