package com.saipbuilds.kpaymentpipeline.DTO;

// Payment initiated by frontend

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HackathonPaymentReceivedEvent {

    private String leader_email;
    private String team_name;
    private String track;
    private String paymentID;
    private String createdAt;
//    private String team_id;
    private Map<Integer,String> member_emails;
    private Map<Integer,String> member_names;
    private Map<Integer,String> member_institutions;
    private Map<Integer,String> member_phones;

}



