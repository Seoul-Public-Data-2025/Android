package com.maumpeace.safeapp.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.DialogExitConfirmBinding

class ExitConfirmBottomSheet(
    private val onExitConfirmed: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogExitConfirmBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ 스타일 적용: 배경 투명
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogExitConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnExit.setOnClickListener {
            dismiss()
            onExitConfirmed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}