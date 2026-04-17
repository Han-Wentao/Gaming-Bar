package com.gamingbar.controller.game;

import com.gamingbar.common.result.ApiResponse;
import com.gamingbar.service.GameService;
import com.gamingbar.vo.game.GameVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ApiResponse<List<GameVo>> listGames() {
        return ApiResponse.success(gameService.listGames());
    }
}
