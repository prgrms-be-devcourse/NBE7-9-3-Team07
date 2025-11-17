package com.back.pinco.domain.user.dto.UserResBody

import com.back.pinco.domain.pin.dto.PinDto

data class MyBookmarkResponse(
    val bookmarkList: List<PinDto>
)
