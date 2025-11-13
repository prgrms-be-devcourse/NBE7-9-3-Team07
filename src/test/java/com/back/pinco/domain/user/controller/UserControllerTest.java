package com.back.pinco.domain.user.controller;

import com.back.pinco.domain.likes.repository.LikesRepository;
import com.back.pinco.domain.likes.service.LikesService;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.service.PinService;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
import com.back.pinco.domain.user.service.UserService;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import com.back.pinco.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 실제 스프링 부트 애플리케이션 컨텍스트 로드
@AutoConfigureMockMvc(addFilters = true) // MockMvc 자동 구성
class UserControllerIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserService userService;
    @Autowired
    LikesRepository likesRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PinService pinService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private LikesService likesService;


    @Test
    @DisplayName("회원가입 성공 - DB에 유저 저장 및 RsData 반환")
    @Transactional
    void t1() throws Exception {
        String email = "yunseo+" + UUID.randomUUID() + "@example.com";
        String rawPwd = "Password123!";
        String body = """
      {"email":"%s","userName":"윤서","password":"%s"}
      """.formatted(email, rawPwd);

        mvc.perform(post("/api/user/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 가입이 완료되었습니다"));

        // ✅ DB 검증
        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getUserName()).isEqualTo("윤서");
        // 비밀번호는 해시가 저장되어야 함
        assertThat(passwordEncoder.matches(rawPwd, saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일로 가입")
    @Transactional
    void t2() throws Exception {
        String email = "user1@example.com";
        String rawPwd = "12345678";
        String body = """
      {"email":"%s","userName":"유저1","password":"%s"}
      """.formatted(email, rawPwd);

        mvc.perform(post("/api/user/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("2004"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 이메일입니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 오류")
    @Transactional
    void t3() throws Exception {
        String email = "yunseo+" + UUID.randomUUID() + "";
        String rawPwd = "Password123!";
        String body = """
      {"email":"%s","userName":"윤서","password":"%s"}
      """.formatted(email, rawPwd);

        mvc.perform(post("/api/user/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2001"))
                .andExpect(jsonPath("$.msg").value("이메일 형식이 올바르지 않습니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 형식 오류")
    @Transactional
    void t4() throws Exception {
        String email = "yunseo+" + UUID.randomUUID() + "@example.com";
        String rawPwd = "";
        String body = """
      {"email":"%s","userName":"윤서","password":"%s"}
      """.formatted(email, rawPwd);

        mvc.perform(post("/api/user/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2002"))
                .andExpect(jsonPath("$.msg").value("비밀번호 형식을 만족하지 않습니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("회원가입 실패 - 회원 이름 형식 오류")
    @Transactional
    void t5() throws Exception {
        String email = "yunseo+" + UUID.randomUUID() + "@example.com";
        String rawPwd = "Password123!";
        String body = """
      {"email":"%s","userName":"냠","password":"%s"}
      """.formatted(email, rawPwd);

        mvc.perform(post("/api/user/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2003"))
                .andExpect(jsonPath("$.msg").value("회원 이름 형식을 만족하지 않습니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 회원 이름으로 가입")
    @Transactional
    void t6() throws Exception {
        String email = "potato@example.com";
        String userName = "유저1";
        String rawPwd = "Password123!";
        String body = """
      {"email":"%s","userName":"%s","password":"%s"}
      """.formatted(email, userName, rawPwd);

        mvc.perform(post("/api/user/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("2005"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 회원이름입니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByUserName(userName)).isPresent();
    }

    @Test
    @DisplayName("로그인 성공")
    @Transactional
    void t7() throws Exception {
        // given: 로그인 가능한 사용자 사전 저장
        String email = "login+" + UUID.randomUUID() + "@example.com";
        String rawPwd = "Password123!";
        String hashed = passwordEncoder.encode(rawPwd);

        User seed = new User(email, hashed, "윤서");
        userRepository.save(seed);

        String body = """
          { "email":"%s", "password":"%s" }
          """.formatted(email, rawPwd);

        // when & then
        mvc.perform(post("/api/user/login") // 클래스 레벨 @RequestMapping("/api/user") 가정
                        .with(csrf())                         // 시큐리티 켠 상태면 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("로그인 성공"));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    @Transactional
    void t8() throws Exception {
        String email = "example1@example.com";
        String rawPwd = "Password123!";
        String body = """
      {"email":"%s","userName":"감자","password":"%s"}
      """.formatted(email, rawPwd);

        mvc.perform(post("/api/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("2006"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 이메일입니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("로그인 실패 - 틀린 비밀번호")
    @Transactional
    void t9() throws Exception {
        String email = "user1@example.com";
        String userName = "유저1";
        String rawPwd = "Password123!";
        String body = """
      {"email":"%s","userName":"%s","password":"%s"}
      """.formatted(email, userName, rawPwd);

        mvc.perform(post("/api/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("2007"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."));

        // ✅ DB 검증
        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("회원 조회 성공 - apiKey 쿠키 인증으로 getInfo")
    void t10() throws Exception {
        // given: 시드된 관리자(혹은 임의 사용자)
        User actor = userRepository.findByUserName("유저1").get();

        // when & then
        mvc.perform(get("/api/user/getInfo")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.email").value(actor.getEmail()))
                .andExpect(jsonPath("$.data.userName").value(actor.getUserName())); // 필드명에 맞춰 수정
    }

    @Test
    @DisplayName("회원정보 조회 실패 - API Key 없음(로그인 X)")
    @Transactional
    void t11() throws Exception {
        mvc.perform(get("/api/user/getInfo")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("2014"))
                .andExpect(jsonPath("$.msg").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("회원 정보 수정 - 비밀번호만 수정 (기존 시드 유저, 롤백)")
    @Transactional
    void t12() throws Exception {
        // given
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String oldRaw = "12345678";   // 실제 비번에 맞게
        String email = actor.getEmail();
        String name = actor.getUserName();

        String oldHashedBefore = actor.getPassword();

        String newRaw = "NewPassword123!";

        String body = """
      {
        "email": "%s",
        "password": "%s",
        "newUserName": "",
        "newPassword": "%s"
      }
      """.formatted(email, oldRaw, newRaw);

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원정보 수정 완료"));

        User updated = userRepository.findById(actor.getId()).orElseThrow();
        assertThat(updated.getUserName()).isEqualTo(name);
        assertThat(passwordEncoder.matches(newRaw, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(oldRaw, updated.getPassword())).isFalse();
        assertThat(updated.getPassword()).isNotEqualTo(oldHashedBefore);
    }

    @Test
    @DisplayName("회원 정보 수정 - 회원 이름만 수정")
    @Transactional
    void t13() throws Exception {
        // given
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String oldRaw = "12345678";   // 실제 비번에 맞게
        String email = actor.getEmail();
        String name = actor.getUserName();

        String oldHashedBefore = actor.getPassword();

        String newName = "NewName";

        String body = """
      {
        "email": "%s",
        "password": "%s",
        "newUserName": "%s",
        "newPassword": ""
      }
      """.formatted(email, oldRaw, newName);

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원정보 수정 완료"));

        User updated = userRepository.findById(actor.getId()).orElseThrow();
        assertThat(updated.getUserName()).isEqualTo(newName);
        assertThat(updated.getUserName()).isNotEqualTo(name);
        assertThat(passwordEncoder.matches(oldRaw, updated.getPassword())).isTrue();
        assertThat(updated.getPassword()).isEqualTo(oldHashedBefore); // 해시까지 동일
    }

    @Test
    @DisplayName("회원 정보 수정 - 회원 이름과 비밀번호 모두 수정")
    @Transactional
    void t14() throws Exception {
        // given
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String oldRaw = "12345678";   // 실제 비번에 맞게
        String email = actor.getEmail();
        String name = actor.getUserName();

        String oldHashedBefore = actor.getPassword();

        String newName = "NewName";
        String newPassword = "NewPassword";

        String body = """
      {
        "email": "%s",
        "password": "%s",
        "newUserName": "%s",
        "newPassword": "%s"
      }
      """.formatted(email, oldRaw, newName, newPassword);

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원정보 수정 완료"));

        User updated = userRepository.findById(actor.getId()).orElseThrow();
        assertThat(updated.getUserName()).isEqualTo(newName);
        assertThat(updated.getUserName()).isNotEqualTo(name);
        assertThat(passwordEncoder.matches(newPassword, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(oldRaw, updated.getPassword())).isFalse();
        assertThat(updated.getPassword()).isNotEqualTo(oldHashedBefore);
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 입력 비밀번호 불일치")
    @Transactional
    void t15() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // when: 틀린 현재 비밀번호로 수정 시도
        String body = """
      {
        "password": "WrongPass!",              
        "newUserName": "새닉네임",
        "newPassword": "NewPassword123!"
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                // then: 401 + PASSWORD_NOT_MATCH(2006)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("2007"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 이름 형식 오류")
    @Transactional
    void t16() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "새",
        "newPassword": ""
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2003"))
                .andExpect(jsonPath("$.msg").value("회원 이름 형식을 만족하지 않습니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 이미 존재하는 이름")
    @Transactional
    void t17() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "유저2",
        "newPassword": ""
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("2005"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 회원이름입니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 비밀번호 형식 오류")
    @Transactional
    void t18() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "새유저",
        "newPassword": "1234"
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2002"))
                .andExpect(jsonPath("$.msg").value("비밀번호 형식을 만족하지 않습니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 회원 정보 모두 수정 시 회원 이름 형식 오류")
    @Transactional
    void t19() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "새",
        "newPassword": "12341234"
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2003"))
                .andExpect(jsonPath("$.msg").value("회원 이름 형식을 만족하지 않습니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 회원 정보 모두 수정 시 회원 이름 중복")
    @Transactional
    void t20() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "유저2",
        "newPassword": "12341234"
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("2005"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 회원이름입니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 회원 정보 모두 수정 시 비밀번호 형식 오류")
    @Transactional
    void t21() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "새유저",
        "newPassword": "1234"
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2002"))
                .andExpect(jsonPath("$.msg").value("비밀번호 형식을 만족하지 않습니다."));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 수정 사항 없음")
    @Transactional
    void t22() throws Exception {
        User actor = userRepository.findByUserName("유저1")
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String body = """
      {
        "password": "12345678",              
        "newUserName": "",
        "newPassword": ""
      }
      """;

        mvc.perform(put("/api/user/edit")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("2011"))
                .andExpect(jsonPath("$.msg").value("변경할 내용이 없습니다."));
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 삭제 후 존재하지 않음")
    @Transactional
    void t23() throws Exception {
        User actor = userRepository.findByUserName("유저1").get();
        String rawPwd = "12345678";

        // JSON body
        String body = """
    {
      "password": "%s"
    }
    """.formatted(rawPwd);

        // when & then
        mvc.perform(delete("/api/user/delete")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원 탈퇴가 완료되었습니다."));

        User updated = userRepository.findById(actor.getId()).get();
        assertThat(updated.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호 불일치")
    @Transactional
    void t24() throws Exception {
        User actor = userRepository.findByUserName("유저1").get();
        String rawPwd = "12341234";

        // JSON body
        String body = """
    {
      "password": "%s"
    }
    """.formatted(rawPwd);

        // when & then
        mvc.perform(delete("/api/user/delete")
                        .cookie(new Cookie("apiKey", actor.getApiKey()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("2007"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."));
    }


    @Test
    @DisplayName("특정 사용자가 좋아요 누른 핀 목록 성공")
    @Transactional
    void getPinsLikedByUser() throws Exception {
        // given
        Long userId = 1L;
        User testUser = userService.findById(userId);
        String jwtToken = jwtTokenProvider.generateAccessToken(testUser.getId(), testUser.getEmail(), testUser.getUserName());

        Integer[] pinIds = likesRepository.findPinsByUserId(userId)
                .stream()
                .map(Pin::getId)
                .map(id -> id.intValue())
                .toArray(Integer[]::new);

        // when & then
        mvc.perform(
                        get("/api/user/{userId}/likespins", userId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print())
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(handler().methodName("getPinsLikedByUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("성공적으로 처리되었습니다"))

                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(likesRepository.countByUserId(userId)))
                .andExpect(jsonPath("$.data[*].id", containsInAnyOrder(pinIds)));;
    }

    @Test
    @DisplayName("특정 사용자가 좋아요 누른 핀 목록 성공 - 비공개 5번 제외")
    @Transactional
    void getPinsLik2edByUser() throws Exception {
        // given
        Long userId = 3L;
        User testUser = userService.findById(userId);
        String jwtToken = jwtTokenProvider.generateAccessToken(testUser.getId(), testUser.getEmail(), testUser.getUserName());

        Integer[] pinIds = likesRepository.findPinsByUserId(userId)
                .stream()
                .map(Pin::getId)
                .map(id -> id.intValue())
                .toArray(Integer[]::new);

        pinService.changePublic(userService.findById(2L), 5L);

        entityManager.flush();
        entityManager.clear();

        // when & then
        mvc.perform(
                        get("/api/user/{userId}/likespins", userId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print())
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(handler().methodName("getPinsLikedByUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("성공적으로 처리되었습니다"))

                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(pinIds.length));
    }

    @Test
    @DisplayName("탈퇴 회원 좋아요 취소 - 성공")
    @Transactional
    void setLikedFalse() throws Exception {
        // given
        Long userId = 3L;
        User testUser = userService.findById(userId);
        String jwtToken = jwtTokenProvider.generateAccessToken(testUser.getId(), testUser.getEmail(), testUser.getUserName());

        Integer[] pinIds = likesRepository.findPinsByUserId(userId)
                .stream()
                .map(Pin::getId)
                .map(Long::intValue)
                .toArray(Integer[]::new);

        // when & then
//        int delcnt = likesService.updateDeleteUserLikedFalse(userId);
//        assertThat(delcnt).isEqualTo(pinIds.length);
    }
}
