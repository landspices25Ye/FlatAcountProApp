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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.*
import com.example.core.errors.BusinessRuleError
import com.example.core.utils.FormatUtils

// ========================================================
// 1. MY PROFILE & ACCOUNT SECURITY VIEW
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileAndSecurityView(
    service: UserManagementService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(service.getCurrentUserProfile()) }
    var name by remember { mutableStateOf(profile.name) }
    var phone by remember { mutableStateOf(profile.phone) }

    // Password fields
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Biometrics state
    var biometricEnabled by remember { mutableStateOf(service.isBiometricEnabled()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Welcome Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "مرحباً، ${profile.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "دورك الحالي: مالك المؤسسة الرئيسي (Owner)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card 1: Edit profile
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تعديل الملف التعريفي والاتصال",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = profile.email,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("البريد الإلكتروني (غير قابل للتعديل)") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف الجوال") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        try {
                            service.updateCurrentUserProfile(name, phone)
                            profile = service.getCurrentUserProfile()
                            Toast.makeText(context, "تم حفظ تغييرات الملف التعريفي بنجاح", Toast.LENGTH_SHORT).show()
                        } catch (e: BusinessRuleError) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ البيـانات")
                }
            }
        }

        // Card 2: Password
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تغيير كلمة المرور الشخصية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "يجب أن تحتوي كلمة المرور على 8 أحرف على الأقل، بما في ذلك حرف كبير ورقم واحد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("كلمة المرور الحالية") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("تأكيد كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "خطأ: تطابق كلمتي المرور الجديدة غير متماثل", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        try {
                            service.changePassword(oldPassword, newPassword)
                            oldPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            Toast.makeText(context, "تم تغيير كلمة المرور للمحاسب بنجاح", Toast.LENGTH_SHORT).show()
                        } catch (e: BusinessRuleError) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تطبيق كلمة السر الجديدة")
                }
            }
        }

        // Card 3: Biometrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تسجيل الدخول بالبصمة البيومترية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "استخدام التعرف على الوجه أو بصمة الإصبع الخاصة بنظام Android لتسجيل دخول سريع وآمن بالتطبيق بدون كلمة مرور.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = {
                        service.setBiometricEnabled(it)
                        biometricEnabled = it
                        Toast.makeText(context, if (it) "تم تفعيل المصادقة البيومترية" else "تم تعطيل المصادقة البيومترية", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}


// ========================================================
// 2. USERS MANAGEMENT & INVITATIONS VIEW
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersAndInvitationsView(
    service: UserManagementService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var usersList by remember { mutableStateOf(service.getUsersByCompany()) }
    var rolesList by remember { mutableStateOf(service.getCustomRoles()) }

    // Dialogue State for inviting
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    var invitePhone by remember { mutableStateOf("") }
    var inviteSelectedRole by remember { mutableStateOf("accountant") }

    // State for viewing/updating user details
    var selectedUser by remember { mutableStateOf<AuthUser?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var userEditRole by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper Title controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "إدارة المستخدمين النشطين وبطاقات الدعوات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إضافة زميل للعمل، تعديل مسميات الأدوار، تعطيل الدخول أو إدارة خطابات الدعوة المعلقة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showInviteDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("دعوة مستخدم")
            }
        }

        // Lazy Users list view
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(usersList) { u ->
                Card(
                    onClick = {
                        selectedUser = u
                        userEditRole = u.role
                        showDetailDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (u.isInvitationPending) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        else if (!u.isActive) Color.Red.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (u.name.length >= 2) u.name.substring(0,2) else u.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (u.isInvitationPending) MaterialTheme.colorScheme.secondary
                                    else if (!u.isActive) Color.Red
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = u.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = u.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "الوظيفة: ${service.getRoleNameById(u.role)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Badge / Status
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (u.isInvitationPending) MaterialTheme.colorScheme.secondaryContainer
                            else if (!u.isActive) Color.Red.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = if (u.isInvitationPending) "دعوة معلقة"
                                else if (!u.isActive) "غير نشط"
                                else "نشط",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (u.isInvitationPending) MaterialTheme.colorScheme.onSecondaryContainer
                                else if (!u.isActive) Color.Red
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Invite User Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("دعوة مستخدم جديد للفريق") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("البريد الإلكتروني للزميل") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = invitePhone,
                        onValueChange = { invitePhone = it },
                        label = { Text("رقم الجوال (اختياري)") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Text(
                        text = "اختر دور الصلاحيات:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Role Selection Column list
                    rolesList.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inviteSelectedRole = r.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = inviteSelectedRole == r.id,
                                onClick = { inviteSelectedRole = r.id }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(text = r.nameAr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = r.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!FormatUtils.isValidEmail(inviteEmail)) {
                            Toast.makeText(context, "البريد الإلكتروني المدخل غير صالح", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        try {
                            service.inviteUser(inviteEmail, inviteSelectedRole, invitePhone)
                            usersList = service.getUsersByCompany()
                            showInviteDialog = false
                            inviteEmail = ""
                            invitePhone = ""
                            Toast.makeText(context, "تم إرسال دعوة العمل بنجاح وتحرير السجلات", Toast.LENGTH_SHORT).show()
                        } catch (e: BusinessRuleError) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("إرسال الدعوة المعتمدة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Detail Dialog Sheet for specific user Actions
    if (showDetailDialog && selectedUser != null) {
        val user = selectedUser!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text("إجراءات وتفاصيل المستخدم") },
            text = {
                Column {
                    Text(text = "الاسم الكامل: ${user.name}", fontWeight = FontWeight.Bold)
                    Text(text = "البريد الإلكتروني: ${user.email}")
                    Text(
                        text = "رقم الجوال: ${if (user.phone.isBlank()) "غير محدد" else user.phone}",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "تغيير وتعيين مسمى الدور الصلاحيات:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Custom dropdown styled Radio buttons for roles
                    rolesList.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { userEditRole = r.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = userEditRole == r.id,
                                onClick = { userEditRole = r.id }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = r.nameAr, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (user.isInvitationPending) {
                        Text(
                            text = "إجراءات الدعوة:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        service.resendInvitation(user.email)
                                        Toast.makeText(context, "تمت إعادة إرسال رابط بريد الدعوة", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("إعادة إرسال")
                            }

                            Button(
                                onClick = {
                                    try {
                                        service.revokeInvitation(user.email)
                                        usersList = service.getUsersByCompany()
                                        showDetailDialog = false
                                        Toast.makeText(context, "تم سحب دعوتك بنجاح للأمن", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                            ) {
                                Text("إلغاء وسحب")
                            }
                        }
                    } else {
                        // User Status Toggle
                        val buttonText = if (user.isActive) "تعطيل حساب الموظف مؤقتاً" else "تنشيط وتفعيل حساب الموظف"
                        Button(
                            onClick = {
                                try {
                                    service.toggleUserStatus(user.id)
                                    usersList = service.getUsersByCompany()
                                    showDetailDialog = false
                                    Toast.makeText(context, "تم تحديث حالة المستخدم بنجاح للفريق", Toast.LENGTH_SHORT).show()
                                } catch (e: BusinessRuleError) {
                                    Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.isActive) Color.Red.copy(alpha = 0.75f) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (user.isActive) Icons.Default.Close else Icons.Default.CheckCircle, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(buttonText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            if (user.role != userEditRole) {
                                service.updateUserRole(user.id, userEditRole)
                                usersList = service.getUsersByCompany()
                            }
                            showDetailDialog = false
                            Toast.makeText(context, "تم حفظ تعيينات الدور الوظيفي للمستخدم", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = false }) { Text("إغلاق") }
            }
        )
    }
}


// ========================================================
// 3. ROLES & FINE PERMISSIONS MATRIX VIEW
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolesAndPermissionsView(
    service: UserManagementService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rolesList by remember { mutableStateOf(service.getCustomRoles()) }
    var selectedRoleId by remember { mutableStateOf("accountant") }
    var selectedRolePerms by remember { mutableStateOf(service.getPermissionsForRole(selectedRoleId)) }

    // Dialog for custom role creation
    var showCreateRoleDialog by remember { mutableStateOf(false) }
    var newRoleName by remember { mutableStateOf("") }
    var newRoleDesc by remember { mutableStateOf("") }

    // State for managing local changes
    var unsavedChanges by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // LHS: Roles Selector Column
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مسميات الأدوار",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showCreateRoleDialog = true }) {
                    Icon(Icons.Default.Add, "إضافة مسمى دور جديد")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rolesList) { r ->
                    val isSelected = selectedRoleId == r.id
                    Card(
                        onClick = {
                            selectedRoleId = r.id
                            selectedRolePerms = service.getPermissionsForRole(r.id)
                            unsavedChanges = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = r.nameAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                                )
                                if (r.isPreset) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "نظامي",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            try {
                                                service.deleteRole(r.id)
                                                rolesList = service.getCustomRoles()
                                                selectedRoleId = "viewer"
                                                selectedRolePerms = service.getPermissionsForRole("viewer")
                                                Toast.makeText(context, "تم حذف الدور مخصص بنجاح", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Text(
                                text = r.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // RHS: Permissions Checker matrix table
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val currentRoleText = rolesList.find { it.id == selectedRoleId }?.nameAr ?: selectedRoleId
                    Text(
                        text = "صلاحيات الدور: $currentRoleText",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تعديل جدول وتراخيص الدخول الدقيقة للوحدات والعمليات التفصيلية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedRoleId != "owner") {
                    Button(
                        onClick = {
                            try {
                                service.saveRolePermissions(selectedRoleId, selectedRolePerms)
                                unsavedChanges = false
                                Toast.makeText(context, "تم حفظ مصفوفة الصلاحيات المطبقة", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = unsavedChanges,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ الصلاحيات")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedRoleId == "owner") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "حساب المالك لديه صلاحيات كاملة ومطلقة (Super API Access) بطبيعته على جميع النظم والمقاصة ولا يمكن تحرير مصفوفتها للتأمين الكلي.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Table of grid
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(AppModule.values()) { module ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = getModuleTitleAr(module),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Grid actions list
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    UserAction.values().forEach { action ->
                                        val permKey = "${module.moduleKey}:${action.actionKey}"
                                        val isChecked = selectedRolePerms.contains(permKey)

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    val updated = selectedRolePerms.toMutableSet()
                                                    if (isChecked) {
                                                        // Unchecking view also removes everything in this module
                                                        if (action == UserAction.VIEW) {
                                                            UserAction.values().forEach { a ->
                                                                updated.remove("${module.moduleKey}:${a.actionKey}")
                                                            }
                                                        } else {
                                                            updated.remove(permKey)
                                                        }
                                                    } else {
                                                        // Checking anything automatically requires and enables 'view'
                                                        updated.add(permKey)
                                                        updated.add("${module.moduleKey}:${UserAction.VIEW.actionKey}")
                                                    }
                                                    selectedRolePerms = updated
                                                    unsavedChanges = true
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { _ ->
                                                    // Managed click action
                                                    val updated = selectedRolePerms.toMutableSet()
                                                    if (isChecked) {
                                                        if (action == UserAction.VIEW) {
                                                            UserAction.values().forEach { a ->
                                                                updated.remove("${module.moduleKey}:${a.actionKey}")
                                                            }
                                                        } else {
                                                            updated.remove(permKey)
                                                        }
                                                    } else {
                                                        updated.add(permKey)
                                                        updated.add("${module.moduleKey}:${UserAction.VIEW.actionKey}")
                                                    }
                                                    selectedRolePerms = updated
                                                    unsavedChanges = true
                                                }
                                            )
                                            Text(
                                                text = getActionTitleAr(action),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp
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
    }

    // Role Creator Dialog
    if (showCreateRoleDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoleDialog = false },
            title = { Text("إنشاء دور مخصص للشركة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newRoleName,
                        onValueChange = { newRoleName = it },
                        label = { Text("اسم المسمى الوظيفي / الدور") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = newRoleDesc,
                        onValueChange = { newRoleDesc = it },
                        maxLines = 3,
                        label = { Text("وصف مختصر للصلاحيات للتوجيه") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newRoleName.isBlank()) {
                            Toast.makeText(context, "الاسم الكامل للدور لا يمكن أن يترك خاوياً", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        try {
                            service.createRole(newRoleName, newRoleDesc)
                            rolesList = service.getCustomRoles()
                            showCreateRoleDialog = false
                            newRoleName = ""
                            newRoleDesc = ""
                            Toast.makeText(context, "تمت إضافة الدور بنجاح وإسكان صلاحياته المبدئية", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("تثبيت الدور")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoleDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// Arabic localization helpers for permissions Matrix
private fun getModuleTitleAr(module: AppModule): String {
    return when (module) {
        AppModule.ACCOUNTING -> "القيود والخدمات المحاسبية"
        AppModule.INVENTORY -> "إدارة المنتجات والمخازن"
        AppModule.PAYROLL -> "الموارد البشرية والرواتب"
        AppModule.REPORTS -> "التقارير والمراجعات المالية"
        AppModule.SETTINGS -> "الإعدادات وإدارة وتفضيلات النظام"
    }
}

private fun getActionTitleAr(action: UserAction): String {
    return when (action) {
        UserAction.VIEW -> "استعراض"
        UserAction.CREATE -> "إضافة"
        UserAction.EDIT -> "تعديل"
        UserAction.DELETE -> "حذف"
        UserAction.POST_JOURNAL -> "ترحيل"
        UserAction.EXPORT -> "تصدير"
    }
}


// ========================================================
// 4. ACTIVE DEVICES & SESSIONS VIEW
// ========================================================
@Composable
fun ActiveSessionsView(
    service: UserManagementService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(service.getActiveSessions()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "الأجهزة المتصلة والجلسات النشطة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "استعراض الأجهزة والجلسات المسجلة الدخول والمصرحة بإمكانية الوصول لـ ERP حالياً وإنهاء أي جلسة بشكل قسري للأمن العاجل.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "لا توجد جلسات نشطة مسجلة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (s.deviceType.contains("Android", ignoreCase = true)) Icons.Default.Phone else Icons.Default.Home,
                                        contentDescription = "Device Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = s.deviceType,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الموقع التقريبي للمودم: ${s.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "آخر تاريخ نشاط: ${FormatUtils.formatDate(s.lastActive, "yyyy-MM-dd HH:mm:ss")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Terminate Session btn
                            Button(
                                onClick = {
                                    try {
                                        service.terminateSession(s.id)
                                        sessions = service.getActiveSessions()
                                        Toast.makeText(context, "تم قطع الاتصال بالفرع وإنهاء صلاحية الجلسة للأمن", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.82f))
                            ) {
                                Text("إنهاء فوري")
                            }
                        }
                    }
                }
            }
        }
    }
}


// ========================================================
// 5. SECURITY ACTIONS & TIMELINE AUDIT FEED
// ========================================================
@Composable
fun UserActivitiesAuditView(
    service: UserManagementService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var originalActivities = remember { service.getActivitiesLog() }
    var activities by remember { mutableStateOf(originalActivities) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "سجل العمليات والتدقيق الأمني لفريق العمل",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تتبع فوري مع طابع زمني لعمليات الدخول، إعدادات الحسابات، وصلاحيات الأدوار والوثائق المالية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    service.clearActivitiesLog()
                    originalActivities = service.getActivitiesLog()
                    activities = originalActivities
                    Toast.makeText(context, "تم تصفير وإعادة تعيين سجل التدقيق بنجاح", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصفير السجل")
            }
        }

        // Search text filter bar (RTL friendly)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                activities = if (it.isBlank()) {
                    originalActivities
                } else {
                    originalActivities.filter { a ->
                        a.username.contains(it, ignoreCase = true) ||
                                a.eventTypeAr.contains(it, ignoreCase = true) ||
                                a.details.contains(it, ignoreCase = true)
                    }
                }
            },
            placeholder = { Text("بحث باسم المستخدم أو نوع العملية...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "لا توجد سجلات تقع تحت مخرجات البحث للتدقيق", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activities) { act ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (act.status == "SUCCESS") Color.Green.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (act.status == "SUCCESS") "مكتملة" else "فشلت",
                                            color = if (act.status == "SUCCESS") Color(0xFF2E7D32) else Color.Red,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = act.eventTypeAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = act.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "بواسطة: ${act.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = FormatUtils.formatDate(act.timestamp, "yyyy-MM-dd\nHH:mm:ss"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
