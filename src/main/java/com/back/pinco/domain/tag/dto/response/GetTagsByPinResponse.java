package com.back.pinco.domain.tag.dto.response;

import com.back.pinco.domain.tag.dto.TagDto;

import java.util.List;

public record GetTagsByPinResponse(
        Long pinId,
        List<TagDto> tags
) {}
