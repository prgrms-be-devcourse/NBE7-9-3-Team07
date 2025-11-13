"use client";

import { useEffect, useState } from "react";
import { apiCreatePin, apiAddTagToPin, apiGetAllTags } from "@/lib/pincoApi";
import { TagDto } from "@/types/types";

export default function CreatePostModal({
  lat,
  lng,
  userId,
  onClose,
  onCreated,
  onTagsUpdated,
}: {
  lat: number;
  lng: number;
  userId?: number | null;
  onClose: () => void;
  onCreated?: () => void;
    onTagsUpdated?: () => Promise<void>; // ✅ 비동기 함수 타입으로 수정
}) {
  const [content, setContent] = useState("");
  const [allTags, setAllTags] = useState<TagDto[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [newTagInput, setNewTagInput] = useState("");
  const [loading, setLoading] = useState(false);

  // ✅ 태그 불러오기
  useEffect(() => {
    const fetchTags = async () => {
      try {
        const tags = await apiGetAllTags();
        setAllTags(Array.isArray(tags) ? tags : []);
      } catch (err) {
        console.error("태그 목록 불러오기 실패:", err);
        setAllTags([]);
      }
    };
    fetchTags();
  }, []);

  // ✅ 태그 토글
    const toggleTag = (keyword: string) => {
        const exists = selectedTags.includes(keyword);
        const next = exists
            ? selectedTags.filter((k) => k !== keyword)
            : [...selectedTags, keyword];
        setSelectedTags(next);
    };

    // ✅ 새 태그 추가
    const addNewTag = () => {
        const trimmed = newTagInput.trim();

        if (!trimmed) {
            alert("태그를 입력해주세요.");
            return;
        }

        // 이미 존재하는 태그인지 확인
        const exists = allTags.some((t) => t.keyword === trimmed);
        if (exists) {
            // 이미 존재하면 선택 상태로 토글
            if (!selectedTags.includes(trimmed)) {
                setSelectedTags((prev) => [...prev, trimmed]);
            }
            alert("이미 존재하는 태그입니다. 선택 목록에 추가했습니다.");
            setNewTagInput("");
            return;
        }

        // 새 태그를 선택 목록에 추가
        setSelectedTags((prev) => [...prev, trimmed]);
        setNewTagInput("");
        alert(`새 태그 "${trimmed}"가 추가되었습니다.`);
    };

    // ✅ Enter 키 입력 처리
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            e.preventDefault();
            addNewTag();
        }
    };

    // ✅ 선택된 태그 제거
    const removeSelectedTag = (keyword: string) => {
        setSelectedTags((prev) => prev.filter((t) => t !== keyword));
    };

  // ✅ 핀 등록
  const handleSubmit = async () => {
    if (!content.trim()) {
      alert("내용을 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const pin = await apiCreatePin(lat, lng, content); // ✅ pin.id 바로 있음

        // ✅ 새 태그가 포함되어 있는지 확인
        const hasNewTags = selectedTags.some(
            (keyword) => !allTags.some((t) => t.keyword === keyword)
        );

      if (selectedTags.length > 0) {
        await Promise.all(
          selectedTags.map((kw) => apiAddTagToPin(pin.id, kw))
        );
      }

      alert("핀이 등록되었습니다 🎉");

        if (hasNewTags && onTagsUpdated) {
            await onTagsUpdated();
        }

      onCreated?.();
      onClose();
    } catch (err) {
      console.error("❌ 핀 등록 실패:", err);
      alert("핀 등록 중 오류가 발생했습니다 ❌");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="absolute inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-[480px] max-w-[90%] relative animate-fadeIn p-6">
        <button
          className="absolute top-3 right-3 text-gray-500 hover:text-black"
          onClick={onClose}
        >
          ✕
        </button>

        <h2 className="text-lg font-semibold mb-3">📍 새 핀 작성</h2>

        {/* 핀 입력 */}
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="핀 내용을 입력하세요..."
          className="w-full border rounded-md p-2 h-32 text-sm resize-none mb-4"
        />

          {/* 태그 선택 섹션 */}
          <div className="mb-4">
              <label className="block text-sm font-medium mb-2">🏷️ 태그 선택</label>

              {allTags.length === 0 ? (
                  <p className="text-sm text-gray-400 mb-3">등록된 태그가 없습니다.</p>
              ) : (
                  <div className="flex flex-wrap gap-2 mb-3">
                      {allTags.map((tag) => {
                          const selected = selectedTags.includes(tag.keyword);
                          return (
                              <button
                                  key={tag.id}
                                  type="button"
                                  onClick={() => toggleTag(tag.keyword)}
                                  className={`px-3 py-1 rounded-full text-sm border transition ${
                                      selected
                                          ? "bg-blue-600 text-white border-blue-600"
                                          : "bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200"
                                  }`}
                              >
                                  #{tag.keyword}
                              </button>
                          );
                      })}
                  </div>
              )}

              {/* 새 태그 입력 */}
              <div className="flex gap-2">
                  <input
                      type="text"
                      value={newTagInput}
                      onChange={(e) => setNewTagInput(e.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder="새 태그 입력"
                      className="flex-1 border rounded-md px-3 py-1 text-sm"
                  />
                  <button
                      type="button"
                      onClick={addNewTag}
                      className="px-4 py-1 rounded-md bg-gray-600 text-white text-sm hover:bg-gray-700 transition"
                  >
                      추가
                  </button>
              </div>
          </div>

          {/* 선택된 태그 목록 */}
          {selectedTags.length > 0 && (
              <div className="mb-4">
                  <label className="block text-sm font-medium mb-2">✅ 선택된 태그</label>
                  <div className="flex flex-wrap gap-2">
                      {selectedTags.map((keyword) => (
                          <span
                              key={keyword}
                              className="px-3 py-1 text-sm border rounded-full bg-blue-50 border-blue-300 text-blue-700 flex items-center gap-2"
                          >
                  #{keyword}
                              <button
                                  type="button"
                                  onClick={() => removeSelectedTag(keyword)}
                                  className="text-red-500 hover:text-red-700 text-sm font-bold"
                              >
                    ×
                  </button>
                </span>
                      ))}
                  </div>
              </div>
          )}

        <button
          onClick={handleSubmit}
          disabled={loading}
          className={`w-full bg-blue-600 text-white py-2 rounded-md mt-6 hover:bg-blue-700 transition ${loading ? "opacity-70 cursor-not-allowed" : ""
            }`}
        >
          {loading ? "등록 중..." : "등록하기"}
        </button>
      </div>
    </div>
  );
}
