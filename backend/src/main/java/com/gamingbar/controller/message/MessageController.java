package com.gamingbar.controller.message;

import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.result.ApiResponse;
import com.gamingbar.dto.message.SendMessageRequest;
import com.gamingbar.service.MessageService;
import com.gamingbar.vo.message.MessagePageResponseVo;
import com.gamingbar.vo.message.MessageVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ApiResponse<MessageVo> sendMessage(@PathVariable Long roomId, @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(messageService.sendMessage(UserContext.getUserId(), roomId, request));
    }

    @GetMapping
    public ApiResponse<MessagePageResponseVo> listMessages(@PathVariable Long roomId,
                                                           @RequestParam(required = false) Long cursor,
                                                           @RequestParam(required = false) Long beforeId,
                                                           @RequestParam(required = false) Integer size) {
        Long actualCursor = cursor == null ? beforeId : cursor;
        return ApiResponse.success(messageService.listMessages(UserContext.getUserId(), roomId, actualCursor, size));
    }
}
