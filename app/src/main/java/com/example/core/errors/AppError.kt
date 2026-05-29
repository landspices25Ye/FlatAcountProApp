package com.example.core.errors

import android.content.Context
import android.widget.Toast

sealed class AppError(
    val errorCode: String,
    val userMessage: String,
    val devMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause)

class ValidationError(
    userMessage: String,
    devMessage: String = "Validation rule failed"
) : AppError("ERR_VAL", userMessage, devMessage)

class AuthenticationError(
    userMessage: String = "يجب تسجيل الدخول أولاً",
    devMessage: String = "Unauthorized or token expired"
) : AppError("ERR_AUTH", userMessage, devMessage)

class AuthorizationError(
    userMessage: String = "ليس لديك الصلاحيات الكافية لإتمام هذا الإجراء",
    devMessage: String = "Required permission was denied"
) : AppError("ERR_FORBIDDEN", userMessage, devMessage)

class NotFoundError(
    userMessage: String,
    devMessage: String = "Entity not found"
) : AppError("ERR_NOT_FOUND", userMessage, devMessage)

class BusinessRuleError(
    userMessage: String,
    devMessage: String = "Business rule violated"
) : AppError("ERR_BIZ", userMessage, devMessage)

class OfflineError(
    userMessage: String = "يبدو أنك غير متصل بالإنترنت حالياً",
    devMessage: String = "Network connection is unavailable"
) : AppError("ERR_OFFLINE", userMessage, devMessage)

class SyncError(
    userMessage: String = "حدث خطأ أثناء مزامنة البيانات السحابية",
    devMessage: String = "Synchronization conflict or network failure"
) : AppError("ERR_SYNC", userMessage, devMessage)

object ErrorHandler {
    fun handleError(context: Context, error: Throwable, onHandled: () -> Unit = {}) {
        val message = when (error) {
            is AppError -> error.userMessage
            else -> "حدث خطأ غير متوقع: ${error.localizedMessage ?: "الرجاء المحاولة لاحقاً"}"
        }
        android.util.Log.e("ErrorHandler", "Error handled: Code=${if (error is AppError) error.errorCode else "UNKNOWN"}, Dev=${if (error is AppError) error.devMessage else error.message}", error)
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        onHandled()
    }
}
