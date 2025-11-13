package com.back.pinco.domain.bookmark.entity;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bookmark_user_pin",
                columnNames = {"user_id", "pin_id"}
        ),
        indexes = {
                @Index(name = "idx_bookmark_user", columnList = "user_id"),
                @Index(name = "idx_bookmark_pin", columnList = "pin_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
        name = "bookmark_id_gen",
        sequenceName = "BOOKMARK_SEQ",
        initialValue = 1,
        allocationSize = 50
)public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bookmark_id_gen")
    @Column(name = "bookmark_id")
    private Long id;    // 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;    // 사용자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;    // 핀 ID

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false; // 삭제 여부

    /**
     * 북마크 생성자
     * @param user 사용자
     * @param pin 핀
     */
    public Bookmark(User user, Pin pin) {
        this.user = user;
        this.pin = pin;
    }

    // 소프트 삭제
    public void setDeleted() {
        this.deleted = true;
    }

    // 북마크 복구
    public void restore() {
        if (this.deleted) {
            this.deleted = false;
        }
    }

}