import { FormEvent, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { createWsTicket } from "../../../api/modules/auth";
import { listMessages } from "../../../api/modules/message";
import { dissolveRoom, getRoomDetail, joinRoom, leaveRoom } from "../../../api/modules/room";
import { useAuthState } from "../../../store/auth-store";
import type { Message, MessagePageResponse, RoomDetail, RoomSocketEvent } from "../../../types/models";

const MESSAGE_PAGE_SIZE = 50;
const HEARTBEAT_INTERVAL_MS = 20000;

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || "?";
}

function sortMessages(messages: Message[]) {
  return [...messages].sort((left, right) => left.id - right.id);
}

function mergeMessages(current: Message[], incoming: Message[]) {
  const unique = new Map<number, Message>();

  for (const item of current) {
    unique.set(item.id, item);
  }

  for (const item of incoming) {
    unique.set(item.id, item);
  }

  return [...unique.values()].sort((left, right) => left.id - right.id);
}

function getSocketUrl(roomId: string, ticket: string) {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/rooms/${encodeURIComponent(roomId)}?ticket=${encodeURIComponent(ticket)}`;
}

function getOnlineCount(detail: RoomDetail) {
  return typeof detail.online_count === "number" ? detail.online_count : detail.members.length;
}

type SocketStatus = "disconnected" | "connecting" | "connected";

export function RoomDetailPage() {
  const navigate = useNavigate();
  const { roomId = "" } = useParams();
  const auth = useAuthState();

  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [nextCursor, setNextCursor] = useState<number | null>(null);
  const [onlineCount, setOnlineCount] = useState(0);
  const [content, setContent] = useState("");
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingEarlier, setIsLoadingEarlier] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [socketStatus, setSocketStatus] = useState<SocketStatus>("disconnected");

  const socketRef = useRef<WebSocket | null>(null);
  const heartbeatTimerRef = useRef<number | null>(null);
  const redirectTimerRef = useRef<number | null>(null);

  function clearRedirectTimer() {
    if (redirectTimerRef.current !== null) {
      window.clearTimeout(redirectTimerRef.current);
      redirectTimerRef.current = null;
    }
  }

  function stopHeartbeat() {
    if (heartbeatTimerRef.current !== null) {
      window.clearInterval(heartbeatTimerRef.current);
      heartbeatTimerRef.current = null;
    }
  }

  function closeSocket() {
    stopHeartbeat();
    const socket = socketRef.current;
    socketRef.current = null;
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      socket.close();
    }
  }

  function applyRoomDetail(detail: RoomDetail) {
    setRoom(detail);
    setOnlineCount(getOnlineCount(detail));
  }

  function applyMessagePage(page: MessagePageResponse) {
    setMessages(sortMessages(page.messages));
    setNextCursor(page.next_cursor);
  }

  async function loadRoomOnly(preserveFeedback = false) {
    if (!roomId) {
      return;
    }

    try {
      const detail = await getRoomDetail(roomId);
      applyRoomDetail(detail);
      if (!detail.is_joined) {
        setMessages([]);
        setNextCursor(null);
      }
      if (!preserveFeedback) {
        setMessage("");
      }
    } catch (error) {
      if (!preserveFeedback) {
        setMessage((error as Error).message);
      }
    }
  }

  async function loadAll() {
    if (!roomId) {
      return;
    }

    setIsLoading(true);
    try {
      const detail = await getRoomDetail(roomId);
      applyRoomDetail(detail);

      if (!detail.is_joined) {
        setMessages([]);
        setNextCursor(null);
        setMessage("");
        return;
      }

      const page = await listMessages(roomId, undefined, MESSAGE_PAGE_SIZE);
      applyMessagePage(page);
      setMessage("");
    } catch (error) {
      setRoom(null);
      setMessages([]);
      setNextCursor(null);
      setOnlineCount(0);
      setMessage((error as Error).message);
    } finally {
      setIsLoading(false);
    }
  }

  async function handleLoadEarlier() {
    if (!roomId || nextCursor === null) {
      return;
    }

    setIsLoadingEarlier(true);
    try {
      const page = await listMessages(roomId, nextCursor, MESSAGE_PAGE_SIZE);
      setMessages((current) => mergeMessages(current, sortMessages(page.messages)));
      setNextCursor(page.next_cursor);
      setMessage("");
    } catch (error) {
      setMessage((error as Error).message);
    } finally {
      setIsLoadingEarlier(false);
    }
  }

  async function handleJoin() {
    try {
      await joinRoom(roomId);
      await loadAll();
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  async function handleLeave() {
    try {
      const result = await leaveRoom(roomId);
      closeSocket();
      setSocketStatus("disconnected");
      setContent("");

      if (result.action === "room_closed") {
        navigate("/rooms");
        return;
      }

      await loadRoomOnly();
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  async function handleDissolve() {
    try {
      await dissolveRoom(roomId);
      closeSocket();
      navigate("/rooms");
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  async function handleSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmed = content.trim();
    if (!trimmed) {
      return;
    }

    if (!room?.is_joined) {
      setMessage("Join the room before sending messages.");
      return;
    }

    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      setMessage("Realtime chat is not connected yet.");
      return;
    }

    setIsSending(true);
    try {
      socket.send(JSON.stringify({ type: "chat", content: trimmed }));
      setContent("");
      setMessage("");
    } catch {
      setMessage("Failed to send the message.");
    } finally {
      setIsSending(false);
    }
  }

  useEffect(() => {
    void loadAll();
  }, [roomId]);

  useEffect(() => {
    if (!roomId || !auth.token || !room?.is_joined) {
      closeSocket();
      setSocketStatus("disconnected");
      return;
    }

    let active = true;
    let currentSocket: WebSocket | null = null;

    clearRedirectTimer();
    closeSocket();
    setSocketStatus("connecting");

    void (async () => {
      try {
        const ticket = await createWsTicket(roomId);
        if (!active) {
          return;
        }

        const socket = new WebSocket(getSocketUrl(roomId, ticket.ticket));
        currentSocket = socket;
        socketRef.current = socket;

        socket.onopen = () => {
          setSocketStatus("connected");
          stopHeartbeat();
          heartbeatTimerRef.current = window.setInterval(() => {
            if (socket.readyState === WebSocket.OPEN) {
              socket.send(JSON.stringify({ type: "ping" }));
            }
          }, HEARTBEAT_INTERVAL_MS);
        };

        socket.onmessage = (event) => {
          let payload: RoomSocketEvent;
          try {
            payload = JSON.parse(event.data) as RoomSocketEvent;
          } catch {
            return;
          }

          if (typeof payload.online_count === "number") {
            setOnlineCount(payload.online_count);
          }

          switch (payload.type) {
            case "chat_message":
              if (payload.message) {
                const nextMessage = payload.message;
                setMessages((current) => mergeMessages(current, [nextMessage]));
              }
              break;
            case "room_closed":
              closeSocket();
              setSocketStatus("disconnected");
              setOnlineCount(0);
              setMessage(payload.text ?? "The room has been closed.");
              clearRedirectTimer();
              redirectTimerRef.current = window.setTimeout(() => {
                navigate("/rooms");
              }, 1200);
              break;
            case "left_room":
              closeSocket();
              setSocketStatus("disconnected");
              setContent("");
              setMessage(payload.text ?? "You have left the room.");
              void loadRoomOnly(true);
              break;
            case "error":
              if (payload.text) {
                setMessage(payload.text);
              }
              break;
            default:
              break;
          }
        };

        socket.onerror = () => {
          setSocketStatus("disconnected");
        };

        socket.onclose = () => {
          if (socketRef.current === socket) {
            socketRef.current = null;
          }
          stopHeartbeat();
          setSocketStatus("disconnected");
        };
      } catch (error) {
        if (!active) {
          return;
        }
        setSocketStatus("disconnected");
        setMessage((error as Error).message);
      }
    })();

    return () => {
      active = false;
      if (socketRef.current === currentSocket) {
        closeSocket();
      } else if (
        currentSocket &&
        (currentSocket.readyState === WebSocket.OPEN || currentSocket.readyState === WebSocket.CONNECTING)
      ) {
        currentSocket.close();
      }
    };
  }, [auth.token, navigate, room?.is_joined, roomId]);

  useEffect(() => {
    return () => {
      clearRedirectTimer();
      closeSocket();
    };
  }, []);

  if (isLoading && !room) {
    return (
      <section className="panel">
        <h2>Room Detail</h2>
        <p className="feedback">{message || "Loading room detail..."}</p>
      </section>
    );
  }

  if (!room) {
    return (
      <section className="panel">
        <h2>Room Detail</h2>
        <p className="feedback error">{message || "Room not found or already closed."}</p>
      </section>
    );
  }

  const socketStatusClass =
    socketStatus === "connected" ? "status-authenticated" : socketStatus === "connecting" ? "status-waiting" : "status-guest";
  const socketStatusText =
    socketStatus === "connected" ? "Realtime on" : socketStatus === "connecting" ? "Connecting" : "Offline";

  return (
    <section className="page-stack room-stage">
      <div className="detail-banner">
        <div className="hero-copy">
          <span className="eyebrow">Room Detail</span>
          <h2>{room.game_name}</h2>
          <p>
            Owner {room.owner_nickname} | Players {room.current_player}/{room.max_player} | Online {onlineCount} | Status{" "}
            {room.status === "ready" ? "Ready" : "Waiting"}
          </p>
        </div>

        <div className="stats-grid">
          <article className="metric-card">
            <span className="muted">Type</span>
            <strong>{room.type}</strong>
            <small>{room.start_time ? `Start time: ${room.start_time}` : "Instant room"}</small>
          </article>
          <article className="metric-card">
            <span className="muted">Role</span>
            <strong>{room.is_owner ? "Owner" : room.is_joined ? "Member" : "Visitor"}</strong>
            <small>Only members can read full history and join realtime chat.</small>
          </article>
          <article className="metric-card">
            <span className="muted">Online</span>
            <strong>{onlineCount}</strong>
            <small>Total members {room.members.length}</small>
          </article>
        </div>

        <div className="detail-actions">
          <button className="ghost-button" onClick={() => void loadAll()}>
            Refresh
          </button>
          {!room.is_joined ? <button onClick={() => void handleJoin()}>Join Room</button> : null}
          {room.is_joined ? (
            <button className="ghost-button" onClick={() => void handleLeave()}>
              Leave Room
            </button>
          ) : null}
          {room.is_owner ? (
            <button className="danger-button" onClick={() => void handleDissolve()}>
              Dissolve Room
            </button>
          ) : null}
        </div>
      </div>

      <div className="split-layout">
        <div className="sub-panel">
          <div className="section-head">
            <div>
              <h3>Members</h3>
              <p>Current room members are listed here.</p>
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
              <h3>Chat</h3>
              <p>History loads over HTTP with cursors, and new messages arrive through WebSocket events.</p>
            </div>
            <span className={`pill ${socketStatusClass}`}>{socketStatusText}</span>
          </div>

          {!room.is_joined ? (
            <div className="empty-state">Join the room to browse history and enter realtime chat.</div>
          ) : (
            <>
              {nextCursor !== null ? (
                <div className="inline-actions">
                  <button className="ghost-button" onClick={() => void handleLoadEarlier()} disabled={isLoadingEarlier}>
                    {isLoadingEarlier ? "Loading..." : "Load Earlier"}
                  </button>
                  <span className="feedback">Online now: {onlineCount}</span>
                </div>
              ) : (
                <p className="feedback">Online now: {onlineCount}</p>
              )}

              {messages.length === 0 ? (
                <div className="empty-state">No messages yet.</div>
              ) : (
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
              )}

              <form className="chat-compose" onSubmit={handleSend}>
                <textarea
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  placeholder="Type a message..."
                />
                <div className="inline-actions">
                  <span className="feedback">
                    {socketStatus === "connected"
                      ? "Messages are being sent over the realtime channel."
                      : "Wait for the realtime channel to reconnect before sending."}
                  </span>
                  <button type="submit" disabled={isSending || socketStatus !== "connected" || !content.trim()}>
                    {isSending ? "Sending..." : "Send"}
                  </button>
                </div>
              </form>
            </>
          )}
        </div>
      </div>

      {message ? <p className="feedback error">{message}</p> : null}
    </section>
  );
}
