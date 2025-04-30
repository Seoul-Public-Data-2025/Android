package com.maumpeace.safeapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.ui.splash.SplashActivity
import com.maumpeace.safeapp.util.PushConstants
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
        val data = remoteMessage.data

        if (title.isNotBlank() && body.isNotBlank()) {
            if (isAppInForeground()) {
                CoroutineScope(Dispatchers.Main).launch {
                    PushEventBus.sendPush(title, body, data)
                }
            } else {
                showSystemNotification(title, body, data[PushConstants.KEY_TYPE], data[PushConstants.KEY_ID])
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
    private fun showSystemNotification(title: String, body: String, type: String?, id: String?) {
        val channelId = "maum_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "SafeApp 알림",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // 🔹 알림 클릭 시 전달할 Intent
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("push_type", type)
            putExtra("push_id", id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun handlePushIntent(intent: Intent?) {
        val type = intent?.getStringExtra("push_type")
        val id = intent?.getStringExtra("push_id")

        if (!type.isNullOrBlank() && !id.isNullOrBlank()) {
            when (type) {
                "regist" -> {
                    // 예: 공지사항 상세 Fragment 로 이동
                    Toast.makeText(this, "regist ID $id 로 이동", Toast.LENGTH_SHORT).show()

                    // 실제로는 Fragment 전환 또는 화면 이동 코드 실행
                }

                "delete" -> {
                    Toast.makeText(this, "delete ID $id 로 이동", Toast.LENGTH_SHORT).show()
                }

                // 기타 케이스
            }
        }
    }

}