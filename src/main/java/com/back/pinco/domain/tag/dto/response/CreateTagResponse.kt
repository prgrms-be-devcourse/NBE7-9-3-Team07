package com.back.pinco.domain.tag.dto.response;

import com.back.pinco.domain.tag.dto.TagDto;

public record CreateTagResponse(
        TagDto tag
) {
}