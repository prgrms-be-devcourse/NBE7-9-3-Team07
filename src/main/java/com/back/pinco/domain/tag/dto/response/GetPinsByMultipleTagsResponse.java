package com.back.pinco.domain.tag.dto.response;

import java.util.List;

public record GetPinsByMultipleTagsResponse(
        List<String> keywords,
        List<GetFilteredPinResponse> pins
) {}
