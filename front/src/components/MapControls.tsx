export default function MapControls() {
  const getMap = () => (window as any)?.kakao?.maps && (document as any)?.mapInstance;

  return (
    <div className="absolute bottom-6 right-6 flex flex-col gap-3 z-50">
      <button
        className="bg-white border rounded-full shadow-md p-3 hover:bg-gray-100"
        onClick={() => {
          const map = (window as any).mapRef;
          if (map) map.setLevel(map.getLevel() + 1);
        }}
      >
        －
      </button>
      <button
        className="bg-white border rounded-full shadow-md p-3 hover:bg-gray-100"
        onClick={() => {
          const map = (window as any).mapRef;
          if (map) map.setLevel(map.getLevel() - 1);
        }}
      >
        ＋
      </button>
    </div>
  );
}
