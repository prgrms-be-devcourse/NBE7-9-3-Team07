package com.back.pinco.domain.user.dto.UserResBody;

import com.back.pinco.domain.pin.dto.PinDto;
import com.back.pinco.domain.pin.entity.Pin;

import java.util.List;

public record MyPinResponse(
        List<PinDto> publicPins,
        List<PinDto> privatePins
) {
}
