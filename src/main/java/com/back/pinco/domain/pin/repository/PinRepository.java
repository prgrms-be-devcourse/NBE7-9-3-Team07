package com.back.pinco.domain.pin.repository;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.global.geometry.GeometryUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PinRepository extends JpaRepository<Pin,Long> {

    String BASE_QUERY =
            "SELECT * FROM pins p " +
                    "WHERE ST_DWithin(p.point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), "
                    + GeometryUtil.SRID +
                    ")::geography, :radiusInMeters) " +
                    "AND p.is_deleted = false ";

    @Query(value = BASE_QUERY + "AND (user_id = :userId OR is_public = true)", nativeQuery = true)
    List<Pin> findPinsWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusInMeters") Double radiusInMeters,
            @Param("userId") Long userId
    );

    @Query(value = BASE_QUERY + "AND is_public = true", nativeQuery = true)
    List<Pin> findPublicPinsWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusInMeters") Double radiusInMeters
    );




    // 특정 사용자의 핀 조회
    @Query(value = """
    SELECT p FROM Pin p
    WHERE p.user.id = :writerId
      AND (p.user.id = :actorId OR p.isPublic = true)
      AND p.deleted=false 
""")
    List<Pin> findAccessibleByUser(Long writerId, Long actorId);

    @Query(value = """
    SELECT p FROM Pin p
    WHERE p.user.id = :writerId
      AND p.isPublic = true
      AND p.deleted=false 
""")
    List<Pin> findPublicByUser(Long writerId);

    @Query(value = "SELECT p.* FROM pins p " +
            "WHERE p.is_deleted = false " +
            "AND p.user_id = :userId " +
            "AND EXTRACT(YEAR FROM p.create_at) = :year " +
            "AND EXTRACT(MONTH FROM p.create_at) = :month " +
            "AND p.is_public = true",
            nativeQuery = true)
    List<Pin> findPublicByUserDate(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
    @Query(value = "SELECT p.* FROM pins p " +
            "WHERE p.is_deleted = false " +
            "AND p.user_id = :writerId " +
            "AND EXTRACT(YEAR FROM p.create_at) = :year " +
            "AND EXTRACT(MONTH FROM p.create_at) = :month " +
            "AND (p.user_id = :actorId OR p.is_public = true)",
            nativeQuery = true)
    List<Pin> findAccessibleByUserDate(
            Long writerId, Long actorId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );



    // 전체 핀 조회
    @Query(value = """
    SELECT p FROM Pin p
    WHERE p.deleted = false
      AND (p.user.id = :userId OR p.isPublic = true)
""")
    List<Pin> findAllAccessiblePins(@Param("userId") Long userId);

    @Query(value = """
    SELECT p FROM Pin p
    WHERE p.deleted = false
      AND p.isPublic = true
""")
    List<Pin> findAllPublicPins();

    // id로 핀 조회
    @Query("""
    SELECT p FROM Pin p
    WHERE p.id = :id
      AND p.deleted = false
      AND (p.user.id = :userId OR p.isPublic = true)
""")
    Optional<Pin> findAccessiblePinById(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
    SELECT p FROM Pin p
    WHERE p.id = :id
      AND p.deleted = false
      AND p.isPublic = true
""")
    Optional<Pin> findPublicPinById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE pins p
        SET like_count = COALESCE((
          SELECT COUNT(*) FROM likes l
          WHERE l.pin_id = p.pin_id
        ), 0)
        WHERE p.pin_id = :pinId
        """, nativeQuery = true)
    void refreshLikeCount(@Param("pinId") Long pinId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE pins p
        SET like_count = COALESCE((
          SELECT COUNT(*) FROM likes l
          WHERE l.pin_id = p.pin_id
        ), 0)
        WHERE p.pin_id = ANY(:pinIds)
        """, nativeQuery = true)
    void refreshLikeCountBatch(@Param("pinIds") Long[] pinIds);

    @Modifying
    @Query("UPDATE Pin p SET p.deleted = true WHERE p.user.id = :userId AND p.deleted = false")
    int updatePinsToDeletedByUserId(@Param("userId") Long userId);

}
