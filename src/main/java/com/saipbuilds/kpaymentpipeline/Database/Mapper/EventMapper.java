package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventMapper {

    @Select("Select name from events where external_id = #{eventId}")
    String findEventNameById(int eventId);

    @Select("Select d.name from events e, departments d where e.department_id = d.id and e.external_id = #{eventId}")
    String findDeptNameFromEventId(int eventId);

}
