package com.back.pinco.domain.bookmark.controller;

import com.back.pinco.domain.bookmark.dto.BookmarkDto;
import com.back.pinco.domain.bookmark.service.BookmarkService;
import com.back.pinco.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.back.pinco.global.rq.Rq;

import java.util.List;

@Tag(name = "북마크 관리", description = "북마크 관련 API")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;
    private final Rq rq;

    @Operation(summary = "나의 북마크 목록 조회", description = "사용자가 저장한 핀들의 목록을 조회")
    @GetMapping
    public RsData<List<BookmarkDto>> getMyBookmarks() {
        Long userId = rq.getActor().getId();
        List<BookmarkDto> bookmarkDtos = bookmarkService.getMyBookmarks(userId);

        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다.",
                bookmarkDtos
        );
    }

    @Operation(summary = "북마크 삭제 (Soft Delete)", description = "특정 북마크를 소프트 삭제 처리")
    @DeleteMapping("/{bookmarkId}")
    public RsData<Void> deleteBookmark(@PathVariable Long bookmarkId) {
        Long userId = rq.getActor().getId();
        bookmarkService.deleteBookmark(userId, bookmarkId);

        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다."
        );
    }

    @Operation(summary = "북마크 복원", description = "소프트 삭제된 북마크를 복원")
    @PatchMapping("/{bookmarkId}")
    public RsData<Void> restoreBookmark(@PathVariable Long bookmarkId) {
        Long userId = rq.getActor().getId();
        bookmarkService.restoreBookmark(userId, bookmarkId);

        return new RsData<>(
                "200",
                "성공적으로 처리되었습니다."
        );
    }

}
