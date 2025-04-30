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
import com.maumpeace.safeapp.viewModel.AlarmViewModel
import com.maumpeace.safeapp.viewModel.LogoutViewModel
import com.maumpeace.safeapp.viewModel.SecessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val logoutViewModel: LogoutViewModel by viewModels()
    private val secessionViewModel: SecessionViewModel by viewModels()
    private val alarmViewModel: AlarmViewModel by viewModels()

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

        setupClickListeners()
        setupAlarmSwitch()
    }

    private fun setupClickListeners() {
        binding.llNoti.setOnClickListener {
            startActivity(Intent(requireContext(), WebActivity::class.java).putExtra("type", 1))
        }
        binding.llHelp.setOnClickListener {
            startActivity(Intent(requireContext(), WebActivity::class.java).putExtra("type", 2))
        }
        binding.llPolicy.setOnClickListener {
            startActivity(Intent(requireContext(), WebActivity::class.java).putExtra("type", 3))
        }
        binding.llLogout.setOnClickListener { showExitConfirmDialog() }
        binding.tvSecession.setOnClickListener { showSecessionConfirmDialog() }
    }

    private fun setupAlarmSwitch() {
        // 초기 스위치 상태 로컬 저장값 기반
        val isAlarmEnabled = TokenManager.getAlarmPermission(requireContext()) ?: true
        binding.switchAlarm.isChecked = isAlarmEnabled

        binding.switchAlarm.setOnCheckedChangeListener { buttonView, isChecked ->
            val hashedPhoneNumber = TokenManager.getHashedPhoneNumber(requireContext())
            if (hashedPhoneNumber.isNullOrBlank()) {
                Toast.makeText(requireContext(), "전화번호 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                buttonView.isChecked = !isChecked // 롤백
                return@setOnCheckedChangeListener
            }

            // 서버에 알람 설정 요청
            alarmViewModel.alarm(isChecked, hashedPhoneNumber)

            alarmViewModel.alarmData.observe(viewLifecycleOwner) { alarmData ->
                alarmData?.result?.notification?.let { notificationEnabled ->
                    TokenManager.saveAlarmPermission(requireContext(), notificationEnabled)
                    binding.switchAlarm.isChecked = notificationEnabled
                }
            }

            alarmViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                error?.let {
                    Toast.makeText(requireContext(), "알람 설정 실패: $it", Toast.LENGTH_SHORT).show()
                    buttonView.isChecked = !isChecked // 실패했으면 롤백
                }
            }
        }
    }

    private fun showExitConfirmDialog() {
        val logoutDialog = parentFragmentManager.findFragmentByTag("LogoutConfirmDialog")
        if (logoutDialog == null || !logoutDialog.isVisible) {
            LogoutConfirmBottomSheet { performLogout() }
                .show(parentFragmentManager, "LogoutConfirmDialog")
        }
    }

    private fun showSecessionConfirmDialog() {
        val secessionDialog = parentFragmentManager.findFragmentByTag("SecessionConfirmDialog")
        if (secessionDialog == null || !secessionDialog.isVisible) {
            SecessionConfirmBottomSheet { performSecession() }
                .show(parentFragmentManager, "SecessionConfirmDialog")
        }
    }

    private fun performAlarm(notification: Boolean, hashedPhoneNumber: String) {
        alarmViewModel.alarm(notification, hashedPhoneNumber)
        alarmViewModel.alarmData.observe(viewLifecycleOwner) { alarmData ->
            alarmData?.let {
            }
        }

        alarmViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Timber.tag("error: ").e(it) }
        }
    }

    private fun performSecession() {
        secessionViewModel.secession()
        secessionViewModel.secessionData.observe(viewLifecycleOwner) { secessionData ->
            secessionData?.let {
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Toast.makeText(requireContext(), "회원탈퇴 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } else {
                        UserApiClient.instance.unlink {
                            requireContext().getSharedPreferences(
                                "auth", AppCompatActivity.MODE_PRIVATE
                            ).edit { clear() }

                            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            Toast.makeText(requireContext(), "서비스를 이용해주셔서 감사합니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        secessionViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Timber.tag("error: ").e(it) }
        }
    }

    private fun performLogout() {
        val refreshToken = TokenManager.getRefreshToken(requireContext())
        logoutViewModel.logout(refreshToken.orEmpty())
        logoutViewModel.logoutData.observe(viewLifecycleOwner) { logoutData ->
            logoutData?.let {
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Toast.makeText(requireContext(), "로그아웃 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } else {
                        UserApiClient.instance.unlink {
                            requireContext().getSharedPreferences(
                                "auth", AppCompatActivity.MODE_PRIVATE
                            ).edit { clear() }

                            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                    }
                }
            }
        }

        logoutViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Timber.tag("error: ").e(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}