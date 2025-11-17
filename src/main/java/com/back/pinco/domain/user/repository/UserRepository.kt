package com.back.pinco.domain.user.repository

import com.back.pinco.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByUserName(userName: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUserName(userName: String): Boolean
    fun existsByUserNameAndIdNot(userName: String, id: Long): Boolean
    fun findByApiKey(apiKey: String): User?
    fun existsByApiKey(key: String): Boolean
}
