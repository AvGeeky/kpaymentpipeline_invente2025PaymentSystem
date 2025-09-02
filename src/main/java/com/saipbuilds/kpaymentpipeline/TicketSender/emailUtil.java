package com.saipbuilds.kpaymentpipeline.TicketSender;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

// emailUtil is a utility class for sending emails asynchronously
// CALL THIS CLASS TO SEND EMAILS
@Service
public class emailUtil {

    private final AsyncEmailSenderUtil asyncEmailSender;

    @Autowired
    public emailUtil(AsyncEmailSenderUtil asyncEmailSender) {
        this.asyncEmailSender = asyncEmailSender;
    }

    /**
     * Sends the final ticket confirmation email after a payment has been reconciled.
     * @param mail The recipient's email address.
     * @param details The ReconciledEvent containing all necessary data for the ticket.
     */
    public void sendTicketPurchaseMail(String mail, ReconciledEvent details) {
        asyncEmailSender.sendTicketEmail(mail, details);
    }
    public void sendHackathonTicketMail(String recipient, HackathonReconciledEvent details, List<String> ticketUuids) {
        asyncEmailSender.sendHackathonTicketEmail(recipient, details, ticketUuids);
    }
}
