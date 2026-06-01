package com.oq.barnote.ui.mypage

import com.oq.barnote.core.domain.User

/**
 * 마이페이지 화면 상태. iOS `MyPageFeature.State` 에 대응.
 */
data class MyPageUiState(
    val myInfo: User? = null,
    val noteCount: Int = 0,
    val favoriteCount: Int = 0,
    val followerCount: Int = 0,
    val neededReviewProduct: Boolean = false,
    val isLoading: Boolean = false,
    val isSubscribed: Boolean = false,
)

/** 마이페이지 사용자 액션. iOS `MyPageFeature.Action` 에 대응. */
sealed interface MyPageUiEvent {
    data object OnAppear : MyPageUiEvent
    data object TappedLogin : MyPageUiEvent
    data object TappedLogout : MyPageUiEvent
    data object TappedMyInfo : MyPageUiEvent
    data object TappedMyNotes : MyPageUiEvent
    data object TappedFavorites : MyPageUiEvent
    data object TappedFollowing : MyPageUiEvent
    data object TappedFollowers : MyPageUiEvent
    data object TappedTastedProducts : MyPageUiEvent
    data object TappedNeededReviewNotes : MyPageUiEvent
    data object TappedProfile : MyPageUiEvent
    data object TappedSubscribe : MyPageUiEvent
}

/** 일회성 네비게이션 효과. iOS `MyPageFeature.Delegate` 에 대응. */
sealed interface MyPageNavEffect {
    data object Login : MyPageNavEffect
    data object UserDetail : MyPageNavEffect
    data class NoteList(val isMine: Boolean) : MyPageNavEffect
    data class ProductList(val type: ProductListType) : MyPageNavEffect
    data object NeededReviewNoteList : MyPageNavEffect
    data object Subscribe : MyPageNavEffect
    data class UserNoteList(val userId: String) : MyPageNavEffect
    data class UserList(val type: UserListType) : MyPageNavEffect
}

/** iOS `ProductListFeature.FetchType` 의 일부 (마이페이지에서 사용하는 두 가지). */
enum class ProductListType { Favorites, Tasted }

/** iOS `UserListFeature.ListType` 에 대응. */
enum class UserListType { Following, Followers }
