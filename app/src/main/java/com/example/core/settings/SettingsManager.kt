package com.example.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.core.errors.BusinessRuleError
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ==========================================
// DATA MODELS
// ==========================================

data class CompanyProfile(
    val name: String = "المحاسب المالي المبتكر",
    val legalName: String = "شركة الحلول البرمجية الذكية المحدودة",
    val crNumber: String = "1010123456",
    val taxNumber: String = "310123456789013",
    val phone: String = "+966501234567",
    val email: String = "info@finance-solution.sa",
    val website: String = "https://finance-solution.sa",
    val address: String = "الرياض - شارع العليا العام - برج الابتكار",
    val logoUrl: String = "",
    val currency: String = "SAR",
    val taxRate: Double = 15.0,
    val decimalPlaces: Int = 2,
    val invoicePrefix: String = "INV",
    val invoiceNotes: String = "شكراً لتعاملكم معنا. الخصم غير مسترجع بعد 14 يوماً.",
    val fiscalYearStartMonth: Int = 1,
    val theme: String = "DARK", // LIGHT, DARK
    val language: String = "AR", // AR, EN
    val notificationsEnabled: Boolean = true
)

data class FiscalPeriod(
    val id: String, // e.g. "2026-01"
    val year: Int,
    val month: Int,
    val nameAr: String,
    var status: String // OPEN, CLOSED, LOCKED
)

data class NumberSequence(
    val type: String, // INVOICE, JOURNAL, PARTNER, EMPLOYEE, INVENTORY
    val prefix: String,
    var current: Int,
    val digits: Int = 5,
    val autoResetAnnually: Boolean = true
)

data class UnitOfMeasure(
    val id: String,
    val name: String,
    val symbol: String,
    val baseUnitId: String? = null,
    val factor: Double = 1.0 // ratio to base unit
)

data class CostCenter(
    val id: String,
    val code: String,
    val name: String,
    val parentId: String? = null
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // COMPANY, PERIOD, ACCOUNT, SEQUENCE, UNIT, COST_CENTER, SYNC
    val description: String,
    val user: String = "مدير المبيعات"
)

// ==========================================
// SETTINGS MANAGER (Offline-first AsyncStorage-like)
// ==========================================

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("erp_settings_prefs", Context.MODE_PRIVATE)

    init {
        // Hydrate initial defaults on first launch
        if (!prefs.contains("company_name")) {
            saveCompanyProfile(CompanyProfile())
            seedFiscalPeriods()
            seedNumberSequences()
            seedUnitsOfMeasure()
            seedCostCenters()
            logAudit("SYSTEM", "تم تشغيل النظام لأول مرة وتهيئة الإعدادات الافتراضية للشركة")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun initialize(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): SettingsManager {
            return INSTANCE ?: throw IllegalStateException("SettingsManager has not been initialized. Call initialize(context) first.")
        }
    }

    // ------------------------------------------
    // 1. COMPANY PROFILE
    // ------------------------------------------

    fun getCompanyProfile(): CompanyProfile {
        return CompanyProfile(
            name = prefs.getString("company_name", "المحاسب المالي المبتكر")!!,
            legalName = prefs.getString("company_legal_name", "شركة الحلول البرمجية الذكية المحدودة")!!,
            crNumber = prefs.getString("company_cr_number", "1010123456")!!,
            taxNumber = prefs.getString("company_tax_number", "310123456789013")!!,
            phone = prefs.getString("company_phone", "+966501234567")!!,
            email = prefs.getString("company_email", "info@finance-solution.sa")!!,
            website = prefs.getString("company_website", "https://finance-solution.sa")!!,
            address = prefs.getString("company_address", "الرياض - شارع العليا العام - برج الابتكار")!!,
            logoUrl = prefs.getString("company_logo_url", "")!!,
            currency = prefs.getString("company_currency", "SAR")!!,
            taxRate = prefs.getFloat("company_tax_rate", 15.0f).toDouble(),
            decimalPlaces = prefs.getInt("company_decimal_places", 2),
            invoicePrefix = prefs.getString("company_invoice_prefix", "INV")!!,
            invoiceNotes = prefs.getString("company_invoice_notes", "شكراً لتعاملكم معنا. الخصم غير مسترجع بعد 14 يوماً.")!!,
            fiscalYearStartMonth = prefs.getInt("company_fiscal_start", 1),
            theme = prefs.getString("app_theme", "DARK")!!,
            language = prefs.getString("app_language", "AR")!!,
            notificationsEnabled = prefs.getBoolean("app_notifications", true)
        )
    }

    fun saveCompanyProfile(profile: CompanyProfile) {
        prefs.edit().apply {
            putString("company_name", profile.name)
            putString("company_legal_name", profile.legalName)
            putString("company_cr_number", profile.crNumber)
            putString("company_tax_number", profile.taxNumber)
            putString("company_phone", profile.phone)
            putString("company_email", profile.email)
            putString("company_website", profile.website)
            putString("company_address", profile.address)
            putString("company_logo_url", profile.logoUrl)
            putString("company_currency", profile.currency)
            putFloat("company_tax_rate", profile.taxRate.toFloat())
            putInt("company_decimal_places", profile.decimalPlaces)
            putString("company_invoice_prefix", profile.invoicePrefix)
            putString("company_invoice_notes", profile.invoiceNotes)
            putInt("company_fiscal_start", profile.fiscalYearStartMonth)
            putString("app_theme", profile.theme)
            putString("app_language", profile.language)
            putBoolean("app_notifications", profile.notificationsEnabled)
            apply()
        }
        logAudit("COMPANY", "تم تحديث الملف الشخصي وتفضيلات الشركة")
    }

    // ------------------------------------------
    // 2. FISCAL PERIODS
    // ------------------------------------------

    private fun seedFiscalPeriods() {
        val list = mutableListOf<FiscalPeriod>()
        val monthsAr = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        for (m in 1..12) {
            val id = String.format("2026-%02d", m)
            list.add(FiscalPeriod(id, 2026, m, monthsAr[m - 1], "OPEN"))
        }
        saveFiscalPeriods(list)
    }

    fun getFiscalPeriods(): List<FiscalPeriod> {
        val raw = prefs.getString("fiscal_periods_json", null) ?: return emptyList()
        val list = mutableListOf<FiscalPeriod>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FiscalPeriod(
                        id = obj.getString("id"),
                        year = obj.getInt("year"),
                        month = obj.getInt("month"),
                        nameAr = obj.getString("nameAr"),
                        status = obj.getString("status")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "Error loading fiscal periods", e)
        }
        return list
    }

    fun saveFiscalPeriods(periods: List<FiscalPeriod>) {
        val arr = JSONArray()
        for (p in periods) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("year", p.year)
                put("month", p.month)
                put("nameAr", p.nameAr)
                put("status", p.status)
            }
            arr.put(obj)
        }
        prefs.edit().putString("fiscal_periods_json", arr.toString()).apply()
    }

    fun updateFiscalPeriodStatus(id: String, newStatus: String) {
        val periods = getFiscalPeriods()
        val period = periods.find { it.id == id } ?: throw BusinessRuleError("الفترة المالية غير موجودة")

        // Rules check
        if (newStatus == "CLOSED" && period.status == "LOCKED") {
            throw BusinessRuleError("لا يمكن فتح فترة مالية تم إقفالها نهائياً")
        }
        if (newStatus == "LOCKED" && period.status == "OPEN") {
            throw BusinessRuleError("يجب إغلاق الفترة المالية أولاً قبل إقفالها نهائياً")
        }

        period.status = newStatus
        saveFiscalPeriods(periods)
        logAudit("PERIOD", "تعديل حالة الفترة المالية [${period.id}] إلى $newStatus")
    }

    // ------------------------------------------
    // 3. DEFAULT ACCOUNTS
    // ------------------------------------------

    fun getLinkedAccountCode(key: String, fallback: String): String {
        return prefs.getString("link_account_$key", fallback)!!
    }

    fun setLinkedAccountCode(key: String, code: String) {
        prefs.edit().putString("link_account_$key", code).apply()
        logAudit("ACCOUNT_LINK", "ربط الحساب الافتراضي [$key] بالحساب [$code]")
    }

    fun getLinkedAccounts(): Map<String, String> {
        return mapOf(
            "CASH" to getLinkedAccountCode("CASH", "1101"),
            "BANK" to getLinkedAccountCode("BANK", "1102"),
            "RECEIVABLE" to getLinkedAccountCode("RECEIVABLE", "1201"),
            "INVENTORY" to getLinkedAccountCode("INVENTORY", "1301"),
            "PAYABLE" to getLinkedAccountCode("PAYABLE", "2101"),
            "SALES" to getLinkedAccountCode("SALES", "4101"),
            "COGS" to getLinkedAccountCode("COGS", "5101"),
            "PAYROLL" to getLinkedAccountCode("PAYROLL", "5201")
        )
    }

    fun resetToDefaults() {
        setLinkedAccountCode("CASH", "1101")
        setLinkedAccountCode("BANK", "1102")
        setLinkedAccountCode("RECEIVABLE", "1201")
        setLinkedAccountCode("INVENTORY", "1301")
        setLinkedAccountCode("PAYABLE", "2101")
        setLinkedAccountCode("SALES", "4101")
        setLinkedAccountCode("COGS", "5101")
        setLinkedAccountCode("PAYROLL", "5201")
        logAudit("ACCOUNT_LINK", "إعادة ضبط روابط الحسابات الافتراضية للقيم القياسية")
    }

    // ------------------------------------------
    // 4. NUMBER SEQUENCES
    // ------------------------------------------

    private fun seedNumberSequences() {
        val list = listOf(
            NumberSequence("INVOICE", "INV-", 21),
            NumberSequence("JOURNAL", "JV-", 5),
            NumberSequence("PARTNER", "PART-", 12),
            NumberSequence("EMPLOYEE", "EMP-", 8),
            NumberSequence("INVENTORY", "ADJ-", 3)
        )
        saveNumberSequences(list)
    }

    fun getNumberSequences(): List<NumberSequence> {
        val raw = prefs.getString("num_seqs_json", null) ?: return emptyList()
        val list = mutableListOf<NumberSequence>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NumberSequence(
                        type = obj.getString("type"),
                        prefix = obj.getString("prefix"),
                        current = obj.getInt("current"),
                        digits = obj.getInt("digits"),
                        autoResetAnnually = obj.getBoolean("autoResetAnnually")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "Error reading number sequences", e)
        }
        return list
    }

    fun saveNumberSequences(seqs: List<NumberSequence>) {
        val arr = JSONArray()
        for (s in seqs) {
            val obj = JSONObject().apply {
                put("type", s.type)
                put("prefix", s.prefix)
                put("current", s.current)
                put("digits", s.digits)
                put("autoResetAnnually", s.autoResetAnnually)
            }
            arr.put(obj)
        }
        prefs.edit().putString("num_seqs_json", arr.toString()).apply()
    }

    fun updateSequence(type: String, newPrefix: String, newCurrent: Int) {
        val seqs = getNumberSequences().toMutableList()
        val idx = seqs.indexOfFirst { it.type == type }
        if (idx != -1) {
            val old = seqs[idx]
            if (newCurrent < old.current) {
                throw BusinessRuleError("لا يمكن تعيين الرقم التسلسلي الحالي لقيمة أقل من الرقم المستخدم حالياً (${old.current})")
            }
            seqs[idx] = old.copy(prefix = newPrefix, current = newCurrent)
            saveNumberSequences(seqs)
            logAudit("SEQUENCE", "تعديل بادئة [$type] إلى \"$newPrefix\" والرقم لـ $newCurrent")
        }
    }

    fun getAndIncrementNextNumber(type: String): String {
        val seqs = getNumberSequences().toMutableList()
        val idx = seqs.indexOfFirst { it.type == type }
        if (idx != -1) {
            val seq = seqs[idx]
            val nextVal = seq.current + 1
            seqs[idx] = seq.copy(current = nextVal)
            saveNumberSequences(seqs)
            
            val formattedNum = String.format("%0${seq.digits}d", nextVal)
            return "${seq.prefix}$formattedNum"
        }
        return "GEN-${System.currentTimeMillis()}"
    }

    // ------------------------------------------
    // 5. UNITS OF MEASURE
    // ------------------------------------------

    private fun seedUnitsOfMeasure() {
        val list = listOf(
            UnitOfMeasure("piece", "حبة", "حبة"),
            UnitOfMeasure("box_12", "كرتون (12 حبة)", "كرتون", "piece", 12.0),
            UnitOfMeasure("kg", "كيلوجرام", "كجم"),
            UnitOfMeasure("meter", "متر", "متر")
        )
        saveUnitsOfMeasure(list)
    }

    fun getUnitsOfMeasure(): List<UnitOfMeasure> {
        val raw = prefs.getString("units_json", null) ?: return emptyList()
        val list = mutableListOf<UnitOfMeasure>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UnitOfMeasure(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        symbol = obj.getString("symbol"),
                        baseUnitId = if (obj.isNull("baseUnitId")) null else obj.getString("baseUnitId"),
                        factor = obj.getDouble("factor")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "Error reading units", e)
        }
        return list
    }

    fun saveUnitsOfMeasure(units: List<UnitOfMeasure>) {
        val arr = JSONArray()
        for (u in units) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("symbol", u.symbol)
                put("baseUnitId", u.baseUnitId)
                put("factor", u.factor)
            }
            arr.put(obj)
        }
        prefs.edit().putString("units_json", arr.toString()).apply()
    }

    fun addUnitOfMeasure(name: String, symbol: String, baseUnitId: String?, factor: Double) {
        val units = getUnitsOfMeasure().toMutableList()
        val id = UUID.randomUUID().toString()
        
        // Prevent recursive definitions or bad factor values
        if (baseUnitId != null && factor <= 0.0) {
            throw BusinessRuleError("يجب أن يكون معامل تحويل الوحدة أكبر من الصفر")
        }

        units.add(UnitOfMeasure(id, name, symbol, baseUnitId, factor))
        saveUnitsOfMeasure(units)
        logAudit("UNIT", "تمت إضافة وحدة قياس جديدة: $name ($symbol)")
    }

    fun deleteUnitOfMeasure(id: String) {
        val units = getUnitsOfMeasure().toMutableList()
        // Prevent deleting if it is a base unit for other unit
        if (units.any { it.baseUnitId == id }) {
            throw BusinessRuleError("لا يمكن حذف هذه الوحدة لأنها مرتبطة كوحدة أساسية لوحدات أخرى")
        }
        units.removeAll { it.id == id }
        saveUnitsOfMeasure(units)
        logAudit("UNIT", "حذف وحدة قياس [$id]")
    }

    // ------------------------------------------
    // 6. COST CENTERS (مراكز التكلفة)
    // ------------------------------------------

    private fun seedCostCenters() {
        val list = listOf(
            CostCenter("cc1", "CC100", "إدارة التسويق والدعاية"),
            CostCenter("cc2", "CC200", "قسم الديكور والتنفيذ"),
            CostCenter("cc3", "CC300", "إدارة المشاريع العامة"),
            CostCenter("cc4", "CC301", "مشروع العليا للمكاتب", "cc3")
        )
        saveCostCenters(list)
    }

    fun getCostCenters(): List<CostCenter> {
        val raw = prefs.getString("cost_centers_json", null) ?: return emptyList()
        val list = mutableListOf<CostCenter>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CostCenter(
                        id = obj.getString("id"),
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        parentId = if (obj.isNull("parentId")) null else obj.getString("parentId")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "Error reading cost centers", e)
        }
        return list
    }

    fun saveCostCenters(centers: List<CostCenter>) {
        val arr = JSONArray()
        for (c in centers) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("code", c.code)
                put("name", c.name)
                put("parentId", c.parentId)
            }
            arr.put(obj)
        }
        prefs.edit().putString("cost_centers_json", arr.toString()).apply()
    }

    fun addCostCenter(code: String, name: String, parentId: String?) {
        val list = getCostCenters().toMutableList()
        if (list.any { it.code == code }) {
            throw BusinessRuleError("رمز مركز التكلفة [$code] مستخدم بالفعل")
        }
        val id = UUID.randomUUID().toString()
        list.add(CostCenter(id, code, name, parentId))
        saveCostCenters(list)
        logAudit("COST_CENTER", "إضافة مركز تكلفة جديد [$code] - $name")
    }

    fun deleteCostCenter(id: String) {
        val list = getCostCenters().toMutableList()
        if (list.any { it.parentId == id }) {
            throw BusinessRuleError("لا يمكن حذف مركز التكلفة لوجود فروع تابعة له")
        }
        list.removeAll { it.id == id }
        saveCostCenters(list)
        logAudit("COST_CENTER", "حذف مركز التكلفة [$id]")
    }

    // ------------------------------------------
    // 7. NOTIFICATIONS & PREFERENCES
    // ------------------------------------------

    fun getNotificationToggles(): Map<String, Boolean> {
        return mapOf(
            "LOW_STOCK" to prefs.getBoolean("toggle_notif_LOW_STOCK", true),
            "INVOICE_OVERDUE" to prefs.getBoolean("toggle_notif_INVOICE_OVERDUE", true),
            "PAYMENT_RECEIVED" to prefs.getBoolean("toggle_notif_PAYMENT_RECEIVED", true),
            "PAYROLL_RELEASE" to prefs.getBoolean("toggle_notif_PAYROLL_RELEASE", false)
        )
    }

    fun updateNotificationToggle(key: String, enabled: Boolean) {
        prefs.edit().putBoolean("toggle_notif_$key", enabled).apply()
        logAudit("NOTIFICATION", "تحديث تفضيلات إشعار [$key] ليصبح ${if (enabled) "مفعلاً" else "معطلاً"}")
    }

    // ------------------------------------------
    // 8. AUDIT LOGS & SYNC ACTIONS
    // ------------------------------------------

    fun logAudit(category: String, description: String) {
        val logs = getAuditLogs().toMutableList()
        logs.add(0, AuditLog(category = category, description = description))
        
        // Cap audit list to last 150 items to optimize local SharedPreferences bandwidth
        val capped = if (logs.size > 150) logs.take(150) else logs
        
        val arr = JSONArray()
        for (l in capped) {
            val obj = JSONObject().apply {
                put("id", l.id)
                put("timestamp", l.timestamp)
                put("category", l.category)
                put("description", l.description)
                put("user", l.user)
            }
            arr.put(obj)
        }
        prefs.edit().putString("audit_logs_json", arr.toString()).apply()
    }

    fun getAuditLogs(): List<AuditLog> {
        val raw = prefs.getString("audit_logs_json", null) ?: return emptyList()
        val list = mutableListOf<AuditLog>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AuditLog(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        category = obj.getString("category"),
                        description = obj.getString("description"),
                        user = obj.getString("user")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "Error reading audit logs", e)
        }
        return list
    }

    fun clearAuditLogs() {
        prefs.edit().remove("audit_logs_json").apply()
        logAudit("SYSTEM", "تم إفراغ سجل العمليات بنجاح")
    }

    fun getSyncSummary(): SyncSummary {
        val lastSyncMs = prefs.getLong("last_sync_timestamp", System.currentTimeMillis() - 180000L) // 3 mins ago
        val pendingChangesCount = prefs.getInt("pending_sync_count", 0)
        return SyncSummary(lastSyncMs, pendingChangesCount)
    }

    fun forceSync(context: Context, onSyncFinished: () -> Unit = {}) {
        prefs.edit()
            .putLong("last_sync_timestamp", System.currentTimeMillis())
            .putInt("pending_sync_count", 0)
            .apply()
        logAudit("SYNC", "بدء مزامنة السحابة اليدوية والمقاصة الثنائية القيمة")
        onSyncFinished()
    }

    data class SyncSummary(
        val lastSyncTimestamp: Long,
        val pendingChangesCount: Int
    )
}
