package com.maumpeace.safeapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.addCallback
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
import com.maumpeace.safeapp.util.PushEventBus
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

        onBackPressedDispatcher.addCallback(this) {
            handleBackPressed()
        }
    }

    /**
     * 🔔 PushEventBus 수신하여 In-App Notification 띄우기
     */
    private fun observePushEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PushEventBus.pushFlow.collect { (title, body) ->
                    showInAppNotification(title, body) {
                        // 🔥 알림 클릭 시 Toast로 body 보여주기
                        Toast.makeText(this@MainActivity, body, Toast.LENGTH_SHORT).show()
                    }
                }
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

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).apply {
            setMinUpdateDistanceMeters(500F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
            location?.let {
                UserStateData.setMyLatLng(LatLng(it.latitude, it.longitude))
            }
        }

        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
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
}