package com.back.pinco.domain.tag.dto;

import com.back.pinco.domain.tag.entity.PinTag;
import java.time.LocalDateTime;

public record PinTagDto(
        Long id,
        Long pinId,
        TagDto tag,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public PinTagDto(PinTag pinTag) {
        this(
                pinTag.getId(),
                pinTag.getPin().getId(),
                new TagDto(pinTag.getTag()),
                pinTag.getDeleted(),
                pinTag.getCreatedAt(),
                pinTag.getModifiedAt()
        );
    }
}
