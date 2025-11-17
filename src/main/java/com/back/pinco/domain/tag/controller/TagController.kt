package com.back.pinco.domain.tag.controller

import com.back.pinco.domain.tag.dto.PinTagDto
import com.back.pinco.domain.tag.dto.TagDto
import com.back.pinco.domain.tag.dto.request.AddTagToPinRequest
import com.back.pinco.domain.tag.dto.request.CreateTagRequest
import com.back.pinco.domain.tag.dto.response.*
import com.back.pinco.domain.tag.service.PinTagService
import com.back.pinco.domain.tag.service.TagService
import com.back.pinco.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class TagController(
    private val tagService: TagService,
    private val pinTagService: PinTagService
) {

    //태그 전체 조회
    @GetMapping("/tags")
    fun allTags() : RsData<GetAllTagsResponse>{
            val tags = tagService.getAllTags()
                .map{ TagDto(it) }

            return RsData(
                "200",
                "태그 목록 조회 성공",
                GetAllTagsResponse(tags)
            )
        }

    // 특정 핀에 태그 추가
    @PostMapping("/pins/{pinId}/tags")
    fun addTagToPin(
        @PathVariable pinId: Long,
        @RequestBody @Valid request: AddTagToPinRequest
    ): RsData<AddTagToPinResponse> {
        val pinTag = pinTagService.addTagToPin(pinId, request.keyword)

        return RsData(
            "200",
            "태그가 핀에 추가되었습니다.",
            AddTagToPinResponse(pinId, PinTagDto(pinTag))
        )
    }

    // 핀에 연결된 태그 조회
    @GetMapping("/pins/{pinId}/tags")
    fun getTagsByPin(
        @PathVariable pinId: Long
    ): RsData<GetTagsByPinResponse> {
        val tags = pinTagService.getTagsByPin(pinId)
            .map { TagDto(it) }
        return RsData(
            "200",
            "핀의 태그 목록 조회 성공",
            GetTagsByPinResponse(pinId, tags)
        )
    }

    // 태그 삭제
    @DeleteMapping("/pins/{pinId}/tags/{tagId}")
    fun removeTagFromPinResponse(
        @PathVariable pinId: Long,
        @PathVariable tagId: Long
    ): RsData<RemoveTagFromPinResponse> {
        pinTagService.removeTagFromPin(pinId, tagId)

        return RsData(
            "200",
            "태그가 삭제되었습니다.",
            RemoveTagFromPinResponse(pinId, tagId)
        )
    }

    // 여러 태그 기반 필터링 조회
    @GetMapping("/tags/filter")
    fun getPinsByMultipleTags(
        @RequestParam keywords: List<String>
    ): RsData<GetPinsByMultipleTagsResponse> {
        val pins = pinTagService.getPinsByMultipleTagKeywords(keywords)
            .map { GetFilteredPinResponse(it) }

        return RsData(
            "200",
            "태그 필터링 기반 게시물 목록 조회 성공",
            GetPinsByMultipleTagsResponse(keywords, pins)
        )
    }

    // 새로운 태그 생성 (관리자용)
    @PostMapping("/tags")
    fun createTag(
        @RequestBody
        @Valid
        request: CreateTagRequest
    ): RsData<CreateTagResponse> {
        val newTag = TagDto(tagService.createTag(request.keyword))
        return RsData(
            "200",
            "새로운 태그가 생성되었습니다.",
            CreateTagResponse(newTag)
        )
    }
}
