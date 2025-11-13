// src/app/page.tsx
import { redirect } from "next/navigation";

export default function RootPage() {
  redirect("/home"); // ✅ "/" 진입 시 "/home"으로 리다이렉트
}