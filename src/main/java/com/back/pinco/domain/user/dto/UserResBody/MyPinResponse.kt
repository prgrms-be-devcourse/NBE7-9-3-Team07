package com.back.pinco.domain.user.dto.UserResBody

import com.back.pinco.domain.pin.dto.PinDto


data class MyPinResponse(
    val publicPins: List<PinDto>,
    val privatePins: List<PinDto>
)
