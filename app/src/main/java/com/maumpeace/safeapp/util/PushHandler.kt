package com.maumpeace.safeapp.util

import android.content.Context
import android.widget.Toast

object PushHandler {
    fun handlePush(context: Context, type: String, id: String) {
        when (type) {
            "regist" -> Toast.makeText(context, "regist ID $id 로 이동", Toast.LENGTH_SHORT).show()
            "delete" -> Toast.makeText(context, "delete ID $id 로 이동", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(context, "알 수 없는 알림 타입", Toast.LENGTH_SHORT).show()
        }
    }
}