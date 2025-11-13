package com.back.pinco.domain.likes.dto;

import com.back.pinco.domain.user.entity.User;

/**
 * 특정 핀을 좋아한 사용자 정보
 * @param id 사용자 ID
 * @param userName 사용자명
 */
public record PinLikedUserResponse(
        Long id,
        String userName
) {
    public static PinLikedUserResponse formEntry(User user) {
        return new PinLikedUserResponse(
                user.getId(),
                user.getUserName()
        );
    }

}
