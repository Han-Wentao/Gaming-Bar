package com.gamingbar.mapper;

import com.gamingbar.entity.User;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("select * from t_user where id = #{id}")
    User selectById(@Param("id") Long id);

    @Select("select * from t_user where id = #{id} for update")
    User selectByIdForUpdate(@Param("id") Long id);

    @Select("select * from t_user where phone = #{phone} limit 1")
    User selectByPhone(@Param("phone") String phone);

    @Select({
        "<script>",
        "select * from t_user where id in ",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<User> selectByIds(@Param("ids") Collection<Long> ids);

    @Insert("""
        insert into t_user(phone, nickname, avatar, credit_score)
        values(#{phone}, #{nickname}, #{avatar}, #{creditScore})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
        <script>
        update t_user
        <set>
            <if test='nickname != null'>nickname = #{nickname},</if>
            <if test='avatar != null'>avatar = #{avatar},</if>
            update_time = current_timestamp
        </set>
        where id = #{id}
        </script>
        """)
    int updateProfile(User user);
}
