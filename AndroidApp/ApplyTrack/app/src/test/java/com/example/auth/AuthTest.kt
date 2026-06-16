package com.example.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.lang.NullPointerException
import java.lang.IllegalArgumentException

class AuthTest {

    // Helper mock user class to simulate FirebaseUser in unit tests
    data class MockFirebaseUser(
        val uid: String,
        val isAnonymous: Boolean
    )

    // Replicates the state calculation matrix from AuthManager
    private fun calculateAuthState(user: MockFirebaseUser?, isOfflineGuestEnabled: Boolean): AuthState {
        return when {
            user != null -> {
                if (user.isAnonymous) AuthState.GUEST else AuthState.AUTHENTICATED
            }
            isOfflineGuestEnabled -> AuthState.GUEST
            else -> AuthState.UNAUTHENTICATED
        }
    }

    @Test
    fun testCalculateInitialState_NoUser_NoOfflineGuest() {
        val state = calculateAuthState(user = null, isOfflineGuestEnabled = false)
        assertEquals(AuthState.UNAUTHENTICATED, state)
    }

    @Test
    fun testCalculateInitialState_NoUser_OfflineGuestEnabled() {
        val state = calculateAuthState(user = null, isOfflineGuestEnabled = true)
        assertEquals(AuthState.GUEST, state)
    }

    @Test
    fun testCalculateInitialState_AnonymousUser_NoOfflineGuest() {
        val user = MockFirebaseUser(uid = "guest_123", isAnonymous = true)
        val state = calculateAuthState(user = user, isOfflineGuestEnabled = false)
        assertEquals(AuthState.GUEST, state)
    }

    @Test
    fun testCalculateInitialState_AnonymousUser_OfflineGuestEnabled() {
        val user = MockFirebaseUser(uid = "guest_123", isAnonymous = true)
        val state = calculateAuthState(user = user, isOfflineGuestEnabled = true)
        assertEquals(AuthState.GUEST, state)
    }

    @Test
    fun testCalculateInitialState_AuthenticatedUser_NoOfflineGuest() {
        val user = MockFirebaseUser(uid = "user_456", isAnonymous = false)
        val state = calculateAuthState(user = user, isOfflineGuestEnabled = false)
        assertEquals(AuthState.AUTHENTICATED, state)
    }

    @Test
    fun testCalculateInitialState_AuthenticatedUser_OfflineGuestEnabled() {
        val user = MockFirebaseUser(uid = "user_456", isAnonymous = false)
        val state = calculateAuthState(user = user, isOfflineGuestEnabled = true)
        assertEquals(AuthState.AUTHENTICATED, state)
    }

    // --- Expanded Exception / Translation Tests ---

    @Test
    fun testAuthErrorTranslator_Cancellations() {
        val exceptions = listOf(
            kotlinx.coroutines.CancellationException("Job was cancelled"),
            Exception("Sign-in cancelled by user"),
            Exception("CredentialManager error code 20002"),
            Exception("CredentialManager error code 20002: Cancelled"),
            Exception("composition error occurred"),
            Exception("Composition cancelled"),
            Exception("Composition failed because user hit back button"),
            Exception("USER_CANCELLED"),
            Exception("cancel")
        )

        for (e in exceptions) {
            assertNull("Exception with message '${e.message}' should be ignored and translated to null.", e.toUserFriendlyMessage())
        }
    }

    @Test
    fun testAuthErrorTranslator_NetworkErrors() {
        val networkExceptions = listOf(
            IOException("Network connection timed out"),
            IOException("Network is unreachable"),
            Exception("Unable to resolve host: play.googleapis.com"),
            Exception("Failed to connect to /127.0.0.1:443"),
            Exception("SocketTimeoutException: Connection timed out"),
            Exception("timeout on network socket read"),
            Exception("network_error occurred while signing in")
        )

        for (e in networkExceptions) {
            assertEquals(
                "Network connection error. Please check your internet and try again.",
                e.toUserFriendlyMessage()
            )
        }
    }

    @Test
    fun testAuthErrorTranslator_GenericFailures() {
        val genericExceptions = listOf(
            Exception("Something went wrong with developer options"),
            NullPointerException("Google ID option default_web_client_id is null"),
            IllegalArgumentException("Invalid client package config ID"),
            Exception("Developer error: google-services.json missing"),
            RuntimeException("Unknown runtime failure inside authentication flow"),
            Exception("")
        )

        for (e in genericExceptions) {
            assertEquals(
                "Authentication failed. Please try again.",
                e.toUserFriendlyMessage()
            )
        }
    }
}
