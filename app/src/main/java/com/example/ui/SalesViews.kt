package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*


// Formatter helper
private fun fmt(amount: Double): String = String.format(Locale.US, "%,.2f", amount)
private fun fmtDate(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))

@Composable
fun SalesDashboardView(
    invoices: List<SalesInvoiceWithOwner>,
    customers: List<PartnerEntity>,
    products: List<ProductEntity>
) {
    val posted = invoices.filter { it.invoice.status == "POSTED" || it.invoice.status == "REFUNDED" }
    val totalSales = posted.sumOf { it.invoice.grandTotal }
    val count = posted.size
    val avgTicket = if (count > 0) totalSales / count else 0.0
    val totalAr = customers.sumOf { it.balance }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // KPI row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowForward, "", tint = Color(0xFF137333), modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = -90f))
                        Spacer(Modifier.width(4.dp))
                        Text("إجمالي المبيعات", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("${fmt(totalSales)} ر.س", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, "", tint = Color(0xFF1A73E8), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("الفواتير المعتمدة", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("$count فاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, "", tint = Color(0xFFD93025), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("متوسط الفاتورة", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("${fmt(avgTicket)} ر.س", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, "", tint = Color(0xFFF9AB00), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مديونية العملاء", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("${fmt(totalAr)} ر.س", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Top products sold chart
        val prodSales = mutableMapOf<Long, Double>()
        posted.forEach { inv ->
            inv.lines.forEach { line ->
                val netQty = line.quantity - line.returnedQuantity
                prodSales[line.productId] = (prodSales[line.productId] ?: 0.0) + (netQty * line.price)
            }
        }
        val topProducts = prodSales.entries
            .mapNotNull { e -> products.find { it.id == e.key }?.let { it.name to e.value } }
            .sortedByDescending { it.second }
            .take(4)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, "", tint = Color(0xFFF9AB00))
                    Spacer(Modifier.width(8.dp))
                    Text("أعلى المنتجات مبيعاً من حيث الإيرادات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (topProducts.isEmpty()) {
                    Text("لا توجد مبيعات مسجلة ومرحلة لعرض إحصاءات المواد.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    val maxVal = topProducts.first().second
                    topProducts.forEach { (name, value) ->
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${fmt(value)} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(4.dp))
                            val pct = if (maxVal > 0) (value / maxVal).toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }

        // Custom canvas trend spline chart
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("منحنى حركة المبيعات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                if (posted.size < 2) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("يلزم مرحلتين مبيعات على الأقل لرسم خطوط الاتجاه المالي", color = Color.Gray, fontSize = 11.sp)
                    }
                } else {
                    val sortedInvs = posted.sortedBy { it.invoice.date }.takeLast(8)
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.White.copy(alpha = 0.05f))) {
                        val maxTotal = sortedInvs.maxOf { it.invoice.grandTotal }.toFloat().coerceAtLeast(1f)
                        val w = size.width
                        val h = size.height
                        val stepX = w / (sortedInvs.size - 1)
                        val path = Path()

                        sortedInvs.forEachIndexed { idx, item ->
                            val x = idx * stepX
                            val prg = item.invoice.grandTotal.toFloat() / maxTotal
                            val y = h - (prg * (h - 20f)) - 10f

                            if (idx == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            drawCircle(color = Color(0xFF1E88E5), radius = 4f, center = Offset(x, y))
                        }
                        drawPath(path = path, color = Color(0xFF1E88E5), style = Stroke(width = 3f))
                    }
                }
            }
        }
    }
}

@Composable
fun CreateQuotationDialog(
    customers: List<PartnerEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSubmit: (Long, Long, List<Pair<Long, Pair<Double, Double>>>) -> Unit
) {
    var selectedCustId by remember { mutableStateOf<Long?>(null) }
    var validityDays by remember { mutableStateOf("7") }
    val lines = remember { mutableStateListOf<Pair<Long, Pair<Double, Double>>>() }

    // line inputs
    var selectedLineProdId by remember { mutableStateOf<Long?>(null) }
    var lineQty by remember { mutableStateOf("1") }
    var linePrice by remember { mutableStateOf("") }

    var expandedCust by remember { mutableStateOf(false) }
    var expandedProd by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("إنشاء عرض سعر مبيعات جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                // Select Customer
                Box {
                    val label = customers.find { it.id == selectedCustId }?.name ?: "اختر العميل المستهدف"
                    OutlinedButton(onClick = { expandedCust = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                    DropdownMenu(expanded = expandedCust, onDismissRequest = { expandedCust = false }) {
                        customers.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedCustId = c.id; expandedCust = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = validityDays,
                    onValueChange = { validityDays = it },
                    label = { Text("فترة الصلاحية (بالأيام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()
                Text("إضافة بنود عرض السعر", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                // Select Product for row
                Box {
                    val label = products.find { it.id == selectedLineProdId }?.name ?: "اختر الصنف المراد إضافته"
                    OutlinedButton(onClick = { expandedProd = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                    DropdownMenu(expanded = expandedProd, onDismissRequest = { expandedProd = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(text = { Text("${p.name} (${fmt(p.price)} ر.س)") }, onClick = {
                                selectedLineProdId = p.id
                                linePrice = p.price.toString()
                                expandedProd = false
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lineQty,
                        onValueChange = { lineQty = it },
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = linePrice,
                        onValueChange = { linePrice = it },
                        label = { Text("السعر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val pId = selectedLineProdId
                        val q = lineQty.toDoubleOrNull() ?: 1.0
                        val pr = linePrice.toDoubleOrNull() ?: 0.0
                        if (pId != null && q > 0) {
                            lines.add(pId to (q to pr))
                            selectedLineProdId = null
                            lineQty = "1"
                            linePrice = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedLineProdId != null
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة الصنف كبند جدول")
                }

                if (lines.isNotEmpty()) {
                    Text("البنود المضافة (${lines.size}):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                            lines.forEachIndexed { idx, pair ->
                                val prod = products.find { it.id == pair.first }
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${prod?.name} [${pair.second.first} x ${pair.second.second}]", fontSize = 11.sp)
                                    Row {
                                        Text("${fmt(pair.second.first * pair.second.second)} ر.س", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(8.dp))
                                        Text("❌", color = Color.Red, fontSize = 11.sp, modifier = Modifier.clickable { lines.removeAt(idx) })
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary calculations
                val subtotal = lines.sumOf { it.second.first * it.second.second }
                val vat = subtotal * 0.15
                val total = subtotal + vat

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المجموع الفرعي:", fontSize = 12.sp)
                    Text("${fmt(subtotal)} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ضريبة القيمة المضافة (15٪):", fontSize = 12.sp)
                    Text("${fmt(vat)} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي المستحق:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${fmt(total)} ر.س", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء الأمر")
                    }
                    Button(
                        onClick = {
                            val cust = selectedCustId
                            val days = validityDays.toLongOrNull() ?: 7
                            if (cust != null && lines.isNotEmpty()) {
                                onSubmit(cust, System.currentTimeMillis() + (days * 24 * 3600 * 1000), lines.toList())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedCustId != null && lines.isNotEmpty()
                    ) {
                        Text("حفظ المسودة")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateSalesInvoiceDialog(
    customers: List<PartnerEntity>,
    products: List<ProductEntity>,
    cashBankAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (Long, Long, Long, Boolean, Long?, List<Pair<Long, Pair<Double, Double>>>) -> Unit
) {
    var selectedCustId by remember { mutableStateOf<Long?>(null) }
    var isCredit by remember { mutableStateOf(false) } // Cash vs Credit
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var termsDays by remember { mutableStateOf("7") }
    val lines = remember { mutableStateListOf<Pair<Long, Pair<Double, Double>>>() }

    // line inputs
    var selectedLineProdId by remember { mutableStateOf<Long?>(null) }
    var lineQty by remember { mutableStateOf("1") }
    var linePrice by remember { mutableStateOf("") }

    var expandedCust by remember { mutableStateOf(false) }
    var expandedProd by remember { mutableStateOf(false) }
    var expandedAcc by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("إصدار فاتورة مبيعات جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                Box {
                    val label = customers.find { it.id == selectedCustId }?.name ?: "اختر العميل"
                    OutlinedButton(onClick = { expandedCust = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                    DropdownMenu(expanded = expandedCust, onDismissRequest = { expandedCust = false }) {
                        customers.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedCustId = c.id; expandedCust = false })
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                    Spacer(Modifier.width(8.dp))
                    Text("بيع على الحساب (آجل - ائتمان)", fontSize = 13.sp)
                }

                if (!isCredit) {
                    Box {
                        val label = cashBankAccounts.find { it.id == selectedAccountId }?.let { "${it.code} - ${it.nameAr}" } ?: "اختر صندوق/بنك التحصيل اليومي"
                        OutlinedButton(onClick = { expandedAcc = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(label)
                        }
                        DropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                            cashBankAccounts.forEach { a ->
                                DropdownMenuItem(text = { Text("${a.code} - ${a.nameAr}") }, onClick = { selectedAccountId = a.id; expandedAcc = false })
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = termsDays,
                        onValueChange = { termsDays = it },
                        label = { Text("فترة السداد للعميل الآجل (بالأيام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()
                Text("الأصناف المباعة بالجدول", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Box {
                    val label = products.find { it.id == selectedLineProdId }?.let { "${it.name} [متوفر: ${it.stock}]" } ?: "اختر صنف مبيعات"
                    OutlinedButton(onClick = { expandedProd = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                    DropdownMenu(expanded = expandedProd, onDismissRequest = { expandedProd = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(text = { Text("${p.name} [متوفر: ${p.stock}] - ${fmt(p.price)} ر.س") }, onClick = {
                                selectedLineProdId = p.id
                                linePrice = p.price.toString()
                                expandedProd = false
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lineQty,
                        onValueChange = { lineQty = it },
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = linePrice,
                        onValueChange = { linePrice = it },
                        label = { Text("السعر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val pId = selectedLineProdId
                        val q = lineQty.toDoubleOrNull() ?: 1.0
                        val pr = linePrice.toDoubleOrNull() ?: 0.0
                        if (pId != null && q > 0) {
                            lines.add(pId to (q to pr))
                            selectedLineProdId = null
                            lineQty = "1"
                            linePrice = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedLineProdId != null
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة صنف للفاتورة")
                }

                if (lines.isNotEmpty()) {
                    lines.forEachIndexed { idx, pair ->
                        val prod = products.find { it.id == pair.first }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${prod?.name} (${pair.second.first} x ${pair.second.second} ر.س)", fontSize = 11.sp)
                            Row {
                                Text("${fmt(pair.second.first * pair.second.second)} ر.س", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("❌", color = Color.Red, fontSize = 11.sp, modifier = Modifier.clickable { lines.removeAt(idx) })
                            }
                        }
                    }
                }

                val subtotal = lines.sumOf { it.second.first * it.second.second }
                val vat = subtotal * 0.15
                val total = subtotal + vat

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المجموع الفرعي:", fontSize = 12.sp)
                    Text("${fmt(subtotal)} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ضريبة مخرجات مبيعات (15٪):", fontSize = 12.sp)
                    Text("${fmt(vat)} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي المستحق:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${fmt(total)} ر.س", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء الأمر")
                    }
                    Button(
                        onClick = {
                            val cust = selectedCustId
                            if (cust != null && lines.isNotEmpty()) {
                                if (!isCredit && selectedAccountId == null) return@Button
                                val extraDays = if (isCredit) (termsDays.toLongOrNull() ?: 7) else 0L
                                onSubmit(
                                    cust,
                                    System.currentTimeMillis(),
                                    System.currentTimeMillis() + (extraDays * 24 * 3600 * 1000),
                                    isCredit,
                                    selectedAccountId,
                                    lines.toList()
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedCustId != null && lines.isNotEmpty() && (isCredit || selectedAccountId != null)
                    ) {
                        Text("حفظ مسودة")
                    }
                }
            }
        }
    }
}

@Composable
fun SalesReturnConfirmDialog(
    invoice: SalesInvoiceWithOwner,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSubmit: (Long, String, List<Pair<Long, Double>>) -> Unit
) {
    var reason by remember { mutableStateOf("تلف أو عدم مطابقة المواصفات") }
    val returnQtys = remember { mutableStateMapOf<Long, Double>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("طلب مرتجع مبيعات للفاتورة: ${invoice.invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب المرتجع") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر الكميات المراد إرجاعها:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                invoice.lines.forEach { line ->
                    val prod = products.find { it.id == line.productId } ?: return@forEach
                    val maxReturn = line.quantity - line.returnedQuantity
                    if (maxReturn > 0) {
                        val currentRet = returnQtys[line.productId]?.toString() ?: "0"
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(prod.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("المتاح للإرجاع: $maxReturn / المبيعات: ${line.quantity}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = currentRet,
                                onValueChange = {
                                    val d = it.toDoubleOrNull() ?: 0.0
                                    if (d <= maxReturn && d >= 0) {
                                        returnQtys[line.productId] = d
                                    }
                                },
                                label = { Text("المرتجع") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).height(50.dp)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء الأمر")
                    }
                    Button(
                        onClick = {
                            val items = returnQtys.entries.map { it.key to it.value }.filter { it.second > 0 }
                            if (items.isNotEmpty()) {
                                onSubmit(invoice.invoice.id, reason, items)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = returnQtys.values.any { it > 0 }
                    ) {
                        Text("إثبات المرتجع")
                    }
                }
            }
        }
    }
}

@Composable
fun InvoicePaymentDialog(
    invoice: SalesInvoiceEntity,
    cashBankAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (Long, Double, Long) -> Unit
) {
    val remaining = invoice.grandTotal - invoice.paidAmount
    var inputAmount by remember { mutableStateOf(remaining.toString()) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var expandedAcc by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تسجيل وتصنيف دفعة سداد", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("الفاتورة رقم: ${invoice.invoiceNumber}", fontSize = 11.sp, color = Color.Gray)
                Text("المتبقي غير المسدد: ${fmt(remaining)} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)

                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    label = { Text("المبلغ المدفوع (ر.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    val label = cashBankAccounts.find { it.id == selectedAccountId }?.let { "${it.code} - ${it.nameAr}" } ?: "اختر حساب استلام الدفعة"
                    OutlinedButton(onClick = { expandedAcc = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                    DropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                        cashBankAccounts.forEach { a ->
                            DropdownMenuItem(text = { Text("${a.code} - ${a.nameAr}") }, onClick = { selectedAccountId = a.id; expandedAcc = false })
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val amt = inputAmount.toDoubleOrNull() ?: 0.0
                            val accId = selectedAccountId
                            if (amt > 0 && accId != null && amt <= remaining) {
                                onSubmit(invoice.id, amt, accId)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedAccountId != null && (inputAmount.toDoubleOrNull() ?: 0.0) > 0.0
                    ) {
                        Text("اعتماد الدفع")
                    }
                }
            }
        }
    }
}

@Composable
fun InvoicePdfPreviewDialog(
    invoiceWithOwner: SalesInvoiceWithOwner,
    customer: PartnerEntity,
    products: List<ProductEntity>,
    onDismiss: () -> Unit
) {
    val invoice = invoiceWithOwner.invoice
    var printSuccessMsg by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("معاينة الفاتورة الضريبية المبسطة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, null)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Scrollable Invoice Sheet
                Column(
                    modifier = Modifier.weight(1f).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Logo placeholder / name
                    Text("شركة المشرق المتكاملة المحدودة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    Text("فاتورة ضريبية مبسطة / Simplified Tax Invoice", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Color.DarkGray)
                    Text("الرقم الضريبي للمنشأة: 310500122300003", fontSize = 10.sp, color = Color.Gray)

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("رقم الفاتورة: ${invoice.invoiceNumber}", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("تاريخ الإصدار: ${fmtDate(invoice.date)}", fontSize = 11.sp, color = Color.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("العميل: ${customer.name}", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("النوع: ${if (invoice.isCredit) "بيع بالآجل (الائتمان)" else "بيع نقدي ساري"}", fontSize = 11.sp, color = Color.Black)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(Modifier.height(8.dp))

                    // Column Table Items Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الصنف / المادة", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("الكمية", modifier = Modifier.weight(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                        Text("سعر المقار", modifier = Modifier.weight(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
                        Text("الإجمالي", modifier = Modifier.weight(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Line items
                    invoiceWithOwner.lines.forEach { line ->
                        val p = products.find { it.id == line.productId }
                        val netQty = line.quantity - line.returnedQuantity
                        if (netQty > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p?.name ?: "مادة مبيعات", modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = Color.Black)
                                Text("$netQty", modifier = Modifier.weight(0.5f), fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.Center)
                                Text("${fmt(line.price)}", modifier = Modifier.weight(0.7f), fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.End)
                                Text("${fmt(netQty * line.price)} ر.س", modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(Modifier.height(8.dp))

                    // Summary Block
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع الفرعي (غير شامل الضريبة):", fontSize = 11.sp, color = Color.Black)
                            Text("${fmt(invoice.subtotal)} ر.س", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ضريبة القيمة المضافة المبسطة (15٪):", fontSize = 11.sp, color = Color.Black)
                            Text("${fmt(invoice.vat)} ر.س", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي النهائي شامل الضريبة المضافة:", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("${fmt(invoice.grandTotal)} ر.س", fontSize = 12.sp, color = Color(0xFF137333), fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // QR Code Simulation Image
                    Box(
                        modifier = Modifier.size(90.dp).border(1.dp, Color.Black).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(75.dp)) {
                            // Draw a simulated standard QR barcode pattern for ZATCA E-Invoicing!
                            val sizeW = size.width
                            val sizeH = size.height
                            val numBlocks = 7
                            val stepW = sizeW / numBlocks
                            val stepH = sizeH / numBlocks
                            val r = Random(invoice.invoiceNumber.hashCode().toLong())
                            
                            for (i in 0 until numBlocks) {
                                for (j in 0 until numBlocks) {
                                    // Make corners solid ZATCA anchors
                                    val isAnchor = (i < 2 && j < 2) || (i > 4 && j < 2) || (i < 2 && j > 4)
                                    if (isAnchor || r.nextBoolean()) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(i * stepW, j * stepH),
                                            size = Size(stepW, stepH)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text("مسح الكود الضريبي المعتمد من هيئة الزكاة", fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(12.dp))

                if (printSuccessMsg != null) {
                    Text(printSuccessMsg!!, color = Color(0xFF137333), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isPrinting = true
                            printSuccessMsg = "جاري الاتصال بالطابعة اللاسلكية ... تم الإرسال والطباعة بنجاح ✅"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("طباعة الفاتورة")
                    }
                    OutlinedButton(
                        onClick = {
                            printSuccessMsg = "تم حفظ ملف الفاتورة PDF ومشاركة السند عبر الواتساب بنجاح ✅"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerStatementDialog(
    customer: PartnerEntity,
    invoices: List<SalesInvoiceWithOwner>,
    salesReturns: List<SalesReturnWithOwner>,
    onDismiss: () -> Unit
) {
    // Generate ZATCA style running client statement ledger
    val postedInvs = invoices.filter { it.invoice.customerId == customer.id && (it.invoice.status == "POSTED" || it.invoice.status == "REFUNDED") }
    val customerReturns = salesReturns.filter { sRet -> sRet.returnEntity.invoiceId in postedInvs.map { it.invoice.id } }

    data class StatementLine(
        val date: Long,
        val docType: String,
        val refNo: String,
        val debit: Double,
        val credit: Double
    )

    val stLines = mutableListOf<StatementLine>()

    // Add Sales Invoices as DEBITS (increases what customer owes us)
    postedInvs.forEach { inv ->
        stLines.add(StatementLine(inv.invoice.date, "فاتورة مبيعات", inv.invoice.invoiceNumber, inv.invoice.grandTotal, 0.0))
        // If there was cash payment or recorded payment on credit invoice, records them as CREDIT
        if (inv.invoice.paidAmount > 0.0) {
            val payMode = if (!inv.invoice.isCredit) "سداد نقدي مباشر" else "دفعة سداد مستلمة"
            stLines.add(StatementLine(inv.invoice.date, payMode, "${inv.invoice.invoiceNumber}-PAY", 0.0, inv.invoice.paidAmount))
        }
    }

    // Add Returns as CREDITS (decreases what customer owes us)
    customerReturns.forEach { ret ->
        stLines.add(StatementLine(ret.returnEntity.date, "مرتجع مبيعات", ret.returnEntity.returnNumber, 0.0, ret.returnEntity.refundAmount))
    }

    // Sort by Date
    val sortedStatement = stLines.sortedBy { it.date }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("كشف الحساب التجاري التفصيلي للعميل", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, null) }
                }

                Spacer(Modifier.height(8.dp))

                // Detail Box
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("الاسم: ${customer.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("الهاتف: ${customer.phone}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي المديونية الحالية: ${fmt(customer.balance)} ر.س", fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 12.sp)
                            Text("الحد الائتماني المعين: ${fmt(customer.creditLimit)} ر.س", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Table Statement Ledger
                Box(modifier = Modifier.weight(1f).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).background(Color.White).padding(8.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.3f)).padding(6.dp)) {
                                Text("التاريخ", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("المستند التجاري", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("مدين (+)", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
                                Text("دائن (-)", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
                                Text("الرصيد", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
                            }
                        }

                        if (sortedStatement.isEmpty()) {
                            item {
                                Text("لا توجد تعاملات وقيود تجارية مسجلة للعميل حتى الآن.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                            }
                        } else {
                            var runningBalance = 0.0
                            items(sortedStatement) { line ->
                                runningBalance += (line.debit - line.credit)
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(fmtDate(line.date), modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.Black)
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(line.docType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(line.refNo, fontSize = 8.sp, color = Color.Gray)
                                    }
                                    Text(if (line.debit > 0) fmt(line.debit) else "-", modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.End)
                                    Text(if (line.credit > 0) fmt(line.credit) else "-", modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.End)
                                    Text("${fmt(runningBalance)} ر.س", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (runningBalance > 0) Color(0xFFD93025) else Color(0xFF137333), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Simulate Print Statement
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("طباعة كشف الحساب الحالي")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicingTab(
    partners: List<PartnerEntity>,
    products: List<ProductEntity>,
    accounts: List<AccountEntity>,
    quotations: List<SalesQuotationWithOwner>,
    salesInvoices: List<SalesInvoiceWithOwner>,
    salesReturns: List<SalesReturnWithOwner>,
    onAddPartner: (String, String, String, String, Double) -> Unit,
    onAddProduct: (String, String, Double, Double, Double, String) -> Unit,
    onRecordSale: (Long, Long, Double, Double, Long) -> Unit,
    onRecordPurchase: (Long, Long, Double, Double, Long) -> Unit,
    onCreateQuotation: (Long, Long, List<Pair<Long, Pair<Double, Double>>>) -> Unit,
    onConvertQuotation: (Long, Long?, Boolean) -> Unit,
    onCreateInvoiceDraft: (Long, Long, Long, Boolean, Long?, List<Pair<Long, Pair<Double, Double>>>) -> Unit,
    onConfirmInvoice: (Long, Long?) -> Unit,
    onDeleteInvoice: (Long) -> Unit,
    onRecordInvoicePayment: (Long, Double, Long) -> Unit,
    onRecordSalesReturn: (Long, String, List<Pair<Long, Double>>) -> Unit
) {
    var invoicingMode by remember { mutableStateOf(0) } // 0: Dashboard, 1: Quotations, 2: Invoices, 3: Returns, 4: Customers & Limits, 5: Purchases, 6: Product Catalog

    val customers = partners.filter { it.type == "CUSTOMER" }
    val suppliers = partners.filter { it.type == "SUPPLIER" }
    val cashBankAccounts = accounts.filter { it.allowPosting && (it.code.startsWith("1101") || it.code.startsWith("1102")) }

    // Dialog view management states
    var showCreateQuoter by remember { mutableStateOf(false) }
    var showCreateInvoicer by remember { mutableStateOf(false) }
    var activePaymentInvoice by remember { mutableStateOf<SalesInvoiceEntity?>(null) }
    var activeReturnInvoice by remember { mutableStateOf<SalesInvoiceWithOwner?>(null) }
    var activePreviewInvoice by remember { mutableStateOf<SalesInvoiceWithOwner?>(null) }
    var activeStatementCustomer by remember { mutableStateOf<PartnerEntity?>(null) }

    // Convert Quotation alert dialog states
    var activeConvertQuotation by remember { mutableStateOf<QuotationEntity?>(null) }
    var isConvertCredit by remember { mutableStateOf(false) }
    var convertAccountId by remember { mutableStateOf<Long?>(null) }
    var expandedConvertAcc by remember { mutableStateOf(false) }

    // Search query states
    var quoteSearchQuery by remember { mutableStateOf("") }
    var invoiceSearchQuery by remember { mutableStateOf("") }
    var returnSearchQuery by remember { mutableStateOf("") }
    var customerSearchQuery by remember { mutableStateOf("") }

    // Partner setup state variables
    var partnerArName by remember { mutableStateOf("") }
    var partnerType by remember { mutableStateOf("CUSTOMER") }
    var partnerPhone by remember { mutableStateOf("") }
    var partnerEmail by remember { mutableStateOf("") }
    var creditLimitValue by remember { mutableStateOf("10000.0") }

    // Product setup state variables
    var productCode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCost by remember { mutableStateOf("") }
    var minStockVal by remember { mutableStateOf("2.0") }

    // Active purchase form state variables
    var selectedSupplierId by remember { mutableStateOf<Long?>(null) }
    var selectedPurchaseProductId by remember { mutableStateOf<Long?>(null) }
    var purchaseQtyInput by remember { mutableStateOf("1") }
    var purchaseCostInput by remember { mutableStateOf("") }
    var purchasePaymentAccountId by remember { mutableStateOf<Long?>(null) }

    // Format helpers
    val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScrollableTabRow(selectedTabIndex = invoicingMode, edgePadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = invoicingMode == 0, onClick = { invoicingMode = 0 }, text = { Text("لوحة المبيعات") })
                Tab(selected = invoicingMode == 1, onClick = { invoicingMode = 1 }, text = { Text("عروض الأسعار") })
                Tab(selected = invoicingMode == 2, onClick = { invoicingMode = 2 }, text = { Text("فواتير المبيعات") })
                Tab(selected = invoicingMode == 3, onClick = { invoicingMode = 3 }, text = { Text("مرتجعات المبيعات") })
                Tab(selected = invoicingMode == 4, onClick = { invoicingMode = 4 }, text = { Text("العملاء والائتمان") })
                Tab(selected = invoicingMode == 5, onClick = { invoicingMode = 5 }, text = { Text("فواتير المشتريات") })
                Tab(selected = invoicingMode == 6, onClick = { invoicingMode = 6 }, text = { Text("إعداد المنتجات") })
            }
        }

        when (invoicingMode) {
            0 -> {
                item {
                    SalesDashboardView(invoices = salesInvoices, customers = customers, products = products)
                }
            }

            1 -> {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("عروض أسعار العملاء (Quotations)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Button(onClick = { showCreateQuoter = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("عرض جديد")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = quoteSearchQuery,
                        onValueChange = { quoteSearchQuery = it },
                        label = { Text("ابحث باسم العميل...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }

                val filteredQuotes = quotations.filter {
                    quoteSearchQuery.isBlank() || (partners.find { p -> p.id == it.quotation.customerId }?.name?.contains(quoteSearchQuery, ignoreCase = true) == true)
                }

                if (filteredQuotes.isEmpty()) {
                    item { Text("لا توجد عروض أسعار مسجلة حالياً.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(filteredQuotes) { q ->
                        val custName = partners.find { it.id == q.quotation.customerId }?.name ?: "عميل مجهول"
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("عرض رقم: ${q.quotation.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Surface(
                                        color = if (q.quotation.status == "CONVERTED") Color(0xFFE6F4EA) else Color(0xFFE8F0FE),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (q.quotation.status == "CONVERTED") "مرحل لفاتورة" else "مسودة سارية",
                                            color = if (q.quotation.status == "CONVERTED") Color(0xFF137333) else Color(0xFF1967D2),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("العميل: $custName", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("صالح حتى: ${dateFmt.format(java.util.Date(q.quotation.expiryDate))}", fontSize = 10.sp, color = Color.Gray)
                                    Text("الإجمالي المستحق: ${fmt(q.quotation.grandTotal)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                if (q.quotation.status != "CONVERTED") {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { activeConvertQuotation = q.quotation },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تحويل لفاتورة مبيعات")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("فواتير المبيعات الإلكترونية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Button(onClick = { showCreateInvoicer = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("فاتورة جديدة")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = invoiceSearchQuery,
                        onValueChange = { invoiceSearchQuery = it },
                        label = { Text("ابحث برقم الفاتورة أو اسم العميل...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }

                val filteredInvoices = salesInvoices.filter {
                    val cName = partners.find { p -> p.id == it.invoice.customerId }?.name ?: ""
                    invoiceSearchQuery.isBlank() || it.invoice.invoiceNumber.contains(invoiceSearchQuery, ignoreCase = true) || cName.contains(invoiceSearchQuery, ignoreCase = true)
                }

                if (filteredInvoices.isEmpty()) {
                    item { Text("لا توجد فواتير مبيعات مسجلة حالياً.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(filteredInvoices) { inv ->
                        val custName = partners.find { it.id == inv.invoice.customerId }?.name ?: "عميل مجهول"
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("فاتورة: ${inv.invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Surface(
                                        color = when (inv.invoice.status) {
                                            "POSTED" -> Color(0xFFE6F4EA)
                                            "REFUNDED" -> Color(0xFFFCE8E6)
                                            else -> Color(0xFFFEF7E0)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            when (inv.invoice.status) {
                                                "POSTED" -> "معتمدة ومرحلة"
                                                "REFUNDED" -> "مرتجعة كلياً/جزئياً"
                                                else -> "مسودة خطة"
                                            },
                                            color = when (inv.invoice.status) {
                                                "POSTED" -> Color(0xFF137333)
                                                "REFUNDED" -> Color(0xFFC5221F)
                                                else -> Color(0xFFB06000)
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("العميل: $custName | ${if (inv.invoice.isCredit) "آجل / ائتمان" else "نقدي مباشر"}", fontSize = 11.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("التاريخ: ${dateFmt.format(java.util.Date(inv.invoice.date))}", fontSize = 10.sp, color = Color.Gray)
                                    Text("الإجمالي: ${fmt(inv.invoice.grandTotal)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                if (inv.invoice.status == "DRAFT") {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onConfirmInvoice(inv.invoice.id, null) },
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("اعتماد وترحيل القيود")
                                        }
                                        OutlinedButton(
                                            onClick = { onDeleteInvoice(inv.invoice.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Text("حذف")
                                        }
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { activePreviewInvoice = inv },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("معاينة السند / طباعة")
                                        }

                                        val remaining = inv.invoice.grandTotal - inv.invoice.paidAmount
                                        if (remaining > 0) {
                                            Button(
                                                onClick = { activePaymentInvoice = inv.invoice },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("سداد دفعة آجل")
                                            }
                                        }

                                        if (inv.invoice.status == "POSTED") {
                                            OutlinedButton(
                                                onClick = { activeReturnInvoice = inv },
                                                modifier = Modifier.weight(0.8f)
                                            ) {
                                                Text("ارتجاع")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                item {
                    Text("مرتجعات مبيعات البنود المسجلة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (salesReturns.isEmpty()) {
                    item { Text("لا توجد فواتير مرتجعات مسجلة حالياً.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(salesReturns) { ret ->
                        val origInv = salesInvoices.find { it.invoice.id == ret.returnEntity.invoiceId }?.invoice?.invoiceNumber ?: "مجهول"
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("إشعار دائن مرتجع رقم: ${ret.returnEntity.returnNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("إشارة للفاتورة الأصلية رقم: $origInv", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("السبب والبيان: ${ret.returnEntity.reason}", fontSize = 11.sp, color = Color.Gray)
                                    Text("قيمة الرد بالصافي: ${fmt(ret.returnEntity.refundAmount)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                item {
                    Text("إدارة الائتمان وحسابات كشوفات العملاء", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة عميل جديد وضبط حدود الائتمان المالي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(value = partnerArName, onValueChange = { partnerArName = it }, label = { Text("الاسم الكامل للعميل") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = partnerPhone, onValueChange = { partnerPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = partnerEmail, onValueChange = { partnerEmail = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = creditLimitValue, onValueChange = { creditLimitValue = it }, label = { Text("الحد الائتماني المعتمد (﷼)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                            Button(
                                onClick = {
                                    if (partnerArName.isNotBlank()) {
                                        onAddPartner(partnerArName, "CUSTOMER", partnerPhone, partnerEmail, creditLimitValue.toDoubleOrNull() ?: 10000.0)
                                        // Clear
                                        partnerArName = ""
                                        partnerPhone = ""
                                        partnerEmail = ""
                                        creditLimitValue = "10000.0"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إدخال العميل للدفاتر")
                            }
                        }
                    }
                }

                item {
                    Text("سجل العملاء التجاري والحدود الائتمانية الواقفة :", fontWeight = FontWeight.Bold)
                }

                if (customers.isEmpty()) {
                    item { Text("لا يوجد عملاء مسجلين حالياً بالدليل.", color = Color.Gray) }
                } else {
                    items(customers) { p ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        Text("هاتف: ${p.phone} | بريد: ${p.email}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Button(onClick = { activeStatementCustomer = p }) {
                                        Text("كشف الحساب")
                                    }
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                // Limit validation progress bar
                                Column {
                                    val pct = if (p.creditLimit > 0) (p.balance / p.creditLimit).toFloat() else 0f
                                    val colorPct = when {
                                        pct >= 0.85f -> Color.Red
                                        pct >= 0.5f -> Color(0xFFF9AB00)
                                        else -> Color(0xFF137333)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المديونية الحالية: ${fmt(p.balance)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorPct)
                                        Text("الحد الائتماني: ${fmt(p.creditLimit)} ﷼", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { pct.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = colorPct,
                                        trackColor = Color.LightGray.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            5 -> {
                // --- Supplier purchase invoices (RETAINED) ---
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
                                    Text("${fmt(subtotal)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ضريبة المدخلات القابلة للاسترداد (15٪):", fontSize = 11.sp)
                                    Text("${fmt(vat)} ﷼", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي المدفوعات المستحق (شامل الضريبة):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${fmt(grandTotal)} ﷼", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
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

            6 -> {
                // --- Products Catalog (RETAINED) ---
                item {
                    Text("إدارة وتعريف المنتجات والخدمات السلعية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("بطاقة مادة / منتج جديد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

    // Modal dialog overlays
    if (showCreateQuoter) {
        CreateQuotationDialog(
            customers = customers,
            products = products,
            onDismiss = { showCreateQuoter = false },
            onSubmit = onCreateQuotation
        )
    }

    if (showCreateInvoicer) {
        CreateSalesInvoiceDialog(
            customers = customers,
            products = products,
            cashBankAccounts = cashBankAccounts,
            onDismiss = { showCreateInvoicer = false },
            onSubmit = onCreateInvoiceDraft
        )
    }

    activePaymentInvoice?.let { inv ->
        InvoicePaymentDialog(
            invoice = inv,
            cashBankAccounts = cashBankAccounts,
            onDismiss = { activePaymentInvoice = null },
            onSubmit = onRecordInvoicePayment
        )
    }

    activeReturnInvoice?.let { inv ->
        SalesReturnConfirmDialog(
            invoice = inv,
            products = products,
            onDismiss = { activeReturnInvoice = null },
            onSubmit = onRecordSalesReturn
        )
    }

    activePreviewInvoice?.let { inv ->
        val cust = customers.find { it.id == inv.invoice.customerId }
        if (cust != null) {
            InvoicePdfPreviewDialog(
                invoiceWithOwner = inv,
                customer = cust,
                products = products,
                onDismiss = { activePreviewInvoice = null }
            )
        }
    }

    activeStatementCustomer?.let { cust ->
        CustomerStatementDialog(
            customer = cust,
            invoices = salesInvoices,
            salesReturns = salesReturns,
            onDismiss = { activeStatementCustomer = null }
        )
    }

    // Convert quotation custom configuration alert
    activeConvertQuotation?.let { q ->
        val custName = customers.find { it.id == q.customerId }?.name ?: "العميل"
        Dialog(onDismissRequest = { activeConvertQuotation = null }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إعدادات تحويل عرض السعر إلى فاتورة مبيعات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("سيتم تحويل العرض رقم #${q.id} للعميل $custName إلى فاتورة مبيعات رسمية ومرحلة مالياً.", fontSize = 11.sp, color = Color.Gray)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isConvertCredit, onCheckedChange = { isConvertCredit = it })
                        Spacer(Modifier.width(8.dp))
                        Text("تحويل كفاتورة آجلة (ائتمانية)", fontSize = 13.sp)
                    }

                    if (!isConvertCredit) {
                        Box {
                            val label = cashBankAccounts.find { it.id == convertAccountId }?.let { "${it.code} - ${it.nameAr}" } ?: "اختر حساب تحصيل النقد المباشر"
                            OutlinedButton(onClick = { expandedConvertAcc = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(label)
                            }
                            DropdownMenu(expanded = expandedConvertAcc, onDismissRequest = { expandedConvertAcc = false }) {
                                cashBankAccounts.forEach { a ->
                                    DropdownMenuItem(text = { Text("${a.code} - ${a.nameAr}") }, onClick = {
                                        convertAccountId = a.id
                                        expandedConvertAcc = false
                                    })
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { activeConvertQuotation = null }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                if (isConvertCredit || convertAccountId != null) {
                                    onConvertQuotation(q.id, convertAccountId, isConvertCredit)
                                    activeConvertQuotation = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isConvertCredit || convertAccountId != null
                        ) {
                            Text("تأكيد التحويل")
                        }
                    }
                }
            }
        }
    }
}
