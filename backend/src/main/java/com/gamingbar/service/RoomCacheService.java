package com.gamingbar.service;

import com.gamingbar.vo.room.RoomListItemVo;
import java.util.List;

public interface RoomCacheService {

    List<RoomListItemVo> getHotRooms();

    void cacheHotRooms(List<RoomListItemVo> rooms);

    void evictHotRooms();

    int getOnlineCount(Long roomId);

    void setOnlineCount(Long roomId, int count);
}
