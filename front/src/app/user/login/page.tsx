"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();

  const [email, setEmail] = useState("user1@example.com");
  const [password, setPassword] = useState("12345678");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitting) return;

    setSubmitting(true);

    try {
      // ✅ 입력값 유효성 검사
      if (!email.trim()) throw new Error("이메일을 입력해주세요.");
      if (!password.trim()) throw new Error("비밀번호를 입력해주세요.");

      const ok = await login(email.trim(), password.trim());
      setSubmitting(false);

      if (ok) {
        alert("로그인 성공 🎉");
        router.push("/");
      }
    } catch (err: any) {
      setSubmitting(false);
      const msg = err?.message || "";

      // ✅ 에러 메시지 분기 (alert만)
      if (msg.includes("이메일을 입력해주세요")) {
        alert("이메일을 입력해주세요.");
      } else if (msg.includes("비밀번호를 입력해주세요")) {
        alert("비밀번호를 입력해주세요.");
      } else if (msg.includes("존재하지 않는 이메일")) {
        alert("존재하지 않는 이메일입니다.");
      } else if (msg.includes("비밀번호가 일치하지 않습니다")) {
        alert("비밀번호가 일치하지 않습니다.");
      } else if (msg.includes("이메일 또는 비밀번호가 올바르지 않습니다")) {
        alert("이메일 또는 비밀번호가 올바르지 않습니다.");
      } else if (msg.includes("404")) {
        alert("로그인 실패: 서버에서 요청을 찾을 수 없습니다 (404)");
      } else {
        alert("로그인 중 오류가 발생했습니다. 다시 시도해주세요.");
      }

      // ❌ console.error("❌ 로그인 오류:", err); ← 제거 완료
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-3 w-80 mx-auto mt-40"
    >
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="이메일"
        className="border rounded p-2"
        autoComplete="email"
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="비밀번호"
        className="border rounded p-2"
        autoComplete="current-password"
      />
      <button
        type="submit"
        className="bg-blue-600 text-white rounded p-2"
        disabled={submitting}
      >
        {submitting ? "로그인 중..." : "로그인"}
      </button>

      <div className="text-sm text-gray-500 text-center mt-2">
        <p>기존 계정으로 로그인해주세요</p>
      </div>
    </form>
  );
}
