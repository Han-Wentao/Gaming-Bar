package com.gamingbar.mapper;

import com.gamingbar.entity.RoomUser;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoomUserMapper {

    @Select("select * from t_room_user where room_id = #{roomId} and user_id = #{userId} limit 1")
    RoomUser selectByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Select("select * from t_room_user where room_id = #{roomId} order by join_time asc, id asc")
    List<RoomUser> selectByRoomId(@Param("roomId") Long roomId);

    @Select("select count(1) from t_room_user where room_id = #{roomId}")
    int countByRoomId(@Param("roomId") Long roomId);

    @Insert("""
        insert into t_room_user(room_id, user_id)
        values(#{roomId}, #{userId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoomUser roomUser);

    @Delete("delete from t_room_user where room_id = #{roomId}")
    int deleteByRoomId(@Param("roomId") Long roomId);

    @Delete("delete from t_room_user where room_id = #{roomId} and user_id = #{userId}")
    int deleteByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
