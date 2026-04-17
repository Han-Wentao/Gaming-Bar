package com.gamingbar.service.impl;

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
import com.gamingbar.service.RoomCleanupService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final RoomUserMapper roomUserMapper;
    private final UserMapper userMapper;
    private final GameMapper gameMapper;
    private final RoomCleanupService roomCleanupService;

    public RoomServiceImpl(RoomMapper roomMapper,
                           RoomUserMapper roomUserMapper,
                           UserMapper userMapper,
                           GameMapper gameMapper,
                           RoomCleanupService roomCleanupService) {
        this.roomMapper = roomMapper;
        this.roomUserMapper = roomUserMapper;
        this.userMapper = userMapper;
        this.gameMapper = gameMapper;
        this.roomCleanupService = roomCleanupService;
    }

    @Override
    @Transactional
    public RoomDetailVo createRoom(Long userId, CreateRoomRequest request) {
        validateCreateRequest(request);
        Game game = gameMapper.selectById(request.getGameId());
        ValidationUtils.badRequest(game != null && "enabled".equals(game.getStatus()));

        userMapper.selectByIdForUpdate(userId);
        cleanupUserExpiredRooms(userId);
        ValidationUtils.require(findValidUserRooms(userId).isEmpty(), 409, "您已在其他未关闭房间中");

        Room room = new Room();
        room.setGameId(request.getGameId());
        room.setOwnerId(userId);
        room.setMaxPlayer(request.getMaxPlayer());
        room.setCurrentPlayer(1);
        room.setType(request.getType());
        room.setStartTime(parseStartTime(request));
        room.setStatus("waiting");
        roomMapper.insert(room);

        RoomUser roomUser = new RoomUser();
        roomUser.setRoomId(room.getId());
        roomUser.setUserId(userId);
        roomUserMapper.insert(roomUser);

        return loadRoomDetail(userId, room.getId());
    }

    @Override
    public PageData<RoomListItemVo> listRooms(Long userId, Integer gameId, String type, String status, Integer page, Integer size) {
        validateListFilters(gameId, type, status, page, size, false);
        int pageValue = page == null ? 1 : page;
        int sizeValue = size == null ? 20 : size;

        cleanupExpiredRooms(roomMapper.selectNonClosedRooms());
        List<Room> rooms = roomMapper.selectNonClosedRooms().stream()
            .filter(room -> "waiting".equals(room.getStatus()) || "ready".equals(room.getStatus()))
            .filter(room -> gameId == null || gameId.equals(room.getGameId()))
            .filter(room -> type == null || type.equals(room.getType()))
            .filter(room -> status == null || status.equals(room.getStatus()))
            .toList();

        List<RoomListItemVo> items = rooms.stream()
            .map(room -> toRoomListItemVo(userId, room))
            .toList();
        return paginate(items, pageValue, sizeValue);
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
        ValidationUtils.require(!inOtherRoom, 409, "您已在其他未关闭房间中");

        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(404, "房间不存在或已失效");
        }
        if (roomCleanupService.isExpired(room)) {
            roomCleanupService.closeRoom(roomId);
            throw new BusinessException(404, "房间不存在或已失效");
        }
        if (roomUserMapper.selectByRoomIdAndUserId(roomId, userId) != null || userRooms.stream().anyMatch(item -> item.getId().equals(roomId))) {
            throw new BusinessException(409, "您已在该房间中");
        }
        if (room.getCurrentPlayer() >= room.getMaxPlayer()) {
            throw new BusinessException(409, "房间已满，无法加入");
        }

        RoomUser roomUser = new RoomUser();
        roomUser.setRoomId(roomId);
        roomUser.setUserId(userId);
        roomUserMapper.insert(roomUser);
        int currentPlayer = roomUserMapper.countByRoomId(roomId);
        roomMapper.updatePlayerAndStatus(roomId, currentPlayer, currentPlayer >= room.getMaxPlayer() ? "ready" : "waiting");
        return loadRoomDetail(userId, roomId);
    }

    @Override
    @Transactional
    public LeaveRoomResponseVo leaveRoom(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(404, "房间不存在或已失效");
        }
        if (roomCleanupService.isExpired(room)) {
            roomCleanupService.closeRoom(roomId);
            throw new BusinessException(404, "房间不存在或已失效");
        }

        RoomUser roomUser = roomUserMapper.selectByRoomIdAndUserId(roomId, userId);
        if (roomUser == null) {
            throw new BusinessException(403, "您不在该房间中");
        }

        if (room.getOwnerId().equals(userId)) {
            roomCleanupService.closeRoom(roomId);
            return new LeaveRoomResponseVo("room_closed");
        }

        roomUserMapper.deleteByRoomIdAndUserId(roomId, userId);
        int currentPlayer = roomUserMapper.countByRoomId(roomId);
        roomMapper.updatePlayerAndStatus(roomId, currentPlayer, currentPlayer < room.getMaxPlayer() ? "waiting" : "ready");
        return new LeaveRoomResponseVo("left");
    }

    @Override
    @Transactional
    public void dissolveRoom(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException(404, "房间不存在或已失效");
        }
        if ("closed".equals(room.getStatus())) {
            return;
        }
        if (!room.getOwnerId().equals(userId)) {
            throw new BusinessException(403, "仅房主可解散房间");
        }
        roomCleanupService.closeRoom(roomId);
    }

    @Override
    public PageData<RoomMyItemVo> listMyRooms(Long userId, String status, Integer page, Integer size) {
        validateListFilters(null, null, status, page, size, true);
        int pageValue = page == null ? 1 : page;
        int sizeValue = size == null ? 20 : size;

        cleanupUserExpiredRooms(userId);
        List<RoomMyItemVo> items = findValidUserRooms(userId).stream()
            .filter(room -> "waiting".equals(room.getStatus()) || "ready".equals(room.getStatus()))
            .filter(room -> status == null || status.equals(room.getStatus()))
            .map(room -> toRoomMyItemVo(userId, room))
            .toList();
        return paginate(items, pageValue, sizeValue);
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

    private RoomDetailVo loadRoomDetail(Long userId, Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(404, "房间不存在或已失效");
        }
        if (roomCleanupService.isExpired(room)) {
            roomCleanupService.cleanupIfExpired(roomId);
            throw new BusinessException(404, "房间不存在或已失效");
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

        boolean joined = roomUserMapper.selectByRoomIdAndUserId(roomId, userId) != null;
        return new RoomDetailVo(
            room.getId(),
            room.getGameId(),
            game == null ? "" : game.getGameName(),
            room.getOwnerId(),
            owner == null ? "" : owner.getNickname(),
            room.getMaxPlayer(),
            room.getCurrentPlayer(),
            room.getType(),
            TimeUtils.format(room.getStartTime()),
            room.getStatus(),
            TimeUtils.format(room.getCreateTime()),
            TimeUtils.format(room.getUpdateTime()),
            room.getOwnerId().equals(userId),
            joined,
            memberVos
        );
    }

    private RoomListItemVo toRoomListItemVo(Long userId, Room room) {
        Map<Integer, Game> gameMap = toGameMap(Set.of(room.getGameId()));
        Map<Long, User> userMap = toUserMap(Set.of(room.getOwnerId()));
        return new RoomListItemVo(
            room.getId(),
            room.getGameId(),
            gameMap.get(room.getGameId()).getGameName(),
            room.getOwnerId(),
            userMap.get(room.getOwnerId()).getNickname(),
            room.getMaxPlayer(),
            room.getCurrentPlayer(),
            room.getType(),
            TimeUtils.format(room.getStartTime()),
            room.getStatus(),
            TimeUtils.format(room.getCreateTime()),
            roomUserMapper.selectByRoomIdAndUserId(room.getId(), userId) != null
        );
    }

    private RoomMyItemVo toRoomMyItemVo(Long userId, Room room) {
        Map<Integer, Game> gameMap = toGameMap(Set.of(room.getGameId()));
        Map<Long, User> userMap = toUserMap(Set.of(room.getOwnerId()));
        return new RoomMyItemVo(
            room.getId(),
            room.getGameId(),
            gameMap.get(room.getGameId()).getGameName(),
            room.getOwnerId(),
            userMap.get(room.getOwnerId()).getNickname(),
            room.getMaxPlayer(),
            room.getCurrentPlayer(),
            room.getType(),
            TimeUtils.format(room.getStartTime()),
            room.getStatus(),
            TimeUtils.format(room.getCreateTime()),
            room.getOwnerId().equals(userId)
        );
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

    private <T> PageData<T> paginate(List<T> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return new PageData<>(
            items.size(),
            page,
            size,
            items.subList(fromIndex, toIndex)
        );
    }

    private LocalDateTime parseRequiredDateTime(String value) {
        try {
            return TimeUtils.parse(value);
        } catch (Exception exception) {
            throw new BusinessException(400, "参数不合法");
        }
    }
}
