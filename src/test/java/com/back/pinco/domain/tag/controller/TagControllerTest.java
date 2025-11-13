package com.back.pinco.domain.tag.controller;

import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.repository.PinRepository;
import com.back.pinco.domain.tag.entity.Tag;
import com.back.pinco.domain.tag.repository.TagRepository;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 비활성화
@Transactional
class TagControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PinRepository pinRepository;
    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // ✅ 테스트 실행 전 DB 초기화
    @BeforeEach
    void clearDatabase() {
        em.createNativeQuery("TRUNCATE TABLE pin_tags RESTART IDENTITY CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE pins RESTART IDENTITY CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE tags RESTART IDENTITY CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate();
    }
    // t1: 전체 태그 조회 - 성공
    @Test
    @DisplayName("t1 - 태그 목록 조회 성공")
    void t1() throws Exception {
        tagRepository.save(new Tag("카페"));
        tagRepository.save(new Tag("감성"));

        mvc.perform(get("/api/tags"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("태그 목록 조회 성공"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)));
    }

    // t2: 태그 생성 - 성공
    @Test
    @DisplayName("t2 - 태그 생성 성공")
    void t2() throws Exception {
        mvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"야경\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("새로운 태그가 생성되었습니다."))
                .andExpect(jsonPath("$.data.tag.keyword").value("야경"));
    }

    // t3: 태그 생성 - 중복 예외
    @Test
    @DisplayName("t3 - 태그 생성 실패 (이미 존재)")
    void t3() throws Exception {
        tagRepository.save(new Tag("카페"));

        mvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\"}"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3002"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 태그입니다."));
    }

    // t4: 태그 생성 - 잘못된 입력 (빈 문자열)
    @Test
    @DisplayName("t4 - 태그 생성 실패 (빈 keyword)")
    void t4() throws Exception {
        mvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"  \"}"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3010"))
                .andExpect(jsonPath("$.msg").value("태그 키워드를 입력해주세요."));
    }

    // t5: 핀에 태그 추가 - 성공
    @Test
    @DisplayName("t5 - 핀에 태그 추가 성공")
    void t5() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.2, 37.4));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));
        tagRepository.save(new Tag("감성"));

        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"감성\"}"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("태그가 핀에 추가되었습니다."));
    }

    // t6: 핀에 태그 추가 실패 - 존재하지 않는 핀
    @Test
    @DisplayName("t6 - 핀에 태그 추가 실패 (핀 없음)")
    void t6() throws Exception {
        mvc.perform(post("/api/pins/9999/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\"}"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3006"))
                .andExpect(jsonPath("$.msg").value("핀을 찾을 수 없습니다."));
    }

    // t7: 핀에 태그 추가 실패 - 이미 연결된 태그
    @Test
    @DisplayName("t7 - 핀에 태그 추가 실패 (이미 연결됨)")
    void t7() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.2, 37.4));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));
        tagRepository.save(new Tag("카페"));

        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\"}"))
                .andDo(print());

        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\"}"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3004"))
                .andExpect(jsonPath("$.msg").value("이미 이 핀에 연결된 태그입니다."));
    }

    // t8: 핀의 태그 목록 조회 - 성공
    @Test
    @DisplayName("t8 - 핀의 태그 목록 조회 성공")
    void t8() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.2, 37.4));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));
        tagRepository.save(new Tag("데이트"));

        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"데이트\"}"))
                .andDo(print());

        mvc.perform(get("/api/pins/" + pin.getId() + "/tags"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("핀의 태그 목록 조회 성공"));
    }

    // t9: 핀의 태그 목록 조회 실패 - 핀 없음
    @Test
    @DisplayName("t9 - 핀의 태그 목록 조회 실패 (핀 없음)")
    void t9() throws Exception {
        mvc.perform(get("/api/pins/9999/tags"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3006"))
                .andExpect(jsonPath("$.msg").value("핀을 찾을 수 없습니다."));
    }

    // t10: 핀의 태그 목록 조회 실패 - 연결된 태그 없음
    @Test
    @DisplayName("t10 - 핀의 태그 목록 조회 실패 (태그 없음)")
    void t10() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.2, 37.4));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));

        mvc.perform(get("/api/pins/" + pin.getId() + "/tags"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3007"))
                .andExpect(jsonPath("$.msg").value("해당 핀에 연결된 태그가 없습니다."));
    }

    // t11: 여러 태그 기반 핀 교집합 조회 - 성공
    @Test
    @DisplayName("t11 - 여러 태그 기반 핀 교집합 조회 성공")
    void t11() throws Exception {
        // 1. 유저 생성
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));

        // 2. 핀 2개 생성
        Point point1 = geometryFactory.createPoint(new Coordinate(127.0276, 37.4979));
        Point point2 = geometryFactory.createPoint(new Coordinate(127.0256, 37.5009));
        Pin pin1 = pinRepository.save(new Pin(point1, user, "테스트용 핀1"));
        Pin pin2 = pinRepository.save(new Pin(point2, user, "테스트용 핀2"));

        // 3. 태그 생성
        Tag tag1 = tagRepository.save(new Tag("카페"));
        Tag tag2 = tagRepository.save(new Tag("감성"));
        Tag tag3 = tagRepository.save(new Tag("데이트"));

        // 4. 핀-태그 매핑 (pin1은 3개 다, pin2는 일부)
        mvc.perform(post("/api/pins/" + pin1.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/pins/" + pin1.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"감성\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/pins/" + pin1.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"데이트\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/pins/" + pin2.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"감성\"}"))
                .andExpect(status().isOk());

        // 5. 여러 태그 기반 교집합 조회
        mvc.perform(get("/api/tags/filter")
                        .param("keywords", "카페")
                        .param("keywords", "감성")
                        .param("keywords", "데이트"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("태그 필터링 기반 게시물 목록 조회 성공"))
                .andExpect(jsonPath("$.data.pins").isArray())
                .andExpect(jsonPath("$.data.pins[0].pin.id").value(pin1.getId()));
    }

    // t12: 여러 태그 기반 핀 교집합 조회 - 존재하지 않는 태그 요청 시
    @Test
    @DisplayName("t12 - 여러 태그 기반 핀 교집합 조회 - 존재하지 않는 태그 요청 시")
    void t12() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new Coordinate(127.1, 37.5));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));

        mvc.perform(get("/api/tags/filter")
                        .param("keywords", "없는태그"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("3001"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 태그입니다."));
    }

    // t13: 태그는 존재하지만 핀이 없을 경우
    @Test
    @DisplayName("t13 - 태그는 존재하지만 핀이 없을 경우")
    void t13() throws Exception {
        tagRepository.save(new Tag("감성"));

        mvc.perform(get("/api/tags/filter")
                        .param("keywords", "감성"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("3007"))
                .andExpect(jsonPath("$.msg").value("해당 핀에 연결된 태그가 없습니다."));
    }

    // t14: 태그는 모두 존재하지만 교집합이 없을 경우
    @Test
    @DisplayName("t14 - 태그는 모두 존재하지만 교집합이 없을 경우")
    void t14() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));

        Point point1 = geometryFactory.createPoint(new Coordinate(127.0276, 37.4979));
        Point point2 = geometryFactory.createPoint(new Coordinate(127.0256, 37.5009));
        Pin pin1 = pinRepository.save(new Pin(point1, user, "테스트용 핀"));
        Pin pin2 = pinRepository.save(new Pin(point2, user, "테스트용 핀"));

        tagRepository.save(new Tag("카페"));
        tagRepository.save(new Tag("데이트"));

        mvc.perform(post("/api/pins/" + pin1.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"카페\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/pins/" + pin2.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"데이트\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/tags/filter")
                        .param("keywords", "카페")
                        .param("keywords", "데이트"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("3012"))
                .andExpect(jsonPath("$.msg").value("해당 태그가 달린 게시물이 없습니다."));
    }

    // t15: 태그 삭제 - 성공
    @Test
    @DisplayName("t14 - 연결된 태그 삭제 성공")
    void t15() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.2, 37.4));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));
        Tag tag = tagRepository.save(new Tag("여행"));

        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"여행\"}"))
                .andDo(print());

        mvc.perform(delete("/api/pins/" + pin.getId() + "/tags/" + tag.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("태그가 삭제되었습니다."));
    }

    // t16: 태그 복구 - 성공
    @Test
    @DisplayName("t15 - 삭제된 태그 복구 성공")
    void t16() throws Exception {
        // 사용자, 핀, 태그 생성
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.2, 37.4));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));
        Tag tag = tagRepository.save(new Tag("복구테스트"));

        // 핀에 태그 추가
        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"복구테스트\"}"))
                .andDo(print());

        // 태그 삭제
        mvc.perform(delete("/api/pins/" + pin.getId() + "/tags/" + tag.getId()))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("태그가 삭제되었습니다."));

        // 태그 복구 요청
        mvc.perform(patch("/api/pins/" + pin.getId() + "/tags/" + tag.getId() + "/restore"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").value("태그가 복구되었습니다."));
    }

    // t17: 태그 복구 실패 - 존재하지 않는 핀 또는 태그
    @Test
    @DisplayName("t16 - 태그 복구 실패 (존재하지 않는 핀 또는 태그)")
    void t17() throws Exception {
        mvc.perform(patch("/api/pins/9999/tags/9999/restore"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3003"))
                .andExpect(jsonPath("$.msg").value("태그 연결이 존재하지 않습니다."));
    }

    // t18: 태그 복구 실패 - 이미 활성화된 태그
    @Test
    @DisplayName("t17 - 태그 복구 실패 (이미 활성화된 태그)")
    void t18() throws Exception {
        User user = userRepository.save(new User("tempUser", "pw", "email@test.com"));
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(127.5, 37.5));
        Pin pin = pinRepository.save(new Pin(point, user, "테스트용 핀"));
        Tag tag = tagRepository.save(new Tag("활성태그"));

        mvc.perform(post("/api/pins/" + pin.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"활성태그\"}"))
                .andDo(print());

        mvc.perform(patch("/api/pins/" + pin.getId() + "/tags/" + tag.getId() + "/restore"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("3004"))
                .andExpect(jsonPath("$.msg").value("이미 이 핀에 연결된 태그입니다."));
    }

    // t19: 태그 전체 조회 실패 - 존재하지 않는 태그 목록
    @Test
    @DisplayName("t18 - 태그 전체 조회 실패 (존재하지 않는 태그 목록)")
    void t19() throws Exception {
        tagRepository.deleteAll();

        ResultActions resultActions = mvc.perform(get("/api/tags"))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("3001"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 태그입니다."));
    }
}
