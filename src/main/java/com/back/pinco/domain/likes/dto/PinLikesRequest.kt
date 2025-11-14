package com.back.pinco.domain.likes.dto;

import jakarta.validation.constraints.NotNull;

public record PinLikesRequest(
        @NotNull
        Long userId
) {
}
