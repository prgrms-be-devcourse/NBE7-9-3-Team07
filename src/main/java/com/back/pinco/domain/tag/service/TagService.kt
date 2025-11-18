package com.back.pinco.domain.tag.service

import com.back.pinco.domain.tag.entity.Tag
import com.back.pinco.domain.tag.repository.TagRepository
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import io.micrometer.common.util.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.CollectionUtils

@Service
class TagService(private val tagRepository: TagRepository) {


    /* =====================================================
            주요 비즈니스 로직 (Public Methods)
        ====================================================== */
    fun getAllTags(): List<Tag> {
        val tags = findAllTags() // 태그 전체 조회
        validateTagList(tags) // 결과 검증
        return tags // 반환
    }

    // 태그 생성
    @Transactional
    fun createTag(keyword: String): Tag {
        validateKeyword(keyword) // 입력값 검증
        checkDuplicateKeyword(keyword) // 중복 검사
        return saveNewTag(keyword) // 저장
    }

    /* =====================================================
        내부 세부 로직 (Private Helper Methods)
    ====================================================== */
    // ===== 검증 유틸 =====
    // 태그 목록 검증
    private fun validateTagList(tags: List<Tag>) {
        if (tags.isEmpty()) {
            throw ServiceException(ErrorCode.TAG_NOT_FOUND)
        }
    }

    // 태그 키워드 검증
    private fun validateKeyword(keyword: String) {
        if (StringUtils.isBlank(keyword)) {
            throw ServiceException(ErrorCode.INVALID_TAG_KEYWORD)
        }
    }

    // 중복 태그 키워드 검사
    private fun checkDuplicateKeyword(keyword: String) {
        if (tagRepository.existsByKeyword(keyword.trim { it <= ' ' })) {
            throw ServiceException(ErrorCode.TAG_ALREADY_EXISTS)
        }
    }

    // ===== 조회/생성 유틸 =====
    // 새로운 태그 저장
    private fun saveNewTag(keyword: String): Tag {
        try {
            return tagRepository.save<Tag>(Tag(keyword.trim { it <= ' ' }))
        } catch (_: Exception) {
            throw ServiceException(ErrorCode.TAG_CREATE_FAILED)
        }
    }

    // 태그 전체 조회
    private fun findAllTags(): List<Tag> {
        return tagRepository.findAll()
    }
}

