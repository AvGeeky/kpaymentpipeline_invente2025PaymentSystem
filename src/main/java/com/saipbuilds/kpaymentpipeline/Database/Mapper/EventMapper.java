package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import com.saipbuilds.kpaymentpipeline.Database.Entities.Event;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EventMapper {

    @Insert("INSERT INTO events(name) VALUES(#{name})")
    void insertEvent(Event event);

    @Select("SELECT * FROM events WHERE id = #{id}")
    Event findById(int id);

    @Select("SELECT * FROM events")
    List<Event> findAll();
}
