package com.back.pinco.domain.user.dto.UserResBody;

import com.back.pinco.domain.bookmark.dto.BookmarkDto;
import com.back.pinco.domain.pin.dto.PinDto;
import com.back.pinco.domain.user.dto.UserDto;

import java.util.List;

public record MyPageResponse(
        String email,
        String userName,
        int myPinCount,
        int bookmarkCount,
        long likesCount
) {
    public MyPageResponse(UserDto userDto,
                          int myPinCount,
                          int bookmarkCount,
                          long likesCount) {
        this(
                userDto.email(),
                userDto.userName(),
                myPinCount,
                bookmarkCount,
                likesCount
        );
    }
}

