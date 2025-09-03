package com.saipbuilds.kpaymentpipeline.Service;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonPaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.HackathonReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Database.Entities.*;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import com.fasterxml.uuid.Generators;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentDatabaseServices {
    private final UserMapper userMapper;
    private final ReceiptMapper receiptMapper;
    private final PassMapper passMapper;
    private final SlotMapper slotMapper;
    private final HackPassMapper hackPassMapper;
    private final HackRegDetailsMapper hackRegDetailsMapper;

    @Autowired
    public PaymentDatabaseServices(UserMapper userMapper, ReceiptMapper receiptMapper, PassMapper passMapper, SlotMapper slotMapper, HackPassMapper hackPassMapper, HackRegDetailsMapper hackRegDetailsMapper) {
        this.userMapper = userMapper;
        this.receiptMapper = receiptMapper;
        this.passMapper = passMapper;
        this.slotMapper = slotMapper;
        this.hackPassMapper = hackPassMapper;
        this.hackRegDetailsMapper = hackRegDetailsMapper;
    }
    private String generateUUID5(String type){
        UUID uuid = UUID.randomUUID();
        if (type == null) {
            return uuid.toString();
        }
        if (type.equalsIgnoreCase("w") || type.equalsIgnoreCase("t") || type.equalsIgnoreCase("n") || type.equalsIgnoreCase("h")){
            return uuid.toString()+"$"+type.toLowerCase();
        }
        return uuid.toString();
    }

    public void insertDetails(PaymentReceivedEvent pre){
        User user = new User(pre.getEmailID(), pre.getName(), pre.getPhoneNumber(), Instant.now().toString());
        userMapper.insertUser(user);
        log.info(" S1: Inserted Into Database for UserDetails: {}", user.getEmail());
    }
    public void insertReceipt(ReceiptReceivedEvent rre){
        Receipt receipt = new Receipt(rre.getPaymentID(), rre.getPaidOn(), rre.getMethod(), rre.getEmailID(), rre.getPhoneNumber(), false, rre.getAmount());
        User u = userMapper.findByEmail(rre.getEmailID());
        if (u == null) {
            log.warn("User with email {} not found." + rre.getEmailID());
            return;
        }
        receiptMapper.insertReceipt(receipt);
        log.info(" S2: Inserted Into Database for Receipt: {}", receipt.getEmail());
    }
    public String insertOnePassAndReturnUUID(String emailId, String paymentID, String type) {
        String uuid = generateUUID5(type);
        Pass pass = new Pass(uuid, emailId, paymentID, false, Instant.now().toString());
        passMapper.insertPass(pass);
        log.info("Inserted pass for user: {}", emailId);
        return uuid;
    }
    public void insertIntoSlots(String uuid, Map<Integer,Integer> pass) {
        for (Map.Entry<Integer, Integer> entry : pass.entrySet()) {
            Integer slotID = entry.getKey();
            Integer eventID = entry.getValue();
            Slot slot = new Slot(uuid, slotID, eventID,false, Instant.now().toString());
            slotMapper.insertSlot(slot);
        }
        log.info("Slots Inserted for passId: {}", pass);
    }

    public void insertHackUserS1(HackathonPaymentReceivedEvent event) {
        User user = new User(event.getLeader_email(), event.getMember_names().get(1), event.getMember_phones().get(1), Instant.now().toString());
        userMapper.insertUser(user);
    }
    public void insertHackDetailsS3(HackathonReconciledEvent event) {
        String teamId = generateUUID5("h");
        log.info("Inserting hackathon details for team_id={} leader={}", teamId, event.getLeader_email());


        // ---- 1. Insert into hack_passes ----
        HackPass pass = new HackPass(
                teamId,
                event.getLeader_email(),
                event.getTeam_name(),
                event.getTrack(),
                event.getPaymentID(),
                false, // ticketIssued defaults to false
                Instant.now()
        );
        hackPassMapper.insertHackPass(pass);

        // ---- 2. Insert each team member into hack_reg_details ----
        Map<Integer, String> emails = event.getMember_emails();
        Map<Integer, String> names = event.getMember_names();
        Map<Integer, String> institutions = event.getMember_institutions();
        Map<Integer, String> phones = event.getMember_phones();

        if (emails != null) {
            for (Map.Entry<Integer, String> entry : emails.entrySet()) {
                Integer idx = entry.getKey();
                String email = entry.getValue();

                HackRegDetail member = new HackRegDetail(
                        teamId,
                        email,
                        names != null ? names.get(idx) : null,
                        institutions != null ? institutions.get(idx) : null,
                        Instant.now(),
                        phones != null ? phones.get(idx) : null,
                        event.getMember_gender().get(idx),
                        event.getMember_department().get(idx),
                        event.getMember_yearofstudy().get(idx)

                );

                hackRegDetailsMapper.insertHackRegDetail(member);
            }
        }

        log.info("Inserted hackathon team={} with {} members", event.getTeam_name(),
                emails != null ? emails.size() : 0);
    }
}
