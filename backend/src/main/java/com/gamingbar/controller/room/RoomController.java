package com.gamingbar.controller.room;

import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.result.ApiResponse;
import com.gamingbar.common.result.PageData;
import com.gamingbar.dto.room.CreateRoomRequest;
import com.gamingbar.service.RoomService;
import com.gamingbar.vo.room.LeaveRoomResponseVo;
import com.gamingbar.vo.room.RoomDetailVo;
import com.gamingbar.vo.room.RoomListItemVo;
import com.gamingbar.vo.room.RoomMyItemVo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ApiResponse<RoomDetailVo> createRoom(@RequestBody CreateRoomRequest request) {
        return ApiResponse.success(roomService.createRoom(UserContext.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<PageData<RoomListItemVo>> listRooms(@RequestParam(required = false) Integer gameId,
                                                           @RequestParam(required = false) String type,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        return ApiResponse.success(roomService.listRooms(UserContext.getUserId(), gameId, type, status, page, size));
    }

    @GetMapping("/my")
    public ApiResponse<PageData<RoomMyItemVo>> listMyRooms(@RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        return ApiResponse.success(roomService.listMyRooms(UserContext.getUserId(), status, page, size));
    }

    @GetMapping("/{roomId}")
    public ApiResponse<RoomDetailVo> getRoomDetail(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.getRoomDetail(UserContext.getUserId(), roomId));
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse<RoomDetailVo> joinRoom(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.joinRoom(UserContext.getUserId(), roomId));
    }

    @PostMapping("/{roomId}/leave")
    public ApiResponse<LeaveRoomResponseVo> leaveRoom(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.leaveRoom(UserContext.getUserId(), roomId));
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> dissolveRoom(@PathVariable Long roomId) {
        roomService.dissolveRoom(UserContext.getUserId(), roomId);
        return ApiResponse.success();
    }
}
