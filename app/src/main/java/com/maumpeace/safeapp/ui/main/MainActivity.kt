package com.maumpeace.safeapp.ui.main

// 필요한 Android 및 앱 관련 라이브러리 import
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
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
import com.maumpeace.safeapp.ui.role.RoleTabActivity
import com.maumpeace.safeapp.ui.settings.SettingsFragment
import com.maumpeace.safeapp.util.PushConstants
import com.maumpeace.safeapp.util.PushEventBus
import com.maumpeace.safeapp.util.UserStateData
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var mapFragment: MapFragment
    private lateinit var settingsFragment: SettingsFragment
    private var lastBackPressedTime = 0L

    // 바인딩 초기화 함수
    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    // 액티비티 생성 시 초기화 작업 수행
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 프래그먼트 및 UI 초기 설정
        setupFragments()
        setupBottomNavigation()
        setupSafetyButton()
        startLocationUpdates()
        observePushEvents()
        handlePushIntent(intent)

        // 백버튼 처리 커스텀
        onBackPressedDispatcher.addCallback(this) {
            handleBackPressed()
        }
    }

    // 푸시 인텐트 새로 들어올 때 처리
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    // 푸시 알림 인텐트 수신 시 화면 전환 또는 기능 수행
    private fun handlePushIntent(intent: Intent?) {
        val alreadyHandled = intent?.getBooleanExtra("push_handled", false) ?: false
        if (alreadyHandled) return

        val type = intent?.getStringExtra(PushConstants.KEY_TYPE)
        val id = intent?.getStringExtra(PushConstants.KEY_ID)
        val url = intent?.getStringExtra(PushConstants.KEY_URL)

        if (!type.isNullOrBlank()) {
            when (type) {
                "regist", "delete" -> {
                    // 자녀 탭 화면으로 이동
                    startActivity(Intent(this, RoleTabActivity::class.java).apply {
                        putExtra("start_tab", "child")
                    })
                }

                "child-location" -> {
                    // 자녀 위치 실시간 연결
                    val mapFragment =
                        supportFragmentManager.findFragmentByTag("MAP") as? MapFragment
                    mapFragment?.apply {
                        Handler(Looper.getMainLooper()).postDelayed({
                            startSse(url)
                        }, 300)
                    }
                }

                else -> {
                    // 알 수 없는 타입
                }
            }

            intent.putExtra("push_handled", true)
            setIntent(Intent())
        }
    }

    // PushEventBus를 통해 수신된 푸시를 앱 내 배너로 표시
    private fun observePushEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PushEventBus.pushFlow.collect { (title, body, data) ->
                    val type = data["type"]
                    val url = data["url"]

                    showInAppNotification(title, body) {
                        if (!type.isNullOrBlank()) {
                            when (type) {
                                "child-location" -> {
                                    val mapFragment =
                                        supportFragmentManager.findFragmentByTag("MAP") as? MapFragment
                                    mapFragment?.apply {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            startSse(url)
                                        }, 300)
                                    }
                                }

                                else -> handlePushType(type)
                            }
                        } else {
                            Toast.makeText(this@MainActivity, body, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // 푸시 타입 분기에 따른 처리 함수
    private fun handlePushType(type: String) = when (type) {
        "regist", "delete" -> {
            startActivity(Intent(this, RoleTabActivity::class.java).apply {
                putExtra("start_tab", "child")
            })
        }

        else -> {
            // 알 수 없는 타입
        }
    }

    // MapFragment, SettingsFragment 초기화 및 추가
    private fun setupFragments() {
        mapFragment = MapFragment()
        settingsFragment = SettingsFragment()

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, mapFragment, "MAP")
            .add(R.id.fragment_container, settingsFragment, "SETTINGS")
            .hide(settingsFragment)
            .commit()
    }

    // 하단 네비게이션 바 설정 및 탭 전환 처리
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

    // 현재 표시할 프래그먼트를 전환
    private fun switchFragment(fragmentToShow: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            supportFragmentManager.fragments.forEach { hide(it) }
            show(fragmentToShow)
        }.commit()
    }

    // 위치 정보 수집 시작
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 위치 요청 조건 설정
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).apply {
            setMinUpdateDistanceMeters(500F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        // 마지막 위치 가져와 저장
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
            location?.let {
                UserStateData.setMyLatLng(LatLng(it.latitude, it.longitude))
            }
        }

        // 실시간 위치 업데이트 요청
        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // 위치 콜백 정의
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                UserStateData.setMyLatLng(LatLng(location.latitude, location.longitude))
            }
        }
    }

    // 안심 버튼 클릭 시 지도 탭으로 전환 및 기능 실행
    private fun setupSafetyButton() {
        binding.btnSafety.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_map
            mapFragment.triggerSafetyFeature()
        }
    }

    // 백버튼 눌렀을 때 처리
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

    // 앱 종료 확인 다이얼로그 표시
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