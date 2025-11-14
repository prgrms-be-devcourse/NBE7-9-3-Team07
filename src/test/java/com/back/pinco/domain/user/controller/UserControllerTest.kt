package com.back.pinco.domain.user.controller

import com.back.pinco.domain.bookmark.service.BookmarkService
import com.back.pinco.domain.likes.entity.Likes
import com.back.pinco.domain.likes.repository.LikesRepository
import com.back.pinco.domain.likes.service.LikesService
import com.back.pinco.domain.pin.entity.Pin
import org.assertj.core.api.Assertions
import com.back.pinco.domain.pin.service.PinService
import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.repository.UserRepository
import com.back.pinco.domain.user.service.UserService
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.servlet.http.Cookie
import org.assertj.core.api.AssertionsForClassTypes
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest // 실제 스프링 부트 애플리케이션 컨텍스트 로드
@AutoConfigureMockMvc(addFilters = true)
class UserControllerIntegrationTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var om: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var bookmarkService: BookmarkService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var pinService: PinService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var likesRepository: LikesRepository


    @Test
    @DisplayName("회원가입 성공")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t1() {
        val email = "yunseo+" + UUID.randomUUID() + "@example.com"
        val userName = "윤서"
        val rawPwd = "Password123!"
        val body = """
      {"email":"${email}","userName":"${userName}","password":"${rawPwd}"}
      
      """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/join")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 가입이 완료되었습니다"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(4))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))   // 필요하면
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userName").value(userName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.createdAt").exists())
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일로 가입")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t2() {
        val email = "user1@example.com"
        val userName = "새유저"
        val rawPwd = "12345678"
        val body = """
      {"email":"${email}","userName":"${userName}","password":"${rawPwd}"}
      
      """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/join")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2004"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 존재하는 이메일입니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t3() {
        val email = "yunseo+" + UUID.randomUUID() + ""
        val userName = "새유저"
        val rawPwd = "Password123!"
        val body = """
      {"email":"${email}","userName":"${userName}","password":"${rawPwd}"}
      
      """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/join")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2001"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이메일 형식이 올바르지 않습니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t4() {
        val email = "yunseo+" + UUID.randomUUID() + "@example.com"
        val userName = "새유저"
        val rawPwd = "1234"
        val body = """
      {"email":"${email}","userName":"${userName}","password":"${rawPwd}"}
      
      """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/join")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호 형식을 만족하지 않습니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 회원 이름 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t5() {
        val email = "yunseo+" + UUID.randomUUID() + "@example.com"
        val userName = "유"
        val rawPwd = "Password123!"
        val body = """
      {"email":"${email}","userName":"${userName}","password":"${rawPwd}"}
      
      """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/join")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2003"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 이름 형식을 만족하지 않습니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 회원 이름으로 가입")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t6() {
        val email = "potato@example.com"
        val userName = "유저1"
        val rawPwd = "Password123!"
        val body = """
      {"email":"${email}","userName":"${userName}","password":"${rawPwd}"}
      
      """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/join")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2005"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 존재하는 회원이름입니다."))
    }

    @Test
    @DisplayName("로그인 성공")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t7() {
        val email = "user1@example.com"
        val rawPwd = "12345678"

        val body = """
          { "email":"${email}", "password":"${rawPwd}" }
          
          """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apiKey").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshToken").isNotEmpty())

        resultActions.
                andExpect { result: MvcResult ->
                    // apiKey 쿠키 검증
                    val apiKeyCookie = result.response.getCookie("apiKey")
                    Assertions.assertThat(apiKeyCookie).isNotNull()
                    Assertions.assertThat(apiKeyCookie!!.path).isEqualTo("/")
                    Assertions.assertThat(apiKeyCookie.isHttpOnly).isTrue()
                    Assertions.assertThat(apiKeyCookie.value).isNotBlank()

                    // accessToken 쿠키 검증
                    val accessTokenCookie = result.response.getCookie("accessToken")
                    Assertions.assertThat(accessTokenCookie).isNotNull()
                    Assertions.assertThat(accessTokenCookie!!.path).isEqualTo("/")
                    Assertions.assertThat(accessTokenCookie.isHttpOnly).isTrue()
                    Assertions.assertThat(accessTokenCookie.value).isNotBlank()
                }
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t8() {
        val email = "example1@example.com"
        val rawPwd = "Password123!"
        val body = """
      {"email":"${email}","password":"${rawPwd}"}
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2006"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("존재하지 않는 이메일입니다."))
    }

    @Test
    @DisplayName("로그인 실패 - 틀린 비밀번호")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t9() {
        val email = "user1@example.com"
        val rawPwd = "Password123!"
        val body = """
      {"email":"${email}","password":"${rawPwd}"}
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2007"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("회원 조회 성공 - apiKey 쿠키 인증으로 getInfo")
    @Throws(
        Exception::class
    )
    fun t10() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey

        // when & then
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/getInfo")
                    .header("Authorization", "Bearer $apiKey")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getUserInfo"))  // 컨트롤러 메서드 이름이 me일 때
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value("회원 정보를 성공적으로 조회했습니다.")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(actor.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(actor.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userName").value(actor.userName))
    }

    @Test
    @DisplayName("회원정보 조회 실패 - API Key 없음(로그인 X)")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t11() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/getInfo")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2014"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인이 필요합니다."))
    }

    @Test
    @DisplayName("회원 정보 수정 - 비밀번호만 수정 (기존 시드 유저, 롤백)")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t12() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldRaw = "12345678" // 실제 비번에 맞게
        val email = actor.email
        val name = actor.userName
        val oldHashedBefore = actor.password

        val newRaw = "NewPassword123!"

        val body = """
      {
        "email": "${email}",
        "password": "${oldRaw}",
        "newUserName": "",
        "newPassword": "${newRaw}"
      }
      
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("edit"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value("회원정보 수정 완료")
            )
        val updated = userRepository.findById(actor.id!!).orElseThrow()

        resultActions
            .andExpect{
                Assertions.assertThat(updated.userName).isEqualTo(name)
                Assertions.assertThat(passwordEncoder.matches(newRaw, updated.password)).isTrue()
                Assertions.assertThat(passwordEncoder.matches(oldRaw, updated.password)).isFalse()
                Assertions.assertThat(updated.password).isNotEqualTo(oldHashedBefore)
            }
    }

    @Test
    @DisplayName("회원 정보 수정 - 회원 이름만 수정")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t13() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldRaw = "12345678" // 실제 비번에 맞게
        val email = actor.email
        val oldName = actor.userName
        val newName = "새이름"

        val body = """
      {
        "email": "${email}",
        "password": "${oldRaw}",
        "newUserName": "${newName}",
        "newPassword": ""
      }
      
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("edit"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value("회원정보 수정 완료")
            )
        val updated = userRepository.findById(actor.id!!).orElseThrow()

        resultActions
            .andExpect{
                Assertions.assertThat(updated.userName).isEqualTo(newName)
                Assertions.assertThat(updated.userName).isNotEqualTo(oldName)
            }
    }

    @Test
    @DisplayName("회원 정보 수정 - 회원 이름과 비밀번호 모두 수정")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t14() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldRaw = "12345678" // 실제 비번에 맞게
        val email = actor.email
        val oldName = actor.userName
        val newName = "새이름"
        val oldHashedBefore = actor.password
        val newRaw = "NewPassword123!"

        val body = """
      {
        "email": "${email}",
        "password": "${oldRaw}",
        "newUserName": "${newName}",
        "newPassword": "${newRaw}"
      }
      
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("edit"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value("회원정보 수정 완료")
            )
        val updated = userRepository.findById(actor.id!!).orElseThrow()

        resultActions
            .andExpect{
                Assertions.assertThat(updated.userName).isEqualTo(newName)
                Assertions.assertThat(updated.userName).isNotEqualTo(oldName)
                Assertions.assertThat(passwordEncoder.matches(newRaw, updated.password)).isTrue()
                Assertions.assertThat(passwordEncoder.matches(oldRaw, updated.password)).isFalse()
                Assertions.assertThat(updated.password).isNotEqualTo(oldHashedBefore)
            }

    }

    @Test
    @DisplayName("회원정보 수정 실패 - 입력 비밀번호 불일치")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t15() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12341234"
        val newName = "새유저"
        val newPassWd = "newPassword"
        // when: 틀린 현재 비밀번호로 수정 시도
        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "${newName}",
        "newPassword": "${newPassWd}"
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2007"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 이름 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t16() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "새"
        val newPassWd = "newPassword"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "${newName}",
        "newPassword": ""
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2003"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 이름 형식을 만족하지 않습니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 이미 존재하는 이름")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t17() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "유저2"
        val newPassWd = "newPassword"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "${newName}",
        "newPassword": ""
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2005"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 존재하는 회원이름입니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 비밀번호 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t18() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "새유저"
        val newPassWd = "new"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "",
        "newPassword": "${newPassWd}"
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호 형식을 만족하지 않습니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 회원 정보 모두 수정 시 회원 이름 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t19() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "새"
        val newPassWd = "newPassword"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "${newName}",
        "newPassword": "${newPassWd}"
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

         resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2003"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 이름 형식을 만족하지 않습니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 회원 정보 모두 수정 시 회원 이름 중복")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t20() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "유저2"
        val newPassWd = "newPassword"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "${newName}",
        "newPassword": "${newPassWd}"
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2005"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 존재하는 회원이름입니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 회원 정보 모두 수정 시 비밀번호 형식 오류")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t21() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "새유저"
        val newPassWd = "new"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "${newName}",
        "newPassword": "${newPassWd}"
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호 형식을 만족하지 않습니다."))
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 수정 사항 없음")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t22() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val oldPassWd = "12345678"
        val newName = "새유저"
        val newPassWd = "new"

        val body = """
      {
        "password": "${oldPassWd}",              
        "newUserName": "",
        "newPassword": ""
      }
      """

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/user/edit")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2011"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("변경할 내용이 없습니다."))
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 삭제 후 존재하지 않음")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t23() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val rawPwd = "12345678"

        // JSON body
        val body = """
    {
      "password": "${rawPwd}"
    }
    
    """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/user/delete")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 탈퇴가 완료되었습니다."))

        val updated = userRepository.findById(actor.id!!).get()
        resultActions
            .andExpect{
                Assertions.assertThat(updated.isDeleted).isTrue()
            }
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호 불일치")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t24() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey
        val rawPwd = "12341234"

        // JSON body
        val body = """
    {
      "password": "${rawPwd}"
    }
    
    """
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/user/delete")
                    .header("Authorization", "Bearer $apiKey")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2007"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("로그아웃")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t25() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/logout")
                    .header("Authorization", "Bearer $apiKey")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그아웃 성공"))
            .andExpect { result: MvcResult ->
                result.response.getCookie("apiKey")
                    ?.let {
                        Assertions.assertThat(it.value).isEmpty()
                        Assertions.assertThat(it.maxAge).isEqualTo(0)
                        Assertions.assertThat(it.path).isEqualTo("/")
                        Assertions.assertThat(it.isHttpOnly).isTrue()
                    }

            }
    }

    @Test
    @DisplayName("로그아웃 실패 - 로그인 X(API 키 없는 경우)")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t26() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/user/logout")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2014"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인이 필요합니다."))
    }

    @Test
    @DisplayName("마이페이지")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t27() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey

        val myPinCount = pinService.findByUserId(actor, actor).size
        val bookmarkCount = bookmarkService.getMyBookmarks(actor.id).size
        val likesCount = userService.likesCount(pinService.findByUserId(actor, actor))


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/mypage")
                    .header("Authorization", "Bearer $apiKey")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("myPage"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("마이페이지 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(actor.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userName").value(actor.userName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.myPinCount").value(myPinCount))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.bookmarkCount").value(bookmarkCount))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.likesCount").value(likesCount))
    }

    @Test
    @DisplayName("마이페이지 - 핀 데이터 조회")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t28() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/mypin")
                    .header("Authorization", "Bearer $apiKey")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("myPin"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("공개 글, 비공개 글을 조회했습니다."))
            // data 존재
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            // 리스트 존재 여부
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.publicPins").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.privatePins").isArray)
    }

    @Test
    @DisplayName("마이페이지 - 북마크 조회")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t29() {
        val actor = userRepository.findByUserName("유저1").get()
        val apiKey = actor.apiKey

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/mybookmark")
                    .header("Authorization", "Bearer $apiKey")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("myBookmark"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("북마크한 게시물을 모두 조회했습니다."))
            // data 존재
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            // 리스트 존재 여부
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.bookmarkList").isArray)
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 로그인 X(API 키 없는 경우)")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t30() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/mypage")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2014"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인이 필요합니다."))
    }

    @Test
    @DisplayName("핀 조회 실패 - 로그인 X(API 키 없는 경우)")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t31() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/mypin")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2014"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인이 필요합니다."))
    }

    @Test
    @DisplayName("북마크 조회 실패 - 로그인 X(API 키 없는 경우)")
    @Transactional
    @Throws(
        Exception::class
    )
    fun t32() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/mybookmark")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("2014"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인이 필요합니다."))
    }

    @Test
    @DisplayName("특정 사용자가 좋아요 누른 핀 목록 성공")
    @Transactional
    @Throws(
        Exception::class
    )
    fun pinsLikedByUser() {
            // given
            val userId = 1L
            val testUser = userService.findById(userId)
            val jwtToken = jwtTokenProvider.generateAccessToken(testUser.id, testUser.email, testUser.userName)
            val likesCount = likesRepository.countByUserId(userId)

            val pinIds = likesRepository.findPinsByUserId(userId)
                .stream()
                .map { pin: Pin -> pin.id }
                .map { id: Long? -> id!!.toInt() }
                .toArray { size -> arrayOfNulls<Int>(size) }

        // when & then
            val resultActions =
                mvc.perform(
                    MockMvcRequestBuilders.get("/api/user/{userId}/likespins", userId)
                        .header("Authorization", "Bearer ${testUser.apiKey} ${jwtToken}")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("getPinsLikedByUser"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("성공적으로 처리되었습니다"))

                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.length()").value(likesCount.toInt())
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[*].id").value(Matchers.containsInAnyOrder(*pinIds)))
        }

    @Test
    @DisplayName("특정 사용자가 좋아요 누른 핀 목록 성공 - 비공개 5번 제외")
    @Transactional
    @Throws(
        Exception::class
    )
    fun pinsLik2edByUser() {
            // given
         val userId = 3L
         val testUser = userService.findById(userId)
         val jwtToken = jwtTokenProvider.generateAccessToken(testUser.id, testUser.email, testUser.userName)

         val pinIds = likesRepository.findPinsByUserId(userId)
             .stream()
             .map<Long> { obj: Pin -> obj.id }
             .map<Int> { id: Long -> id.toInt() }
             .toArray { size -> arrayOfNulls<Int>(size) }

         pinService.changePublic(userService.findById(2L), 5L)

         entityManager.flush()
         entityManager.clear()

         // when & then

        val resultActions =
            mvc.perform(
                MockMvcRequestBuilders.get("/api/user/{userId}/likespins", userId)
                    .header("Authorization", "Bearer ${testUser.apiKey} ${jwtToken}")
                    .accept(MediaType.APPLICATION_JSON)
             )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinsLikedByUser"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("성공적으로 처리되었습니다"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(pinIds.size))
    }

    @Test
    @DisplayName("탈퇴 회원 좋아요 취소 - 성공")
    @Transactional
    @Throws(
        Exception::class
    )
    fun setLikedFalse() {
        // given
        val userId = 3L
        val testUser = userService!!.findById(userId)
        val jwtToken = jwtTokenProvider!!.generateAccessToken(testUser.id, testUser.email, testUser.userName)

        val pinIds = likesRepository!!.findPinsByUserId(userId)
            .stream()
            .map { obj: Pin -> obj.id }
            .map { id: Long? -> id!!.toInt() }
            .toArray { size -> arrayOfNulls<Int>(size) }

        // when & then
//        int delcnt = likesService.updateDeleteUserLikedFalse(userId);
//        assertThat(delcnt).isEqualTo(pinIds.length);
    }
}
