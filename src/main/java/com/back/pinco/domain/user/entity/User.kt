package com.back.pinco.domain.user.entity;

import com.back.pinco.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(
        name = "users",
        indexes = @Index(name = "idx_user_email", columnList = "email")
)
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
        name = "user_id_gen",
        sequenceName = "USER_SEQ",
        initialValue = 1,
        allocationSize = 50
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_gen")
    @Column(name = "user_id")
    private Long id;    // 고유 ID

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;    // 이메일

    @Column(name = "password", nullable = false)
    private String password;    // 비밀번호

    @Column(name = "username", nullable = false, length = 50)
    private String userName;    // 사용자명

    @Column(name = "api_key", unique = true, length = 64)
    private String apiKey; // apiKey

    // ✅ 소프트 삭제용 필드 추가
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;   // 기본값 false (삭제되지 않음)

    public User(String email, String password, String userName) {
        this.email = email;
        this.password = password;
        this.userName = userName;
    }

    // 사용자명 변경
    public void updateUserName(String userName) {
        this.userName = userName;
    }

    // 비밀번호 변경
    public void updatePassword(String password) {
        this.password = password;
    }

    // ✅ 삭제 여부 변경 (setter)
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // ✅ 소프트 삭제 수행 (편의 메서드)
    public void softDelete() {
        this.deleted = true;
    }

    // ✅ 삭제 여부 확인 (선택)
    public boolean isDeleted() {
        return deleted;
    }
}
