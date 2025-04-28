package com.maumpeace.safeapp.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.kakao.sdk.user.UserApiClient
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.FragmentSettingsBinding
import com.maumpeace.safeapp.ui.dialog.LogoutConfirmBottomSheet
import com.maumpeace.safeapp.ui.dialog.SecessionConfirmBottomSheet
import com.maumpeace.safeapp.ui.login.LoginActivity
import com.maumpeace.safeapp.util.TokenManager
import com.maumpeace.safeapp.viewModel.LogoutViewModel
import com.maumpeace.safeapp.viewModel.SecessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * ⚙️ SettingsFragment - 설정 화면
 */

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val logoutViewModel: LogoutViewModel by viewModels()
    private val secessionViewModel: SecessionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profile = TokenManager.getProfile(requireContext())
        val nickname = TokenManager.getNickname(requireContext())

        Glide.with(this).load(profile).error(R.drawable.ic_default_profile).into(binding.ivProfile)
        binding.tvNickName.text = nickname ?: "마음이"

        //공지사항
        binding.llNoti.setOnClickListener {
            startActivity(Intent(requireContext(), WebActivity::class.java).putExtra("type", 1))
        }

        //도움말
        binding.llHelp.setOnClickListener {
            startActivity(Intent(requireContext(), WebActivity::class.java).putExtra("type", 2))
        }

        //개인정보처리방침
        binding.llPolicy.setOnClickListener {
            startActivity(Intent(requireContext(), WebActivity::class.java).putExtra("type", 3))
        }

        // 🔐 로그아웃 클릭 리스너 설정
        binding.llLogout.setOnClickListener {
            showExitConfirmDialog()
        }

        // 회원탈퇴
        binding.tvSecession.setOnClickListener {
            showSecessionConfirmDialog()
        }
    }

    private fun showExitConfirmDialog() {
        val logoutDialog = parentFragmentManager.findFragmentByTag("LogoutConfirmDialog")
        if (logoutDialog != null && logoutDialog.isVisible) {
            // 이미 팝업이 떠있으면 새로 띄우지 않는다
            return
        }

        val dialog = LogoutConfirmBottomSheet {
            performLogout()
        }
        dialog.show(parentFragmentManager, "LogoutConfirmDialog")
    }

    private fun showSecessionConfirmDialog() {
        val secessionDialog = parentFragmentManager.findFragmentByTag("SecessionConfirmDialog")
        if (secessionDialog != null && secessionDialog.isVisible) {
            // 이미 팝업이 떠있으면 새로 띄우지 않는다
            return
        }

        val dialog = SecessionConfirmBottomSheet {
            performSecession()
        }
        dialog.show(parentFragmentManager, "SecessionConfirmDialog")
    }

    private fun performSecession() {
        secessionViewModel.secession()
        secessionViewModel.secessionData.observe(viewLifecycleOwner) { secessionData ->
            // 회원탈퇴 성공 처리
            secessionData?.let {
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Toast.makeText(
                            requireContext(),
                            "회원탈퇴 실패: ${error.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        UserApiClient.instance.unlink {
                            // 🔄 SharedPreferences 초기화
                            requireContext().getSharedPreferences(
                                "auth", AppCompatActivity.MODE_PRIVATE
                            ).edit { clear() }

                            // LoginActivity로 이동
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            Toast.makeText(requireContext(), "서비스를 이용해주셔서 감사합니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        secessionViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Timber.tag("error: ").e(it)
            }
        }
    }

    private fun performLogout() {
        val refreshToken = TokenManager.getRefreshToken(requireContext())
        logoutViewModel.logout(refreshToken.toString())
        logoutViewModel.logoutData.observe(viewLifecycleOwner) { logoutData ->
            // 로그인 성공 처리
            logoutData?.let {
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Toast.makeText(
                            requireContext(),
                            "로그아웃 실패: ${error.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        UserApiClient.instance.unlink {
                            // 🔄 SharedPreferences 초기화
                            requireContext().getSharedPreferences(
                                "auth", AppCompatActivity.MODE_PRIVATE
                            ).edit { clear() }

                            // 🚪 LoginActivity로 이동
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    }
                }
            }
        }

        logoutViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Timber.tag("error: ").e(it)
//                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}