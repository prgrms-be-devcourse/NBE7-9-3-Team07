package com.back.pinco.domain.tag.service;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.repository.PinRepository;
import com.back.pinco.domain.tag.entity.PinTag;
import com.back.pinco.domain.tag.entity.Tag;
import com.back.pinco.domain.tag.repository.PinTagRepository;
import com.back.pinco.domain.tag.repository.TagRepository;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PinTagService {

    private final TagRepository tagRepository;
    private final PinTagRepository pinTagRepository;
    private final PinRepository pinRepository;

    /* =====================================================
        주요 비즈니스 로직 (Public Method)
    ====================================================== */

    // 핀에 태그 연결
    @Transactional
    public PinTag addTagToPin(Long pinId, String keyword) {
        validateKeyword(keyword); // 입력값 검증
        Pin pin = findPinById(pinId); // 핀 존재 여부 검증
        Tag tag = findOrCreateTag(keyword); // 태그 조회 또는 생성
        handleExistingPinTag(pinId, tag); // 기존 연결 처리
        return saveNewPinTag(pin, tag); // 새로운 핀-태그 연결 저장
    }

    // 핀에 연결된 태그 조회
    @Transactional(readOnly = true)
    public List<Tag> getTagsByPin(Long pinId) {
        validatePinExists(pinId); // 핀 존재 여부 검증
        List<Tag> tags = findActiveTagsByPin(pinId); // 활성 태그 조회
        validateTagList(tags); // 결과 검증
        return tags; // 태그 목록 반환
    }

    // 태그 삭제
    @Transactional
    public void removeTagFromPin(Long pinId, Long tagId) {
        PinTag pinTag = findPinTagOrThrow(pinId, tagId); // 핀-태그 연결 조회
        deletePinTag(pinTag); // 핀-태그 연결 삭제
    }

    // 태그 복구
    @Transactional
    public void restoreTagFromPin(Long pinId, Long tagId) {
        PinTag pinTag = findPinTagOrThrow(pinId, tagId); // 핀-태그 연결 조회
        validateDeletedState(pinTag); // 삭제 상태 검증
        restorePinTag(pinTag); // 핀-태그 연결 복구
    }

    // 여러 태그를 핀에 연결(PinController용)
    @Transactional
    public List<Tag> linkTagsToPin(Long pinId, List<String> tagKeywords) {
        validateKeywordList(tagKeywords); // 입력값 검증
        Pin pin = findPinById(pinId); // 핀 존재 여부 검증
        return processTagLinks(pin, tagKeywords); // 태그 연결 처리
    }

    // 여러 태그로 핀 조회
    @Transactional(readOnly = true)
    public List<Pin> getPinsByMultipleTagKeywords(List<String> keywords) {
        validateKeywordList(keywords); // 입력값 검증
        List<List<Pin>> pinsByEachTag = findPinsByTags(keywords); // 태그별 핀 조회
        List<Pin> intersection = intersectPins(pinsByEachTag); // 교집합 계산
        validateResultPins(intersection); // 결과 검증
        return intersection; // 핀 목록 반환
    }

    // 초기 데이터용 핀-태그 연결 생성
    @Transactional
    public PinTag createPinTag(Pin pin, Tag tag) {
        if (ObjectUtils.isEmpty(pin) || ObjectUtils.isEmpty(tag)) {
            throw new ServiceException(ErrorCode.INVALID_TAG_INPUT);
        }
        return pinTagRepository.save(new PinTag(pin, tag));
    }

    /* =====================================================
        내부 세부 로직 (Private Helper Methods)
    ====================================================== */

    // ===== 검증 유틸 =====

    // 입력값 검증
    private void validateKeyword(String keyword) {
        if (io.micrometer.common.util.StringUtils.isBlank(keyword)) {
            throw new ServiceException(ErrorCode.INVALID_TAG_KEYWORD);
        }
    }

    // 키워드 리스트 검증
    private void validateKeywordList(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            throw new ServiceException(ErrorCode.INVALID_TAG_INPUT);
        }
    }

    // 핀 존재 여부 검증
    private void validatePinExists(Long pinId) {
        if (!pinRepository.existsById(pinId)) {
            throw new ServiceException(ErrorCode.TAG_PIN_NOT_FOUND);
        }
    }

    // 태그 리스트 검증
    private void validateTagList(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new ServiceException(ErrorCode.PIN_TAG_LIST_EMPTY);
        }
    }

    // 삭제 상태 검증
    private void validateDeletedState(PinTag pinTag) {
        if (Boolean.FALSE.equals(pinTag.getDeleted())) {
            throw new ServiceException(ErrorCode.TAG_ALREADY_LINKED);
        }
    }

    // 결과 핀 리스트 검증
    private void validateResultPins(List<Pin> pins) {
        if (CollectionUtils.isEmpty(pins)) {
            throw new ServiceException(ErrorCode.TAG_POSTS_NOT_FOUND);
        }
    }

    // ===== 조회/생성 유틸 =====

    // 핀 조회
    private Pin findPinById(Long pinId) {
        return pinRepository.findById(pinId)
                .orElseThrow(() -> new ServiceException(ErrorCode.TAG_PIN_NOT_FOUND));
    }

    // 태그 조회 또는 생성
    private Tag findOrCreateTag(String keyword) {
        return tagRepository.findByKeyword(keyword)
                .orElseGet(() -> tagRepository.save(new Tag(keyword)));
    }

    // 핀-태그 연결 조회
    private PinTag findPinTagOrThrow(Long pinId, Long tagId) {
        return pinTagRepository.findByPin_IdAndTag_Id(pinId, tagId)
                .orElseThrow(() -> new ServiceException(ErrorCode.TAG_LINK_NOT_FOUND));
    }

    // 활성 태그 조회
    private List<Tag> findActiveTagsByPin(Long pinId) {
        return pinTagRepository.findAllByPin_IdAndDeletedFalse(pinId)
                .stream()
                .map(PinTag::getTag)
                .toList();
    }

    // 여러 태그로 핀 조회
    private List<List<Pin>> findPinsByTags(List<String> keywords) {
        return keywords.stream()
                .map(this::getPinsBySingleTagKeyword)
                .toList();
    }

    // 단일 태그 키워드로 핀 조회
    private List<Pin> getPinsBySingleTagKeyword(String keyword) {
        tagRepository.findByKeyword(keyword)
                .orElseThrow(() -> new ServiceException(ErrorCode.TAG_NOT_FOUND));

        List<Pin> pins = pinTagRepository.findPinsByTagKeyword(keyword);
        if (CollectionUtils.isEmpty(pins)) {
            throw new ServiceException(ErrorCode.PIN_TAG_LIST_EMPTY);
        }
        return pins;
    }

    // ===== 조작 유틸 =====

    // 기존 핀-태그 연결 처리
    private void handleExistingPinTag(Long pinId, Tag tag) {
        var existing = pinTagRepository.findByPin_IdAndTag_Id(pinId, tag.getId());
        if (existing.isPresent()) {
            PinTag pinTag = existing.get();
            if (pinTag.getDeleted()) {
                pinTag.restore();
                pinTagRepository.save(pinTag);
            } else {
                throw new ServiceException(ErrorCode.TAG_ALREADY_LINKED);
            }
        }
    }

    // 새로운 핀-태그 연결 저장
    private PinTag saveNewPinTag(Pin pin, Tag tag) {
        try {
            return pinTagRepository.save(new PinTag(pin, tag, false));
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.TAG_CREATE_FAILED);
        }
    }

    // 핀-태그 연결 삭제
    private void deletePinTag(PinTag pinTag) {
        try {
            pinTag.setDeleted();
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.PIN_TAG_DELETE_FAILED);
        }
    }

    // 핀-태그 연결 복구
    private void restorePinTag(PinTag pinTag) {
        try {
            pinTag.restore();
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.PIN_TAG_RESTORE_FAILED);
        }
    }

    // 태그 링크 처리
    private List<Tag> processTagLinks(Pin pin, List<String> tagKeywords) {
        List<Tag> linkedTags = new ArrayList<>();
        for (String keyword : tagKeywords) {
            if (io.micrometer.common.util.StringUtils.isBlank(keyword)) continue;
            Tag tag = findOrCreateTag(keyword);
            linkOrRestoreTag(pin, tag);
            linkedTags.add(tag);
        }
        return linkedTags;
    }

    // 태그 링크 또는 복구
    private void linkOrRestoreTag(Pin pin, Tag tag) {
        pinTagRepository.findByPin_IdAndTag_Id(pin.getId(), tag.getId())
                .ifPresentOrElse(
                        existing -> {
                            if (Boolean.TRUE.equals(existing.getDeleted())) existing.restore();
                        },
                        () -> pinTagRepository.save(new PinTag(pin, tag, false))
                );
    }

    // 태그별 핀 교집합 계산
    private List<Pin> intersectPins(List<List<Pin>> pinsByEachTag) {
        if (CollectionUtils.isEmpty(pinsByEachTag)) {
            return Collections.emptyList();
        }

        List<Pin> result = new ArrayList<>(pinsByEachTag.get(0));
        pinsByEachTag.stream().skip(1).forEach(result::retainAll);
        return result;
    }
}

