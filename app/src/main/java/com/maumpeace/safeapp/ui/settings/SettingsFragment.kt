package com.maumpeace.safeapp.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.FragmentSettingsBinding
import com.maumpeace.safeapp.util.TokenManager
import androidx.core.content.edit
import com.maumpeace.safeapp.ui.login.LoginActivity

/**
 * ⚙️ SettingsFragment - 설정 화면
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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

        Glide.with(this).load(profile)
            .error(R.drawable.ic_default_profile).into(binding.ivProfile)
        binding.tvNickName.text = nickname?:"마음이"

        // 🔐 로그아웃 클릭 리스너 설정
        binding.llLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        com.kakao.sdk.user.UserApiClient.instance.logout { error ->
            if (error != null) {
                Toast.makeText(requireContext(), "로그아웃 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            } else {
                // 🔄 SharedPreferences 초기화
                requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE).edit() { clear() }

                // 🔐 앱 내 토큰도 초기화
                TokenManager.clearAllTokens(requireContext())

                // 🚪 LoginActivity로 이동
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}