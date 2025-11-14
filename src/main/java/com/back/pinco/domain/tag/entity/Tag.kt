package com.back.pinco.domain.tag.entity;

import com.back.pinco.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "tags",
        indexes = {
                @Index(name = "idx_tag_keyword", columnList = "keyword")
        }
)
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
        name = "tag_id_gen",
        sequenceName = "TAG_SEQ",
        initialValue = 1,
        allocationSize = 50
)
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_id_gen")
    @Column(name = "tag_id")
    private Long id;    // 고유 ID

    @Column(name = "keyword", nullable = false, unique = true, length = 50)
    private String keyword;    // 키워드


    public Tag(String keyword) {
        this.keyword = keyword;
    }
    
    public Tag(Long id, String keyword) { //태그 생성용
        this.id = id;
        this.keyword = keyword;
    }

}