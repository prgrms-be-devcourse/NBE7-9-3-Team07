package com.back.pinco.domain.tag.dto.response

import com.back.pinco.domain.pin.dto.PinDto
import com.back.pinco.domain.pin.entity.Pin

data class GetFilteredPinResponse(
    val pin: PinDto
) {
    constructor(pin: Pin) : this(PinDto(pin))
}
