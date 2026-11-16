package com.oblab.toiletmap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("UPDATE place SET name = :name, password = :password, memo = :memo, updated_at = :now WHERE id = :id")
    abstract suspend fun updateInfo(id: String, name: String?, password: String?, memo: String?, now: Long)

    // 위치를 옮기면 이전 주소는 틀리므로 비우고 다시 채운다
    @Query("UPDATE place SET latitude = :lat, longitude = :lng, address = NULL, updated_at = :now WHERE id = :id")
    abstract suspend fun updateLocation(id: String, lat: Double, lng: Double, now: Long)

    @Query("UPDATE place SET deleted = 'Y', updated_at = :now WHERE id = :id")
    abstract suspend fun softDelete(id: String, now: Long)

    @Transaction
    open suspend fun insertPlaceWithVisit(place: PlaceEntity, visit: VisitEntity) {
        insertPlace(place)
        insertVisit(visit)
    }
}
