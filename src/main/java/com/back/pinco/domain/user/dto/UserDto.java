package com.back.pinco.domain.user.dto;

import com.back.pinco.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String email,
        String userName,
        String password,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public UserDto(User user) {
        this(
                user.getId(),
                user.getEmail(),
                user.getUserName(),
                user.getPassword(),
                user.getCreatedAt(),
                user.getModifiedAt()
        );
    }
}

