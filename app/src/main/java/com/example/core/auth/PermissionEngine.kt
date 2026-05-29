package com.example.core.auth

enum class SystemRole(val roleNameAr: String) {
    ADMIN("مدير النظام الكلي"),
    ACCOUNTANT("المحاسب المالي"),
    INVENTORY_MANAGER("أمين المستودع"),
    HR_MANAGER("مسؤول الموارد البشرية")
}

enum class AppModule(val moduleKey: String) {
    ACCOUNTING("accounting"),
    INVENTORY("inventory"),
    PAYROLL("payroll"),
    REPORTS("reports"),
    SETTINGS("settings")
}

enum class UserAction(val actionKey: String) {
    VIEW("view"),
    CREATE("create"),
    EDIT("edit"),
    DELETE("delete"),
    POST_JOURNAL("post_journal"),
    EXPORT("export")
}

object PermissionEngine {

    // Default permission matrices
    private val rolePermissions: Map<SystemRole, Set<String>> = mapOf(
        SystemRole.ADMIN to getAllSystemPermissions(),
        
        SystemRole.ACCOUNTANT to setOf(
            buildPermKey(AppModule.ACCOUNTING, UserAction.VIEW),
            buildPermKey(AppModule.ACCOUNTING, UserAction.CREATE),
            buildPermKey(AppModule.ACCOUNTING, UserAction.EDIT),
            buildPermKey(AppModule.ACCOUNTING, UserAction.POST_JOURNAL),
            buildPermKey(AppModule.REPORTS, UserAction.VIEW),
            buildPermKey(AppModule.REPORTS, UserAction.EXPORT),
            buildPermKey(AppModule.INVENTORY, UserAction.VIEW)
        ),

        SystemRole.INVENTORY_MANAGER to setOf(
            buildPermKey(AppModule.INVENTORY, UserAction.VIEW),
            buildPermKey(AppModule.INVENTORY, UserAction.CREATE),
            buildPermKey(AppModule.INVENTORY, UserAction.EDIT),
            buildPermKey(AppModule.REPORTS, UserAction.VIEW)
        ),

        SystemRole.HR_MANAGER to setOf(
            buildPermKey(AppModule.PAYROLL, UserAction.VIEW),
            buildPermKey(AppModule.PAYROLL, UserAction.CREATE),
            buildPermKey(AppModule.PAYROLL, UserAction.EDIT),
            buildPermKey(AppModule.PAYROLL, UserAction.EXPORT)
        )
    )

    private fun buildPermKey(module: AppModule, action: UserAction): String {
        return "${module.moduleKey}:${action.actionKey}"
    }

    private fun getAllSystemPermissions(): Set<String> {
        val all = mutableSetOf<String>()
        for (m in AppModule.values()) {
            for (a in UserAction.values()) {
                all.add("${m.moduleKey}:${a.actionKey}")
            }
        }
        return all
    }

    /**
     * Engine policy checker
     */
    fun canUserPerformAction(role: SystemRole, module: AppModule, action: UserAction): Boolean {
        // Super admin rule
        if (role == SystemRole.ADMIN) return true
        
        val permissions = rolePermissions[role] ?: return false
        val key = buildPermKey(module, action)
        return permissions.contains(key)
    }
}
