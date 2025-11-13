package com.back.pinco.domain.user.controller;

import com.back.pinco.domain.likes.dto.PinsLikedByUserResponse;
import com.back.pinco.domain.likes.service.LikesService;
import com.back.pinco.domain.pin.dto.PinDto;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.user.dto.UserDto;
import com.back.pinco.domain.user.dto.UserReqBody.*;
import com.back.pinco.domain.user.dto.UserResBody.*;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.service.AuthService;
import com.back.pinco.domain.user.service.UserService;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import com.back.pinco.global.rq.Rq;
import com.back.pinco.global.rsData.RsData;
import com.back.pinco.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.OnError;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "User", description = "회원 관리 기능")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final LikesService likesService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final Rq rq;

    @Operation(summary = "회원 가입", description = "이메일, 회원이름, 비밀번호를 입력받아 회원가입합니다.")
    @PostMapping("/join")
    public RsData<JoinResponse> join(
            @RequestBody JoinRequest reqBody
    ) {
        User user = userService.createUser(reqBody.email(), reqBody.password(), reqBody.userName());
        String apiKey = userService.ensureApiKey(user);
        String access = authService.genAccessToken(user);
        String refresh = authService.genRefreshToken(user);

        rq.setCookie("apiKey", apiKey);
        rq.setCookie("accessToken", access);

        return new RsData<>(
                "200",
                "회원 가입이 완료되었습니다",
                new JoinResponse(new UserDto(user))
        );
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호를 입력받아 로그인합니다.")
    @PostMapping("/login")
    public RsData<Map<String, String>> login(
            @RequestBody LoginRequest reqBody
    ) {
        userService.login(reqBody.email(), reqBody.password());
        User user = userService.findByEmail(reqBody.email());

        // apiKey 보장
        String apiKey = userService.ensureApiKey(user);
        // 토큰 발급
        String accessToken = authService.genAccessToken(user);
        String refreshToken = authService.genRefreshToken(user);

        // 쿠키
        rq.setCookie("apiKey", apiKey);
        rq.setCookie("accessToken", accessToken);

        return new RsData<>(
                "200",
                "로그인 성공",
                Map.of(
                        "apiKey", apiKey,
                        "accessToken", accessToken,
                        "refreshToken", refreshToken
                )
        );
    }

    @Operation(summary = "로그아웃", description = "사용자의 인증 쿠키(accessToken, refreshToken, apiKey)를 만료시켜 세션을 종료합니다.")
    @PostMapping("/logout")
    public RsData<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        authService.logout(req, res);
        return new RsData<>(
                "200",
                "로그아웃 성공"
        );
    }

    @Operation(summary = "토큰 재발급", description = "refreshToken이 유효한 경우 새로운 accessToken과 refreshToken 발급하여 로그인 세션을 연장합니다.")
    @PostMapping("/reissue")
    public RsData<Map<String, String>> reissue(
            @RequestBody Map<String, String> body
    ) {
        String refreshToken = body.getOrDefault("refreshToken", "");
        if (refreshToken.isBlank() || !jwtTokenProvider.isValid(refreshToken)) {
            throw new ServiceException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userService.findById(userId);

        String newAccess = authService.genAccessToken(user);
        String newRefresh = authService.genRefreshToken(user);

        rq.setCookie("accessToken", newAccess);
        rq.setHeader("Authorization", "Bearer " + user.getApiKey() + " " + newAccess);

        return new RsData<>(
                "200",
                "재발급 성공",
                Map.of(
                        "apiKey", user.getApiKey(),
                        "accessToken", newAccess,
                        "refreshToken", newRefresh
                )
        );
    }

    @Operation(summary = "회원 정보 조회", description = "id, 이메일, 회원 이름을 조회합니다.")
    @GetMapping("/getInfo")
    public RsData<GetInfoResponse> getUserInfo() {
        User user = rq.getActor();
        return new RsData<>(
                "200",
                "회원 정보를 성공적으로 조회했습니다.",
                new GetInfoResponse(new UserDto(user))
        );
    }

    @Operation(summary = "회원 정보 수정", description = "현재 로그인 중인 회원의 비밀번호를 입력받고, 일치하는 경우 회원 이름 또는 비밀번호를 수정합니다.")
    @PutMapping("/edit")
    public RsData<Void> edit(
            @RequestBody EditRequest reqBody
    ) {
        User currentUser = rq.getActor();
        userService.checkPwd(currentUser, reqBody.password());
        userService.editUserInfo(currentUser.getId(), reqBody.newUserName(), reqBody.newPassword());
        return new RsData<>(
                "200",
                "회원정보 수정 완료"
        );
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인 중인 회원의 비밀번호를 입력받고, 일치하는 경우 회원 정보 및 인증 쿠키를 삭제합니다.")
    @DeleteMapping("/delete")
    public RsData<Void> delete(
            @RequestBody DeleteRequest reqBody
    ) {
        User user = rq.getActor();
        userService.checkPwd(user, reqBody.password());
        userService.delete(user);
        rq.deleteCookie("accessToken");
        rq.deleteCookie("apiKey");
        return new RsData<>(
                "200",
                "회원 탈퇴가 완료되었습니다."
        );
    }

    @Operation(summary = "사용자가 좋아요 등록한 핀 목록 조회", description = "지정된 userId를 가진 사용자가 좋아요한 모든 핀 목록을 반환합니다.")
    @GetMapping("/{userId}/likespins")
    public RsData<List<PinsLikedByUserResponse>> getPinsLikedByUser(
            @PathVariable("userId") Long userId
    ) {
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다", likesService.getPinsLikedByUser(userId)
        );
    }

    @Operation(summary = "마이페이지", description = "로그인한 회원이 작성한 핀 개수 및 목록, 북마크한 핀 개수 및 목록, 받은 좋아요 수를 조회합니다.")
    @GetMapping("/mypage")
    public RsData<MyPageResponse> myPage() {
        // 로그인 사용자
        User user = rq.getActor();
        List<Pin> listPin = userService.getMyPins(); // DB 접근은 한 번만
        if (user == null) {
            throw new ServiceException(ErrorCode.AUTH_REQUIRED);
        }
        // 2) 내가 작성한 핀 개수
        int pinCount = listPin.size();

        // 3) 내가 북마크한 핀 개수
        int bookmarkCount = userService.getMyBookmarks().size();

        // 내가 지금까지 받은 총 '좋아요 수'
        long likesCount = userService.likesCount(listPin);

        return new RsData<>(
                "200",
                "마이페이지 조회 성공",
                new MyPageResponse(
                        new UserDto(user), pinCount, bookmarkCount, likesCount)
                );
    }

    @Operation(summary = "작성한 핀 조회", description = "로그인한 회원이 작성한 공개, 비공개 핀을 모두 조회합니다.")
    @GetMapping("/mypin")
    public RsData<MyPinResponse> myPin() {
        // DB 한 번만 조회
        MyPinResponse pinLists = userService.listPublicAndPrivate();

        // PinLists 내부에는 두 개의 리스트(publicPins, privatePins)가 들어있음
        List<PinDto> publicList = pinLists.publicPins();
        List<PinDto> privateList = pinLists.privatePins();

        return new RsData<>(
                "200",
                "공개 글, 비공개 글을 조회했습니다.",
                new MyPinResponse(publicList, privateList)
        );
    }

    @Operation(summary = "북마크 조회", description = "로그인한 회원이 북마크한 핀을 조회합니다.")
    @GetMapping("/mybookmark")
    public RsData<MyBookmarkResponse> myBookmark() {
        List<PinDto> bookmarkList = userService.bookmarkList();
        return new RsData<>(
                "200",
                "북마크한 게시물을 모두 조회했습니다.",
                new MyBookmarkResponse(bookmarkList)
        );
    }
}

