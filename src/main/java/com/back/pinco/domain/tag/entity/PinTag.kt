package com.back.pinco.domain.tag.entity;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.global.jpa.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "pin_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pin_tag",
                columnNames = {"pin_id", "tag_id"}
        ),
        indexes = {
                @Index(name = "idx_pin_tag_pin", columnList = "pin_id"),
                @Index(name = "idx_pin_tag_tag", columnList = "tag_id"),
                @Index(name = "idx_pin_tag_deleted", columnList = "is_deleted")
        }
)
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
        name = "pin_tag_id_gen",
        sequenceName = "PIN_TAG_SEQ",
        initialValue = 1,
        allocationSize = 50
)
public class PinTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pin_tag_id_gen")
    @Column(name = "pin_tag_id")
    private Long id;    // 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    @JsonIgnore
    private Pin pin;    // 핀 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;    // 태그 ID

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;    // 삭제 여부


    public PinTag(Pin pin, Tag tag) {
        this.pin = pin;
        this.tag = tag;
    }

    // 소프트 삭제
    public void setDeleted() {
        this.deleted = true;
    }

    // 태그 복구
    public void restore() {
        this.deleted = false;
    }

    public PinTag(Pin pin, Tag tag, boolean deleted) {
        this.pin = pin;
        this.tag = tag;
        this.deleted = deleted;
    }
}