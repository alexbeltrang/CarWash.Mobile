package com.carwash.mobile.auth

import android.content.Context

interface SessionStore {
    fun load(): Session?
    fun save(session: Session?)
}

object NoSessionStore : SessionStore {
    override fun load(): Session? = null
    override fun save(session: Session?) = Unit
}

// Guarda solo los datos de la sesión (nunca usuario ni contraseña) para no pedir el acceso al volver a la app.
class AndroidSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences("carwash_session", Context.MODE_PRIVATE)

    override fun load(): Session? {
        if (!prefs.contains(USER_ID)) return null
        val baseUrl = prefs.getString(BASE_URL, null) ?: return null
        return Session(
            userId = prefs.getInt(USER_ID, 0),
            displayName = prefs.getString(DISPLAY_NAME, "").orEmpty(),
            isAdmin = prefs.getBoolean(IS_ADMIN, false),
            baseUrl = baseUrl,
        )
    }

    override fun save(session: Session?) {
        prefs.edit().apply {
            clear()
            if (session != null) {
                putInt(USER_ID, session.userId)
                putString(DISPLAY_NAME, session.displayName)
                putBoolean(IS_ADMIN, session.isAdmin)
                putString(BASE_URL, session.baseUrl)
            }
        }.apply()
    }

    private companion object {
        const val USER_ID = "userId"
        const val DISPLAY_NAME = "displayName"
        const val IS_ADMIN = "isAdmin"
        const val BASE_URL = "baseUrl"
    }
}
