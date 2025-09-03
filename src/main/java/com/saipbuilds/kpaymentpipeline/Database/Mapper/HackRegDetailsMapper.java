package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import com.saipbuilds.kpaymentpipeline.Database.Entities.HackRegDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HackRegDetailsMapper {

    // Insert new team member row
    @Insert("INSERT INTO hack_reg_details(team_id, email, full_name, institution, created_at, phone_number, gender, department, year_of_study) " +
            "VALUES(#{teamId}, #{email}, #{fullName}, #{institution}, now(), #{phoneNumber}, #{gender}, #{Department}, #{yearOfStudy})")
    void insertHackRegDetail(HackRegDetail hackRegDetail);

    // Fetch all members for a team
    @Select("SELECT * FROM hack_reg_details WHERE team_id = #{teamId}")
    List<HackRegDetail> findByTeamId(String teamId);
}
