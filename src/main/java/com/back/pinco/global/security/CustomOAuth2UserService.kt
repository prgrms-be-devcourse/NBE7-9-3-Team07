package com.back.pinco.global.security

import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class CustomOAuth2UserService(
    private val userService: UserService
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private val delegate = DefaultOAuth2UserService()

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        // 카카오 사용자 정보 가져오기
        val oAuth2User: OAuth2User = delegate.loadUser(userRequest)

        // 카카오 사용자 정보 추출
        val attributes = oAuth2User.attributes
        val kakaoId = attributes["id"].toString()

        val kakaoAccount = attributes["kakao_account"] as? Map<*, *>

        val email = kakaoAccount?.get("email") as? String
            ?: "kakao_$kakaoId@kakao-temp.com"

        val profile = kakaoAccount?.get("profile") as? Map<*, *>
        val nickname = profile?.get("nickname") as? String
            ?: "kakao_user_$kakaoId"

        // User 엔티티로 변환 또는 업데이트
        val user: User = userService.modifyOrJoin(email, nickname)

        return UserPrincipal(user, attributes)
    }
}