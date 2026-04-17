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
          <strong>GamingBar</strong>
          <span>约玩联调台</span>
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
        <button
          className="ghost-button"
          onClick={() => {
            clearAuthState();
            navigate("/login");
          }}
        >
          退出登录
        </button>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <h1>GamingBar 控制台</h1>
            <p>对照接口文档进行登录、房间、聊天和资料联调。</p>
          </div>
          <div className="user-chip">
            <span>{auth.user?.nickname ?? "未登录"}</span>
            <small>{auth.user?.phone ?? ""}</small>
          </div>
        </header>
        <Outlet />
      </main>
    </div>
  );
}
