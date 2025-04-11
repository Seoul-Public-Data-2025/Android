package com.maumpeace.safeapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.repository.LoginRepository
import com.maumpeace.safeapp.util.GlobalApplication
import com.maumpeace.safeapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
) : ViewModel() {
    private val _loginData = MutableLiveData<LoginData>()
    val loginData: LiveData<LoginData> get() = _loginData
    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> get() = _errorLiveData

    fun fetchLoginData(email: String, code: String) {
        _loginData.value = null
        _errorLiveData.value = null
        viewModelScope.launch {
            try {
                val result = loginRepository.fetchLogin(email, code)
                if (result.success) {
                    val accessToken = result.result.accessToken
                    val refreshToken = result.result.refreshToken
                    TokenManager.saveAccessToken(
                        context = GlobalApplication.INSTANCE.applicationContext,
                        accessToken.toString()
                    )
                    TokenManager.saveRefreshToken(
                        context = GlobalApplication.INSTANCE.applicationContext,
                        refreshToken.toString()
                    )
                }
                _loginData.postValue(result)
            } catch (e: Exception) {
                _errorLiveData.postValue(e.message)
            }
        }
    }
}