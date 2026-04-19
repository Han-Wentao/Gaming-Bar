import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { clearAuthState, useAuthState } from "../../store/auth-store";

const links = [
  { to: "/rooms", label: "房间列表" },
  { to: "/rooms/create", label: "创建房间" },
  { to: "/rooms/my", label: "我的房间" },
  { to: "/games", label: "游戏字典" },
  { to: "/profile", label: "个人资料" }
];

export function AppLayout() {
  const auth = useAuthState();
  const navigate = useNavigate();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">GB</div>
          <strong>GamingBar</strong>
          <span>组局、沟通、进房，一套节奏连起来。</span>
          <small>Play faster. Organize cleaner.</small>
        </div>

        <div className="sidebar-note">
          <strong>Live Lobby</strong>
          <p>让房间、成员、聊天和个人状态在同一个视觉语境里工作。</p>
        </div>

        <nav className="nav">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-actions">
          <button
            className="ghost-button"
            onClick={() => {
              clearAuthState();
              navigate("/login");
            }}
          >
            退出登录
          </button>
        </div>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <span className="eyebrow">Matchmaking Dashboard</span>
            <h1>GamingBar 控制台</h1>
            <p>现在的界面更强调状态、路径和操作反馈，而不是单纯堆表单。</p>
          </div>

          <div className="user-chip">
            <span className={`status-badge ${auth.status === "authenticated" ? "status-authenticated" : "status-guest"}`}>
              {auth.status === "authenticated" ? "已认证" : "未登录"}
            </span>
            <strong>{auth.user?.nickname ?? "访客"}</strong>
            <small>{auth.user?.phone ?? ""}</small>
          </div>
        </header>

        <Outlet />
      </main>
    </div>
  );
}
