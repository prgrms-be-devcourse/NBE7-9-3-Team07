"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL!; // 예: http://localhost:8080

// ✅ [추가] 서버 표준 응답 타입 (RsData)
type RsData<T = unknown> = {
  errorCode: string;
  msg: string;
  data?: T;
};

export default function EditMyInfoPage() {
  const router = useRouter();
  const [newUserName, setNewUserName] = useState("");
  const [newPassword, setNewPassword] = useState(""); // ✅ 새 비밀번호 하나만
  const [password, setPassword] = useState(""); // 현재 비밀번호
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 새 닉네임과 새 비밀번호가 모두 비어있을 때
    if (!newUserName.trim() && !newPassword) {
      alert("변경할 닉네임 또는 새 비밀번호를 입력해주세요.");
      return;
    }

    // 현재 비밀번호 검증
    if (!password) {
      alert("현재 비밀번호를 입력해주세요.");
      return;
    }
    if (password.length < 8) {
      alert("비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    // 새 비밀번호 검증 (있을 때만)
    if (newPassword && newPassword.length < 8) {
      alert("비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    // ✅ 서버에 보낼 payload 구성 (newUserName, newPassword는 선택적 - 값이 있을 때만 전송)
    const payload: Record<string, string> = { password };
    if (newUserName.trim()) payload.newUserName = newUserName.trim();
    if (newPassword) payload.newPassword = newPassword;

    try {
      setLoading(true);
      const res = await fetch(`${API_BASE}/api/user/edit`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      const contentType = res.headers.get("content-type") || "";

      // ❌ (기존) const rs: any = ...
      // ✅ [수정] 명시적 타입 + 안전한 파싱
      let rs: RsData | null = null;
      if (contentType.includes("application/json")) {
        rs = (await res.json()) as RsData;
      } else {
        // JSON 이 아닌 응답 방어
        console.error("서버 응답이 JSON이 아닙니다.");
      }

      // ✅ [수정] 타입 기반 검사
      if (!res.ok || rs?.errorCode !== "200") {
        const msg =
          rs?.msg ||
          (contentType.includes("text/")
            ? await res.text()
            : `요청 실패 (${res.status})`);
        alert(msg);
        return;
      }

      alert(rs.msg || "회원정보 수정 완료 🎉");
      router.replace("/user/mypage");
    } catch (err) {
      console.error(err);
      alert("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center bg-gray-50 px-4 py-10 min-h-[calc(100vh-64px)]">
      <div className="bg-white shadow-md rounded-xl p-8 w-full max-w-md">
        <h1 className="text-2xl font-bold text-center text-blue-600 mb-2">
          회원 정보 수정
        </h1>

        <form className="space-y-4" onSubmit={handleSubmit} noValidate>
          {/* 새 닉네임 */}
          <input
            type="text"
            placeholder="새 닉네임 (선택)"
            value={newUserName}
            onChange={(e) => setNewUserName(e.target.value)}
            className="w-full border rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500"
          />

          {/* 새 비밀번호 */}
          <input
            type="password"
            placeholder="새 비밀번호 (선택, 8자 이상)"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full border rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500"
            minLength={8}
          />

          {/* 현재 비밀번호 */}
          <input
            type="password"
            placeholder="현재 비밀번호 (필수, 8자 이상)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full border rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500"
          />

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded-md mt-4 hover:bg-blue-700 transition disabled:opacity-50"
          >
            {loading ? "수정 중..." : "수정 완료"}
          </button>
        </form>
      </div>
    </div>
  );
}
