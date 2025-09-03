package com.saipbuilds.kpaymentpipeline.Database.Mapper;



import com.saipbuilds.kpaymentpipeline.Database.Entities.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users(email, name, phone, created_at) " +
            "VALUES(#{email}, #{name}, #{phone}, now()) on conflict do nothing")
    void insertUser(User user);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);

    @Select("SELECT * FROM users")
    List<User> findAll();
}

