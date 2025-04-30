package com.maumpeace.safeapp.ui.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.ViewInAppNotificationBinding

/**
 * ✅ BaseActivity
 * - 모든 Activity의 공통 기능 관리
 * - In-App Notification Banner 제공
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
    private lateinit var notificationBinding: ViewInAppNotificationBinding

    abstract fun inflateBinding(inflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        notificationBinding = ViewInAppNotificationBinding.bind(findViewById(R.id.inAppNotificationBanner))
    }

    /**
     * 🔔 In-App Notification 표시
     */
    fun showInAppNotification(title: String, body: String, onClickAction: (() -> Unit)? = null) {
        with(notificationBinding) {
            tvNotificationTitle.text = title
            tvNotificationBody.text = body

            root.visibility = View.VISIBLE
            root.setOnClickListener {
                root.visibility = View.GONE
                onClickAction?.invoke()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                root.visibility = View.GONE
            }, 5000)
        }
    }

}