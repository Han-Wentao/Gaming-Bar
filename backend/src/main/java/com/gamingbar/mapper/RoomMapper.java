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

    @Select("""
        <script>
        select * from t_room
        where status in ('waiting', 'ready')
        <if test='gameId != null'>
            and game_id = #{gameId}
        </if>
        <if test='type != null'>
            and type = #{type}
        </if>
        <if test='status != null'>
            and status = #{status}
        </if>
        order by create_time desc, id desc
        limit #{size} offset #{offset}
        </script>
        """)
    List<Room> selectPage(@Param("gameId") Integer gameId,
                          @Param("type") String type,
                          @Param("status") String status,
                          @Param("offset") int offset,
                          @Param("size") int size);

    @Select("""
        <script>
        select count(1) from t_room
        where status in ('waiting', 'ready')
        <if test='gameId != null'>
            and game_id = #{gameId}
        </if>
        <if test='type != null'>
            and type = #{type}
        </if>
        <if test='status != null'>
            and status = #{status}
        </if>
        </script>
        """)
    long countPage(@Param("gameId") Integer gameId,
                   @Param("type") String type,
                   @Param("status") String status);

    @Select("""
        <script>
        select r.* from t_room r
        join t_room_user ru on ru.room_id = r.id
        where ru.user_id = #{userId}
          and r.status in ('waiting', 'ready')
        <if test='status != null'>
            and r.status = #{status}
        </if>
        order by r.create_time desc, r.id desc
        limit #{size} offset #{offset}
        </script>
        """)
    List<Room> selectMyPage(@Param("userId") Long userId,
                            @Param("status") String status,
                            @Param("offset") int offset,
                            @Param("size") int size);

    @Select("""
        <script>
        select count(1) from t_room r
        join t_room_user ru on ru.room_id = r.id
        where ru.user_id = #{userId}
          and r.status in ('waiting', 'ready')
        <if test='status != null'>
            and r.status = #{status}
        </if>
        </script>
        """)
    long countMyPage(@Param("userId") Long userId, @Param("status") String status);

    @Select("""
        select * from t_room
        where status in ('waiting', 'ready')
        order by current_player desc, create_time desc, id desc
        limit #{limit}
        """)
    List<Room> selectHotRooms(@Param("limit") int limit);

    @Insert("""
        insert into t_room(game_id, owner_id, max_player, current_player, type, start_time, status, version)
        values(#{gameId}, #{ownerId}, #{maxPlayer}, #{currentPlayer}, #{type}, #{startTime}, #{status}, #{version})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Room room);

    @Update("""
        update t_room
        set current_player = #{currentPlayer},
            status = #{status},
            version = version + 1,
            update_time = current_timestamp
        where id = #{id}
          and version = #{version}
        """)
    int updatePlayerAndStatus(@Param("id") Long id,
                              @Param("version") Long version,
                              @Param("currentPlayer") Integer currentPlayer,
                              @Param("status") String status);

    @Update("""
        update t_room
        set status = #{status},
            current_player = #{currentPlayer},
            version = version + 1,
            update_time = current_timestamp
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
