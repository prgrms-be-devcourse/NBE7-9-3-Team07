package com.back.pinco.domain.user.controller

import com.back.pinco.domain.likes.dto.PinsLikedByUserResponse
import com.back.pinco.domain.likes.service.LikesService
import com.back.pinco.domain.user.dto.UserDto
import com.back.pinco.domain.user.dto.UserReqBody.DeleteRequest
import com.back.pinco.domain.user.dto.UserReqBody.EditRequest
import com.back.pinco.domain.user.dto.UserReqBody.JoinRequest
import com.back.pinco.domain.user.dto.UserReqBody.LoginRequest
import com.back.pinco.domain.user.dto.UserResBody.*
import com.back.pinco.domain.user.service.AuthService
import com.back.pinco.domain.user.service.UserService
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.rq.Rq
import com.back.pinco.global.rsData.RsData
import com.back.pinco.global.security.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "회원 관리 기능")
@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val likesService: LikesService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authService: AuthService,
    private val rq: Rq
) {


    @Operation(summary = "회원 가입", description = "이메일, 회원이름, 비밀번호를 입력받아 회원가입합니다.")
    @PostMapping("/join")
    fun join(
        @RequestBody reqBody: JoinRequest
    ): RsData<JoinResponse> {
        val user = userService.createUser(reqBody.email, reqBody.password, reqBody.userName)
        val apiKey = userService.ensureApiKey(user)
        val access = authService.genAccessToken(user)
        val refresh = authService.genRefreshToken(user)

        rq.setCookie("apiKey", apiKey)
        rq.setCookie("accessToken", access)

        return RsData(
            "200",
            "회원 가입이 완료되었습니다",
            JoinResponse(UserDto(user))
        )
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호를 입력받아 로그인합니다.")
    @PostMapping("/login")
    fun login(
        @RequestBody reqBody: LoginRequest
    ): RsData<Map<String, String>> {
        userService.login(reqBody.email, reqBody.password)
        val user = userService.findByEmail(reqBody.email)

        // apiKey 보장
        val apiKey = userService.ensureApiKey(user)
        // 토큰 발급
        val accessToken = authService.genAccessToken(user)
        val refreshToken = authService.genRefreshToken(user)

        // 쿠키
        rq.setCookie("apiKey", apiKey)
        rq.setCookie("accessToken", accessToken)

        return RsData(
            "200",
            "로그인 성공",
            mapOf(
                "apiKey" to apiKey,
                "accessToken" to accessToken,
                "refreshToken" to refreshToken
            )
        )
    }

    @Operation(summary = "로그아웃", description = "사용자의 인증 쿠키(accessToken, refreshToken, apiKey)를 만료시켜 세션을 종료합니다.")
    @PostMapping("/logout")
    fun logout(req: HttpServletRequest, res: HttpServletResponse): RsData<Void> {
        authService.logout(req, res)
        return RsData(
            "200",
            "로그아웃 성공",
            null
        )
    }

    @Operation(
        summary = "토큰 재발급",
        description = "refreshToken이 유효한 경우 새로운 accessToken과 refreshToken 발급하여 로그인 세션을 연장합니다."
    )
    @PostMapping("/reissue")
    fun reissue(
        @RequestBody body: Map<String, String>
    ): RsData<Map<String, String>> {
        val refreshToken = body.getOrDefault("refreshToken", "")
        if (refreshToken.isBlank() || !jwtTokenProvider.isValid(refreshToken)) {
            throw ServiceException(ErrorCode.INVALID_ACCESS_TOKEN)
        }
        val userId = jwtTokenProvider.getUserId(refreshToken)
        val user = userService.findById(userId)

        val newAccess = authService.genAccessToken(user)
        val newRefresh = authService.genRefreshToken(user)

        rq.setCookie("accessToken", newAccess)
        rq.setHeader("Authorization", "Bearer ${user.apiKey} ${newAccess}")

        return RsData(
            "200",
            "재발급 성공",
            mapOf(
                "apiKey" to user.apiKey!!,
                "accessToken" to newAccess,
                "refreshToken" to newRefresh
            )
        )
    }

    @GetMapping("/getInfo")
    @Operation(summary = "회원 정보 조회", description = "id, 이메일, 회원 이름을 조회합니다.")
    fun getUserInfo(): RsData<GetInfoResponse> {
            val user = rq.getActorOrNull()
            return RsData(
                "200",
                "회원 정보를 성공적으로 조회했습니다.",
                GetInfoResponse(UserDto(user))
            )
        }

    @Operation(summary = "회원 정보 수정", description = "현재 로그인 중인 회원의 비밀번호를 입력받고, 일치하는 경우 회원 이름 또는 비밀번호를 수정합니다.")
    @PutMapping("/edit")
    fun edit(
        @RequestBody reqBody: EditRequest
    ): RsData<Void> {
        val currentUser = rq.getActorOrNull()
        userService.checkPwd(currentUser, reqBody.password)
        userService.editUserInfo(currentUser.id!!, reqBody.newUserName, reqBody.newPassword)
        return RsData(
            "200",
            "회원정보 수정 완료",
            null
        )
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인 중인 회원의 비밀번호를 입력받고, 일치하는 경우 회원 정보 및 인증 쿠키를 삭제합니다.")
    @DeleteMapping("/delete")
    fun delete(
        @RequestBody reqBody: DeleteRequest
    ): RsData<Void> {
        val user = rq.getActorOrNull()
        userService.checkPwd(user, reqBody.password)
        userService.delete(user)
        rq.deleteCookie("accessToken")
        rq.deleteCookie("apiKey")
        return RsData(
            "200",
            "회원 탈퇴가 완료되었습니다.",
            null
        )
    }

    @Operation(summary = "사용자가 좋아요 등록한 핀 목록 조회", description = "지정된 userId를 가진 사용자가 좋아요한 모든 핀 목록을 반환합니다.")
    @GetMapping("/{userId}/likespins")
    fun getPinsLikedByUser(
        @PathVariable("userId") userId: Long
    ): RsData<List<PinsLikedByUserResponse>> {
        return RsData(
            "200",
            "성공적으로 처리되었습니다", likesService.getPinsLikedByUser(userId)
        )
    }

    @Operation(summary = "마이페이지", description = "로그인한 회원이 작성한 핀 개수 및 목록, 북마크한 핀 개수 및 목록, 받은 좋아요 수를 조회합니다.")
    @GetMapping("/mypage")
    fun myPage(): RsData<MyPageResponse> {
        // 로그인 사용자
        val user = rq.getActorOrNull()
        val listPin = userService.getMyPins() // DB 접근은 한 번만

        //내가 작성한 핀 개수
        val pinCount = listPin.size

        // 내가 북마크한 핀 개수
        val bookmarkCount = userService.getMyBookmarks().size

        // 내가 지금까지 받은 총 '좋아요 수'
        val likesCount = userService.likesCount(listPin).toLong()

        return RsData(
            "200",
            "마이페이지 조회 성공",
            MyPageResponse(
                UserDto(user), pinCount, bookmarkCount, likesCount
            )
        )
    }

    @Operation(summary = "작성한 핀 조회", description = "로그인한 회원이 작성한 공개, 비공개 핀을 모두 조회합니다.")
    @GetMapping("/mypin")
    fun myPin(): RsData<MyPinResponse> {
        // DB 한 번만 조회
        val pinLists = userService.listPublicAndPrivate()

        // PinLists 내부에는 두 개의 리스트(publicPins, privatePins)가 들어있음
        val publicList = pinLists.publicPins
        val privateList = pinLists.privatePins

        return RsData(
            "200",
            "공개 글, 비공개 글을 조회했습니다.",
            MyPinResponse(publicList, privateList)
        )
    }

    @Operation(summary = "북마크 조회", description = "로그인한 회원이 북마크한 핀을 조회합니다.")
    @GetMapping("/mybookmark")
    fun myBookmark(): RsData<MyBookmarkResponse> {
        val bookmarkList = userService.bookmarkList()
        return RsData(
            "200",
            "북마크한 게시물을 모두 조회했습니다.",
            MyBookmarkResponse(bookmarkList)
        )
    }
}

