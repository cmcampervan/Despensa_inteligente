package com.example.util

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DriveOAuthScopes {
    // Scope restringido a los archivos que la propia app crea en Drive (no da acceso a
    // todo el Drive del usuario, solo a la copia de seguridad que sube esta aplicación).
    const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
}

sealed class DriveTokenResult {
    data class Success(val accessToken: String) : DriveTokenResult()
    data class RecoverableError(val recoveryIntent: Intent) : DriveTokenResult()
    data class Failure(val message: String) : DriveTokenResult()
}

/**
 * Gestiona el inicio de sesión con Google (selector de cuenta) y la obtención de un token
 * OAuth con permiso sobre Drive para esa cuenta. Sin esto, el respaldo nunca sabe a qué
 * cuenta de correo pertenece ni tiene autorización real para subir nada a Drive.
 */
class GoogleDriveAuthManager(private val context: Context) {

    private val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveOAuthScopes.DRIVE_FILE))
            .build()
    }

    private val signInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    /** Intent para lanzar el selector de cuenta de Google (usar con un ActivityResultLauncher). */
    fun getSignInIntent(): Intent = signInClient.signInIntent

    /** Cuenta de Google con sesión ya iniciada Y con el permiso de Drive ya concedido, si existe. */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val hasDriveScope = GoogleSignIn.hasPermissions(account, Scope(DriveOAuthScopes.DRIVE_FILE))
        return if (hasDriveScope) account else null
    }

    /** Extrae la cuenta elegida del resultado del selector de cuenta. Null si se canceló o falló. */
    fun extractAccountFromResult(data: Intent?): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        } catch (e: ApiException) {
            e.printStackTrace()
            null
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        signInClient.signOut().addOnCompleteListener { onComplete() }
    }

    /**
     * Obtiene un token de acceso OAuth fresco (válido ~1 hora) con permiso de Drive para
     * la cuenta indicada. Debe pedirse justo antes de cada subida/descarga; GoogleAuthUtil
     * gestiona la caché y renovación internamente, así que no hace falta guardarlo nosotros.
     */
    suspend fun fetchAccessToken(account: GoogleSignInAccount): DriveTokenResult = withContext(Dispatchers.IO) {
        val systemAccount: Account = account.account
            ?: return@withContext DriveTokenResult.Failure("La cuenta de Google seleccionada no está disponible en el dispositivo.")
        try {
            val token = GoogleAuthUtil.getToken(context, systemAccount, "oauth2:${DriveOAuthScopes.DRIVE_FILE}")
            DriveTokenResult.Success(token)
        } catch (e: UserRecoverableAuthException) {
            val recoveryIntent = e.intent
            if (recoveryIntent != null) {
                DriveTokenResult.RecoverableError(recoveryIntent)
            } else {
                DriveTokenResult.Failure("Se requiere autorización de Drive, pero no se pudo abrir la pantalla de consentimiento.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DriveTokenResult.Failure(e.localizedMessage ?: "No se pudo obtener el token de acceso a Drive.")
        }
    }
}
