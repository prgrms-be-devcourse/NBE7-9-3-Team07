package com.back.pinco.domain.bookmark.entity

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_bookmark_user_pin",
        columnNames = ["user_id", "pin_id"]
    )],
    indexes = [
        Index(name = "idx_bookmark_user", columnList = "user_id"),
        Index(name = "idx_bookmark_pin", columnList = "pin_id")
    ]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "bookmark_id_gen",
    sequenceName = "BOOKMARK_SEQ",
    initialValue = 1, allocationSize = 50
)
class Bookmark(
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @JoinColumn(name = "pin_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val pin: Pin
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bookmark_id_gen")
    @Column(name = "bookmark_id")
    var id: Long? = null
        protected set

    @Column(name = "is_deleted", nullable = false)
    var deleted: Boolean = false


    // 소프트 삭제
    fun setDeleted() {
        deleted = true
    }

    // 북마크 복구
    fun restore() {
        deleted = false
    }
}