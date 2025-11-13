package com.back.pinco.domain.likes.entity;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_like_user_pin",
                columnNames = {"user_id", "pin_id"}
        ),
        indexes = {
                @Index(name = "idx_like_user", columnList = "user_id"),
                @Index(name = "idx_like_pin", columnList = "pin_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
        name = "like_id_gen",
        sequenceName = "LIKE_SEQ",
        initialValue = 1,
        allocationSize = 50
)
public class Likes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "like_id_gen")
    @Column(name = "like_id")
    private Long id;    // 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;    // 핀 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;    // 사용자 ID

    public Likes(Pin pin, User user) {
        this.pin = pin;
        this.user = user;
    }

    @Profile("test")
    @Override
    public String toString() {
        return "Likes{" +
                "id=" + id +
                ", pinId=" + (pin != null ? pin.getId() : null) +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }

}