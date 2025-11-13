package com.back.pinco.domain.user.dto.UserResBody;

import com.back.pinco.domain.user.dto.UserDto;

public record GetInfoResponse(
        Long id,
        String email,
        String userName
){
    public GetInfoResponse(UserDto userDto) {
        this(
                userDto.id(),
                userDto.email(),
                userDto.userName()
        );
    }
}
