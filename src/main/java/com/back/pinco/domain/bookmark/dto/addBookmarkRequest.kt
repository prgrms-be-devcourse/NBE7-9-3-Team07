package com.back.pinco.domain.bookmark.dto;

/**
 * 북마크 생성 요청 DTO
 * @param pinId 핀 ID
 */
public record addBookmarkRequest(
        Long pinId
) {}