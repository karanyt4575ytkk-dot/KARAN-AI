package com.example.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ConfirmationRequest(
    val id: String,
    val title: String,
    val action: String,
    val consequence: String,
    val details: String? = null
)

class ConfirmationManager {
    private val _pendingRequest = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingRequest: StateFlow<ConfirmationRequest?> = _pendingRequest.asStateFlow()

    private var activeContinuation: Continuation<Boolean>? = null

    suspend fun requestConfirmation(
        title: String,
        action: String,
        consequence: String,
        details: String? = null
    ): Boolean {
        return suspendCoroutine { cont ->
            activeContinuation = cont
            _pendingRequest.value = ConfirmationRequest(
                id = System.currentTimeMillis().toString(),
                title = title,
                action = action,
                consequence = consequence,
                details = details
            )
        }
    }

    fun resolve(approved: Boolean) {
        val cont = activeContinuation
        activeContinuation = null
        _pendingRequest.value = null
        cont?.resume(approved)
    }
}
