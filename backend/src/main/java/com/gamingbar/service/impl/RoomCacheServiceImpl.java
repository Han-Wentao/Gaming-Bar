package com.gamingbar.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamingbar.cache.CacheService;
import com.gamingbar.service.RoomCacheService;
import com.gamingbar.vo.room.RoomListItemVo;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RoomCacheServiceImpl implements RoomCacheService {

    private static final String HOT_ROOM_KEY = "room:hot:list";
    private static final String ONLINE_ROOM_PREFIX = "room:online:";

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final Duration hotRoomTtl;
    private final Duration onlineRoomTtl;

    public RoomCacheServiceImpl(CacheService cacheService,
                                ObjectMapper objectMapper,
                                @Value("${app.room.hot-cache-seconds}") long hotCacheSeconds,
                                @Value("${app.room.online-cache-seconds}") long onlineCacheSeconds) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.hotRoomTtl = Duration.ofSeconds(hotCacheSeconds);
        this.onlineRoomTtl = Duration.ofSeconds(onlineCacheSeconds);
    }

    @Override
    public List<RoomListItemVo> getHotRooms() {
        String value = cacheService.get(HOT_ROOM_KEY);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            cacheService.delete(HOT_ROOM_KEY);
            return List.of();
        }
    }

    @Override
    public void cacheHotRooms(List<RoomListItemVo> rooms) {
        try {
            cacheService.set(HOT_ROOM_KEY, objectMapper.writeValueAsString(rooms), hotRoomTtl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize hot room cache", exception);
        }
    }

    @Override
    public void evictHotRooms() {
        cacheService.delete(HOT_ROOM_KEY);
    }

    @Override
    public int getOnlineCount(Long roomId) {
        String value = cacheService.get(ONLINE_ROOM_PREFIX + roomId);
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    @Override
    public void setOnlineCount(Long roomId, int count) {
        cacheService.set(ONLINE_ROOM_PREFIX + roomId, String.valueOf(Math.max(count, 0)), onlineRoomTtl);
    }
}
