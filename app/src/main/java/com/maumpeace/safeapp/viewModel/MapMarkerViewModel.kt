package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.MapMarkerData
import com.maumpeace.safeapp.repository.MapMarkerRepository
import com.maumpeace.safeapp.util.HttpErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MapMarkerViewModel @Inject constructor(
    private val mapMarkerRepository: MapMarkerRepository
) : ViewModel() {

    private val _mapMarkerData = MutableLiveData<MapMarkerData?>()
    val mapMarkerData: LiveData<MapMarkerData?> get() = _mapMarkerData

    // 에러 메시지 전달용
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun mapMarker() {
        _mapMarkerData.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = mapMarkerRepository.mapMarker()

                if (result.success) {
                    _mapMarkerData.postValue(result)
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