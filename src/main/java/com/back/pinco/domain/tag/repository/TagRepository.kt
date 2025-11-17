package com.back.pinco.domain.tag.repository;

import com.back.pinco.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    // 키워드로 태그 조회
    Optional<Tag> findByKeyword(String keyword);

    // 키워드 존재 여부 확인
    boolean existsByKeyword(String keyword);
}

