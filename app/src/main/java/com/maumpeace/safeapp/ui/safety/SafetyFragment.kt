package com.maumpeace.safeapp.ui.safety

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.maumpeace.safeapp.databinding.FragmentSafetyBinding

/**
 * ⚪ SafetyFragment - 안심 토글 화면
 */
class SafetyFragment : Fragment() {

    private var _binding: FragmentSafetyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSafetyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}