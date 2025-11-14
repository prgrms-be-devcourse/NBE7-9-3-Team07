package com.back.pinco.domain.pin.repository

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.global.geometry.GeometryUtil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
interface PinRepository : JpaRepository<Pin, Long> {
    @Query(
        value = "SELECT * FROM pins p WHERE ST_DWithin(p.point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), "
                + GeometryUtil.SRID +
                ")::geography, :radiusInMeters) AND p.is_deleted = false"
                +" AND (user_id = :userId OR is_public = true)",
        nativeQuery = true
    )
    fun findPinsWithinRadius(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusInMeters") radiusInMeters: Double,
        @Param("userId") userId: Long
    ): List<Pin>

    @Query(
        value = "SELECT * FROM pins p WHERE ST_DWithin(p.point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), "
                + GeometryUtil.SRID +
                ")::geography, :radiusInMeters) AND p.is_deleted = false"
                +" AND is_public = true",
                nativeQuery = true
    )
    fun findPublicPinsWithinRadius(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusInMeters") radiusInMeters: Double
    ): List<Pin>

    @Query(
        value = "SELECT p.* FROM pins p WHERE p.is_deleted = false " +
                "AND p.point && ST_MakeEnvelope(:lonMin, :latMin, :lonMax, :latMax, "
                + GeometryUtil.SRID +
                ") "
                +" AND (user_id = :userId OR is_public = true)",
                nativeQuery = true
    )
    fun findScreenPins(
        @Param("latMax") latMax: Double,
        @Param("lonMax") lonMax: Double,
        @Param("latMin") latMin: Double,
        @Param("lonMin") lonMin: Double,
        @Param("userId") userId: Long
    ): List<Pin>

    @Query(
        value = "SELECT p.* FROM pins p WHERE p.is_deleted = false " +
                "AND p.point && ST_MakeEnvelope(:lonMin, :latMin, :lonMax, :latMax, "
                + GeometryUtil.SRID +
                ") "
                +" AND is_public = true",
                nativeQuery = true
    )
    fun findPublicScreenPins(
        @Param("latMax") latMax: Double,
        @Param("lonMax") lonMax: Double,
        @Param("latMin") latMin: Double,
        @Param("lonMin") lonMin: Double
    ): List<Pin>

    // 특정 사용자의 핀 조회
    @Query(
        value = """
    SELECT p FROM Pin p
    WHERE p.user.id = :writerId
      AND (p.user.id = :actorId OR p.isPublic = true)
      AND p.deleted=false
"""
    )
    fun findAccessibleByUser(writerId: Long, actorId: Long): List<Pin>

    @Query(
        value = """
    SELECT p FROM Pin p
    WHERE p.user.id = :writerId
      AND p.isPublic = true
      AND p.deleted=false
"""
    )
    fun findPublicByUser(writerId: Long): List<Pin>

    @Query(
        value = ("SELECT p.* FROM pins p " +
                "WHERE p.is_deleted = false " +
                "AND p.user_id = :userId " +
                "AND EXTRACT(YEAR FROM p.create_at) = :year " +
                "AND EXTRACT(MONTH FROM p.create_at) = :month " +
                "AND p.is_public = true"),
        nativeQuery = true
    )
    fun findPublicByUserDate(
        @Param("userId") userId: Long,
        @Param("year") year: Int,
        @Param("month") month: Int
    ): List<Pin>

    @Query(
        value = ("SELECT p.* FROM pins p " +
                "WHERE p.is_deleted = false " +
                "AND p.user_id = :writerId " +
                "AND EXTRACT(YEAR FROM p.create_at) = :year " +
                "AND EXTRACT(MONTH FROM p.create_at) = :month " +
                "AND (p.user_id = :actorId OR p.is_public = true)"), nativeQuery = true
    )
    fun findAccessibleByUserDate(
        writerId: Long?, actorId: Long,
        @Param("year") year: Int,
        @Param("month") month: Int
    ): List<Pin>


    // 전체 핀 조회
    @Query(
        value = """
    SELECT p FROM Pin p
    WHERE p.deleted = false
      AND (p.user.id = :userId OR p.isPublic = true)
"""
    )
    fun findAllAccessiblePins(@Param("userId") userId: Long): List<Pin>

    @Query(
        value = """
    SELECT p FROM Pin p
    WHERE p.deleted = false
      AND p.isPublic = true
"""
    )
    fun findAllPublicPins(): List<Pin>

    // id로 핀 조회
    @Query(
        """
    SELECT p FROM Pin p
    WHERE p.id = :id
      AND p.deleted = false
      AND (p.user.id = :userId OR p.isPublic = true)
"""
    )
    fun findAccessiblePinById(@Param("id") id: Long, @Param("userId") userId: Long): Pin?

    @Query(
        """
    SELECT p FROM Pin p
    WHERE p.id = :id
      AND p.deleted = false
      AND p.isPublic = true
"""
    )
    fun findPublicPinById(@Param("id") id: Long): Pin?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "UPDATE pins p SET " +
                "like_count = COALESCE(" +
                "( SELECT COUNT(*) FROM likes l WHERE l.pin_id = p.pin_id)," +
                " 0) " +
                "WHERE p.pin_id = :pinId",
        nativeQuery = true
    )
    fun refreshLikeCount(@Param("pinId") pinId: Long)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "UPDATE pins p SET like_count = COALESCE(( SELECT COUNT(*) FROM likes l WHERE l.pin_id = p.pin_id), 0) WHERE p.pin_id = ANY(:pinIds)",
        nativeQuery = true
    )
    fun refreshLikeCountBatch(@Param("pinIds") pinIds: Array<Long>)

    @Modifying
    @Query("UPDATE Pin p SET p.deleted = true WHERE p.user.id = :userId AND p.deleted = false")
    fun updatePinsToDeletedByUserId(@Param("userId") userId: Long): Int


}
