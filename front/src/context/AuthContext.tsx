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

    // ✅ 쿠키 기반: 현재 로그인 사용자 정보를 가져오는 공통 함수
    const fetchCurrentUser = async () => {
        try {
            const res = await fetch(
                `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/getInfo`,
                {
                    method: "GET",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include", // 🔑 쿠키를 전송하기 위해 항상 포함
                }
            );

            if (!res.ok) return null; // 401/403/500 등은 "로그인 안됨"으로 취급

            const text = await res.text();
            if (!text?.trim()) return null;

            let data: any;
            try {
                data = JSON.parse(text);
            } catch {
                return null;
            }

            const code = data?.resultCode ?? data?.errorCode;
            if (code && !`${code}`.startsWith("200")) return null;
            if (!data?.data) return null;

            const u = {
                id: data.data.id,
                email: data.data.email,
                name: data.data.userName,
                createdAt: data.data.createdAt,
                modifiedAt: data.data.modifiedAt,
            };

            if (typeof window !== "undefined") {
                localStorage.setItem("user", JSON.stringify(u));
            }
            setUser(u);
            return u;
        } catch {
            return null;
        }
    };

    useEffect(() => {
        const initAuth = async () => {
            // 1) 클라이언트 저장값 우선 사용 (새로고침 등)
            if (typeof window !== "undefined") {
                const saved = localStorage.getItem("user");
                if (saved) {
                    try {
                        setUser(JSON.parse(saved));
                        setIsLoading(false);
                        return;
                    } catch {
                        localStorage.removeItem("user");
                    }
                }
            }

            // 2) 없으면 쿠키 기반으로 서버에 현재 유저 정보 요청 (소셜 로그인 직후 포함)
            await fetchCurrentUser();
            setIsLoading(false);
        };

        initAuth();
    }, []);

    // ✅ 이메일/비밀번호 로그인 (쿠키 발급을 전제로 함)
    const login = async (email: string, password: string) => {
        try {
            const res = await fetch(
                `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/login`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include", // 🔑 로그인 시에도 쿠키를 받기 위해 필요
                    body: JSON.stringify({ email, password }),
                }
            );

            if (!res.ok) {
                if (res.status === 401) {
                    alert("이메일 또는 비밀번호가 올바르지 않습니다.");
                } else if (res.status === 404) {
                    alert("존재하지 않는 이메일입니다.");
                } else {
                    alert(`로그인 실패 (${res.status})`);
                }
                return false;
            }

            // 백엔드는 여기서 HttpOnly 쿠키(apiKey/accessToken 등)를 내려준다고 가정
            // 프론트는 쿠키 내용을 직접 건드리지 않고, 바로 현재 사용자 정보를 다시 요청
            const userInfo = await fetchCurrentUser();
            if (!userInfo) {
                alert("사용자 정보를 불러오지 못했습니다.");
                return false;
            }

            return true;
        } catch {
            alert("네트워크 오류가 발생했습니다. 연결을 확인해주세요.");
            return false;
        }
    };

    // ✅ 로그아웃: 서버에서 쿠키 삭제 + 프론트 상태/로컬 저장소 초기화
    const logout = async () => {
        try {
            await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/user/logout`, {
                method: "POST",
                credentials: "include", // 🔑 서버가 쿠키를 지우도록 전달
            });
        } catch {
            // 네트워크 오류여도 클라이언트 상태는 정리
        }

        if (typeof window !== "undefined") {
            localStorage.removeItem("user");
        }
        setUser(null);

        if (typeof window !== "undefined") {
            window.location.assign("/");
        }
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