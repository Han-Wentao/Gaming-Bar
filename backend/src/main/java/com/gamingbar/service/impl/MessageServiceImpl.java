package com.gamingbar.service.impl;

import com.gamingbar.common.exception.BusinessException;
import com.gamingbar.common.util.TimeUtils;
import com.gamingbar.common.util.ValidationUtils;
import com.gamingbar.dto.message.SendMessageRequest;
import com.gamingbar.entity.Message;
import com.gamingbar.entity.Room;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.MessageMapper;
import com.gamingbar.mapper.RoomMapper;
import com.gamingbar.mapper.RoomUserMapper;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.MessageService;
import com.gamingbar.service.RoomCleanupService;
import com.gamingbar.vo.message.MessagePageResponseVo;
import com.gamingbar.vo.message.MessageVo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final RoomMapper roomMapper;
    private final RoomUserMapper roomUserMapper;
    private final UserMapper userMapper;
    private final RoomCleanupService roomCleanupService;

    public MessageServiceImpl(MessageMapper messageMapper,
                              RoomMapper roomMapper,
                              RoomUserMapper roomUserMapper,
                              UserMapper userMapper,
                              RoomCleanupService roomCleanupService) {
        this.messageMapper = messageMapper;
        this.roomMapper = roomMapper;
        this.roomUserMapper = roomUserMapper;
        this.userMapper = userMapper;
        this.roomCleanupService = roomCleanupService;
    }

    @Override
    @Transactional
    public MessageVo sendMessage(Long userId, Long roomId, SendMessageRequest request) {
        ValidationUtils.positive(roomId);
        ValidationUtils.notBlankMessage(request.getContent(), 500);

        Room room = roomMapper.selectByIdForUpdate(roomId);
        ensureRoomAccessible(roomId, room);
        if (roomUserMapper.selectByRoomIdAndUserId(roomId, userId) == null) {
            throw new BusinessException(403, "您不在该房间中");
        }

        Message message = new Message();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setContent(request.getContent());
        messageMapper.insert(message);
        Message stored = messageMapper.selectById(message.getId());
        User user = userMapper.selectById(userId);
        return toMessageVo(stored, user);
    }

    @Override
    public MessagePageResponseVo listMessages(Long userId, Long roomId, Long beforeId, Integer size) {
        ValidationUtils.positive(roomId);
        ValidationUtils.badRequest(beforeId == null || beforeId > 0);
        ValidationUtils.badRequest(size == null || (size > 0 && size <= 100));

        int sizeValue = size == null ? 50 : size;
        Room room = roomMapper.selectById(roomId);
        ensureRoomAccessible(roomId, room);
        if (roomUserMapper.selectByRoomIdAndUserId(roomId, userId) == null) {
            throw new BusinessException(403, "您不在该房间中");
        }

        List<Message> messages = messageMapper.selectMessages(roomId, beforeId, sizeValue + 1);
        boolean hasMore = messages.size() > sizeValue;
        List<Message> messageList = hasMore ? messages.subList(0, sizeValue) : messages;
        Map<Long, User> userMap = new HashMap<>();
        List<Long> userIds = messageList.stream().map(Message::getUserId).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectByIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        return new MessagePageResponseVo(
            hasMore,
            messageList.stream()
                .map(message -> toMessageVo(message, userMap.get(message.getUserId())))
                .toList()
        );
    }

    private void ensureRoomAccessible(Long roomId, Room room) {
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(404, "房间不存在或已失效");
        }
        if (roomCleanupService.isExpired(room)) {
            roomCleanupService.closeRoom(roomId);
            throw new BusinessException(404, "房间不存在或已失效");
        }
    }

    private MessageVo toMessageVo(Message message, User user) {
        return new MessageVo(
            message.getId(),
            message.getRoomId(),
            message.getUserId(),
            user == null ? "" : user.getNickname(),
            user == null ? "" : user.getAvatar(),
            message.getContent(),
            TimeUtils.format(message.getCreateTime())
        );
    }
}
