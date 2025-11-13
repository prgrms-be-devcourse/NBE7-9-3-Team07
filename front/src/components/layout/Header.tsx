"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function Header() {
  const { isLoggedIn, logout } = useAuth();
  const pathname = usePathname();

  // ✅ 현재 경로에 따라 색상 강조
  const linkClass = (path: string) =>
    `hover:text-blue-600 transition ${
      pathname === path ? "text-blue-600 font-semibold" : "text-gray-700"
    }`;

  return (
    <header className="flex justify-between items-center px-6 py-4 bg-white shadow-sm">
      {/* ✅ 로고 */}
      <Link
        href="/home"
        className={`text-xl font-bold ${
          pathname === "/home" ? "text-blue-600" : "text-gray-700"
        }`}
      >
        🎈 <span>PinCo</span>
      </Link>

      {/* ✅ 네비게이션 */}
      <nav className="flex gap-6 items-center text-sm">
        {isLoggedIn ? (
          <>
            {/* 🔹 로그인 상태일 때 표시 */}
            <Link href="/home" className={linkClass("/home")}>
              홈
            </Link>
            <Link href="/calendar" className={linkClass("/calendar")}>
              캘린더
            </Link>
            <Link href="/user/mypage" className={linkClass("/user/mypage")}>
              마이페이지
            </Link>
            <button
              onClick={logout}
              className="text-gray-700 hover:text-red-500 transition"
            >
              로그아웃
            </button>
          </>
        ) : (
          <>
            {/* 🔹 로그아웃 상태일 때 표시 */}
            <Link href="/user/login" className={linkClass("/user/login")}>
              로그인
            </Link>
            <Link href="/user/join" className={linkClass("/user/join")}>
              회원가입
            </Link>
          </>
        )}
      </nav>
    </header>
  );
}
