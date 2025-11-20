"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Mail, Lock, User, Key } from "lucide-react";
import { apiJoin, apiSendVerificationCode } from "@/lib/pincoApi";

export default function SignUpPage() {
  const router = useRouter();
  const [form, setForm] = useState({ userName: "", email: "", password: "", verificationCode: "" });
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [codeSent, setCodeSent] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handlePasswordBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const password = e.target.value.trim();
    if (password && password.length < 8) {
      alert("비밀번호는 8자 이상 입력해야 합니다.");
    }
  };

  const handleSendVerificationCode = async () => {
    const email = form.email.trim().toLowerCase();
    
    if (!email) {
      alert("이메일을 먼저 입력해주세요.");
      return;
    }

    // 이메일 형식 간단 검증
    if (!email.includes("@") || !email.includes(".")) {
      alert("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    setSendingCode(true);
    try {
      await apiSendVerificationCode(email);
      alert("인증코드가 발송되었습니다. 이메일을 확인해주세요.");
      setCodeSent(true);
    } catch (err: any) {
      const msg = err?.message ?? "";
      if (msg.includes("이메일 형식")) {
        alert("이메일 형식이 올바르지 않습니다.");
      } else {
        alert(msg || "인증코드 발송에 실패했습니다.");
      }
    } finally {
      setSendingCode(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);

    const email = form.email.trim().toLowerCase();
    const password = form.password.trim();
    const userName = form.userName.trim();
    const verificationCode = form.verificationCode.trim();

    // ⚙️ 프론트 최소 유효성
    if (password.length < 8) {
      alert("비밀번호는 8자 이상이어야 합니다.");
      setLoading(false);
      return;
    }

    if (!verificationCode) {
      alert("인증코드를 입력해주세요.");
      setLoading(false);
      return;
    }

    try {
      // ⚙️ 서버 요청
      const res: any = await apiJoin(email, password, userName, verificationCode);
      const code = res?.resultCode ?? res?.errorCode ?? "200";
      const msg = res?.msg ?? "";

      // ✅ 이메일 중복 처리
      if (
        String(code).startsWith("409") ||
        msg.includes("이메일") ||
        msg.includes("EMAIL_ALREADY_EXISTS")
      ) {
        if (msg.includes("이메일") || msg.includes("EMAIL_ALREADY_EXISTS")) {
          alert(msg || "이미 존재하는 이메일입니다. 로그인하거나 다른 이메일을 사용해주세요.");
          setLoading(false);
          return;
        }
      }

      // ✅ 회원 이름(닉네임) 중복 처리
      if (
        code === "2005" ||                     // ⬅️ 핵심: 백엔드가 준 숫자 코드
        code === "NICKNAME_ALREADY_EXISTS" ||  // 혹시 문자열 코드로 올 때
        msg.includes("이미 존재하는 회원이름입니다.") ||
        msg.includes("회원이름") || msg.includes("회원 이름") || msg.includes("닉네임")
      ) {
        alert(msg || "이미 존재하는 회원이름입니다. 다른 이름을 입력해주세요.");
        setLoading(false);
        return;
      }

      // ✅ 성공 케이스
      alert("회원가입이 완료되었습니다 🎉");
      router.push("/user/login");
    } catch (err: any) {
      // ❌ 예외 (서버에서 throw한 Error)
      const raw = err?.message ?? "";
      if (raw.includes("이메일 형식"))
        alert("이메일 형식이 올바르지 않습니다. 예: example@domain.com");
      else if (raw.includes("비밀번호") && raw.includes("8"))
        alert("비밀번호는 8자 이상 입력해야 합니다.");
      else if (raw.includes("닉네임") || raw.includes("이름 형식"))
        alert("닉네임은 2자 이상 20자 이하로 입력해주세요.");
      else if (raw.includes("이미 존재하는 이메일"))
        alert("이미 가입된 이메일입니다. 로그인해주세요.");
      else if (raw.includes("이미 사용 중인 회원이름"))
        alert("이미 사용 중인 회원 이름입니다. 다른 이름을 입력해주세요.");
      else if (raw.includes("인증 코드") || raw.includes("인증코드"))
        alert(raw || "인증코드가 일치하지 않거나 만료되었습니다.");
      else alert(raw || "회원가입 중 오류가 발생했습니다 ❌");
    } finally {
      setLoading(false);
    }
  };

  // ==========================
  // UI 영역
  // ==========================
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="bg-white shadow-md rounded-xl p-8 w-full max-w-md">
        <h1 className="text-2xl font-bold text-center text-blue-600 mb-6">
          회원가입
        </h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 이름 */}
          <div className="relative">
            <User className="absolute left-3 top-3 text-gray-400" size={18} />
            <input
              name="userName"
              value={form.userName}
              onChange={handleChange}
              placeholder="이름 (2~20자)"
              required
              className="w-full border rounded-md pl-10 pr-3 py-2 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* 이메일 */}
          <div className="relative">
            <Mail className="absolute left-3 top-3 text-gray-400" size={18} />
            <div className="flex gap-2">
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="이메일 주소"
                required
                className="flex-1 border rounded-md pl-10 pr-3 py-2 focus:ring-2 focus:ring-blue-500"
                disabled={codeSent}
              />
              <button
                type="button"
                onClick={handleSendVerificationCode}
                disabled={sendingCode || codeSent || !form.email}
                className={`px-4 py-2 rounded-md text-sm whitespace-nowrap transition
                  ${sendingCode || codeSent || !form.email
                    ? "bg-gray-300 text-gray-500 cursor-not-allowed"
                    : "bg-blue-500 text-white hover:bg-blue-600"
                  }`}
              >
                {sendingCode ? "발송 중..." : codeSent ? "발송 완료" : "인증번호 전송"}
              </button>
            </div>
          </div>

          {/* 인증코드 */}
          {codeSent && (
            <div className="relative">
              <Key className="absolute left-3 top-3 text-gray-400" size={18} />
              <input
                type="text"
                name="verificationCode"
                value={form.verificationCode}
                onChange={handleChange}
                placeholder="인증코드 6자리 입력"
                maxLength={6}
                required
                className="w-full border rounded-md pl-10 pr-3 py-2 focus:ring-2 focus:ring-blue-500"
              />
            </div>
          )}

          {/* 비밀번호 */}
          <div className="relative">
            <Lock className="absolute left-3 top-3 text-gray-400" size={18} />
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              onBlur={handlePasswordBlur}
              placeholder="비밀번호 (8자 이상)"
              minLength={8}
              required
              className="w-full border rounded-md pl-10 pr-3 py-2 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className={`w-full bg-blue-600 text-white py-2 rounded-md mt-4 transition 
              ${loading ? "opacity-50 cursor-not-allowed" : "hover:bg-blue-700"}`}
          >
            {loading ? "처리 중..." : "회원가입"}
          </button>
        </form>
      </div>
    </div>
  );
}
