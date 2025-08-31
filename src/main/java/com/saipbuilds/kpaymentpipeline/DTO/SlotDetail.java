package com.saipbuilds.kpaymentpipeline.DTO;

import lombok.Data;

/**
 * A simple DTO to hold the result of a query for a slot number and event ID.
 */
@Data
public class SlotDetail {
    private int slotNo;
    private int eventId;
}
