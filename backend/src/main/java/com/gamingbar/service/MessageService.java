package com.gamingbar.service;

import com.gamingbar.dto.message.SendMessageRequest;
import com.gamingbar.vo.message.MessagePageResponseVo;
import com.gamingbar.vo.message.MessageVo;

public interface MessageService {

    MessageVo sendMessage(Long userId, Long roomId, SendMessageRequest request);

    MessagePageResponseVo listMessages(Long userId, Long roomId, Long cursor, Integer size);
}
