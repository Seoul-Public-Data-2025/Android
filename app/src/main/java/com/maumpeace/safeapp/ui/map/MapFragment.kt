package com.maumpeace.safeapp.ui.map

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.maumpeace.safeapp.databinding.FragmentMapBinding

/**
 * 🗺 MapFragment - 지도 화면
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun triggerSafetyFeature() {
        // 예: 다이얼로그 호출, 맵 위에 효과 표시 등
        Log.d("MapFragment", "triggerSafetyFeature 호출됨")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}