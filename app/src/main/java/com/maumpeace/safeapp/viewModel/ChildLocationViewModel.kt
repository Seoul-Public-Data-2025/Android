package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.ChildLocationData
import com.maumpeace.safeapp.repository.ChildLocationRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ChildLocationViewModel @Inject constructor(
    private val childLocationRepository: ChildLocationRepository
) : ViewModel() {
    private val _childLocationData = MutableLiveData<ChildLocationData?>()
    val childLocationData: LiveData<ChildLocationData?> get() = _childLocationData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun childLocation(time: String, lat: String, lot: String) {
        _childLocationData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = childLocationRepository.childLocation(time, lat, lot)

                if (result.success) {
                    _childLocationData.postValue(result)
                } else {
                    _errorMessage.postValue("위치 전송 실패: 서버 응답 실패")
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