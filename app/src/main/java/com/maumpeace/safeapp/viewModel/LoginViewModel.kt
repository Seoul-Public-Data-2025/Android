package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.repository.LoginRepository
import com.maumpeace.safeapp.util.GlobalApplication
import com.maumpeace.safeapp.util.HttpErrorHandler
import com.maumpeace.safeapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

/**
 * ✅ LoginViewModel
 * - Repository와 UI 사이를 연결하는 역할
 * - 로그인 성공 여부 및 오류 메시지를 관리
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {

    // 로그인 결과
    private val _loginData = MutableLiveData<LoginData?>()
    val loginData: LiveData<LoginData?> get() = _loginData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    /**
     * 🔐 카카오 로그인 처리
     */
    fun loginWithKakao(email: String, kakaoAccessToken: String) {
        _loginData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = loginRepository.loginWithKakao(email, kakaoAccessToken)

                if (result.success) {
                    TokenManager.saveTokens(
                        context = GlobalApplication.INSTANCE.applicationContext,
                        accessToken = result.result.accessToken,
                        refreshToken = result.result.refreshToken
                    )
                    _loginData.postValue(result)
                } else {
                    _errorMessage.postValue("로그인 실패: 서버 응답 실패")
                }

            } catch (e: HttpException) {
                val message = HttpErrorHandler.parseErrorMessage(e)
                _errorMessage.postValue(message)
            } catch (e: Exception) {
                _errorMessage.postValue("예기치 않은 오류: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 🔓 로그아웃 처리
     */
    fun logout(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = loginRepository.logout()
            if (success) {
                TokenManager.clearAllTokens(GlobalApplication.INSTANCE)
            }
            onComplete(success)
        }
    }
}