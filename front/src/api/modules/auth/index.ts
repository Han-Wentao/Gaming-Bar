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
  expires_in: number;
  user: User;
}

export function sendSms(payload: SendSmsPayload) {
  return client.post<{ expires_in: number }>("/auth/sms/send", payload);
}

export function login(payload: LoginPayload) {
  return client.post<LoginResponse>("/auth/login", payload);
}

export function getMe() {
  return client.get<User>("/auth/me");
}
