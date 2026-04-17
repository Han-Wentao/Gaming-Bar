import { Navigate, createBrowserRouter } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { LoginPage } from "../pages/auth/LoginPage";
import { GamesPage } from "../pages/games/GamesPage";
import { MyRoomsPage } from "../pages/my/MyRoomsPage";
import { ProfilePage } from "../pages/profile/ProfilePage";
import { CreateRoomPage } from "../pages/rooms/create/CreateRoomPage";
import { RoomDetailPage } from "../pages/rooms/detail/RoomDetailPage";
import { RoomsPage } from "../pages/rooms/RoomsPage";
import { useAuthState } from "../store/auth-store";

function AuthGuard() {
  const auth = useAuthState();
  return auth.token ? <AppLayout /> : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />
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
]);
