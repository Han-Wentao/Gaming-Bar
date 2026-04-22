package com.gamingbar.service;

import com.gamingbar.common.result.PageData;
import com.gamingbar.dto.room.CreateRoomRequest;
import com.gamingbar.vo.room.LeaveRoomResponseVo;
import com.gamingbar.vo.room.RoomDetailVo;
import com.gamingbar.vo.room.RoomListItemVo;
import com.gamingbar.vo.room.RoomMyItemVo;
import java.util.List;

public interface RoomService {

    RoomDetailVo createRoom(Long userId, CreateRoomRequest request);

    PageData<RoomListItemVo> listRooms(Long userId, Integer gameId, String type, String status, Integer page, Integer size);

    List<RoomListItemVo> listHotRooms(Long userId, Integer limit);

    RoomDetailVo getRoomDetail(Long userId, Long roomId);

    RoomDetailVo joinRoom(Long userId, Long roomId);

    LeaveRoomResponseVo leaveRoom(Long userId, Long roomId);

    void dissolveRoom(Long userId, Long roomId);

    PageData<RoomMyItemVo> listMyRooms(Long userId, String status, Integer page, Integer size);
}
