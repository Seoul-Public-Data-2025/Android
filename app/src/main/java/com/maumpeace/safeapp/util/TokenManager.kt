package com.maumpeace.safeapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * ✅ TokenManager
 * - JWT access/refresh 토큰을 SharedPreferences에 안전하게 저장 및 삭제
 */
object TokenManager {

    private const val PREF_NAME = "auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val PROFILE = "profile"
    private const val NICKNAME = "nickname"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 🔐 accessToken 저장
     */
    fun saveAccessToken(context: Context, token: String) {
        getPrefs(context).edit {
            putString(KEY_ACCESS_TOKEN, token)
        }
    }

    /**
     * 🔐 refreshToken 저장
     */
    fun saveRefreshToken(context: Context, token: String) {
        getPrefs(context).edit {
            putString(KEY_REFRESH_TOKEN, token)
        }
    }

    /**
     * 🧾 accessToken 조회
     */
    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * 🧾 refreshToken 조회
     */
    fun getRefreshToken(context: Context): String? {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 🧾 profile 조회
     */
    fun getProfile(context: Context): String? {
        return getPrefs(context).getString(PROFILE, null)
    }

    /**
     * 🧾 nickname 조회
     */
    fun getNickname(context: Context): String? {
        return getPrefs(context).getString(NICKNAME, null)
    }

    /**
     * ❌ accessToken 삭제
     */
    fun clearAccessToken(context: Context) {
        getPrefs(context).edit {
            remove(KEY_ACCESS_TOKEN)
        }
    }

    /**
     * ❌ refreshToken 삭제
     */
    fun clearRefreshToken(context: Context) {
        getPrefs(context).edit {
            remove(KEY_REFRESH_TOKEN)
        }
    }

    /**
     * ❌ 모든 토큰 삭제
     */
    fun clearAllTokens(context: Context) {
        getPrefs(context).edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
        }
    }

    /**
     * ✅ access + refreshToken + kakao profile + kakao nickname 한 번에 저장
     */
    fun saveTokens(context: Context, accessToken: String?, refreshToken: String?) {
        getPrefs(context).edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    fun saveUserInfo(context: Context, profile: String, nickname: String) {
        getPrefs(context).edit {
            putString(PROFILE, profile)
            putString(NICKNAME, nickname)
        }
    }
}