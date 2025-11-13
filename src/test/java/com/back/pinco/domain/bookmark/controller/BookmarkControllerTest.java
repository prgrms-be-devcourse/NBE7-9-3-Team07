package com.back.pinco.domain.bookmark.controller;

import com.back.pinco.domain.bookmark.entity.Bookmark;
import com.back.pinco.domain.bookmark.repository.BookmarkRepository;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.repository.PinRepository;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookmarkControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PinRepository pinRepository;
    @Autowired
    private BookmarkRepository bookmarkRepository;

    private final long failedTargetId = Long.MAX_VALUE;

    /**
     * 특정 사용자(user)로 인증된 MockHttpServletRequestBuilder를 반환하는 헬퍼 메서드
     * @param user 인증할 사용자 객체
     * @return Authorization 헤더가 추가된 String
     */
    private String getAuthHeader(User user) {
        return "Bearer %s".formatted(user.getApiKey());
    }

    private Pin findPinByContent(String content) {
        return pinRepository.findAll().stream()
                .filter(p -> content.equals(p.getContent()))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("t1_1. 북마크 생성 성공")
    void t1_1() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        // user1은 pinA, pinD를 북마크 하고 있음. pinC는 북마크 하지 않음.
        Pin pinC = findPinByContent("청계천 산책로 발견 👣");
        Long targetPinId = pinC.getId();

        String jsonContent = """
                                {
                                  "pinId": %d
                                }
                            """.formatted(targetPinId);

        ResultActions resultActions = mvc.perform(
                post("/api/pins/{pinId}/bookmarks", targetPinId)
                        .header("Authorization", getAuthHeader(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.pin.id").value(targetPinId.intValue()));

        assertThat(bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinC)).isPresent();
    }

    @Test
    @DisplayName("t1_2. 북마크 생성 실패 (이미 북마크된 핀)")
    void t1_2() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        // user1은 pinA를 이미 북마크 하고 있음
        Pin pinA = findPinByContent("서울 시청 근처 카페 ☕");
        Long targetPinId = pinA.getId();

        String jsonContent = """
                                {
                                  "pinId": %d
                                }
                            """.formatted(targetPinId);

        ResultActions resultActions = mvc.perform(
                post("/api/pins/{pinId}/bookmarks", targetPinId)
                        .header("Authorization", getAuthHeader(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
        ).andDo(print());

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("4002"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("t1_3. 북마크 생성 실패 (인증되지 않은 사용자)")
    void t1_3() throws Exception {
        Pin pinA = findPinByContent("서울 시청 근처 카페 ☕");
        Long targetPinId = pinA.getId();

        String jsonContent = """
                                {
                                  "pinId": %d
                                }
                            """.formatted(targetPinId);

        // Authorization 헤더 없이 요청
        ResultActions resultActions = mvc.perform(
                post("/api/pins/{pinId}/bookmarks", targetPinId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
        ).andDo(print());

        // 인증되지 않은 요청은 403 Forbidden
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("t1_4. 북마크 생성 실패 (존재하지 않는 핀 ID)")
    void t1_4() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        Long targetPinId = failedTargetId;

        String jsonContent = """
                                {
                                  "pinId": %d
                                }
                            """.formatted(targetPinId);

        ResultActions resultActions = mvc.perform(
                post("/api/pins/{pinId}/bookmarks", targetPinId)
                        .header("Authorization", getAuthHeader(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
        ).andDo(print());

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("1002"))
                .andExpect(jsonPath("$.msg").exists());
    }


    @Test
    @DisplayName("t2_1. 나의 북마크 목록 조회 성공")
    void t2_1() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();

        ResultActions resultActions = mvc.perform(
                get("/api/bookmarks")
                        .header("Authorization", getAuthHeader(user1))
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.data.length()").value(2)); // pinA, pinD
    }

    @Test
    @DisplayName("t2_2. 나의 북마크 목록 조회 성공 (북마크 없음)")
    void t2_2() throws Exception {
        User user2 = userRepository.findByEmail("user2@example.com").orElseThrow();

        ResultActions resultActions = mvc.perform(
                get("/api/bookmarks")
                        .header("Authorization", getAuthHeader(user2))
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.data.length()").value(1)); // pinB
    }

    @Test
    @DisplayName("t2_3. 나의 북마크 목록 조회 실패 (인증되지 않은 사용자)")
    void t2_3() throws Exception {
        ResultActions resultActions = mvc.perform(
                get("/api/bookmarks")
        ).andDo(print());

        resultActions.andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("t3_1. 북마크 삭제 성공 (soft delete)")
    void t3_1() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        Pin pinA = findPinByContent("서울 시청 근처 카페 ☕");
        Bookmark bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA).orElseThrow();

        Long targetBookmarkId = bookmark1A.getId();

        ResultActions resultActions = mvc.perform(
                delete("/api/bookmarks/{bookmarkId}", targetBookmarkId)
                        .header("Authorization", getAuthHeader(user1))
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"));

        Bookmark deletedBookmark = bookmarkRepository.findById(targetBookmarkId).orElseThrow();
        assertThat(deletedBookmark.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("t3_2. 북마크 삭제 실패 (존재하지 않는 북마크 ID)")
    void t3_2() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();

        ResultActions resultActions = mvc.perform(
                delete("/api/bookmarks/{bookmarkId}", failedTargetId)
                        .header("Authorization", getAuthHeader(user1))
        ).andDo(print());

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("4001"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("t3_3. 북마크 삭제 실패 (소유자가 아님)")
    void t3_3() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        User user2 = userRepository.findByEmail("user2@example.com").orElseThrow();
        Pin pinA = findPinByContent("서울 시청 근처 카페 ☕");
        Bookmark bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA).orElseThrow();

        Long targetBookmarkId = bookmark1A.getId();

        // user2가 user1의 북마크 삭제 시도
        ResultActions resultActions = mvc.perform(
                delete("/api/bookmarks/{bookmarkId}", targetBookmarkId)
                        .header("Authorization", getAuthHeader(user2)) // user2로 인증
        ).andDo(print());

        //소유자 체크 실패 시 Not Found 반환
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("4001"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("t3_4. 북마크 복원 성공")
    void t3_4() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        Pin pinA = findPinByContent("서울 시청 근처 카페 ☕");

        // 기존 북마크를 삭제 상태로 만들어 놓기
        Bookmark bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA)
                .orElseThrow(() -> new RuntimeException("Test setup failed: Bookmark not found"));
        bookmark1A.setDeleted();
        bookmarkRepository.save(bookmark1A);

        Long targetBookmarkId = bookmark1A.getId();

        ResultActions resultActions = mvc.perform(
                patch("/api/bookmarks/{bookmarkId}", targetBookmarkId)
                        .header("Authorization", getAuthHeader(user1))
        ).andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"));

        Bookmark restored = bookmarkRepository.findById(targetBookmarkId).orElseThrow();
        assertThat(restored.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("t3_5. 북마크 복원 실패 (존재하지 않는 북마크 ID)")
    void t3_5() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();

        ResultActions resultActions = mvc.perform(
                patch("/api/bookmarks/{bookmarkId}", failedTargetId)
                        .header("Authorization", getAuthHeader(user1))
        ).andDo(print());

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("4001"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("t3_6. 북마크 복원 실패 (소유자가 아님)")
    void t3_6() throws Exception {
        User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        User user2 = userRepository.findByEmail("user2@example.com").orElseThrow();
        Pin pinA = findPinByContent("서울 시청 근처 카페 ☕");

        // 기존 북마크를 삭제 상태로 만들어 놓기
        Bookmark bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA)
                .orElseThrow(() -> new RuntimeException("Test setup failed: Bookmark not found"));
        bookmark1A.setDeleted();
        bookmarkRepository.save(bookmark1A);

        Long targetBookmarkId = bookmark1A.getId();

        // user2가 user1의 북마크 복원 시도
        ResultActions resultActions = mvc.perform(
                patch("/api/bookmarks/{bookmarkId}", targetBookmarkId)
                        .header("Authorization", getAuthHeader(user2)) // user2로 인증
        ).andDo(print());

        // 소유자 체크 실패 시 Not Found 반환
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("4001"))
                .andExpect(jsonPath("$.msg").exists());
    }
}
