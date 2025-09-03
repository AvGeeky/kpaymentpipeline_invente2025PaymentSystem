package com.saipbuilds.kpaymentpipeline.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Event produced after successful reconciliation of a hackathon payment
 * with its receipt. Combines payment request and receipt confirmation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonReconciledEvent {

    // ---- Payment Info ----
    private String leader_email;
    private String team_name;
    private String track;
    private String paymentID;
    private String createdAt;  // from payment event
//    private String team_id;
    private Map<Integer, String> member_emails;
    private Map<Integer, String> member_names;
    private Map<Integer, String> member_institutions;
    private Map<Integer,String> member_phones;

    private Map<Integer,String> member_gender;
    private Map<Integer,String> member_department;
    private Map<Integer,String> member_yearofstudy;

    // ---- Receipt Info ----
    private String emailID;
    private String paidOn;
    private String method;
    private String amount;
    private String phoneNumber;

    // ---- Reconciliation Status ----
    private boolean reconciled;
}
