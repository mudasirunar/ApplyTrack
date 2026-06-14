package com.example.auth

fun Throwable.toUserFriendlyMessage(): String? {
    val msg = this.localizedMessage ?: this.message ?: ""
    return when {
        this is kotlinx.coroutines.CancellationException -> null
        msg.contains("cancel", ignoreCase = true) -> null
        msg.contains("20002") -> null
        msg.contains("composition", ignoreCase = true) -> null
        msg.contains("network", ignoreCase = true) || 
        msg.contains("timeout", ignoreCase = true) || 
        msg.contains("connection", ignoreCase = true) ||
        msg.contains("Unable to resolve host", ignoreCase = true) -> 
            "Network connection error. Please check your internet and try again."
        else -> "Authentication failed. Please try again."
    }
}
