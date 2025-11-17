package com.back.pinco.domain.tag.service;

import com.back.pinco.domain.tag.entity.Tag;
import com.back.pinco.domain.tag.repository.TagRepository;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /* =====================================================
        주요 비즈니스 로직 (Public Methods)
    ====================================================== */

    // 모든 태그 조회
    public List<Tag> getAllTags() {
        List<Tag> tags = findAllTags();      // 태그 전체 조회
        validateTagList(tags);               // 결과 검증
        return tags; // 반환
    }

    // 태그 생성
    @Transactional
    public Tag createTag(String keyword) {
        validateKeyword(keyword);            // 입력값 검증
        checkDuplicateKeyword(keyword);      // 중복 검사
        return saveNewTag(keyword);          // 저장
    }

    /* =====================================================
        내부 세부 로직 (Private Helper Methods)
    ====================================================== */

    // ===== 검증 유틸 =====

    // 태그 목록 검증
    private void validateTagList(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new ServiceException(ErrorCode.TAG_NOT_FOUND);
        }
    }

    // 태그 키워드 검증
    private void validateKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            throw new ServiceException(ErrorCode.INVALID_TAG_KEYWORD);
        }
    }

    // 중복 태그 키워드 검사
    private void checkDuplicateKeyword(String keyword) {
        if (tagRepository.existsByKeyword(keyword.trim())) {
            throw new ServiceException(ErrorCode.TAG_ALREADY_EXISTS);
        }
    }

    // ===== 조회/생성 유틸 =====

    // 새로운 태그 저장
    private Tag saveNewTag(String keyword) {
        try {
            return tagRepository.save(new Tag(keyword.trim()));
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.TAG_CREATE_FAILED);
        }
    }

    // 태그 전체 조회
    private List<Tag> findAllTags() {
        return tagRepository.findAll();
    }
}

