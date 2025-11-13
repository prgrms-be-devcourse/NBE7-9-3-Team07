package com.back.pinco.domain.tag.dto.response;

public record RemoveTagFromPinResponse(
        Long pinId,
        Long tagId
) {
}
