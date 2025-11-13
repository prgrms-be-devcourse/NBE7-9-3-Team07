package com.back.pinco.domain.pin.controller;

import com.back.pinco.domain.likes.entity.Likes;
import com.back.pinco.domain.likes.repository.LikesRepository;
import com.back.pinco.domain.likes.service.LikesService;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.repository.PinRepository;
import com.back.pinco.domain.pin.service.PinService;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
import com.back.pinco.domain.user.service.UserService;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import com.back.pinco.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PinControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PinService pinService;

    @Autowired
    private LikesRepository likesRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LikesService likesService;


    long targetId = 1L;
    long failedTargetId = Integer.MAX_VALUE;

    long noAuthId = 4L;

    private String jwtToken;
    private User testUser;


    @BeforeEach
    void setUp() {
        testUser = userRepository.findById(1L).get();
        jwtToken = jwtTokenProvider.generateAccessToken(testUser.getId(), testUser.getEmail(), testUser.getUserName());
    }


    @Test
    @DisplayName("핀 생성")
    void t1_1() throws Exception {

        double lat = 0;
        double lon = 0;
        String content = "new Content!";

        String jsonContent = String.format(
                """
                        {
                            "content": "%s",
                            "latitude" : %s, 
                            "longitude" : %s
                        }
                        """, content, lat, lon
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/pins")
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonContent)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("createPin"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.latitude").value(lat))
                .andExpect(jsonPath("$.data.longitude").value(lon))
                .andExpect(jsonPath("$.data.content").value(content));
    }

    @Test
    @DisplayName("핀 생성 - 실패 (경도 정보 오류)")
    void t1_2() throws Exception {

        double lat = 0;

        String content = "new Content!";

        String jsonContent = String.format(
                """
                        {
                            "content": "%s",
                            "latitude" : %s
                        }
                        """, content, lat
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/pins")
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonContent)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("createPin"))
                .andExpect(jsonPath("$.errorCode").value("1007"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 생성 - 실패 (위도 정보 오류)")
    void t1_3() throws Exception {

        double lon = 0;
        String content = "new Content!";

        String jsonContent = String.format(
                """
                        {
                            "content": "%s",
                            "longitude" : %s
                        }
                        """, content, lon
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/pins")
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonContent)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("createPin"))
                .andExpect(jsonPath("$.errorCode").value("1006"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 생성 - 실패 (내용 정보 오류)")
    void t1_4() throws Exception {

        double lat = 0;
        double lon = 0;

        String jsonContent = String.format(
                """
                        {
                            "latitude" : %s,
                            "longitude" : %s
                        }
                        """, lat, lon
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/pins")
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonContent)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("createPin"))
                .andExpect(jsonPath("$.errorCode").value("1005"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 생성 - 실패 (jwt 없음)")
    void t1_5() throws Exception {
        double lat = 0;
        double lon = 0;
        String content = "new Content!";

        String jsonContent = String.format(
                """
                        {
                            "content": "%s",
                            "latitude" : %s, 
                            "longitude" : %s
                        }
                        """, content, lat, lon
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/pins")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonContent)
                )
                .andDo(print());

        resultActions
                .andExpect(status().is(403));
    }

    @Test
    @DisplayName("id로 핀 조회 - 로그인 - 성공")
    void t2_1_1() throws Exception {

        Pin pin = pinRepository.findAccessiblePinById(targetId, testUser.getId()).get();
        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/%s".formatted(targetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getPinById"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pin.getId()))
                .andExpect(jsonPath("$.data.latitude").value(pin.getPoint().getY()))
                .andExpect(jsonPath("$.data.longitude").value(pin.getPoint().getX()))
                .andExpect(jsonPath("$.data.createdAt").value(matchesPattern(pin.getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                .andExpect(jsonPath("$.data.modifiedAt").value(matchesPattern(pin.getModifiedAt().toString().replaceAll("0+$", "") + ".*")))
                .andExpect(jsonPath("$.data.pinTags.length()").value(pin.getPinTags().size()))
        ;


    }

    @Test
    @DisplayName("id로 핀 조회 - 비 로그인 - 성공")
    void t2_1_2() throws Exception {

        Pin pin = pinRepository.findPublicPinById(targetId).get();
        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/%s".formatted(targetId))

                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getPinById"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pin.getId()))
                .andExpect(jsonPath("$.data.latitude").value(pin.getPoint().getY()))
                .andExpect(jsonPath("$.data.longitude").value(pin.getPoint().getX()))
                .andExpect(jsonPath("$.data.createdAt").value(matchesPattern(pin.getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                .andExpect(jsonPath("$.data.modifiedAt").value(matchesPattern(pin.getModifiedAt().toString().replaceAll("0+$", "") + ".*")))
                .andExpect(jsonPath("$.data.pinTags.length()").value(pin.getPinTags().size()))
        ;


    }

    @Test
    @DisplayName("id로 핀 조회 - 로그인 - 실패 (id가 없음)")
    void t2_2() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/%s".formatted(failedTargetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getPinById"))
                .andExpect(jsonPath("$.errorCode").value("1002"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("id로 핀 조회 - 비로그인 - 실패 (id가 없음)")
    void t2_3() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/%s".formatted(failedTargetId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getPinById"))
                .andExpect(jsonPath("$.errorCode").value("1002"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("특정 지점에서 범위 내 좌표 확인")
    void t3_1_1() throws Exception {

        Pin pin = pinRepository.findById(targetId).get();
        List<Pin> pins = pinRepository.findPinsWithinRadius(pin.getPoint().getX(), pin.getPoint().getY(), 1000.0, testUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins")
                                .param("radius", String.valueOf(1000))
                                .param("longitude", String.valueOf(pin.getPoint().getX()))
                                .param("latitude", String.valueOf(pin.getPoint().getY()))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getRadiusPins"))
                .andExpect(status().isOk());

        for (int i = 0; i < pins.size(); i++) {
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(pins.get(i).getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(pins.get(i).getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(pins.get(i).getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i)).value(matchesPattern(pins.get(i).getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].modifiedAt".formatted(i)).value(matchesPattern(pins.get(i).getModifiedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(pins.get(i).getPinTags().size()));
        }

    }

    @Test
    @DisplayName("특정 지점에서 범위 내 좌표 확인- 비로그인")
    void t3_1_2() throws Exception {

        Pin pin = pinRepository.findById(targetId).get();
        List<Pin> pins = pinRepository.findPublicPinsWithinRadius(pin.getPoint().getX(),pin.getPoint().getY(),1000.0);

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins")
                                .param("radius", String.valueOf(1000))
                                .param("longitude", String.valueOf(pin.getPoint().getX()))
                                .param("latitude", String.valueOf(pin.getPoint().getY()))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getRadiusPins"))
                .andExpect(status().isOk());

        for (int i = 0; i < pins.size(); i++) {
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(pins.get(i).getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(pins.get(i).getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(pins.get(i).getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i)).value(matchesPattern(pins.get(i).getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].modifiedAt".formatted(i)).value(matchesPattern(pins.get(i).getModifiedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(pins.get(i).getPinTags().size()));
        }

    }

    @Test
    @DisplayName("특정 지점에서 범위 내 핀 확인 - 핀 없음")
    void t3_2() throws Exception {
        // 핀이 없는 위치와 반경을 설정합니다.
        double outOfRangeLat = 0;
        double outOfRangeLon = 0;
        double radius = 10; // 10미터

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins")
                                .param("radius", String.valueOf(radius))
                                .param("latitude", String.valueOf(outOfRangeLat))
                                .param("longitude", String.valueOf(outOfRangeLon))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getRadiusPins"))
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("특정 범위(사각형) 내 좌표 확인 - 로그인 - 사각형")
    void t3_3_1() throws Exception {

        Pin pin = pinRepository.findAll().get(0);
        double centerLat = pin.getPoint().getY();
        double centerLon = pin.getPoint().getX();

        double delta = 0.01; // 대략 1km 정도의 범위
        double latMax = centerLat + delta;
        double latMin = centerLat - delta;
        double lonMax = centerLon + delta;
        double lonMin = centerLon - delta;

        List<Pin> pins = pinRepository.findScreenPins(latMax, lonMax, latMin, lonMin, testUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/screen")
                                .param("latMax", String.valueOf(latMax))
                                .param("latMin", String.valueOf(latMin))
                                .param("lonMax", String.valueOf(lonMax))
                                .param("lonMin", String.valueOf(lonMin))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getRectanglePins"))
                .andExpect(status().isOk());

        // 반환된 데이터의 개수 검증
        resultActions.andExpect(jsonPath("$.data.length()").value(pins.size()));

        // 개별 요소 검증
        for (int i = 0; i < pins.size(); i++) {
            Pin p = pins.get(i);
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(p.getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(p.getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(p.getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(p.getPinTags().size()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i))
                            .value(matchesPattern(p.getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].modifiedAt".formatted(i))
                            .value(matchesPattern(p.getModifiedAt().toString().replaceAll("0+$", "") + ".*")));
        }
    }


    @Test
    @DisplayName("특정 지점에서 범위 내 좌표 확인- 비로그인 - 사각형")
    void t3_3_2() throws Exception {

        Pin pin = pinRepository.findAll().get(0);
        double centerLat = pin.getPoint().getY();
        double centerLon = pin.getPoint().getX();

        double delta = 0.01; // 대략 1km 정도의 범위
        double latMax = centerLat + delta;
        double latMin = centerLat - delta;
        double lonMax = centerLon + delta;
        double lonMin = centerLon - delta;

        List<Pin> pins = pinRepository.findPublicScreenPins(latMax, lonMax, latMin, lonMin);

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/screen")
                                .param("latMax", String.valueOf(latMax))
                                .param("latMin", String.valueOf(latMin))
                                .param("lonMax", String.valueOf(lonMax))
                                .param("lonMin", String.valueOf(lonMin))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getRectanglePins"))
                .andExpect(status().isOk());

        // 반환된 데이터의 개수 검증
        resultActions.andExpect(jsonPath("$.data.length()").value(pins.size()));

        // 개별 요소 검증
        for (int i = 0; i < pins.size(); i++) {
            Pin p = pins.get(i);
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(p.getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(p.getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(p.getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(p.getPinTags().size()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i))
                            .value(matchesPattern(p.getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].modifiedAt".formatted(i))
                            .value(matchesPattern(p.getModifiedAt().toString().replaceAll("0+$", "") + ".*")));
        }

    }

    @Test
    @DisplayName("특정 지점에서 범위 내 핀 확인 - 핀 없음 - 사각형")
    void t3_4() throws Exception {
        double centerLat = 0;
        double centerLon = 0;

        double delta = 0.01; // 대략 1km 정도의 범위
        double latMax = centerLat + delta;
        double latMin = centerLat - delta;
        double lonMax = centerLon + delta;
        double lonMin = centerLon - delta;

        List<Pin> pins = pinRepository.findPublicScreenPins(latMax, lonMax, latMin, lonMin);

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/screen")
                                .param("latMax", String.valueOf(latMax))
                                .param("latMin", String.valueOf(latMin))
                                .param("lonMax", String.valueOf(lonMax))
                                .param("lonMin", String.valueOf(lonMin))
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getRectanglePins"))
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").isEmpty());
    }


    @Test
    @DisplayName("모든 핀 리턴")
    void t4_1_1() throws Exception {
        List<Pin> pins = pinRepository.findAllAccessiblePins(testUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/all")
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getAll"))
                .andExpect(status().isOk());

        for (int i = 0; i < pins.size(); i++) {
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(pins.get(i).getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(pins.get(i).getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(pins.get(i).getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i)).value(matchesPattern(pins.get(i).getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(pins.get(i).getPinTags().size()));
        }
    }

    @Test
    @DisplayName("모든 핀 리턴 - 비로그인")
    void t4_1_2() throws Exception {
        List<Pin> pins = pinRepository.findAllPublicPins();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/all")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getAll"))
                .andExpect(status().isOk());

        for (int i = 0; i < pins.size(); i++) {
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(pins.get(i).getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(pins.get(i).getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(pins.get(i).getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i)).value(matchesPattern(pins.get(i).getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(pins.get(i).getPinTags().size()));
        }
    }

    @Test
    @DisplayName("특정 사용자 핀 리턴 -성공 ")
    void t4_2_1() throws Exception {
        User testUser = userRepository.getReferenceById(1L);
        List<Pin> pins = pinRepository.findAccessibleByUser(testUser.getId(), testUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/user/%s".formatted(targetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getUserPins"))
                .andExpect(status().isOk());

        for (int i = 0; i < pins.size(); i++) {
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(pins.get(i).getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(pins.get(i).getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(pins.get(i).getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i)).value(matchesPattern(pins.get(i).getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(pins.get(i).getPinTags().size()));
        }
    }

    @Test
    @DisplayName("특정 사용자 핀 리턴 - 비로그인 - 성공 ")
    void t4_2_2() throws Exception {
        User testUser = userRepository.getReferenceById(1L);
        List<Pin> pins = pinRepository.findPublicByUser(testUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/user/%s".formatted(targetId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getUserPins"))
                .andExpect(status().isOk());

        for (int i = 0; i < pins.size(); i++) {
            resultActions
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(pins.get(i).getId()))
                    .andExpect(jsonPath("$.data[%d].latitude".formatted(i)).value(pins.get(i).getPoint().getY()))
                    .andExpect(jsonPath("$.data[%d].longitude".formatted(i)).value(pins.get(i).getPoint().getX()))
                    .andExpect(jsonPath("$.data[%d].createdAt".formatted(i)).value(matchesPattern(pins.get(i).getCreatedAt().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data[%d].pinTags.length()".formatted(i)).value(pins.get(i).getPinTags().size()));
        }
    }

    @Test
    @DisplayName("특정 사용자 핀 리턴 -실패 ")
    void t4_3() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        get("/api/pins/user/%s".formatted(failedTargetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getUserPins"))
                .andExpect(status().isNotFound());

    }

    @Test
    @DisplayName("핀 내용 수정")
    void t5_1_1() throws Exception {

        String content = "updated Content!";
        Pin pin = pinRepository.findById(targetId).get();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s".formatted(targetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "%s"
                                        }
                                        """.formatted(content))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("updatePinContent"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.id").value(targetId))
                .andExpect(jsonPath("$.data.latitude").value(pin.getPoint().getY()))
                .andExpect(jsonPath("$.data.longitude").value(pin.getPoint().getX()))
                .andExpect(jsonPath("$.data.content").value(content));
    }

    @Test
    @DisplayName("핀 내용 수정 - 실패 (id없음)")
    void t5_1_2() throws Exception {

        String content = "updated Content!";
        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s".formatted(failedTargetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "%s"
                                        }
                                        """.formatted(content))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("updatePinContent"))
                .andExpect(jsonPath("$.errorCode").value("1002"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 내용 수정 - 실패 (내용 없음)")
    void t5_1_3() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s".formatted(targetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("updatePinContent"))
                .andExpect(jsonPath("$.errorCode").value("1005"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 내용 수정 - 실패 (권한 없음)")
    void t5_1_4() throws Exception {
        String content = "updated Content!";
        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s".formatted(noAuthId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "%s"
                                        }
                                        """.formatted(content))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("updatePinContent"))
                .andExpect(jsonPath("$.errorCode").value("1010"))
                .andExpect(jsonPath("$.msg").exists());
    }


    @Test
    @DisplayName("핀 공개 여부 수정")
    void t5_2_1() throws Exception {

        Pin pin = pinRepository.findById(targetId).get();

        String expectedIsPublicString = String.valueOf(!pin.getIsPublic());

        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s/public".formatted(targetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("changePinPublic"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.id").value(targetId))
                .andExpect(jsonPath("$.data.isPublic").value(expectedIsPublicString));
    }

    @Test
    @DisplayName("핀 공개 여부 수정 - 실패 (id 없음)")
    void t5_2_2() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s/public".formatted(failedTargetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("changePinPublic"))
                .andExpect(jsonPath("$.errorCode").value("1002"))
                .andExpect(jsonPath("$.msg").exists());

    }

    @Test
    @DisplayName("핀 공개 여부 수정 - 실패 (권한 없음)")
    void t5_2_3() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        put("/api/pins/%s/public".formatted(noAuthId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("changePinPublic"))
                .andExpect(jsonPath("$.errorCode").value("1010"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 삭제 - 성공")
    void t6_1() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/pins/%s".formatted(targetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("deletePin"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("핀 삭제 - 실패 (id없음)")
    void t6_2() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/pins/%s".formatted(failedTargetId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("deletePin"))
                .andExpect(jsonPath("$.errorCode").value("1002"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("핀 삭제 - 실패 (권한 없음)")
    void t6_3() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/pins/%s".formatted(noAuthId))
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("deletePin"))
                .andExpect(jsonPath("$.errorCode").value("1010"))
                .andExpect(jsonPath("$.msg").exists());
    }


    @Test
    @DisplayName("좋아요 저장 성공")
    @Transactional
    void 좋아요등록성공() throws Exception {
        //given
        Long pinId = 5L;
        Long userId = 2L;
        String requestBody = "{\"userId\": " + userId + "}";
        User testUser = userService.findById(userId);

        int likeCnt = likesService.getLikesCount(pinId);

        // when & then
        mvc.perform(
                        post("/api/pins/{pinId}/likes", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(likeCnt + 1));

        // DB 검증
        Likes likes = likesRepository.findByPinIdAndUserId(pinId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LIKES_CREATE_FAILED));
//        assertThat(likes.getLiked()).isTrue();
        assertThat(likes.getPin().getId()).isEqualTo(pinId);
        assertThat(likes.getUser().getId()).isEqualTo(userId);
        assertThat(likes.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("좋아요 저장 실패 - 존재하지 않는 핀")
    @Transactional
    void likesCreatefailPinId() throws Exception {
        // given
        Long pinId = 99999L;
        Long userId = 2L;
        String requestBody = "{\"userId\": " + userId + "}";
        User testUser = userService.findById(userId);

        // when & then
        mvc.perform(
                        post("/api/pins/{pinId}/likes", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("5002"))
                .andExpect(jsonPath("$.msg").value("잘못된 핀 정보입니다."));

        // DB 검증
        Optional<Likes> likes = likesRepository.findByPinIdAndUserId(pinId, userId);
        assertThat(likes).isEmpty();
    }

//    @Test
//    @DisplayName("좋아요 저장 실패 - 존재하지 않는 사용자 - 테스트 불가")
//    @Transactional
//    void likesCreatefailUserId() throws Exception {
//        // 인증 도입으로 존재하지 않는 사용자 테스트 불가
//        // given
//        Long pinId = 2L;
//        Long userId = 999L;
//        String requestBody = "{\"userId\": " + userId + "}";
//        User testUser = userService.findById(userId);
//
//        // when & then
//        mvc.perform(
//                        post("/api/pins/{pinId}/likes", pinId)
//                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(requestBody)
////                                .with(csrf())
////                                .with(user("testuser").roles("USER"))
//                )
//                .andDo(print())
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.errorCode").value("2006"));
//
//        // DB 검증
//        Optional<Likes> likes = likesRepository.findByPinIdAndUserId(pinId, userId);
//        assertThat(likes).isEmpty();
//    }

    @Test
    @DisplayName("좋아요 취소 성공 - 좋아요 true")
    @Transactional
    void likesDeleteTSuccess() throws Exception {
        //given
        Long pinId = 1L;
        Long userId = 1L;
        String requestBody = "{\"userId\": " + userId + "}";
        int lcount = likesService.getLikesCount(pinId);


        // when & then
        mvc.perform(
                        delete("/api/pins/{pinId}/likes", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(lcount - 1));

        // DB 검증
        Likes likes = likesRepository.findByPinIdAndUserId(pinId, userId).orElse(null);
        assertThat(likes).isNull();
    }

    @Test
    @DisplayName("좋아요 취소 다시 좋아요 성공 - 좋아요 true")
    @Transactional
    void likesDeleteFSuccess() throws Exception {
        //given
        Long pinId = 4L;
        Long userId = 1L;
        String requestBody = "{\"userId\": " + userId + "}";

        // when & then
        mvc.perform(
                        post("/api/pins/{pinId}/likes", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // DB 검증
        Likes likes = likesRepository.findByPinIdAndUserId(pinId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LIKES_CREATE_FAILED));
        assertThat(likes.getPin().getId()).isEqualTo(pinId);
        assertThat(likes.getUser().getId()).isEqualTo(userId);
        assertThat(likes.getModifiedAt()).isNotNull();
    }


    @Test
    @DisplayName("좋아요 토글 성공 - 좋아요 취소 -> 등록")
    @Transactional
    void likseToggleTF() throws Exception {
        // given
        Long pinId = 2L;
        Long userId = 1L;
        String requestBody = "{\"userId\": " + userId + "}";

        // when & then
        mvc.perform(
                        delete("/api/pins/{pinId}/likes", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.data.isLiked").value(false));

        // DB 검증
        Likes likes = likesRepository.findByPinIdAndUserId(pinId, userId).orElse(null);
        assertThat(likes).isNull();

        // 좋아요 재등록
        mvc.perform(
                        post("/api/pins/{pinId}/likes", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))
                .andExpect(jsonPath("$.data.isLiked").value(true));

        // DB 검증
        likes = likesRepository.findByPinIdAndUserId(pinId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LIKES_CREATE_FAILED));
    }


    @Test
    @DisplayName("좋아요 개수 가져오기 성공 - 특정 핀 조회")
    @Transactional
    void likeGetLikeCountTByPin() throws Exception {
        // given
        Long pinId = 1L;
        User testUser = userService.findById(1L);
        Pin pin = pinService.findById(pinId, userService.findById(1L));

        // when & then
        mvc.perform(
                        get("/api/pins/{pinId}", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getPinById"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data.id").value(pin.getId()))
                .andExpect(jsonPath("$.data.likeCount").value(likesRepository.countByPinId(pinId)));
    }

    @Test
    @DisplayName("좋아요 개수 가져오기 성공 - 취소된 핀")
    void likeGetLikeCountByPin() throws Exception {
        // given
        Long pinId = 4L;
        User user = userService.findById(1L);
        Pin pin = pinService.findById(pinId, testUser);

        // when & then
        mvc.perform(
                        get("/api/pins/{pinId}", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getPinById"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data.id").value(pin.getId()))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }


    @Test
    @DisplayName("좋아요한 사용자 목록 조회 성공")
    void likesGetUsersWhoLikedPin() throws Exception {
        // given
        Long pinId = 1L;

        Integer[] userIds = likesRepository.findUsersByPinId(pinId)
                .stream()
                .map(User::getId)
                .map(id -> id.intValue())
                .toArray(Integer[]::new);

        // when & then
        mvc.perform(
                        get("/api/pins/{pinId}/likesusers", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getUsersWhoLikedPin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(likesRepository.countByPinId(pinId)))
                .andExpect(jsonPath("$.data[*].id", containsInAnyOrder(userIds)));
    }

    @Test
    @DisplayName("좋아요한 사용자 목록 조회 성공 - 취소건")
    void likesGetUsersWhoLikedPinF() throws Exception {
        // given
        Long pinId = 4L;

        Integer[] userIds = likesRepository.findUsersByPinId(pinId)
                .stream()
                .map(User::getId)
                .map(id -> id.intValue())
                .toArray(Integer[]::new);

        // when & then
        mvc.perform(
                        get("/api/pins/{pinId}/likesusers", pinId)
                                .header("Authorization", "Bearer %s %s".formatted(testUser.getApiKey(), jwtToken))
//                                .with(csrf())
//                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
                )
                .andDo(print())
                .andExpect(handler().handlerType(PinController.class))
                .andExpect(handler().methodName("getUsersWhoLikedPin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("200"))

                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.data[*].id", containsInAnyOrder(userIds)));
    }
}
