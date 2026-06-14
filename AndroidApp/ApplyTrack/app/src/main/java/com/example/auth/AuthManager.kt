package com.example.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.local.AppDatabase
import com.example.utils.PreferencesHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class AuthManager(
    private val applicationContext: Context,
    private val preferencesHelper: PreferencesHelper
) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(applicationContext)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onUserUpgradeCallback: (suspend () -> Unit)? = null

    private val _authState = MutableStateFlow(calculateInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUserFlow: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private fun calculateInitialState(): AuthState {
        val user = auth.currentUser
        val isOfflineGuest = preferencesHelper.isOfflineGuestEnabled()
        return when {
            user != null -> {
                if (user.isAnonymous) AuthState.GUEST else AuthState.AUTHENTICATED
            }
            isOfflineGuest -> AuthState.GUEST
            else -> AuthState.UNAUTHENTICATED
        }
    }

    init {
        scope.launch {
            combine(
                preferencesHelper.isOfflineGuest,
                _currentUser
            ) { isOfflineGuest, user ->
                when {
                    user != null -> {
                        if (user.isAnonymous) AuthState.GUEST else AuthState.AUTHENTICATED
                    }
                    isOfflineGuest -> AuthState.GUEST
                    else -> AuthState.UNAUTHENTICATED
                }
            }.collect { state ->
                _authState.value = state
            }
        }

        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    val currentUser get() = auth.currentUser

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun signInAnonymously(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Instantly toggle offline guest preference locally to trigger immediate navigation
            preferencesHelper.setOfflineGuest(true)
            
            // Execute the Firebase anonymous login on a background thread in application scope
            scope.launch(Dispatchers.IO) {
                try {
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().await()
                    }
                } catch (e: Exception) {
                    // Suppress connection failures for offline-first support; login was saved locally
                    e.printStackTrace()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> {
        if (!isNetworkAvailable(applicationContext)) {
            return Result.failure(IOException("No internet connection. Please check your network and try again."))
        }
        try {
            // 1. Generate Nonce
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // 2. Get Web Client ID from google-services.json
            val webClientIdId = applicationContext.resources.getIdentifier("default_web_client_id", "string", applicationContext.packageName)
            if (webClientIdId == 0) throw Exception("Web Client ID not found. Ensure google-services.json is configured.")
            val webClientId = applicationContext.getString(webClientIdId)
            
            // 3. Build Google ID Option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 4. Request Credential (Run on Main thread directly to avoid context-switching lag when launching system UI)
            val result = credentialManager.getCredential(context = activityContext, request = request)
            val credential = result.credential

            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw Exception("Unexpected credential type")
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            val currentUser = auth.currentUser

            // Check if we were already a guest
            val isOldGuest = (currentUser != null && currentUser.isAnonymous) || preferencesHelper.isOfflineGuestEnabled()

            if (isOldGuest) {
                if (currentUser != null && currentUser.isAnonymous) {
                    try {
                        currentUser.linkWithCredential(authCredential).await()
                    } catch (e: Exception) {
                        auth.signInWithCredential(authCredential).await()
                        try { currentUser.delete().await() } catch (ignored: Exception) {}
                    }
                } else {
                    auth.signInWithCredential(authCredential).await()
                }
                preferencesHelper.setOfflineGuest(false)
                onUserUpgradeCallback?.invoke()
            } else {
                val oldUid = currentUser?.uid
                auth.signInWithCredential(authCredential).await()
                val newUid = auth.currentUser?.uid
                if (newUid != null && newUid != oldUid) {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        db.jobApplicationDao().deleteAllApplications()
                        db.jobApplicationDao().clearDeletedJobs()
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clear Room Database completely to prevent the next user from seeing this user's data
            val db = AppDatabase.getDatabase(applicationContext)
            db.jobApplicationDao().deleteAllApplications()
            db.jobApplicationDao().clearDeletedJobs()

            // Clear Preferences (Theme, guest state, etc)
            preferencesHelper.clearAll()

            // Clear Google Credentials
            credentialManager.clearCredentialState(ClearCredentialStateRequest())

            // Sign out of Firebase
            auth.signOut()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
