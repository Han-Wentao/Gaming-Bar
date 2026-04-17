import client from "../../client";
import type { PageData } from "../../../types/api";
import type { RoomDetail, RoomListItem, RoomMyItem } from "../../../types/models";

export interface CreateRoomPayload {
  game_id: number;
  max_player: number;
  type: "instant" | "scheduled";
  start_time?: string | null;
}

export interface RoomListQuery {
  game_id?: number;
  type?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface MyRoomQuery {
  status?: string;
  page?: number;
  size?: number;
}

export interface LeaveRoomResponse {
  action: "left" | "room_closed";
}

export function createRoom(payload: CreateRoomPayload) {
  return client.post<RoomDetail>("/rooms", payload);
}

export function listRooms(params: RoomListQuery) {
  return client.get<PageData<RoomListItem>>("/rooms", { params });
}

export function getRoomDetail(roomId: string) {
  return client.get<RoomDetail>(`/rooms/${roomId}`);
}

export function joinRoom(roomId: string) {
  return client.post<RoomDetail>(`/rooms/${roomId}/join`);
}

export function leaveRoom(roomId: string) {
  return client.post<LeaveRoomResponse>(`/rooms/${roomId}/leave`);
}

export function dissolveRoom(roomId: string) {
  return client.delete<null>(`/rooms/${roomId}`);
}

export function listMyRooms(params: MyRoomQuery) {
  return client.get<PageData<RoomMyItem>>("/rooms/my", { params });
}
