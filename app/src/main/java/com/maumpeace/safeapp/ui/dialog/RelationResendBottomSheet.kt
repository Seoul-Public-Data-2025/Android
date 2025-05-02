package com.maumpeace.safeapp.ui.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.DialogRelationResendConfirmBinding

class RelationResendBottomSheet(
    private val onRelationResendConfirmed: () -> Unit,
    private val name: String,
) : BottomSheetDialogFragment() {

    private var _binding: DialogRelationResendConfirmBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogRelationResendConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textTitle.text = "보호자(${name}) 재전송하시겠습니까?"
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnResend.setOnClickListener {
            dismiss()
            onRelationResendConfirmed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}