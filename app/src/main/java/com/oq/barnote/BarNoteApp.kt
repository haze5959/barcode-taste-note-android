package com.oq.barnote

import android.app.Application
import com.oq.barnote.core.data.auth.AuthSessionObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 앱 진입점 Application 클래스.
 * Hilt 의존성 주입 컨테이너의 루트를 제공합니다.
 */
@HiltAndroidApp
class BarNoteApp : Application() {

    @Inject
    lateinit var authSessionObserver: AuthSessionObserver

    override fun onCreate() {
        super.onCreate()
        // AuthStore.isLoggedIn = false 로 전환되면 UserStore 캐시 자동 clear
        authSessionObserver.start()
    }
}
