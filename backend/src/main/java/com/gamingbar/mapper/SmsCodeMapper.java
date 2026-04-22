package com.gamingbar.mapper;

import com.gamingbar.entity.SmsCode;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SmsCodeMapper {

    @Select("select * from t_sms_code where phone = #{phone} order by id desc limit 1")
    SmsCode selectLatestByPhone(@Param("phone") String phone);

    @Select("select * from t_sms_code where phone = #{phone} and used_status = 0 order by id desc limit 1")
    SmsCode selectLatestUnusedByPhone(@Param("phone") String phone);

    @Insert("""
        insert into t_sms_code(phone, sms_code, expired_at, used_status)
        values(#{phone}, #{smsCode}, #{expiredAt}, #{usedStatus})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SmsCode smsCode);

    @Update("""
        update t_sms_code
        set used_status = 1, used_time = #{usedTime}
        where phone = #{phone} and used_status = 0
        """)
    int markUnusedAsUsed(@Param("phone") String phone, @Param("usedTime") LocalDateTime usedTime);

    @Update("""
        update t_sms_code
        set used_status = 1, used_time = #{usedTime}
        where id = #{id} and used_status = 0
        """)
    int markUsed(@Param("id") Long id, @Param("usedTime") LocalDateTime usedTime);
}
