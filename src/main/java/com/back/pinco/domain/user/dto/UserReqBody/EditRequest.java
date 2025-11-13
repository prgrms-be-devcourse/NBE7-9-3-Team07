package com.back.pinco.domain.user.dto.UserReqBody;

public record EditRequest(
        String password,         // 현재 비밀번호 (검증용)
        String newUserName,      // 변경할 닉네임
        String newPassword       // 변경할 비밀번호
) {}
