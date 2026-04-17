import client from "../../client";
import type { Game } from "../../../types/models";

export function listGames() {
  return client.get<Game[]>("/games");
}
