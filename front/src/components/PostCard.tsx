import { PinDto } from "../types/types";

export default function PostCard({
  pin,
  onClick,
}: {
  pin: PinDto;
  onClick: () => void;
}) {
  return (
    <div
      className="border rounded-md p-3 cursor-pointer hover:bg-blue-50 transition"
      onClick={onClick}
    >
      <p className="text-sm text-gray-800 line-clamp-2">{pin.content}</p>

      {pin.tags?.length ? (
        <div className="flex gap-1 mt-2 flex-wrap">
          {pin.tags.map(tag => (
            <span key={tag} className="text-xs text-blue-600">
              #{tag}
            </span>
          ))}
        </div>
      ) : null}

      <div className="flex justify-between text-xs text-gray-500 mt-2">
        <span>{pin.createdAt?.slice(0, 10)}</span>
        <span>{pin.modifiedAt?.slice(0, 10)}</span>
      </div>
    </div>
  );
}
