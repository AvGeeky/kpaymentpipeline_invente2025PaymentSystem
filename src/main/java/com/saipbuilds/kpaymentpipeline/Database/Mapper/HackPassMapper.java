package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import com.saipbuilds.kpaymentpipeline.Database.Entities.HackPass;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HackPassMapper {

    // Insert new hack_pass (ticket_issued always false at creation)
    @Insert("INSERT INTO hack_passes(team_id, leader_email, team_name, track, payment_id, ticket_issued, created_at) " +
            "VALUES(#{teamId}, #{leaderEmail}, #{teamName}, #{track}, #{paymentId}, false, now())")
    void insertHackPass(HackPass hackPass);

    // Update ticket_issued to true
    @Update("UPDATE hack_passes SET ticket_issued = true WHERE team_id = #{teamId}")
    void markTicketIssued(String teamId);

    // Check if ticket_issued is true, or record doesn’t exist
    @Select("SELECT ticket_issued FROM hack_passes WHERE team_id = #{teamId}")
    Boolean isTicketIssued(String teamId);

    @Select("SELECT team_id FROM hack_passes WHERE payment_id = #{paymentId}")
    List<String> findTeamIdByPaymentId(String paymentId);
}
