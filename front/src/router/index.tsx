import { useEffect } from "react";
import { Navigate, Outlet, createBrowserRouter } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { LoginPage } from "../pages/auth/LoginPage";
import { GamesPage } from "../pages/games/GamesPage";
import { MyRoomsPage } from "../pages/my/MyRoomsPage";
import { ProfilePage } from "../pages/profile/ProfilePage";
import { CreateRoomPage } from "../pages/rooms/create/CreateRoomPage";
import { RoomDetailPage } from "../pages/rooms/detail/RoomDetailPage";
import { RoomsPage } from "../pages/rooms/RoomsPage";
import { bootstrapAuth, useAuthState } from "../store/auth-store";

function FullPageState({ message }: { message: string }) {
  return (
    <div className="auth-page">
      <section className="panel auth-panel">
        <p className="feedback">{message}</p>
      </section>
    </div>
  );
}

function AuthBootstrapOutlet() {
  const auth = useAuthState();

  useEffect(() => {
    void bootstrapAuth();
  }, []);

  if (auth.status === "unknown") {
    return <FullPageState message="正在验证登录状态..." />;
  }

  return <Outlet />;
}

function AuthGuard() {
  const auth = useAuthState();

  if (auth.status === "unknown") {
    return <FullPageState message="正在验证登录状态..." />;
  }

  return auth.status === "authenticated" ? <AppLayout /> : <Navigate to="/login" replace />;
}

function LoginGuard() {
  const auth = useAuthState();

  if (auth.status === "unknown") {
    return <FullPageState message="正在验证登录状态..." />;
  }

  return auth.status === "authenticated" ? <Navigate to="/rooms" replace /> : <LoginPage />;
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AuthBootstrapOutlet />,
    children: [
      {
        path: "/login",
        element: <LoginGuard />
      },
      {
        path: "/",
        element: <AuthGuard />,
        children: [
          {
            index: true,
            element: <Navigate to="/rooms" replace />
          },
          {
            path: "rooms",
            element: <RoomsPage />
          },
          {
            path: "rooms/create",
            element: <CreateRoomPage />
          },
          {
            path: "rooms/:roomId",
            element: <RoomDetailPage />
          },
          {
            path: "rooms/my",
            element: <MyRoomsPage />
          },
          {
            path: "games",
            element: <GamesPage />
          },
          {
            path: "profile",
            element: <ProfilePage />
          }
        ]
      }
    ]
  }
]);
