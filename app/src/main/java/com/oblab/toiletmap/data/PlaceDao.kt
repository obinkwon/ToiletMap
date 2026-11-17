package com.oblab.toiletmap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaceDao {
    @Query(
        """
        SELECT p.id, p.name, p.latitude, p.longitude, p.address, p.password, p.memo,
               COUNT(v.id) AS visit_count, MAX(v.visited_at) AS last_visited_at
        FROM place p LEFT JOIN visit v ON v.place_id = p.id
        WHERE p.deleted = 'N'
        GROUP BY p.id
        ORDER BY last_visited_at DESC
        """,
    )
    abstract fun observePlaces(): Flow<List<PlaceWithStats>>

    @Insert
    abstract suspend fun insertPlace(place: PlaceEntity)

    @Insert
    abstract suspend fun insertVisit(visit: VisitEntity)

    @Query("UPDATE place SET address = :address, updated_at = :now WHERE id = :id")
    abstract suspend fun updateAddress(id: String, address: String, now: Long)

    @Query("SELECT * FROM place WHERE deleted = 'N' AND address IS NULL")
    abstract suspend fun placesWithoutAddress(): List<PlaceEntity>

    @Query("UPDATE place SET name = :name, password = :password, memo = :memo, updated_at = :now WHERE id = :id")
    abstract suspend fun updateInfo(id: String, name: String?, password: String?, memo: String?, now: Long)

    // 위치를 옮기면 이전 주소는 틀리므로 비우고 다시 채운다
    @Query("UPDATE place SET latitude = :lat, longitude = :lng, address = NULL, updated_at = :now WHERE id = :id")
    abstract suspend fun updateLocation(id: String, lat: Double, lng: Double, now: Long)

    @Query("UPDATE place SET deleted = 'Y', updated_at = :now WHERE id = :id")
    abstract suspend fun softDelete(id: String, now: Long)

    /** 위젯으로 등록해서 아직 합칠지 안 물어본 장소 */
    @Query("SELECT id FROM place WHERE deleted = 'N' AND dup_checked = 'N'")
    abstract suspend fun uncheckedIds(): List<String>

    @Query("UPDATE place SET dup_checked = 'Y', updated_at = :now WHERE id = :id")
    abstract suspend fun markDupChecked(id: String, now: Long)

    @Query("UPDATE visit SET place_id = :toId WHERE place_id = :fromId")
    abstract suspend fun moveVisits(fromId: String, toId: String)

    // 합치는 쪽에 비어 있는 이름·비밀번호·메모만 합쳐지는 장소 값으로 채운다
    @Query(
        """
        UPDATE place SET
            name = COALESCE(name, (SELECT name FROM place WHERE id = :fromId)),
            password = COALESCE(password, (SELECT password FROM place WHERE id = :fromId)),
            memo = COALESCE(memo, (SELECT memo FROM place WHERE id = :fromId)),
            updated_at = :now
        WHERE id = :toId
        """,
    )
    abstract suspend fun fillMissingInfo(fromId: String, toId: String, now: Long)

    /** [fromId]의 방문을 [toId]로 옮기고 [fromId]는 지운다(soft delete). */
    @Transaction
    open suspend fun merge(fromId: String, toId: String, now: Long) {
        fillMissingInfo(fromId, toId, now)
        moveVisits(fromId, toId)
        markDupChecked(fromId, now)
        softDelete(fromId, now)
    }

    @Query("SELECT * FROM place")
    abstract suspend fun allPlaces(): List<PlaceEntity>

    @Query("SELECT * FROM visit")
    abstract suspend fun allVisits(): List<VisitEntity>

    // REPLACE는 지웠다 다시 넣어서 방문의 외래키가 깨진다. Upsert는 있으면 고치고 없으면 넣는다
    @Upsert
    abstract suspend fun upsertPlaces(places: List<PlaceEntity>)

    @Upsert
    abstract suspend fun upsertVisits(visits: List<VisitEntity>)

    /** 백업 가져오기. 같은 id는 백업 내용으로 덮어쓴다. 하나라도 실패하면 전부 취소. */
    @Transaction
    open suspend fun importBackup(places: List<PlaceEntity>, visits: List<VisitEntity>) {
        upsertPlaces(places)
        upsertVisits(visits)
    }

    @Transaction
    open suspend fun insertPlaceWithVisit(place: PlaceEntity, visit: VisitEntity) {
        insertPlace(place)
        insertVisit(visit)
    }
}
