package com.oq.barnote.ui.review

import android.app.Activity
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.oq.barnote.core.oqcore.utils.OQLog

/**
 * Google Play In-App Review 요청 헬퍼. iOS `AppStore.requestReview(in:)` 대응.
 *
 * Play Core Review API 는 Activity 컨텍스트가 필요하므로 ViewModel 이 아닌 Activity 에서 호출합니다.
 * AppController.reviewRequestEvent 를 collect 해 [request] 를 호출하면 됩니다.
 *
 * Note: 실제 리뷰 다이얼로그가 떴는지 여부는 Play Core 가 quota / 사용자 상태(이미 리뷰함 등)에 따라
 * 결정합니다. 호출이 실패하거나 표시되지 않아도 정상 동작이며, iOS 와 동일하게 silent fail 처리.
 * Play Store 미설치 / 미인증 기기 (사이드로드, 에뮬레이터 등) 에서도 안전하게 실패합니다.
 */
object AppReviewRequester {

    /** Activity context 에서 In-App Review flow 를 시작합니다. */
    fun request(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                val ex = task.exception
                val errorCode = (ex as? ReviewException)?.errorCode
                OQLog.w("[AppReviewRequester] requestReviewFlow 실패: code=$errorCode, error=$ex")
                return@addOnCompleteListener
            }
            val info = task.result
            manager.launchReviewFlow(activity, info).addOnCompleteListener { launchTask ->
                // Play Core 가 다이얼로그 노출 여부와 무관하게 항상 success 로 마침.
                if (!launchTask.isSuccessful) {
                    OQLog.w("[AppReviewRequester] launchReviewFlow 실패: ${launchTask.exception}")
                }
            }
        }
    }
}
