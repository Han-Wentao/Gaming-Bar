package com.gamingbar.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WsTicketResponseVo {

    private String ticket;

    private Long expiresIn;
}
