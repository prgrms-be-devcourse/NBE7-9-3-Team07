"use client";

// ✅ 필요한 모듈 추가
import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { apiGetMyBookmarks, apiGetPin } from "@/lib/pincoApi"; // ✅ apiGetPin 추가
import { PinDto as ImportedPinDto } from "@/types/types";
import { fetchApi } from "@/lib/client";
import { useAuth } from "@/context/AuthContext"; // ✅ useAuth 추가
import PostModal from "../../../components/PostModal"; // ✅ PostModal 추가

type Pin = {
  id: number;
  title: string;
  createdAt: string;
  likes: number;
  isPublic: boolean;
};

export default function MyPage() {
  const router = useRouter();
  const { user } = useAuth(); // ✅ 현재 로그인 유저 정보

  // === 왼쪽 프로필 & 오른쪽 통계 ===
  const [email, setEmail] = useState("");
  const [userName, setUserName] = useState("");
  const [pinCount, setPinCount] = useState(0);
  const [bookmarkCount, setBookmarkCount] = useState(0);
  const [likesCount, setLikesCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // === 가운데 핀 목록 ===
  const [pins, setPins] = useState<Pin[]>([]);
  const [pinsLoading, setPinsLoading] = useState(true);
  const [pinsError, setPinsError] = useState<string | null>(null);
  const [view, setView] = useState<"grid" | "list">("grid");
  const [visibility, setVisibility] = useState<"all" | "public" | "private">(
    "all"
  );

  // === 북마크 목록 ===
  const [bookmarks, setBookmarks] = useState<ImportedPinDto[]>([]);
  const [bookmarksLoading, setBookmarksLoading] = useState(true);
  const [bookmarksError, setBookmarksError] = useState<string | null>(null);
  const [bmView, setBmView] = useState<"grid" | "list">("grid");

  // ✅ 모달용 상태 추가
  const [selectedPin, setSelectedPin] = useState<ImportedPinDto | null>(null);

  // 통계/프로필
  useEffect(() => {
    const fetchMyPage = async () => {
      try {
        const data = await fetchApi<any>("/api/user/mypage", { method: "GET" });
        if (!data) throw new Error("서버 응답에 data 없음");

        setEmail(typeof data.email === "string" ? data.email : "");
        setUserName(typeof data.userName === "string" ? data.userName : "");
        setPinCount(
          typeof data.pinCount === "number"
            ? data.pinCount
            : typeof data.myPinCount === "number"
            ? data.myPinCount
            : 0
        );
        setBookmarkCount(
          typeof data.bookmarkCount === "number" ? data.bookmarkCount : 0
        );
        setLikesCount(
          typeof data.likesCount === "number"
            ? data.likesCount
            : typeof data.totalLikesCount === "number"
            ? data.totalLikesCount
            : 0
        );
        setError(null);
      } catch (e) {
        setError(e instanceof Error ? e.message : "네트워크 오류");
      } finally {
        setLoading(false);
      }
    };
    fetchMyPage();
  }, []);

  // ✅ 내 핀 목록 (useCallback으로 추출)
  const fetchMyPins = useCallback(async () => {
    setPinsLoading(true);
    try {
      const data = await fetchApi<any>("/api/user/mypin", { method: "GET" });
      const d =
        data && typeof data === "object"
          ? (data as Record<string, unknown>)
          : {};
      const publicRaw = Array.isArray(d["publicPins"]) ? d["publicPins"] : [];
      const privateRaw = Array.isArray(d["privatePins"]) ? d["privatePins"] : [];

      const toPins = (arr: unknown[], defaultPublic: boolean): Pin[] => {
        const out: Pin[] = [];
        for (const p of arr) {
          if (typeof p !== "object" || p === null) continue;
          const o = p as Record<string, unknown>;

          let id: number | undefined;
          if (typeof o.id === "number") id = o.id;
          else if (typeof o.pinId === "number") id = o.pinId;
          else if (
            typeof o.pinId === "string" &&
            !Number.isNaN(Number(o.pinId))
          )
            id = Number(o.pinId);
          if (id === undefined) continue;

          const createdAt =
            (typeof o.createdAt === "string" && o.createdAt) ||
            (typeof o.modifiedAt === "string" && o.modifiedAt) ||
            (typeof o.createdDate === "string" && o.createdDate) ||
            new Date().toISOString();

          const title =
            (typeof o.title === "string" && o.title) ||
            (typeof o.content === "string" && o.content) ||
            "(제목 없음)";

          const likes =
            (typeof o.likes === "number" && o.likes) ||
            (typeof o.likeCount === "number" && o.likeCount) ||
            0;

          const isPublic =
            (typeof o.isPublic === "boolean" && o.isPublic) ||
            (typeof (o as any).public === "boolean" &&
              ((o as any).public as boolean)) ||
            defaultPublic;

          out.push({ id, title, createdAt, likes, isPublic });
        }
        return out;
      };

      const merged = [
        ...toPins(publicRaw, true),
        ...toPins(privateRaw, false),
      ];
      setPins(merged);
      setPinsError(null);
    } catch (e) {
      setPinsError(e instanceof Error ? e.message : "목록 조회 실패");
      setPins([]);
    } finally {
      setPinsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMyPins();
  }, [fetchMyPins]);

  // ✅ 북마크 (useCallback으로 추출)
  const fetchBookmarks = useCallback(async () => {
    setBookmarksLoading(true);
    setBookmarksError(null);
    try {
      const pins = await apiGetMyBookmarks(); // PinDto[] | null
      setBookmarks(pins || []);
    } catch (err: any) {
      console.error("북마크 로드 실패:", err);
      setBookmarksError(err?.message || "북마크 목록을 불러오지 못했습니다.");
      if (err?.status === 401) router.push("/login");
    } finally {
      setBookmarksLoading(false);
    }
  }, [router]);

  useEffect(() => {
    fetchBookmarks();
  }, [fetchBookmarks]);

  // ✅ '내 핀' 클릭 핸들러 (apiGetPin으로 상세 정보 조회)
  const handleMyPinClick = async (pinId: number) => {
    try {
      const fullPin = await apiGetPin(pinId);
      if (fullPin) {
        setSelectedPin(fullPin);
      } else {
        alert("핀 정보를 불러오는데 실패했습니다.");
      }
    } catch (e) {
      console.error("Failed to fetch pin details", e);
      alert("핀 정보를 불러오는데 실패했습니다.");
    }
  };

  // ✅ '북마크' 클릭 핸들러 (이미 PinDto이므로 바로 사용)
  const handleBookmarkClick = (pin: ImportedPinDto) => {
    setSelectedPin(pin);
  };

  const stats = [
    { icon: "📍", label: "등록한 핀", value: pinCount },
    { icon: "❤️", label: "받은 좋아요", value: likesCount },
    { icon: "🔖", label: "북마크", value: bookmarks.length },
  ];

  const filteredPins =
    visibility === "all"
      ? pins
      : visibility === "public"
      ? pins.filter((p) => p.isPublic)
      : pins.filter((p) => !p.isPublic);

  // 북마크 렌더링
  const renderBookmarks = () => {
    if (bookmarksLoading)
      return (
        <div className="text-sm text-gray-500 py-8 text-center">
          불러오는 중…
        </div>
      );
    if (bookmarksError)
      return (
        <div className="text-sm text-red-500 py-8 text-center">
          {bookmarksError}
        </div>
      );
    if (bookmarks.length === 0)
      return (
        <div className="text-sm text-gray-400 py-8 text-center">
          북마크한 게시물이 없습니다.
        </div>
      );

    if (bmView === "grid") {
      return (
        <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {bookmarks.map((pin) => (
            <li
              key={pin.id}
              className="group border border-gray-100 rounded-xl overflow-hidden hover:shadow-md transition cursor-pointer bg-white"
              onClick={() => handleBookmarkClick(pin)} // ✅ 클릭 핸들러 변경
            >
              <div className="aspect-video bg-gray-100 flex items-center justify-center text-3xl">
                📍
              </div>
              <div className="p-3">
                <h4 className="font-medium text-gray-900 line-clamp-1 group-hover:underline">
                  {pin.content || "(내용 없음)"}
                </h4>
                <div className="mt-1 flex items-center justify-between text-xs text-gray-500">
                  <span>
                    {new Date(pin.createdAt).toLocaleDateString("ko-KR")}
                  </span>
                  <span>❤️ {pin.likeCount}</span>
                </div>
              </div>
            </li>
          ))}
        </ul>
      );
    }

    return (
      <ul className="divide-y">
        {bookmarks.map((pin) => (
          <li
            key={pin.id}
            className="py-3 flex items-center justify-between gap-3 hover:bg-gray-50 px-2 rounded-lg transition cursor-pointer"
            onClick={() => handleBookmarkClick(pin)} // ✅ 클릭 핸들러 변경
          >
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center flex-none text-xl">
                📍
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 border text-gray-700">
                    {pin.isPublic ? "공개" : "비공개"}
                  </span>
                  <span className="text-xs text-gray-500">
                    {new Date(pin.createdAt).toLocaleDateString("ko-KR")}
                  </span>
                </div>
                <h4 className="text-sm font-medium text-gray-900 truncate">
                  {pin.content || "(내용 없음)"}
                </h4>
              </div>
            </div>
            <div className="text-xs text-gray-600 flex items-center gap-1 flex-none">
              ❤️ {pin.likeCount}
            </div>
          </li>
        ))}
      </ul>
    );
  };

  return (
    <main className="bg-gray-50 min-h-[100vh]">
      <div className="mx-auto max-w-6xl px-6 py-8 grid grid-cols-1 gap-6 md:grid-cols-[250px_minmax(0,1fr)_220px] items-start">
        {/* 왼쪽 프로필 */}
        <aside className="space-y-5 md:col-start-1">
          <div className="bg-white border rounded-2xl p-4 shadow-sm flex items-center gap-3">
            <div className="w-14 h-14 rounded-full bg-gray-200 flex items-center justify-center text-3xl">
              🧑‍🦱
            </div>
            <div>
              <div className="text-base font-semibold">
                {loading ? "로딩 중..." : userName || "-"}
              </div>
              <div className="text-gray-500 text-sm">
                {loading ? "" : email}
              </div>
            </div>
          </div>
          <button
            className="w-full bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium py-2 rounded-lg transition"
            onClick={() => router.push("/user/mypage/edit")}
          >
            회원 정보 수정
          </button>
          <button
            className="w-full bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium py-2 rounded-lg transition"
            onClick={() => router.push("/user/mypage/delete")}
          >
            회원 탈퇴
          </button>
        </aside>

        {/* 가운데 핀 목록 */}
        <section className="grid grid-cols-1 gap-6 md:col-start-2">
          <div className="bg-orange-50 border border-orange-100 rounded-2xl p-5 shadow-sm">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-baseline gap-2">
                <h3 className="text-orange-700 font-semibold">
                  📍 내가 작성한 핀
                </h3>
                <span className="text-gray-500 text-sm">
                  {pinsLoading ? "…" : `${filteredPins.length}개`}
                </span>
              </div>

              <div className="flex gap-2">
                {(["all", "public", "private"] as const).map((v) => (
                  <button
                    key={v}
                    onClick={() => setVisibility(v)}
                    className={`px-3 py-1 rounded-full text-sm border transition ${
                      visibility === v
                        ? "bg-gray-900 text-white border-gray-900"
                        : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                    }`}
                  >
                    {v === "all" ? "전체" : v === "public" ? "공개" : "비공개"}
                  </button>
                ))}
                {(["grid", "list"] as const).map((v) => (
                  <button
                    key={v}
                    onClick={() => setView(v)}
                    className={`px-3 py-1 rounded-full text-sm border transition ${
                      view === v
                        ? "bg-gray-900 text-white border-gray-900"
                        : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                    }`}
                  >
                    {v === "grid" ? "그리드" : "리스트"}
                  </button>
                ))}
              </div>
            </div>

            <div className="bg-white border border-gray-100 rounded-xl p-4">
              {pinsLoading ? (
                <div className="text-sm text-gray-500 py-8 text-center">
                  불러오는 중…
                </div>
              ) : pinsError ? (
                <div className="text-sm text-red-500 py-8 text-center">
                  {pinsError}
                </div>
              ) : filteredPins.length === 0 ? (
                <div className="text-sm text-gray-400 py-8 text-center">
                  게시물이 없습니다.
                </div>
              ) : view === "grid" ? (
                <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {filteredPins.map((p) => (
                    <li
                      key={p.id}
                      className="group border border-gray-100 rounded-xl overflow-hidden hover:shadow-md transition cursor-pointer bg-white"
                      onClick={() => handleMyPinClick(p.id)} // ✅ 클릭 핸들러 변경
                    >
                      <div className="aspect-video bg-gray-100 flex items-center justify-center text-3xl">
                        📍
                      </div>
                      <div className="p-3">
                        <h4 className="font-medium text-gray-900 line-clamp-1 group-hover:underline">
                          {p.title}
                        </h4>
                        <div className="mt-1 flex items-center justify-between text-xs text-gray-500">
                          <span>
                            {new Date(p.createdAt).toLocaleDateString("ko-KR")}
                          </span>
                          <span>❤️ {p.likes}</span>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <ul className="divide-y">
                  {filteredPins.map((p) => (
                    <li
                      key={p.id}
                      className="py-3 flex items-center justify-between gap-3 hover:bg-gray-50 px-2 rounded-lg transition cursor-pointer"
                      onClick={() => handleMyPinClick(p.id)} // ✅ 클릭 핸들러 변경
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center flex-none text-xl">
                          📍
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 border text-gray-700">
                              {p.isPublic ? "공개" : "비공개"}
                            </span>
                            <span className="text-xs text-gray-500">
                              {new Date(p.createdAt).toLocaleDateString(
                                "ko-KR"
                              )}
                            </span>
                          </div>
                          <h4 className="text-sm font-medium text-gray-900 truncate">
                            {p.title}
                          </h4>
                        </div>
                      </div>
                      <div className="text-xs text-gray-600 flex items-center gap-1 flex-none">
                        ❤️ {p.likes}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {/* 내가 북마크한 핀 */}
          <div className="bg-blue-50 border border-blue-100 rounded-2xl p-5 shadow-sm">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-baseline gap-2">
                <h3 className="text-blue-700 font-semibold">
                  🔖 내가 북마크한 핀
                </h3>
                <span className="text-gray-500 text-sm">
                  {bookmarksLoading ? "…" : `${bookmarks.length}개`}
                </span>
              </div>
              <div className="flex gap-2">
                {(["grid", "list"] as const).map((v) => (
                  <button
                    key={v}
                    onClick={() => setBmView(v)}
                    className={`px-3 py-1 rounded-full text-sm border transition ${
                      bmView === v
                        ? "bg-gray-900 text-white border-gray-900"
                        : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"
                    }`}
                  >
                    {v === "grid" ? "그리드" : "리스트"}
                  </button>
                ))}
              </div>
            </div>

            <div className="bg-white border border-gray-100 rounded-xl p-4">
              {renderBookmarks()}
            </div>
          </div>
        </section>

        {/* 오른쪽 통계 */}
        <div className="flex flex-col gap-4 md:col-start-3 w-[220px] flex-none justify-self-end items-stretch">
          {loading ? (
            <div className="bg-white border rounded-2xl shadow-sm py-6 px-3 text-center text-sm text-gray-500">
              불러오는 중…
            </div>
          ) : error ? (
            <div className="bg-white border rounded-2xl shadow-sm py-6 px-3 text-center text-sm text-red-500">
              {error}
            </div>
          ) : (
            stats.map((s) => (
              <div
                key={s.label}
                className="bg-white border rounded-2xl shadow-sm py-3 px-3 text-center flex flex-col items-center justify-center w-full"
              >
                <div className="text-2xl mb-1">{s.icon}</div>
                <div className="text-lg font-semibold">
                  {s.value.toLocaleString()}
                </div>
                <div className="text-gray-500 text-sm">{s.label}</div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* ✅ PostModal 렌더링 (홈페이지와 동일) */}
      {selectedPin && (
        <PostModal
          pin={selectedPin}
          onClose={() => setSelectedPin(null)}
          userId={user?.id ?? 1} // 홈화면과 동일하게 fallback
          onChanged={async () => {
            // 모달에서 핀 정보 변경 시 (좋아요, 북마크 등)
            // 두 목록 모두 새로고침
            await fetchMyPins();
            await fetchBookmarks();
          }}
        />
      )}
    </main>
  );
}