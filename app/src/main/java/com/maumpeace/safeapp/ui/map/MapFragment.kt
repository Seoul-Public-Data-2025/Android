package com.maumpeace.safeapp.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.maumpeace.safeapp.databinding.FragmentMapBinding
import com.maumpeace.safeapp.util.UserStateData
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource

/**
 * 🗺 MapFragment - 지도 화면
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private var locationBtnIsClickable: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = binding.mapBottomSheet
        val behavior = BottomSheetBehavior.from(bottomSheet)

        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = 480

        toggleBottomSheet()

        locationSource = FusedLocationSource(this, 1000)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults
            )
        ) {
            getLastKnownLocation(naverMap)
            naverMap.locationTrackingMode = LocationTrackingMode.NoFollow
            naverMap.uiSettings.isCompassEnabled = false
            naverMap.uiSettings.isZoomControlEnabled = false
            naverMap.uiSettings.isScaleBarEnabled = false
            naverMap.uiSettings.setLogoMargin(10, 0, 0, 10)
            naverMap.locationOverlay.isVisible = true

            //자신의 위치로 카메라 이동
            val locationBtn = binding.ivMyLocation
            locationBtn.setOnClickListener {
                if (locationBtnIsClickable) {
                    locationBtnIsClickable = false
                    naverMap.locationOverlay.isVisible = true
                    naverMapMoveCamera(naverMap)
                    Handler(Looper.getMainLooper()).postDelayed({
                        locationBtnIsClickable = true
                    }, 1000)    //1초에 한 번씩 버튼 예외처리
                }
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun toggleBottomSheet() {
        val behavior = BottomSheetBehavior.from(binding.mapBottomSheet)
        behavior.state =
            if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_EXPANDED
    }

    private fun naverMapMoveCamera(naverMap: NaverMap) {
        naverMap.moveCamera(
            CameraUpdate.scrollTo(
                LatLng(
                    UserStateData.getMyLatLng().latitude, UserStateData.getMyLatLng().longitude
                )
            )
        )
    }

    private fun getLastKnownLocation(naverMap: NaverMap) {
        if (requireContext().let {
                ActivityCompat.checkSelfPermission(
                    it, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED && requireContext().let {
                ActivityCompat.checkSelfPermission(
                    it, Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED) {
            return
        }
        LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                UserStateData.setMyLatLng(LatLng(location))
                naverMap.locationOverlay.isVisible = true
                naverMap.locationOverlay.position = LatLng(
                    UserStateData.getMyLatLng().latitude, UserStateData.getMyLatLng().longitude
                )
                naverMap.moveCamera(
                    CameraUpdate.scrollTo(
                        LatLng(
                            UserStateData.getMyLatLng().latitude,
                            UserStateData.getMyLatLng().longitude
                        )
                    )
                )
            }
        }
    }

    fun triggerSafetyFeature() {
        val overlay = binding.emergencyOverlay
        val btnCancel = binding.btnEmergencyCancel
        val btnConfirm = binding.btnEmergencyConfirm
        val ivMyLocation = binding.ivMyLocation

        ivMyLocation.visibility = View.GONE
        overlay.visibility = View.VISIBLE

        btnCancel.setOnClickListener {
            ivMyLocation.visibility = View.VISIBLE
            overlay.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            ivMyLocation.visibility = View.VISIBLE
            overlay.visibility = View.GONE
            Toast.makeText(requireContext(), "안심 경로 안내를 시작합니다.", Toast.LENGTH_SHORT).show()
            if (locationBtnIsClickable) {
                locationBtnIsClickable = false
                naverMap.locationOverlay.isVisible = true
                naverMapMoveCamera(naverMap)
                Handler(Looper.getMainLooper()).postDelayed({
                    locationBtnIsClickable = true
                }, 1000)    //1초에 한 번씩 버튼 예외처리
            }
            // TODO: 실제 안내 로직 호출
        }
    }

    override fun onMapReady(naverMapSet: NaverMap) {
        this.naverMap = naverMapSet
        this.naverMap.locationSource = locationSource
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000
        )
        naverMap.lightness = 0.0f
        naverMap.maxZoom = 19.0
        naverMap.minZoom = 13.0
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }
}