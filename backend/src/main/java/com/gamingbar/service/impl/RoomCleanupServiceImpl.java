package com.gamingbar.service.impl;

import com.gamingbar.common.util.TimeUtils;
import com.gamingbar.entity.Room;
import com.gamingbar.mapper.MessageMapper;
import com.gamingbar.mapper.RoomMapper;
import com.gamingbar.mapper.RoomUserMapper;
import com.gamingbar.service.RoomCacheService;
import com.gamingbar.service.RoomCleanupService;
import com.gamingbar.service.RoomRealtimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomCleanupServiceImpl implements RoomCleanupService {

    private final RoomMapper roomMapper;
    private final RoomUserMapper roomUserMapper;
    private final MessageMapper messageMapper;
    private final RoomCacheService roomCacheService;
    private final RoomRealtimeService roomRealtimeService;

    public RoomCleanupServiceImpl(RoomMapper roomMapper,
                                  RoomUserMapper roomUserMapper,
                                  MessageMapper messageMapper,
                                  RoomCacheService roomCacheService,
                                  RoomRealtimeService roomRealtimeService) {
        this.roomMapper = roomMapper;
        this.roomUserMapper = roomUserMapper;
        this.messageMapper = messageMapper;
        this.roomCacheService = roomCacheService;
        this.roomRealtimeService = roomRealtimeService;
    }

    @Override
    public boolean isExpired(Room room) {
        if (room == null || "closed".equals(room.getStatus())) {
            return false;
        }
        if ("instant".equals(room.getType())) {
            return room.getCreateTime().plusHours(2).isBefore(TimeUtils.now());
        }
        return room.getStartTime() != null && room.getStartTime().plusHours(1).isBefore(TimeUtils.now());
    }

    @Override
    @Transactional
    public boolean cleanupIfExpired(Long roomId) {
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null || "closed".equals(room.getStatus()) || !isExpired(room)) {
            return false;
        }
        doCloseRoom(roomId);
        return true;
    }

    @Override
    @Transactional
    public void closeRoom(Long roomId) {
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null || "closed".equals(room.getStatus())) {
            return;
        }
        doCloseRoom(roomId);
    }

    private void doCloseRoom(Long roomId) {
        roomMapper.closeRoom(roomId, "closed", 0);
        roomUserMapper.deleteByRoomId(roomId);
        messageMapper.deleteByRoomId(roomId);
        roomCacheService.evictHotRooms();
        roomCacheService.setOnlineCount(roomId, 0);
        roomRealtimeService.closeRoomSessions(roomId, "room_closed");
    }
}
