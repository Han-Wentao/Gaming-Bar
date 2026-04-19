import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { listMessages, sendMessage } from "../../../api/modules/message";
import { dissolveRoom, getRoomDetail, joinRoom, leaveRoom } from "../../../api/modules/room";
import type { Message, RoomDetail } from "../../../types/models";

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || "?";
}

export function RoomDetailPage() {
  const navigate = useNavigate();
  const { roomId = "" } = useParams();
  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [content, setContent] = useState("");
  const [message, setMessage] = useState("");

  async function loadAll() {
    if (!roomId) {
      return;
    }
    try {
      const [detail, messagePage] = await Promise.all([getRoomDetail(roomId), listMessages(roomId)]);
      setRoom(detail);
      setMessages(messagePage.messages);
      setMessage("");
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  useEffect(() => {
    void loadAll();
  }, [roomId]);

  async function handleSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await sendMessage(roomId, content);
      setContent("");
      await loadAll();
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  if (!room) {
    return (
      <section className="panel">
        <h2>房间详情</h2>
        <p className="feedback">{message || "正在加载房间详情..."}</p>
      </section>
    );
  }

  return (
    <section className="page-stack room-stage">
      <div className="detail-banner">
        <div className="hero-copy">
          <span className="eyebrow">房间详情</span>
          <h2>{room.game_name}</h2>
          <p>
            房主 {room.owner_nickname} · 当前人数 {room.current_player}/{room.max_player} · 状态{" "}
            {room.status === "ready" ? "已就绪" : "等待中"}
          </p>
        </div>

        <div className="stats-grid">
          <article className="metric-card">
            <span className="muted">类型</span>
            <strong>{room.type}</strong>
            <small>{room.start_time ? `开始时间：${room.start_time}` : "即时开局"}</small>
          </article>
          <article className="metric-card">
            <span className="muted">我的身份</span>
            <strong>{room.is_owner ? "房主" : room.is_joined ? "成员" : "访客"}</strong>
            <small>只有成员可以查看完整内容</small>
          </article>
          <article className="metric-card">
            <span className="muted">成员数量</span>
            <strong>{room.members.length}</strong>
            <small>按加入顺序展示</small>
          </article>
        </div>

        <div className="detail-actions">
          <button className="ghost-button" onClick={() => void loadAll()}>
            刷新
          </button>
          {!room.is_joined ? (
            <button onClick={() => void joinRoom(roomId).then(loadAll).catch((e: Error) => setMessage(e.message))}>
              加入房间
            </button>
          ) : null}
          {room.is_joined ? (
            <button
              className="ghost-button"
              onClick={() =>
                void leaveRoom(roomId)
                  .then((result) => {
                    if (result.action === "room_closed") {
                      navigate("/rooms");
                      return;
                    }
                    void loadAll();
                  })
                  .catch((e: Error) => setMessage(e.message))
              }
            >
              退出房间
            </button>
          ) : null}
          {room.is_owner ? (
            <button
              className="danger-button"
              onClick={() =>
                void dissolveRoom(roomId)
                  .then(() => navigate("/rooms"))
                  .catch((e: Error) => setMessage(e.message))
              }
            >
              解散房间
            </button>
          ) : null}
        </div>
      </div>

      <div className="split-layout">
        <div className="sub-panel">
          <div className="section-head">
            <div>
              <h3>成员列表</h3>
              <p>当前房间中的成员会显示在这里。</p>
            </div>
          </div>

          <ul className="simple-list member-list">
            {room.members.map((member) => (
              <li key={member.user_id}>
                <div className="avatar-dot">{getInitial(member.nickname)}</div>
                <div className="member-copy">
                  <strong>{member.nickname}</strong>
                  <span>{member.join_time}</span>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="sub-panel">
          <div className="section-head">
            <div>
              <h3>聊天消息</h3>
              <p>房间成员可以查看历史消息并继续发送聊天内容。</p>
            </div>
          </div>

          <ul className="simple-list messages message-list">
            {messages.map((item) => (
              <li key={item.id}>
                <div className="message-copy">
                  <strong>{item.nickname}</strong>
                  <p>{item.content}</p>
                  <span>{item.create_time}</span>
                </div>
              </li>
            ))}
          </ul>

          <form className="chat-compose" onSubmit={handleSend}>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="输入 1 到 500 字消息..."
            />
            <div className="inline-actions">
              <button type="submit">发送消息</button>
            </div>
          </form>
        </div>
      </div>

      {message ? <p className="feedback error">{message}</p> : null}
    </section>
  );
}
