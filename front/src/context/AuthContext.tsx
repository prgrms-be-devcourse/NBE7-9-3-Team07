"use client";
import {createContext, useContext, useEffect, useState} from "react";

type User = { id: number; email: string; name?: string } | null;

type AuthContextType = {
    user: User;
    isLoggedIn: boolean;
    login: (email: string, password: string) => Promise<boolean>;
    logout: () => void;
};

const AuthContext = createContext<AuthContextType>({
    user: null,
    isLoggedIn: false,
    login: async () => false,
    logout: () => {
    },
});

export const AuthProvider = ({children}: { children: React.ReactNode }) => {
    const [user, setUser] = useState<User>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (typeof window !== "undefined") {
            const saved = localStorage.getItem("user");
            if (saved) {
                try {
                    setUser(JSON.parse(saved));
                } catch (error) {
                    console.error("사용자 정보 파싱 실패:", error);
                    localStorage.removeItem("user");
                }
            }
        }
        setIsLoading(false);
    }, []);

    const login = async (email: string, password: string) => {
      // (선호에 따라 여기서도 빈 값 체크 가능)
      // if (!email?.trim() || !password?.trim()) {
      //   alert("이메일과 비밀번호를 입력해주세요.");
      //   return false;
      // }

      try {
        // 1) 로그인 요청
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({ email, password }),
        });

        // 실패 상태코드 → 알림만 띄우고 종료
        if (!res.ok) {
          // 상태코드 기반 메시지 (404도 깔끔 처리)
          if (res.status === 401) {
            alert("이메일 또는 비밀번호가 올바르지 않습니다.");
          } else if (res.status === 404) {
            // 백엔드가 404를 주는 경우도 “존재하지 않는 이메일”로 매핑
            alert("존재하지 않는 이메일입니다.");
          } else if (res.status === 403) {
            alert("접근이 거부되었습니다. CORS 설정을 확인해주세요.");
          } else {
            alert(`로그인 실패 (${res.status})`);
          }
          return false;
        }

        // 안전 JSON 파싱 (빈 본문 대비)
        const text = await res.text();
        if (!text?.trim()) {
          alert("서버에서 응답이 없습니다.");
          return false;
        }
        let data: any = null;
        try {
          data = JSON.parse(text);
        } catch {
          alert("서버 응답 형식이 올바르지 않습니다.");
          return false;
        }

        // 2) RsData 검사 및 토큰 저장
        if (data?.errorCode !== "200") {
          alert(data?.msg || "로그인에 실패했습니다.");
          return false;
        }

        const { apiKey, accessToken, refreshToken } = data.data ?? {};
        if (typeof window !== "undefined") {
          localStorage.setItem("apiKey", apiKey ?? "");
          localStorage.setItem("accessToken", accessToken ?? "");
          localStorage.setItem("refreshToken", refreshToken ?? "");
        }

        // 3) 사용자 정보 조회
        const userRes = await fetch(
          `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/getInfo`,
          {
            method: "GET",
            headers: {
              "Content-Type": "application/json",
              // 백엔드 규약에 맞춰 유지 (apiKey + accessToken)
              Authorization: `Bearer ${apiKey} ${accessToken}`,
            },
            credentials: "include",
          }
        );

        if (!userRes.ok) {
          alert("사용자 정보를 불러오는데 실패했습니다.");
          // 토큰 정리
          if (typeof window !== "undefined") {
            localStorage.removeItem("apiKey");
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
          }
          return false;
        }

        const userText = await userRes.text();
        if (!userText?.trim()) {
          alert("빈 사용자 정보 응답입니다.");
          return false;
        }

        let userData: any = null;
        try {
          userData = JSON.parse(userText);
        } catch {
          alert("사용자 정보 응답 형식이 올바르지 않습니다.");
          return false;
        }

        if (userData?.errorCode !== "200") {
          alert(userData?.msg || "사용자 정보를 불러올 수 없습니다.");
          // 필요 시 토큰 삭제
          if (typeof window !== "undefined") {
            localStorage.removeItem("apiKey");
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
          }
          return false;
        }

        // 4) 사용자 상태 반영
        const loggedUser = {
          id: userData.data.id,
          email: userData.data.email,
          name: userData.data.userName,
          createdAt: userData.data.createdAt,
          modifiedAt: userData.data.modifiedAt,
        };

        if (typeof window !== "undefined") {
          localStorage.setItem("user", JSON.stringify(loggedUser));
        }
        setUser(loggedUser);

        // 성공
        return true;
      } catch (err) {
        // 어떤 경우에도 콘솔/오버레이 노출 피하기 위해 throw/console.error 안 함
        alert("네트워크 오류가 발생했습니다. 연결을 확인해주세요.");
        return false;
      }
    };


    const logout = async () => {
      try {
        // 1️⃣ 백엔드 로그아웃 API 호출
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/logout`, {
          method: "POST",
          credentials: "include", // ✅ 쿠키 기반 인증이라면 필수
        });

        // 2️⃣ 응답 확인
        if (res.ok) {
          const text = await res.text();
          let data: any = null;
          try {
            data = JSON.parse(text);
          } catch {
            // 비정상 응답일 경우에도 그냥 진행
          }

          alert(data?.msg || "로그아웃 성공");
        } else {
          alert(`로그아웃 실패 (${res.status})`);
        }
      } catch (err) {
        alert("로그아웃 중 오류가 발생했습니다.");
      }

      // 3️⃣ 항상 클라이언트 측 세션 초기화
      if (typeof window !== "undefined") {
        localStorage.removeItem("user");
        localStorage.removeItem("apiKey");
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
      }

      setUser(null);

       window.location.assign("/");
    };


    const isLoggedIn = !!user;

    if (isLoading) {
        return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
    }

    return (
        <AuthContext.Provider value={{user, isLoggedIn, login, logout}}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);