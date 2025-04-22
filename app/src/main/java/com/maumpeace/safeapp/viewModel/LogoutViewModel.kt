package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.LogoutData
import com.maumpeace.safeapp.repository.LogoutRepository
import com.maumpeace.safeapp.util.GlobalApplication
import com.maumpeace.safeapp.util.HttpErrorHandler
import com.maumpeace.safeapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val logoutRepository: LogoutRepository
) : ViewModel() {

    // 로그인 결과
    private val _logoutData = MutableLiveData<LogoutData?>()
    val logoutData: LiveData<LogoutData?> get() = _logoutData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    /**
     * 🔐 카카오 로그인 처리
     */
    fun logout(refreshToken: String) {
        _logoutData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = logoutRepository.logout(refreshToken)

                if (result.success) {
                    TokenManager.clearAllTokens(GlobalApplication.INSTANCE)
                    _logoutData.postValue(result)
                } else {
                    _errorMessage.postValue("로그아웃 실패: 서버 응답 실패")
                }

            } catch (e: HttpException) {
                val message = HttpErrorHandler.parseErrorMessage(e)
                _errorMessage.postValue(message)
            } catch (e: Exception) {
                _errorMessage.postValue("예기치 않은 오류: ${e.localizedMessage}")
            }
        }
    }
}