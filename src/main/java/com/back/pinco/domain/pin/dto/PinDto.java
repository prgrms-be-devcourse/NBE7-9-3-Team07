package com.back.pinco.domain.pin.dto;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.tag.dto.PinTagDto;
import com.back.pinco.domain.tag.dto.TagDto;
import com.back.pinco.domain.tag.entity.PinTag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


public record PinDto(
        Long id,
        Double latitude,
        Double longitude,
        String content,
        Long userId,
        List<TagDto> pinTags,
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
                        .map(tag -> new TagDto(tag.getId(), tag.getKeyword(), tag.getCreatedAt()))
                        .collect(Collectors.toList()),
                pin.getLikeCount(),
                pin.getIsPublic(),
                pin.getCreatedAt(),
                pin.getModifiedAt()
        );
    }



}
