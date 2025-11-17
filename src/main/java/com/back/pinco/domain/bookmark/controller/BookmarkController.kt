package com.back.pinco.domain.bookmark.controller

import com.back.pinco.domain.bookmark.dto.BookmarkDto
import com.back.pinco.domain.bookmark.service.BookmarkService
import com.back.pinco.global.rq.Rq
import com.back.pinco.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "북마크 관리", description = "북마크 관련 API")
@RestController
@RequestMapping("/api/bookmarks")
class BookmarkController(
    private val bookmarkService: BookmarkService,
    private val rq: Rq
) {

    @GetMapping
    @Operation(summary = "나의 북마크 목록 조회", description = "사용자가 저장한 핀들의 목록을 조회")
    fun getMyBookmarks(): RsData<List<BookmarkDto?>> {
        val userId = rq.getActorIdOrThrow()
        val bookmarkDtos = bookmarkService.getMyBookmarks(userId)

        return RsData(
            "200",
            "성공적으로 처리되었습니다.",
            bookmarkDtos
        )
    }

    @Operation(summary = "북마크 삭제 (Soft Delete)", description = "특정 북마크를 소프트 삭제 처리")
    @DeleteMapping("/{bookmarkId}")
    fun deleteBookmark(@PathVariable bookmarkId: Long): RsData<Unit> {
        val userId = rq.getActorIdOrThrow()
        bookmarkService.deleteBookmark(userId, bookmarkId)

        return RsData(
            "200",
            "성공적으로 처리되었습니다.",
            Unit
        )
    }

    @Operation(summary = "북마크 복원", description = "소프트 삭제된 북마크를 복원")
    @PatchMapping("/{bookmarkId}")
    fun restoreBookmark(@PathVariable bookmarkId: Long): RsData<Unit> {
        val userId = rq.getActorIdOrThrow()
        bookmarkService.restoreBookmark(userId, bookmarkId)

        return RsData(
            "200",
            "성공적으로 처리되었습니다.",
            Unit
        )
    }
}