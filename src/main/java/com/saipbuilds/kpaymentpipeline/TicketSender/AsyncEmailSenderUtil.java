package com.saipbuilds.kpaymentpipeline.TicketSender;


import com.saipbuilds.kpaymentpipeline.DTO.HackathonReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.SlotDetail;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.SlotMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.EventMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
// AsyncEmailSenderUtil is a functions class for sending emails asynchronously
@Service
public class AsyncEmailSenderUtil {

    private static Transport mailTransport = null;
    private static Session session = null;
    private static final int MAX_ATTEMPTS = 3; // Increased for robustness
    private final SlotMapper slotMapper;
    private final EventMapper eventMapper;

    private final PassMapper passMapper;

    // PassMapper is injected to fetch UUIDs for QR codes
    public AsyncEmailSenderUtil(SlotMapper slotMapper, EventMapper eventMapper, PassMapper passMapper) {
        this.slotMapper = slotMapper;
        this.eventMapper = eventMapper;
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

            // Map slotNo → eventId
            Map<Integer, Integer> ticketDetails = slotDetails.stream()
                    .collect(Collectors.toMap(SlotDetail::getSlotNo, SlotDetail::getEventId));

            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data="
                    + URLEncoder.encode(ticketUuid, StandardCharsets.UTF_8);

            ticketsHtml.append(String.format("""
    <div style="background:#f9f9f9; border:1px solid #ddd; border-radius:12px; padding:20px; margin-bottom:20px; text-align:center;">
        <h3 style="color:#0055A4; font-size:1.4rem; margin-top:0; margin-bottom:15px;">Ticket #%d</h3>
        <img src="%s" alt="QR Code for Ticket %s" style="margin-bottom:15px;">
        <p style="font-size:14px; color:#555; margin-bottom:10px;">Ticket ID: %s</p>

        <div style="border:1px solid #ddd; border-radius:8px; overflow:hidden; display:inline-block; max-width:100%%;">
            <table style="width:100%%; text-align:left; font-size:15px; color:#333; border-collapse:separate; border-spacing:0;">
                <thead style="background:#f5f7fa;">
                    <tr>
                        <th style="padding:8px; border-bottom:1px solid #ddd; text-align:left;">Slot No.</th>
                        <th style="padding:8px; border-bottom:1px solid #ddd; text-align:left;">Event ID</th>
                        <th style="padding:8px; border-bottom:1px solid #ddd; text-align:left;">Event Name</th>
                        <th style="padding:8px; border-bottom:1px solid #ddd; text-align:left;word-break:break-word; white-space:normal;">Department</th>
                    </tr>
                </thead>
                <tbody>
    """, ticketCounter++, qrCodeUrl, ticketUuid, ticketUuid));


            for (Map.Entry<Integer, Integer> slotEntry : ticketDetails.entrySet()) {
                int slotNo = slotEntry.getKey();
                int eventId = slotEntry.getValue();
                String eventName = null;
                String eventDept = null;
                try {
                    // Fetch event name using EventMapper
                    eventName = eventMapper.findEventNameById(eventId);
                    eventDept = eventMapper.findDeptNameFromEventId(eventId);
                } catch (Exception e) {
                    eventName = "Unknown Event";
                    eventDept = "Unknown Dept";
                }

                ticketsHtml.append(String.format(
                        "<tr><td style='padding:8px; border-bottom:1px solid #eee;'>%d</td>"
                                + "<td style='padding:8px; border-bottom:1px solid #eee;'>%d</td>"
                                + "<td style='padding:8px; border-bottom:1px solid #eee;'>%s</td>"
                                + "<td style='padding:8px; border-bottom:1px solid #eee;'>%s</td></tr>",
                        slotNo, eventId, eventName, eventDept
                ));
            }
            ticketsHtml.append("</table></div>");
        }

        return String.format("""
                        <div style='background:linear-gradient(135deg,#f0f5ff 0%%,#ffffff 100%%);padding:40px 0;font-family:Segoe UI,Arial,sans-serif;'>
                            <div style='max-width:600px;margin:auto;background:#FFFFFF;border-radius:18px;box-shadow:0 6px 30px rgba(0,0,0,0.1);padding:40px 32px;'>
                                <img src="%s" alt="Event Logo" style="display:block; margin-left:auto; margin-right:auto; max-width:160px; margin-bottom:25px;">
                                <div style='text-align:center;margin-bottom:24px;'>
                                    <h2 style='color:#0055A4;font-size:2.2rem;margin:0 0 8px 0;'>Your Invente 2025 Tickets Are Here!</h2>
                                    <p style='font-size:18px;color:#333;margin-bottom:20px;'>Hello %s, thank you for your purchase!</p>
                                </div>
                        
                                <div style="border-top:1px solid #ddd; border-bottom:1px solid #ddd; padding: 20px 0; margin-bottom:25px;">
                                     <p style='font-size:16px;color:#333;margin-bottom:12px; line-height:1.6;'>
                                        <strong>Payment ID:</strong> %s<br>
                                        <strong>Amount Paid:</strong> %s<br>
                                        <strong>Payment Method:</strong> %s<br>
                                        <strong>Date and Time:</strong> %s
                                    </p>
                                </div>
                        
                                %s
                        
                                <p style='color:#999;font-size:12px;text-align:center;margin-top:30px;'>
                                    Sent by <strong>kPaymentPipeline</strong>, built by\s
                                    <a href="https://www.linkedin.com/in/saipranav-m/" target="_blank" style="color:#0055A4;text-decoration:none;">
                                        Saipranav
                                    </a>and his late night Cup(s) of Tea :). Please keep this E-Mail safe && present the QR code at the event.
                                </p>
                            </div>
                        </div>
""",
                "https://ssnsnucinvente.com/logos/invente.png",
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
                message.setFrom(new InternetAddress(username, "Invente Tickets"));
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


    private String generateHackathonTicketHtml(HackathonReconciledEvent event, List<String> ticketUuids) {
        StringBuilder membersHtml = new StringBuilder();

        if (event.getMember_names() != null) {
            for (Integer idx : event.getMember_names().keySet()) {
                membersHtml.append(String.format(
                        "<tr>" +
                                "<td>%s</td>" +
                                "<td>%s</td>" +
                                "<td>%s</td>" +
                                "<td>%s</td>" +
                                "</tr>",
                        event.getMember_names().get(idx),
                        event.getMember_emails().get(idx),
                        event.getMember_institutions().get(idx),
                        event.getMember_phones().get(idx)
                ));
            }
        }

        // Only one QR for the team (use first element if multiple exist)
        String qrCodeUrl = (ticketUuids != null && !ticketUuids.isEmpty())
                ? "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + ticketUuids.get(0)
                : "";

        return String.format("""
            <div style='font-family:Segoe UI,Arial,sans-serif; background:#f9f9f9; padding:20px;'>
            <img src="%s" alt="Event Logo" style="display:block; margin-left:auto; margin-right:auto; max-width:160px; margin-bottom:25px;">
                <h2 style='color:#0055A4;'>Your HackInfinity 2025 Registrations have been Confirmed!</h2>
                <p>Hello <b>%s</b>, your team <b>%s</b> has successfully registered for the hackathon.</p>
                <p><strong>Track:</strong> %s</p>
                <p><strong>Payment ID:</strong> %s</p>
                <p><strong>Payment Method:</strong> %s</p>
                <p><strong>Amount:</strong> %s</p>
                <p><strong>Date:</strong> %s</p>
                <div style='text-align:center; margin:20px 0;'>
                    <p><strong>Team Ticket QR Code:</strong></p>
                    %s
                </div>
                
                <h3>Team Members:</h3>
                <table style='width:100%%; border-collapse:collapse;' border='1'>
                    <tr><th>Name</th><th>Email</th><th>Institution</th><th>Phone</th></tr>
                    %s
                </table>
                
                 <p style='color:#999;font-size:12px;text-align:center;margin-top:30px;'>
            Sent by <strong>kPaymentPipeline</strong>, built by 
            <a href="https://www.linkedin.com/in/saipranav-m/" target="_blank" style="color:#0055A4;text-decoration:none;">
                Saipranav
            </a> and his late night Coffee Cup(s) :). Please keep this E-Mail safe && present the QR code at the event.
        </p>
            </div>
            """,
                "https://ssnsnucinvente.com/logos/invente.png",
                event.getLeader_email(),
                event.getTeam_name(),
                event.getTrack(),
                event.getPaymentID(),
                event.getMethod(),
                event.getAmount(),
                event.getPaidOn(),
                (qrCodeUrl.isEmpty() ? "" : "<img src='" + qrCodeUrl + "' alt='Team QR Code' width='220' height='220'>"),
                membersHtml
        );
    }


    @Async
    public void sendHackathonTicketEmail(String recipient, HackathonReconciledEvent event, List<String> ticketUuids) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            if (mailTransport == null || !mailTransport.isConnected()) {
                initEmailProperties();
            }

            String username = System.getenv("MAIL_ID");

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username, "HackInfinity Team Tickets"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject("HackInfinity Ticket Confirmation - Team: " + event.getTeam_name());

                // Generate HTML including QR codes
                String htmlContent = generateHackathonTicketHtml(event, ticketUuids);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                org.slf4j.LoggerFactory.getLogger(emailUtil.class)
                        .info("Successfully sent hackathon ticket email to {}", recipient);
                break;
            } catch (MessagingException | UnsupportedEncodingException e) {
                org.slf4j.LoggerFactory.getLogger(emailUtil.class)
                        .error("Failed to send hackathon ticket email to {}", recipient, e);
                try {
                    Thread.sleep((long) (Math.pow(2, i) * 2000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }



    private String generateBundledPaperPublicationHtml(Map<String, String> eventIdToName, Map<String, String> eventIdToForm) {
        StringBuilder rows = new StringBuilder();

        for (String eventId : eventIdToName.keySet()) {
            String eventName = eventIdToName.get(eventId);
            String gform = eventIdToForm.getOrDefault(eventId, "Link will be shared shortly");
            rows.append(String.format("""
            <tr>
                <td style="padding:10px; border:1px solid #ddd;">%s</td>
                <td style="padding:10px; border:1px solid #ddd;">%s</td>
                <td style="padding:10px; border:1px solid #ddd; text-align:center;">
                    <a href="%s" target="_blank" style="color:#0055A4; font-weight:bold;">Submit Abstract</a>
                </td>
            </tr>
        """, eventName, eventId, gform));
        }

        return String.format("""
        <div style="font-family:Segoe UI,Arial,sans-serif; background:#ffffff; padding:30px; border-radius:12px; border:1px solid #eee;">
            <h2 style="color:#0055A4; text-align:center;">Paper Presentation Abstract Submissions</h2>
            <p style="font-size:15px; color:#444; text-align:center;">
                Hello, you have registered for one or more paper presentation events.<br>
                Please find the submission forms below:
            </p>
            <table style="width:100%%; border-collapse:collapse; margin-top:20px; font-size:14px;">
                <thead>
                    <tr style="background:#f0f5ff;">
                        <th style="padding:10px; border:1px solid #ddd;">Event Name</th>
                        <th style="padding:10px; border:1px solid #ddd;">Event ID</th>
                        <th style="padding:10px; border:1px solid #ddd;">Submission Link</th>
                    </tr>
                </thead>
                <tbody>
                    %s
                </tbody>
            </table>
            <p style="font-size:13px; color:#999; text-align:center; margin-top:20px;">
                Sent by <strong>kPaymentPipeline</strong> · Please submit your abstracts before the deadline. Built by 
            <a href="https://www.linkedin.com/in/saipranav-m/" target="_blank" style="color:#0055A4;text-decoration:none;">
                Saipranav
            </a>
            </p>
        </div>
    """, rows);
    }


    @Async
    public void sendBundledPaperPublicationEmail(String recipient, Map<String, String> eventIdToName, Map<String, String> eventIdToForm) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            if (mailTransport == null || !mailTransport.isConnected()) {
                initEmailProperties();
            }

            String username = System.getenv("MAIL_ID");

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username, "Invente Paper Presentation"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject("Paper Presentation Abstract Submission Links");

                String htmlContent = generateBundledPaperPublicationHtml(eventIdToName, eventIdToForm);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class)
                        .info("Successfully sent bundled Paper Publication email to {}", recipient);
                break;
            } catch (MessagingException | UnsupportedEncodingException e) {
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class)
                        .error("Failed to send bundled Paper Publication email to {}", recipient, e);
                try {
                    Thread.sleep((long) (Math.pow(2, i) * 2000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }






    /**
     * Sends a secure OTP email with liability disclaimer.
     *
     * @param recipient Recipient's email address.
     * @param otp The one-time password to send.
     */
    @Async
    public void sendOtpEmail(String recipient, String otp) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            if (mailTransport == null || !mailTransport.isConnected()) {
                initEmailProperties();
            }

            String username = System.getenv("MAIL_ID");

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username, "Invente Security"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject("Your Invente OTP - DO NOT SHARE");

                String htmlContent = String.format("""
                        <div style='font-family:Segoe UI,Arial,sans-serif; background:#ffffff; padding:30px; border-radius:12px; border:1px solid #eee;'>
                            <h2 style='color:#d32f2f;'>Your One-Time Password (OTP)</h2>
                            <p style='font-size:16px; color:#333;'>Dear user,</p>
                            <p style='font-size:18px; color:#000;'><strong>%s</strong></p>
                            <p style='font-size:14px; color:#555;'>Please use the above OTP to complete your verification. This OTP is valid for a limited time only.</p>
                            <hr style='margin:20px 0;'>
                            <p style='font-size:13px; color:#444; line-height:1.6;'>
                                <strong>Strict Notice:</strong><br>
                                - You are <strong>liable</strong> for any payments collected using an Invente account bound to this email.<br>
                                - All transactions are <strong>monitored and tracked</strong> for security and compliance.<br>
                                - DO NOT share this OTP with anyone. Our team will <strong>never</strong> ask you for your OTP.
                            </p>
                            <p style='font-size:12px; color:#999; text-align:center; margin-top:20px;'>
                                Sent securely by Invente 2025 Systems. Built by 
            <a href="https://www.linkedin.com/in/saipranav-m/" target="_blank" style="color:#0055A4;text-decoration:none;">
                Saipranav
            </a>
                            </p>
                        </div>
                        """, otp);

                message.setContent(htmlContent, "text/html; charset=utf-8");
                Transport.send(message);
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class)
                        .info("Successfully sent OTP email to {}", recipient);
                break;
            } catch (MessagingException | UnsupportedEncodingException e) {
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class)
                        .error("Failed to send OTP email to {}", recipient, e);
                try {
                    Thread.sleep((long) (Math.pow(2, i) * 2000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }



}
