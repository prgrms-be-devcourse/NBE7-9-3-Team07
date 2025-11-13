package com.back.pinco.domain.tag.dto;

import com.back.pinco.domain.tag.entity.Tag;
import java.time.LocalDateTime;

public record TagDto(
        Long id,
        String keyword,
        LocalDateTime createdAt
) {
    public TagDto(Tag tag) {
        this(
                tag.getId(),
                tag.getKeyword(),
                tag.getCreatedAt()
        );
    }
}
