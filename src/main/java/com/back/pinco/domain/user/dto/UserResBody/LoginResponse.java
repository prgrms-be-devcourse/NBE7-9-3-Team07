package com.back.pinco.domain.user.dto.UserResBody;

import com.back.pinco.domain.user.dto.UserDto;

public record LoginResponse(
        Long id,
        String email,
        String userName
) {
    public LoginResponse(UserDto userDto) {
        this(
                userDto.id(),
                userDto.email(),
                userDto.userName()
        );
    }
}
