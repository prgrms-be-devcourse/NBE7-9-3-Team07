package com.back.pinco.domain.tag.service

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.tag.entity.PinTag
import com.back.pinco.domain.tag.entity.Tag
import com.back.pinco.domain.tag.repository.PinTagRepository
import com.back.pinco.domain.tag.repository.TagRepository
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.CollectionUtils

@Service
class PinTagService(
    private val tagRepository: TagRepository,
    private val pinTagRepository: PinTagRepository,
    private val pinRepository: PinRepository
) {
    

    /* =====================================================
        주요 비즈니스 로직 (Public Method)
    ====================================================== */
    // 핀에 태그 연결
    @Transactional
    fun addTagToPin(pinId: Long, keyword: String): PinTag {
        val pin = findPinById(pinId) // 핀 존재 여부 검증
        val tag = findOrCreateTag(keyword) // 태그 조회 또는 생성

        //handleExistingPinTag(pin, tag) // 기존 연결 처리
        return saveNewPinTag(pin, tag) // 새로운 핀-태그 연결 저장
    }

    // 핀에 연결된 태그 조회
    @Transactional(readOnly = true)
    fun getTagsByPin(pinId: Long): List<Tag> {
        validatePinExists(pinId) // 핀 존재 여부 검증
        val tags = findActiveTagsByPin(pinId) // 활성 태그 조회
        return tags // 태그 목록 반환
    }

    // 태그 삭제
    @Transactional
    fun removeTagFromPin(pinId: Long, tagId: Long) {
        val pinTag = findPinTagOrThrow(pinId, tagId) // 핀-태그 연결 조회
        deletePinTag(pinTag) // 핀-태그 연결 삭제
    }

    // 여러 태그를 핀에 연결(PinController용)
    @Transactional
    fun linkTagsToPin(pinId: Long, tagKeywords: List<String>): List<Tag> {
        validateKeywordList(tagKeywords) // 입력값 검증
        val pin = findPinById(pinId) // 핀 존재 여부 검증
        return processTagLinks(pin, tagKeywords) // 태그 연결 처리
    }

    // 여러 태그로 핀 조회
    @Transactional(readOnly = true)
    fun getPinsByMultipleTagKeywords(keywords: List<String>): List<Pin> {
        validateKeywordList(keywords) // 입력값 검증
        val pinsByEachTag = findPinsByTags(keywords) // 태그별 핀 조회
        val intersection = intersectPins(pinsByEachTag) // 교집합 계산
        validateResultPins(intersection) // 결과 검증
        return intersection // 핀 목록 반환
    }

    // 초기 데이터용 핀-태그 연결 생성
    @Transactional
    fun createPinTag(pin: Pin, tag: Tag): PinTag {
        return pinTagRepository.save<PinTag>(PinTag(pin, tag))
    }

    /* =====================================================
        내부 세부 로직 (Private Helper Methods)
    ====================================================== */
    // ===== 검증 유틸 =====
    // 입력값 검증
    private fun validateKeyword(keyword: String) {
        if (keyword.isBlank()) {
            throw ServiceException(ErrorCode.INVALID_TAG_KEYWORD)
        }
    }

    // 키워드 리스트 검증
    private fun validateKeywordList(list: List<String>) {
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceException(ErrorCode.INVALID_TAG_INPUT)
        }
    }

    // 핀 존재 여부 검증
    private fun validatePinExists(pinId: Long) {
        if (!pinRepository.existsById(pinId)) {
            throw ServiceException(ErrorCode.TAG_PIN_NOT_FOUND)
        }
    }

    // 태그 리스트 검증
    private fun validateTagList(tags: List<Tag>) {
        if (CollectionUtils.isEmpty(tags)) {
            throw ServiceException(ErrorCode.PIN_TAG_LIST_EMPTY)
        }
    }

    // 결과 핀 리스트 검증
    private fun validateResultPins(pins: List<Pin>) {
        if (CollectionUtils.isEmpty(pins)) {
            throw ServiceException(ErrorCode.TAG_POSTS_NOT_FOUND)
        }
    }

    // ===== 조회/생성 유틸 =====
    // 핀 조회
    private fun findPinById(pinId: Long): Pin {
        return pinRepository.findById(pinId)
            .orElseThrow{ ServiceException(ErrorCode.TAG_PIN_NOT_FOUND) }
    }

    // 태그 조회 또는 생성
    private fun findOrCreateTag(keyword: String): Tag {
        return tagRepository.findByKeyword(keyword) ?:tagRepository.save<Tag>(Tag(keyword))
    }

    // 핀-태그 연결 조회
    private fun findPinTagOrThrow(pinId: Long, tagId: Long): PinTag {
        return pinTagRepository.findByPin_IdAndTag_Id(pinId, tagId)?: throw ServiceException(ErrorCode.TAG_LINK_NOT_FOUND)
    }

    // 활성 태그 조회
    private fun findActiveTagsByPin(pinId: Long): List<Tag> {
        return pinTagRepository.findAllByPin_Id(pinId)
            .map(PinTag::tag)
            .toList()
    }

    // 여러 태그로 핀 조회
    private fun findPinsByTags(keywords: List<String>): List<List<Pin>> {
        return keywords
            .map { keyword: String -> this.getPinsBySingleTagKeyword(keyword) }
            .toList()
    }

    // 단일 태그 키워드로 핀 조회
    private fun getPinsBySingleTagKeyword(keyword: String): List<Pin> {
        tagRepository.findByKeyword(keyword) ?:throw ServiceException(ErrorCode.TAG_NOT_FOUND)

        val pins = pinTagRepository.findPinsByTagKeyword(keyword)
        return pins
    }

    // ===== 조작 유틸 =====
    // 기존 핀-태그 연결 처리
//    private fun handleExistingPinTag(pin: Pin, tag: Tag) {
//        val check = pinTagRepository.findByPin_IdAndTag_Id(pin, tag)
//        if(check != null)throw ServiceException(ErrorCode.TAG_ALREADY_LINKED)
//
//    }



    // 새로운 핀-태그 연결 저장
    private fun saveNewPinTag(pin: Pin, tag: Tag): PinTag {
        try {
            val pintag= pinTagRepository.save<PinTag>(PinTag(pin, tag))
            pinTagRepository.flush()
            return pintag
        } catch (e: DataIntegrityViolationException) {
            throw ServiceException(ErrorCode.TAG_ALREADY_LINKED)
        }catch (_: Exception) {
            throw ServiceException(ErrorCode.TAG_CREATE_FAILED)
        }
    }

    // 핀-태그 연결 삭제
    private fun deletePinTag(pinTag: PinTag) {
        try {
            pinTagRepository.delete(pinTag)
        } catch (_: Exception) {
            throw ServiceException(ErrorCode.PIN_TAG_DELETE_FAILED)
        }
    }

    // 태그 링크 처리
    private fun processTagLinks(pin: Pin, tagKeywords: List<String>): List<Tag> {
        val linkedTags = mutableListOf<Tag>()
        for (keyword in tagKeywords) {
            if (keyword.isBlank()) continue
            val tag = findOrCreateTag(keyword)
            pinTagRepository.save<PinTag>(PinTag(pin, tag))
            linkedTags.add(tag)
        }
        return linkedTags
    }

    // 태그별 핀 교집합 계산
    private fun intersectPins(pinsByEachTag: List<List<Pin>>): List<Pin> {
        if (CollectionUtils.isEmpty(pinsByEachTag)) {
            return emptyList()
        }

        val result = pinsByEachTag[0].toMutableList()
        pinsByEachTag.drop(1).forEach { c: List<Pin> -> result.retainAll(c) }
        return result
    }
}

