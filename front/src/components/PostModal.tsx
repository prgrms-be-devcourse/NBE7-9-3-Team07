"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import type { BookmarkDto } from "../types/types"; // 🔧 find 콜백에 타입 명시용
import { PinDto, PinLikedUserDto, TagDto } from "../types/types";
import {
  apiAddTagToPin,
  apiDeleteBookmark,
  apiDeletePin,
  apiGetLikeUsers,
  apiGetPinTags,
  apiAddLike,
  apiRemoveLike,
  apiTogglePublic,
  apiUpdatePin,
  apiCreateBookmark,
  apiRemoveTagFromPin,
  apiGetMyBookmarks,
  apiListBookmarks, // bookmarkId 조회
} from "../lib/pincoApi";

// 서버 공통 래퍼 타입 (json.data 접근용)
type RsData<T> = { code?: string; message?: string; data?: T };

export default function PostModal({
  pin,
  onClose,
  userId,
  onChanged,
}: {
  pin: PinDto;
  onClose: () => void;
  userId?: number | null;
  onChanged?: (updatedPin?: PinDto) => void;
}) {
  const [tags, setTags] = useState<TagDto[]>([]);
  const [likeUsers, setLikeUsers] = useState<PinLikedUserDto[]>([]);
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(pin.likeCount ?? 0);
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [bookmarkId, setBookmarkId] = useState<number | null>(null);
  const [newTag, setNewTag] = useState("");
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(pin.content);
  const [currentPin, setCurrentPin] = useState(pin);

  const isOwner = userId !== null && currentPin.userId === userId;

  useEffect(() => {
    setCurrentPin(pin);
    setContent(pin.content);
  }, [pin.id, pin.content]);

  const [localPublic, setLocalPublic] = useState(pin.isPublic);
  useEffect(() => {
    setLocalPublic(pin.isPublic);
  }, [pin.isPublic]);

  // any 지양: unknown으로 받고 내부에서 좁히기
  const parseTags = (resp: unknown): TagDto[] => {
    const r = resp as any;
    if (Array.isArray(r?.data?.tags)) return r.data.tags as TagDto[];
    if (Array.isArray(r?.data)) return r.data as TagDto[];
    if (Array.isArray(r)) return r as TagDto[];
    return [];
  };

  // 초기 로드
  useEffect(() => {
    let mounted = true;

    const loadData = async () => {
      try {
        const t = await apiGetPinTags(pin.id);
        const parsedTags = parseTags(t);
        if (mounted) setTags(parsedTags);
      } catch (err) {
        console.error("태그 로드 실패:", err);
        if (mounted) setTags([]);
      }

      try {
        const u = await apiGetLikeUsers(pin.id);
        const likeUserList: PinLikedUserDto[] = Array.isArray(u) ? u : [];
        if (mounted) {
          setLikeUsers(likeUserList);
          setIsLiked(likeUserList.some((usr) => usr.id === userId));
          setLikeCount(likeUserList.length);
        }
      } catch (err) {
        console.error("좋아요 로드 실패:", err);
      }

      // 북마크 상태 + bookmarkId
        if (userId) {
            try {
                const myPins = await apiGetMyBookmarks(); // PinDto[]
                const bookmarkedNow =
                    Array.isArray(myPins) && myPins.some((p) => p.id === pin.id);

                let id: number | null = null;
                if (bookmarkedNow) {
                    const list = await apiListBookmarks(); // BookmarkDto[] | null
                    const found = (list ?? []).find(
                        (b: BookmarkDto) => b.pin?.id === pin.id
                    );
                    id = found ? found.id : null;
                }

                if (mounted) {
                    setIsBookmarked(Boolean(bookmarkedNow));
                    setBookmarkId(id);
                }
            } catch (err) {
                console.error("북마크 로드 실패:", err);
                if (mounted) {
                    setIsBookmarked(false);
                    setBookmarkId(null);
                }
            }
        } else {
            // userId가 없으면 북마크 상태를 기본값으로 설정하고 API 호출을 건너뜀
            if (mounted) {
                setIsBookmarked(false);
                setBookmarkId(null);
            }
        }
    };

    loadData();
    return () => {
      mounted = false;
    };
  }, [pin.id, userId]);

  // 태그 추가/삭제
  const addTag = async () => {
    if (!newTag.trim()) return;
    await apiAddTagToPin(pin.id, newTag.trim());
    const res = await apiGetPinTags(pin.id);
    setTags(parseTags(res));
    setNewTag("");
    onChanged?.();
  };

  const removeTag = async (tagId: number) => {
    await apiRemoveTagFromPin(pin.id, tagId);
    const res = await apiGetPinTags(pin.id);
    setTags(parseTags(res));
    onChanged?.();
  };

  // 이 컴포넌트 안 어딘가, useState 들 아래에 추가  <<< ADD
  const getBookmarkIdForPin = async (pinId: number) => {
    const list = await apiListBookmarks(); // BookmarkDto[] | null
    return (list ?? []).find((b: BookmarkDto) => b.pin?.id === pinId)?.id ?? null;
  };


  // 좋아요 토글
  const toggleLike = async () => {
    if (!userId) {
      alert("로그인 후 이용 가능합니다.");
      return;
    }
    try {
      const res = !isLiked
        ? await apiAddLike(pin.id, userId)
        : await apiRemoveLike(pin.id, userId);

      if (res) {
        setIsLiked(res.isLiked);
        setLikeCount(res.likeCount);
        onChanged?.({ ...pin, likeCount: res.likeCount }); // 최신 값 전달
      }
    } catch (err) {
      console.error("좋아요 요청 실패:", err);
    }
  };

  // 북마크 토글
    const toggleBookmark = async () => {
      if (!userId) {
        alert("로그인 후 이용 가능합니다.");
        return;
      }

      try {
        if (isBookmarked) {
          // 현재 '북마크됨' → 삭제
          const id = bookmarkId ?? (await getBookmarkIdForPin(pin.id));
          if (!id) throw new Error("북마크 ID를 찾을 수 없습니다.");
          await apiDeleteBookmark(id);
          setIsBookmarked(false);
          setBookmarkId(null);
          onChanged?.();
          return;
        }

        // 현재 '북마크 안됨' → 생성 시도
        try {
          const created = await apiCreateBookmark(pin.id);
          if (created) {
            setIsBookmarked(true);
            setBookmarkId(created.id);
            onChanged?.();
          }
        } catch (err: unknown) {
          const e = err as { status?: number; message?: string };
          const msg = e?.message ?? "";
          // 서버가 "이미 북마크됨"으로 409를 던지면 → 즉시 삭제로 폴백
          if (e?.status === 409 || /이미 북마크된/.test(msg)) {
            const id = await getBookmarkIdForPin(pin.id);
            if (id) {
              await apiDeleteBookmark(id);
              setIsBookmarked(false);
              setBookmarkId(null);
              onChanged?.();
              return;
            }
          }
          throw err; // 다른 에러는 그대로 노출
        }
      } catch (err) {
        console.error("북마크 토글 실패:", err);
      }
    };


  // 공개 토글
  const togglePublic = async () => {
    if (!userId) {
      alert("로그인 후 이용 가능합니다.");
      return;
    } else if (userId != pin.userId) {
      alert("수정 권한이 없습니다.");
      return;
    }
    const next = !localPublic;
    setLocalPublic(next);

    try {
      const res = await apiTogglePublic(pin.id);
      const updatedPin =
        (res as any)?.data && (res as any).data.isPublic !== undefined
          ? (res as any).data
          : res;
      const confirmed = (updatedPin as PinDto)?.isPublic ?? next;
      setLocalPublic(confirmed);

      alert(confirmed ? "🌍 공개로 전환되었습니다" : "🔒 비공개로 전환되었습니다");
      await onChanged?.();
    } catch (err) {
      console.error("공개 토글 실패:", err);
      setLocalPublic(!next);
      alert("공개 설정 변경 중 오류가 발생했습니다.");
    }
  };

  // 내용 수정 저장
  const saveEdit = async () => {
    if (!userId) {
      alert("로그인 후 이용 가능합니다.");
      return;
    } else if (userId != pin.userId) {
      alert("수정 권한이 없습니다.");
      return;
    }
    try {
      await apiUpdatePin(
        currentPin.id,
        currentPin.latitude,
        currentPin.longitude,
        content
      );

      // 응답 타입 명시(RsData<PinDto>) → json.data 접근 에러 해결
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/pins/${currentPin.id}`
      );
      const json = (await res.json()) as RsData<PinDto>;

      setEditing(false);

      if (json.data) {
        const updated = json.data;
        setCurrentPin(updated);
        setContent(updated.content);
        onChanged?.(updated);
      } else {
        setCurrentPin({ ...currentPin, content });
        onChanged?.({ ...currentPin, content });
      }

      alert("핀이 수정되었습니다 ✅");
    } catch (err) {
      console.error("핀 수정 실패:", err);
      alert("핀 수정 중 오류가 발생했습니다.");
    }
  };

  const deletePin = async () => {
    if (!userId) {
      alert("로그인 후 이용 가능합니다.");
      return;
    } else if (userId != pin.userId) {
      alert("수정 권한이 없습니다.");
      return;
    }
    if (!confirm("이 핀을 삭제할까요?")) return;
    await apiDeletePin(pin.id);
    onChanged?.();
    onClose();
  };

  return (
    <div className="absolute inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-[480px] max-w-[90%] relative animate-fadeIn">
        <button
          className="absolute top-3 right-3 text-gray-500 hover:text-black"
          onClick={onClose}
        >
          <X className="w-5 h-5" />
        </button>

        <div className="p-6 space-y-4">
          <h2 className="text-lg font-semibold">📍 핀</h2>

          {editing ? (
            <textarea
              className="w-full border rounded-md p-2 h-32 text-sm"
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
          ) : (
            <p className="text-gray-800 leading-relaxed">{currentPin.content}</p>
          )}

          <div className="text-xs text-gray-500 flex justify-between">
            <span>
              작성:{" "}
              {new Date(currentPin.createdAt).toLocaleString("ko-KR", {
                dateStyle: "medium",
                timeStyle: "short",
              })}
            </span>
            <span>
              수정:{" "}
              {new Date(currentPin.modifiedAt).toLocaleString("ko-KR", {
                dateStyle: "medium",
                timeStyle: "short",
              })}
            </span>
          </div>

          {/* 태그 */}
          <div className="mt-3">
            <div className="text-sm font-medium mb-2">🏷️ 태그</div>

            <div className="flex flex-wrap gap-2">
              {(!Array.isArray(tags) || tags.length === 0) && (
                <span className="text-xs text-gray-400">등록된 태그 없음</span>
              )}

              {Array.isArray(tags) &&
                tags.map((t) => (
                  <span
                    key={t.id}
                    className="px-2 py-1 text-xs border rounded-full bg-gray-50 flex items-center gap-1"
                  >
                    #{t.keyword}
                    {editing && (
                      <button
                        onClick={() => removeTag(t.id)}
                        className="text-red-500 hover:text-red-700 text-xs"
                      >
                        ×
                      </button>
                    )}
                  </span>
                ))}
            </div>

            {editing && (
              <div className="mt-2 flex gap-2">
                <input
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  placeholder="새 태그 입력"
                  className="flex-1 border rounded-md px-2 py-1 text-sm"
                />
                <button
                  onClick={addTag}
                  className="px-3 py-1 rounded-md bg-blue-600 text-white text-sm"
                >
                  추가
                </button>
              </div>
            )}
          </div>

          {/* 컨트롤 버튼들 */}
          <div className="flex flex-col gap-2">
            {editing ? (
              <>
                <button
                  onClick={saveEdit}
                  className="px-3 py-1 rounded-md bg-blue-600 text-white"
                >
                  저장
                </button>
                <button
                  onClick={() => setEditing(false)}
                  className="px-3 py-1 rounded-md border text-gray-600"
                >
                  취소
                </button>
              </>
            ) : (
              <>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={toggleLike}
                    className={`px-3 py-1 rounded-md border transition ${
                      isLiked
                        ? "bg-red-100 text-red-600 border-red-300"
                        : "border-gray-300"
                    }`}
                  >
                    {isLiked ? "💔 좋아요 취소" : "👍 좋아요"} ({likeCount})
                  </button>

                  <button
                    onClick={toggleBookmark}
                    className={`px-3 py-1 rounded-md border transition ${
                      isBookmarked
                        ? "bg-blue-100 text-blue-600 border-blue-300"
                        : "border-gray-300"
                    }`}
                  >
                    {isBookmarked ? "🔖 북마크됨" : "📌 북마크"}
                  </button>
                </div>

                {isOwner && (
                  <div className="flex flex-wrap gap-2">
                    <button
                      onClick={togglePublic}
                      className={`px-3 py-1 rounded-md border transition ${
                        localPublic
                          ? "bg-green-100 text-green-700 border-green-400 hover:bg-green-200"
                          : "bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200"
                      }`}
                    >
                      {localPublic ? "🔓 공개 중" : "🔒 비공개"}
                    </button>

                    <button
                      onClick={() => setEditing(true)}
                      className="px-3 py-1 rounded-md border"
                    >
                      ✏️ 편집
                    </button>

                    <button
                      onClick={deletePin}
                      className="px-3 py-1 rounded-md border text-red-600"
                    >
                      🗑 삭제
                    </button>
                  </div>
                )}
              </>
            )}
          </div>

          <div className="text-sm">
            <span className="font-medium">좋아요한 유저:</span>{" "}
            {likeUsers.length
              ? likeUsers.map((u) => u.userName).join(", ")
              : "없음"}
          </div>
        </div>
      </div>
    </div>
  );
}
