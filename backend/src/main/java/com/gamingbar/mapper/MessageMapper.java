package com.gamingbar.mapper;

import com.gamingbar.entity.Message;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper {

    @Insert("""
        insert into t_message(room_id, user_id, content)
        values(#{roomId}, #{userId}, #{content})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Delete("delete from t_message where room_id = #{roomId}")
    int deleteByRoomId(@Param("roomId") Long roomId);

    @Select("select * from t_message where id = #{id}")
    Message selectById(@Param("id") Long id);

    @Select("""
        <script>
        select * from t_message
        where room_id = #{roomId}
        <if test='cursor != null'>
            and id &lt; #{cursor}
        </if>
        order by id desc
        limit #{limit}
        </script>
        """)
    List<Message> selectMessages(@Param("roomId") Long roomId,
                                 @Param("cursor") Long cursor,
                                 @Param("limit") Integer limit);
}
