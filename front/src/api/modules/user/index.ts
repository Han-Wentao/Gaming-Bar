import client from "../../client";
import type { User } from "../../../types/models";

export interface UpdateProfilePayload {
  nickname?: string;
  avatar?: string;
}

export function updateProfile(payload: UpdateProfilePayload) {
  return client.put<User>("/users/profile", payload);
}
