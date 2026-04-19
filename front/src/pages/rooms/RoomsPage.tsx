import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { joinRoom, listRooms } from "../../api/modules/room";
import type { RoomListItem } from "../../types/models";

function getOccupancy(room: RoomListItem) {
  return Math.min(100, Math.round((room.current_player / room.max_player) * 100));
}

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

  const waitingCount = rooms.filter((room) => room.status === "waiting").length;
  const readyCount = rooms.filter((room) => room.status === "ready").length;

  return (
    <section className="page-stack">
      <div className="page-hero hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">Live Rooms</span>
          <h2>房间大厅</h2>
          <p>优先展示可加入房间，并把人数、状态和行动按钮放在同一层视线里。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">可见房间</span>
            <strong>{rooms.length}</strong>
            <small>当前返回 20 条以内</small>
          </article>
          <article className="metric-card">
            <span className="muted">等待中</span>
            <strong>{waitingCount}</strong>
            <small>更适合快速加入</small>
          </article>
          <article className="metric-card">
            <span className="muted">已就绪</span>
            <strong>{readyCount}</strong>
            <small>接近满员</small>
          </article>
        </div>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>快速匹配视图</h3>
            <p>点击详情可以直接进入房间观察；若有空位，也可以在卡片上立即加入。</p>
          </div>
          <div className="inline-actions">
            <button className="ghost-button" onClick={() => void loadRooms()}>
              刷新
            </button>
            <Link to="/rooms/create" className="button-link">
              创建新房间
            </Link>
          </div>
        </div>

        {message ? <p className="feedback error">{message}</p> : null}

        <div className="card-grid">
          {rooms.map((room) => (
            <article key={room.id} className="card stagger-in">
              <div className="card-topline">
                <div className="card-title">
                  <span className="card-kicker">ROOM #{room.id}</span>
                  <strong>{room.game_name}</strong>
                </div>
                <span className={`status-badge ${room.status === "ready" ? "status-ready" : "status-waiting"}`}>
                  {room.status}
                </span>
              </div>

              <div className="meta-grid">
                <div className="meta-row">
                  <span>房主</span>
                  <span className="meta-strong">{room.owner_nickname}</span>
                </div>
                <div className="meta-row">
                  <span>类型</span>
                  <span className="type-badge">{room.type}</span>
                </div>
              </div>

              <div className="occupancy">
                <div className="meta-row">
                  <span>席位占用</span>
                  <span className="meta-strong">
                    {room.current_player}/{room.max_player}
                  </span>
                </div>
                <div className="occupancy-track">
                  <div className="occupancy-fill" style={{ width: `${getOccupancy(room)}%` }} />
                </div>
              </div>

              <div className="inline-actions">
                <button className="ghost-button" onClick={() => navigate(`/rooms/${room.id}`)}>
                  查看详情
                </button>
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
                  {room.is_joined ? "已加入" : "加入房间"}
                </button>
              </div>
            </article>
          ))}
        </div>

        {rooms.length === 0 ? <div className="empty-state">当前没有可展示的有效房间。</div> : null}
      </section>
    </section>
  );
}
