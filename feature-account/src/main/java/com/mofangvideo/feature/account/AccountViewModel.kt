package com.mofangvideo.feature.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mofangvideo.core.model.UserAccount
import com.mofangvideo.core.repo.AccountRepository
import com.mofangvideo.core.repo.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 账号 ViewModel。
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AccountRepository
) : ViewModel() {

    val currentUser: LiveData<UserAccount?> = repo.currentUser().asLiveData()

    private val _loginResult = MutableLiveData<Result<UserAccount>?>(null)
    val loginResult: LiveData<Result<UserAccount>?> = _loginResult

    private val _registerResult = MutableLiveData<Result<UserAccount>?>(null)
    val registerResult: LiveData<Result<UserAccount>?> = _registerResult

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = repo.login(phone, password)
        }
    }

    fun register(phone: String, password: String) {
        viewModelScope.launch {
            _registerResult.value = repo.register(phone, password)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _loginResult.value = null
            _registerResult.value = null
        }
    }

    fun clearResults() {
        _loginResult.value = null
        _registerResult.value = null
    }
}
