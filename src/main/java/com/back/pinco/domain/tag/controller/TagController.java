package com.back.pinco.domain.tag.controller;

import com.back.pinco.domain.tag.dto.PinTagDto;
import com.back.pinco.domain.tag.dto.TagDto;
import com.back.pinco.domain.tag.dto.request.AddTagToPinRequest;
import com.back.pinco.domain.tag.dto.request.CreateTagRequest;
import com.back.pinco.domain.tag.dto.response.*;
import com.back.pinco.domain.tag.entity.PinTag;
import com.back.pinco.domain.tag.service.PinTagService;
import com.back.pinco.domain.tag.service.TagService;
import com.back.pinco.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TagController {

    private final TagService tagService;
    private final PinTagService pinTagService;

    // 태그 전체 조회
    @GetMapping("/tags")
    public RsData<GetAllTagsResponse> getAllTags() {
        List<TagDto> tags = tagService.getAllTags().stream()
                .map(TagDto::new)
                .toList();
        return new RsData<>("200", "태그 목록 조회 성공", new GetAllTagsResponse(tags));
    }

    // 특정 핀에 태그 추가
    @PostMapping("/pins/{pinId}/tags")
    public RsData<AddTagToPinResponse> addTagToPin(@PathVariable Long pinId,
                                                   @RequestBody AddTagToPinRequest request) {
        PinTag pinTag = pinTagService.addTagToPin(pinId, request.keyword());
        return new RsData<>("200", "태그가 핀에 추가되었습니다.", new AddTagToPinResponse(pinId, new PinTagDto(pinTag)));
    }

    // 핀에 연결된 태그 조회
    @GetMapping("/pins/{pinId}/tags")
    public RsData<GetTagsByPinResponse> getTagsByPin(@PathVariable Long pinId) {
        List<TagDto> tags = pinTagService.getTagsByPin(pinId).stream()
                .map(TagDto::new)
                .toList();
        return new RsData<>("200", "핀의 태그 목록 조회 성공", new GetTagsByPinResponse(pinId, tags));
    }

    // 태그 삭제 (Soft Delete)
    @DeleteMapping("/pins/{pinId}/tags/{tagId}")
    public RsData<RemoveTagFromPinResponse> removeTagFromPinResponse(
            @PathVariable Long pinId,
            @PathVariable Long tagId
    ) {
        pinTagService.removeTagFromPin(pinId, tagId);
        return new RsData<>(
                "200",
                "태그가 삭제되었습니다.",
                new RemoveTagFromPinResponse(pinId, tagId)
        );
    }

    // 태그 복구 (관리자용)
    @PatchMapping("/pins/{pinId}/tags/{tagId}/restore")
    public RsData<RestoreTagFromPinResponse> restoreTagFromPinResponse(
            @PathVariable Long pinId,
            @PathVariable Long tagId
    ) {
        pinTagService.restoreTagFromPin(pinId, tagId);
        return new RsData<>(
                "200",
                "태그가 복구되었습니다.",
                new RestoreTagFromPinResponse(pinId, tagId)
        );
    }

    // 여러 태그 기반 필터링 조회
    @GetMapping("/tags/filter")
    public RsData<GetPinsByMultipleTagsResponse> getPinsByMultipleTags(@RequestParam List<String> keywords) {
        List<GetFilteredPinResponse> pins = pinTagService.getPinsByMultipleTagKeywords(keywords)
                .stream()
                .map(GetFilteredPinResponse::new)
                .toList();

        return new RsData<>("200", "태그 필터링 기반 게시물 목록 조회 성공", new GetPinsByMultipleTagsResponse(keywords, pins));
    }

    // 새로운 태그 생성 (관리자용)
    @PostMapping("/tags")
    public RsData<CreateTagResponse> createTag(@RequestBody CreateTagRequest request) {
        TagDto newTag = new TagDto(tagService.createTag(request.keyword()));
        return new RsData<>("200", "새로운 태그가 생성되었습니다.", new CreateTagResponse(newTag));
    }
}
