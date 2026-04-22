import client from "../../client";
import type { Message, MessagePageResponse } from "../../../types/models";

export function sendMessage(roomId: string, content: string) {
  return client.post<Message>(`/rooms/${roomId}/messages`, { content });
}

export function listMessages(roomId: string, cursor?: number, size = 50) {
  return client.get<MessagePageResponse>(`/rooms/${roomId}/messages`, {
    params: {
      cursor,
      size
    }
  });
}
