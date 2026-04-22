import client from "../../client";
import type { User } from "../../../types/models";

export interface SendSmsPayload {
  phone: string;
}

export interface LoginPayload {
  phone: string;
  code: string;
}

export interface LoginResponse {
  token: string;
  refresh_token: string;
  expires_in: number;
  user: User;
}

export interface RefreshTokenPayload {
  refresh_token: string;
}

export interface LogoutPayload {
  refresh_token?: string;
}

export interface WsTicketResponse {
  ticket: string;
  expires_in: number;
}

export function sendSms(payload: SendSmsPayload) {
  return client.post<{ expires_in: number }>("/auth/sms/send", payload);
}

export function login(payload: LoginPayload) {
  return client.post<LoginResponse>("/auth/login", payload);
}

export function refreshToken(payload: RefreshTokenPayload) {
  return client.post<LoginResponse>("/auth/refresh", payload);
}

export function logout(payload?: LogoutPayload) {
  return client.post<null>("/auth/logout", payload);
}

export function createWsTicket(roomId: number | string) {
  return client.post<WsTicketResponse>(`/auth/ws-ticket?roomId=${encodeURIComponent(String(roomId))}`);
}

export function getMe() {
  return client.get<User>("/auth/me");
}
