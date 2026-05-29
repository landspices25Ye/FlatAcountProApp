package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.core.utils.FormatUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesTab(
    viewModel: AccountingViewModel,
    partners: List<PartnerEntity>,
    products: List<ProductEntity>,
    accounts: List<AccountEntity>,
    purchaseOrders: List<PurchaseOrderWithOwner>,
    purchaseInvoices: List<PurchaseInvoiceWithOwner>,
    purchaseReturns: List<PurchaseReturnWithOwner>,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(0) } // 0: Suppliers, 1: Orders, 2: Invoices, 3: Returns
    val subTabTitles = listOf("الموردين والحسابات", "أوامر الشراء", "فواتير المشتريات", "مرتجع المشتريات")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Purchases KPI Summary Row ---
        PurchasesSummarySection(purchaseInvoices, purchaseReturns)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Tab Selection Navigation ---
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            subTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Render Sub-tab Content ---
        when (subTab) {
            0 -> SuppliersSubTab(viewModel, partners, accounts)
            1 -> PurchaseOrdersSubTab(viewModel, partners, products)
            2 -> PurchaseInvoicesSubTab(viewModel, partners, products, accounts, purchaseInvoices)
            3 -> PurchaseReturnsSubTab(viewModel, purchaseInvoices, products)
        }
    }
}

@Composable
fun PurchasesSummarySection(
    invoices: List<PurchaseInvoiceWithOwner>,
    returns: List<PurchaseReturnWithOwner>
) {
    val postedInvoices = invoices.filter { it.invoice.status == "POSTED" }
    val totalPurchases = postedInvoices.sumOf { it.invoice.grandTotal }
    val totalPaid = postedInvoices.sumOf { it.invoice.paidAmount }
    val totalUnpaid = postedInvoices.sumOf { it.invoice.grandTotal - it.invoice.paidAmount }
    val totalReturns = returns.sumOf { it.returnEntity.refundAmount }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            title = "إجمالي المشتريات",
            amount = totalPurchases,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "مسدد دفوعات",
            amount = totalPaid,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "ذمم دائنة مستحقة",
            amount = totalUnpaid,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "مرتجع مشتريات",
            amount = totalReturns,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = FormatUtils.formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ==========================================
// --- SUB TAB 1: SUPPLIERS & STATEMENTS ---
// ==========================================
@Composable
fun SuppliersSubTab(
    viewModel: AccountingViewModel,
    partners: List<PartnerEntity>,
    accounts: List<AccountEntity>
) {
    val suppliers = partners.filter { it.type == "SUPPLIER" }
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    var selectedSupplierToShowStatement by remember { mutableStateOf<PartnerEntity?>(null) }
    val statementData by viewModel.supplierStatement.collectAsStateWithLifecycle()

    var showStatementDatePickerDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دليل الموردين الذمم الدائنة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddSupplierDialog = true },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعريف مورد جديد", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (suppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد موردين معرفين بالنظام حالياً. اضغط على أزرار تفعيل مورد.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(suppliers) { sup ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(sup.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("الهاتف: ${sup.phone.ifEmpty { "غير محدد" }} | البريد: ${sup.email.ifEmpty { "غير محدد" }}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("الرصيد القائم (ذمة)", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            FormatUtils.formatCurrency(sup.balance),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Payables Aging Indicator (Visual Bar matching PUR-07)
                                SupplierAgingProgress(sup.balance, sup.creditLimit ?: 50000.0)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedSupplierToShowStatement = sup
                                            viewModel.loadSupplierStatement(sup.id, startDate, endDate)
                                            showStatementDatePickerDialog = true
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كشف الحساب والعمليات", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Supplier Dialog
        if (showAddSupplierDialog) {
            AddSupplierDialog(
                onDismiss = { showAddSupplierDialog = false },
                onConfirm = { name, phone, email, limit ->
                    viewModel.addPartner(name, "SUPPLIER", phone, email, limit)
                    showAddSupplierDialog = false
                }
            )
        }

        // Supplier Statement Visual Dialog Drawer/Popup
        if (showStatementDatePickerDialog && selectedSupplierToShowStatement != null) {
            val sup = selectedSupplierToShowStatement!!
            AlertDialog(
                onDismissRequest = { showStatementDatePickerDialog = false },
                title = { Text("كشف كشف الحساب التهويدي لـ: ${sup.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تاريخ البداية (تلقائي)", style = MaterialTheme.typography.labelSmall)
                                Text(FormatUtils.formatDate(startDate), modifier = Modifier.padding(8.dp).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).fillMaxWidth().padding(4.dp), textAlign = TextAlign.Center)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تاريخ النهاية", style = MaterialTheme.typography.labelSmall)
                                Text(FormatUtils.formatDate(endDate), modifier = Modifier.padding(8.dp).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).fillMaxWidth().padding(4.dp), textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("العمليات المعمدة والواردة:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (statementData.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Text("لا يوجد حركات قيود معتمدة في هذه الفترة.", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                items(statementData) { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(tx.type + " (${tx.reference})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(FormatUtils.formatDate(tx.date), fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (tx.credit > 0.0) {
                                                Text("+ " + FormatUtils.formatCurrency(tx.credit), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("- " + FormatUtils.formatCurrency(tx.debit), fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                            }
                                            Text(" رصيد: " + FormatUtils.formatCurrency(tx.balanceAfter), fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showStatementDatePickerDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}

@Composable
fun SupplierAgingProgress(currentBalance: Double, creditLimit: Double) {
    val faction = if (creditLimit > 0.0) (currentBalance / creditLimit).coerceIn(0.0, 1.0) else 0.0
    val color = when {
        faction > 0.8 -> MaterialTheme.colorScheme.error
        faction > 0.5 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مؤشر الاستغلال الائتماني والتعرض للأعمار الديون", fontSize = 11.sp, color = Color.Gray)
            Text("${String.format(Locale.US, "%.0f", faction * 100)}% (حد ائتماني: ${FormatUtils.formatCurrency(creditLimit)})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { faction.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, limit: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("50000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بطاقة مـورد جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشركة / المورد") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("الحد الائتماني المسموح به") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        onConfirm(name, phone, email, limit.toDoubleOrNull() ?: 50000.0)
                    }
                }
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// ==========================================
// --- SUB TAB 2: PURCHASE ORDERS ---
// ==========================================
@Composable
fun PurchaseOrdersSubTab(
    viewModel: AccountingViewModel,
    partners: List<PartnerEntity>,
    products: List<ProductEntity>
) {
    val suppliers = partners.filter { it.type == "SUPPLIER" }
    val orders by viewModel.purchaseOrders.collectAsStateWithLifecycle()
    var showCreateOrderSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "طلبات وأوامر المشتريات المعمدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showCreateOrderSheet = true },
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إنشاء طلب شراء", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("لا يوجد أوامر شراء حالياً.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders) { orderWithOwner ->
                    val o = orderWithOwner.order
                    val supName = suppliers.find { it.id == o.supplierId }?.name ?: "مورد غير معروف"
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("أمر رقم: ${o.orderNumber}", fontWeight = FontWeight.Bold)
                                    Text("المورد: $supName", fontSize = 12.sp)
                                    Text("التاريخ: ${FormatUtils.formatDate(o.date)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val statusColor = when (o.status) {
                                        "DRAFT" -> Color.Gray
                                        "CONFIRMED" -> Color(0xFF2196F3)
                                        "CONVERTED" -> Color(0xFF4CAF50)
                                        else -> Color.DarkGray
                                    }
                                    val statusAr = when (o.status) {
                                        "DRAFT" -> "مسودة"
                                        "CONFIRMED" -> "معمد"
                                        "CONVERTED" -> "مرحل لفاتورة"
                                        else -> o.status
                                    }
                                    Text(
                                        text = statusAr,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(FormatUtils.formatCurrency(o.grandTotal), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Line item short descriptions
                            Text(
                                text = "الأصناف المطلوبة: ${orderWithOwner.lines.joinToString { line ->
                                    val name = products.find { it.id == line.productId }?.name ?: "منتج"
                                    "$name (×${line.quantity})"
                                }}",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )

                            if (o.status == "DRAFT") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { viewModel.deletePurchaseOrder(o.id, {}, {}) }) {
                                        Text("حذف", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.confirmPurchaseOrder(o.id, {}, {}) },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تأكيد وتعميد", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.convertOrderToInvoice(o.id, null, true, {}, {}) },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تحويل لفاتورة مسودة", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateOrderSheet) {
            CreateOrderDialog(
                suppliers = suppliers,
                products = products,
                onDismiss = { showCreateOrderSheet = false },
                onSave = { supId, items ->
                    viewModel.createPurchaseOrder(supId, System.currentTimeMillis(), items, {
                        showCreateOrderSheet = false
                    }, {})
                }
            )
        }
    }
}

@Composable
fun CreateOrderDialog(
    suppliers: List<PartnerEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (supplierId: Long, items: List<Pair<Long, Pair<Double, Double>>>) -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 0L) }
    val lineItems = remember { mutableStateListOf<Triple<Long, Double, Double>>() } // productId, qty, unitPrice

    var activeProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    var inputQty by remember { mutableStateOf("10") }
    var inputPrice by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("طلب تعميد أمر شراء", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("المورد لطلب الشراء:", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth()) {
                    suppliers.forEach { sup ->
                        FilterChip(
                            selected = selectedSupplierId == sup.id,
                            onClick = { selectedSupplierId = sup.id },
                            label = { Text(sup.name) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                HorizontalDivider()

                Text("إضافة سطر منتج / صنف:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("السلعة", fontSize = 10.sp)
                        // Simple selector of items
                        products.forEach { pr ->
                            if (activeProductId == pr.id) {
                                Text(pr.name, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(4.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الكمية", fontSize = 10.sp)
                        OutlinedTextField(
                            value = inputQty,
                            onValueChange = { inputQty = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("السعر", fontSize = 10.sp)
                        OutlinedTextField(
                            value = inputPrice,
                            onValueChange = { inputPrice = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            val q = inputQty.toDoubleOrNull() ?: 0.0
                            val p = inputPrice.toDoubleOrNull() ?: 0.0
                            if (q > 0.0 && p > 0.0) {
                                lineItems.add(Triple(activeProductId, q, p))
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("+")
                    }
                }

                // Show selected items
                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(lineItems) { line ->
                        val prodName = products.find { it.id == line.first }?.name ?: "سلعة"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$prodName - الكمية: ${line.second} بسعر: ${line.third}", fontSize = 11.sp)
                            TextButton(onClick = { lineItems.remove(line) }) {
                                Text("إزالة", color = Color.Red, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSupplierId != 0L && lineItems.isNotEmpty()) {
                        val mapped = lineItems.map { Pair(it.first, Pair(it.second, it.third)) }
                        onSave(selectedSupplierId, mapped)
                    }
                }
            ) {
                Text("حفظ أمر الشراء المسودة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// ==========================================
// --- SUB TAB 3: PURCHASE INVOICES ---
// ==========================================
@Composable
fun PurchaseInvoicesSubTab(
    viewModel: AccountingViewModel,
    partners: List<PartnerEntity>,
    products: List<ProductEntity>,
    accounts: List<AccountEntity>,
    invoices: List<PurchaseInvoiceWithOwner>
) {
    val suppliers = partners.filter { it.type == "SUPPLIER" }
    var showCreateInvoiceDraftSheet by remember { mutableStateOf(false) }

    var selectedInvoiceForConfirmation by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var selectedInvoiceForPayment by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var openConfirmationDialogWithLandedCosts by remember { mutableStateOf(false) }
    var openPaymentDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل فواتير المشتريات والالتزامات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showCreateInvoiceDraftSheet = true },
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("فاتورة مباشرة", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("لا يوجد فواتير مشتريات حالياً.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(invoices) { wrapper ->
                    val inv = wrapper.invoice
                    val supName = suppliers.find { it.id == inv.supplierId }?.name ?: "مورد غير معروف"
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("فاتورة: ${inv.invoiceNumber}", fontWeight = FontWeight.Bold)
                                    Text("المورد: $supName", fontSize = 12.sp)
                                    Text("التاريخ: ${FormatUtils.formatDate(inv.date)} | الاستحقاق: ${FormatUtils.formatDate(inv.dueDate)}", fontSize = 11.sp, color = Color.Gray)
                                    Text("طبيعة الدفع: " + if(inv.isCredit) "آجل على الحساب" else "سداد نقدي مباشر", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val statusColor = when (inv.status) {
                                        "DRAFT" -> Color.Gray
                                        "POSTED" -> Color(0xFF4CAF50)
                                        "REFUNDED" -> Color(0xFFE91E63)
                                        else -> Color.DarkGray
                                    }
                                    val statusAr = when (inv.status) {
                                        "DRAFT" -> "مسودة غ.مرحلة"
                                        "POSTED" -> "معتمدة مرحلة"
                                        "REFUNDED" -> "مرتجعة بالكامل"
                                        else -> inv.status
                                    }
                                    Text(
                                        text = statusAr,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(FormatUtils.formatCurrency(inv.grandTotal), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    
                                    if (inv.paidAmount > 0.0) {
                                        Text("مسدد: ${FormatUtils.formatCurrency(inv.paidAmount)}", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "البنود: ${wrapper.lines.joinToString { line ->
                                    val name = products.find { it.id == line.productId }?.name ?: "صنف"
                                    "$name ×${line.quantity} (${FormatUtils.formatCurrency(line.price)})"
                                }}",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )

                            if (inv.landedCostsAllocated > 0.0) {
                                Text("تكاليف شحن إضافية موزعة بنجاح WAC: ${FormatUtils.formatCurrency(inv.landedCostsAllocated)} (${if(inv.landedAllocationMethod == "VALUE") "حسب القيمة" else "حسب الكمية"})", fontSize = 11.sp, color = Color(0xFF9C27B0), fontWeight = FontWeight.Medium)
                            }

                            if (inv.status == "DRAFT") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.deletePurchaseInvoice(inv.id, {}, {}) }) {
                                        Text("حذف المسودة", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            selectedInvoiceForConfirmation = inv
                                            openConfirmationDialogWithLandedCosts = true
                                        },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("ترحيل وتوزيع تكاليف الشحن", fontSize = 11.sp)
                                    }
                                }
                            } else if (inv.status == "POSTED" && inv.isCredit) {
                                val remaining = inv.grandTotal - inv.paidAmount
                                if (remaining > 0.01) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                selectedInvoiceForPayment = inv
                                                openPaymentDialog = true
                                            },
                                            modifier = Modifier.height(30.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text("تسجيل سداد دفعة للمورد", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateInvoiceDraftSheet) {
            CreateInvoiceDraftDialog(
                suppliers = suppliers,
                products = products,
                accounts = accounts,
                onDismiss = { showCreateInvoiceDraftSheet = false },
                onSave = { supId, isCred, pAcc, date, due, items ->
                    viewModel.createPurchaseInvoice(supId, date, due, isCred, pAcc, items, {
                        showCreateInvoiceDraftSheet = false
                    }, {})
                }
            )
        }

        // --- Landed Costs Allocation Window (CONFIRM INVOICE WINDOW - PUR-25 & PUR-27) ---
        if (openConfirmationDialogWithLandedCosts && selectedInvoiceForConfirmation != null) {
            var landedDouble by remember { mutableStateOf("0.0") }
            var allocationMethod by remember { mutableStateOf("VALUE") } // VALUE, QUANTITY
            val cashOrBankAccounts = accounts.filter { it.type == "ASSETS" && (it.code.startsWith("1101") || it.code.startsWith("1102")) }
            var paidByAccountId by remember { mutableStateOf(cashOrBankAccounts.firstOrNull()?.id) }

            val inv = selectedInvoiceForConfirmation!!
            
            AlertDialog(
                onDismissRequest = { openConfirmationDialogWithLandedCosts = false },
                title = { Text("معالجة وتأكيد الفاتورة وتوزيع تكاليف الشحن (Landed Costs)", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("مبلغ الفاتورة الصافي: ${FormatUtils.formatCurrency(inv.grandTotal)}", fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = landedDouble,
                            onValueChange = { landedDouble = it },
                            label = { Text("تكاليف إضافية (شحن، جمـارك، رسـوم) ر.س") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("طريقة توزيع وطرح التكاليف الذكية لواردات WAC الكلفة:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                RadioButton(selected = allocationMethod == "VALUE", onClick = { allocationMethod = "VALUE" })
                                Text("حسب القيمة (Value)", fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                RadioButton(selected = allocationMethod == "QUANTITY", onClick = { allocationMethod = "QUANTITY" })
                                Text("حسب الكمية (Qty)", fontSize = 11.sp)
                            }
                        }

                        if ((landedDouble.toDoubleOrNull() ?: 0.0) > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("حساب السداد والتسوية لمصاريف الشحن:", fontSize = 11.sp)
                            cashOrBankAccounts.forEach { acc ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = paidByAccountId == acc.id, onClick = { paidByAccountId = acc.id })
                                    Text("${acc.nameAr} (${acc.code})", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.confirmPurchaseInvoice(
                                invoiceId = inv.id,
                                paymentAccountId = paidByAccountId,
                                landedCosts = landedDouble.toDoubleOrNull() ?: 0.0,
                                landedAllocationMethod = allocationMethod,
                                onSuccess = { openConfirmationDialogWithLandedCosts = false },
                                onError = {}
                            )
                        }
                    ) {
                        Text("اعتماد وترحيل الدفتر والمخزون")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { openConfirmationDialogWithLandedCosts = false }) { Text("إلغاء") }
                }
            )
        }

        // --- Supplier Payment dialog ---
        if (openPaymentDialog && selectedInvoiceForPayment != null) {
            val inv = selectedInvoiceForPayment!!
            val unpaid = inv.grandTotal - inv.paidAmount
            var payAmountRaw by remember { mutableStateOf(String.format(Locale.US, "%.2f", unpaid)) }
            
            val cashOrBankAccounts = accounts.filter { it.type == "ASSETS" && (it.code.startsWith("1101") || it.code.startsWith("1102")) }
            var selectedPayAccountId by remember { mutableStateOf(cashOrBankAccounts.firstOrNull()?.id ?: 0L) }

            AlertDialog(
                onDismissRequest = { openPaymentDialog = false },
                title = { Text("تسجيل سند صرف سداد للمورد", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("الفاتورة: ${inv.invoiceNumber}", fontWeight = FontWeight.SemiBold)
                        Text("المبلغ غير المسدد المتبقي: ${FormatUtils.formatCurrency(unpaid)}", color = Color.Gray)

                        OutlinedTextField(
                            value = payAmountRaw,
                            onValueChange = { payAmountRaw = it },
                            label = { Text("مبلغ سداد الصرف الصادر ر.س") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("حساب النقد والبنك (الصرف منه):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        cashOrBankAccounts.forEach { acc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedPayAccountId == acc.id, onClick = { selectedPayAccountId = acc.id })
                                Text("${acc.nameAr} (${acc.code})", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = payAmountRaw.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0 && selectedPayAccountId != 0L) {
                                viewModel.recordSupplierPayment(
                                    invoiceId = inv.id,
                                    amount = amt,
                                    payAccountId = selectedPayAccountId,
                                    onSuccess = { openPaymentDialog = false },
                                    onError = {}
                                )
                            }
                        }
                    ) {
                        Text("اعتماد ترحيل سند الصرف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { openPaymentDialog = false }) { Text("إلغاء") }
                }
            )
        }
    }
}

@Composable
fun CreateInvoiceDraftDialog(
    suppliers: List<PartnerEntity>,
    products: List<ProductEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (supplierId: Long, isCredit: Boolean, paymentId: Long?, date: Long, due: Long, items: List<Pair<Long, Pair<Double, Double>>>) -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 0L) }
    var isCredit by remember { mutableStateOf(true) }

    val cashOrBankAccounts = accounts.filter { it.type == "ASSETS" && (it.code.startsWith("1101") || it.code.startsWith("1102")) }
    var selectedCashAccountId by remember { mutableStateOf(cashOrBankAccounts.firstOrNull()?.id) }

    val lineItems = remember { mutableStateListOf<Triple<Long, Double, Double>>() } // productId, qty, price

    var activeProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    var inputQty by remember { mutableStateOf("10") }
    var inputPrice by remember { mutableStateOf("120") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فاتورة مشتريات مباشرة بضاعة", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("المورد المعتمد للفاتورة:", fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    suppliers.forEach { sup ->
                        FilterChip(
                            selected = selectedSupplierId == sup.id,
                            onClick = { selectedSupplierId = sup.id },
                            label = { Text(sup.name) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        RadioButton(selected = isCredit, onClick = { isCredit = true })
                        Text("شراء آجل على الحساب", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        RadioButton(selected = !isCredit, onClick = { isCredit = false })
                        Text("شراء نقدي فوري", fontSize = 11.sp)
                    }
                }

                if (!isCredit) {
                    Text("حساب الدفع النقدي الفوري المعتمد:", fontSize = 11.sp)
                    cashOrBankAccounts.forEach { acc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCashAccountId == acc.id, onClick = { selectedCashAccountId = acc.id })
                            Text("${acc.nameAr} (${acc.code})", fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider()

                Text("إضافة سطر بند بضاعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("السلعة", fontSize = 9.sp)
                        products.forEach { pr ->
                            if (activeProductId == pr.id) {
                                Text(pr.name, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(2.dp).clickable { 
                                    // rotate selection easily
                                }, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الكمية", fontSize = 9.sp)
                        OutlinedTextField(
                            value = inputQty,
                            onValueChange = { inputQty = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("السعر الكلفة", fontSize = 9.sp)
                        OutlinedTextField(
                            value = inputPrice,
                            onValueChange = { inputPrice = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            val q = inputQty.toDoubleOrNull() ?: 0.0
                            val p = inputPrice.toDoubleOrNull() ?: 0.0
                            if (q > 0.0 && p > 0.0) {
                                lineItems.add(Triple(activeProductId, q, p))
                            }
                        },
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text("+")
                    }
                }

                LazyColumn(modifier = Modifier.height(80.dp)) {
                    items(lineItems) { line ->
                        val prodName = products.find { it.id == line.first }?.name ?: "سلعة"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$prodName ×${line.second} بسعر: ${line.third}", fontSize = 11.sp)
                            TextButton(onClick = { lineItems.remove(line) }) {
                                Text("إزالة", color = Color.Red, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSupplierId != 0L && lineItems.isNotEmpty()) {
                        val mapped = lineItems.map { Pair(it.first, Pair(it.second, it.third)) }
                        onSave(selectedSupplierId, isCredit, selectedCashAccountId, System.currentTimeMillis(), System.currentTimeMillis() + (7 * 24 * 3600 * 1000), mapped)
                    }
                }
            ) {
                Text("حفظ الفاتورة المسودة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// ==========================================
// --- SUB TAB 4: PURCHASE RETURNS ---
// ==========================================
@Composable
fun PurchaseReturnsSubTab(
    viewModel: AccountingViewModel,
    invoices: List<PurchaseInvoiceWithOwner>,
    products: List<ProductEntity>
) {
    val returns by viewModel.purchaseReturns.collectAsStateWithLifecycle()
    val postedInvoices = invoices.filter { it.invoice.status == "POSTED" }

    var showCreateReturnSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مرتجع المشتريات والخصومات المعادة للموردين",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showCreateReturnSheet = true },
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إجراء مرتجع جديد", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (returns.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("لا يوجد قيود مرتجع مشتريات حالياً.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(returns) { wrapper ->
                    val ret = wrapper.returnEntity
                    val originalInvNo = invoices.find { it.invoice.id == ret.invoiceId }?.invoice?.invoiceNumber ?: "غير متاح"
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("قيد مرتجع رقم: ${ret.returnNumber}", fontWeight = FontWeight.Bold)
                                    Text("مرتبط بالفاتورة الأصلية رقم: $originalInvNo", fontSize = 12.sp)
                                    Text("التاريخ: ${FormatUtils.formatDate(ret.date)}", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("سبب الارتجاع والرفض: ${ret.reason}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("المبلغ المرتجع المسترد", fontSize = 11.sp)
                                    Text(FormatUtils.formatCurrency(ret.refundAmount), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "الأصناف المرجعة: ${wrapper.lines.joinToString { line ->
                                    val name = products.find { it.id == line.productId }?.name ?: "صنف"
                                    "$name ×${line.quantity} (${FormatUtils.formatCurrency(line.price)})"
                                }}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        if (showCreateReturnSheet) {
            CreateReturnDialog(
                postedInvoices = postedInvoices,
                products = products,
                onDismiss = { showCreateReturnSheet = false },
                onSave = { invoiceId, reason, items ->
                    viewModel.createPurchaseReturn(invoiceId, reason, items, {
                        showCreateReturnSheet = false
                    }, {})
                }
            )
        }
    }
}

@Composable
fun CreateReturnDialog(
    postedInvoices: List<PurchaseInvoiceWithOwner>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (invoiceId: Long, reason: String, items: List<Pair<Long, Double>>) -> Unit
) {
    var selectedInvoiceId by remember { mutableStateOf(postedInvoices.firstOrNull()?.invoice?.id ?: 0L) }
    var reason by remember { mutableStateOf("بضاعة تالفة / مخالفة للمواصفات المعيارية") }

    val activeInvoiceWithOwner = postedInvoices.find { it.invoice.id == selectedInvoiceId }
    val returnQuantities = remember { mutableStateMapOf<Long, String>() } // productId -> qtyString

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعداد قيد مرتجع مشتريات للمورد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (postedInvoices.isEmpty()) {
                    Text("لا يوجد فواتير مشتريات معتمدة ومرحلة محاسبياً في النظام حالياً لإرجاعها.", color = Color.Red, fontSize = 12.sp)
                } else {
                    Text("حدد فاتورة المشتريات المستهدفة:", fontSize = 11.sp)
                    LazyColumn(modifier = Modifier.height(60.dp)) {
                        items(postedInvoices) { invWithOwner ->
                            val inv = invWithOwner.invoice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedInvoiceId = inv.id }
                                    .background(if (selectedInvoiceId == inv.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("فاتورة ${inv.invoiceNumber} (${FormatUtils.formatCurrency(inv.grandTotal)})", fontSize = 11.sp)
                                Text(FormatUtils.formatDate(inv.date), fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("سبب الارتجاع") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (activeInvoiceWithOwner != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("الأصناف المتوفرة للفاتورة المحددة (اضبط الكميات المعادة):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        LazyColumn(modifier = Modifier.height(130.dp)) {
                            items(activeInvoiceWithOwner.lines) { line ->
                                val pName = products.find { it.id == line.productId }?.name ?: "صنف"
                                val maxAllowed = line.quantity - line.returnedQuantity
                                val inputVal = returnQuantities[line.productId] ?: "0"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text("$pName - المجموع: ${line.quantity}", fontSize = 11.sp)
                                        Text("أقصى مرتجع مسموح: $maxAllowed", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    OutlinedTextField(
                                        value = inputVal,
                                        onValueChange = { returnQuantities[line.productId] = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(60.dp).height(46.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedInvoiceId != 0L) {
                        val toReturn = mutableListOf<Pair<Long, Double>>()
                        returnQuantities.forEach { (productId, qtyStr) ->
                            val q = qtyStr.toDoubleOrNull() ?: 0.0
                            if (q > 0.0) {
                                toReturn.add(Pair(productId, q))
                            }
                        }
                        if (toReturn.isNotEmpty()) {
                            onSave(selectedInvoiceId, reason, toReturn)
                        }
                    }
                }
            ) {
                Text("إنجاز قيد المرتجع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
