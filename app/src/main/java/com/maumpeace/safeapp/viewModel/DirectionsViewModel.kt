package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.repository.NaverDirectionsRepository
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DirectionsViewModel @Inject constructor(
    private val repository: NaverDirectionsRepository
) : ViewModel() {

    private val _path = MutableLiveData<List<LatLng>>()
    val path: LiveData<List<LatLng>> get() = _path

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchRoute(start: String, goal: String, clientId: String, clientSecret: String) {
        viewModelScope.launch {
            try {
                val response = repository.getDrivingRoute(start, goal, clientId, clientSecret)
                val coords = response.route.traoptimal.first().path.map {
                    LatLng(it[1], it[0])
                }
                _path.value = coords
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "길찾기 오류"
            }
        }
    }
}
