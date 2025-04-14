package com.maumpeace.safeapp.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.ActivityMainBinding
import com.maumpeace.safeapp.ui.map.MapFragment
import com.maumpeace.safeapp.ui.settings.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapFragment: MapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupSafetyButton()
    }

    /**
     * ✨ BottomNavigationView 초기화
     * - 지도, 설정 버튼 구성
     */
    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = binding.bottomNav

        // 초기 화면은 지도 화면으로 설정
        mapFragment = MapFragment()
        loadFragment(mapFragment)

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_map -> {
                    // 🗺 지도 화면 로드
                    mapFragment = MapFragment()
                    loadFragment(mapFragment)
                    true
                }

                R.id.nav_settings -> {
                    // ⚙️ 설정 화면 로드
                    loadFragment(SettingsFragment())
                    true
                }

                else -> false
            }
        }
    }

    /**
     * 🚨 안심 버튼 클릭 초기화
     * - 지도 탭으로 전환 후 MapFragment에서 safety 메서드 실행
     */
    private fun setupSafetyButton() {
        binding.btnSafety.setOnClickListener {
            mapFragment = MapFragment()
            loadFragment(mapFragment)
            binding.bottomNav.selectedItemId = R.id.nav_map
            supportFragmentManager.executePendingTransactions()

            // 호출 안전 보장 위해 Handler.post 사용 가능
            mapFragment.triggerSafetyFeature()
        }
    }

    /**
     * ✨ Fragment 로드 함수
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}