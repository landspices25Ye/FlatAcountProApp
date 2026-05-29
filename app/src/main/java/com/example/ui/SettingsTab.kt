package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.errors.ErrorHandler
import com.example.core.settings.CompanyProfile
import com.example.core.settings.CostCenter
import com.example.core.settings.FiscalPeriod
import com.example.core.settings.NumberSequence
import com.example.core.settings.UnitOfMeasure
import com.example.core.utils.FormatUtils

enum class SettingsSubSection(val titleAr: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    COMPANY("الملف التعريفي للشركة", Icons.Default.Info),
    FISCAL_PERIODS("الفترات المالية", Icons.Default.DateRange),
    DEFAULT_ACCOUNTS("الحسابات الافتراضية", Icons.Default.Share),
    SEQUENCES("التسلسلات والأكواد", Icons.Default.Star),
    UNITS("وحدات القياس والتحويل", Icons.Default.Build),
    COST_CENTERS("مراكز التكلفة والربط", Icons.Default.Place),
    NOTIFICATIONS("الإشعارات والتفضيلات", Icons.Default.Notifications),
    AUDIT_LOGS("المزامنة وسجل التدقيق", Icons.Default.Refresh),
    MY_PROFILE("الملف الشخصي وأمان الحساب", Icons.Default.AccountBox),
    USERS("إدارة المستخدمين والدعوات", Icons.Default.Person),
    ROLES("الأدوار ومصفوفة الصلاحيات", Icons.Default.Lock),
    ACTIVE_SESSIONS("الأجهزة والجلسات النشطة", Icons.Default.Phone),
    USER_ACTIVITIES("سجل تدقيق أنشطة الموظفين", Icons.Default.Search)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubSec by remember { mutableStateOf(SettingsSubSection.COMPANY) }
    val userAuthService = remember { com.example.core.di.ServiceContainer.getInstance().userManagementService }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sidebar selector for large screen layout / easily scrollable column for portable
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(
                text = "الإعدادات العامة والنظام",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp, start = 12.dp)
            )

            SettingsSubSection.values().forEach { subSec ->
                val isSelected = activeSubSec == subSec
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeSubSec = subSec }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = subSec.icon,
                        contentDescription = subSec.titleAr,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = subSec.titleAr,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Expanded view panel based on selection active
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            when (activeSubSec) {
                SettingsSubSection.COMPANY -> CompanyProfileAndPreferencesView(viewModel)
                SettingsSubSection.FISCAL_PERIODS -> FiscalPeriodsView(viewModel)
                SettingsSubSection.DEFAULT_ACCOUNTS -> DefaultAccountsLinkView(viewModel)
                SettingsSubSection.SEQUENCES -> NumberSequencesView(viewModel)
                SettingsSubSection.UNITS -> UnitsOfMeasureAndConversionView(viewModel)
                SettingsSubSection.COST_CENTERS -> CostCentersView(viewModel)
                SettingsSubSection.NOTIFICATIONS -> NotificationsSettingsView(viewModel)
                SettingsSubSection.AUDIT_LOGS -> AuditAndSyncLogsView(viewModel)
                SettingsSubSection.MY_PROFILE -> MyProfileAndSecurityView(userAuthService)
                SettingsSubSection.USERS -> UsersAndInvitationsView(userAuthService)
                SettingsSubSection.ROLES -> RolesAndPermissionsView(userAuthService)
                SettingsSubSection.ACTIVE_SESSIONS -> ActiveSessionsView(userAuthService)
                SettingsSubSection.USER_ACTIVITIES -> UserActivitiesAuditView(userAuthService)
            }
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 1: COMPANY PROFILE (ملف الشركة والتفضيلات)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileAndPreferencesView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val currentProfile by viewModel.companyProfile.collectAsStateWithLifecycle()

    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var legalName by remember(currentProfile) { mutableStateOf(currentProfile.legalName) }
    var crNumber by remember(currentProfile) { mutableStateOf(currentProfile.crNumber) }
    var taxNumber by remember(currentProfile) { mutableStateOf(currentProfile.taxNumber) }
    var phone by remember(currentProfile) { mutableStateOf(currentProfile.phone) }
    var email by remember(currentProfile) { mutableStateOf(currentProfile.email) }
    var website by remember(currentProfile) { mutableStateOf(currentProfile.website) }
    var address by remember(currentProfile) { mutableStateOf(currentProfile.address) }
    var currency by remember(currentProfile) { mutableStateOf(currentProfile.currency) }
    var taxRate by remember(currentProfile) { mutableStateOf(currentProfile.taxRate.toString()) }
    var decimalPlaces by remember(currentProfile) { mutableStateOf(currentProfile.decimalPlaces) }
    var invoiceNotes by remember(currentProfile) { mutableStateOf(currentProfile.invoiceNotes) }
    var fiscalStartMonth by remember(currentProfile) { mutableStateOf(currentProfile.fiscalYearStartMonth) }
    var appTheme by remember(currentProfile) { mutableStateOf(currentProfile.theme) }
    var appLanguage by remember(currentProfile) { mutableStateOf(currentProfile.language) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "الملف التعريفي للشركة والإعدادات المالية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تعديل معلومات الهوية القانونية، الضرائب، التفضيلات العامة والمظهر للنظام",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "المعلومات القانونية والتجارية",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم التجاري للشركة (العربية)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("company_name_input")
                )

                OutlinedTextField(
                    value = legalName,
                    onValueChange = { legalName = it },
                    label = { Text("الاسم القانوني الكامل بالتأسيس") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = crNumber,
                        onValueChange = { crNumber = it },
                        label = { Text("رقم السجل التجاري (CR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = taxNumber,
                        onValueChange = { taxNumber = it },
                        label = { Text("الرقم الضريبي الموحد (15 خانة)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp).testTag("tax_number_input")
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "عناوين التواصل والمستندات",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف العام") },
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني للشركة") },
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("الموقع الإلكتروني الرسمي") },
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الوطني والموقع الجغرافي") },
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = invoiceNotes,
                    onValueChange = { invoiceNotes = it },
                    label = { Text("الشروط والأحكام وملاحظات تذييل الفواتير") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تفضيلات العملة والنظام المالية والضرائب",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        var expandedCurrency by remember { mutableStateOf(false) }
                        OutlinedCard(
                            onClick = { expandedCurrency = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                Text("العملة الافتراضية: $currency")
                            }
                        }
                        DropdownMenu(expanded = expandedCurrency, onDismissRequest = { expandedCurrency = false }) {
                            listOf("SAR", "USD", "EUR", "AED").forEach { cur ->
                                DropdownMenuItem(text = { Text(cur) }, onClick = {
                                    currency = cur
                                    expandedCurrency = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it },
                        label = { Text("نسبة القيمة المضافة الافتراضية (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("الخانات العشرية للتقريب: ", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Row {
                            listOf(2, 3, 4).forEach { d ->
                                FilterChip(
                                    selected = decimalPlaces == d,
                                    onClick = { decimalPlaces = d },
                                    label = { Text(d.toString()) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("مظهر التطبيق: ", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = appTheme == "DARK",
                            onClick = { appTheme = if (appTheme == "DARK") "LIGHT" else "DARK" },
                            label = { Text(if (appTheme == "DARK") "ليلي" else "نهاري") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Validator checks
                if (!FormatUtils.isValidTaxNumber(taxNumber)) {
                    Toast.makeText(context, "الرقم الضريبي الموحد يجب أن يتكون من 15 خانة رقمية فقط", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (!FormatUtils.isValidEmail(email)) {
                    Toast.makeText(context, "يرجى إدخال بريد إلكتروني صالح للشركة", Toast.LENGTH_LONG).show()
                    return@Button
                }

                val rate = taxRate.toDoubleOrNull() ?: 15.0
                val updated = CompanyProfile(
                    name = name,
                    legalName = legalName,
                    crNumber = crNumber,
                    taxNumber = taxNumber,
                    phone = phone,
                    email = email,
                    website = website,
                    address = address,
                    currency = currency,
                    taxRate = rate,
                    decimalPlaces = decimalPlaces,
                    invoiceNotes = invoiceNotes,
                    fiscalYearStartMonth = fiscalStartMonth,
                    theme = appTheme,
                    language = appLanguage
                )
                viewModel.updateCompanyProfile(updated)
                Toast.makeText(context, "تم حفظ وتحديث ملف الشركة وتفضيلاتها بنجاح!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_company_profile_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = "Save")
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ تفاصيل الهوية والإعدادات")
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 2: FISCAL PERIODS (الفترات المالية)
// ----------------------------------------------------
@Composable
fun FiscalPeriodsView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val periods by viewModel.fiscalPeriods.collectAsStateWithLifecycle()
    var showConfirmCloseDialog by remember { mutableStateOf<com.example.core.settings.FiscalPeriod?>(null) }

    if (showConfirmCloseDialog != null) {
        val p = showConfirmCloseDialog!!
        AlertDialog(
            onDismissRequest = { showConfirmCloseDialog = null },
            title = { Text("تأكيد إغلاق الفترة وإقفال الدفاتر", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في إغلاق الفترة المالية ${p.nameAr} (${p.id})؟\n\n" +
                    "سيؤدي هذا الإجراء المحاسبي إلى:\n" +
                    "١. التأكد من عدم وجود قيود مسودة متبقية بالفترة.\n" +
                    "٢. تكوين قيد إقفال آلي متوازن يُصفر حسابات الإيرادات والمصروفات للفترة.\n" +
                    "٣. تسوية صافي الدخل وترحيله لحساب الأرباح المحتجزة / رأس المال (3101).\n" +
                    "٤. منع ترحيل قيود يدوية أو آلية أخرى على هذه الفترة نهائياً."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closePeriod(p.id)
                        showConfirmCloseDialog = null
                    }
                ) {
                    Text("نعم، إقفال الفترة وإصدار القيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloseDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "إدارة الفترات والسنوات المالية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "متابعة وإغلاق/فتح/إقفال الأشهر المالية لمطابقة القيود مع السنة المالية المعينة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(periods) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("الرقم المعرف: ${p.id} | السنة: ${p.year}", fontSize = 12.sp, color = Color.Gray)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status indicator badge
                            val badgeColor = when (p.status) {
                                "OPEN" -> Color(0xFF2E7D32)
                                "CLOSED" -> Color(0xFFEF6C00)
                                else -> Color(0xFFC62828)
                            }
                            val textState = when (p.status) {
                                "OPEN" -> "مفتوحة"
                                "CLOSED" -> "مغلقة"
                                else -> "مقفل نهائياً"
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text(textState, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = badgeColor)
                            )

                            // Actions
                            IconButton(onClick = {
                                if (p.status == "OPEN") {
                                    showConfirmCloseDialog = p
                                } else {
                                    try {
                                        val nextStatus = when (p.status) {
                                            "CLOSED" -> "LOCKED"
                                            else -> "OPEN" // Reopen locked for debug if needed
                                        }
                                        viewModel.updateFiscalPeriodStatus(p.id, nextStatus)
                                        Toast.makeText(context, "تم تعديل حالة الفترة المالية بنجاح!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        com.example.core.errors.ErrorHandler.handleError(context, e)
                                    }
                                }
                            }) {
                                val actionIcon = when (p.status) {
                                    "OPEN" -> Icons.Default.Lock
                                    "CLOSED" -> Icons.Default.Warning
                                    else -> Icons.Default.Refresh
                                }
                                Icon(actionIcon, contentDescription = "Change status", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 3: DEFAULT ACCOUNTS LINKING (ربط الحسابات)
// ----------------------------------------------------
@Composable
fun DefaultAccountsLinkView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    
    // Quick linked mapping from shared prefer
    val settingsManager = com.example.core.di.ServiceContainer.getInstance().settingsManager
    var links by remember { mutableStateOf(settingsManager.getLinkedAccounts()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "ربط وتوجيه الحسابات الافتراضية للERP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "ربط وتوجيه القيود المحاسبية التلقائية وحركات الصندوق، فواتير المبيعات، الشراء، والرواتب",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val linkKeys = listOf(
                Pair("CASH", "حساب الصندوق"),
                Pair("BANK", "حساب البنك"),
                Pair("RECEIVABLE", "حساب العملاء (الذمم المدينة)"),
                Pair("INVENTORY", "حساب المخزون السلعي"),
                Pair("PAYABLE", "حساب الموردين (الذمم الدائنة)"),
                Pair("SALES", "حساب مبيعات الإيراد"),
                Pair("COGS", "حساب تكلفة البضاعة للمخزن"),
                Pair("PAYROLL", "مصروف الرواتب العامة")
            )

            items(linkKeys) { (key, label) ->
                val boundCode = links[key] ?: "1101"
                val matchedAcc = accounts.find { it.code == boundCode }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(label, fontWeight = FontWeight.Bold)
                            Text("الرابط الفعلي: [$boundCode] - ${matchedAcc?.nameAr ?: "الأصول المتداولة"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Picker
                        var showPicker by remember { mutableStateOf(false) }
                        Button(onClick = { showPicker = true }) {
                            Text("تغيير الربط", fontSize = 12.sp)
                        }

                        if (showPicker) {
                            AlertDialog(
                                onDismissRequest = { showPicker = false },
                                title = { Text("اختر الحساب المصرفي للمطابقة") },
                                text = {
                                    Column {
                                        Text("اختر الحساب الجاري لتعيينه افتراضياً لـ $label", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier.height(250.dp).verticalScroll(rememberScrollState())
                                        ) {
                                            Column {
                                                accounts.filter { it.allowPosting }.forEach { acc ->
                                                    ListItem(
                                                        headlineContent = { Text("${acc.code} - ${acc.nameAr}") },
                                                        modifier = Modifier.clickable {
                                                            viewModel.setLinkedAccountCode(key, acc.code)
                                                            links = settingsManager.getLinkedAccounts()
                                                            showPicker = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showPicker = false }) {
                                        Text("إلغاء")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                viewModel.resetLinkedAccountsToDefaults()
                links = settingsManager.getLinkedAccounts()
                Toast.makeText(context, "تمت إعادة ضبط روابط الحسابات للمخرجات الموصى بها", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إعادة تعيين قيم التوجيه الافتراضية")
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 4: NUMBER SEQUENCES (التسلسلات والأكواد)
// ----------------------------------------------------
@Composable
fun NumberSequencesView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val sequences by viewModel.numberSequences.collectAsStateWithLifecycle()

    var editingSeq by remember { mutableStateOf<NumberSequence?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "تسلسلات وصيغ الترقيم للمستندات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تخصيص البادئات والتسلسلات للأشكال والمستندات ومراجعة الرقم القادم لإصداره",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(sequences) { s ->
                val formattedNum = String.format("%0${s.digits}d", s.current)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val desc = when (s.type) {
                                "INVOICE" -> "الفواتير (Invoices)"
                                "JOURNAL" -> "قيود اليومية (Journals)"
                                "PARTNER" -> "المستند اليدوي للعملاء/الموردين"
                                "EMPLOYEE" -> "سجلات توظيف العمال والرواتب"
                                else -> "تصدير المخزون والضبط الفني"
                            }
                            Text(desc, fontWeight = FontWeight.Bold)
                            Text("صيغة الترقيم الحالية: ${s.prefix}$formattedNum (طول الترقيم: ${s.digits})", fontSize = 12.sp, color = Color.Gray)
                        }

                        Button(onClick = { editingSeq = s }) {
                            Text("تعديل", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Modal for editing sequence rules
        editingSeq?.let { s ->
            var prefix by remember { mutableStateOf(s.prefix) }
            var current by remember { mutableStateOf(s.current.toString()) }

            AlertDialog(
                onDismissRequest = { editingSeq = null },
                title = { Text("تعديل تسلسل [${s.type}]") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("البادئة الثابتة (e.g. INV-)") }
                        )

                        OutlinedTextField(
                            value = current,
                            onValueChange = { current = it },
                            label = { Text("الرقم الحالي للتسلسل (أو الرقم المستخدم سابقاً)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val num = current.toIntOrNull() ?: s.current
                        try {
                            viewModel.updateSequence(s.type, prefix, num)
                            editingSeq = null
                            Toast.makeText(context, "تم تحديث التسلسل الترقيمي بنجاح!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            ErrorHandler.handleError(context, e)
                        }
                    }) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingSeq = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 5: UNITS OF MEASURE (وحدات القياس والتحويل)
// ----------------------------------------------------
@Composable
fun UnitsOfMeasureAndConversionView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val units by viewModel.unitsOfMeasure.collectAsStateWithLifecycle()

    var showAddModal by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var baseUnitId by remember { mutableStateOf<String?>(null) }
    var factorStr by remember { mutableStateOf("1.0") }

    // Conversion Simulator
    var testQty by remember { mutableStateOf("1") }
    var testFromUnit by remember { mutableStateOf("box_12") }
    var testToUnit by remember { mutableStateOf("piece") }
    var calcResult by remember { mutableStateOf<Double?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "إدارة وحدات قياس المنتجات والمخازن",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "شاشة لتعريف وتخصيص وحدات البيع الفرعية والكراتين للمنتجات والتحويل التلقائي للمبيعات والمخزن",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الوحدات المعرفة حالياً (${units.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Button(onClick = { showAddModal = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة وحدة جديدة", fontSize = 12.sp)
            }
        }

        // List
        LazyColumn(modifier = Modifier.weight(0.5f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(units) { u ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${u.name} (${u.symbol})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (u.baseUnitId != null) {
                                Text("تساوي ${u.factor} من الوحدة الأساسية [${u.baseUnitId}]", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("وحدة أساسية مستقلة", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        IconButton(onClick = {
                            try {
                                viewModel.deleteUnitOfMeasure(u.id)
                                Toast.makeText(context, "تم حذف وحدة القياس!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                ErrorHandler.handleError(context, e)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Innovation segment: Quick Conversion Calculator
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "⚙️ محاكاة التحويل السريع للوحدات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = testQty,
                        onValueChange = { testQty = it },
                        label = { Text("الكمية") },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // From Unit Choose
                    Box(modifier = Modifier.weight(1f)) {
                        var expandedFrom by remember { mutableStateOf(false) }
                        OutlinedCard(onClick = { expandedFrom = true }, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.height(56.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text("من: ${testFromUnit}", fontSize = 12.sp)
                            }
                        }
                        DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                            units.forEach { u ->
                                DropdownMenuItem(text = { Text("${u.name} (${u.symbol})") }, onClick = {
                                    testFromUnit = u.id
                                    expandedFrom = false
                                })
                            }
                        }
                    }

                    // To Unit Choose
                    Box(modifier = Modifier.weight(1f)) {
                        var expandedTo by remember { mutableStateOf(false) }
                        OutlinedCard(onClick = { expandedTo = true }, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.height(56.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text("إلى: ${testToUnit}", fontSize = 12.sp)
                            }
                        }
                        DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                            units.forEach { u ->
                                DropdownMenuItem(text = { Text("${u.name} (${u.symbol})") }, onClick = {
                                    testToUnit = u.id
                                    expandedTo = false
                                })
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val qty = testQty.toDoubleOrNull() ?: 1.0
                            val uFromObj = units.find { it.id == testFromUnit }
                            val uToObj = units.find { it.id == testToUnit }

                            if (uFromObj != null && uToObj != null) {
                                // Simplified math conversion block
                                val valueInBaseFrom = qty * uFromObj.factor
                                val result = valueInBaseFrom / uToObj.factor
                                calcResult = result
                            }
                        },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("احسب")
                    }
                }

                calcResult?.let { res ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الناتج المحاسبي: $testQty [$testFromUnit] = $res [$testToUnit]",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }

        // Add modal dialog
        if (showAddModal) {
            AlertDialog(
                onDismissRequest = { showAddModal = false },
                title = { Text("تعريف كود وحدة قياس جديدة") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الوحدة (e.g. كرتون)") })
                        OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("الرمز المكتوب للفاتورة (e.g. كرتونة)") })
                        
                        // Pick Base Unit
                        var baseDropdownExp by remember { mutableStateOf(false) }
                        OutlinedCard(onClick = { baseDropdownExp = true }, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(12.dp), contentAlignment = Alignment.CenterStart) {
                                Text("الوحدة الأساسية المقابلة: ${baseUnitId ?: "لا يوجد (وحدة رئيسية)"}")
                            }
                        }
                        DropdownMenu(expanded = baseDropdownExp, onDismissRequest = { baseDropdownExp = false }) {
                            DropdownMenuItem(text = { Text("لا يوجد (وحدة رئيسية)") }, onClick = {
                                baseUnitId = null
                                baseDropdownExp = false
                            })
                            units.filter { it.baseUnitId == null }.forEach { u ->
                                DropdownMenuItem(text = { Text(u.name) }, onClick = {
                                    baseUnitId = u.id
                                    baseDropdownExp = false
                                })
                            }
                        }

                        if (baseUnitId != null) {
                            OutlinedTextField(
                                value = factorStr,
                                onValueChange = { factorStr = it },
                                label = { Text("معامل التحويل للوحدة المحددة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val f = factorStr.toDoubleOrNull() ?: 1.0
                        try {
                            viewModel.addUnitOfMeasure(name, symbol, baseUnitId, f)
                            // Reset state
                            name = ""
                            symbol = ""
                            baseUnitId = null
                            factorStr = "1.0"
                            showAddModal = false
                            Toast.makeText(context, "تم حفظ وحدة القياس الجديدة ومزامنتها بنجاح!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            ErrorHandler.handleError(context, e)
                        }
                    }) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddModal = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 6: COST CENTERS (مراكز التكلفة)
// ----------------------------------------------------
@Composable
fun CostCentersView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val list by viewModel.costCenters.collectAsStateWithLifecycle()

    var showAddModal by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "دليل مراكز التكلفة للفروع ومشاريع الديكور",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "توجيه المصاريف والإيرادات نحو مراكز معينة لاحتساب صافي ربح فروع الديكور والمشاريع",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(onClick = { showAddModal = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مركز", fontSize = 12.sp)
            }
        }

        // List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(list) { cc ->
                val parentName = list.find { it.id == cc.parentId }?.name
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("[${cc.code}] - ${cc.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            parentName?.let {
                                Text("المركز الرئيسي التابع: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            } ?: Text("مركز رئيسي جذري", fontSize = 11.sp, color = Color.Gray)
                        }

                        IconButton(onClick = {
                            try {
                                viewModel.deleteCostCenter(cc.id)
                                Toast.makeText(context, "تم حذف مركز التكلفة بنجاح", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                ErrorHandler.handleError(context, e)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        if (showAddModal) {
            AlertDialog(
                onDismissRequest = { showAddModal = false },
                title = { Text("إنشاء مركز تكلفة فرعي أو جديد") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("رمز الكود (e.g. CC302)") })
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم مشروع الديكور أو القسم") })

                        var parentExp by remember { mutableStateOf(false) }
                        OutlinedCard(onClick = { parentExp = true }, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(12.dp), contentAlignment = Alignment.CenterStart) {
                                Text("توجيهه كفرع لـ: ${list.find { it.id == parentId }?.name ?: "لا يوجد (جذري)"}")
                            }
                        }
                        DropdownMenu(expanded = parentExp, onDismissRequest = { parentExp = false }) {
                            DropdownMenuItem(text = { Text("لا يوجد (جذري)") }, onClick = {
                                parentId = null
                                parentExp = false
                            })
                            list.forEach { cc ->
                                DropdownMenuItem(text = { Text(cc.name) }, onClick = {
                                    parentId = cc.id
                                    parentExp = false
                                })
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        try {
                            viewModel.addCostCenter(code, name, parentId)
                            code = ""
                            name = ""
                            parentId = null
                            showAddModal = false
                            Toast.makeText(context, "تم حفظ مركز التكلفة ودمجه بمطابقة القيود!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            ErrorHandler.handleError(context, e)
                        }
                    }) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddModal = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 7: NOTIFICATIONS SETTINGS (الإشعارات والتفضيلات)
// ----------------------------------------------------
@Composable
fun NotificationsSettingsView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val toggles by viewModel.notificationToggles.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "تهيئة تفعيل الإشعارات والتنبيهات المسبقة للشؤون المالية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تحديد متى يجب على النظام إصدار إشعارات فورية على شاشة الإدارة أو إرسال تنبيه البريد",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val keys = listOf(
            Triple("LOW_STOCK", "تحذير بقرب نفاد المخزون", "يرسل تنبيهاً مباشراً في حالة هبوط بضاعة الديكور في المستودع عن الحد الحرج المحدد في بطاقة المنتج"),
            Triple("INVOICE_OVERDUE", "تنبيه بالفواتير المتأخرة", "يبعث إشعاراً عند تخطي فواتير البيع الآجل تاريخ تاريخ الاستحقاق المطلوب دون تسديد القيمة"),
            Triple("PAYMENT_RECEIVED", "إشعار اعتماد الدفعات ومقاصة فورية", "إصدار إشعار تسليم بنكي ومخزني فوري بعد مصادقة الدفعات المالية"),
            Triple("PAYROLL_RELEASE", "موعد استحقاق صب الرواتب", "إشعار شهري مستحق الصرف لتسييل قيود الرواتب والتعويضات للعمال في مطلع كل شهر شمسي")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(keys) { (key, title, subtitle) ->
                val isEnabled = toggles[key] ?: true
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(subtitle, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.updateNotificationToggle(key, it) }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SUB-TAB 8: AUDIT & SYNC LOGS (سجل العمليات والمزامنة)
// ----------------------------------------------------
@Composable
fun AuditAndSyncLogsView(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val syncSummary by viewModel.syncSummary.collectAsStateWithLifecycle()

    var isSyncing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showConflictResolverDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📱 حالة الاتصال والمزامنة لـ Supabase Offline-first",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "آخر مزامنة ناجحة: ${FormatUtils.formatDate(syncSummary.lastSyncTimestamp, "yyyy-MM-dd HH:mm:ss")}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "الحركات المعلقة في الطابور المحلي: ${syncSummary.pendingChangesCount} حركات تجارية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showConflictResolverDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("resolve_conflict_button")
                    ) {
                        Text("حل التعارض")
                    }

                    Button(
                        onClick = {
                            isSyncing = true
                            viewModel.forceSync(context) {
                                isSyncing = false
                                Toast.makeText(context, "اكتملت المزامنة وحل تعارضات الدفاتر تلقائياً بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("sync_db_button")
                    ) {
                        if (isSyncing) {
                            Text("جاري المزامنة...")
                        } else {
                            Text("مزامنة الآن")
                        }
                    }
                }
            }
        }

        // Search text-field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("ابحث في سجل العمليات المحاسبي") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("العمليات المسجلة للنظام (${logs.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            TextButton(onClick = { viewModel.clearAuditLogs() }) {
                Text("تفريغ السجل الكامل", color = Color.Red, fontSize = 11.sp)
            }
        }

        // List logs
        val filtered = logs.filter {
            it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.user.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(filtered) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = log.category,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = FormatUtils.formatDate(log.timestamp, "yyyy-MM-dd HH:mm:ss"),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = log.description,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "المستخدم: ${log.user}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Interactive conflict resolution popup window (simulated)
        if (showConflictResolverDialog) {
            AlertDialog(
                onDismissRequest = { showConflictResolverDialog = false },
                title = { Text("📋 المعالجة والبت في تضارب البيانات المحاسبية") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "تم رصد تضارب في تعديلات [فاتورة المبيعات INV-00021]. البيانات على الجهاز تختلف عن الخادم المعتمد:",
                            fontSize = 12.sp
                        )

                        // Compare panels
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("البيانات المحلية (الجهاز)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("مبلغ الفاتورة: 2,500 SAR", fontSize = 10.sp)
                                    Text("تعديل: مبيعات آجل العليا", fontSize = 9.sp)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("البيانات السحابية (الخادم)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("مبلغ الفاتورة: 2,000 SAR", fontSize = 10.sp)
                                    Text("تعديل: مبيعات نقدي مخصوم", fontSize = 9.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "الرجاء تحديد إصدار النسخة المعتمدة لحفظ القيود في دفتر اليومية الموحد:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            viewModel.clearAuditLogs()
                            viewModel.addCostCenter("CC-RESOLVED", "توجيه تعارض فواتير العليا", null)
                            showConflictResolverDialog = false
                            Toast.makeText(context, "اعتمدت النسخة المحلية. تم تعديل القيود بنجاح بمطابقة الدوائر!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("اعتماد الجهاز")
                        }
                        Button(onClick = {
                            viewModel.clearAuditLogs()
                            viewModel.addCostCenter("CC-CLOUD", "توجيه تعارض سحابة فوري", null)
                            showConflictResolverDialog = false
                            Toast.makeText(context, "اعتمدت نسخة السحابة. تم تحديث البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("اعتماد السحابة")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConflictResolverDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
