package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.CreateRelationData
import com.maumpeace.safeapp.repository.CreateRelationRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CreateRelationViewModel @Inject constructor(
    private val createRelationRepository: CreateRelationRepository
) : ViewModel() {
    private val _createRelationData = MutableLiveData<CreateRelationData?>()
    val createRelationData: LiveData<CreateRelationData?> get() = _createRelationData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun createRelation(parentPhoneNumber: String, parentName: String) {
        _createRelationData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = createRelationRepository.createRelation(parentPhoneNumber, parentName)

                if (result.success) {
                    _createRelationData.postValue(result)
                } else {
                    _errorMessage.postValue("관계 생성 실패: 서버 응답 실패")
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