package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.ChildLocationDisconnectData
import com.maumpeace.safeapp.repository.ChildLocationDisconnectRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ChildLocationDisconnectViewModel  @Inject constructor(
    private val childLocationDisconnectRepository: ChildLocationDisconnectRepository
) : ViewModel() {
    private val _childLocationDisconnectData = MutableLiveData<ChildLocationDisconnectData?>()
    val childLocationDisconnectData: LiveData<ChildLocationDisconnectData?> get() = _childLocationDisconnectData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun childLocationDisconnect() {
        _childLocationDisconnectData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = childLocationDisconnectRepository.childLocationDisconnect()

                if (result.success) {
                    _childLocationDisconnectData.postValue(result)
                } else {
                    _errorMessage.postValue("위치 전송 종료 실패: 서버 응답 실패")
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