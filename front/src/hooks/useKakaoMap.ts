import {useEffect, useRef} from "react";
import {PinDto} from "../types/types";

export function useKakaoMap({
                                pins,
                                center,
                                onSelectPin,
                                kakaoReady,
                                onCenterChange,
                                onRightClick,
                            }: {
    pins: PinDto[];
    center: { lat: number; lng: number };
    onSelectPin: (pin: PinDto) => void;
    kakaoReady?: boolean;
    onCenterChange?: (lat: number, lng: number) => void;
    onRightClick?: (lat: number, lng: number) => void;
}) {
    const mapRef = useRef<any>(null);
    const clustererRef = useRef<any>(null);
    const debounceTimerRef = useRef<NodeJS.Timeout | null>(null);
    const lastCenterRef = useRef({lat: center.lat, lng: center.lng});
    const userZoomLevelRef = useRef<number>(4);
    const isUpdatingCenterRef = useRef(false);

    // ✅ 지도 초기화 (kakaoReady 이후에만)
    useEffect(() => {
        if (!kakaoReady) return;

        const kakao = (window as any).kakao;
        if (!kakao?.maps) return;

        const el = document.getElementById("map");
        if (!el) return;

        const map = new kakao.maps.Map(el, {
            center: new kakao.maps.LatLng(center.lat, center.lng),
            level: 4,
        });

        mapRef.current = map;
        (window as any).mapRef = map;

        userZoomLevelRef.current = 4;

        // 줌 레벨 변경 이벤트 리스너
        kakao.maps.event.addListener(map, 'zoom_changed', () => {
            if (!isUpdatingCenterRef.current) {
                const currentLevel = map.getLevel();
                userZoomLevelRef.current = currentLevel;
            }
        });

        // 드래그 이벤트 - onCenterChange 호출 제거
        kakao.maps.event.addListener(map, 'dragend', () => {
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }

            debounceTimerRef.current = setTimeout(() => {
                const centerLatLng = map.getCenter();
                const newLat = centerLatLng.getLat();
                const newLng = centerLatLng.getLng();

                const latDiff = Math.abs(newLat - lastCenterRef.current.lat);
                const lngDiff = Math.abs(newLng - lastCenterRef.current.lng);

                if (latDiff > 0.0001 || lngDiff > 0.0001) {
                    lastCenterRef.current = {lat: newLat, lng: newLng};

                    if (onCenterChange) {
                      onCenterChange(newLat, newLng);
                    }
                }
            }, 500);
        });

        kakao.maps.event.addListener(map, 'rightclick', (mouseEvent: any) => {
            const latlng = mouseEvent.latLng;
            const lat = latlng.getLat();
            const lng = latlng.getLng();

            // onRightClick 콜백 함수가 존재하면 호출
            if (onRightClick) {
                onRightClick(lat, lng);
            }
        });

        return () => {
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }
        };
    }, [kakaoReady]); // onCenterChange 의존성 제거

    // ✅ center prop이 외부에서 변경되었을 때만 지도 이동
    useEffect(() => {
        const kakao = (window as any).kakao;
        if (!kakao?.maps || !mapRef.current) return;

        const map = mapRef.current;

        // 외부에서 center가 변경되었는지 확인 (드래그가 아닌 경우)
        const currentMapCenter = map.getCenter();
        const currentLat = currentMapCenter.getLat();
        const currentLng = currentMapCenter.getLng();

        const latDiff = Math.abs(currentLat - center.lat);
        const lngDiff = Math.abs(currentLng - center.lng);

        // 외부에서 center prop이 실제로 변경되었을 때만 실행
        if (latDiff > 0.0001 || lngDiff > 0.0001) {
            isUpdatingCenterRef.current = true;
            const savedLevel = userZoomLevelRef.current;

            const ll = new kakao.maps.LatLng(center.lat, center.lng);

            // panTo 사용 (부드러운 이동 + 줌 레벨 유지)
            map.panTo(ll);

            // 줌 레벨 강제 유지
            setTimeout(() => {
                const afterLevel = map.getLevel();
                if (afterLevel !== savedLevel) {
                    map.setLevel(savedLevel);
                }
                isUpdatingCenterRef.current = false;
            }, 100);

            // lastCenterRef 업데이트
            lastCenterRef.current = {lat: center.lat, lng: center.lng};
        }
    }, [center, kakaoReady]);

    // ✅ 마커 및 클러스터러 관리
    useEffect(() => {
        const kakao = (window as any).kakao;
        const map = mapRef.current;
        if (!kakao?.maps || !map) return;

        if (clustererRef.current) clustererRef.current.clear();

        const clusterer = new kakao.maps.MarkerClusterer({
            map,
            averageCenter: true,
            minLevel: 3,
            gridSize: 60,
            disableClickZoom: false,
            calculator: [10, 30, 50],
        });

        const markers = pins.map((pin) => {
            const marker = new kakao.maps.Marker({
                position: new kakao.maps.LatLng(pin.latitude, pin.longitude),
            });
            kakao.maps.event.addListener(marker, "click", () => onSelectPin(pin));
            return marker;
        });

        clusterer.addMarkers(markers);
        clustererRef.current = clusterer;

        return () => {
            clusterer.clear();
            markers.forEach((m) => m.setMap(null));
        };
    }, [pins, onSelectPin, kakaoReady]);

    return {map: mapRef.current};
}