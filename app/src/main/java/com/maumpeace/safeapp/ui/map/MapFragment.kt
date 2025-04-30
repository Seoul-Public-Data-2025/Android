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
import timber.log.Timber
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

    private var isRoutingStarted = false
    private var selectedMarkerData: MapMarkerInfoData? = null

    private var isUserTurnedOffCctv = false
    private var isUserTurnedOffSafety = false
    private var isGuiding = false
    private val cctvMarkerMap = mutableMapOf<LatLng, Marker>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationSource = FusedLocationSource(this, 1000)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        setupBottomSheets()
        setupWaypointRecyclerView()
        setupCategoryClickListeners()

        binding.optionBottomSheet.setOnTouchListener { _, _ -> true }
        binding.markerInfoBottomSheet.setOnTouchListener { _, _ -> true }

        binding.btnCancelRoute.setOnClickListener { clearRoute() }

        binding.btnAddWaypoint.setOnClickListener {
            if (waypoints.size >= 3) {
                Toast.makeText(requireContext(), "경유지는 최대 3개까지 추가할 수 있습니다.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            selectedMarkerData?.let { waypoint ->
                waypoints.add(waypoint)
                waypointAdapter.notifyItemInserted(waypoints.size - 1)

                // 🆕 경유지 추가되었으면 RecyclerView 보이기
                binding.llRecyclerWaypoint.visibility = View.VISIBLE
            }
        }

        binding.btnRouteDesignation.setOnClickListener {
            selectedMarkerData?.let { data ->
                destination = data
                binding.tvDestination.text = "도착지: ${data.address}"

                // 🆕 도착지 추가되었으면 RecyclerView 보이기
                binding.llRecyclerWaypoint.visibility = View.VISIBLE
                binding.btnRemoveDestination.visibility = View.VISIBLE
            }
        }

        binding.btnRemoveDestination.setOnClickListener {
            destination = null
            binding.tvDestination.text = "도착지를 지정해주세요"
            binding.btnRemoveDestination.visibility = View.GONE
        }

        binding.btnRoute.setOnClickListener {
            if (destination == null) {
                Toast.makeText(requireContext(), "도착지를 지정해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isRoutingStarted = true
            updateRoute()

            // 버튼 숨기기
            binding.btnRoute.visibility = View.GONE
            binding.btnAddWaypoint.visibility = View.GONE
            binding.btnRouteDesignation.visibility = View.GONE
            binding.btnRemoveDestination.visibility = View.GONE

            waypointAdapter.notifyDataSetChanged()

            // 카테고리 버튼 비활성화
            disableCategoryButtons()

            // 경로에 맞게 줌
            moveCameraToRoute()

            // 연관 없는 마커 숨기기
            deactivateUnrelatedMarkers()
        }

        binding.btnCancelRoute.setOnClickListener {
            clearRoute()
            isRoutingStarted = false

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
        }
    }

    private fun restoreMarkersAfterRouteCancel() {
        val zoom = naverMap.cameraPosition.zoom
        markerMap.forEach { (type, markers) ->
            val shouldShow = when (type) {
                "002", "003" -> zoom >= 14.5
                else -> true
            }
            markers.forEach { marker ->
                marker.map = if (shouldShow) naverMap else null
            }
        }
    }

    private fun disableCategoryButtons() {
        binding.llPolice.isEnabled = false
        binding.llCctv.isEnabled = false
        binding.llSafetyLight.isEnabled = false
        binding.llSafetyFacility.isEnabled = false
    }

    private fun enableCategoryButtons() {
        binding.llPolice.isEnabled = true
        binding.llCctv.isEnabled = true
        binding.llSafetyLight.isEnabled = true
        binding.llSafetyFacility.isEnabled = true
    }


    private fun deactivateUnrelatedMarkers() {
        val includedLatLngs = mutableListOf<LatLng>()
        UserStateData.getMyLatLng()?.let { includedLatLngs.add(it) }
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
            isRoutingStarted = isRoutingStarted // 🆕 현재 길찾기 상태 넘기기
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
            Toast.makeText(requireContext(), "지도를 더 확대하면 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
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
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
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
            // ✨ 길찾기 중에는 마커 상태 변경 금지
            if (isRoutingStarted) return@addOnCameraChangeListener

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
                Toast.makeText(requireContext(), "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    fun triggerSafetyFeature() {
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
            isRoutingStarted = false

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
                // ✨ '가장 가까운 안전 시설물'을 찾아서 길찾기 시작
                val nearestMarker = findNearestSafetyMarker()
                if (nearestMarker != null) {
                    startSafetyRoute(nearestMarker)
                } else {
                    Toast.makeText(requireContext(), "근처에 안전 시설물이 없습니다.", Toast.LENGTH_SHORT).show()
                }

                textEmergencyMessage.text = "안심경로 안내를 취소할까요?"
                btnConfirm.text = "안내 취소"
                isGuiding = true
            } else {
                // ✨ 길찾기 취소
                clearRoute()
                isRoutingStarted = false

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
            }
        }
    }

    private fun findNearestSafetyMarker(): MapMarkerInfoData? {
        val currentLocation = UserStateData.getMyLatLng() ?: return null

        // 001(경찰서), 004(지킴이집) 타입만 대상으로 안전지점 찾기
        val candidates = markerMap.filterKeys { it == "001" || it == "004" }.values.flatten()

        return candidates.minByOrNull { marker ->
            val dx = currentLocation.longitude - marker.position.longitude
            val dy = currentLocation.latitude - marker.position.latitude
            dx * dx + dy * dy // 거리 제곱 (루트 안 씌워도 최소값 찾는데 문제없음)
        }?.let { marker ->
            // 마커를 통해 MapMarkerInfoData 찾기
            mapMarkerViewModel.mapMarkerData.value?.result?.find { data ->
                data.lat?.toDoubleOrNull() == marker.position.latitude && data.lot?.toDoubleOrNull() == marker.position.longitude
            }
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun startSafetyRoute(destinationData: MapMarkerInfoData) {
        destination = destinationData
        binding.tvDestination.text = "도착지: ${destinationData.address}"
        isRoutingStarted = true

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

                // 바로 카메라 이동
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
                it.position.latitude == markerData.lat?.toDoubleOrNull() &&
                        it.position.longitude == markerData.lot?.toDoubleOrNull()
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

        if(markerData.image != null && markerData.image != "") {
            binding.imageMarker.visibility = View.VISIBLE
            Glide.with(this).load(markerData.image).error(R.drawable.ic_default_profile).into(binding.imageMarker)
        } else {
            binding.imageMarker.visibility = View.GONE
        }
    }

    private fun moveCameraToRoute() {
        val boundsBuilder = LatLngBounds.Builder()

        UserStateData.getMyLatLng().let { boundsBuilder.include(it) }
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
        val currentLocation = UserStateData.getMyLatLng()
        val start = UserStateData.getMyLatLng()
        val goal = destination ?: return

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

                moveCameraToRoute()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(), "경로 계산 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT
                ).show()
            }
        }

        Timber.d("START: $startParam, GOAL: $goalParam, WAYPOINTS: $waypointParam")
    }
}