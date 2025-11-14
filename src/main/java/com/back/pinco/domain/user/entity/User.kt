package com.back.pinco.domain.user.entity

import com.back.pinco.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_user_email", columnList = "email")]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "user_id_gen",
    sequenceName = "USER_SEQ",
    initialValue = 1, allocationSize = 50
)
class User(// 이메일
    @Column( name = "email", nullable = false, unique = true, length = 100)
    val email: String,

    @Column(name = "password", nullable = false)
    var password: String,

    @Column(name = "username", nullable = false, length = 50)
    var userName: String
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_gen")
    @Column(name = "user_id")
    var id: Long? = null

    @Column(name = "api_key", unique = true, length = 64)
    var apiKey: String? = null

    // ✅ 삭제 여부 변경 (setter)
    // ✅ 삭제 여부 확인 (선택)
    // ✅ 소프트 삭제용 필드 추가
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false // 기본값 false (삭제되지 않음)

    // 사용자명 변경
    fun updateUserName(userName: String) {
        this.userName = userName
    }

    // 비밀번호 변경
    fun updatePassword(password: String) {
        this.password = password
    }

    // ✅ 소프트 삭제 수행 (편의 메서드)
    fun softDelete() {
        isDeleted = true
    }
}
