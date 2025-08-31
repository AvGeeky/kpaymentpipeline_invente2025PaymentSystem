package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import com.saipbuilds.kpaymentpipeline.Database.Entities.Pass;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface PassMapper {

    @Insert("INSERT INTO pass(uuid, user_email, payment_id, ticket_issued, created_at) " +
            "VALUES(#{uuid}, #{userEmail}, #{paymentId}, false, now())")
    void insertPass(Pass pass);



    @Select("SELECT * FROM pass WHERE uuid = #{uuid}")
    Pass findByUuid(UUID uuid);

    @Update("UPDATE pass SET ticket_issued = true WHERE uuid = #{uuid}")
    void markTicketIssued(UUID uuid);

    @Select("SELECT * FROM pass")
    List<Pass> findAll();
}
