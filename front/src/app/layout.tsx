// src/app/layout.tsx
import { AuthProvider } from "@/context/AuthContext";
import "./globals.css";
import Header from "@/components/layout/Header";

export const metadata = {
  title: "PinCo",
  description: "위치 기반 게시물 플랫폼",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <AuthProvider>
          <Header />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
