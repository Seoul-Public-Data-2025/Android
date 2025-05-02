package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.RelationGuardianListData
import com.maumpeace.safeapp.repository.RelationGuardianListRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class RelationGuardianListViewModel @Inject constructor(
    private val relationGuardianListRepository: RelationGuardianListRepository
) : ViewModel() {
    private val _relationGuardianListData = MutableLiveData<RelationGuardianListData?>()
    val relationGuardianListData: LiveData<RelationGuardianListData?> get() = _relationGuardianListData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun relationGuardianList() {
        _relationGuardianListData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = relationGuardianListRepository.relationGuardianList()

                if (result.success) {
                    _relationGuardianListData.postValue(result)
                } else {
                    _errorMessage.postValue("보호자 조회 실패: 서버 응답 실패")
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