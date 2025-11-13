package com.back.pinco.domain.likes.dto;

/**
 * 좋아요 상태 응답 DTO
 * @param isLiked 사용자의 좋아요 여부
 * @param likeCount 해당 포스트의 총 좋아요 개수
 */
public record PinLikesResponse(
        boolean isLiked,
        int likeCount
) {};
