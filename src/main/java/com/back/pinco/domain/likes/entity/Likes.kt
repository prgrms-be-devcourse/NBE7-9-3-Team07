package com.back.pinco.domain.likes.entity

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_like_user_pin",
        columnNames = ["user_id", "pin_id"]
    )],
    indexes = [
        Index(name = "idx_like_user", columnList = "user_id"),
        Index(name = "idx_like_pin", columnList = "pin_id")
    ]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "like_id_gen",
    sequenceName = "LIKE_SEQ",
    initialValue = 1, allocationSize = 50
)
class Likes(
    @JoinColumn(name = "pin_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val pin: Pin,

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "like_id_gen")
    @Column(name = "like_id")
    var id: Long? = null
        protected set
}