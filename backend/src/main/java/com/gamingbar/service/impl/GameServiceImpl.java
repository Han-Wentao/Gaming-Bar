package com.gamingbar.service.impl;

import com.gamingbar.mapper.GameMapper;
import com.gamingbar.service.GameService;
import com.gamingbar.vo.game.GameVo;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameServiceImpl implements GameService {

    private final GameMapper gameMapper;

    public GameServiceImpl(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    @Override
    public List<GameVo> listGames() {
        return gameMapper.selectEnabledGames().stream()
            .map(game -> new GameVo(game.getId(), game.getGameName(), game.getStatus()))
            .toList();
    }
}
