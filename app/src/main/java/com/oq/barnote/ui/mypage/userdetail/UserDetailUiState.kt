package com.oq.barnote.ui.mypage.userdetail

import com.oq.barnote.core.domain.User

/** 내 정보 화면 상태. iOS `UserDetailFeature.State` 에 대응. */
data class UserDetailUiState(
    val myInfo: User? = null,
    val noteCount: Int? = null,
    val favoriteCount: Int? = null,
    val isSubscribed: Boolean = false,
    val isEditingProfile: Boolean = false,
    val editingNickname: String = "",
    val editingIntro: String = "",
    val showDeleteAccountAlert: Boolean = false,
)

/** 내 정보 화면 사용자 액션. iOS `UserDetailFeature.Action` 에 대응. */
sealed interface UserDetailUiEvent {
    data object OnAppear : UserDetailUiEvent
    data object TappedDeleteAccount : UserDetailUiEvent
    data object ConfirmDeleteAccount : UserDetailUiEvent
    data object DismissDeleteAccountAlert : UserDetailUiEvent
    data object TappedEditProfile : UserDetailUiEvent
    data class SetEditingProfile(val isEditing: Boolean) : UserDetailUiEvent
    data class NicknameChanged(val value: String) : UserDetailUiEvent
    data class IntroChanged(val value: String) : UserDetailUiEvent
    data object TappedSaveProfile : UserDetailUiEvent
    data object TappedSubscribe : UserDetailUiEvent
    data class TappedCopyWebUrl(val url: String) : UserDetailUiEvent
    data class TappedOpenWebUrl(val url: String) : UserDetailUiEvent
    data object TappedProfileImage : UserDetailUiEvent

    /** Compose 측에서 picker 결과를 ViewModel 에 전달. */
    data class ProfileImagePicked(val attachment: com.oq.barnote.core.domain.MediaAttachment) :
        UserDetailUiEvent
}

/** 일회성 네비게이션 / 외부 효과. iOS `UserDetailFeature.Delegate` 에 대응. */
sealed interface UserDetailNavEffect {
    data object Subscribe : UserDetailNavEffect
    data object DidDeleteAccount : UserDetailNavEffect

    /** Compose 측에서 picker 를 띄우도록 요청. */
    data object RequestProfileImagePicker : UserDetailNavEffect

    /** Compose 측에서 url 을 클립보드에 복사 + 토스트. */
    data class CopyToClipboard(val text: String) : UserDetailNavEffect

    /** Compose 측에서 browser 로 url 열기. */
    data class OpenExternalUrl(val url: String) : UserDetailNavEffect
}
