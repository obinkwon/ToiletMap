package com.oblab.toiletmap.data

import androidx.room.Database
import androidx.room.RoomDatabase

// ponytail: exportSchema=false. 스키마를 바꿀 때(버전 2) 마이그레이션과 함께 스키마 내보내기 켜기
@Database(entities = [PlaceEntity::class, VisitEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}
