package com.maumpeace.safeapp.util

import android.content.Context
import androidx.core.content.edit

object TokenManager {
    private const val PREF_NAME = "auth"
    private const val KEY_JWT_ACCESS = "jwt_access"
    private const val KEY_JWT_REFRESH = "refresh_token"

    fun saveAccessToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putString(KEY_JWT_ACCESS, token) }
    }

    fun getAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_JWT_ACCESS, null)
    }

    fun clearAccessToken(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { remove(KEY_JWT_ACCESS) }
    }

    fun saveRefreshToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putString(KEY_JWT_REFRESH, token) }
    }

    fun getRefreshToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_JWT_REFRESH, null)
    }

    fun clearRefreshToken(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { remove(KEY_JWT_REFRESH) }
    }
}