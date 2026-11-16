package com.oblab.toiletmap.data

import android.content.Context

/** 설정값. 몇 개 안 돼서 SharedPreferences로 충분. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 등록할 때 이 거리(m) 안의 이전 장소를 중복 후보로 본다. */
    var duplicateRadiusM: Int
        get() = prefs.getInt(KEY_DUPLICATE_RADIUS, DUPLICATE_RADIUS_M.toInt())
        set(value) = prefs.edit().putInt(KEY_DUPLICATE_RADIUS, value).apply()

    companion object {
        private const val KEY_DUPLICATE_RADIUS = "duplicate_radius_m"
        val RADIUS_OPTIONS = listOf(15, 30, 50, 100)
    }
}
