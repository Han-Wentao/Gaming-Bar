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
    <section className="page-stack">
      <div className="page-hero hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">Game Dictionary</span>
          <h2>游戏字典</h2>
          <p>这里更像系统素材库。你创建房间时看到的游戏列表，最终都以这里的接口结果为准。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">总游戏数</span>
            <strong>{games.length}</strong>
            <small>用于建房选择</small>
          </article>
          <article className="metric-card">
            <span className="muted">可用状态</span>
            <strong>{games.filter((game) => game.status === "enabled").length}</strong>
            <small>通常可直接开放</small>
          </article>
          <article className="metric-card">
            <span className="muted">数据来源</span>
            <strong>API</strong>
            <small>前端只做展示</small>
          </article>
        </div>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>可选游戏列表</h3>
            <p>给每个条目单独做成素材卡，更像后台字典面板，而不是普通纯文本列表。</p>
          </div>
        </div>

        {error ? <p className="feedback error">{error}</p> : null}

        <div className="card-grid">
          {games.map((game) => (
            <article key={game.id} className="card stagger-in">
              <div className="card-topline">
                <div className="card-title">
                  <span className="card-kicker">GAME #{game.id}</span>
                  <strong>{game.game_name}</strong>
                </div>
                <span className={`status-badge ${game.status === "enabled" ? "status-enabled" : "status-guest"}`}>
                  {game.status}
                </span>
              </div>

              <div className="meta-grid">
                <div className="meta-row">
                  <span>展示 ID</span>
                  <span className="meta-strong">{game.id}</span>
                </div>
                <div className="meta-row">
                  <span>接口状态</span>
                  <span className="meta-strong">{game.status}</span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}
