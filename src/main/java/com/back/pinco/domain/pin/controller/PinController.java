package com.back.pinco.domain.pin.controller;

import com.back.pinco.domain.bookmark.dto.BookmarkDto;
import com.back.pinco.domain.bookmark.dto.addBookmarkRequest;
import com.back.pinco.domain.bookmark.service.BookmarkService;
import com.back.pinco.domain.likes.dto.*;
import com.back.pinco.domain.likes.service.LikesService;
import com.back.pinco.domain.pin.dto.CreatePinRequest;
import com.back.pinco.domain.pin.dto.PinDto;
import com.back.pinco.domain.pin.dto.UpdatePinContentRequest;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.service.PinService;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.service.UserService;
import com.back.pinco.global.rq.Rq;
import com.back.pinco.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Pin", description = "pin(장소) 관리 기능")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pins")
public class PinController {

    private final PinService pinService;

    private final UserService userService;

    private final BookmarkService bookmarkService;

    private final LikesService likesService;

    private final Rq rq;


    //생성
    @Operation(summary = "핀 생성", description = "사용자의 위치와 설명을 받아 핀을 생성")
    @PostMapping
    public RsData<PinDto> createPin(@Valid @RequestBody CreatePinRequest pinReqbody) {
        User actor = rq.getActor();
        Pin pin = pinService.write(actor, pinReqbody);
        System.out.println("핀 아이디:"+pin.getUser().getId());
        PinDto pinDto= new PinDto(pin);
        return new RsData<>(
                "200",
        "성공적으로 처리되었습니다",
                pinDto
        );
    }

    //조회
    //id로 조회
    @Operation(summary = "핀 조회 - 단건 (pinId)", description = "핀의 ID로 핀을 단건 조회")
    @GetMapping("/{pinId}")
    public RsData<PinDto> getPinById(@PathVariable("pinId") Long pinId){
        User actor = rq.getActor();
        Pin pin = pinService.findById(pinId, actor);

        PinDto pinDto = new PinDto(pin);

        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                pinDto
        );
    }
    //범위로 조회
    @Operation(summary = "핀 조회 - 다건 (범위)", description = "범위로 핀을 다건 조회")
    @GetMapping
    public RsData<List<PinDto>> getRadiusPins(
            @NotNull
            @Min(-90)
            @Max(90)
            @RequestParam double latitude,
            @NotNull
            @Min(-180)
            @Max(180)
            @RequestParam double longitude,
            @RequestParam(defaultValue = "1000.0") double radius
    ) {
        User actor = rq.getActor();
        List<Pin> pins = pinService.findNearPins(latitude, longitude, radius, actor);

        List<PinDto> pinDtos = pins.stream()
                .map(PinDto::new)
                .collect(Collectors.toList());

        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                pinDtos
        );
    }

    //사용자로 조회
    @Operation(summary = "핀 조회 - 다건 (작성자+연도+월)", description = "작성자로 핀을 다건 조회")
    @GetMapping("/user/{userId}/date")
    public RsData<List<PinDto>> getUserPinsByDate(
            @NotNull
            @PathVariable Long userId,
            @NotNull
            @RequestParam double year,
            @NotNull
            @Min(1)
            @Max(12)
            @RequestParam double month
    ){
        User actor = rq.getActor();
        User writer = userService.findById(userId);
        List<Pin> pins = pinService.findByUserIdDate(actor, writer,year,month);
        List<PinDto> pinDtos = pins.stream()
                .map(PinDto::new)
                .collect(Collectors.toList());
        return new RsData<>(
                "200",
        "성공적으로 처리되었습니다",
                pinDtos
        );
    }

    //사용자로, 날짜로 조회
    @Operation(summary = "핀 조회 - 다건 (작성자)", description = "작성자로 핀을 다건 조회")
    @GetMapping("/user/{userId}")
    public RsData<List<PinDto>> getUserPins(
            @NotNull
            @PathVariable Long userId
    ){
        User actor = rq.getActor();
        User writer = userService.findById(userId);
        List<Pin> pins = pinService.findByUserId(actor, writer);
        List<PinDto> pinDtos = pins.stream()
                .map(PinDto::new)
                .collect(Collectors.toList());
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                pinDtos
        );
    }

    //전부 조회
    @Operation(summary = "핀 조회 - 다건 (all)", description = "모든 핀을 다건 조회")
    @GetMapping("/all")
    public RsData<List<PinDto>> getAll() {
        User actor = rq.getActor();
        List<Pin> pins = pinService.findAll(actor);

        List<PinDto> pinDtos = pins.stream()
                .map(PinDto::new)
                .toList();


        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                pinDtos
        );
    }

    //수정
    //핀 내용 수정
    @Operation(summary = "핀 수정 - 내용 (pinId)", description = "핀의 내용(Content)을 수정")
    @PutMapping(("/{pinId}"))
    public RsData<PinDto> updatePinContent(
            @PathVariable("pinId") Long pinId,
            @Valid @RequestBody UpdatePinContentRequest putPinReqbody
            ){
        User actor = rq.getActor();
        Pin pin = pinService.update(actor, pinId, putPinReqbody);
        PinDto pinDto = new PinDto(pin);
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                pinDto
        );
    }
    //공개 여부 수정
    @Operation(summary = "핀 수정 - 공개 여부 (pinId)", description = "핀의 공개 여부를 수정")
    @PutMapping(("/{pinId}/public"))
    public RsData<PinDto> changePinPublic(
            @PathVariable("pinId") Long pinId
    ){
        User actor = rq.getActor();
        Pin pin = pinService.changePublic(actor, pinId);
        PinDto pinDto = new PinDto(pin);
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                pinDto
        );
    }

    //삭제
    @Operation(summary = "핀 삭제 - pinId", description = "핀을 id로 조회하여 삭제")
    @DeleteMapping("/{pinId}")
    public RsData<Void> deletePin(@PathVariable Long pinId) {
        User actor = rq.getActor();
        pinService.deleteById(pinId,actor);
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                null
        );
    }


    // 좋아요 등록
    @Operation(summary = "핀 좋아요 등록 - pinId", description = "핀의 id로 조회하여 좋아요 등록")
    @PostMapping("/{pinId}/likes")
    public RsData<PinLikesResponse> addPinLikes(
            @PathVariable("pinId") Long pinId,
            @Valid @RequestBody PinLikesRequest reqbody
    ) {
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                likesService.toggleLikeOn(pinId, reqbody.userId())
        );

    }

    // 좋아요 취소
    @Operation(summary = "핀 좋아요 취소 - pinId", description = "핀의 id로 조회하여 좋아요 취소")
    @DeleteMapping("/{pinId}/likes")
    public RsData<PinLikesResponse> revokePinLikes(
            @PathVariable("pinId") Long pinId,
            @Valid @RequestBody PinLikesRequest reqbody
    ) {
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                likesService.toggleLikeOff(pinId, reqbody.userId())
        );
    }

    // 해당 핀을 좋아요 누른 유저 ID 목록 전달
    @Operation(summary = "핀 좋아요 사용자 조회 - pinId", description = "핀을 id로 조회하여 좋아요 등록한 사용자 조회")
    @GetMapping("{pinId}/likesusers")
    public RsData<List<PinLikedUserResponse>> getUsersWhoLikedPin(
            @PathVariable("pinId") Long pinId
    ) {
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다",
                likesService.getUsersWhoLikedPin(pinId)
        );
    }


    // 해당 핀 북마크 추가
    @Operation(summary = "핀 북마크 등록 - pinId", description = "핀을 id로 조회하여 북마크에 등록")
    @PostMapping("{pinId}/bookmarks")
    public RsData<BookmarkDto> addBookmark(
            @RequestBody addBookmarkRequest requestDto
    ) {
        User actor = rq.getActor();

        BookmarkDto bookmarkDto = bookmarkService.addBookmark(actor.getId(), requestDto.pinId());
        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다.",
                bookmarkDto
        );
    }

}