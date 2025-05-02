package com.maumpeace.safeapp.ui.role

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.maumpeace.safeapp.databinding.FragmentChildBinding
import com.maumpeace.safeapp.viewModel.RelationChildApproveViewModel
import com.maumpeace.safeapp.viewModel.RelationChildListViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 자녀 목록을 표시하는 Fragment
 */
@AndroidEntryPoint
class ChildFragment : Fragment() {

    private var _binding: FragmentChildBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RelationChildListViewModel by viewModels()
    private val relationChildApproveViewModel: RelationChildApproveViewModel by viewModels()
    private lateinit var childAdapter: ChildAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        observeApproveRelation()
        viewModel.relationChildList()
    }

    override fun onResume() {
        super.onResume()
        binding.tvNotChildList.visibility = View.GONE
        viewModel.relationChildList()
    }

    private fun setupRecyclerView() {
        childAdapter = ChildAdapter(onAccept = { child ->
            relationChildApproveViewModel.relationChildApprove(child.id)
        }, onReject = { child ->
            Toast.makeText(requireContext(), "${child.name} 거절 요청", Toast.LENGTH_SHORT).show()

            // 리스트에서 제거
            val currentList = childAdapter.currentList.toMutableList()
            currentList.remove(child)
            childAdapter.submitList(currentList)
            if (currentList.isEmpty()) {
                binding.tvNotChildList.visibility = View.VISIBLE
            }
        })
        binding.rvChildList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = childAdapter
        }
    }

    private fun observeApproveRelation() {
        relationChildApproveViewModel.relationChildApproveData.observe(viewLifecycleOwner) { result ->
            result?.let {
                Toast.makeText(requireContext(), "자녀 요청을 수락했습니다.", Toast.LENGTH_SHORT).show()
                viewModel.relationChildList() // 목록 갱신
            }
        }

        relationChildApproveViewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), "오류 발생: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun observeViewModel() {
        viewModel.relationChildListData.observe(viewLifecycleOwner) { data ->
            if (data?.success == true) {
                val list = data.result.relations
                childAdapter.submitList(list)

                if (list.isEmpty()) {
                    binding.tvNotChildList.visibility = View.VISIBLE
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                binding.tvNotChildList.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}