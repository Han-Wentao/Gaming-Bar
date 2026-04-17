import { useEffect, useState } from "react";
import { listGames } from "../../api/modules/game";
import type { Game } from "../../types/models";

export function GamesPage() {
  const [games, setGames] = useState<Game[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    listGames().then(setGames).catch((reason: Error) => setError(reason.message));
  }, []);

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h2>游戏字典</h2>
          <p>创建房间前端应以接口返回的游戏 ID 为准。</p>
        </div>
      </div>
      {error ? <p className="feedback error">{error}</p> : null}
      <div className="card-grid">
        {games.map((game) => (
          <article key={game.id} className="card">
            <strong>{game.game_name}</strong>
            <span>ID: {game.id}</span>
            <span>状态: {game.status}</span>
          </article>
        ))}
      </div>
    </section>
  );
}
