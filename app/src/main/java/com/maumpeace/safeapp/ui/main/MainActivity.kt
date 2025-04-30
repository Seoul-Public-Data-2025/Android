package com.maumpeace.safeapp.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.ActivityMainBinding
import com.maumpeace.safeapp.ui.base.BaseActivity
import com.maumpeace.safeapp.ui.dialog.ExitConfirmBottomSheet
import com.maumpeace.safeapp.ui.map.MapFragment
import com.maumpeace.safeapp.ui.settings.SettingsFragment
import com.maumpeace.safeapp.ui.splash.SplashActivity
import com.maumpeace.safeapp.util.PushConstants
import com.maumpeace.safeapp.util.PushEventBus
import com.maumpeace.safeapp.util.PushHandler
import com.maumpeace.safeapp.util.UserStateData
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * ✅ MainActivity
 * - 지도 & 설정 화면 전환
 * - 위치 정보 수집
 * - 안심 버튼 처리
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var mapFragment: MapFragment
    private lateinit var settingsFragment: SettingsFragment
    private var lastBackPressedTime = 0L

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFragments()
        setupBottomNavigation()
        setupSafetyButton()
        startLocationUpdates()
        observePushEvents()
        handlePushIntent(intent)

        onBackPressedDispatcher.addCallback(this) {
            handleBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handlePushIntent(intent) // ✅ 새 인텐트 들어올 때도 처리
    }

    private fun handlePushIntent(intent: Intent?) {
        val type = intent?.getStringExtra(PushConstants.KEY_TYPE)
        val id = intent?.getStringExtra(PushConstants.KEY_ID)
        if (!type.isNullOrBlank() && !id.isNullOrBlank()) {
            PushHandler.handlePush(this, type, id)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(500F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
    }

    /**
     * 🔔 PushEventBus 수신하여 In-App Notification 띄우기
     */
    private fun observePushEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PushEventBus.pushFlow.collect { (title, body, data) ->
                    val type = data["type"]
                    val id = data["id"]

                    showInAppNotification(title, body) {
                        // 🔥 배너 클릭 시도 type/id 기반 분기
                        if (!type.isNullOrBlank() && !id.isNullOrBlank()) {
                            handlePushType(type, id)
                        } else {
                            Toast.makeText(this@MainActivity, body, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun handlePushType(type: String, id: String) {
        when (type) {
            "regist" -> {
                Toast.makeText(this, "regist ID $id 로 이동", Toast.LENGTH_SHORT).show()
                // TODO: 공지 화면 이동 로직 삽입
            }

            "delete" -> {
                Toast.makeText(this, "delete ID $id 로 이동", Toast.LENGTH_SHORT).show()
                // TODO: 채팅방 화면 이동
            }

            else -> {
                Toast.makeText(this, "알 수 없는 알림 타입", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFragments() {
        mapFragment = MapFragment()
        settingsFragment = SettingsFragment()

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, mapFragment, "MAP")
            .add(R.id.fragment_container, settingsFragment, "SETTINGS").hide(settingsFragment)
            .commit()
    }

    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = binding.bottomNav

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_map -> {
                    switchFragment(mapFragment)
                    true
                }

                R.id.nav_settings -> {
                    switchFragment(settingsFragment)
                    true
                }

                else -> false
            }
        }
    }

    private fun switchFragment(fragmentToShow: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            supportFragmentManager.fragments.forEach { hide(it) }
            show(fragmentToShow)
        }.commit()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        val locationRequest = createLocationRequest()
        val locationClient = LocationServices.getFusedLocationProviderClient(this)

        locationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                UserStateData.setMyLatLng(LatLng(it.latitude, it.longitude))
            }
        }

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                UserStateData.setMyLatLng(LatLng(location.latitude, location.longitude))
            }
        }
    }

    private fun setupSafetyButton() {
        binding.btnSafety.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_map
            mapFragment.triggerSafetyFeature()
        }
    }

    private fun handleBackPressed() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastBackPressedTime < 500) {
            return
        }
        lastBackPressedTime = currentTime

        val currentFragment = supportFragmentManager.fragments.find { it.isVisible }

        when (currentFragment) {
            is SettingsFragment -> {
                switchFragment(mapFragment)
                binding.bottomNav.selectedItemId = R.id.nav_map
            }

            is MapFragment -> {
                showExitConfirmDialog()
            }

            else -> {
                finish()
            }
        }
    }

    private fun showExitConfirmDialog() {
        val existingDialog = supportFragmentManager.findFragmentByTag("ExitConfirmDialog")
        if (existingDialog != null && existingDialog.isVisible) {
            return
        }

        val dialog = ExitConfirmBottomSheet {
            finish()
        }
        dialog.show(supportFragmentManager, "ExitConfirmDialog")
    }

    private fun showSystemNotification(title: String, body: String, type: String?, id: String?) {
        val channelId = "maum_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "SafeApp 알림", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(PushConstants.PUSH_TYPE, type)
            putExtra(PushConstants.PUSH_ID, id)
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

        val notificationId = (System.nanoTime() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)
    }
}