package com.back.pinco.domain.tag.dto.response;

import com.back.pinco.domain.tag.dto.PinTagDto;

public record AddTagToPinResponse(
        Long pinId,
        PinTagDto pinTag
) {}