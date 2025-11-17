package com.back.pinco.domain.bookmark.controller

import com.back.pinco.domain.bookmark.repository.BookmarkRepository
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookmarkControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var pinRepository: PinRepository

    @Autowired
    private lateinit var bookmarkRepository: BookmarkRepository

    private val failedTargetId = Long.MAX_VALUE

    private fun getAuthHeader(user: User): String {
        return "Bearer ${user.apiKey}"
    }

    private fun findPinByContent(content: String): Pin {
        return pinRepository.findAll()
            .first { p: Pin -> content == p.content }
    }

    @Test
    @DisplayName("t1_1. 북마크 생성 성공")
    fun t1_1() {
        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
        val pinC = findPinByContent("청계천 산책로 발견 👣")
        val targetPinId = pinC.id

        val jsonContent: String = """
                                {
                                  "pinId": $targetPinId
                                }
                            
                            """.trimIndent()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/{pinId}/bookmarks", targetPinId)
                .header("Authorization", getAuthHeader(user1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pin.id").value(targetPinId!!.toInt()))

        Assertions.assertThat(bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinC)).isNotNull
    }

    @Test
    @DisplayName("t1_2. 북마크 생성 실패 (이미 북마크된 핀)")
    fun t1_2() {
        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
        val pinA = findPinByContent("서울 시청 근처 카페 ☕")
        val targetPinId = pinA.id

        val jsonContent: String = """
                                {
                                  "pinId": $targetPinId
                                }
                            
                            """.trimIndent()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/{pinId}/bookmarks", targetPinId)
                .header("Authorization", getAuthHeader(user1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("4002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("t1_3. 북마크 생성 실패 (인증되지 않은 사용자)")
    fun t1_3() {
        val pinA = findPinByContent("서울 시청 근처 카페 ☕")
        val targetPinId = pinA.id

        val jsonContent: String = """
                                {
                                  "pinId": $targetPinId
                                }
                            
                            """.trimIndent()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/{pinId}/bookmarks", targetPinId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("t1_4. 북마크 생성 실패 (존재하지 않는 핀 ID)")
    fun t1_4() {
        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
        val targetPinId = failedTargetId

        val jsonContent: String = """
                                {
                                  "pinId": $targetPinId
                                }
                            
                            """.trimIndent()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/{pinId}/bookmarks", targetPinId)
                .header("Authorization", getAuthHeader(user1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }


    @Test
    @DisplayName("t2_1. 나의 북마크 목록 조회 성공")
    fun t2_1() {
        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.get("/api/bookmarks")
                .header("Authorization", getAuthHeader(user1))
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(5))
    }

    @Test
    @DisplayName("t2_2. 나의 북마크 목록 조회 성공 (북마크 없음)")
    fun t2_2() {
        val user3 = userRepository.findByEmail("no@example.com").orElseThrow()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.get("/api/bookmarks")
                .header("Authorization", getAuthHeader(user3))
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2))
    }

    @Test
    @DisplayName("t2_3. 나의 북마크 목록 조회 실패 (인증되지 않은 사용자)")
    fun t2_3() {
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.get("/api/bookmarks")
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }


//    @Test
//    @DisplayName("t3_1. 북마크 삭제 성공 (soft delete)")
//    fun t3_1() {
//        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
//        val pinA = findPinByContent("서울 시청 근처 카페 ☕")
//        val bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA)
//            ?: throw RuntimeException("테스트 설정 실패: 북마크를 찾을 수 없음")
//
//        val targetBookmarkId = bookmark1A.id
//
//        val resultActions = mvc.perform(
//            MockMvcRequestBuilders.delete("/api/bookmarks/{bookmarkId}", targetBookmarkId)
//                .header("Authorization", getAuthHeader(user1))
//        ).andDo(MockMvcResultHandlers.print())
//
//        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
//
//        val deletedBookmark = bookmarkRepository.findById(targetBookmarkId!!).orElseThrow()
//        Assertions.assertThat(deletedBookmark.deleted).isTrue()
//    }

    @Test
    @DisplayName("t3_2. 북마크 삭제 실패 (존재하지 않는 북마크 ID)")
    fun t3_2() {
        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.delete("/api/bookmarks/{bookmarkId}", failedTargetId)
                .header("Authorization", getAuthHeader(user1))
        ).andDo(MockMvcResultHandlers.print())

        resultActions.andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("4001"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("t3_3. 북마크 삭제 실패 (소유자가 아님)")
    fun t3_3() {
        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
        val user2 = userRepository.findByEmail("user2@example.com").orElseThrow()
        val pinA = findPinByContent("서울 시청 근처 카페 ☕")
        val bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA)
            ?: throw RuntimeException("테스트 설정 실패: 북마크를 찾을 수 없음")

        val targetBookmarkId = bookmark1A.id

        // user2가 user1의 북마크 삭제 시도
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.delete("/api/bookmarks/{bookmarkId}", targetBookmarkId)
                .header("Authorization", getAuthHeader(user2))
        ).andDo(MockMvcResultHandlers.print())

        //소유자 체크 실패 시 Not Found 반환
        resultActions.andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("4001"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

//    @Test
//    @DisplayName("t3_4. 북마크 복원 성공")
//    fun t3_4() {
//        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
//        val pinA = findPinByContent("서울 시청 근처 카페 ☕")
//
//        // 기존 북마크를 삭제 상태로 만들어 놓기
//        val bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA)
//            ?: throw RuntimeException("테스트 설정 실패: 북마크를 찾을 수 없음")
//        bookmark1A.setDeleted()
//        bookmarkRepository.save(bookmark1A)
//
//        val targetBookmarkId = bookmark1A.id
//
//        val resultActions = mvc.perform(
//            MockMvcRequestBuilders.patch("/api/bookmarks/{bookmarkId}", targetBookmarkId)
//                .header("Authorization", getAuthHeader(user1))
//        ).andDo(MockMvcResultHandlers.print())
//
//        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
//
//        val restored = bookmarkRepository.findById(targetBookmarkId!!).orElseThrow()
//        Assertions.assertThat(restored.deleted).isFalse()
//    }
//
//    @Test
//    @DisplayName("t3_5. 북마크 복원 실패 (존재하지 않는 북마크 ID)")
//    fun t3_5() {
//        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
//
//        val resultActions = mvc.perform(
//            MockMvcRequestBuilders.patch("/api/bookmarks/{bookmarkId}", failedTargetId)
//                .header("Authorization", getAuthHeader(user1))
//        ).andDo(MockMvcResultHandlers.print())
//
//        resultActions.andExpect(MockMvcResultMatchers.status().isNotFound())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("4001"))
//            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
//    }


//    @Test
//    @DisplayName("t3_6. 북마크 복원 실패 (소유자가 아님)")
//    fun t3_6() {
//        val user1 = userRepository.findByEmail("user1@example.com").orElseThrow()
//        val user2 = userRepository.findByEmail("user2@example.com").orElseThrow()
//        val pinA = findPinByContent("서울 시청 근처 카페 ☕")
//
//        // 기존 북마크를 삭제 상태로 만들어 놓기
//        val bookmark1A = bookmarkRepository.findByUserAndPinAndDeletedFalse(user1, pinA)
//            ?: throw RuntimeException("테스트 설정 실패: 북마크를 찾을 수 없음")
//        bookmark1A.setDeleted()
//        bookmarkRepository.save(bookmark1A)
//
//        val targetBookmarkId = bookmark1A.id
//
//        // user2가 user1의 북마크 복원 시도
//        val resultActions = mvc.perform(
//            MockMvcRequestBuilders.patch("/api/bookmarks/{bookmarkId}", targetBookmarkId)
//                .header("Authorization", getAuthHeader(user2))
//        ).andDo(MockMvcResultHandlers.print())
//
//        // 소유자 체크 실패 시 Not Found 반환
//        resultActions.andExpect(MockMvcResultMatchers.status().isNotFound())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("4001"))
//            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
//    }
}