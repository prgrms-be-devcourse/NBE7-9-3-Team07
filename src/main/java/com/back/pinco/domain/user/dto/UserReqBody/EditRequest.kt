package com.back.pinco.domain.user.dto.UserReqBody

data class EditRequest(
    val password: String,  // 현재 비밀번호 (검증용)
    val newUserName: String? = null,  // 변경할 닉네임 (선택)
    val newPassword: String? = null // 변경할 비밀번호 (선택)
)
