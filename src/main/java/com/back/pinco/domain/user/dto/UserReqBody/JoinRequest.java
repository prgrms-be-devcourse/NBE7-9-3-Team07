package com.back.pinco.domain.user.dto.UserReqBody;

public record JoinRequest(
        String email,
        String password,
        String userName
) {}
