import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { joinRoom, listRooms } from "../../api/modules/room";
import type { RoomListItem } from "../../types/models";

export function RoomsPage() {
  const navigate = useNavigate();
  const [rooms, setRooms] = useState<RoomListItem[]>([]);
  const [message, setMessage] = useState("");

  const loadRooms = async () => {
    try {
      const result = await listRooms({ page: 1, size: 20 });
      setRooms(result.list);
      setMessage("");
    } catch (error) {
      setMessage((error as Error).message);
    }
  };

  useEffect(() => {
    void loadRooms();
  }, []);

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h2>房间列表</h2>
          <p>默认只展示可加入的 `waiting/ready` 房间。</p>
        </div>
        <div className="inline-actions">
          <button onClick={() => void loadRooms()}>刷新</button>
          <Link to="/rooms/create" className="button-link">
            创建房间
          </Link>
        </div>
      </div>
      {message ? <p className="feedback error">{message}</p> : null}
      <div className="card-grid">
        {rooms.map((room) => (
          <article key={room.id} className="card">
            <strong>{room.game_name}</strong>
            <span>房主: {room.owner_nickname}</span>
            <span>
              人数: {room.current_player}/{room.max_player}
            </span>
            <span>类型: {room.type}</span>
            <span>状态: {room.status}</span>
            <div className="inline-actions">
              <button onClick={() => navigate(`/rooms/${room.id}`)}>详情</button>
              <button
                onClick={async () => {
                  try {
                    await joinRoom(String(room.id));
                    navigate(`/rooms/${room.id}`);
                  } catch (error) {
                    setMessage((error as Error).message);
                  }
                }}
                disabled={room.is_joined || room.status === "ready"}
              >
                {room.is_joined ? "已加入" : "加入"}
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
