import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listGames } from "../../../api/modules/game";
import { createRoom } from "../../../api/modules/room";
import type { Game } from "../../../types/models";

export function CreateRoomPage() {
  const navigate = useNavigate();
  const [games, setGames] = useState<Game[]>([]);
  const [gameId, setGameId] = useState(1);
  const [maxPlayer, setMaxPlayer] = useState(4);
  const [type, setType] = useState<"instant" | "scheduled">("instant");
  const [startTime, setStartTime] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    listGames()
      .then((result) => {
        setGames(result);
        if (result.length > 0) {
          setGameId(result[0].id);
        }
      })
      .catch((error: Error) => setMessage(error.message));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const room = await createRoom({
        game_id: gameId,
        max_player: maxPlayer,
        type,
        start_time: type === "scheduled" ? startTime : null
      });
      navigate(`/rooms/${room.id}`);
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h2>创建房间</h2>
          <p>按接口文档要求，`instant` 不能提交 `start_time`，`scheduled` 必须提交。</p>
        </div>
      </div>
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>游戏</span>
          <select value={gameId} onChange={(event) => setGameId(Number(event.target.value))}>
            {games.map((game) => (
              <option key={game.id} value={game.id}>
                {game.game_name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>最大人数</span>
          <input
            type="number"
            min={2}
            max={10}
            value={maxPlayer}
            onChange={(event) => setMaxPlayer(Number(event.target.value))}
          />
        </label>
        <label>
          <span>类型</span>
          <select value={type} onChange={(event) => setType(event.target.value as "instant" | "scheduled")}>
            <option value="instant">instant</option>
            <option value="scheduled">scheduled</option>
          </select>
        </label>
        {type === "scheduled" ? (
          <label>
            <span>开始时间</span>
            <input
              value={startTime}
              onChange={(event) => setStartTime(event.target.value)}
              placeholder="yyyy-MM-dd HH:mm:ss"
            />
          </label>
        ) : null}
        <div className="inline-actions">
          <button type="submit">创建</button>
        </div>
      </form>
      {message ? <p className="feedback error">{message}</p> : null}
    </section>
  );
}
