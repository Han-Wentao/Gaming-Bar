import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { listMessages, sendMessage } from "../../../api/modules/message";
import { dissolveRoom, getRoomDetail, joinRoom, leaveRoom } from "../../../api/modules/room";
import type { Message, RoomDetail } from "../../../types/models";

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
    <section className="panel">
      <div className="section-head">
        <div>
          <h2>{room.game_name}</h2>
          <p>
            房主 {room.owner_nickname} · 人数 {room.current_player}/{room.max_player} · 状态 {room.status}
          </p>
        </div>
        <div className="inline-actions">
          <button onClick={() => void loadAll()}>刷新</button>
          {!room.is_joined ? <button onClick={() => void joinRoom(roomId).then(loadAll).catch((e: Error) => setMessage(e.message))}>加入房间</button> : null}
          {room.is_joined ? (
            <button
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
          <h3>成员</h3>
          <ul className="simple-list">
            {room.members.map((member) => (
              <li key={member.user_id}>
                <strong>{member.nickname}</strong>
                <span>{member.join_time}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="sub-panel">
          <h3>聊天</h3>
          <ul className="simple-list messages">
            {messages.map((item) => (
              <li key={item.id}>
                <strong>{item.nickname}</strong>
                <p>{item.content}</p>
                <span>{item.create_time}</span>
              </li>
            ))}
          </ul>
          <form className="inline-form" onSubmit={handleSend}>
            <input value={content} onChange={(event) => setContent(event.target.value)} placeholder="输入 1-500 字消息" />
            <button type="submit">发送</button>
          </form>
        </div>
      </div>

      {message ? <p className="feedback error">{message}</p> : null}
    </section>
  );
}
