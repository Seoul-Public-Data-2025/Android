package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.AlarmData
import com.maumpeace.safeapp.repository.AlarmRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {
    private val _alarmData = MutableLiveData<AlarmData?>()
    val alarmData: LiveData<AlarmData?> get() = _alarmData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun alarm(notification: Boolean, hashedPhoneNumber: String) {
        _alarmData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = alarmRepository.alarm(notification, hashedPhoneNumber)

                if (result.success) {
                    _alarmData.postValue(result)
                } else {
                    _errorMessage.postValue("알람 설정 실패: 서버 응답 실패")
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