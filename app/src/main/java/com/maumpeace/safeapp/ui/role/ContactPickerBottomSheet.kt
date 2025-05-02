package com.maumpeace.safeapp.ui.role

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.DialogContactPickerBinding

class ContactPickerBottomSheet(
    private val contactList: List<Pair<String, String>>,
    private val onRegister: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: ContactAdapter
    private var _binding: DialogContactPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogContactPickerBinding.inflate(inflater, container, false)

        adapter = ContactAdapter { name, phone ->
            onRegister(name, phone)
            dismiss()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        adapter.submitList(contactList)

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet =
                    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.setBackgroundResource(R.drawable.bg_bottom_sheet_radius)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
