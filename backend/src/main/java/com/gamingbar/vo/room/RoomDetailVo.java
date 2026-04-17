package com.gamingbar.vo.room;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomDetailVo {

    private Long id;
    private Integer gameId;
    private String gameName;
    private Long ownerId;
    private String ownerNickname;
    private Integer maxPlayer;
    private Integer currentPlayer;
    private String type;
    private String startTime;
    private String status;
    private String createTime;
    private String updateTime;
    private Boolean isOwner;
    private Boolean isJoined;
    private List<RoomMemberVo> members;
}
