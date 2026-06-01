package com.oq.barnote.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.result.Credentials
import com.oq.barnote.R
import com.oq.barnote.core.data.auth.Auth0AuthStore
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQHapticService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoggingIn: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginUiEvent {
    data object LoginStarted : LoginUiEvent
    data class LoginSuccess(val credentials: Credentials) : LoginUiEvent
    data object LoginCancelled : LoginUiEvent
    data class LoginError(val message: String) : LoginUiEvent
    data object DismissError : LoginUiEvent
}

sealed interface LoginNavEffect {
    data object LoggedIn : LoginNavEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authStore: Auth0AuthStore,
    private val appController: AppController,
    private val haptic: OQHapticService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<LoginNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            LoginUiEvent.LoginStarted ->
                _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }
            is LoginUiEvent.LoginSuccess -> handleSuccess(event.credentials)
            LoginUiEvent.LoginCancelled ->
                _uiState.update { it.copy(isLoggingIn = false) }
            is LoginUiEvent.LoginError ->
                _uiState.update { it.copy(isLoggingIn = false, errorMessage = event.message) }
            LoginUiEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun handleSuccess(creds: Credentials) {
        viewModelScope.launch {
            authStore.saveAuth0Credentials(creds)
            _uiState.update { it.copy(isLoggingIn = false) }
            // iOS `loginResponse(.success)`: 성공 햅틱 + "로그인 성공 🎉" 토스트.
            haptic.success()
            appController.showToast(context.getString(R.string.rogeuin_seonggong))
            _navEffect.send(LoginNavEffect.LoggedIn)
        }
    }
}
