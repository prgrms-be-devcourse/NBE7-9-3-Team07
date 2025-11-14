package com.back.pinco.domain.pin.dto;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.tag.dto.PinTagDto;
import com.back.pinco.domain.tag.dto.TagDto;
import com.back.pinco.domain.tag.entity.PinTag;
import com.back.pinco.domain.tag.entity.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


public record PinDto(
        Long id,
        Double latitude,
        Double longitude,
        String content,
        Long userId,
        List<String> pinTags,
        int likeCount,
        Boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public PinDto(Pin pin) {
        this(
                pin.getId(),
                pin.getPoint().getY(),
                pin.getPoint().getX(),
                pin.getContent(),
                pin.getUser().getId(),
                pin.getPinTags().stream()
                        .map(PinTag::getTag)
                        .map(Tag::getKeyword)
                        .collect(Collectors.toList()),
                pin.getLikeCount(),
                pin.isPublic(),
                pin.getCreatedAt(),
                pin.getModifiedAt()
        );
    }



}
