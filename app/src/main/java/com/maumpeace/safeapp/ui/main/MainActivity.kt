package com.maumpeace.safeapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.ActivityMainBinding
import com.maumpeace.safeapp.ui.map.MapFragment
import com.maumpeace.safeapp.ui.settings.SettingsFragment
import com.maumpeace.safeapp.util.UserStateData
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapFragment: MapFragment
    private lateinit var settingsFragment: SettingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragments()
        setupBottomNavigation()
        setupSafetyButton()
        startLocationUpdates()
    }


    private fun setupFragments() {
        mapFragment = MapFragment()
        settingsFragment = SettingsFragment()

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, mapFragment, "MAP")
            .add(R.id.fragment_container, settingsFragment, "SETTINGS")
            .hide(settingsFragment)
            .commit()
    }

    /**
     * ✨ BottomNavigationView 초기화
     * - 지도, 설정 버튼 구성
     */

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

    // 위치 권한을 확인하고 권한이 없다면 반환
    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(500F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                UserStateData.setMyLatLng(LatLng(location))
            }
        }

        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                UserStateData.setMyLatLng(LatLng(location.latitude, location.longitude))
            }
        }
    }

    /**
     * 🚨 안심 버튼 클릭 초기화
     * - 지도 탭으로 전환 후 MapFragment에서 safety 메서드 실행
     */
    private fun setupSafetyButton() {
        binding.btnSafety.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_map
            mapFragment.triggerSafetyFeature()
        }
    }
}