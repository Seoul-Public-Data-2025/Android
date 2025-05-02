package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.RelationGuardianDeleteData
import com.maumpeace.safeapp.repository.RelationGuardianDeleteRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class RelationGuardianDeleteViewModel @Inject constructor(
    private val relationGuardianDeleteRepository: RelationGuardianDeleteRepository
) : ViewModel() {

    private val _relationGuardianDeleteData = MutableLiveData<RelationGuardianDeleteData?>()
    val relationGuardianDeleteData: LiveData<RelationGuardianDeleteData?> get() = _relationGuardianDeleteData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun relationGuardianDelete(guardianId: Int) {
        _relationGuardianDeleteData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = relationGuardianDeleteRepository.guardianDelete(guardianId)

                if (result.success) {
                    _relationGuardianDeleteData.postValue(result)
                } else {
                    _errorMessage.postValue("해지 실패: 서버 응답 실패")
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