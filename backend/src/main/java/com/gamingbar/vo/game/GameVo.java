package com.gamingbar.vo.game;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameVo {

    private Integer id;
    private String gameName;
    private String status;
}
