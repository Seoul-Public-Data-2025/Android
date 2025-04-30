package com.maumpeace.safeapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.ui.main.MainActivity
import com.maumpeace.safeapp.util.PushEventBus
import com.maumpeace.safeapp.util.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ SafeAppFirebaseMessagingService
 * - 포그라운드 수신 처리
 * - 토큰 갱신 처리
 */
class SafeAppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: ""
        val body = remoteMessage.notification?.body ?: ""

        if (title.isNotBlank() && body.isNotBlank()) {
            if (isAppInForeground()) {
                // 🔥 포그라운드 → PushEventBus로 알림
                CoroutineScope(Dispatchers.Main).launch {
                    PushEventBus.sendPush(title, body)
                }
            } else {
                // 🔥 백그라운드 → 시스템 Notification 직접 띄우기
                showSystemNotification(title, body)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        TokenManager.saveFcmToken(applicationContext, token)

        val accessToken = TokenManager.getAccessToken(applicationContext)
        if (!accessToken.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 성공 로깅 등
                } catch (e: Exception) {
                    // 실패 시 로깅
                }
            }
        }
    }


    /**
     * 앱이 포그라운드 상태인지 확인
     */
    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    /**
     * 백그라운드 알림(Notification) 생성
     */
    private fun showSystemNotification(title: String, body: String) {
        val channelId = "maum_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId, "SafeApp 알림", NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // 작은 아이콘 필요
            .setContentTitle(title).setContentText(body).setContentIntent(pendingIntent)
            .setAutoCancel(true).build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}