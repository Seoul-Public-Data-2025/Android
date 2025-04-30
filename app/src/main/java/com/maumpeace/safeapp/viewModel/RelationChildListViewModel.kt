package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.RelationChildListData
import com.maumpeace.safeapp.repository.RelationChildListRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class RelationChildListViewModel @Inject constructor(
    private val relationChildListRepository: RelationChildListRepository
) : ViewModel() {
    private val _relationChildListData = MutableLiveData<RelationChildListData?>()
    val relationChildListData: LiveData<RelationChildListData?> get() = _relationChildListData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun relationChildList() {
        _relationChildListData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = relationChildListRepository.relationChildList()

                if (result.success) {
                    _relationChildListData.postValue(result)
                } else {
                    _errorMessage.postValue("자녀 조회 실패: 서버 응답 실패")
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