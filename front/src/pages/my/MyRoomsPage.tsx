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

  const ownerCount = rooms.filter((room) => room.is_owner).length;

  return (
    <section className="page-stack">
      <div className="page-hero hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">我的房间</span>
          <h2>当前参与的房间</h2>
          <p>这里会展示你仍然可以进入的有效房间，方便继续聊天或管理成员。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">有效房间</span>
            <strong>{rooms.length}</strong>
            <small>已过滤失效记录</small>
          </article>
          <article className="metric-card">
            <span className="muted">我是房主</span>
            <strong>{ownerCount}</strong>
            <small>可直接管理房间</small>
          </article>
          <article className="metric-card">
            <span className="muted">我是成员</span>
            <strong>{rooms.length - ownerCount}</strong>
            <small>可继续查看聊天</small>
          </article>
        </div>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>房间记录</h3>
            <p>点击任意房间即可进入详情页面，查看成员和聊天信息。</p>
          </div>
        </div>

        {message ? <p className="feedback error">{message}</p> : null}

        {rooms.length === 0 ? (
          <div className="empty-state">当前没有可继续进入的房间，你可以先去房间列表看看。</div>
        ) : (
          <div className="card-grid">
            {rooms.map((room) => (
              <article key={room.id} className="card stagger-in">
                <div className="card-topline">
                  <div className="card-title">
                    <span className="card-kicker">ROOM #{room.id}</span>
                    <strong>{room.game_name}</strong>
                  </div>
                  <span className={`status-badge ${room.status === "ready" ? "status-ready" : "status-waiting"}`}>
                    {room.status === "ready" ? "已满" : "等待中"}
                  </span>
                </div>

                <div className="meta-grid">
                  <div className="meta-row">
                    <span>房主</span>
                    <span className="meta-strong">{room.owner_nickname}</span>
                  </div>
                  <div className="meta-row">
                    <span>身份</span>
                    <span className="type-badge">{room.is_owner ? "房主" : "成员"}</span>
                  </div>
                  <div className="meta-row">
                    <span>人数</span>
                    <span className="meta-strong">
                      {room.current_player}/{room.max_player}
                    </span>
                  </div>
                </div>

                <button onClick={() => navigate(`/rooms/${room.id}`)}>进入详情</button>
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}
