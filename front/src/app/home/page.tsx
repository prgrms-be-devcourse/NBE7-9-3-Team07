"use client";

import Script from "next/script";
import {useEffect, useState} from "react";
import Sidebar from "../../components/Sidebar";
import PostModal from "../../components/PostModal";
import CreatePostModal from "../../components/CreatePostModal";
import {usePins} from "../../hooks/usePins";
import {useKakaoMap} from "../../hooks/useKakaoMap";
import {apiCreatePin} from "../../lib/pincoApi";
import {useAuth} from "@/context/AuthContext";
import {Heart, Star, LogOut} from "lucide-react";

export default function PinCoMainPage() {
    const {user, logout} = useAuth();

    // ✅ 추가: 사용자 위치 상태
    // const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
    // const [locationLoading, setLocationLoading] = useState(true);

    const {
        pins,
        loading,
        mode,
        center,
        selectedPin,
        selectedTags,
        allTags,
        setCenter,
        setSelectedPin,
        loadAllPins,
        loadNearbyPins,
        applyTagFilter,
        clearTagFilter,
        loadMyBookmarks,
        loadLikedPins,
        ensurePinTagsLoaded,
        reloadTags,
    } = usePins(
        // ✅ 수정: 초기 중심을 사용자 위치 또는 기본값으로
        // userLocation || { lat: 37.5665, lng: 126.978 },
        { lat: 37.5665, lng: 126.978 },
        user?.id ?? null
    );

    const [kakaoReady, setKakaoReady] = useState(false);
    const [rightClickCenter, setRightClickCenter] = useState<{ lat: number; lng: number } | null>(null);

    // // ✅ 추가: 사용자 위치 가져오기
    // useEffect(() => {
    //     if (navigator.geolocation) {
    //         console.log("📍 GPS 위치 요청 중...");
    //
    //         navigator.geolocation.getCurrentPosition(
    //             (position) => {
    //                 const { latitude, longitude } = position.coords;
    //                 console.log("✅ GPS 위치 획득:", { lat: latitude, lng: longitude });
    //
    //                 setUserLocation({ lat: latitude, lng: longitude });
    //                 setCenter({ lat: latitude, lng: longitude });
    //                 setLocationLoading(false);
    //             },
    //             (error) => {
    //                 console.error("❌ GPS 위치 획득 실패:", error);
    //
    //                 // 에러 메시지 표시
    //                 let errorMsg = "";
    //                 switch(error.code) {
    //                     case error.PERMISSION_DENIED:
    //                         errorMsg = "위치 권한이 거부되었습니다. 기본 위치(서울)로 설정합니다.";
    //                         break;
    //                     case error.POSITION_UNAVAILABLE:
    //                         errorMsg = "위치 정보를 사용할 수 없습니다. 기본 위치(서울)로 설정합니다.";
    //                         break;
    //                     case error.TIMEOUT:
    //                         errorMsg = "위치 요청 시간이 초과되었습니다. 기본 위치(서울)로 설정합니다.";
    //                         break;
    //                 }
    //
    //                 console.warn("⚠️", errorMsg);
    //
    //                 // 기본 위치 설정 (서울 시청)
    //                 setUserLocation({ lat: 37.5665, lng: 126.978 });
    //                 setCenter({ lat: 37.5665, lng: 126.978 });
    //                 setLocationLoading(false);
    //             },
    //             {
    //                 enableHighAccuracy: true, // 높은 정확도 요청
    //                 timeout: 10000, // 10초 타임아웃
    //                 maximumAge: 0 // 캐시된 위치 사용 안 함
    //             }
    //         );
    //     } else {
    //         console.error("❌ 브라우저가 Geolocation을 지원하지 않습니다.");
    //         alert("이 브라우저는 위치 서비스를 지원하지 않습니다. 기본 위치(서울)로 설정합니다.");
    //
    //         setUserLocation({ lat: 37.5665, lng: 126.978 });
    //         setCenter({ lat: 37.5665, lng: 126.978 });
    //         setLocationLoading(false);
    //     }
    // }, []);

    useEffect(() => {
        const t = setInterval(() => {
            const w = window as any;
            if (w.kakao?.maps) {
                w.kakao.maps.load(() => setKakaoReady(true));
                clearInterval(t);
            }
        }, 80);
        return () => clearInterval(t);
    }, []);

    useKakaoMap({
        pins,
        center,
        onSelectPin: async (p) => {
            setSelectedPin(p);  // 태그 로드 없이 바로 설정
            setCenter({ lat: p.latitude, lng: p.longitude });
        },
        kakaoReady,
        onCenterChange: (lat, lng) => {
            setCenter({lat, lng});
        },
        onRightClick: (lat, lng) => {
            setRightClickCenter({lat, lng});
        },
    });

    const [radius, setRadius] = useState(1000.0);

    const updateRadiusFromScreen = () => {
        const kakao = (window as any).kakao;
        const map = (window as any).mapRef;
        if (!kakao?.maps || !map) return;

        const bounds = map.getBounds();
        const sw = bounds.getSouthWest();
        const ne = bounds.getNorthEast();

        const R = 6371000;
        const toRad = (deg: number) => (deg * Math.PI) / 180;
        const dLat = toRad(ne.getLat() - sw.getLat());
        const dLng = toRad(ne.getLng() - sw.getLng());
        const a =
            Math.sin(dLat / 2) ** 2 +
            Math.cos(toRad(sw.getLat())) *
            Math.cos(toRad(ne.getLat())) *
            Math.sin(dLng / 2) ** 2;
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        const diagonal = R * c;

        const newRadius = diagonal / 2;
        setRadius(newRadius);
    };

    useEffect(() => {
        if (!kakaoReady) return;

        const kakao = (window as any).kakao;
        const map = (window as any).mapRef;
        if (!kakao?.maps || !map) return;

        const handleMapIdle = () => {
            updateRadiusFromScreen();

            if (mode === "screen") {
                loadAllPins(center.lat, center.lng, radius);
            }
        };

        kakao.maps.event.addListener(map, "idle", handleMapIdle);

        updateRadiusFromScreen();
        if (mode === "screen") {
            loadAllPins(center.lat, center.lng, radius);
        }

        return () => {
            kakao.maps.event.removeListener(map, "idle", handleMapIdle);
        };
    }, [kakaoReady, mode, center.lat, center.lng, radius]);

    const [showCreate, setShowCreate] = useState(false);

    useEffect(() => {
        if (rightClickCenter) {
            if (!user) {
                alert("로그인 후 핀을 추가할 수 있습니다.");
                setRightClickCenter(null);
                return;
            }
            setShowCreate(true);
        }
    }, [rightClickCenter, user]);

    // ✅ 추가: 위치 로딩 중 화면
    // if (locationLoading) {
    //     return (
    //         <div className="flex items-center justify-center h-screen bg-gray-50">
    //             <div className="text-center">
    //                 <div className="animate-spin text-4xl mb-4">📍</div>
    //                 <p className="text-gray-600">현재 위치를 확인하는 중...</p>
    //             </div>
    //         </div>
    //     );
    // }

    return (
        <div className="flex flex-col h-[calc(100vh-64px)] overflow-hidden">
            <Script
                src={`//dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=${process.env.NEXT_PUBLIC_KAKAO_APP_KEY}&libraries=clusterer`}
                strategy="afterInteractive"
            />

            <main className="flex flex-1 overflow-hidden">
                <Sidebar
                    pins={pins}
                    loading={loading}
                    mode={mode}
                    allTags={allTags}
                    selectedTags={selectedTags}
                    // ✅ 태그만 변경: 현재 모드 유지하면서 필터 적용
                    onChangeTags={(next) => {
                        applyTagFilter(next);
                    }}
                    // ✅ 모드 변경: 필터 초기화하고 지도에서 찾기
                    onClickAll={() => {
                        loadAllPins(center.lat, center.lng, radius);
                    }}
                    // ✅ 모드 변경: 필터 초기화하고 주변 보기
                    onClickNearBy={() => {
                        loadNearbyPins();
                    }}
                    // ✅ 모드 변경: 필터 초기화하고 북마크 보기
                    onClickMyBookmarks={() => {
                        loadMyBookmarks();
                    }}
                    // ✅ 모드 변경: 필터 초기화하고 좋아요 보기
                    onClickLikedPins={() => {
                        loadLikedPins();
                    }}
                    onSelectPin={async (p) => {
                        const withTags = await ensurePinTagsLoaded(p);
                        setSelectedPin(withTags);
                        setCenter({lat: withTags.latitude, lng: withTags.longitude});
                    }}
                />

                <div className="flex-1 relative">
                    <div id="map" className="w-full h-full"/>

                    <div className="absolute top-4 left-4 bg-white p-3 px-5 rounded-lg shadow-md z-50">
                        <p className="text-sm text-gray-600">
                            👋 {user ? `${user.name} (${user.email})` : "로그인 안됨"}
                        </p>
                        {user && (
                            <button
                                onClick={logout}
                                className="text-xs text-red-500 hover:underline mt-1 flex items-center gap-1"
                            >
                                <LogOut size={14}/> 로그아웃
                            </button>
                        )}
                    </div>

                    {selectedPin && (
                        <PostModal
                            pin={selectedPin}
                            onClose={() => setSelectedPin(null)}
                            userId={user?.id ?? null}
                            onChanged={async () => {
                                if (mode === "screen") await loadAllPins(center.lat, center.lng, radius);
                                else if (mode === "nearby") await loadNearbyPins(center.lat, center.lng);
                                else if (mode === "tag") await applyTagFilter(selectedTags);
                                else if (mode === "bookmark") await loadMyBookmarks();
                                else if (mode === "liked") await loadLikedPins();
                                else await loadAllPins();
                            }}
                        />
                    )}

                    {showCreate && (
                        <CreatePostModal
                            lat={rightClickCenter?.lat ?? center.lat}
                            lng={rightClickCenter?.lng ?? center.lng}
                            userId={user?.id}
                            onClose={() => {
                                setShowCreate(false);
                                setRightClickCenter(null);
                            }}
                            onCreated={async () => {
                                if (mode === "screen") await loadAllPins(center.lat, center.lng, radius);
                                else if (mode === "nearby") await loadNearbyPins(center.lat, center.lng);
                                else if (mode === "tag") await applyTagFilter(selectedTags);
                                else if (mode === "bookmark") await loadMyBookmarks();
                                else if (mode === "liked") await loadLikedPins();
                                else await loadAllPins();
                            }}
                            onTagsUpdated={async () => {
                                await reloadTags();
                            }}
                        />
                    )}

                    <button
                        className="absolute bottom-6 left-1/2 -translate-x-1/2 bg-blue-600 text-white px-5 py-3 rounded-lg shadow-lg hover:bg-blue-700 z-50"
                        onClick={() => {
                            if (!user) {
                                alert("로그인 후 이용 가능합니다.");
                                return;
                            }
                            setRightClickCenter(null);
                            setShowCreate(true);
                        }}
                    >
                        + 핀 등록
                    </button>

                    <div className="absolute bottom-6 right-6 flex flex-col gap-3 z-50">
                        {/*<button*/}
                        {/*    className="bg-white border rounded-full shadow-md p-3 hover:bg-gray-100 z-50"*/}
                        {/*    onClick={() => {*/}
                        {/*        if (userLocation) {*/}
                        {/*            setCenter(userLocation);*/}
                        {/*            alert("현재 위치로 이동합니다.");*/}
                        {/*        } else {*/}
                        {/*            alert("위치 정보를 사용할 수 없습니다.");*/}
                        {/*        }*/}
                        {/*    }}*/}
                        {/*>*/}
                        {/*    🚩*/}
                        {/*</button>*/}
                        <button
                            className="bg-white border rounded-full shadow-md p-3 hover:bg-gray-100"
                            onClick={() => (window as any).mapRef?.setLevel((window as any).mapRef.getLevel() + 1)}
                        >
                            －
                        </button>
                        <button
                            className="bg-white border rounded-full shadow-md p-3 hover:bg-gray-100"
                            onClick={() => (window as any).mapRef?.setLevel((window as any).mapRef.getLevel() - 1)}
                        >
                            ＋
                        </button>
                    </div>
                </div>
            </main>

            <style jsx global>{`
                @keyframes fadeIn {
                    from {
                        opacity: 0;
                        transform: translateY(10px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }

                .animate-fadeIn {
                    animation: fadeIn 0.2s ease-out;
                }
            `}</style>
        </div>
    );
}