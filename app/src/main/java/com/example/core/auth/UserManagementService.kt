package com.example.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.core.errors.BusinessRuleError
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ==========================================
// USER & SECURITY DATA MODELS
// ==========================================

data class AuthUser(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val role: String, // Maps to CustomRole.id or SystemRole.name
    val phone: String = "",
    val isActive: Boolean = true,
    val isInvitationPending: Boolean = false,
    val invitedAt: Long = 0L
)

data class CustomRole(
    val id: String,
    val nameAr: String,
    val description: String,
    val isPreset: Boolean = false
)

data class ActiveSession(
    val id: String = UUID.randomUUID().toString(),
    val deviceType: String,
    val lastActive: Long = System.currentTimeMillis(),
    val location: String
)

data class UserActivity(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val eventTypeAr: String,
    val details: String,
    val status: String // "SUCCESS", "FAILED"
)

// ==========================================
// USER MANAGEMENT SERVICE
// ==========================================

class UserManagementService private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("erp_auth_prefs", Context.MODE_PRIVATE)

    init {
        // Seed default database entities if they do not exist
        if (!prefs.contains("users_json")) {
            seedUserData()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserManagementService? = null

        fun initialize(context: Context): UserManagementService {
            return INSTANCE ?: synchronized(this) {
                val instance = UserManagementService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): UserManagementService {
            return INSTANCE ?: throw IllegalStateException("UserManagementService has not been initialized. Call initialize(context) first.")
        }
    }

    // ------------------------------------------
    // SEED INITIAL DATA
    // ------------------------------------------
    private fun seedUserData() {
        // Seed Custom Roles
        val roles = listOf(
            CustomRole("owner", "المالك", "الوصول غير المحدود إلى جميع أدوات النظام والمقاصة وإعدادات الشركة والأدوار", isPreset = true),
            CustomRole("admin", "مدير النظام الكلي", "إدارة صلاحيات الموظفين، الحسابات، التهيئة والعمليات في النظام", isPreset = true),
            CustomRole("accountant", "المحاسب المالي", "إدخال القيود اليومية، عرض ميزان المراجعة، إصدار التقارير والفواتير", isPreset = true),
            CustomRole("viewer", "المراقب المالي", "عرض واطلاع فقط على التقارير المالية والقيود دون صلاحيات تعديل أو حذف", isPreset = true),
            CustomRole("trainee", "محاسب متدرب", "صلاحية استعراض القيود وإضافة فواتير مسودة بانتظار الاعتماد من المحاسب المالي", isPreset = false)
        )
        saveCustomRoles(roles)

        // Seed Users
        val users = listOf(
            AuthUser(
                id = "usr_1",
                name = "أحمد القحطاني",
                email = "ahmed@accounting-system.sa",
                role = "owner",
                phone = "+966551234567",
                isActive = true
            ),
            AuthUser(
                id = "usr_2",
                name = "سارة الشمري",
                email = "sara@accounting-system.sa",
                role = "accountant",
                phone = "+966509876543",
                isActive = true
            ),
            AuthUser(
                id = "usr_3",
                name = "خالد العتيبي",
                email = "khaled@accounting-system.sa",
                role = "viewer",
                phone = "+966541234567",
                isActive = true
            ),
            AuthUser(
                id = "usr_4",
                name = "منى الحربي",
                email = "mona@accounting-system.sa",
                role = "accountant",
                phone = "+966531112222",
                isActive = true,
                isInvitationPending = true,
                invitedAt = System.currentTimeMillis() - 172800000L // 48 hours ago
            )
        )
        saveUsersList(users)

        // Seed Default Role Permissions Matrices
        // Owner has absolute everything (checked in permission utility)
        // Others map module:action
        val accountantPerms = setOf(
            "accounting:view", "accounting:create", "accounting:edit", "accounting:post_journal",
            "reports:view", "reports:export", "inventory:view"
        )
        val viewerPerms = setOf(
            "accounting:view", "reports:view", "inventory:view", "payroll:view"
        )
        val traineePerms = setOf(
            "accounting:view", "accounting:create"
        )

        saveRolePermissions("accountant", accountantPerms)
        saveRolePermissions("viewer", viewerPerms)
        saveRolePermissions("trainee", traineePerms)

        // Seed Sessions
        val sessions = listOf(
            ActiveSession("sess_1", "Chrome - macOS (الرياض)", System.currentTimeMillis(), "الرياض، المملكة العربية السعودية"),
            ActiveSession("sess_2", "Android - Samsung Galaxy S24 (جدة)", System.currentTimeMillis() - 7200000, "جدة، المملكة العربية السعودية")
        )
        saveActiveSessions(sessions)

        // Seed Activities
        val activities = listOf(
            UserActivity(username = "أحمد القحطاني", eventTypeAr = "تسجيل دخول", details = "تسجيل دخول ناجح من الرياض عبر متصفح ديسكتوب", status = "SUCCESS"),
            UserActivity(username = "سارة الشمري", eventTypeAr = "ترحيل قيود", details = "ترحيل القيد رقم JV-00005 المالي بنجاح", status = "SUCCESS"),
            UserActivity(username = "أحمد القحطاني", eventTypeAr = "تعديل صلاحيات", details = "تغيير صلاحيات دور محاسب متدرب في النظام", status = "SUCCESS")
        )
        saveActivitiesList(activities)

        // Personal settings seed
        prefs.edit()
            .putString("current_user_name", "أحمد القحطاني")
            .putString("current_user_email", "ahmed@accounting-system.sa")
            .putString("current_user_phone", "+966551234567")
            .putString("current_user_password", "Admin@1234")
            .putBoolean("biometric_enabled", true)
            .apply()
    }

    // ------------------------------------------
    // 1. SERVICES: USER MANAGEMENT (USR-01 to USR-13)
    // ------------------------------------------

    fun getUsersByCompany(): List<AuthUser> {
        val raw = prefs.getString("users_json", null) ?: return emptyList()
        val list = mutableListOf<AuthUser>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AuthUser(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        role = obj.getString("role"),
                        phone = obj.getString("phone"),
                        isActive = obj.getBoolean("isActive"),
                        isInvitationPending = obj.getBoolean("isInvitationPending"),
                        invitedAt = obj.optLong("invitedAt", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManagementService", "Error loading users", e)
        }
        return list
    }

    fun saveUsersList(users: List<AuthUser>) {
        val arr = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("email", u.email)
                put("role", u.role)
                put("phone", u.phone)
                put("isActive", u.isActive)
                put("isInvitationPending", u.isInvitationPending)
                put("invitedAt", u.invitedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("users_json", arr.toString()).apply()
    }

    fun createUser(name: String, email: String, role: String, phone: String = ""): AuthUser {
        val users = getUsersByCompany().toMutableList()
        if (users.any { it.email.lowercase() == email.lowercase() }) {
            throw BusinessRuleError("البريد الإلكتروني للبريد مضاف مسبقاً في النظام")
        }
        val newUser = AuthUser(
            name = name,
            email = email,
            role = role,
            phone = phone,
            isActive = true,
            isInvitationPending = false
        )
        users.add(newUser)
        saveUsersList(users)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "إضافة مستخدم",
            details = "تم إنشاء حساب للمستخدم الجديد [$name - $email] بنجاح",
            status = "SUCCESS"
        )
        return newUser
    }

    fun inviteUser(email: String, role: String, phone: String = ""): AuthUser {
        val users = getUsersByCompany().toMutableList()
        if (users.any { it.email.lowercase() == email.lowercase() }) {
            throw BusinessRuleError("البريد الإلكتروني المدعو مضاف مسبقاً أو به دعوة قائمة")
        }
        val newUser = AuthUser(
            name = email.substringBefore("@"),
            email = email,
            role = role,
            phone = phone,
            isActive = true,
            isInvitationPending = true,
            invitedAt = System.currentTimeMillis()
        )
        users.add(newUser)
        saveUsersList(users)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "دعوة مستخدم",
            details = "تم إرسال دعوة بريد إلكتروني لـ [$email] بدور [${getRoleNameById(role)}]",
            status = "SUCCESS"
        )
        return newUser
    }

    fun resendInvitation(email: String) {
        val users = getUsersByCompany().toMutableList()
        val idx = users.indexOfFirst { it.email.lowercase() == email.lowercase() }
        if (idx == -1) throw BusinessRuleError("المستخدم المدعو غير موجود")
        val u = users[idx]
        if (!u.isInvitationPending) throw BusinessRuleError("لقد قبل هذا المستخدم الدعوة مسبقاً")

        users[idx] = u.copy(invitedAt = System.currentTimeMillis())
        saveUsersList(users)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "إعادة إرسال دعوة",
            details = "تمت إعادة إرسال رابط دعوة البريد الإلكتروني لـ [${u.email}]",
            status = "SUCCESS"
        )
    }

    fun revokeInvitation(email: String) {
        val users = getUsersByCompany().toMutableList()
        val removed = users.removeAll { it.email.lowercase() == email.lowercase() && it.isInvitationPending }
        if (!removed) throw BusinessRuleError("لا توجد دعوة قائمة معلقة لهذا البريد")
        saveUsersList(users)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "إلغاء دعوة",
            details = "تم إلغاء وسحب دعوة المستخدم المعلقة [$email]",
            status = "SUCCESS"
        )
    }

    fun updateUserRole(userId: String, newRole: String) {
        val users = getUsersByCompany().toMutableList()
        val idx = users.indexOfFirst { it.id == userId }
        if (idx == -1) throw BusinessRuleError("المستخدِم المطلوب غير موجود")
        val u = users[idx]

        // Rules: Owner cannot have role changed by others easily, for this system UI is secured
        users[idx] = u.copy(role = newRole)
        saveUsersList(users)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "تعديل دور مستخدم",
            details = "تغيير دور المستخدم [${u.name}] لصحيفة الدور الجديد [${getRoleNameById(newRole)}]",
            status = "SUCCESS"
        )
    }

    fun toggleUserStatus(userId: String) {
        val users = getUsersByCompany().toMutableList()
        val idx = users.indexOfFirst { it.id == userId }
        if (idx == -1) throw BusinessRuleError("المستخدِم المطلوب غير موجود")
        val u = users[idx]

        // Rules: Self deactivation is forbidden. Owner cannot be deactivated
        val myEmail = prefs.getString("current_user_email", "ahmed@accounting-system.sa")!!
        if (u.email.lowercase() == myEmail.lowercase()) {
            throw BusinessRuleError("لا يمكنك تعطيل حسابك الشخصي الذي تستخدمه حالياً")
        }
        if (u.role == "owner") {
            throw BusinessRuleError("لا يمكنك تمكين أو تعطيل حساب مالك المؤسسة الرئيسي")
        }

        val nextStatus = !u.isActive
        users[idx] = u.copy(isActive = nextStatus)
        saveUsersList(users)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = if (nextStatus) "تفعيل مستخدم" else "تعطيل مستخدم",
            details = "تم تغيير حالة الحساب للعميل [${u.name}] ليصبح [${if (nextStatus) "نشط" else "غير نشط"}]",
            status = "SUCCESS"
        )
    }

    // ------------------------------------------
    // 2. SERVICES: ROLE MANAGEMENT (USR-14 to USR-23)
    // ------------------------------------------

    fun getCustomRoles(): List<CustomRole> {
        val raw = prefs.getString("custom_roles_json", null) ?: return emptyList()
        val list = mutableListOf<CustomRole>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CustomRole(
                        id = obj.getString("id"),
                        nameAr = obj.getString("nameAr"),
                        description = obj.getString("description"),
                        isPreset = obj.getBoolean("isPreset")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManagementService", "Error loading roles", e)
        }
        return list
    }

    fun saveCustomRoles(roles: List<CustomRole>) {
        val arr = JSONArray()
        for (r in roles) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("nameAr", r.nameAr)
                put("description", r.description)
                put("isPreset", r.isPreset)
            }
            arr.put(obj)
        }
        prefs.edit().putString("custom_roles_json", arr.toString()).apply()
    }

    fun getRoleNameById(roleId: String): String {
        return getCustomRoles().find { it.id == roleId }?.nameAr ?: roleId
    }

    fun createRole(nameAr: String, description: String): CustomRole {
        val roles = getCustomRoles().toMutableList()
        if (roles.any { it.nameAr == nameAr }) {
            throw BusinessRuleError("اسم الدور [$nameAr] مضاف ومطروح مسبقاً في الشركة")
        }
        val newId = "custom_${UUID.randomUUID().toString().take(6)}"
        val newRole = CustomRole(id = newId, nameAr = nameAr, description = description, isPreset = false)
        roles.add(newRole)
        saveCustomRoles(roles)
        // Seed blank permissions
        saveRolePermissions(newId, emptySet())

        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "إنشاء دور مخصص",
            details = "تم إنشاء مسمى وظيفي أو دور صلاحيات جديد باسم [$nameAr]",
            status = "SUCCESS"
        )
        return newRole
    }

    fun updateRole(id: String, nameAr: String, description: String) {
        val roles = getCustomRoles().toMutableList()
        val idx = roles.indexOfFirst { it.id == id }
        if (idx == -1) throw BusinessRuleError("الدور المطلوب تعليمه غير موجود")
        val r = roles[idx]
        if (r.isPreset) throw BusinessRuleError("لا يمكن تعديل أدوار حزم النظام المثبتة الافتراضية")

        if (roles.any { it.nameAr == nameAr && it.id != id }) {
            throw BusinessRuleError("اسم الدور الآخر المطروح [$nameAr] مضاف ومعرف مسبقاً")
        }

        roles[idx] = r.copy(nameAr = nameAr, description = description)
        saveCustomRoles(roles)
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "تعديل تفاصيل دور",
            details = "تحديث توصيف مسمى الدور [$nameAr]",
            status = "SUCCESS"
        )
    }

    fun deleteRole(id: String) {
        val roles = getCustomRoles().toMutableList()
        val r = roles.find { it.id == id } ?: throw BusinessRuleError("الدور المطلوب غير متوفر")
        if (r.isPreset) throw BusinessRuleError("لا يمكن إزالة أو مسح الأدوار المدمجة الافتراضية للشركة")

        // Reassign affected users to Viewer
        val users = getUsersByCompany().toMutableList()
        var reassignedCount = 0
        for (i in users.indices) {
            if (users[i].role == id) {
                users[i] = users[i].copy(role = "viewer")
                reassignedCount++
            }
        }
        saveUsersList(users)

        roles.remove(r)
        saveCustomRoles(roles)

        // Remote stored role permissions
        prefs.edit().remove("role_perms_$id").apply()

        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "حذف دور مخصص",
            details = "حذف الدور [${r.nameAr}] وتم تحويل عدد ($reassignedCount) من المستخدمين التابعين له للدور الاحتياطي 'مراقب مالى'",
            status = "SUCCESS"
        )
    }

    // ------------------------------------------
    // 3. SERVICES: DETAILED PERMISSIONS (USR-24 to USR-29)
    // ------------------------------------------

    fun getPermissionsForRole(roleId: String): Set<String> {
        if (roleId == "owner") {
            // Owner is Superuser and runs all modules
            val all = mutableSetOf<String>()
            for (m in AppModule.values()) {
                for (a in UserAction.values()) {
                    all.add("${m.moduleKey}:${a.actionKey}")
                }
            }
            return all
        }
        val raw = prefs.getString("role_perms_$roleId", null) ?: return emptySet()
        val set = mutableSetOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManagementService", "Error loading role permissions", e)
        }
        return set
    }

    fun saveRolePermissions(roleId: String, permissions: Set<String>) {
        if (roleId == "owner") throw BusinessRuleError("لا يمكن تعديل جدول صلاحيات المالك الافتراضي المطلق")
        val arr = JSONArray()
        for (p in permissions) {
            arr.put(p)
        }
        prefs.edit().putString("role_perms_$roleId", arr.toString()).apply()
        logUserActivity(
            username = "أحمد القحطاني",
            eventTypeAr = "تعديل جدول الصلاحيات",
            details = "تعديل القيود والتعيينات الخاصة بالدور [${getRoleNameById(roleId)}] بمجموع (${permissions.size}) صلاحيات معتمدة",
            status = "SUCCESS"
        )
    }

    // ------------------------------------------
    // 4. SERVICES: CURRENT PROFILE & PASSWORD & BIOMETRICS (USR-30 to USR-39)
    // ------------------------------------------

    fun getCurrentUserProfile(): AuthUser {
        val name = prefs.getString("current_user_name", "أحمد القحطاني")!!
        val email = prefs.getString("current_user_email", "ahmed@accounting-system.sa")!!
        val phone = prefs.getString("current_user_phone", "+966551234567")!!
        return AuthUser(id = "current_user", name = name, email = email, role = "owner", phone = phone)
    }

    fun updateCurrentUserProfile(name: String, phone: String) {
        if (name.isBlank()) throw BusinessRuleError("الاسم لا يمكن أن يكون فارغاً")
        prefs.edit()
            .putString("current_user_name", name)
            .putString("current_user_phone", phone)
            .apply()
        logUserActivity(
            username = name,
            eventTypeAr = "تحديث الملف الشخصي",
            details = "قام المستخدم بتحديث معلومات ملفه التعريفي والهاتف بنجاح",
            status = "SUCCESS"
        )
    }

    fun changePassword(oldPass: String, newPass: String) {
        val currentPass = prefs.getString("current_user_password", "Admin@1234")!!
        if (oldPass != currentPass) {
            throw BusinessRuleError("كلمة المرور الحالية المدخلة غير صحيحة")
        }

        // Validate strength: 8+ characters, must contain uppercase and digit
        if (newPass.length < 8) {
            throw BusinessRuleError("يجب أن تكون كلمة المرور الجديدة مكونة من 8 خانات على الأقل")
        }
        if (!newPass.any { it.isUpperCase() }) {
            throw BusinessRuleError("يجب أن تحتوي كلمة المرور على حرف كبير واحد على الأقل")
        }
        if (!newPass.any { it.isDigit() }) {
            throw BusinessRuleError("يجب أن تحتوي مصفوفة كلمة المرور على رقم حسابي واحد")
        }

        prefs.edit().putString("current_user_password", newPass).apply()
        logUserActivity(
            username = prefs.getString("current_user_name", "أحمد القحطاني")!!,
            eventTypeAr = "تغيير كلمة المرور",
            details = "قام المحاسب بتعديل وتحديث كلمة المرور السرية للنظام",
            status = "SUCCESS"
        )
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        logUserActivity(
            username = prefs.getString("current_user_name", "أحمد القحطاني")!!,
            eventTypeAr = if (enabled) "تفعيل البصمة البيومترية" else "تعطيل البصمة البيومترية",
            details = "تغيير تفضيلات المصادقة بالبصمة الحية للدخول السريع",
            status = "SUCCESS"
        )
    }

    fun getActiveSessions(): List<ActiveSession> {
        val raw = prefs.getString("active_sessions_json", null) ?: return emptyList()
        val list = mutableListOf<ActiveSession>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ActiveSession(
                        id = obj.getString("id"),
                        deviceType = obj.getString("deviceType"),
                        lastActive = obj.getLong("lastActive"),
                        location = obj.getString("location")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManagementService", "Error loading active sessions", e)
        }
        return list
    }

    fun saveActiveSessions(sessions: List<ActiveSession>) {
        val arr = JSONArray()
        for (s in sessions) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("deviceType", s.deviceType)
                put("lastActive", s.lastActive)
                put("location", s.location)
            }
            arr.put(obj)
        }
        prefs.edit().putString("active_sessions_json", arr.toString()).apply()
    }

    fun terminateSession(sessionId: String) {
        val sessions = getActiveSessions().toMutableList()
        val found = sessions.removeAll { it.id == sessionId }
        if (!found) throw BusinessRuleError("الجلسة المطلوبة لم يعد لها طابع مسبق أو منتهية بالفعل")
        saveActiveSessions(sessions)
        logUserActivity(
            username = prefs.getString("current_user_name", "أحمد القحطاني")!!,
            eventTypeAr = "إنهاء جلسة نشطة",
            details = "تم تسجيل خروج طارئ أو إغلاق قسري للجلسة المحددة [$sessionId]",
            status = "SUCCESS"
        )
    }

    // ------------------------------------------
    // 5. SERVICES: ACTIVITY MONITORING & TIMELINE (USR-45 to USR-48)
    // ------------------------------------------

    fun getActivitiesLog(): List<UserActivity> {
        val raw = prefs.getString("activities_json", null) ?: return emptyList()
        val list = mutableListOf<UserActivity>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UserActivity(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        username = obj.getString("username"),
                        eventTypeAr = obj.getString("eventTypeAr"),
                        details = obj.getString("details"),
                        status = obj.getString("status")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManagementService", "Error reading authorization activities", e)
        }
        return list
    }

    fun saveActivitiesList(activities: List<UserActivity>) {
        val capped = if (activities.size > 100) activities.take(100) else activities
        val arr = JSONArray()
        for (a in capped) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("timestamp", a.timestamp)
                put("username", a.username)
                put("eventTypeAr", a.eventTypeAr)
                put("details", a.details)
                put("status", a.status)
            }
            arr.put(obj)
        }
        prefs.edit().putString("activities_json", arr.toString()).apply()
    }

    fun logUserActivity(username: String, eventTypeAr: String, details: String, status: String) {
        val acts = getActivitiesLog().toMutableList()
        acts.add(0, UserActivity(username = username, eventTypeAr = eventTypeAr, details = details, status = status))
        saveActivitiesList(acts)
    }

    fun clearActivitiesLog() {
        prefs.edit().remove("activities_json").apply()
        logUserActivity("أحمد القحطاني", "مسح سجلات النشاط المعزز", "مسح كامل لسجلات الأنشطة بنجاح", "SUCCESS")
    }
}
