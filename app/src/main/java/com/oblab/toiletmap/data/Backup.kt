package com.oblab.toiletmap.data

import org.json.JSONArray
import org.json.JSONObject

/** 백업 파일 형식 버전. 필드를 바꾸면 올리고 parseBackup에서 이전 버전도 읽게 한다. */
const val BACKUP_VERSION = 1

class BackupData(val places: List<PlaceEntity>, val visits: List<VisitEntity>)

/** 장소·방문 전체를 JSON 문자열로. 삭제 표시된 장소도 그대로 넣는다(나중에 동기화 대비). */
fun toBackupJson(places: List<PlaceEntity>, visits: List<VisitEntity>, exportedAt: Long): String =
    JSONObject()
        .put("app", "toiletmap")
        .put("version", BACKUP_VERSION)
        .put("exported_at", exportedAt)
        .put(
            "places",
            JSONArray(
                places.map {
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name.orJsonNull())
                        .put("latitude", it.latitude)
                        .put("longitude", it.longitude)
                        .put("address", it.address.orJsonNull())
                        .put("password", it.password.orJsonNull())
                        .put("memo", it.memo.orJsonNull())
                        .put("created_at", it.createdAt)
                        .put("updated_at", it.updatedAt)
                        .put("deleted", it.deleted)
                        .put("dup_checked", it.dupChecked)
                },
            ),
        )
        .put(
            "visits",
            JSONArray(
                visits.map {
                    JSONObject()
                        .put("id", it.id)
                        .put("place_id", it.placeId)
                        .put("visited_at", it.visitedAt)
                        .put("raw_latitude", it.rawLatitude)
                        .put("raw_longitude", it.rawLongitude)
                        .put("accuracy_m", it.accuracyM.orJsonNull())
                },
            ),
        )
        .toString(2)

/**
 * 사용자가 고른 파일이라 믿지 않고 검사한다. 형식이 틀리면 IllegalArgumentException 또는 JSONException.
 * 에러 메시지는 그대로 사용자에게 보여준다.
 */
fun parseBackup(text: String): BackupData {
    val root = JSONObject(text)
    require(root.optString("app") == "toiletmap") { "화장실 지도 백업 파일이 아닙니다" }
    require(root.optInt("version", -1) in 1..BACKUP_VERSION) { "지원하지 않는 백업 버전입니다. 앱을 업데이트해 주십시오" }

    val places = root.getJSONArray("places").objects().map {
        PlaceEntity(
            id = it.getString("id"),
            name = it.stringOrNull("name"),
            latitude = it.getDouble("latitude").also { v -> require(v in -90.0..90.0) { "위도 값이 잘못되었습니다" } },
            longitude = it.getDouble("longitude").also { v -> require(v in -180.0..180.0) { "경도 값이 잘못되었습니다" } },
            address = it.stringOrNull("address"),
            password = it.stringOrNull("password"),
            memo = it.stringOrNull("memo"),
            createdAt = it.getLong("created_at"),
            updatedAt = it.getLong("updated_at"),
            deleted = it.getString("deleted").also { v -> require(v == "Y" || v == "N") { "deleted 값이 잘못되었습니다" } },
            dupChecked = it.getString("dup_checked").also { v -> require(v == "Y" || v == "N") { "dup_checked 값이 잘못되었습니다" } },
        )
    }
    val visits = root.getJSONArray("visits").objects().map {
        VisitEntity(
            id = it.getString("id"),
            placeId = it.getString("place_id"),
            visitedAt = it.getLong("visited_at"),
            rawLatitude = it.getDouble("raw_latitude"),
            rawLongitude = it.getDouble("raw_longitude"),
            accuracyM = if (it.isNull("accuracy_m")) null else it.getDouble("accuracy_m"),
        )
    }
    return BackupData(places, visits)
}

private fun Any?.orJsonNull(): Any = this ?: JSONObject.NULL

private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
