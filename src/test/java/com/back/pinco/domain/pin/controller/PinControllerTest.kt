package com.back.pinco.domain.pin.controller

import com.back.pinco.domain.likes.repository.LikesRepository
import com.back.pinco.domain.likes.service.LikesService
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.pin.service.PinService
import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.repository.UserRepository
import com.back.pinco.domain.user.service.UserService
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.security.JwtTokenProvider
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
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
import java.util.function.Supplier

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PinControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var pinRepository: PinRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var pinService: PinService

    @Autowired
    private lateinit var likesRepository: LikesRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var likesService: LikesService


    var targetId: Long = 1L
    var failedTargetId: Long = Int.MAX_VALUE.toLong()

    var noAuthId: Long = 4L

    private lateinit var jwtToken: String
    private lateinit var testUser: User


    @BeforeEach
    fun setUp() {
        testUser = userRepository.findById(1L).get()
        jwtToken =
            jwtTokenProvider.generateAccessToken(testUser.id, testUser.email, testUser.userName)
    }


    @Test
    @DisplayName("핀 생성")
    @Throws(Exception::class)
    fun t1_1() {
        val lat = 0.0
        val lon = 0.0
        val content = "new Content!"

        val jsonContent = String.format(
            """
                        {
                            "content": "$content",
                            "latitude" : $lat, 
                            "longitude" : $lon
                        }
                        
                        """.trimIndent()
        )

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/pins")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonContent)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createPin"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        resultActions
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.latitude").value(lat))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.longitude").value(lon))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value(content))
    }

    @Test
    @DisplayName("핀 생성 - 실패 (경도 정보 오류)")
    @Throws(Exception::class)
    fun t1_2() {
        val lat = 0.0

        val content = "new Content!"

        val jsonContent = String.format(
            """
                        {
                            "content": "$content",
                            "latitude" : $lat
                        }
                        
                        """.trimIndent()
        )

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/pins")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonContent)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createPin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1007"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 생성 - 실패 (위도 정보 오류)")
    @Throws(Exception::class)
    fun t1_3() {
        val lon = 0.0
        val content = "new Content!"

        val jsonContent = String.format(
            """
                        {
                            "content": "$content",
                            "longitude" : $lon
                        }
                        
                        """.trimIndent()
        )

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/pins")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonContent)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createPin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1006"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 생성 - 실패 (내용 정보 오류)")
    @Throws(Exception::class)
    fun t1_4() {
        val lat = 0.0
        val lon = 0.0

        val jsonContent = String.format(
            """
                        {
                            "latitude" : $lat, 
                            "longitude" : $lon
                        }
                        
                        """.trimIndent()
        )

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/pins")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonContent)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createPin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1005"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 생성 - 실패 (jwt 없음)")
    @Throws(Exception::class)
    fun t1_5() {
        val lat = 0.0
        val lon = 0.0
        val content = "new Content!"

        val jsonContent = String.format(
            """
                        {
                            "content": "$content",
                            "latitude" : $lat, 
                            "longitude" : $lon
                        }
                        
                        """.trimIndent()
        )

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/pins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonContent)
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().`is`(401))
    }

    @Test
    @DisplayName("id로 핀 조회 - 로그인 - 성공")
    @Throws(Exception::class)
    fun t2_1_1() {
        val pin = pinRepository.findAccessiblePinById(targetId, testUser.id).get()
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/$targetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinById"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(pin.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.latitude").value(pin.point.y))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.longitude").value(pin.point.x))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.createdAt").value(
                    Matchers.matchesPattern(
                        pin.createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.modifiedAt").value(
                    Matchers.matchesPattern(
                        pin.modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pinTags.length()").value(pin.pinTags.size))
    }

    @Test
    @DisplayName("id로 핀 조회 - 비 로그인 - 성공")
    @Throws(Exception::class)
    fun t2_1_2() {
        val pin = pinRepository.findPublicPinById(targetId).get()
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/$targetId")

            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinById"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(pin.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.latitude").value(pin.point.y))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.longitude").value(pin.point.x))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.createdAt").value(
                    Matchers.matchesPattern(
                        pin.createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.modifiedAt").value(
                    Matchers.matchesPattern(
                        pin.modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.pinTags.length()").value(pin.pinTags.size))
    }

    @Test
    @DisplayName("id로 핀 조회 - 로그인 - 실패 (id가 없음)")
    @Throws(Exception::class)
    fun t2_2() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/$failedTargetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinById"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("id로 핀 조회 - 비로그인 - 실패 (id가 없음)")
    @Throws(Exception::class)
    fun t2_3() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/$failedTargetId")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinById"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("특정 지점에서 범위 내 좌표 확인")
    @Throws(Exception::class)
    fun t3_1_1() {
        val pin = pinRepository.findById(targetId).get()
        val pins = pinRepository.findPinsWithinRadius(
            pin.point.x,
            pin.point.y,
            1000.0,
            testUser.id
        )

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins")
                    .param("radius", 1000.toString())
                    .param("longitude", pin.point.x.toString())
                    .param("latitude", pin.point.y.toString())
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getRadiusPins"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("특정 지점에서 범위 내 좌표 확인- 비로그인")
    @Throws(Exception::class)
    fun t3_1_2() {
        val pin = pinRepository.findById(targetId).get()
        val pins = pinRepository.findPublicPinsWithinRadius(pin.point.x, pin.point.y, 1000.0)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins")
                    .param("radius", 1000.toString())
                    .param("longitude", pin.point.x.toString())
                    .param("latitude", pin.point.y.toString())
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getRadiusPins"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("특정 지점에서 범위 내 핀 확인 - 핀 없음")
    @Throws(Exception::class)
    fun t3_2() {
        // 핀이 없는 위치와 반경을 설정합니다.
        val outOfRangeLat = 0.0
        val outOfRangeLon = 0.0
        val radius = 10.0 // 10미터

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins")
                    .param("radius", radius.toString())
                    .param("latitude", outOfRangeLat.toString())
                    .param("longitude", outOfRangeLon.toString())
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getRadiusPins"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isEmpty())
    }

    @Test
    @DisplayName("특정 범위(사각형) 내 좌표 확인 - 로그인 - 사각형")
    @Throws(Exception::class)
    fun t3_3_1() {
        val pin = pinRepository.findAll()[0]
        val centerLat = pin.point.y
        val centerLon = pin.point.x

        val delta = 0.01 // 대략 1km 정도의 범위
        val latMax = centerLat + delta
        val latMin = centerLat - delta
        val lonMax = centerLon + delta
        val lonMin = centerLon - delta

        val pins = pinRepository.findScreenPins(latMax, lonMax, latMin, lonMin, testUser.id)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/screen")
                    .param("latMax", latMax.toString())
                    .param("latMin", latMin.toString())
                    .param("lonMax", lonMax.toString())
                    .param("lonMin", lonMin.toString())
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getRectanglePins"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        // 반환된 데이터 검증
        resultActions.andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(pins.size))
        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }


    @Test
    @DisplayName("특정 지점에서 범위 내 좌표 확인- 비로그인 - 사각형")
    @Throws(Exception::class)
    fun t3_3_2() {
        val pin = pinRepository.findAll()[0]
        val centerLat = pin.point.y
        val centerLon = pin.point.x

        val delta = 0.01 // 대략 1km 정도의 범위
        val latMax = centerLat + delta
        val latMin = centerLat - delta
        val lonMax = centerLon + delta
        val lonMin = centerLon - delta

        val pins = pinRepository.findPublicScreenPins(latMax, lonMax, latMin, lonMin)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/screen")
                    .param("latMax", latMax.toString())
                    .param("latMin", latMin.toString())
                    .param("lonMax", lonMax.toString())
                    .param("lonMin", lonMin.toString())
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getRectanglePins"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        // 반환된 데이터의 개수 검증
        resultActions.andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(pins.size))

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("특정 지점에서 범위 내 핀 확인 - 핀 없음 - 사각형")
    @Throws(Exception::class)
    fun t3_4() {
        val centerLat = 0.0
        val centerLon = 0.0

        val delta = 0.01 // 대략 1km 정도의 범위
        val latMax = centerLat + delta
        val latMin = centerLat - delta
        val lonMax = centerLon + delta
        val lonMin = centerLon - delta

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/screen")
                    .param("latMax", latMax.toString())
                    .param("latMin", latMin.toString())
                    .param("lonMax", lonMax.toString())
                    .param("lonMin", lonMin.toString())
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getRectanglePins"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isEmpty())
    }


    @Test
    @DisplayName("모든 핀 리턴")
    @Throws(Exception::class)
    fun t4_1_1() {
        val pins = pinRepository.findAllAccessiblePins(testUser.id)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/all")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getAll"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("모든 핀 리턴 - 비로그인")
    @Throws(Exception::class)
    fun t4_1_2() {
        val pins = pinRepository.findAllPublicPins()

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/all")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getAll"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("특정 사용자 핀 리턴 -성공 ")
    @Throws(Exception::class)
    fun t4_2_1() {
        val testUser = userRepository.getReferenceById(1L)
        val pins = pinRepository.findAccessibleByUser(testUser.id, testUser.id)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/user/$targetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getUserPins"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("특정 사용자 핀 리턴 - 비로그인 - 성공 ")
    @Throws(Exception::class)
    fun t4_2_2() {
        val testUser = userRepository.getReferenceById(1L)
        val pins = pinRepository.findPublicByUser(testUser.id)

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/user/$targetId")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getUserPins"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        for (i in pins.indices) {
            resultActions
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[$i].id").value(pins[i].id))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].latitude")
                        .value(pins[i].point.y)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].longitude")
                        .value(pins[i].point.x)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].createdAt").value(
                        Matchers.matchesPattern(
                            pins[i].createdAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].modifiedAt").value(
                        Matchers.matchesPattern(
                            pins[i].modifiedAt.toString().replace("0+$".toRegex(), "") + ".*"
                        )
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data[$i].pinTags.length()")
                        .value(pins[i].pinTags.size)
                )
        }
    }

    @Test
    @DisplayName("특정 사용자 핀 리턴 -실패 ")
    @Throws(Exception::class)
    fun t4_3() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/pins/user/$failedTargetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getUserPins"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
    }

    @Test
    @DisplayName("핀 내용 수정")
    @Throws(Exception::class)
    fun t5_1_1() {
        val content = "updated Content!"
        val pin = pinRepository.findById(targetId).get()

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$targetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "content": "$content"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("updatePinContent"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        resultActions
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(targetId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.latitude").value(pin.point.y))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.longitude").value(pin.point.x))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value(content))
    }

    @Test
    @DisplayName("핀 내용 수정 - 실패 (id없음)")
    @Throws(Exception::class)
    fun t5_1_2() {
        val content = "updated Content!"
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$failedTargetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "content": "$content"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("updatePinContent"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 내용 수정 - 실패 (내용 없음)")
    @Throws(Exception::class)
    fun t5_1_3() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$targetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("updatePinContent"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1005"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 내용 수정 - 실패 (권한 없음)")
    @Throws(Exception::class)
    fun t5_1_4() {
        val content = "updated Content!"
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$noAuthId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "content": "$content"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("updatePinContent"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1010"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }


    @Test
    @DisplayName("핀 공개 여부 수정")
    @Throws(Exception::class)
    fun t5_2_1() {
        val pin = pinRepository.findById(targetId).get()

        val expectedIsPublicString = (!pin.isPublic).toString()

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$targetId/public")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changePinPublic"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        resultActions
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(targetId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isPublic").value(expectedIsPublicString))
    }

    @Test
    @DisplayName("핀 공개 여부 수정 - 실패 (id 없음)")
    @Throws(Exception::class)
    fun t5_2_2() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$failedTargetId/public")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changePinPublic"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 공개 여부 수정 - 실패 (권한 없음)")
    @Throws(Exception::class)
    fun t5_2_3() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/pins/$noAuthId/public")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changePinPublic"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1010"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 삭제 - 성공")
    @Throws(Exception::class)
    fun t6_1() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/pins/$targetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deletePin"))
            .andExpect(MockMvcResultMatchers.status().isOk())
    }

    @Test
    @DisplayName("핀 삭제 - 실패 (id없음)")
    @Throws(Exception::class)
    fun t6_2() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/pins/$failedTargetId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deletePin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @DisplayName("핀 삭제 - 실패 (권한 없음)")
    @Throws(Exception::class)
    fun t6_3() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/pins/$noAuthId")
                    .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deletePin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("1010"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }


    @Test
    @DisplayName("좋아요 저장 성공")
    @Transactional
    @Throws(Exception::class)
    fun t7_1() {
        //given
        val pinId = 5L
        val userId = 2L
        val requestBody = "{\"userId\": $userId}"
        val testUser = userService.findById(userId)

        val likeCnt = likesService.getLikesCount(pinId)

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/$pinId/likes" )
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isLiked").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.likeCount").value(likeCnt + 1))

        // DB 검증
        val likes = likesRepository.findByPinIdAndUserId(pinId, userId)
            .orElseThrow(Supplier { ServiceException(ErrorCode.LIKES_CREATE_FAILED) })
        //        assertThat(likes.getLiked()).isTrue();
        Assertions.assertThat(likes.pin.id).isEqualTo(pinId)
        Assertions.assertThat(likes.user.id).isEqualTo(userId)
        Assertions.assertThat(likes.createdAt).isNotNull()
    }

    @Test
    @DisplayName("좋아요 저장 실패 - 존재하지 않는 핀")
    @Transactional
    @Throws(Exception::class)
    fun t7_2() {
        // given
        val pinId = 99999L
        val userId = 2L
        val requestBody = "{\"userId\": $userId}"
        val testUser = userService.findById(userId)

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/$pinId/likes")
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("5002"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("잘못된 핀 정보입니다."))

        // DB 검증
        val likes = likesRepository.findByPinIdAndUserId(pinId, userId)
        Assertions.assertThat(likes).isEmpty()
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
    //                                .header("Authorization", "Bearer ${testUser.getApiKey()} $jwtToken")
    //                                .contentType(MediaType.APPLICATION_JSON)
    //                                .content(requestBody)
    //                                .with(csrf())
    //                                .with(user("testuser").roles("USER")) */
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
    @Throws(Exception::class)
    fun t8_1() {
        //given
        val pinId = 1L
        val userId = 1L
        val requestBody = "{\"userId\": $userId}"
        val lcount = likesService.getLikesCount(pinId)


        // when & then
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/pins/$pinId/likes")
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isLiked").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.likeCount").value(lcount - 1))

        // DB 검증
        val likes = likesRepository.findByPinIdAndUserId(pinId, userId).orElse(null)
        Assertions.assertThat(likes).isNull()
    }

    @Test
    @DisplayName("좋아요 취소 다시 좋아요 성공 - 좋아요 true")
    @Transactional
    @Throws(Exception::class)
    fun t8_2() {
        //given
        val pinId = 4L
        val userId = 1L
        val requestBody = "{\"userId\": $userId}"

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/$pinId/likes")
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isLiked").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.likeCount").value(1))

        // DB 검증
        val likes = likesRepository.findByPinIdAndUserId(pinId, userId)
            .orElseThrow(Supplier { ServiceException(ErrorCode.LIKES_CREATE_FAILED) })
        Assertions.assertThat(likes.pin.id).isEqualTo(pinId)
        Assertions.assertThat(likes.user.id).isEqualTo(userId)
        Assertions.assertThat(likes.modifiedAt).isNotNull()
    }


    @Test
    @DisplayName("좋아요 토글 성공 - 좋아요 취소 -> 등록")
    @Transactional
    @Throws(Exception::class)
    fun likseToggleTF() {
        // given
        val pinId = 2L
        val userId = 1L
        val requestBody = "{\"userId\": $userId}"

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/pins/$pinId/likes")
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isLiked").value(false))

        // DB 검증
        val likes = likesRepository.findByPinIdAndUserId(pinId, userId).orElse(null)
        Assertions.assertThat(likes).isNull()

        // 좋아요 재등록
        mvc.perform(
            MockMvcRequestBuilders.post("/api/pins/{pinId}/likes", pinId)
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isLiked").value(true))

        // DB 검증
        likesRepository.findByPinIdAndUserId(pinId, userId)
            .orElseThrow(Supplier { ServiceException(ErrorCode.LIKES_CREATE_FAILED) })
    }


    @Test
    @DisplayName("좋아요 개수 가져오기 성공 - 특정 핀 조회")
    @Transactional
    @Throws(Exception::class)
    fun likeGetLikeCountTByPin() {
        // given
        val pinId = 1L
        val testUser = userService.findById(1L)
        val pin = pinService.findById(pinId, userService.findById(1L))

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.get("/api/pins/$pinId")
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinById"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(pin.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.likeCount").value(likesRepository.countByPinId(pinId)))
    }

    @Test
    @DisplayName("좋아요 개수 가져오기 성공 - 취소된 핀")
    @Throws(Exception::class)
    fun likeGetLikeCountByPin() {
        // given
        val pinId = 4L
        val pin = pinService.findById(pinId, testUser)

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.get("/api/pins/$pinId")
                .header("Authorization", "Bearer ${testUser.apiKey} $jwtToken")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getPinById"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(pin.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.likeCount").value(0))
    }


    @Test
    @DisplayName("좋아요한 사용자 목록 조회 성공")
    @Throws(Exception::class)
    fun likesGetUsersWhoLikedPin() {
        // given
        val pinId = 1L

        val userIds = likesRepository.findUsersByPinId(pinId)
            .map { it.id!!.toInt() }
            .toTypedArray()


        // when & then
        mvc.perform(
            MockMvcRequestBuilders.get("/api/pins/$pinId/likesusers")
                .header(
                    "Authorization",
                    "Bearer ${testUser.apiKey} $jwtToken"
                ) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getUsersWhoLikedPin"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(likesRepository.countByPinId(pinId)))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.data[*].id",
                    Matchers.containsInAnyOrder(*userIds)
                )
            )
    }

    @Test
    @DisplayName("좋아요한 사용자 목록 조회 성공 - 취소건")
    @Throws(Exception::class)
    fun likesGetUsersWhoLikedPinF() {
        // given
        val pinId = 4L

        val userIds = likesRepository.findUsersByPinId(pinId)
            .map { it.id!!.toInt() }
            .toTypedArray()


        // when & then
        mvc.perform(
            MockMvcRequestBuilders.get("/api/pins/$pinId/likesusers")
                .header(
                    "Authorization",
                    "Bearer ${testUser.apiKey} $jwtToken"
                ) //                                .with(csrf())
            //                                .with(user("testuser").roles("USER"))  // 인증 사용자 추가
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(PinController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getUsersWhoLikedPin"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("200"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(0))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.data[*].id",
                    Matchers.containsInAnyOrder(*userIds)
                )
            )
    }
}
