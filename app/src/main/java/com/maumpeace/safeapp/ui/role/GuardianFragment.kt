package com.maumpeace.safeapp.ui.role

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.maumpeace.safeapp.databinding.FragmentGuardianBinding
import com.maumpeace.safeapp.model.RelationGuardianInfoData
import com.maumpeace.safeapp.ui.dialog.RelationGuardianDeleteBottomSheet
import com.maumpeace.safeapp.ui.dialog.RelationResendBottomSheet
import com.maumpeace.safeapp.viewModel.CreateRelationViewModel
import com.maumpeace.safeapp.viewModel.RelationGuardianDeleteViewModel
import com.maumpeace.safeapp.viewModel.RelationGuardianListViewModel
import com.maumpeace.safeapp.viewModel.RelationResendViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.security.MessageDigest

/**
 * 보호자 목록을 표시하고, 연락처에서 보호자를 추가하는 Fragment
 */
@AndroidEntryPoint
class GuardianFragment : Fragment() {

    private var _binding: FragmentGuardianBinding? = null
    private val binding get() = _binding!!

    private val relationGuardianListViewModel: RelationGuardianListViewModel by viewModels()
    private val createRelationViewModel: CreateRelationViewModel by viewModels()
    private val relationGuardianDeleteViewModel: RelationGuardianDeleteViewModel by viewModels()
    private val relationResendViewModel: RelationResendViewModel by viewModels()
    private lateinit var guardianAdapter: GuardianAdapter
    private var lastDeletedGuardian: RelationGuardianInfoData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuardianBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeRelationList()
        observeCreateRelation()
        setupAddGuardianButton()
        observeDeleteRelation()
        observeRelationResend()

        relationGuardianListViewModel.relationGuardianList()
    }

    private fun observeRelationResend() {
        relationResendViewModel.relationResendData.observe(viewLifecycleOwner) { result ->
            result?.let {
                Toast.makeText(requireContext(), "재전송했어요", Toast.LENGTH_SHORT).show()
            }
        }

        relationResendViewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), "오류 발생: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvNotGuardianList.visibility = View.GONE
        relationGuardianListViewModel.relationGuardianList()
    }

    private fun observeCreateRelation() {
        createRelationViewModel.createRelationData.observe(viewLifecycleOwner) { result ->
            result?.let {
                Toast.makeText(requireContext(), "보호자가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                relationGuardianListViewModel.relationGuardianList()
                binding.tvNotGuardianList.visibility = View.GONE
            }
        }

        createRelationViewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        guardianAdapter = GuardianAdapter(onDeleteClick = { data ->
            showRelationGuardianDeleteDialog(data.id, data.name)
            lastDeletedGuardian = data
        }, onResendClick = { id, name ->
            showRelationResendDialog(id, name)
        })

        binding.rvGuardianList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = guardianAdapter
        }

    }

    private fun showRelationGuardianDeleteDialog(id: Int, name: String) {
        val relationGuardianDeleteDialog =
            parentFragmentManager.findFragmentByTag("RelationGuardianDeleteDialog")
        if (relationGuardianDeleteDialog == null || !relationGuardianDeleteDialog.isVisible) {
            val bottomSheet = RelationGuardianDeleteBottomSheet(
                name = name, onRelationGuardianDeleteConfirmed = {
                    relationGuardianDeleteViewModel.relationGuardianDelete(id)
                })
            bottomSheet.show(parentFragmentManager, "RelationGuardianDeleteDialog")
        }
    }

    private fun showRelationResendDialog(id: Int, name: String) {
        val relationResendDialog = parentFragmentManager.findFragmentByTag("RelationResendDialog")
        if (relationResendDialog == null || !relationResendDialog.isVisible) {
            val bottomSheet = RelationResendBottomSheet(
                name = name, onRelationResendConfirmed = {
                    relationResendViewModel.relationResend(id)
                })
            bottomSheet.show(parentFragmentManager, "RelationResendDialog")
        }
    }

    private fun observeDeleteRelation() {
        relationGuardianDeleteViewModel.relationGuardianDeleteData.observe(viewLifecycleOwner) { result ->
            result?.let {
                lastDeletedGuardian?.let { deleted ->
                    val isEmpty = guardianAdapter.removeItem(deleted)
                    if (isEmpty) {
                        binding.tvNotGuardianList.visibility = View.VISIBLE
                    }
                }
                Toast.makeText(requireContext(), "보호자 관계가 해지되었어요", Toast.LENGTH_SHORT).show()
            }
        }

        relationGuardianDeleteViewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), "오류 발생: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeRelationList() {
        relationGuardianListViewModel.relationGuardianListData.observe(viewLifecycleOwner) { data ->
            if (data?.success == true) {
                val list = data.result.relations
                guardianAdapter.submitList(list)
                if (list.isEmpty()) {
                    binding.tvNotGuardianList.visibility = View.VISIBLE
                }
            }
        }

        relationGuardianListViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                binding.tvNotGuardianList.visibility = View.GONE
            }
        }
    }

    private fun showContactPickerBottomSheet() {
        val contacts = getContacts(requireContext())

        val sheet = ContactPickerBottomSheet(contacts) { name, phone ->
            try {
                val hashedPhone = hashPhoneNumber(phone)
                createRelationViewModel.createRelation(
                    parentPhoneNumber = hashedPhone, parentName = name
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "전화번호 처리 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }

        sheet.show(parentFragmentManager, "ContactPicker")
    }

    private fun setupAddGuardianButton() {
        binding.llAddGuardian.setOnClickListener {
            when {
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showContactPickerBottomSheet()
                }

                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACT_PERMISSION
                    )
                }
            }
        }
    }

    private fun getContacts(context: Context): List<Pair<String, String>> {
        val contactList = mutableListOf<Pair<String, String>>()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ), null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val number = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                contactList.add(Pair(name, number))
            }
        }

        return contactList
    }

    private fun hashPhoneNumber(phone: String): String {
        val clean = phone.replace("\\s".toRegex(), "").replace("-", "").replace("+82", "0")
            .filter { it.isDigit() }

        if (clean.isBlank()) throw IllegalArgumentException("Invalid phone number")

        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(clean.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CONTACT_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showContactPickerBottomSheet()
        } else {
            Toast.makeText(requireContext(), "연락처 접근 권한이 필요해요", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CONTACT_PERMISSION = 1001
    }
}