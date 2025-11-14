package com.back.pinco.domain.bookmark.repository;

import com.back.pinco.domain.bookmark.entity.Bookmark;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 특정 사용자의 삭제되지 않은 북마크 목록 조회
     *
     * @param user 사용자 엔티티
     * @return 삭제되지 않은 북마크 목록
     */
    List<Bookmark> findByUserAndDeletedFalse(User user);

    /**
     * 특정 사용자가 특정 핀을 북마크했는지 확인 (삭제 여부와 관계없이)
     *
     * @param user 사용자 엔티티
     * @param pin 핀 엔티티
     * @return 북마크가 존재하면 Optional에 담아 반환, 없으면 빈 Optional 반환
     */
    Optional<Bookmark> findByUserAndPin(User user, Pin pin);

    /**
     * 특정 사용자가 특정 핀을 이미 북마크했는지 확인 (삭제되지 않은 것만)
     *
     * @param user 사용자 엔티티
     * @param pin 핀 엔티티
     * @return 삭제되지 않은 북마크가 존재하면 Optional에 담아 반환, 없으면 빈 Optional 반환
     */
    Optional<Bookmark> findByUserAndPinAndDeletedFalse(User user, Pin pin);
}
