
package com.saipbuilds.kpaymentpipeline.Email;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailParser {
    private static final Logger log = LoggerFactory.getLogger(EmailParser.class);

    // Re-used REGEX patterns from your original code
    private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("(pay_[A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("₹\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?91[\\-\\s]?)?(\\d{10})");

    private EmailParser() {}

    /**
     * Main entry point. Tries to parse HTML, falls back to Plain Text.
     */
    public static PaymentInfo parse(Message message) {
        // Tier 1: Attempt to parse HTML content
        try {
            String htmlContent = getTextFromMessagePart(message, "text/html");
            if (htmlContent != null && !htmlContent.isEmpty()) {
                PaymentInfo info = parseFromHtml(htmlContent, message);
                if (info != null && info.getPaymentId() != null && info.getAmount() != null) {
                    log.debug("Successfully parsed payment info from HTML for message {}", info.getMessageId());
                    return info; // Success from HTML
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse HTML content, will try plain text.", e);
        }

        // Tier 2: Fallback to parsing Plain Text content
        try {
            String plainTextContent = getTextFromMessagePart(message, "text/plain");
            if (plainTextContent != null && !plainTextContent.isEmpty()) {
                PaymentInfo info = parseFromPlainText(plainTextContent, message);
                if (info != null && info.getPaymentId() != null && info.getAmount() != null) {
                    log.debug("Successfully parsed payment info from Plain Text for message {}", info.getMessageId());
                    return info; // Success from Plain Text
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse plain text content.", e);
        }

        log.warn("Could not parse payment info from either HTML or Plain Text.");
        return null; // Both methods failed
    }

    /**
     * Orchestrator for parsing HTML content using Jsoup.
     */
    private static PaymentInfo parseFromHtml(String htmlContent, Message message) throws MessagingException {
        Document doc = Jsoup.parse(htmlContent);
        String subject = safeGetSubject(message);
        String messageId = safeGetMessageId(message);

        String paymentId = extractPaymentIdFromHtml(doc, subject);
        String amount = extractAmountFromHtml(doc, subject);

        if (paymentId == null && amount == null) return null;

        Instant paidOn = extractPaidOn(doc.text(), message);
        String payerEmail = extractPayerEmailFromHtml(doc, message);
        String phone = extractPhone(doc.text());
        String method = extractMethodFromHtml(doc);
        String merchantName = extractMerchant(doc);

        return new PaymentInfo(paymentId, normalizeAmount(amount), paidOn, payerEmail, phone, method, merchantName, subject, messageId);
    }

    /**
     * Orchestrator for parsing Plain Text content using Regex.
     */
    private static PaymentInfo parseFromPlainText(String textContent, Message message) throws MessagingException {
        String subject = safeGetSubject(message);
        String messageId = safeGetMessageId(message);
        String combinedText = textContent + " " + subject; // Include subject for better matching

        String paymentId = extractFromText(PAYMENT_ID_PATTERN, combinedText);
        String amountText = extractFromText(AMOUNT_PATTERN, combinedText);
        String amount = (amountText != null) ? amountText.replaceAll("[^0-9,.]", "") : null;

        if (paymentId == null && amount == null) return null;

        Instant paidOn = extractPaidOn(textContent, message);
        String payerEmail = extractFromText(EMAIL_PATTERN, textContent);
        String phone = extractFromText(PHONE_PATTERN, textContent);
        String method = extractMethodFromText(textContent);

        return new PaymentInfo(paymentId, normalizeAmount(amount), paidOn, payerEmail, phone, method, null, subject, messageId);
    }

    // =================================================================
    // =========== HELPER METHODS FOR EXTRACTION =======================
    // =================================================================

    private static String getTextFromMessagePart(Part part, String mimeType) throws MessagingException, IOException {
        if (part.isMimeType(mimeType)) {
            return part.getContent().toString();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String result = getTextFromMessagePart(multipart.getBodyPart(i), mimeType);
                if (result != null) return result;
            }
        }
        return null;
    }

    // --- HTML Specific Extractors ---
    private static String extractMerchant(Document doc) {
        Element e = doc.selectFirst("h2, .branding-content, .header h2, .title-content, .content-element");
        return e != null ? e.text().trim() : null;
    }

    private static String extractPaymentIdFromHtml(Document doc, String subject) {
        return extractFromText(PAYMENT_ID_PATTERN, doc.text() + " " + subject);
    }

    private static String extractAmountFromHtml(Document doc, String subject) {
        Element rupee = doc.selectFirst(".amount, .rupees, .symbol");
        if (rupee != null) return rupee.text();
        return extractFromText(AMOUNT_PATTERN, doc.text() + " " + subject);
    }

    private static String extractPayerEmailFromHtml(Document doc, Message message) throws MessagingException {
        Element emailEl = doc.selectFirst(".information-row:contains(Email) .value, .card:contains(Email) .value");
        if (emailEl != null) {
            return extractFromText(EMAIL_PATTERN, emailEl.text());
        }
        // Fallback to message headers if not in HTML body
        return extractPayerEmailFromMessage(message);
    }

    private static String extractMethodFromHtml(Document doc) {
        Element methodEl = doc.selectFirst(".information-row:contains(Method) .value, .information-row:contains(UPI)");
        if (methodEl != null) return methodEl.text();
        return extractMethodFromText(doc.text());
    }

    // --- Plain Text Specific Extractors ---
    private static String extractMethodFromText(String text) {
        String t = text.toLowerCase();
        if (t.contains("upi")) return "UPI";
        if (t.contains("card")) return "CARD";
        return null;
    }

    // --- Shared Extractors ---
    private static String extractPhone(String text) {
        return extractFromText(PHONE_PATTERN, text);
    }

    private static String extractFromText(Pattern pattern, String text) {
        if (text == null) return null;
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            // The amount pattern has two capturing groups, check both
            if (pattern == AMOUNT_PATTERN) {
                return m.group(1) != null ? m.group(1) : m.group(2);
            }
            // All other patterns use the first group
            return m.group(1);
        }
        return null;
    }

    private static Instant extractPaidOn(String textSource, Message message) {
        // This logic can be shared as it's primarily regex based
        // First try: look for "Paid On"
        int startIndex = textSource.toLowerCase().indexOf("paid on");
        if (startIndex != -1) {
            String afterMarker = textSource.substring(startIndex + "paid on".length()).trim();
            Instant parsed = tryParseDate(afterMarker.split("\\s+|,")[0]);
            if(parsed != null) return parsed;
        }

        // Second try: look for ISO date
        Matcher iso = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:?\\d{2})?)").matcher(textSource);
        if (iso.find()) {
            try { return Instant.parse(iso.group(1).replace(' ', 'T')); } catch (Exception ignored) {}
        }

        // Fallback: email sent date
        try {
            java.util.Date sent = message.getSentDate();
            if (sent != null) return sent.toInstant();
        } catch (MessagingException ignored) {}

        return Instant.now();
    }

    private static Instant tryParseDate(String dateStr) {
        // Simplified date parser, can be expanded
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dMMM,yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dMMMyyyy", Locale.ENGLISH)
        };
        for(DateTimeFormatter fmt : formatters) {
            try {
                return LocalDate.parse(dateStr, fmt).atStartOfDay(ZoneId.of("UTC")).toInstant();
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private static String extractPayerEmailFromMessage(Message message) throws MessagingException {
        javax.mail.Address[] froms = message.getFrom();
        if (froms != null && froms.length > 0) {
            String addr = ((InternetAddress) froms[0]).getAddress();
            if (addr != null && !addr.isBlank()) return addr.toLowerCase();
        }
        return null;
    }

    private static String safeGetSubject(Message m) {
        try { return m.getSubject(); } catch (MessagingException e) { return ""; }
    }

    static String safeGetMessageId(Message message) {
        try {
            String[] hdr = message.getHeader("Message-ID");
            if (hdr != null && hdr.length > 0) return hdr[0];
        } catch (Exception ignored) {}
        return "synth-" + Math.abs(message.hashCode()) + "-" + System.currentTimeMillis();
    }

    private static String normalizeAmount(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^0-9.]", "");
        if (cleaned.isBlank()) return null;
        if (!cleaned.contains(".")) cleaned = cleaned + ".00";
        return cleaned;
    }
}