package com.saipbuilds.kpaymentpipeline.Service;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Database.Entities.Pass;
import com.saipbuilds.kpaymentpipeline.Database.Entities.Receipt;
import com.saipbuilds.kpaymentpipeline.Database.Entities.Slot;
import com.saipbuilds.kpaymentpipeline.Database.Entities.User;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.ReceiptMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.SlotMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.UserMapper;
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
    UUID namespace = UUID.fromString("1b402880-873b-449d-8501-155e8283b4b8");
    @Autowired
    public PaymentDatabaseServices(UserMapper userMapper, ReceiptMapper receiptMapper, PassMapper passMapper, SlotMapper slotMapper) {
        this.userMapper = userMapper;
        this.receiptMapper = receiptMapper;
        this.passMapper = passMapper;
        this.slotMapper = slotMapper;
    }
    private String generateUUID5(String paymentID){
        UUID uuid = UUID.randomUUID();
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
    public String insertOnePassAndReturnUUID(String emailId, String paymentID) {
        String uuid = generateUUID5(paymentID);
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


}
