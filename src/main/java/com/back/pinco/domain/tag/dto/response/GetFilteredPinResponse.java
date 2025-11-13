package com.back.pinco.domain.tag.dto.response;

import com.back.pinco.domain.pin.dto.PinDto;
import com.back.pinco.domain.pin.entity.Pin;

public record GetFilteredPinResponse(
        PinDto pin
) {
    public GetFilteredPinResponse(Pin pin) {
        this(new PinDto(pin));
    }
}
