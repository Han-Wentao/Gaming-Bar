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
    <section className="page-stack">
      <div className="page-hero hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">创建房间</span>
          <h2>发起新的组队</h2>
          <p>选择游戏、人数和开局方式后即可创建房间，并进入详情页面。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">可选游戏</span>
            <strong>{games.length}</strong>
            <small>来自当前接口数据</small>
          </article>
          <article className="metric-card">
            <span className="muted">默认人数</span>
            <strong>{maxPlayer}</strong>
            <small>可在 2 到 10 之间调整</small>
          </article>
          <article className="metric-card">
            <span className="muted">开局方式</span>
            <strong>{type}</strong>
            <small>{type === "instant" ? "即时开局" : "预约开局"}</small>
          </article>
        </div>
      </div>

      <section className="profile-layout">
        <article className="panel">
          <div className="section-head">
            <div>
              <h3>房间设置</h3>
              <p>即时房间无需填写开始时间，预约房间则需要填写计划开始时间。</p>
            </div>
          </div>

          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span className="label-caption">游戏</span>
              <select value={gameId} onChange={(event) => setGameId(Number(event.target.value))}>
                {games.map((game) => (
                  <option key={game.id} value={game.id}>
                    {game.game_name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span className="label-caption">最大人数</span>
              <input
                type="number"
                min={2}
                max={10}
                value={maxPlayer}
                onChange={(event) => setMaxPlayer(Number(event.target.value))}
              />
            </label>

            <label>
              <span className="label-caption">房间类型</span>
              <select value={type} onChange={(event) => setType(event.target.value as "instant" | "scheduled")}>
                <option value="instant">instant</option>
                <option value="scheduled">scheduled</option>
              </select>
            </label>

            {type === "scheduled" ? (
              <label>
                <span className="label-caption">开始时间</span>
                <input
                  value={startTime}
                  onChange={(event) => setStartTime(event.target.value)}
                  placeholder="yyyy-MM-dd HH:mm:ss"
                />
              </label>
            ) : null}

            <div className="inline-actions">
              <button type="submit">确认创建</button>
            </div>
          </form>

          {message ? <p className="feedback error">{message}</p> : null}
        </article>

        <aside className="panel">
          <div className="hero-copy">
            <span className="eyebrow">创建提示</span>
            <h3>使用建议</h3>
          </div>

          <ul className="simple-list">
            <li>
              <strong>即时房间</strong>
              <span>适合马上开局，不需要额外填写时间。</span>
            </li>
            <li>
              <strong>预约房间</strong>
              <span>适合提前约人，便于成员确认开始时间。</span>
            </li>
            <li>
              <strong>人数设置</strong>
              <span>请根据你的组队规模调整房间人数上限。</span>
            </li>
          </ul>
        </aside>
      </section>
    </section>
  );
}
