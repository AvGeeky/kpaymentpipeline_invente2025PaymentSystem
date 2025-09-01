package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import com.saipbuilds.kpaymentpipeline.Database.Entities.Pass;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface PassMapper {

    @Insert("INSERT INTO passes(pass_id, user_email, payment_id, ticket_issued, created_at) " +
            "VALUES(#{uuid}, #{userEmail}, #{paymentId}, false, now())")
    void insertPass(Pass pass);


    @Select("SELECT pass_id FROM passes WHERE payment_id = #{paymentId}")
    List<String> findUuidsByPaymentId(String paymentId);

    @Select("SELECT * FROM passes WHERE pass_id = #{uuid}")
    Pass findByUuid(UUID uuid);

    @Update("UPDATE passes SET ticket_issued = true WHERE pass_id = #{uuid}")
    void markTicketIssued(String uuid);

    @Select("SELECT * FROM passes")
    List<Pass> findAll();
}
