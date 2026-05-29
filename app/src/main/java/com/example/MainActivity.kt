package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.ui.AccountingViewModel
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // Setup Global Uncaught Exception Handler
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
      android.util.Log.e("FATAL_CRASH", "UNCAUGHT APPLICATION EXCEPTION OCCURRED!", throwable)
      try {
        val prefs = getSharedPreferences("app_crashes", MODE_PRIVATE)
        val stackTrace = android.util.Log.getStackTraceString(throwable)
        prefs.edit().putString("last_crash_report", stackTrace).commit()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
          launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
          startActivity(launchIntent)
        }
      } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Failed in global exception handler", e)
      } finally {
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(10)
      }
    }

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    var initialError: Throwable? = null

    // Initialize Core ServiceLocator
    try {
      com.example.core.di.ServiceContainer.initialize(this)
    } catch (t: Throwable) {
      android.util.Log.e("MainActivity", "Failed to initialize ServiceContainer", t)
      initialError = t
    }

    // Load crash report from shared preferences if any, or intent extra
    val sharedPrefs = getSharedPreferences("app_crashes", MODE_PRIVATE)
    val savedCrashReport = sharedPrefs.getString("last_crash_report", null)

    if (initialError == null) {
      if (savedCrashReport != null) {
        initialError = RuntimeException(savedCrashReport)
        // Clear it so it doesn't show up again on standard launch
        sharedPrefs.edit().remove("last_crash_report").apply()
      } else {
        val crashReportExtra = intent.getStringExtra("crash_report")
        if (crashReportExtra != null) {
          initialError = RuntimeException(crashReportExtra)
        }
      }
    }

    var appViewModel: AccountingViewModel? = null

    if (initialError == null) {
      try {
        appViewModel = androidx.lifecycle.ViewModelProvider(this).get(AccountingViewModel::class.java)
      } catch (t: Throwable) {
        android.util.Log.e("MainActivity", "Failed to initialize AccountingViewModel inside onCreate", t)
        initialError = t
      }
    }

    setContent {
      MyApplicationTheme {
        var crashError by remember { mutableStateOf<Throwable?>(initialError) }
        val context = LocalContext.current

        if (crashError != null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFF0F172A))
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "تحذير خطأ",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(64.dp)
              )

              Text(
                text = "عذراً، حدث خطأ غير متوقع",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
              )

              Text(
                text = "لتفادي تعطل التطبيق، يمكنك تصفير قاعدة البيانات وإعادة بنائها بأمان لتخطي المشكلة.",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
              )

              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    text = "تفاصيل المشكلة الفنية:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                  )
                  Text(
                    text = crashError?.stackTraceToString() ?: "خطأ غامض",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }

              Button(
                onClick = {
                  try {
                    AppDatabase.resetDatabase(context)
                    // Reboot current package
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    if (intent != null) {
                      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                      context.startActivity(intent)
                      finish()
                    }
                  } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to reset and reboot app", e)
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("إعادة تهيئة قاعدة البيانات والتطبيق", fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = {
                  val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                  if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    finish()
                  }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("محاولة التشغيل مجدداً")
              }
            }
          }
        } else {
          appViewModel?.let { viewModel ->
            DashboardScreen(
              viewModel = viewModel,
              modifier = Modifier.fillMaxSize()
            )
          } ?: run {
            // Fallback screen if viewModel is somehow null and initialError wasn't set (unlikely)
            Box(
              modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = Color(0xFF0D9488))
            }
          }
        }
      }
    }
  }
}
