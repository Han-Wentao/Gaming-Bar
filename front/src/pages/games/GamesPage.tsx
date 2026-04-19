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
          <span className="eyebrow">游戏列表</span>
          <h2>可用游戏</h2>
          <p>创建房间时会使用这里的游戏数据，请以接口返回结果为准。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">游戏总数</span>
            <strong>{games.length}</strong>
            <small>用于创建房间</small>
          </article>
          <article className="metric-card">
            <span className="muted">可用游戏</span>
            <strong>{games.filter((game) => game.status === "enabled").length}</strong>
            <small>当前可正常选择</small>
          </article>
          <article className="metric-card">
            <span className="muted">数据来源</span>
            <strong>接口</strong>
            <small>实时读取当前配置</small>
          </article>
        </div>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>游戏目录</h3>
            <p>你可以先在这里查看支持的游戏，再前往创建房间页面发起组局。</p>
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
                  {game.status === "enabled" ? "可用" : game.status}
                </span>
              </div>

              <div className="meta-grid">
                <div className="meta-row">
                  <span>游戏 ID</span>
                  <span className="meta-strong">{game.id}</span>
                </div>
                <div className="meta-row">
                  <span>状态</span>
                  <span className="meta-strong">{game.status === "enabled" ? "可用" : game.status}</span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}
