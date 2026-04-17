export interface User {
  id: number;
  phone: string;
  nickname: string;
  avatar: string;
  credit_score: number;
}

export interface Game {
  id: number;
  game_name: string;
  status: string;
}

export interface RoomMember {
  user_id: number;
  nickname: string;
  avatar: string;
  join_time: string;
}

export interface RoomDetail {
  id: number;
  game_id: number;
  game_name: string;
  owner_id: number;
  owner_nickname: string;
  max_player: number;
  current_player: number;
  type: string;
  start_time: string | null;
  status: string;
  create_time: string;
  update_time: string;
  is_owner: boolean;
  is_joined: boolean;
  members: RoomMember[];
}

export interface RoomListItem {
  id: number;
  game_id: number;
  game_name: string;
  owner_id: number;
  owner_nickname: string;
  max_player: number;
  current_player: number;
  type: string;
  start_time: string | null;
  status: string;
  create_time: string;
  is_joined: boolean;
}

export interface RoomMyItem {
  id: number;
  game_id: number;
  game_name: string;
  owner_id: number;
  owner_nickname: string;
  max_player: number;
  current_player: number;
  type: string;
  start_time: string | null;
  status: string;
  create_time: string;
  is_owner: boolean;
}

export interface Message {
  id: number;
  room_id: number;
  user_id: number;
  nickname: string;
  avatar: string;
  content: string;
  create_time: string;
}

export interface MessagePageResponse {
  has_more: boolean;
  messages: Message[];
}
