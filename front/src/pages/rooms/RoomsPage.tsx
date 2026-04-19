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
          <span className="eyebrow">房间大厅</span>
          <h2>当前可加入的房间</h2>
          <p>优先展示仍可加入的房间，方便你快速查看人数、状态和房主信息。</p>
          <div className="hero-rail">
            <span className="hero-chip">空位可见</span>
            <span className="hero-chip">人数直观</span>
            <span className="hero-chip">支持直接加入</span>
          </div>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">房间总数</span>
            <strong>{rooms.length}</strong>
            <small>当前列表返回结果</small>
          </article>
          <article className="metric-card">
            <span className="muted">等待中</span>
            <strong>{waitingCount}</strong>
            <small>可以继续加入</small>
          </article>
          <article className="metric-card">
            <span className="muted">已就绪</span>
            <strong>{readyCount}</strong>
            <small>接近或已经满员</small>
          </article>
        </div>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>房间列表</h3>
            <p>你可以先查看详情，也可以直接加入仍有空位的房间。</p>
          </div>
          <div className="inline-actions">
            <button className="ghost-button" onClick={() => void loadRooms()}>
              刷新
            </button>
            <Link to="/rooms/create" className="button-link">
              创建房间
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
                  {room.status === "ready" ? "已就绪" : "等待中"}
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
                  <span>人数</span>
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

        {rooms.length === 0 ? <div className="empty-state">当前没有可展示的房间。</div> : null}
      </section>
    </section>
  );
}
