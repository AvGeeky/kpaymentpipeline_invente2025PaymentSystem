package com.saipbuilds.kpaymentpipeline.TicketSender;


import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.SlotDetail;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.SlotMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

// AsyncEmailSenderUtil is a functions class for sending emails asynchronously
@Service
public class AsyncEmailSenderUtil {

    private static Transport mailTransport = null;
    private static Session session = null;
    private static final int MAX_ATTEMPTS = 3; // Increased for robustness
    private final SlotMapper slotMapper;

    private final PassMapper passMapper;

    // PassMapper is injected to fetch UUIDs for QR codes
    public AsyncEmailSenderUtil(SlotMapper slotMapper, PassMapper passMapper) {
        this.slotMapper = slotMapper;
        this.passMapper = passMapper;
        initEmailProperties();
    }

    public static void initEmailProperties() {
        // config setup
        String host = "smtp.gmail.com";
        String username = System.getenv("MAIL_ID");
        String password = System.getenv("MAIL_PASSKEY");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        properties.put("mail.smtp.connectiontimeout", "7000");
        properties.put("mail.smtp.timeout", "7000");
        properties.put("mail.smtp.writetimeout", "7000");

        session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            mailTransport = session.getTransport("smtp");
            mailTransport.connect();
            System.out.println("Successfully connected to email server.");
        } catch (MessagingException e) {
            org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class).error("Failed to connect to email server", e);
        }
    }

    /**
     * Generates the HTML content for the ticket confirmation email.
     *
     * @param event The reconciled event details.
     * @param ticketUuids The list of UUIDs for the QR codes.
     * @return A formatted HTML string for the email body.
     */
    private String generateTicketDetailsHtml(ReconciledEvent event, List<String> ticketUuids) {
        StringBuilder ticketsHtml = new StringBuilder();
        int ticketCounter = 1;

        for (int i = 0; i < event.getEventBookingDetails().size(); i++) {
            String ticketUuid = (i < ticketUuids.size()) ? ticketUuids.get(i) : "N/A";
            List<SlotDetail> slotDetails = slotMapper.findSlotsByUuid(ticketUuid);
            // Use Java Streams to efficiently convert the List into a Map.
            Map<Integer, Integer> ticketDetails = slotDetails.stream()
                    .collect(Collectors.toMap(SlotDetail::getSlotNo, SlotDetail::getEventId));


            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + URLEncoder.encode(ticketUuid, StandardCharsets.UTF_8);

            ticketsHtml.append(String.format("""
                <div style="background:#f9f9f9; border:1px solid #ddd; border-radius:12px; padding:20px; margin-bottom:20px; text-align:center;">
                    <h3 style="color:#0055A4; font-size:1.4rem; margin-top:0; margin-bottom:15px;">Ticket #%d</h3>
                    <img src="%s" alt="QR Code for Ticket %s" style="margin-bottom:15px;">
                    <p style="font-size:14px; color:#555; margin-bottom:10px;">Ticket ID: %s</p>
                    <table style="width:100%%; text-align:left; font-size:15px; color:#333; border-collapse: collapse;">
            """, ticketCounter++, qrCodeUrl, ticketUuid, ticketUuid));

            ticketsHtml.append("<tr><th style='padding:8px; border-bottom:1px solid #eee;'>Slot No.</th><th style='padding:8px; border-bottom:1px solid #eee;'>Event ID</th></tr>");
            for (Map.Entry<Integer, Integer> slotEntry : ticketDetails.entrySet()) {
                ticketsHtml.append(String.format("<tr><td style='padding:8px; border-bottom:1px solid #eee;'>%d</td><td style='padding:8px; border-bottom:1px solid #eee;'>%d</td></tr>", slotEntry.getKey(), slotEntry.getValue()));
            }
            ticketsHtml.append("</table></div>");
        }


        return String.format("""
<div style='background:linear-gradient(135deg,#f0f5ff 0%%,#ffffff 100%%);padding:40px 0;font-family:Segoe UI,Arial,sans-serif;'>
    <div style='max-width:600px;margin:auto;background:#FFFFFF;border-radius:18px;box-shadow:0 6px 30px rgba(0,0,0,0.1);padding:40px 32px;'>
        <div style='text-align:center;margin-bottom:24px;'>
            <h2 style='color:#0055A4;font-size:2.2rem;margin:0 0 8px 0;'>Your Tickets Are Here!</h2>
            <p style='font-size:18px;color:#333;margin-bottom:20px;'>Hello %s, thank you for your purchase!</p>
        </div>
        
        <div style="border-top:1px solid #ddd; border-bottom:1px solid #ddd; padding: 20px 0; margin-bottom:25px;">
             <p style='font-size:16px;color:#333;margin-bottom:12px; line-height:1.6;'>
                <strong>Payment ID:</strong> %s<br>
                <strong>Amount Paid:</strong> %s<br>
                <strong>Payment Method:</strong> %s<br>
                <strong>Date:</strong> %s
            </p>
        </div>

        %s

        <p style='color:#999;font-size:12px;text-align:center;margin-top:30px;'>
            Sent by kPaymentPipeline &copy; 2025. Please present the QR code at the event.
        </p>
    </div>
</div>
""",
                event.getName(),
                event.getPaymentID(),
                event.getAmount(),
                event.getMethod(),
                event.getPaidOn(),
                ticketsHtml
        );
    }

    /**
     * Fetches ticket UUIDs, generates QR codes, and sends the ticket email.
     * @param recipient The user's email address.
     * @param event The reconciled event containing all payment and booking details.
     */
    @Async
    public void sendTicketEmail(String recipient, ReconciledEvent event) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            if (mailTransport == null || !mailTransport.isConnected()) {
                initEmailProperties();
            }

            String username = System.getenv("MAIL_ID");

            try {
                // 1. Fetch the UUIDs from the database for the given payment ID
                List<String> ticketUuids = passMapper.findUuidsByPaymentId(event.getPaymentID());
                if (ticketUuids == null || ticketUuids.isEmpty()) {
                    org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class).error("No ticket UUIDs found for paymentID {}, cannot send email.", event.getPaymentID());
                    return; // Stop if no UUIDs are found
                }

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username, "kPaymentPipeline Tickets"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject("Your Tickets for Payment: " + event.getPaymentID());

                // 2. Generate the HTML body with event details and QR codes
                String htmlContent = generateTicketDetailsHtml(event, ticketUuids);

                message.setContent(htmlContent, "text/html; charset=utf-8");
                Transport.send(message);
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class).info("Successfully sent ticket email to {}", recipient);
                break; // Exit loop on success
            } catch (MessagingException | UnsupportedEncodingException mex) {
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class).error("Failed to send ticket email to {}: {}", recipient, mex.getMessage(), mex);
                try {
                    Thread.sleep((long) (Math.pow(2, i) * 2000)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
