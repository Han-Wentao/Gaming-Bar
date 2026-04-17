package com.gamingbar.mapper;

import com.gamingbar.entity.Room;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoomMapper {

    @Select("select * from t_room where id = #{id}")
    Room selectById(@Param("id") Long id);

    @Select("select * from t_room where id = #{id} for update")
    Room selectByIdForUpdate(@Param("id") Long id);

    @Select("""
        select r.* from t_room r
        join t_room_user ru on ru.room_id = r.id
        where ru.user_id = #{userId} and r.status != 'closed'
        order by r.create_time desc, r.id desc
        """)
    List<Room> selectNonClosedByUserId(@Param("userId") Long userId);

    @Select("select * from t_room where status != 'closed' order by create_time desc, id desc")
    List<Room> selectNonClosedRooms();

    @Insert("""
        insert into t_room(game_id, owner_id, max_player, current_player, type, start_time, status)
        values(#{gameId}, #{ownerId}, #{maxPlayer}, #{currentPlayer}, #{type}, #{startTime}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Room room);

    @Update("""
        update t_room
        set current_player = #{currentPlayer}, status = #{status}, update_time = current_timestamp
        where id = #{id}
        """)
    int updatePlayerAndStatus(@Param("id") Long id,
                              @Param("currentPlayer") Integer currentPlayer,
                              @Param("status") String status);

    @Update("""
        update t_room
        set status = #{status}, current_player = #{currentPlayer}, update_time = current_timestamp
        where id = #{id}
        """)
    int closeRoom(@Param("id") Long id,
                  @Param("status") String status,
                  @Param("currentPlayer") Integer currentPlayer);

    @Update("""
        update t_room
        set start_time = #{startTime}, update_time = current_timestamp
        where id = #{id}
        """)
    int updateStartTime(@Param("id") Long id, @Param("startTime") LocalDateTime startTime);
}
