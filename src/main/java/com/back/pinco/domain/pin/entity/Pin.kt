package com.back.pinco.domain.pin.entity;

import com.back.pinco.domain.pin.dto.UpdatePinContentRequest;
import com.back.pinco.domain.tag.entity.PinTag;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.global.geometry.GeometryUtil;
import com.back.pinco.global.jpa.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "pins",
        indexes = @Index(name = "idx_pin_point", columnList = "point")
)
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
        name = "pin_id_gen",
        sequenceName = "PIN_SEQ",
        initialValue = 1,
        allocationSize = 50
)
public class Pin extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pin_id_gen")
    @Column(name = "pin_id")
    private Long id;    // 고유 ID

    @Column(name = "point", nullable = false, columnDefinition = "geography(Point, " + GeometryUtil.SRID + ")")
    private Point point;    // 위치

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;    // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;    // 작성자

    @OneToMany(mappedBy = "pin", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PinTag> pinTags = new ArrayList<>();

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;    // 좋아요 수

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;    // 공개 여부

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;    // 삭제 여부


    public Pin(Point point, User user, String content) {
        this.point = point;
        this.user = user;
        this.content  = content;
    }

    // 소프트 삭제
    public void setDeleted() {
        this.deleted = true;
    }
//삭제 복구
    public void unSetDeleted() {
        this.deleted = true;
    }

    // 공개 여부 변경
    public void togglePublic() {
        this.isPublic = !this.isPublic;
    }

    public void update(UpdatePinContentRequest updatePinContentRequest) {
        this.content= updatePinContentRequest.content();
        //추가로 수정 할 수 있는 필드가 있다면 여기 추가
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
}