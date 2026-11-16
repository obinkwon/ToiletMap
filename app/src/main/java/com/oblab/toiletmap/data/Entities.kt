package com.oblab.toiletmap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** 장소. 같은 화장실에 여러 번 가도 장소는 하나. 기획서 place 스키마 그대로. */
@Entity(tableName = "place")
data class PlaceEntity(
    // 2단계 서버 동기화 대비해 처음부터 UUID
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val password: String? = null,
    val memo: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** Y/N. 삭제는 soft delete */
    val deleted: String = "N",
    /** Y/N. 앱 등록=Y, 위젯 등록=N(다음 앱 실행 때 합칠지 물어봄) */
    @ColumnInfo(name = "dup_checked") val dupChecked: String = "N",
)

/** 방문 기록. 등록 당시 GPS 원본 좌표를 남긴다. */
@Entity(
    tableName = "visit",
    foreignKeys = [ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["place_id"])],
    indices = [Index("place_id")],
)
data class VisitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "visited_at") val visitedAt: Long,
    @ColumnInfo(name = "raw_latitude") val rawLatitude: Double,
    @ColumnInfo(name = "raw_longitude") val rawLongitude: Double,
    @ColumnInfo(name = "accuracy_m") val accuracyM: Double?,
)

/** 지도·목록에 쓰는 장소 + 방문 통계. */
data class PlaceWithStats(
    val id: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val password: String?,
    val memo: String?,
    @ColumnInfo(name = "visit_count") val visitCount: Int,
    @ColumnInfo(name = "last_visited_at") val lastVisitedAt: Long?,
)
