package com.back.pinco.domain.user.dto.UserResBody;

import com.back.pinco.domain.user.dto.UserDto;

import java.time.LocalDateTime;

public record JoinResponse(
        Long id,
        String email,
        String userName,
        LocalDateTime createdAt
) {
    public JoinResponse(UserDto userDto) {
        this(
                userDto.id(),
                userDto.email(),
                userDto.userName(),
                userDto.createdAt()
        );
    }
}

