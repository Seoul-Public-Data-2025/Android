package com.maumpeace.safeapp.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.FragmentMapBinding
import com.maumpeace.safeapp.model.MapMarkerInfoData
import com.maumpeace.safeapp.network.NaverDirectionsService
import com.maumpeace.safeapp.util.HttpErrorHandler
import com.maumpeace.safeapp.util.TokenManager
import com.maumpeace.safeapp.util.UserStateData
import com.maumpeace.safeapp.viewModel.MapMarkerViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import androidx.core.graphics.scale

/**
 * 🗺 MapFragment - 지도 화면
 */
@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    @Inject
    lateinit var directionsService: NaverDirectionsService
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private var locationBtnIsClickable: Boolean = true
    private val mapMarkerViewModel: MapMarkerViewModel by viewModels()
    private val markerMap = mutableMapOf<String, MutableList<Marker>>()
    private val markerVisibleMap = mutableMapOf(
        "001" to true, "002" to true, "003" to true, "004" to true
    )
    private var currentPolyline: PathOverlay? = null
    private var selectedMarker: Marker? = null
    private var selectedMarkerType: String? = null
    private var isGuiding = false
    private var guidingEndMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheet()
        toggleBottomSheet()

        binding.textEmergencyMessage.text = "근처 안심경로로\n안내를 시작할까요?"
        binding.btnEmergencyConfirm.text = "안내 시작"
        locationSource = FusedLocationSource(this, 1000)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.llPolice.setOnClickListener { toggleMarker("001", binding.ivPolice) }
        binding.llCctv.setOnClickListener { toggleMarker("002", binding.ivCctv) }
        binding.llSafetyLight.setOnClickListener { toggleMarker("003", binding.ivSafetyLight) }
        binding.llSafetyFacility.setOnClickListener {
            toggleMarker("004", binding.ivSafetyFacility)
        }
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

        val profileUrl = TokenManager.getProfile(requireContext())
        val desiredSize = 100 // 원하는 픽셀 크기 (width, height 둘 다)

        LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Glide.with(requireContext())
                    .asBitmap()
                    .load(profileUrl)
                    .circleCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val resized = resource.scale(desiredSize, desiredSize, false)
                            val overlayImage = OverlayImage.fromBitmap(resized)

                            naverMap.locationOverlay.icon = overlayImage
                            naverMap.locationOverlay.isVisible = true
                            naverMap.locationOverlay.position = LatLng(
                                location.latitude,
                                location.longitude
                            )
                            naverMap.moveCamera(CameraUpdate.scrollTo(LatLng(location.latitude, location.longitude)))
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // 필요 없다면 비워두기
                        }
                    })
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun triggerSafetyFeature() {
        val overlay = binding.emergencyOverlay
        val textEmergencyMessage = binding.textEmergencyMessage
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
            if (!isGuiding) {
                textEmergencyMessage.text = "안심경로 안내를 취소할까요?"
                btnConfirm.text = "안내 취소"
                isGuiding = true
                startGuidance() // 현재 구현된 길찾기 실행 코드
            } else {
                // 안내 종료
                textEmergencyMessage.text = "근처 안심경로로\n안내를 시작할까요?"
                btnConfirm.text = "안내 시작"
                isGuiding = false
                stopGuidance() // 아래에 새로 정의
            }
        }
    }

    private fun setMarkerVisible(type: String, iconView: ImageView, shouldShow: Boolean) {
        markerVisibleMap[type] = shouldShow
        markerMap[type]?.forEach { marker ->
            marker.map = if (shouldShow) naverMap else null
        }

        val newIcon = if (shouldShow) getMarkerIconRes(type) else getOffMarkerIconRes(type)
        iconView.setImageResource(newIcon)
    }

    private fun startGuidance() {
        Toast.makeText(requireContext(), "안심 경로 안내를 시작합니다.", Toast.LENGTH_SHORT).show()
        val currentLocation = UserStateData.getMyLatLng()
        var closestMarker: Marker? = null
        var minDistance = Double.MAX_VALUE

        setMarkerVisible("001", binding.ivPolice, true)
        setMarkerVisible("002", binding.ivCctv, true)
        setMarkerVisible("003", binding.ivSafetyLight, true)
        setMarkerVisible("004", binding.ivSafetyFacility, true)

        // "001" (경찰서) + "004" (안심지킴이집) 마커만 필터링
        val targetMarkers = (markerMap["001"] ?: emptyList()) + (markerMap["004"] ?: emptyList())

        for (marker in targetMarkers) {
            val distance = currentLocation.distanceTo(marker.position)
            if (distance < minDistance) {
                minDistance = distance
                closestMarker = marker
            }
        }

        if (closestMarker != null) {
            val start = "${currentLocation.longitude},${currentLocation.latitude}"
            val goal = "${closestMarker.position.longitude},${closestMarker.position.latitude}"

            lifecycleScope.launch {
                try {
                    val response = directionsService.getRoutePath(
                        start = start,
                        goal = goal,
                        clientId = "6zm16zmxr2",
                        clientSecret = "4QG3QWQ0oRE9tk01Ym10XrRz4Vi8vHGT5hlTPKUF"
                    )

                    val path = response.route.traoptimal.first().path
                    val coords = path.map { LatLng(it[1], it[0]) }

                    // 기존 경로 제거
                    currentPolyline?.map = null

                    val pathOverlay = PathOverlay().apply {
                        this.coords = coords
                        color = resources.getColor(android.R.color.transparent, null)
                        passedColor = resources.getColor(R.color.red_f55b63, null)
                        outlineWidth = 5
                        width = 15
                        progress = 0.0
                        map = naverMap
                    }
                    currentPolyline = pathOverlay

                    // 애니메이션 진행
                    var time = 0.0
                    val timer = kotlin.concurrent.timer(period = 25) {
                        if (time <= 1.0) {
                            time += 0.01
                        } else {
                            cancel()
                        }

                        Handler(Looper.getMainLooper()).post {
                            pathOverlay.progress = time
                        }
                    }

                    // 카메라 이동: 내 위치 기준
                    val cameraUpdate =
                        CameraUpdate.scrollTo(currentLocation).animate(CameraAnimation.Fly, 2500)
                    naverMap.moveCamera(cameraUpdate)

                } catch (e: HttpException) {
                    val message = HttpErrorHandler.parseErrorMessage(e)
                    Toast.makeText(requireContext(), "경로 안내 실패: $message", Toast.LENGTH_SHORT)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(), "경로 안내 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // 모든 마커 비활성화
            for ((type, markers) in markerMap) {
                markers.forEach { marker ->
                    val isEndMarker = marker == closestMarker
                    marker.icon = if (isEndMarker) {
                        marker.width = 88 * 3
                        marker.height = 88 * 3
                        marker.map = null
                        marker.map = naverMap
                        OverlayImage.fromResource(getSelectedMarkerIconRes(type))
                    } else {
                        marker.width = 88
                        marker.height = 88
                        marker.map = null
                        marker.map = naverMap
                        OverlayImage.fromResource(
                            when (type) {
                                "001" -> R.drawable.ic_off_police
                                "002" -> R.drawable.ic_off_cctv
                                "003" -> R.drawable.ic_off_safety_light
                                else -> R.drawable.ic_off_safety_facility
                            }
                        )
                    }
                }
            }

        } else {
            Toast.makeText(requireContext(), "근처에 안전 목적지 마커가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopGuidance() {

        setMarkerVisible("001", binding.ivPolice, true)
        setMarkerVisible("002", binding.ivCctv, true)
        setMarkerVisible("003", binding.ivSafetyLight, true)
        setMarkerVisible("004", binding.ivSafetyFacility, true)

        // 경로 제거
        currentPolyline?.map = null

        // 마커 초기화
        for ((type, markers) in markerMap) {
            for (marker in markers) {
                marker.icon = OverlayImage.fromResource(getMarkerIconRes(type))
                marker.width = 88
                marker.height = 88
                marker.map = null
                marker.map = naverMap
            }
        }

        // 선택된 마커 초기화
        selectedMarker = null
        selectedMarkerType = null
        guidingEndMarker = null
    }


    private fun mapMarker() {
        mapMarkerViewModel.mapMarker()
        mapMarkerViewModel.mapMarkerData.observe(viewLifecycleOwner) { mapMarkerData ->
            mapMarkerData?.let {
                showMarkers(mapMarkerData.result)
            }
        }

        mapMarkerViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Timber.tag("error: ").e(it)
//                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMarker(type: String, iconView: ImageView) {
        val isVisible = markerVisibleMap[type] ?: true
        markerVisibleMap[type] = !isVisible

        // 마커 on/off
        markerMap[type]?.forEach { marker ->
            marker.map = if (!isVisible) naverMap else null
        }

        // 아이콘 변경
        val newIcon = if (!isVisible) getMarkerIconRes(type) else {
            when (type) {
                "001" -> R.drawable.ic_off_police
                "002" -> R.drawable.ic_off_cctv
                "003" -> R.drawable.ic_off_safety_light
                else -> R.drawable.ic_off_safety_facility
            }
        }
        iconView.setImageResource(newIcon)
    }


    private fun showMarkers(markerList: List<MapMarkerInfoData>) {
        binding.progressLoading.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            markerMap.clear()

            for (markerData in markerList) {
                val lat = markerData.lat?.toDoubleOrNull()
                val lot = markerData.lot?.toDoubleOrNull()
                val type = markerData.type ?: continue

                if (lat != null && lot != null) {
                    val marker = Marker().apply {
                        position = LatLng(lat, lot)
                        icon = OverlayImage.fromResource(getMarkerIconRes(type))
                        captionText = markerData.name?.split(" ")?.last() ?: ""
                        width = 88
                        height = 88
                        map = if (markerVisibleMap[type] == true) naverMap else null
                    }

                    markerMap.getOrPut(type) { mutableListOf() }.add(marker)

                    marker.setOnClickListener {
                        // 이전 마커 원복
                        selectedMarker?.let { prev ->
                            val prevType = selectedMarkerType
                            prev.icon = OverlayImage.fromResource(
                                if (isGuiding && prev != guidingEndMarker) getOffMarkerIconRes(
                                    prevType ?: ""
                                )
                                else getMarkerIconRes(prevType ?: "")
                            )
                            prev.width = 88
                            prev.height = 88
                            prev.map = null
                            prev.map = naverMap
                        }

                        // 현재 마커 저장 및 강조 표시
                        selectedMarker = marker
                        selectedMarkerType = type
                        marker.icon = OverlayImage.fromResource(getSelectedMarkerIconRes(type))
                        marker.width = 88 * 3
                        marker.height = 88 * 3
                        marker.map = null
                        marker.map = naverMap
                        true
                    }
                }
            }

            binding.progressLoading.visibility = View.GONE
        }, 500)
    }

    private fun getSelectedMarkerIconRes(type: String?): Int {
        return when (type) {
            "001" -> R.drawable.ic_police_destination
            "002" -> R.drawable.ic_cctv_destination
            "003" -> R.drawable.ic_safety_light_destination
            "004" -> R.drawable.ic_safety_facility_destination
            else -> R.drawable.ic_red_caution
        }
    }

    private fun getOffMarkerIconRes(type: String?): Int {
        return when (type) {
            "001" -> R.drawable.ic_off_police
            "002" -> R.drawable.ic_off_cctv
            "003" -> R.drawable.ic_off_safety_light
            else -> R.drawable.ic_off_safety_facility
        }
    }


    private fun setupBottomSheet() {
        val behavior = BottomSheetBehavior.from(binding.mapBottomSheet)
        val screenHeight = resources.displayMetrics.heightPixels
        val calculatedPeekHeight = (screenHeight * 0.25).toInt() // 기존 0.35 → 0.25 로 낮춤
        behavior.peekHeight = calculatedPeekHeight

        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getMarkerIconRes(type: String): Int {
        return when (type) {
            "001" -> R.drawable.ic_police
            "002" -> R.drawable.ic_cctv
            "003" -> R.drawable.ic_safety_light
            "004" -> R.drawable.ic_safety_facility
            else -> R.drawable.ic_red_caution // 기본값
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

        mapMarker()

        binding.llSafetyDirections.setOnClickListener {

        }
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