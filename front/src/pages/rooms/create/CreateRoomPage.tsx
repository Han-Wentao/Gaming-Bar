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
          <span className="eyebrow">Create Session</span>
          <h2>创建房间</h2>
          <p>把玩法、人数和开局方式收拢成一套更像“场景编排器”的录入体验。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">游戏来源</span>
            <strong>{games.length}</strong>
            <small>接口返回可选项</small>
          </article>
          <article className="metric-card">
            <span className="muted">默认人数</span>
            <strong>{maxPlayer}</strong>
            <small>可在 2 到 10 间调整</small>
          </article>
          <article className="metric-card">
            <span className="muted">开局方式</span>
            <strong>{type}</strong>
            <small>即时或预约</small>
          </article>
        </div>
      </div>

      <section className="profile-layout">
        <article className="panel">
          <div className="section-head">
            <div>
              <h3>房间配置</h3>
              <p>接口约束已经内置在表单逻辑里，`instant` 不会提交 `start_time`。</p>
            </div>
          </div>

          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span className="label-caption">选择游戏</span>
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
            <span className="eyebrow">Builder Notes</span>
            <h3>创建策略</h3>
            <p>这个侧栏不只用来说明，而是帮用户在提交前确认自己在创建什么样的局。</p>
          </div>

          <ul className="simple-list">
            <li>
              <strong>即时房间</strong>
              <span>适合马上开局，省掉时间输入，动作更直接。</span>
            </li>
            <li>
              <strong>预约房间</strong>
              <span>适合提前约人，保留时间字段，方便组织节奏。</span>
            </li>
            <li>
              <strong>人数上限</strong>
              <span>人数越小越容易凑齐，人数越大越适合社交型场景。</span>
            </li>
          </ul>
        </aside>
      </section>
    </section>
  );
}
