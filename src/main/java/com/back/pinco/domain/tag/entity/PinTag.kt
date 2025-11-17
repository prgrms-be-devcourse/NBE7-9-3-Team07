package com.back.pinco.domain.tag.entity

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.global.jpa.entity.BaseEntity
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "pin_tags",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_pin_tag",
        columnNames = ["pin_id", "tag_id"]
    )],
    indexes = [
        Index(name = "idx_pin_tag_pin", columnList = "pin_id"),
        Index(name = "idx_pin_tag_tag",columnList = "tag_id"),
        Index(name = "idx_pin_tag_deleted", columnList = "is_deleted")
    ]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "pin_tag_id_gen",
    sequenceName = "PIN_TAG_SEQ",
    initialValue = 1, allocationSize = 50
)
class PinTag(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    @JsonIgnore
    var pin: Pin,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    var tag: Tag
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pin_tag_id_gen")
    @Column(name = "pin_tag_id")
    var id: Long? = null // 고유 ID
        protected set
}