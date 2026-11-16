package com.oblab.toiletmap

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class ToiletMapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
