package com.back.pinco.domain.tag.repository;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.tag.entity.PinTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PinTagRepository extends JpaRepository<PinTag, Long> {
    // 특정 핀에 연결된 삭제되지 않은 PinTag 목록 조회
    List<PinTag> findAllByPin_IdAndDeletedFalse(Long pinId);

    // 특정 핀과 태그에 대한 PinTag 조회
    Optional<PinTag> findByPin_IdAndTag_Id(Long pinId, Long tagId);

    // 특정 태그 키워드와 연결된 삭제되지 않은 핀 목록 조회
    @Query("SELECT pt.pin FROM PinTag pt " +
            "JOIN pt.tag t " +
            "WHERE t.keyword = :keyword AND pt.deleted = false")
    List<Pin> findPinsByTagKeyword(@Param("keyword") String keyword);
}

