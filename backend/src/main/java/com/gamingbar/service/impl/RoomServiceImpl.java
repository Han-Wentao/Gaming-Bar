package com.gamingbar.service.impl;

import com.gamingbar.common.enums.ErrorCode;
import com.gamingbar.common.exception.BusinessException;
import com.gamingbar.common.result.PageData;
import com.gamingbar.common.util.TimeUtils;
import com.gamingbar.common.util.ValidationUtils;
import com.gamingbar.dto.room.CreateRoomRequest;
import com.gamingbar.entity.Game;
import com.gamingbar.entity.Room;
import com.gamingbar.entity.RoomUser;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.GameMapper;
import com.gamingbar.mapper.RoomMapper;
import com.gamingbar.mapper.RoomUserMapper;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.RoomCacheService;
import com.gamingbar.service.RoomCleanupService;
import com.gamingbar.service.RoomRealtimeService;
import com.gamingbar.service.RoomService;
import com.gamingbar.vo.room.LeaveRoomResponseVo;
import com.gamingbar.vo.room.RoomDetailVo;
import com.gamingbar.vo.room.RoomListItemVo;
import com.gamingbar.vo.room.RoomMemberVo;
import com.gamingbar.vo.room.RoomMyItemVo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final RoomUserMapper roomUserMapper;
    private final UserMapper userMapper;
    private final GameMapper gameMapper;
    private final RoomCleanupService roomCleanupService;
    private final RoomCacheService roomCacheService;
    private final RoomRealtimeService roomRealtimeService;

    public RoomServiceImpl(RoomMapper roomMapper,
                           RoomUserMapper roomUserMapper,
                           UserMapper userMapper,
                           GameMapper gameMapper,
                           RoomCleanupService roomCleanupService,
                           RoomCacheService roomCacheService,
                           RoomRealtimeService roomRealtimeService) {
        this.roomMapper = roomMapper;
        this.roomUserMapper = roomUserMapper;
        this.userMapper = userMapper;
        this.gameMapper = gameMapper;
        this.roomCleanupService = roomCleanupService;
        this.roomCacheService = roomCacheService;
        this.roomRealtimeService = roomRealtimeService;
    }

    @Override
    @Transactional
    public RoomDetailVo createRoom(Long userId, CreateRoomRequest request) {
        validateCreateRequest(request);
        Game game = gameMapper.selectById(request.getGameId());
        ValidationUtils.badRequest(game != null && "enabled".equals(game.getStatus()));

        userMapper.selectByIdForUpdate(userId);
        cleanupUserExpiredRooms(userId);
        ValidationUtils.require(findValidUserRooms(userId).isEmpty(), ErrorCode.CONFLICT.getCode(), "您已在其他未关闭房间中");

        Room room = new Room();
        room.setGameId(request.getGameId());
        room.setOwnerId(userId);
        room.setMaxPlayer(request.getMaxPlayer());
        room.setCurrentPlayer(1);
        room.setType(request.getType());
        room.setStartTime(parseStartTime(request));
        room.setStatus("waiting");
        room.setVersion(0L);
        roomMapper.insert(room);

        RoomUser roomUser = new RoomUser();
        roomUser.setRoomId(room.getId());
        roomUser.setUserId(userId);
        roomUserMapper.insert(roomUser);

        roomCacheService.evictHotRooms();
        log.info("Room created, roomId={}, ownerId={}", room.getId(), userId);
        return loadRoomDetail(userId, room.getId());
    }

    @Override
    public PageData<RoomListItemVo> listRooms(Long userId, Integer gameId, String type, String status, Integer page, Integer size) {
        validateListFilters(gameId, type, status, page, size, false);
        int pageValue = page == null ? 1 : page;
        int sizeValue = size == null ? 20 : size;
        int offset = (pageValue - 1) * sizeValue;

        List<Room> rooms = roomMapper.selectPage(gameId, type, status, offset, sizeValue);
        cleanupExpiredRooms(rooms);
        long total = roomMapper.countPage(gameId, type, status);
        Set<Long> joinedRoomIds = findValidUserRooms(userId).stream().map(Room::getId).collect(Collectors.toSet());

        return new PageData<>(total, pageValue, sizeValue, toRoomListItemVos(rooms, joinedRoomIds));
    }

    @Override
    public List<RoomListItemVo> listHotRooms(Long userId, Integer limit) {
        int safeLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 20));
        Set<Long> joinedRoomIds = findValidUserRooms(userId).stream().map(Room::getId).collect(Collectors.toSet());

        List<RoomListItemVo> cached = roomCacheService.getHotRooms();
        if (!cached.isEmpty()) {
            return cached.stream()
                .limit(safeLimit)
                .map(item -> new RoomListItemVo(
                    item.getId(),
                    item.getGameId(),
                    item.getGameName(),
                    item.getOwnerId(),
                    item.getOwnerNickname(),
                    item.getMaxPlayer(),
                    item.getCurrentPlayer(),
                    item.getType(),
                    item.getStartTime(),
                    item.getStatus(),
                    item.getCreateTime(),
                    joinedRoomIds.contains(item.getId())
                ))
                .toList();
        }

        List<Room> hotRooms = roomMapper.selectHotRooms(safeLimit);
        List<RoomListItemVo> snapshots = toRoomListItemVos(hotRooms, Set.of());
        roomCacheService.cacheHotRooms(snapshots);
        return snapshots.stream()
            .map(item -> new RoomListItemVo(
                item.getId(),
                item.getGameId(),
                item.getGameName(),
                item.getOwnerId(),
                item.getOwnerNickname(),
                item.getMaxPlayer(),
                item.getCurrentPlayer(),
                item.getType(),
                item.getStartTime(),
                item.getStatus(),
                item.getCreateTime(),
                joinedRoomIds.contains(item.getId())
            ))
            .toList();
    }

    @Override
    public RoomDetailVo getRoomDetail(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        return loadRoomDetail(userId, roomId);
    }

    @Override
    @Transactional
    public RoomDetailVo joinRoom(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        userMapper.selectByIdForUpdate(userId);
        cleanupUserExpiredRooms(userId);

        List<Room> userRooms = findValidUserRooms(userId);
        boolean inOtherRoom = userRooms.stream().anyMatch(room -> !room.getId().equals(roomId));
        ValidationUtils.require(!inOtherRoom, ErrorCode.CONFLICT.getCode(), "您已在其他未关闭房间中");
        if (roomUserMapper.selectByRoomIdAndUserId(roomId, userId) != null || userRooms.stream().anyMatch(item -> item.getId().equals(roomId))) {
            throw new BusinessException(ErrorCode.CONFLICT, "您已在该房间中");
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            Room room = roomMapper.selectById(roomId);
            ensureJoinableRoom(roomId, room);
            if (room.getCurrentPlayer() >= room.getMaxPlayer()) {
                throw new BusinessException(ErrorCode.CONFLICT, "房间已满，无法加入");
            }

            int nextPlayer = room.getCurrentPlayer() + 1;
            int updated = roomMapper.updatePlayerAndStatus(
                roomId,
                room.getVersion(),
                nextPlayer,
                nextPlayer >= room.getMaxPlayer() ? "ready" : "waiting"
            );
            if (updated == 0) {
                continue;
            }

            try {
                RoomUser roomUser = new RoomUser();
                roomUser.setRoomId(roomId);
                roomUser.setUserId(userId);
                roomUserMapper.insert(roomUser);
            } catch (DataIntegrityViolationException exception) {
                throw new BusinessException(ErrorCode.CONFLICT, "当前账号已在其他房间中");
            }

            roomCacheService.evictHotRooms();
            log.info("User joined room, roomId={}, userId={}, currentPlayer={}", roomId, userId, nextPlayer);
            return loadRoomDetail(userId, roomId);
        }

        throw new BusinessException(ErrorCode.CONFLICT, "房间状态已变化，请重试");
    }

    @Override
    @Transactional
    public LeaveRoomResponseVo leaveRoom(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        Room room = roomMapper.selectByIdForUpdate(roomId);
        ensureRoomExists(roomId, room);

        RoomUser roomUser = roomUserMapper.selectByRoomIdAndUserId(roomId, userId);
        if (roomUser == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您不在该房间中");
        }

        if (room.getOwnerId().equals(userId)) {
            roomCleanupService.closeRoom(roomId);
            return new LeaveRoomResponseVo("room_closed");
        }

        roomUserMapper.deleteByRoomIdAndUserId(roomId, userId);
        int currentPlayer = roomUserMapper.countByRoomId(roomId);
        roomMapper.updatePlayerAndStatus(
            roomId,
            room.getVersion(),
            currentPlayer,
            currentPlayer < room.getMaxPlayer() ? "waiting" : "ready"
        );
        roomRealtimeService.disconnectUserSessions(roomId, userId, "left_room");
        roomCacheService.evictHotRooms();
        log.info("User left room, roomId={}, userId={}", roomId, userId);
        return new LeaveRoomResponseVo("left");
    }

    @Override
    @Transactional
    public void dissolveRoom(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if ("closed".equals(room.getStatus())) {
            return;
        }
        if (!room.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅房主可解散房间");
        }
        roomCleanupService.closeRoom(roomId);
        log.info("Room dissolved, roomId={}, ownerId={}", roomId, userId);
    }

    @Override
    public PageData<RoomMyItemVo> listMyRooms(Long userId, String status, Integer page, Integer size) {
        validateListFilters(null, null, status, page, size, true);
        int pageValue = page == null ? 1 : page;
        int sizeValue = size == null ? 20 : size;
        int offset = (pageValue - 1) * sizeValue;

        cleanupUserExpiredRooms(userId);
        List<Room> rooms = roomMapper.selectMyPage(userId, status, offset, sizeValue);
        long total = roomMapper.countMyPage(userId, status);
        return new PageData<>(total, pageValue, sizeValue, toRoomMyItemVos(userId, rooms));
    }

    private void validateCreateRequest(CreateRoomRequest request) {
        ValidationUtils.badRequest(request.getGameId() != null);
        ValidationUtils.badRequest(request.getMaxPlayer() != null && request.getMaxPlayer() >= 2 && request.getMaxPlayer() <= 10);
        ValidationUtils.badRequest("instant".equals(request.getType()) || "scheduled".equals(request.getType()));

        if ("instant".equals(request.getType())) {
            ValidationUtils.badRequest(request.getStartTime() == null);
        } else {
            ValidationUtils.badRequest(request.getStartTime() != null);
            LocalDateTime startTime = parseRequiredDateTime(request.getStartTime());
            LocalDateTime now = TimeUtils.now();
            ValidationUtils.badRequest(startTime.isAfter(now) && !startTime.isAfter(now.plusDays(1)));
        }
    }

    private LocalDateTime parseStartTime(CreateRoomRequest request) {
        return request.getStartTime() == null ? null : parseRequiredDateTime(request.getStartTime());
    }

    private void validateListFilters(Integer gameId, String type, String status, Integer page, Integer size, boolean myRooms) {
        if (gameId != null) {
            ValidationUtils.positiveInt(gameId);
        }
        if (type != null) {
            ValidationUtils.badRequest("instant".equals(type) || "scheduled".equals(type));
        }
        if (status != null) {
            ValidationUtils.badRequest("waiting".equals(status) || "ready".equals(status));
        }
        ValidationUtils.page(page, size, myRooms ? 50 : 50);
    }

    private void cleanupExpiredRooms(Collection<Room> rooms) {
        for (Room room : rooms) {
            if (roomCleanupService.isExpired(room)) {
                roomCleanupService.cleanupIfExpired(room.getId());
            }
        }
    }

    private void cleanupUserExpiredRooms(Long userId) {
        cleanupExpiredRooms(roomMapper.selectNonClosedByUserId(userId));
    }

    private List<Room> findValidUserRooms(Long userId) {
        return roomMapper.selectNonClosedByUserId(userId).stream()
            .filter(room -> !roomCleanupService.isExpired(room))
            .toList();
    }

    private void ensureJoinableRoom(Long roomId, Room room) {
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (roomCleanupService.isExpired(room)) {
            roomCleanupService.closeRoom(roomId);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private void ensureRoomExists(Long roomId, Room room) {
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (roomCleanupService.isExpired(room)) {
            roomCleanupService.closeRoom(roomId);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private RoomDetailVo loadRoomDetail(Long userId, Long roomId) {
        Room room = roomMapper.selectById(roomId);
        ensureRoomExists(roomId, room);

        RoomUser membership = roomUserMapper.selectByRoomIdAndUserId(roomId, userId);
        if (membership == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您不在该房间中");
        }

        Game game = gameMapper.selectById(room.getGameId());
        User owner = userMapper.selectById(room.getOwnerId());
        List<RoomUser> members = new ArrayList<>(roomUserMapper.selectByRoomId(roomId));
        members.sort(Comparator
            .comparing((RoomUser member) -> !member.getUserId().equals(room.getOwnerId()))
            .thenComparing(RoomUser::getJoinTime));

        Map<Long, User> userMap = toUserMap(members.stream().map(RoomUser::getUserId).collect(Collectors.toSet()));
        List<RoomMemberVo> memberVos = members.stream()
            .map(member -> {
                User user = userMap.get(member.getUserId());
                return new RoomMemberVo(
                    member.getUserId(),
                    user == null ? "" : user.getNickname(),
                    user == null ? "" : user.getAvatar(),
                    TimeUtils.format(member.getJoinTime())
                );
            })
            .toList();

        return new RoomDetailVo(
            room.getId(),
            room.getGameId(),
            game == null ? "" : game.getGameName(),
            room.getOwnerId(),
            owner == null ? "" : owner.getNickname(),
            room.getMaxPlayer(),
            room.getCurrentPlayer(),
            roomCacheService.getOnlineCount(roomId),
            room.getType(),
            TimeUtils.format(room.getStartTime()),
            room.getStatus(),
            TimeUtils.format(room.getCreateTime()),
            TimeUtils.format(room.getUpdateTime()),
            room.getOwnerId().equals(userId),
            true,
            memberVos
        );
    }

    private List<RoomListItemVo> toRoomListItemVos(List<Room> rooms, Set<Long> joinedRoomIds) {
        List<Room> validRooms = rooms.stream()
            .filter(room -> !"closed".equals(room.getStatus()))
            .filter(room -> !roomCleanupService.isExpired(room))
            .toList();
        Map<Integer, Game> gameMap = toGameMap(validRooms.stream().map(Room::getGameId).collect(Collectors.toSet()));
        Map<Long, User> userMap = toUserMap(validRooms.stream().map(Room::getOwnerId).collect(Collectors.toSet()));
        return validRooms.stream()
            .map(room -> new RoomListItemVo(
                room.getId(),
                room.getGameId(),
                gameMap.get(room.getGameId()) == null ? "" : gameMap.get(room.getGameId()).getGameName(),
                room.getOwnerId(),
                userMap.get(room.getOwnerId()) == null ? "" : userMap.get(room.getOwnerId()).getNickname(),
                room.getMaxPlayer(),
                room.getCurrentPlayer(),
                room.getType(),
                TimeUtils.format(room.getStartTime()),
                room.getStatus(),
                TimeUtils.format(room.getCreateTime()),
                joinedRoomIds.contains(room.getId())
            ))
            .toList();
    }

    private List<RoomMyItemVo> toRoomMyItemVos(Long userId, List<Room> rooms) {
        List<Room> validRooms = rooms.stream()
            .filter(room -> !"closed".equals(room.getStatus()))
            .filter(room -> !roomCleanupService.isExpired(room))
            .toList();
        Map<Integer, Game> gameMap = toGameMap(validRooms.stream().map(Room::getGameId).collect(Collectors.toSet()));
        Map<Long, User> userMap = toUserMap(validRooms.stream().map(Room::getOwnerId).collect(Collectors.toSet()));
        return validRooms.stream()
            .map(room -> new RoomMyItemVo(
                room.getId(),
                room.getGameId(),
                gameMap.get(room.getGameId()) == null ? "" : gameMap.get(room.getGameId()).getGameName(),
                room.getOwnerId(),
                userMap.get(room.getOwnerId()) == null ? "" : userMap.get(room.getOwnerId()).getNickname(),
                room.getMaxPlayer(),
                room.getCurrentPlayer(),
                room.getType(),
                TimeUtils.format(room.getStartTime()),
                room.getStatus(),
                TimeUtils.format(room.getCreateTime()),
                room.getOwnerId().equals(userId)
            ))
            .toList();
    }

    private Map<Long, User> toUserMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> map = new HashMap<>();
        for (User user : userMapper.selectByIds(ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<Integer, Game> toGameMap(Set<Integer> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Game> map = new HashMap<>();
        for (Game game : gameMapper.selectByIds(ids)) {
            map.put(game.getId(), game);
        }
        return map;
    }

    private LocalDateTime parseRequiredDateTime(String value) {
        try {
            return TimeUtils.parse(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
