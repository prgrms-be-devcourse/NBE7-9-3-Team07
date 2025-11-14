package com.back.pinco.domain.pin.entity

import com.back.pinco.domain.pin.dto.PinUpdateRequest
import com.back.pinco.domain.tag.entity.PinTag
import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.geometry.GeometryUtil
import com.back.pinco.global.jpa.entity.BaseEntity
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "pins",
    indexes = [Index(name = "idx_pin_point", columnList = "point")]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "pin_id_gen",
    sequenceName = "PIN_SEQ",
    initialValue = 1, allocationSize = 50
)
class Pin(
    @Column(name = "point", nullable = false, columnDefinition = "geography(Point, ${GeometryUtil.SRID})")
    val point: Point,

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @Column(name = "content", columnDefinition = "TEXT")
    var content: String
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pin_id_gen")
    @Column(name = "pin_id")
    var id: Long? = null
        protected set

    @OneToMany(mappedBy = "pin", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnore
    val pinTags: MutableList<PinTag> = mutableListOf<PinTag>()

    @Column(name = "like_count", nullable = false)
    var likeCount = 0 // 좋아요 수

    @Column(name = "is_public", nullable = false)
    var isPublic = true // 공개 여부

    @Column(name = "is_deleted", nullable = false)
    var deleted = false // 삭제 여부
        protected  set


    // 소프트 삭제
    fun setDeleted() {
        deleted = true
    }

    //삭제 복구
    fun unSetDeleted() {
        deleted = false
    }

    // 공개 여부 변경
    fun togglePublic() {
        isPublic = !isPublic
    }

    fun update(updatePinContentRequest: PinUpdateRequest) {
        content = updatePinContentRequest.content
        //추가로 수정 할 수 있는 필드가 있다면 여기 추가
    }

}