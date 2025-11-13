import { TagDto } from "../types/types";

export default function TagFilter({
  allTags,
  selectedTags,
  onChange,
  onBookmarkClick,
  currentMode,
}: {
  allTags: TagDto[];
  selectedTags: string[];
  onChange: (next: string[]) => void;
  onBookmarkClick: () => void;
  currentMode: "screen" | "nearby" | "tag" | "bookmark";
}) {
  const toggle = (keyword: string) => {
    const exists = selectedTags.includes(keyword);
    const next = exists
      ? selectedTags.filter(k => k !== keyword)
      : [...selectedTags, keyword];
    onChange(next);
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      {allTags.map(t => (
        <button
          key={t.id}
          onClick={() => toggle(t.keyword)}
          className={`px-2 py-1 text-xs rounded-full border transition ${
            selectedTags.includes(t.keyword)
              ? "bg-blue-600 text-white border-blue-600"
              : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
          }`}
        >
          #{t.keyword}
        </button>
      ))}

      <button
        onClick={onBookmarkClick}
        className={`px-2 py-1 text-xs rounded-full border transition ${
          currentMode === "bookmark"
            ? "bg-pink-600 text-white border-pink-600"
            : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
        }`}
      >
        ❤️ 내 북마크
      </button>
    </div>
  );
}
