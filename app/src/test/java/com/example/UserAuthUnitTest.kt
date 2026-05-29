package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.auth.UserManagementService
import com.example.core.errors.BusinessRuleError
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserAuthUnitTest {

    private lateinit var context: Context
    private lateinit var authService: UserManagementService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear old prefs to ensure clean state
        context.getSharedPreferences("erp_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        authService = UserManagementService.initialize(context)
    }

    @Test
    fun test_01_seed_defaults() {
        val users = authService.getUsersByCompany()
        assertTrue(users.isNotEmpty())
        assertTrue(users.any { it.email == "ahmed@accounting-system.sa" && it.role == "owner" })
        assertTrue(users.any { it.email == "sara@accounting-system.sa" && it.role == "accountant" })

        val roles = authService.getCustomRoles()
        assertTrue(roles.isNotEmpty())
        assertTrue(roles.any { it.id == "owner" && it.isPreset })
        assertTrue(roles.any { it.id == "trainee" && !it.isPreset })

        val sessions = authService.getActiveSessions()
        assertTrue(sessions.isNotEmpty())
    }

    @Test
    fun test_02_create_and_invite_user() {
        // Create standard active user
        val newUser = authService.createUser(
            name = "عبدالرحمن الحربي",
            email = "abdul@accounting-system.sa",
            role = "accountant",
            phone = "+966551112223"
        )
        assertNotNull(newUser.id)
        assertFalse(newUser.isInvitationPending)
        assertTrue(newUser.isActive)

        // Duplicate email creation should fail
        assertThrows(BusinessRuleError::class.java) {
            authService.createUser("عبدالرحمن مكرر", "abdul@accounting-system.sa", "viewer")
        }

        // Invite pending user via email
        val inviteUser = authService.inviteUser(
            email = "external@accounting-system.sa",
            role = "viewer"
        )
        assertTrue(inviteUser.isInvitationPending)
        assertTrue(inviteUser.invitedAt > 0L)
    }

    @Test
    fun test_03_toggle_user_status_safeguards() {
        val users = authService.getUsersByCompany()
        val ownerUser = users.find { it.role == "owner" }!!
        val accountantUser = users.find { it.role == "accountant" && !it.isInvitationPending }!!

        // Rule check: Cannot deactivate owner
        assertThrows(BusinessRuleError::class.java) {
            authService.toggleUserStatus(ownerUser.id)
        }

        // Rule check: Cannot deactivate self
        // (Current logged-in user in seeded data defaults to ahmed@accounting-system.sa which has ID usr_1 or owner role)
        val selfUser = users.find { it.email == "ahmed@accounting-system.sa" }!!
        assertThrows(BusinessRuleError::class.java) {
            authService.toggleUserStatus(selfUser.id)
        }

        // Toggle standard accountant user successfully
        assertTrue(accountantUser.isActive)
        authService.toggleUserStatus(accountantUser.id)
        
        val updatedUser = authService.getUsersByCompany().find { it.id == accountantUser.id }!!
        assertFalse(updatedUser.isActive)
    }

    @Test
    fun test_04_role_crud_and_deletion_reassignment() {
        // Create custom role
        val newRole = authService.createRole("محاسب أول", "مسمى لصلاحيات الحسابات المتقدمة وإقفال القيود")
        assertNotNull(newRole.id)

        // Attempting to duplicate role name should fail
        assertThrows(BusinessRuleError::class.java) {
            authService.createRole("محاسب أول", "وصف آخر")
        }

        // Assign a user this role
        val user = authService.createUser("سلمان العتيبي", "salman@system.sa", newRole.id)
        assertEquals(newRole.id, user.role)

        // Delete the custom role and check safety rule
        // Salman must be reassigned into Viewer ("viewer") fallback role automatically
        authService.deleteRole(newRole.id)

        val updatedUser = authService.getUsersByCompany().find { it.id == user.id }!!
        assertEquals("viewer", updatedUser.role)
    }

    @Test
    fun test_05_role_preset_deletion_safeguard() {
        // Cannot delete a system preset role like "accountant"
        assertThrows(BusinessRuleError::class.java) {
            authService.deleteRole("accountant")
        }
    }

    @Test
    fun test_06_password_strength_validator() {
        // Short password fails
        assertThrows(BusinessRuleError::class.java) {
            authService.changePassword("Admin@1234", "Sh0rt")
        }

        // No uppercase character fails
        assertThrows(BusinessRuleError::class.java) {
            authService.changePassword("Admin@1234", "lowercase123")
        }

        // No digit fails
        assertThrows(BusinessRuleError::class.java) {
            authService.changePassword("Admin@1234", "UppercaseNoDigit")
        }

        // Strong password succeeds
        authService.changePassword("Admin@1234", "NewStrong@2026")
    }

    @Test
    fun test_07_active_sessions_and_term() {
        val initialSessions = authService.getActiveSessions()
        assertTrue(initialSessions.isNotEmpty())

        val targetSessionId = initialSessions.first().id
        authService.terminateSession(targetSessionId)

        val updatedSessions = authService.getActiveSessions()
        assertFalse(updatedSessions.any { it.id == targetSessionId })
    }
}
