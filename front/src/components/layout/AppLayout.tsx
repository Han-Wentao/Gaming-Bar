import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { logout } from "../../api/modules/auth";
import { clearAuthState, useAuthState } from "../../store/auth-store";

const links = [
  { to: "/rooms", label: "Rooms" },
  { to: "/rooms/create", label: "Create" },
  { to: "/rooms/my", label: "My Rooms" },
  { to: "/games", label: "Games" },
  { to: "/profile", label: "Profile" }
];

export function AppLayout() {
  const auth = useAuthState();
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await logout(auth.refreshToken ? { refresh_token: auth.refreshToken } : undefined);
    } catch {
      // Ignore logout API errors and clear local state anyway.
    } finally {
      clearAuthState();
      navigate("/login");
    }
  }

  return (
    <div className="app-shell">
      <div className="promo-bar">SMS login, room matchmaking, and realtime chat are now wired into the latest backend.</div>

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
            <small>{auth.status === "authenticated" ? "Logged in" : "Guest"}</small>
            <strong>{auth.user?.nickname ?? "Visitor"}</strong>
          </div>
          <button className="ghost-button" onClick={() => void handleLogout()}>
            Log out
          </button>
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
