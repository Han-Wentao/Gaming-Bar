package com.gamingbar.service;

import com.gamingbar.entity.Room;

public interface RoomCleanupService {

    boolean isExpired(Room room);

    boolean cleanupIfExpired(Long roomId);

    void closeRoom(Long roomId);
}
