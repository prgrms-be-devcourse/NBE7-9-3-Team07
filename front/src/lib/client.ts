export type RsData<T> = {
  resultCode?: string; // ✅ resultCode는 있을 수도 있고 없을 수도 있음
  errorCode?: string;  // ✅ 백엔드에서 사용하는 필드명
  msg: string;
  data: T;
};

export async function fetchApi<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}${url}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers || {}),
    },
    credentials: "include", // ✅ 세션 쿠키 유지
  });
  // ✅ JSON 파싱 (예외 처리)
  let rsData: RsData<T>;
  try {
    rsData = await res.json();
  } catch {
    throw new Error("⚠️ 서버로부터 올바른 JSON 응답을 받지 못했습니다.");
  }

  // ✅ HTTP 상태 코드 확인 (ex. 404, 500 등)
if (!res.ok) {
  // ✅ 409(중복), 404(데이터 없음) 등은 비즈니스 로직 오류로 간주
  if (res.status === 409 || res.status === 404) {
    console.warn(`[Business Warning ${res.status}] ${rsData?.msg}`);
    return rsData as unknown as T;
  }
  throw new Error(rsData?.msg || "API 요청 실패");
}


  // ✅ 백엔드의 resultCode / errorCode 대응
  const code = rsData.resultCode || rsData.errorCode;
  if (!code?.startsWith("200") && !code?.startsWith("201")) {
    throw new Error(rsData.msg || "서버 처리 실패");
  }

  // ✅ resultCode/msg 구조면 data만 반환, 그 외엔 전체 반환
  return rsData.data ?? (rsData as unknown as T);
}
