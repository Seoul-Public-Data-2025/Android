package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.RelationResendData
import com.maumpeace.safeapp.repository.RelationResendRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class RelationResendViewModel @Inject constructor(
    private val relationResendRepository: RelationResendRepository
) : ViewModel() {

    private val _relationResendData = MutableLiveData<RelationResendData?>()
    val relationResendData: LiveData<RelationResendData?> get() = _relationResendData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun relationResend(id: Int) {
        _relationResendData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = relationResendRepository.guardianDelete(id)

                if (result.success) {
                    _relationResendData.postValue(result)
                } else {
                    _errorMessage.postValue("재발송 실패: 서버 응답 실패")
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