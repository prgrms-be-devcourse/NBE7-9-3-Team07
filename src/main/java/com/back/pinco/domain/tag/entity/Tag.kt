package com.back.pinco.domain.tag.entity

import com.back.pinco.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "tags",
    indexes = [Index(name = "idx_tag_keyword", columnList = "keyword")]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "tag_id_gen",
    sequenceName = "TAG_SEQ",
    initialValue = 1, allocationSize = 50
)
class Tag(
    @Column(name = "keyword", nullable = false, unique = true, length = 50)
    var keyword: String
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_id_gen")
    @Column(name = "tag_id")
    var id: Long? = null // 고유 ID
        protected set

}