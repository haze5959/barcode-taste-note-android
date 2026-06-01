package com.oq.barnote.extension

import com.oq.barnote.Constants
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.User
import com.oq.barnote.core.domain.UserInfo

/**
 * iOS 의 `NoteInfo.shareUrl` / `UserInfo.shareUrl` computed property 를 Kotlin 확장 프로퍼티로 포팅.
 *
 * 도메인 모듈은 [Constants.S.WEB_BASE_URL] (app 모듈) 을 모르므로 app 의 extension 으로 둡니다.
 * 이렇게 분리하지 않으면 각 화면 (Settings 데이터 내보내기 / UserNoteList 공유 / NoteDetail 공유 /
 * UserDetail 웹 페이지 진입) 마다 `"${WEB_BASE_URL}/note/${id}"` 문자열을 반복 조립하다 보니
 * 잘못된 path / case / id 정규화로 표류할 위험이 있습니다 (실제로 .lowercase() 누락 케이스가 있었음).
 *
 * UUID case 정책: 서버 canonical 은 lowercase 라 [String.lowercase] 로 정규화합니다. iOS 는 Swift
 * `UUID.description` 이 uppercase 를 돌려주는 특성상 사실상 uppercase 가 섞여 있지만, 안드로이드는
 * 한 가지로 통일하는 쪽이 deep-link / 서버 매칭 / 분석 이벤트 모두 일관됩니다.
 */

/** 시음 노트의 공유 URL. iOS `NoteInfo.shareUrl` 과 동등. */
val NoteInfo.shareUrl: String
    get() = "${Constants.S.WEB_BASE_URL}/note/${id.lowercase()}"

/** 사용자 정보의 공유 URL. iOS `UserInfo.shareUrl` 과 동등. */
val UserInfo.shareUrl: String
    get() = "${Constants.S.WEB_BASE_URL}/user/${id.lowercase()}"

/**
 * `UserInfo` 가 아닌 raw `User` 객체에서도 동일한 형식의 공유 URL 이 필요할 때 사용.
 * UserDetailScreen 등 `UserInfo` 가 없는 진입점 (자기 자신의 프로필 화면) 용도.
 */
val User.shareUrl: String
    get() = "${Constants.S.WEB_BASE_URL}/user/${id.lowercase()}"
