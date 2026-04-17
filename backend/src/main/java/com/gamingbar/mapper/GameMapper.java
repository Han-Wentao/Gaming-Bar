package com.gamingbar.mapper;

import com.gamingbar.entity.Game;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GameMapper {

    @Select("select * from t_game where id = #{id} limit 1")
    Game selectById(@Param("id") Integer id);

    @Select("select * from t_game where status = 'enabled' order by sort_no asc, id asc")
    List<Game> selectEnabledGames();

    @Select({
        "<script>",
        "select * from t_game where id in ",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<Game> selectByIds(@Param("ids") Collection<Integer> ids);
}
