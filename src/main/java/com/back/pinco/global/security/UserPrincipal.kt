package com.back.pinco.global.security

import com.back.pinco.domain.user.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class UserPrincipal(
    val user: User,
    private val attributesMap: Map<String, Any>
) : OAuth2User {

    override fun getAttributes(): MutableMap<String, Any> = attributesMap.toMutableMap()

    override fun getAuthorities(): MutableCollection<GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
    }

    override fun getName(): String = user.id?.toString() ?: user.email
}

