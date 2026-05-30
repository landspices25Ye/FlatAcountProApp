package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.StockMovementEntity
import com.example.data.WarehouseEntity
import com.example.core.utils.FormatUtils.formatCurrency
import com.example.core.utils.FormatUtils.formatDate

@Composable
fun AdvancedInventoryTab(
    products: List<ProductEntity>,
    movements: List<StockMovementEntity>,
    warehouses: List<WarehouseEntity>,
    onAdjustStock: (Long, Long?, Double, String) -> Unit,
    onAddWarehouse: (String, String, String, String, Boolean) -> Unit,
    onUpdateWarehouse: (WarehouseEntity) -> Unit,
    onTransferStock: (Long, Long, Long, Double, String) -> Unit
) {
    var invViewMode by remember { mutableStateOf(0) } // 0: Warehouse Stock, 1: Stock Transfers, 2: Inventory Adjustments, 3: Movements, 4: Warehouse Mgt
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = invViewMode,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = invViewMode == 0, onClick = { invViewMode = 0 }, text = { Text("الأرصدة والمستودعات") })
            Tab(selected = invViewMode == 1, onClick = { invViewMode = 1 }, text = { Text("تحويل بين المستودعات") })
            Tab(selected = invViewMode == 2, onClick = { invViewMode = 2 }, text = { Text("تسوية جردية") })
            Tab(selected = invViewMode == 3, onClick = { invViewMode = 3 }, text = { Text("سجل حركات المخزون") })
            Tab(selected = invViewMode == 4, onClick = { invViewMode = 4 }, text = { Text("إعداد المستودعات") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (invViewMode) {
                0 -> WarehouseStockSubTab(products, warehouses, movements)
                1 -> TransferStockSubTab(products, warehouses, onTransferStock)
                2 -> InventoryAdjustmentsSubTab(products, warehouses, onAdjustStock)
                3 -> StockMovementsSubTab(movements, products, warehouses)
                4 -> WarehouseManagementSubTab(warehouses, onAddWarehouse, onUpdateWarehouse)
            }
        }
    }
}

@Composable
fun WarehouseStockSubTab(products: List<ProductEntity>, warehouses: List<WarehouseEntity>, movements: List<StockMovementEntity>) {
    Text("الأرصدة الحالية للمستودعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))

    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد منتجات معرفة. يرجى إضافتها من إعداد المنتجات.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(products) { p ->
                val lowStock = p.stock <= p.minStock
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${p.code} - ${p.name}", fontWeight = FontWeight.Bold)
                                Text("متوسط التكلفة: ${formatCurrency(p.cost)} | القيمة الكلية: ${formatCurrency(p.stock * p.cost)}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${p.stock} وحدة", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (lowStock) Color.Red else Color(0xFF137333))
                                if (lowStock) Text("مخزون منخفض!", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (warehouses.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("توزيع المخزون على المستودعات:", fontSize = 10.sp, color = Color.Gray)
                            warehouses.forEach { wh ->
                                // Calculate stock for this warehouse
                                val whStock = movements.filter { it.productId == p.id && it.warehouseId == wh.id }
                                    .sumOf { it.quantity }
                                if (whStock != 0.0) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("• ${wh.name}", fontSize = 11.sp)
                                        Text("$whStock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransferStockSubTab(products: List<ProductEntity>, warehouses: List<WarehouseEntity>, onTransferStock: (Long, Long, Long, Double, String) -> Unit) {
    if (warehouses.size < 2) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("يجب أن يكون لديك مستودعين على الأقل لإجراء عملية تحويل.", color = Color.Red)
        }
        return
    }

    var selectedProdId by remember { mutableStateOf<Long?>(null) }
    var selectedFromWh by remember { mutableStateOf<Long?>(null) }
    var selectedToWh by remember { mutableStateOf<Long?>(null) }
    var qtyInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    // Dropdown expanding states
    var expProd by remember { mutableStateOf(false) }
    var expFromWh by remember { mutableStateOf(false) }
    var expToWh by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("تحويل المخزون بين المستودعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Box {
            val label = products.find { it.id == selectedProdId }?.name ?: "اختر المنتج"
            OutlinedButton(onClick = { expProd = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
            DropdownMenu(expanded = expProd, onDismissRequest = { expProd = false }) {
                products.forEach { 
                    DropdownMenuItem(text = { Text(it.name) }, onClick = { selectedProdId = it.id; expProd = false }) 
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                val label = warehouses.find { it.id == selectedFromWh }?.name ?: "من مستودع"
                OutlinedButton(onClick = { expFromWh = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                DropdownMenu(expanded = expFromWh, onDismissRequest = { expFromWh = false }) {
                    warehouses.forEach { 
                        DropdownMenuItem(text = { Text(it.name) }, onClick = { selectedFromWh = it.id; expFromWh = false }) 
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                val label = warehouses.find { it.id == selectedToWh }?.name ?: "إلى مستودع"
                OutlinedButton(onClick = { expToWh = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                DropdownMenu(expanded = expToWh, onDismissRequest = { expToWh = false }) {
                    warehouses.forEach { 
                        if (it.id != selectedFromWh) {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { selectedToWh = it.id; expToWh = false })
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = qtyInput, onValueChange = { qtyInput = it },
            label = { Text("الكمية المحولة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = descInput, onValueChange = { descInput = it },
            label = { Text("السبب أو المرجع") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val q = qtyInput.toDoubleOrNull() ?: 0.0
                if (selectedProdId != null && selectedFromWh != null && selectedToWh != null && q > 0) {
                    onTransferStock(selectedProdId!!, selectedFromWh!!, selectedToWh!!, q, descInput)
                    selectedProdId = null; selectedFromWh = null; selectedToWh = null
                    qtyInput = ""; descInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedProdId != null && selectedFromWh != null && selectedToWh != null && qtyInput.isNotEmpty()
        ) {
            Text("تأكيد تحويل المخزون")
        }
    }
}

@Composable
fun InventoryAdjustmentsSubTab(products: List<ProductEntity>, warehouses: List<WarehouseEntity>, onAdjustStock: (Long, Long?, Double, String) -> Unit) {
    var selectedAdjustProductId by remember { mutableStateOf<Long?>(null) }
    var selectedAdjustWhId by remember { mutableStateOf<Long?>(null) }
    var adjustQtyInput by remember { mutableStateOf("") }
    var adjustDescInput by remember { mutableStateOf("") }
    var expProd by remember { mutableStateOf(false) }
    var expWh by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("تسجيل تسوية جردية مخزنية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("تستخدم للإدخال والإخراج المباشر اليدوي لتسوية الفروقات والتوالف", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        Box {
            val label = products.find { it.id == selectedAdjustProductId }?.name ?: "اختر المنتج للتسوية"
            OutlinedButton(onClick = { expProd = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
            DropdownMenu(expanded = expProd, onDismissRequest = { expProd = false }) {
                products.forEach { p ->
                    DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedAdjustProductId = p.id; expProd = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (warehouses.isNotEmpty()) {
            Box {
                val label = warehouses.find { it.id == selectedAdjustWhId }?.name ?: "اختر المستودع المستهدف"
                OutlinedButton(onClick = { expWh = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                DropdownMenu(expanded = expWh, onDismissRequest = { expWh = false }) {
                    warehouses.forEach { w ->
                        DropdownMenuItem(text = { Text(w.name) }, onClick = { selectedAdjustWhId = w.id; expWh = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = adjustQtyInput, onValueChange = { adjustQtyInput = it },
            label = { Text("قيمة التغيير (مثال: 5 للزيادة، -3 للعجز)") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = adjustDescInput, onValueChange = { adjustDescInput = it },
            label = { Text("سبب التسوية (مثال: تلف جزء من الشحنة أو فروق جرد)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val pId = selectedAdjustProductId
                val qM = adjustQtyInput.toDoubleOrNull() ?: 0.0
                if (pId != null && qM != 0.0) {
                    onAdjustStock(pId, selectedAdjustWhId, qM, adjustDescInput)
                    selectedAdjustProductId = null; selectedAdjustWhId = null
                    adjustQtyInput = ""; adjustDescInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedAdjustProductId != null && adjustQtyInput.isNotEmpty()
        ) {
            Text("ترحيل التسوية المخزنية")
        }
    }
}

@Composable
fun StockMovementsSubTab(movements: List<StockMovementEntity>, products: List<ProductEntity>, warehouses: List<WarehouseEntity>) {
    Text("سجل حركات المخزون التفصيلي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))

    if (movements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد حركات مخزنية مسجلة حتى الآن.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(movements) { mv ->
                val p = products.find { it.id == mv.productId }
                val whName = warehouses.find { it.id == mv.warehouseId }?.name ?: "المخزون العام"
                val isIncoming = mv.type.endsWith("_IN") || mv.type == "PURCHASE" || (mv.type == "ADJUSTMENT" && mv.quantity > 0)
                
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(p?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                            Text("${mv.type} | مستودع: $whName", fontSize = 11.sp, color = Color.Gray)
                            Text("السبب: ${mv.description}", fontSize = 11.sp, color = Color.DarkGray)
                            Text(formatDate(mv.date), fontSize = 10.sp, color = Color.Gray)
                        }
                        Text(
                            "${if (mv.quantity > 0) "+" else ""}${mv.quantity}",
                            fontWeight = FontWeight.Bold, color = if (mv.quantity > 0) Color(0xFF137333) else Color.Red, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarehouseManagementSubTab(warehouses: List<WarehouseEntity>, onAddWarehouse: (String, String, String, String, Boolean) -> Unit, onUpdateWarehouse: (WarehouseEntity) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("إدارة المستودعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Warehouse", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مستودع")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (warehouses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد مستودعات معرفة.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(warehouses) { wh ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(wh.name, fontWeight = FontWeight.Bold)
                                Text("الكود: ${wh.code} | الموقع: ${wh.location}", fontSize = 11.sp, color = Color.Gray)
                                if (wh.manager.isNotBlank()) Text("أمين المستودع: ${wh.manager}", fontSize = 11.sp)
                            }
                            if (wh.isDefault) {
                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) {
                                    Text("الافتراضي")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var manager by remember { mutableStateOf("") }
        var isDef by remember { mutableStateOf(warehouses.isEmpty()) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة مستودع جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المستودع") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("كود المستودع") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("الموقع الفرعي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = manager, onValueChange = { manager = it }, label = { Text("أمين المستودع (اختياري)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDef, onCheckedChange = { isDef = it })
                        Text("تعيين كمستودع افتراضي للعمليات", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddWarehouse(name, code, location, manager, isDef)
                    showAddDialog = false
                }, enabled = name.isNotBlank() && code.isNotBlank()) {
                    Text("حفظ المستودع")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
