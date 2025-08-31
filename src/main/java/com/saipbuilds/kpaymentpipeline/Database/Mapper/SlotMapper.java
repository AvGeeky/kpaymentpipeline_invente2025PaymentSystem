package com.saipbuilds.kpaymentpipeline.Database.Mapper;

import com.saipbuilds.kpaymentpipeline.DTO.SlotDetail;
import com.saipbuilds.kpaymentpipeline.Database.Entities.Slot;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SlotMapper {

    @Insert("INSERT INTO slots(uuid, slot_no, event_id, attended, created_at) " +
            "VALUES(#{uuid}, #{slotNo}, #{eventId}, false, now())")
    void insertSlot(Slot slot);

    @Select("SELECT * FROM slots WHERE uuid = #{uuid}")
    List<Slot> findByPass(UUID uuid);

    @Update("UPDATE slots SET attended = true WHERE uuid = #{uuid} AND slot_no = #{slotNo}")
    void markAttended(@Param("uuid") UUID uuid, @Param("slotNo") int slotNo);

    @Select("SELECT * FROM slots")
    List<Slot> findAll();

    @Select("SELECT slot_no AS slotNo, event_id AS eventId FROM slots WHERE uuid = #{uuid} ORDER BY slot_no ASC")
    List<SlotDetail> findSlotsByUuid(String uuid);
}

