package com.maumpeace.safeapp.ui.map

import android.Manifest
import android.animation.ValueAnimator
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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.maumpeace.safeapp.BuildConfig
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.FragmentMapBinding
import com.maumpeace.safeapp.model.MapMarkerInfoData
import com.maumpeace.safeapp.network.NaverDirectionsService
import com.maumpeace.safeapp.util.TokenManager
import com.maumpeace.safeapp.util.UserStateData
import com.maumpeace.safeapp.viewModel.ChildLocationDisconnectViewModel
import com.maumpeace.safeapp.viewModel.ChildLocationViewModel
import com.maumpeace.safeapp.viewModel.MapMarkerViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    @Inject
    lateinit var directionsService: NaverDirectionsService
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private val mapMarkerViewModel: MapMarkerViewModel by viewModels()
    private val childLocationViewModel: ChildLocationViewModel by viewModels()

    private val childLocationDisconnectViewModel: ChildLocationDisconnectViewModel by viewModels()
    private var lastSseMessageTime: Long = 0L
    private val sseTimeoutHandler = Handler(Looper.getMainLooper())
    private val sseTimeoutRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (now - lastSseMessageTime >= 10_000) {
                Timber.w("SSE 수신 중단 감지됨, 연결 종료 처리")
                Toast.makeText(requireContext(), "자녀가 위치 공유를 종료했어요", Toast.LENGTH_SHORT).show()
                sseCall?.cancel()
                sseCall = null
                sseTimeoutHandler.removeCallbacks(this)

                currentChildId?.let { id ->
                    Timber.d("📍 childLocationMarkerMap contains id: ${childLocationMarkerMap.containsKey(id)}")

                    markerTimeoutMap[id]?.let { locationHandler.removeCallbacks(it) }
                    markerTimeoutMap.remove(id)

                    childLocationMarkerMap[id]?.let { marker ->
                        Timber.d("🧹 Marker found, removing from map")
                        Handler(Looper.getMainLooper()).post {
                            marker.map = null
                        }
                        childLocationMarkerMap.remove(id)
                    } ?: Timber.w("❗ Marker not found for id: $id")
                }
            } else {
                sseTimeoutHandler.postDelayed(this, 10_000)
            }
        }
    }

    private val markerMap = mutableMapOf<String, MutableList<Marker>>()
    private val markerVisibleMap =
        mutableMapOf("001" to true, "002" to true, "003" to true, "004" to true)
    private val waypoints = mutableListOf<MapMarkerInfoData>()

    private var destination: MapMarkerInfoData? = null
    private var selectedMarker: Marker? = null
    private var selectedMarkerType: String? = null
    private var currentPolyline: PathOverlay? = null

    private lateinit var waypointAdapter: WaypointAdapter
    private lateinit var optionBottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var markerInfoBottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var selectedMarkerData: MapMarkerInfoData? = null

    private var isUserTurnedOffCctv = false
    private var isUserTurnedOffSafety = false
    private var isGuiding = false
    private val cctvMarkerMap = mutableMapOf<LatLng, Marker>()

    private var sseCall: Call? = null
    private val childLocationMarkerMap = mutableMapOf<String, Marker>()

    private val locationHandler = Handler(Looper.getMainLooper())
    private var isSendingLocation = false

    private val markerTimeoutMap = mutableMapOf<String, Runnable>()

    private var currentChildId: String? = null

    private var isFirstLocationSent = false

    private val locationRunnable = object : Runnable {
        override fun run() {
            if (!isSendingLocation) return

            val userLatLng = naverMap.locationOverlay.position ?: return
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val lat = userLatLng.latitude.toString()
            val lot = userLatLng.longitude.toString()
            childLocationViewModel.childLocation(time, lat, lot)

            locationHandler.postDelayed(this, 2000) // 다시 20초 후 실행
        }
    }

    private fun extractChildIdFromUrl(url: String?): String? {
        return url?.substringAfterLast("/events/child/")?.removeSuffix("/")
            ?.takeIf { it.isNotBlank() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Fragment의 뷰 바인딩 초기화
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun startSendingLocation() {
        // 위치 전송이 이미 시작되었으면 무시
        if (isSendingLocation) return
        isSendingLocation = true
        locationHandler.post(locationRunnable)
    }

    private fun stopSendingLocation() {
        // 위치 전송 중지 및 핸들러에서 Runnable 제거
        isSendingLocation = false
        locationHandler.removeCallbacks(locationRunnable)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 위치 소스 초기화 (사용자 현재 위치 추적에 사용)
        locationSource = FusedLocationSource(this, 1000)

        // MapView 생성 및 지도 로딩
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // UI 요소 및 이벤트 초기화
        setupBottomSheets()
        setupWaypointRecyclerView()
        setupCategoryClickListeners()
        setupObservers()

        // BottomSheet 터치 이벤트 차단
        binding.optionBottomSheet.setOnTouchListener { _, _ -> true }
        binding.markerInfoBottomSheet.setOnTouchListener { _, _ -> true }

        // 버튼 클릭 리스너 설정
        setupMapButtons()
    }

    private fun setupMapButtons() {
        // 경로 취소 버튼 클릭 처리
        binding.btnCancelRoute.setOnClickListener { clearRoute() }

        // 경유지 추가 버튼 클릭 처리
        binding.btnAddWaypoint.setOnClickListener { handleAddWaypoint() }

        // 도착지 지정 버튼 클릭 처리
        binding.btnRouteDesignation.setOnClickListener { handleRouteDesignation() }

        // 도착지 제거 버튼 클릭 처리
        binding.btnRemoveDestination.setOnClickListener {
            destination = null
            binding.tvDestination.text = "도착지를 지정해주세요"
            binding.btnRemoveDestination.visibility = View.GONE
        }

        // 경로 생성 버튼 클릭 처리
        binding.btnRoute.setOnClickListener { handleCreateRoute() }
    }

    private fun handleAddWaypoint() {
        // 최대 3개까지 경유지 추가 가능
        if (waypoints.size >= 3) {
            Toast.makeText(requireContext(), "경유지는 최대 3개까지 추가할 수 있어요", Toast.LENGTH_SHORT).show()
            return
        }

        selectedMarkerData?.let { waypoint ->
            // 도착지와 중복 여부 체크
            if (destination?.lat == waypoint.lat && destination?.lot == waypoint.lot) {
                Toast.makeText(
                    requireContext(), "도착지로 지정된 장소는 경유지로 추가할 수 없어요", Toast.LENGTH_SHORT
                ).show()
                return
            }

            // 이미 추가된 경유지인지 체크
            val alreadyAdded = waypoints.any { it.lat == waypoint.lat && it.lot == waypoint.lot }
            if (alreadyAdded) {
                Toast.makeText(requireContext(), "이미 경유지로 추가된 장소예요", Toast.LENGTH_SHORT).show()
                return
            }

            // 경유지 추가 및 리사이클러뷰 갱신
            waypoints.add(waypoint)
            waypointAdapter.notifyItemInserted(waypoints.size - 1)
            binding.llRecyclerWaypoint.visibility = View.VISIBLE
        }
    }

    private fun handleRouteDesignation() {
        selectedMarkerData?.let { data ->
            val isAlreadyWaypoint = waypoints.any { it.lat == data.lat && it.lot == data.lot }
            if (isAlreadyWaypoint) {
                Toast.makeText(requireContext(), "이미 경유지로 추가된 장소예요", Toast.LENGTH_SHORT).show()
                return
            }

            if (destination?.lat == data.lat && destination?.lot == data.lot) {
                Toast.makeText(requireContext(), "이미 도착지로 지정된 장소예요", Toast.LENGTH_SHORT).show()
                return
            }

            destination = data
            binding.tvDestination.text = "도착지: ${data.address}"
            binding.llRecyclerWaypoint.visibility = View.VISIBLE
            binding.btnRemoveDestination.visibility = View.VISIBLE
        }
    }

    private fun handleCreateRoute() {
        if (destination == null) {
            Toast.makeText(requireContext(), "도착지를 지정해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        waypointAdapter.isRoutingStarted = true
        updateRoute()

        // UI 요소 숨기기
        binding.btnRoute.visibility = View.GONE
        binding.btnAddWaypoint.visibility = View.GONE
        binding.btnRouteDesignation.visibility = View.GONE
        binding.btnRemoveDestination.visibility = View.GONE

        waypointAdapter.notifyDataSetChanged()
        disableCategoryButtons()
        moveCameraToRoute()
        deactivateUnrelatedMarkers()
    }

    fun startSse(url: String?) {
        Timber.d("SSE 연결 시도: $url")
        lastSseMessageTime = System.currentTimeMillis()
        currentChildId = extractChildIdFromUrl(url)
        sseTimeoutHandler.postDelayed(sseTimeoutRunnable, 10_000)
        Handler(Looper.getMainLooper()).postDelayed({ connectSseInternal(url) }, 300)
    }

    private fun connectSseInternal(url: String?) {
        if (url.isNullOrBlank()) return

        val sseUrl = "https://maum-seoul.shop$url"
        val request = Request.Builder().url(sseUrl)
            .header("Authorization", "Bearer ${TokenManager.getAccessToken(requireContext())}")
            .build()

        val client = OkHttpClient()
        sseCall = client.newCall(request)

        sseCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToastOnMain("SSE 연결 실패: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    showToastOnMain("SSE 실패: ${response.code}")
                    return
                }

                val source = response.body?.source() ?: return
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (line.startsWith("data:")) {
                        val jsonString = line.removePrefix("data:").trim()
                        try {
                            val json = JSONObject(jsonString)
                            lastSseMessageTime = System.currentTimeMillis() // 메시지 수신 시간 갱신

                            when (json.getString("type")) {
                                "location" -> {
                                    val lat = json.getDouble("lat")
                                    val lot = json.getDouble("lot")

                                    val childId = currentChildId ?: return

                                    Handler(Looper.getMainLooper()).post {
                                        updateChildLocationMarker(childId, lat, lot) // ✅ childEmail → childId 로 교체
                                    }
                                }

                                else -> {
                                    continue
                                }

//                                "done" -> {
//                                    val message = json.getString("message")
//                                    if (isDoneMessageHandled) return
//                                    isDoneMessageHandled = true
//                                    Handler(Looper.getMainLooper()).post {
//                                        showToastOnMain(message)
//                                        sseCall?.cancel()
//                                        childLocationMarkerMap.forEach { (_, marker) ->
//                                            marker.map = null
//                                        }
//                                        childLocationMarkerMap.clear()
//                                    }
//                                }
                            }
                        } catch (e: Exception) {
                            Timber.e("SSE 파싱 오류: ${e.message}")
                        }
                    }
                }
            }
        })
    }

    fun updateChildLocationMarker(childId: String, lat: Double, lot: Double) {
        val latLng = LatLng(lat, lot)

        val existingMarker = childLocationMarkerMap[childId]
        if (existingMarker != null) {
            existingMarker.position = latLng
            return
        }

        val marker = Marker().apply {
            icon = OverlayImage.fromResource(R.drawable.ic_default_profile)
            width = 200
            height = 200
            position = latLng
            map = naverMap
        }

        childLocationMarkerMap[childId] = marker // ✅ key로 childId 사용

        val cameraUpdate = CameraUpdate.scrollAndZoomTo(latLng, 16.0)
            .animate(CameraAnimation.Fly, 1000)
        Handler(Looper.getMainLooper()).post { naverMap.moveCamera(cameraUpdate) }

        markerTimeoutMap[childId]?.let { locationHandler.removeCallbacks(it) }
        val timeoutRunnable = Runnable {
            // 생략 가능
        }
        markerTimeoutMap[childId] = timeoutRunnable
        locationHandler.postDelayed(timeoutRunnable, 2000)
    }

    private fun showToastOnMain(message: String) {
        // UI 스레드에서 토스트 출력
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreMarkersAfterRouteCancel() {
        // 경로 안내 취소 후 마커 다시 보이기
        val zoom = naverMap.cameraPosition.zoom
        markerMap.forEach { (type, markers) ->
            val shouldShow = when (type) {
                "002", "003" -> zoom >= 14.5
                else -> true
            }
            markers.forEach { marker -> marker.map = if (shouldShow) naverMap else null }
        }
    }

    private fun disableCategoryButtons() {
        // 카테고리 버튼 비활성화
        binding.llPolice.isEnabled = false
        binding.llCctv.isEnabled = false
        binding.llSafetyLight.isEnabled = false
        binding.llSafetyFacility.isEnabled = false
    }

    private fun enableCategoryButtons() {
        // 카테고리 버튼 활성화
        binding.llPolice.isEnabled = true
        binding.llCctv.isEnabled = true
        binding.llSafetyLight.isEnabled = true
        binding.llSafetyFacility.isEnabled = true
    }


    private fun deactivateUnrelatedMarkers() {
        val includedLatLngs = mutableListOf<LatLng>()
        naverMap.locationOverlay.position.let { includedLatLngs.add(it) }
        waypoints.forEach { waypoint ->
            waypoint.lat?.toDoubleOrNull()?.let { lat ->
                waypoint.lot?.toDoubleOrNull()?.let { lot ->
                    includedLatLngs.add(LatLng(lat, lot))
                }
            }
        }
        destination?.let { dest ->
            dest.lat?.toDoubleOrNull()?.let { lat ->
                dest.lot?.toDoubleOrNull()?.let { lot ->
                    includedLatLngs.add(LatLng(lat, lot))
                }
            }
        }

        markerMap.values.flatten().forEach { marker ->
            val isRelated =
                includedLatLngs.any { it.latitude == marker.position.latitude && it.longitude == marker.position.longitude }
            marker.map = if (isRelated) naverMap else null
        }
    }


    private fun setupBottomSheets() {
        optionBottomSheetBehavior = BottomSheetBehavior.from(binding.optionBottomSheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
        }

        markerInfoBottomSheetBehavior =
            BottomSheetBehavior.from(binding.markerInfoBottomSheet).apply {
                state = BottomSheetBehavior.STATE_HIDDEN
                isHideable = true
                skipCollapsed = true
                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            binding.optionBottomSheet.visibility = View.VISIBLE
                            optionBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            restoreSelectedMarker()
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
    }

    private fun restoreSelectedMarker() {
        selectedMarker?.let { marker ->
            selectedMarkerType?.let { type ->
                ValueAnimator.ofInt(marker.width, 88).apply {
                    duration = 300
                    addUpdateListener { animator ->
                        val animatedSize = animator.animatedValue as Int
                        marker.width = animatedSize
                        marker.height = animatedSize
                        marker.map = null
                        marker.map = naverMap
                    }
                    start()
                }
                marker.icon = OverlayImage.fromResource(getMarkerIconRes(type))
            }
        }
        selectedMarker = null
        selectedMarkerType = null
    }

    private fun setupWaypointRecyclerView() {
        waypointAdapter = WaypointAdapter(
            items = waypoints,
            onRemoveClick = { removeWaypoint(it) },
        )
        binding.recyclerWaypoint.apply {
            adapter = waypointAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCategoryClickListeners() {
        binding.llPolice.setOnClickListener { toggleMarker("001", binding.ivPolice) }
        binding.llCctv.setOnClickListener {
            toggleMarker("002", binding.ivCctv)
            isUserTurnedOffCctv = !isUserTurnedOffCctv
        }
        binding.llSafetyLight.setOnClickListener {
            toggleMarker("003", binding.ivSafetyLight)
            isUserTurnedOffSafety = !isUserTurnedOffSafety
        }
        binding.llSafetyFacility.setOnClickListener {
            toggleMarker(
                "004", binding.ivSafetyFacility
            )
        }
    }

    private fun toggleMarker(type: String, iconView: ImageView) {
        val zoom = naverMap.cameraPosition.zoom
        if ((type == "002" || type == "003") && zoom < 14.5) {
            Toast.makeText(requireContext(), "지도를 좀 더 확대해야 해요", Toast.LENGTH_SHORT).show()
            return
        }

        val isVisible = markerVisibleMap[type] ?: true
        markerVisibleMap[type] = !isVisible

        markerMap[type]?.forEach { it.map = if (!isVisible) naverMap else null }

        iconView.setImageResource(
            if (!isVisible) getMarkerIconRes(type) else getOffMarkerIconRes(
                type
            )
        )
    }

    private fun removeWaypoint(position: Int) {
        if (position in waypoints.indices) {
            waypoints.removeAt(position)
            waypointAdapter.notifyItemRemoved(position)
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun clearRoute() {
        binding.llRecyclerWaypoint.visibility = View.GONE
        currentPolyline?.map = null
        waypoints.clear()
        destination = null
        waypointAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        stopSendingLocation()
        sseCall?.cancel()
        sseCall = null
        sseTimeoutHandler.removeCallbacks(sseTimeoutRunnable) // 타임아웃 핸들러 제거
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        sseCall?.cancel()
        sseCall = null
        Timber.d("MapFragment: SSE 연결 종료됨 (onStop)")
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.locationSource = locationSource

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
        getLastKnownLocation(naverMap)
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        moveToCurrentLocationImmediately()
        mapMarkers()

        naverMap.setOnMapClickListener { _, _ ->
            restoreSelectedMarker()
            binding.optionBottomSheet.visibility = View.VISIBLE
            optionBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.markerInfoBottomSheet.visibility = View.GONE
            markerInfoBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.ivMyLocation.setOnClickListener { moveToCurrentLocation() }

        naverMap.addOnCameraChangeListener { _, _ ->
            if (waypointAdapter.isRoutingStarted) return@addOnCameraChangeListener

            val zoom = naverMap.cameraPosition.zoom

            if (zoom < 14.5) {
                if (markerVisibleMap["002"] == true) {
                    markerVisibleMap["002"] = false
                    binding.ivCctv.setImageResource(R.drawable.ic_off_cctv)
                    markerMap["002"]?.forEach { it.map = null }
                }
                if (markerVisibleMap["003"] == true) {
                    markerVisibleMap["003"] = false
                    binding.ivSafetyLight.setImageResource(R.drawable.ic_off_safety_light)
                    markerMap["003"]?.forEach { it.map = null }
                }
            } else {
                if (markerVisibleMap["002"] == false && !isUserTurnedOffCctv) {
                    markerVisibleMap["002"] = true
                    binding.ivCctv.setImageResource(R.drawable.ic_cctv)
                    markerMap["002"]?.forEach { it.map = naverMap }
                }
                if (markerVisibleMap["003"] == false && !isUserTurnedOffSafety) {
                    markerVisibleMap["003"] = true
                    binding.ivSafetyLight.setImageResource(R.drawable.ic_safety_light)
                    markerMap["003"]?.forEach { it.map = naverMap }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)

                // 여기 수정! 확대까지 같이!
                val cameraUpdate =
                    CameraUpdate.scrollAndZoomTo(userLatLng, 16.0) // 👉 16 정도면 적당히 확대 (필요하면 숫자 조정)
                        .animate(CameraAnimation.Fly, 1000)

                naverMap.moveCamera(cameraUpdate)

            } else {
                Toast.makeText(requireContext(), "현재 위치를 가져올 수 없어요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    fun triggerSafetyFeature() {
        val nearestMarker = findNearestSafetyMarker()
        if (nearestMarker != null) {
            val overlay = binding.emergencyOverlay
            val textEmergencyMessage = binding.textEmergencyMessage
            val btnCancel = binding.btnEmergencyCancel
            val btnConfirm = binding.btnEmergencyConfirm
            val ivMyLocation = binding.ivMyLocation

            selectedMarker = null
            selectedMarkerType = null
            ivMyLocation.visibility = View.GONE
            overlay.visibility = View.VISIBLE

            btnCancel.setOnClickListener {
                ivMyLocation.visibility = View.VISIBLE
                overlay.visibility = View.GONE
            }

            btnConfirm.setOnClickListener {
                ivMyLocation.visibility = View.VISIBLE
                overlay.visibility = View.GONE

                clearRoute()
                waypointAdapter.isRoutingStarted = false

                binding.btnRoute.visibility = View.VISIBLE
                binding.btnAddWaypoint.visibility = View.VISIBLE
                binding.btnRouteDesignation.visibility = View.VISIBLE
                binding.btnRemoveDestination.visibility = View.GONE

                waypointAdapter.notifyDataSetChanged()

                binding.tvDestination.text = "도착지를 지정해주세요"

                // 카테고리 버튼 복구
                enableCategoryButtons()

                // 마커 복구 + 줌 기준 CCTV/안전등은 숨기기
                restoreMarkersAfterRouteCancel()

                if (!isGuiding) {
                    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val currentTime = formatter.format(Date())
                    val position = naverMap.locationOverlay.position
                    val lat = position.latitude.toString()
                    val lot = position.longitude.toString()

                    childLocationViewModel.childLocation(currentTime, lat, lot)
                    startSafetyRoute(nearestMarker)
                    startSendingLocation()

                    isGuiding = true
                    textEmergencyMessage.text = "안심경로 안내를 취소할까요?"
                    btnConfirm.text = "안내 취소"
                } else {
                    // ✨ 길찾기 취소
                    clearRoute()
                    waypointAdapter.isRoutingStarted = false

                    binding.btnRoute.visibility = View.VISIBLE
                    binding.btnAddWaypoint.visibility = View.VISIBLE
                    binding.btnRouteDesignation.visibility = View.VISIBLE
                    binding.btnRemoveDestination.visibility = View.GONE

                    waypointAdapter.notifyDataSetChanged()

                    binding.tvDestination.text = "도착지를 지정해주세요"

                    // 카테고리 버튼 복구
                    enableCategoryButtons()

                    // 마커 복구 + 줌 기준 CCTV/안전등은 숨기기
                    restoreMarkersAfterRouteCancel()
                    textEmergencyMessage.text = "근처 안심경로로\n안내를 시작할까요?"
                    btnConfirm.text = "안내 시작"
                    isGuiding = false
                    stopSendingLocation()

                    // 연결 종료 API 호출 → observe()에서 결과 처리
                    childLocationDisconnectViewModel.childLocationDisconnect()
                }
            }
        } else {
            Toast.makeText(requireContext(), "주변 안전 시설물이 없어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findNearestSafetyMarker(): MapMarkerInfoData? {
        val currentLocation = naverMap.locationOverlay.position ?: return null

        // 001(경찰서), 004(지킴이집) 타입만 대상으로 안전지점 찾기
        val candidates = markerMap.filterKeys { it == "001" || it == "004" }.values.flatten()

        return candidates.minByOrNull { marker ->
            val dx = currentLocation.longitude - marker.position.longitude
            val dy = currentLocation.latitude - marker.position.latitude
            dx * dx + dy * dy
        }?.let { marker ->
            mapMarkerViewModel.mapMarkerData.value?.result?.find { data ->
                data.lat?.toDoubleOrNull() == marker.position.latitude && data.lot?.toDoubleOrNull() == marker.position.longitude
            }
        }
    }


    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun startSafetyRoute(destinationData: MapMarkerInfoData) {
        destination = destinationData
        binding.tvDestination.text = "도착지: ${destinationData.address}"

        waypointAdapter.isRoutingStarted = true


        // 기존 waypoint 비우기
        waypoints.clear()
        waypointAdapter.notifyDataSetChanged()

        updateRoute()

        binding.btnRoute.visibility = View.GONE
        binding.btnAddWaypoint.visibility = View.GONE
        binding.btnRouteDesignation.visibility = View.GONE
        binding.btnRemoveDestination.visibility = View.GONE

        disableCategoryButtons()
        deactivateUnrelatedMarkers()
        restoreAllMarkers()
    }

    private fun restoreAllMarkers() {
        markerMap.values.flatten().forEach { marker ->
            marker.map = naverMap
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocationImmediately() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)

                UserStateData.setMyLatLng(userLatLng)

                naverMap.moveCamera(CameraUpdate.scrollTo(userLatLng))
            }
        }
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
                Glide.with(requireContext()).asBitmap().load(profileUrl).circleCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap, transition: Transition<in Bitmap>?
                        ) {
                            val resized = resource.scale(desiredSize, desiredSize, false)
                            val overlayImage = OverlayImage.fromBitmap(resized)

                            naverMap.locationOverlay.icon = overlayImage
                            naverMap.locationOverlay.isVisible = true
                            naverMap.locationOverlay.position = LatLng(
                                location.latitude, location.longitude
                            )
                            naverMap.moveCamera(
                                CameraUpdate.scrollTo(
                                    LatLng(
                                        location.latitude, location.longitude
                                    )
                                )
                            )
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // 필요 없다면 비워두기
                        }
                    })
            }
        }
    }

    private fun mapMarkers() {
        mapMarkerViewModel.mapMarker()

        mapMarkerViewModel.mapMarkerData.observe(viewLifecycleOwner) { mapMarkerData ->
            mapMarkerData?.let { showMarkers(it.result) }
        }
    }

    private fun showMarkers(markerList: List<MapMarkerInfoData>) {
        markerMap.clear()

        markerList.forEach { data ->
            val lat = data.lat?.toDoubleOrNull()
            val lot = data.lot?.toDoubleOrNull()
            val type = data.type

            if (lat == null || lot == null || type.isNullOrBlank()) {
                Timber.e("Invalid marker data: $data")
                return@forEach
            }

            val marker = Marker().apply {
                position = LatLng(lat, lot)
                icon = OverlayImage.fromResource(getMarkerIconRes(type))
                width = 88
                height = 88
                map = if (markerVisibleMap[type] == true) naverMap else null

                if (type == "001" || type == "004") {
                    captionText = data.name?.split(" ")?.last() ?: ""
                    captionColor = resources.getColor(R.color.black, null)
                    captionHaloColor = resources.getColor(android.R.color.transparent, null)
                }
            }

            marker.setOnClickListener {
                showMarkerDetail(data)
                true
            }

            markerMap.getOrPut(type) { mutableListOf() }.add(marker)

            if (type == "002") { // CCTV만 따로 관리
                cctvMarkerMap[LatLng(lat, lot)] = marker
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showMarkerDetail(markerData: MapMarkerInfoData) {
        selectedMarkerData = markerData

        // 1. 이전에 선택된 마커 복구
        selectedMarker?.let { previousMarker ->
            selectedMarkerType?.let { prevType ->
                previousMarker.icon = OverlayImage.fromResource(getMarkerIconRes(prevType))
                previousMarker.width = 88
                previousMarker.height = 88
                previousMarker.map = null
                previousMarker.map = naverMap
            }
        }

        // 2. 현재 클릭한 마커 저장
        selectedMarker = if (markerData.type == "002") {
            // CCTV는 빠르게 HashMap 조회
            val lat = markerData.lat?.toDoubleOrNull()
            val lot = markerData.lot?.toDoubleOrNull()
            if (lat != null && lot != null) {
                cctvMarkerMap[LatLng(lat, lot)]
            } else {
                null
            }
        } else {
            // 나머지는 기존 방식
            markerMap[markerData.type]?.find {
                it.position.latitude == markerData.lat?.toDoubleOrNull() && it.position.longitude == markerData.lot?.toDoubleOrNull()
            }
        }
        selectedMarkerType = markerData.type


        // 3. 선택된 마커를 destination 스타일로 변경
        selectedMarker?.let { marker ->
            marker.icon = OverlayImage.fromResource(getSelectedMarkerIconRes(markerData.type))
            marker.width = 88 * 2 // 클릭된 마커 크기 2배
            marker.height = 88 * 2
            marker.map = null
            marker.map = naverMap
        }

        // 4. BottomSheet 동작
        binding.optionBottomSheet.visibility = View.GONE
        optionBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.markerInfoBottomSheet.visibility = View.VISIBLE
        markerInfoBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.textMarkerName.text = markerData.name ?: when (markerData.type) {
            "001" -> "경찰서"
            "002" -> "CCTV"
            "003" -> "안전 시설물"
            else -> "지킴이집"
        }
        binding.textMarkerAddress.text = markerData.address ?: "주소 없음"
        binding.textMarkerDistance.text = ""

        if (markerData.image != null && markerData.image != "") {
            binding.imageMarker.visibility = View.VISIBLE
            Glide.with(this).load(markerData.image).error(R.drawable.ic_default_profile)
                .into(binding.imageMarker)
        } else {
            binding.imageMarker.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        childLocationViewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        childLocationViewModel.childLocationData.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (!isFirstLocationSent) {
                    isFirstLocationSent = true
                    Toast.makeText(requireContext(), "위치 전송했어요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ✅ 연결 종료 성공 시 마커 제거
        childLocationDisconnectViewModel.childLocationDisconnectData.observe(viewLifecycleOwner) { result ->
            result?.let {
                Timber.d("🚨 Disconnect triggered, childId = $currentChildId")
            }
        }

        // ✅ 연결 종료 실패 시 메시지 표시
        childLocationDisconnectViewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveCameraToRoute() {
        val boundsBuilder = LatLngBounds.Builder()
        naverMap.locationOverlay.position.let { boundsBuilder.include(it) }
        waypoints.forEach { waypoint ->
            waypoint.lat?.toDoubleOrNull()?.let { lat ->
                waypoint.lot?.toDoubleOrNull()?.let { lot ->
                    boundsBuilder.include(LatLng(lat, lot))
                }
            }
        }
        destination?.let { dest ->
            dest.lat?.toDoubleOrNull()?.let { lat ->
                dest.lot?.toDoubleOrNull()?.let { lot ->
                    boundsBuilder.include(LatLng(lat, lot))
                }
            }
        }

        val bounds = boundsBuilder.build()

        // 일단 bounds 맞춰서 카메라 이동
        val cameraUpdate = CameraUpdate.fitBounds(bounds, 50).animate(CameraAnimation.Linear, 1000)

        naverMap.moveCamera(cameraUpdate)

        // 💥 그리고 살짝 줌아웃 추가
        Handler(Looper.getMainLooper()).postDelayed({
            val currentZoom = naverMap.cameraPosition.zoom
            val zoomOutUpdate =
                CameraUpdate.zoomTo(currentZoom - 1.5).animate(CameraAnimation.Linear, 1000)
            naverMap.moveCamera(zoomOutUpdate)
        }, 1200) // bounds 맞춘 후 1.2초 뒤에 줌아웃
    }


    private fun getMarkerIconRes(type: String): Int = when (type) {
        "001" -> R.drawable.ic_police
        "002" -> R.drawable.ic_cctv
        "003" -> R.drawable.ic_safety_light
        else -> R.drawable.ic_safety_facility
    }

    private fun getSelectedMarkerIconRes(type: String?): Int = when (type) {
        "001" -> R.drawable.ic_police_destination
        "002" -> R.drawable.ic_cctv_destination
        "003" -> R.drawable.ic_safety_light_destination
        else -> R.drawable.ic_safety_facility_destination
    }

    private fun getOffMarkerIconRes(type: String): Int = when (type) {
        "001" -> R.drawable.ic_off_police
        "002" -> R.drawable.ic_off_cctv
        "003" -> R.drawable.ic_off_safety_light
        else -> R.drawable.ic_off_safety_facility
    }

    private fun updateRoute() {
        val start = naverMap.locationOverlay.position
        if (start == null) {
            requestUserLocationThenRoute()
            return
        }

        val goal = destination

        if (goal == null || goal.lat.isNullOrBlank() || goal.lot.isNullOrBlank()) {
            Toast.makeText(requireContext(), "도착지 정보가 없어요", Toast.LENGTH_SHORT).show()
            return
        }

        val startParam = "${start.longitude},${start.latitude}"
        val goalParam = "${goal.lot},${goal.lat}"
        val waypointParam = waypoints.joinToString("|") { "${it.lot},${it.lat}" }

        val clientId = BuildConfig.NAVER_CLIENT_ID
        val clientSecret = BuildConfig.NAVER_CLIENT_SECRET

        lifecycleScope.launch {
            try {
                val response = directionsService.getRoutePath(
                    start = startParam,
                    goal = goalParam,
                    waypoints = waypointParam,
                    clientId = clientId,
                    clientSecret = clientSecret
                )

                val path = response.route.traoptimal.first().path
                val coords = path.map { LatLng(it[1], it[0]) }
                if (coords.isEmpty()) {
                    Toast.makeText(requireContext(), "경로가 없어요", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                currentPolyline?.map = null

                val pathOverlay = PathOverlay().apply {
                    this.coords = coords
                    color = resources.getColor(android.R.color.transparent, null)
                    passedColor = resources.getColor(R.color.orange_ffaa62, null)
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

                moveCameraToRoute()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(), "경로 계산 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT
                ).show()
            }
        }

        Timber.d("START: $startParam, GOAL: $goalParam, WAYPOINTS: $waypointParam")
    }

    @SuppressLint("MissingPermission")
    private fun requestUserLocationThenRoute() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                UserStateData.setMyLatLng(userLatLng)

                Timber.d("위치 재획득 성공: $userLatLng")
                updateRoute() // ✅ 다시 경로 계산 시도
            } else {
                Toast.makeText(requireContext(), "현재 위치를 가져올 수 없어요", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "위치 요청 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}