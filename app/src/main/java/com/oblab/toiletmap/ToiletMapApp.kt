package com.oblab.toiletmap

import android.app.Application
import androidx.room.Room
import com.kakao.vectormap.KakaoMapSdk
import com.oblab.toiletmap.data.AppDatabase
import com.oblab.toiletmap.data.PlaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ToiletMapApp : Application() {

    /** 화면과 상관없이 앱이 살아 있는 동안 도는 작업용 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ponytail: Hilt 대신 수동 주입. 의존성이 늘어나면 Hilt로 교체
    val repository by lazy {
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "toiletmap.db").build()
        PlaceRepository(db.placeDao(), this, appScope)
    }

    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
