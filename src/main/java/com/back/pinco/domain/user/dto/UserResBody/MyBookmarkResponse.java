package com.back.pinco.domain.user.dto.UserResBody;

import com.back.pinco.domain.pin.dto.PinDto;

import java.util.List;

public record MyBookmarkResponse(
        List<PinDto> bookmarkList
) {
}
