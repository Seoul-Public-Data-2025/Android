package com.maumpeace.safeapp.util

import android.content.Context
import androidx.core.content.edit

object TokenManager {
    private const val PREF_NAME = "auth"
    private const val KEY_JWT = "jwt"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putString(KEY_JWT, token) }
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_JWT, null)
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { remove(KEY_JWT) }
    }
}