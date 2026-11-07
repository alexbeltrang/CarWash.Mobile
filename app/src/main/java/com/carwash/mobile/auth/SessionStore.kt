package com.carwash.mobile.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionStore {
    fun load(): Session?
    fun save(session: Session?)
}

object NoSessionStore : SessionStore {
    override fun load(): Session? = null
    override fun save(session: Session?) = Unit
}

class AndroidSessionStore(context: Context, preferencesName: String = "carwash_session") : SessionStore {
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun load(): Session? {
        val encoded = prefs.getString(ENCRYPTED_SESSION, null) ?: return null
        return runCatching {
            require(encoded.length <= 24_000)
            val bytes = Base64.getDecoder().decode(encoded)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(KEY_ALIAS.toByteArray(Charsets.UTF_8))
            Gson().fromJson(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8), Session::class.java)
        }.getOrElse { prefs.edit().clear().apply(); null }
    }

    override fun save(session: Session?) {
        val encoded = session?.let {
            runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key())
                cipher.updateAAD(KEY_ALIAS.toByteArray(Charsets.UTF_8))
                Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(Gson().toJson(it).toByteArray(Charsets.UTF_8)))
            }.getOrNull()
        }
        prefs.edit().clear().apply { if (encoded != null) putString(ENCRYPTED_SESSION, encoded) }.commit()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object {
        const val ENCRYPTED_SESSION = "encryptedSession"
        const val KEY_ALIAS = "com.carwash.mobile.session.v1"
    }
}
