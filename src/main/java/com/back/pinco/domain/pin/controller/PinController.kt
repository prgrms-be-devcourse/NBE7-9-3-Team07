package com.back.pinco.domain.pin.controller

import com.back.pinco.domain.bookmark.dto.BookmarkDto
import com.back.pinco.domain.bookmark.dto.addBookmarkRequest
import com.back.pinco.domain.bookmark.service.BookmarkService
import com.back.pinco.domain.likes.dto.PinLikedUserResponse
import com.back.pinco.domain.likes.dto.PinLikesRequest
import com.back.pinco.domain.likes.dto.PinLikesResponse
import com.back.pinco.domain.likes.service.LikesService
import com.back.pinco.domain.pin.dto.PinDto
import com.back.pinco.domain.pin.dto.PinRequest
import com.back.pinco.domain.pin.service.PinService
import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.service.UserService
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.rq.Rq
import com.back.pinco.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.*

@Tag(name = "Pin", description = "pin(장소) 관리 기능")
@RestController
@RequestMapping("/api/pins")
class PinController(
    private val pinService: PinService,

    private val userService: UserService,

    private val bookmarkService: BookmarkService,

    private val likesService: LikesService,

    private val rq: Rq
) {


    //생성
    @Operation(summary = "핀 생성", description = "사용자의 위치와 설명을 받아 핀을 생성")
    @PostMapping
    fun createPin(
        @RequestBody @Valid
        pinReqbody: PinRequest
    ): RsData<PinDto> {
        val pin = pinService.write(rq.actor, pinReqbody)
        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            PinDto(pin)
        )
    }

    //조회
    //id로 조회
    @Operation(summary = "핀 조회 - 단건 (pinId)", description = "핀의 ID로 핀을 단건 조회")
    @GetMapping("/{pinId}")
    fun getPinById(
        @PathVariable("pinId")
        pinId: Long
    ): RsData<PinDto> {
        val pin = pinService.findById(pinId, rq.actor)

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            PinDto(pin)
        )
    }

    //범위로 조회 - 원
    @Operation(summary = "핀 조회 - 다건 (범위)", description = "범위로 핀을 다건 조회")
    @GetMapping
    fun getRadiusPins(
        @RequestParam
        @NotNull
        @Min(-90)
        @Max(90)
        latitude: Double,

        @RequestParam
        @NotNull
        @Min(-180)
        @Max(180)
        longitude: Double,

        @RequestParam(defaultValue = "1000.0")
        radius: Double
    ): RsData<List<PinDto>> {
        val pins = pinService.findNearPins(latitude, longitude, radius, rq.actor)
            .map { PinDto(it) }

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pins
        )
    }


    //범위로 조회 - 사각형
    @Operation(summary = "핀 조회 - 다건 (범위-사각형)", description = "범위로 핀을 다건 조회")
    @GetMapping("/screen")
    fun getRectanglePins(
        @RequestParam
        @NotNull
        @Min(-90)
        @Max(90)
        latMax: Double,

        @RequestParam
        @NotNull
        @Min(-180)
        @Max(180)
        lonMax: Double,

        @RequestParam
        @NotNull
        @Min(-90)
        @Max(90)
        latMin: Double,

        @RequestParam
        @NotNull
        @Min(-180)
        @Max(180)
        lonMin: Double

    ): RsData<List<PinDto>> {
        val pins = pinService.findScreenPins(latMax, lonMax, latMin, lonMin, rq.actor)
            .map { PinDto(it) }

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pins
        )
    }

    //사용자로 조회
    @Operation(summary = "핀 조회 - 다건 (작성자+연도+월)", description = "작성자로 핀을 다건 조회")
    @GetMapping("/user/{userId}/date")
    fun getUserPinsByDate(
        @PathVariable
        userId: Long,

        @RequestParam
        year: Double,

        @RequestParam
        @Min(1)
        @Max(12)
        month: Double

    ): RsData<List<PinDto>> {
        val writer : User = userService.findById(userId)
        val pins = pinService.findByUserIdDate(rq.actor, writer, year, month)
            .map { PinDto(it) }

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pins
        )
    }

    //사용자로 조회
    @Operation(summary = "핀 조회 - 다건 (작성자)", description = "작성자로 핀을 다건 조회")
    @GetMapping("/user/{userId}")
    fun getUserPins(
        @PathVariable
        userId: Long
    ): RsData<List<PinDto>> {
        val writer = userService.findById(userId)
        val pins = pinService.findByUserId(rq.actor, writer)
            .map { PinDto(it) }

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pins
        )
    }

    @GetMapping("/all")
    @Operation(summary = "핀 조회 - 다건 (all)", description = "모든 핀을 다건 조회")
    fun getAll(): RsData<List<PinDto>> {

        val pins = pinService.findAll(rq.actor)
            .map { PinDto(it) }

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pins
        )
    }

    //수정
    //핀 내용 수정
    @Operation(summary = "핀 수정 - 내용 (pinId)", description = "핀의 내용(Content)을 수정")
    @PutMapping(("/{pinId}"))
    fun updatePinContent(
        @PathVariable("pinId")
        pinId: Long,

        @RequestBody
        @Valid
        putPinReqbody: PinRequest

    ): RsData<PinDto> {
        val pin = pinService.update(rq.actor, pinId, putPinReqbody)
        val pinDto = PinDto(pin)

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pinDto
        )
    }

    //공개 여부 수정
    @Operation(summary = "핀 수정 - 공개 여부 (pinId)", description = "핀의 공개 여부를 수정")
    @PutMapping(("/{pinId}/public"))
    fun changePinPublic(
        @PathVariable("pinId")
        pinId: Long
    ): RsData<PinDto> {
        val pin = pinService.changePublic(rq.actor, pinId)
        val pinDto = PinDto(pin)

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            pinDto
        )
    }

    //삭제
    @Operation(summary = "핀 삭제 - pinId", description = "핀을 id로 조회하여 삭제")
    @DeleteMapping("/{pinId}")
    fun deletePin(
        @PathVariable
        pinId: Long
    ): RsData<Void> {
        pinService.deleteById(pinId, rq.actor)

        return RsData(
            "200",
            "성공적으로 처리되었습니다",
            null
        )
    }


    // 좋아요 등록
    @Operation(summary = "핀 좋아요 등록 - pinId", description = "핀의 id로 조회하여 좋아요 등록")
    @PostMapping("/{pinId}/likes")
    fun addPinLikes(
        @PathVariable("pinId")
        pinId: Long,

        @RequestBody
        @Valid
        reqbody: PinLikesRequest

    ): RsData<PinLikesResponse> =
        RsData(
            "200",
            "성공적으로 처리되었습니다",
            likesService.toggleLikeOn(pinId, reqbody.userId)
        )


    // 좋아요 취소
    @Operation(summary = "핀 좋아요 취소 - pinId", description = "핀의 id로 조회하여 좋아요 취소")
    @DeleteMapping("/{pinId}/likes")
    fun revokePinLikes(
        @PathVariable("pinId")
        pinId: Long,

        @RequestBody
        @Valid
        reqbody: PinLikesRequest

    ): RsData<PinLikesResponse> =
        RsData(
            "200",
            "성공적으로 처리되었습니다",
            likesService.toggleLikeOff(pinId, reqbody.userId)
        )


    // 해당 핀을 좋아요 누른 유저 ID 목록 전달
    @Operation(summary = "핀 좋아요 사용자 조회 - pinId", description = "핀을 id로 조회하여 좋아요 등록한 사용자 조회")
    @GetMapping("{pinId}/likesusers")
    fun getUsersWhoLikedPin(
        @PathVariable("pinId")
        pinId: Long

    ): RsData<List<PinLikedUserResponse>> =
        RsData(
            "200",
            "성공적으로 처리되었습니다",
            likesService.getUsersWhoLikedPin(pinId)
        )


    // 해당 핀 북마크 추가
    @Operation(summary = "핀 북마크 등록 - pinId", description = "핀을 id로 조회하여 북마크에 등록")
    @PostMapping("{pinId}/bookmarks")
    fun addBookmark(
        @RequestBody requestDto: addBookmarkRequest
    ): RsData<BookmarkDto> {

        //TODO : User 엔티티 변경 후 .getId() -> .id 로 변경
        val actor : User=rq.actor ?: throw ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT)
        val bookmarkDto = bookmarkService.addBookmark(actor.getId(), requestDto.pinId)

        return RsData(
            "200",
            "성공적으로 처리되었습니다.",
            bookmarkDto
        )
    }
}