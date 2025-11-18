package com.back.pinco.global.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Column(name = "create_at", nullable = false, updatable = false)
    @CreatedDate
    var createdAt: LocalDateTime? = null
        protected set

    @Column(name = "create_by")
    @CreatedBy
    var createdBy: Long? = null
        protected set

    @Column(name = "modified_at")
    @LastModifiedDate
    var modifiedAt: LocalDateTime? = null
        protected set

    @Column(name = "modified_by")
    @LastModifiedBy
    var modifiedBy: Long? = null
        protected set
}
