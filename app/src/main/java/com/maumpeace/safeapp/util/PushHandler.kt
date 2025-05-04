package com.maumpeace.safeapp.util

import android.content.Context
import android.content.Intent
import com.maumpeace.safeapp.ui.role.RoleTabActivity

object PushHandler {
    fun handlePush(context: Context, type: String, id: String) {
        when (type) {
            "regist" -> {
                val intent = Intent(context, RoleTabActivity::class.java).apply {
                    putExtra("start_tab", "child")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
//            "delete" -> Toast.makeText(context, "delete ID $id 로 이동", Toast.LENGTH_SHORT).show()
//            else -> Toast.makeText(context, "알 수 없는 알림 타입", Toast.LENGTH_SHORT).show()
        }
    }
}