package com.back.pinco.domain.user.dto.UserReqBody;

public record LoginRequest(
        String email,
        String password
) {}
