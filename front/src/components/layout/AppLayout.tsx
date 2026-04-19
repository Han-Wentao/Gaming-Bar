import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { clearAuthState, useAuthState } from "../../store/auth-store";

const links = [
  { to: "/rooms", label: "房间" },
  { to: "/rooms/create", label: "创建" },
  { to: "/rooms/my", label: "我的" },
  { to: "/games", label: "游戏" },
  { to: "/profile", label: "资料" }
];

export function AppLayout() {
  const auth = useAuthState();
  const navigate = useNavigate();

  return (
    <div className="app-shell">
      <div className="promo-bar">短信登录、房间组队、实时聊天，全部已接入当前版本。</div>

      <header className="top-nav">
        <div className="nav-brand">
          <div className="brand-mark">GB</div>
          <div className="brand-copy">
            <strong>GamingBar</strong>
            <small>Join The Room</small>
          </div>
        </div>

        <nav className="nav nav-horizontal">
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

        <div className="nav-actions">
          <div className="user-chip">
            <small>{auth.status === "authenticated" ? "已登录" : "未登录"}</small>
            <strong>{auth.user?.nickname ?? "游客"}</strong>
          </div>
          <button
            className="ghost-button"
            onClick={() => {
              clearAuthState();
              navigate("/login");
            }}
          >
            退出
          </button>
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
