import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listMyRooms } from "../../api/modules/room";
import type { RoomMyItem } from "../../types/models";

export function MyRoomsPage() {
  const navigate = useNavigate();
  const [rooms, setRooms] = useState<RoomMyItem[]>([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    listMyRooms({ page: 1, size: 20 })
      .then((result) => setRooms(result.list))
      .catch((error: Error) => setMessage(error.message));
  }, []);

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h2>我的房间</h2>
          <p>接口会先清理已失效房间，再返回剩余有效房间。</p>
        </div>
      </div>
      {message ? <p className="feedback error">{message}</p> : null}
      <div className="card-grid">
        {rooms.map((room) => (
          <article key={room.id} className="card">
            <strong>{room.game_name}</strong>
            <span>房主: {room.owner_nickname}</span>
            <span>状态: {room.status}</span>
            <span>{room.is_owner ? "我是房主" : "我是成员"}</span>
            <button onClick={() => navigate(`/rooms/${room.id}`)}>进入详情</button>
          </article>
        ))}
        {rooms.length === 0 ? <p className="feedback">当前没有有效房间。</p> : null}
      </div>
    </section>
  );
}
