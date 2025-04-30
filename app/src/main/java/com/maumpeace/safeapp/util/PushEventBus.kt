package com.maumpeace.safeapp.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ✅ 푸시 알림 이벤트를 앱 내부로 전달하는 싱글톤
 */
object PushEventBus {
    private val _pushFlow = MutableSharedFlow<Triple<String, String, Map<String, String>>>()
    val pushFlow = _pushFlow.asSharedFlow()

    suspend fun sendPush(title: String, body: String, data: Map<String, String> = emptyMap()) {
        _pushFlow.emit(Triple(title, body, data))
    }
}