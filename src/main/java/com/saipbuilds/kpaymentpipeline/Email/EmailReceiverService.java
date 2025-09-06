
package com.saipbuilds.kpaymentpipeline.Email;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentInfo;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Redis.RedisPaymentStore;
import com.saipbuilds.kpaymentpipeline.Kafka.Producers.EventProducer;
import com.sun.mail.imap.IMAPFolder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.saipbuilds.kpaymentpipeline.Email.EmailParser.safeGetMessageId;

@Service
public class EmailReceiverService {
    private ScheduledFuture<?> sweepFuture;
    private static final Logger log = LoggerFactory.getLogger(EmailReceiverService.class);

    private final EmailProperties props;
    private final TokenProvider tokenProvider;
    private final RedisPaymentStore redisPaymentStore;
    private final EmailProcessedStoreProperties processedProps;
    private final Session session;
    private final RedisTemplate<String, Object> redisTemplate; // used for heartbeat
    private final EventProducer eventProducer;

    private volatile Store store;
    private volatile IMAPFolder inbox;

    private final ScheduledExecutorService keepAliveScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "imap-keepalive"));
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "email-heartbeat"));
    private final ScheduledExecutorService sweepScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "email-sweep"));
    private final ExecutorService workerPool = Executors.newFixedThreadPool(6, r -> new Thread(r, "email-worker-"));
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledFuture<?> keepAliveFuture;
    private ScheduledFuture<?> heartbeatFuture;

    // single listener instance
    private final MessageCountAdapter messageListener = new MessageCountAdapter() {
        @Override
        public void messagesAdded(MessageCountEvent event) {
            Message[] msgs = event.getMessages();
            log.info("IDLE: {} new messages", msgs.length);
            for (Message m : msgs) workerPool.submit(() -> safeHandle(m));
        }
    };

    // business TTL and processed TTL
    private final Duration businessKeyTtl = Duration.ofMinutes(20);
    private final int processedKeyTtlSeconds;

    public EmailReceiverService(EmailProperties props,
                                TokenProvider tokenProvider,
                                RedisPaymentStore redisPaymentStore,
                                EmailProcessedStoreProperties processedProps,
                                Session session,
                                RedisTemplate<String, Object> redisTemplate,
                                EventProducer eventProducer) {
        this.eventProducer = eventProducer;
        this.props = props;
        this.tokenProvider = tokenProvider;
        this.redisPaymentStore = redisPaymentStore;
        this.processedProps = processedProps;
        this.session = session;
        this.redisTemplate = redisTemplate;
        this.processedKeyTtlSeconds = (int) processedProps.getProcessedMessageTtlSeconds();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("EmailReceiverService already running");
            return;
        }
        Thread t = new Thread(this::mainLoop, "EmailReceiverService-Loop");
        t.setDaemon(true);
        t.start();
    }

    private void mainLoop() {
        int backoff = props.getIdleReconnectBackoffSeconds();
        while (running.get()) {
            try {
                scheduleSweep();
                connectAndOpenInbox();
                scheduleHeartbeat();
                try { inbox.removeMessageCountListener(messageListener); } catch (Exception ignored) {}
                inbox.addMessageCountListener(messageListener);
                scheduleKeepAlive();

                while (running.get() && store != null && store.isConnected() && inbox != null && inbox.isOpen()) {
                    try {
                        log.debug("Entering IMAP IDLE");
                        inbox.idle();
                        log.debug("IDLE returned");
                        backoff = props.getIdleReconnectBackoffSeconds();
                    } catch (FolderClosedException | StoreClosedException fce) {
                        log.warn("Folder/Store closed, reconnecting", fce);
                        break;
                    } catch (MessagingException me) {
                        log.warn("Messaging exception during IDLE, reconnecting", me);
                        break;
                    }
                }
            } catch (Exception ex) {
                log.error("Unexpected error in IMAP main loop", ex);
            } finally {
                safeCloseFolder();
                safeCloseStore();
                try {
                    int wait = Math.min(props.getIdleReconnectMaxBackoffSeconds(), backoff);
                    log.info("Reconnect backoff {}s", wait);
                    TimeUnit.SECONDS.sleep(wait);
                    backoff = Math.min(props.getIdleReconnectMaxBackoffSeconds(), backoff * 2);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        shutdownExecutors();
        log.info("EmailReceiverService stopped.");
    }

    private void connectAndOpenInbox() throws MessagingException {
        if (session == null) throw new IllegalStateException("Session bean not injected");

        if (store != null && store.isConnected() && inbox != null && inbox.isOpen()) return;

        log.info("Connecting to IMAP {}:{}", props.getHost(), props.getPort());
        store = session.getStore(props.getProtocol());

        if (props.isUseOauth2()) {
            String token = tokenProvider.getAccessToken();
            if (token == null) {
                log.warn("OAuth2 requested but token not available; falling back to password auth");
                store.connect(props.getHost(), props.getPort(), props.getUsername(), props.getPassword());
            } else {
                session.getProperties().put("mail.imaps.sasl.enable", "true");
                session.getProperties().put("mail.imaps.sasl.mechanisms", "XOAUTH2");
                store.connect(props.getHost(), props.getPort(), props.getUsername(), token);
            }
        } else {
            store.connect(props.getHost(), props.getPort(), props.getUsername(), props.getPassword());
        }

        Folder f = store.getFolder("INBOX");
        if (f == null || !f.exists()) throw new MessagingException("INBOX not found");
        inbox = (IMAPFolder) f;
        inbox.open(Folder.READ_WRITE);
        log.info("INBOX opened");
    }

    private void scheduleKeepAlive() {
        if (keepAliveFuture != null && !keepAliveFuture.isDone()) keepAliveFuture.cancel(true);
        keepAliveFuture = keepAliveScheduler.scheduleAtFixedRate(() -> {
            try {
                if (inbox != null && inbox.isOpen()) {
                    log.debug("Keepalive NOOP");
                    inbox.doCommand(protocol -> { protocol.simpleCommand("NOOP", null); return null; });
                }
            } catch (Exception e) {
                log.warn("Keepalive failed", e);
            }
        }, props.getKeepAliveFreqMillis(), props.getKeepAliveFreqMillis(), TimeUnit.MILLISECONDS);
    }

    private void scheduleHeartbeat() {
        log.warn("Heartbeat scheduler started");
        if (heartbeatFuture != null && !heartbeatFuture.isDone()) heartbeatFuture.cancel(true);
        long ttlSeconds = 90;
        heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                StringBuilder issues = new StringBuilder();

                if (keepAliveScheduler.isShutdown()) issues.append("keepAliveScheduler, ");
                if (heartbeatScheduler.isShutdown()) issues.append("heartbeatScheduler, ");
                if (sweepScheduler.isShutdown()) issues.append("sweepScheduler, ");
                if (workerPool.isShutdown()) issues.append("workerPool, ");
                if (inbox == null) issues.append("inbox=null, ");
                else if (!inbox.isOpen()) issues.append("inbox not open, ");
                if (store == null) issues.append("store=null, ");
                else if (!store.isConnected()) issues.append("store not connected, ");
                if (!running.get()) issues.append("service not running, ");

                if (issues.isEmpty()) {
                    String hbKey = "email-listener:heartbeat";
                    String value = Instant.now().toString();
                    redisTemplate.opsForValue().set(hbKey, value, Duration.ofSeconds(ttlSeconds));
                    log.debug("Heartbeat written to Redis");
                } else {
                    log.warn("Heartbeat not written: service not fully running. Issues: {}", issues.toString());
                }
            } catch (Exception e) {
                log.warn("Failed to write heartbeat to Redis", e);
            }
        }, 5, 60, TimeUnit.SECONDS);
    }

    private void safeHandle(Message msg) {
        try { handleIncoming(msg); } catch (Exception e) { log.error("Failed to handle message", e); }
    }

    private void handleIncoming(Message message) {
        String messageId = null;
        try {
            messageId = safeGetMessageId(message);
            if (messageId == null) {
                messageId = "synth-" + Math.abs(message.hashCode()) + "-" + System.currentTimeMillis();
            }

            if (redisPaymentStore.isProcessed(messageId)) {
                log.debug("Message already processed in Redis (mid={}), skipping.", messageId);
                return;
            }

            PaymentInfo info = EmailParser.parse(message);
            if (info == null) {
                log.info("Could not parse payment info (mid={})", messageId);
                return;
            }

            if (info.getMessageId() == null || info.getMessageId().isBlank()) {
                info.setMessageId(messageId);
            } else {
                messageId = info.getMessageId();
            }

            boolean claimed = redisPaymentStore.savePaymentAtomic(info, businessKeyTtl(), processedKeyTtlSeconds);
            if (claimed) {
                log.info("Claimed and saved payment {} (mid={})", info.getPaymentId(), messageId);

                ReceiptReceivedEvent event = ReceiptReceivedEvent.builder()
                        .emailID(info.getPayerEmail())
                        .paymentID(info.getPaymentId())
                        .amount(info.getAmount())
                        .method(info.getMethod())
                        .phoneNumber(info.getPhone())
                        .paidOn(info.getPaidOn().toString())
                        .build();

                // Wait synchronously for Kafka ack
                try {
                    eventProducer.sendReceiptEvent(event);
                    message.setFlag(Flags.Flag.SEEN, true); // only after Kafka ack
                    log.info("Kafka ACK received, marked SEEN mid={}", messageId);
                } catch (Exception e) {
                    log.error("Kafka send failed for mid={}, leaving in INBOX", messageId, e);
                }
            } else {
                log.info("Payment {} already claimed by another instance (mid={})", info.getPaymentId(), messageId);
            }

        } catch (Exception ex) {
            log.error("Error processing incoming message mid=" + messageId, ex);
        }
    }

    private Duration businessKeyTtl() { return businessKeyTtl; }

    // Sweep all messages without cutoff
    private void scheduleSweep() {
        if (sweepFuture != null && !sweepFuture.isDone()) return;
        log.info("Is sweep scheduler shut down? {}", sweepScheduler.isShutdown());
        log.info("Scheduling Redis-based sweep for all messages in the last 24 hours.");
        int sweepIntervalMinutes = Integer.parseInt(System.getenv("SCHEDULED_SWEEP_TIME"));
        sweepFuture = sweepScheduler.scheduleAtFixedRate(() -> {
            try {
                if (inbox != null && inbox.isOpen()) {
                    log.debug("Running Redis-based sweep...");

                    // 1. Define the time cutoff for the search (24 hours ago)
                    Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
                    Date cutoffDate = Date.from(cutoff);

                    // 2. Create search criteria: Received AFTER the cutoff date
                    SearchTerm recentTerm = new ReceivedDateTerm(ComparisonTerm.GT, cutoffDate); // <-- MODIFIED: Only this term is needed

                    // 3. Execute the search on the server for ALL recent messages
                    Message[] messages = inbox.search(recentTerm); // <-- MODIFIED: No longer using the combined term

                    if (messages.length > 0) {
                        log.info("Sweep found {} recent messages to check against Redis.", messages.length);
                    } else {
                        log.debug("Sweep found no recent messages.");
                    }
                    // The rest of the logic remains the same, but now operates on a much smaller list
                    for (Message m : messages) {
                        try {
                            if (m.isExpunged()) continue;

                            // This fetch is now much cheaper as it's only for a few messages
                            m.getFolder().fetch(new Message[]{m}, new FetchProfile() {{
                                add(Item.ENVELOPE);
                                add("Message-ID");
                            }});

                            String messageId = safeGetMessageId(m);
                            if (messageId == null) continue;

                            if (!redisPaymentStore.isProcessed(messageId)) {
                                log.info("Sweep found unprocessed message mid={}, submitting for handling", messageId);
                                try {
                                    Message fullMessage = inbox.getMessage(m.getMessageNumber());
                                    workerPool.submit(() -> safeHandle(fullMessage));
                                } catch (MessageRemovedException mre) {
                                    log.debug("Message was expunged before full fetch: mid={}", messageId);
                                }
                            }
                        } catch (MessageRemovedException mre) {
                            log.debug("Message already expunged, skipping: msgNum={}", m.getMessageNumber());
                        } catch (Exception innerEx) {
                            log.warn("Error while checking message in sweep", innerEx);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error during Redis-based sweep", e);
            }
        }, 1, sweepIntervalMinutes, TimeUnit.MINUTES);
    }

    // Disabled: we no longer move mails out of INBOX
    private void moveToFolder(Message msg, String folderName) {
        // intentionally no-op: we now keep all mails in INBOX for safety
    }

    private void safeCloseFolder() {
        try {
            if (inbox != null) {
                try { inbox.removeMessageCountListener(messageListener); } catch (Exception ignored) {}
                if (inbox.isOpen()) inbox.close(true);
            }
        } catch (Exception e) { log.warn("Error closing inbox", e); } finally { inbox = null; }
    }

    private void safeCloseStore() {
        try { if (store != null && store.isConnected()) store.close(); } catch (Exception e) { log.warn("Error closing store", e); } finally { store = null; }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        safeCloseFolder();
        safeCloseStore();
        try { if (keepAliveFuture != null) keepAliveFuture.cancel(true); } catch (Exception ignored) {}
        try { if (heartbeatFuture != null) heartbeatFuture.cancel(true); } catch (Exception ignored) {}
        shutdownExecutors();
    }

    private void shutdownExecutors() {
        try { keepAliveScheduler.shutdownNow(); } catch (Exception ignored) {}
        try { heartbeatScheduler.shutdownNow(); } catch (Exception ignored) {}
        try { sweepScheduler.shutdownNow(); } catch (Exception ignored) {}
        try {
            workerPool.shutdown();
            if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) workerPool.shutdownNow();
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("keepAliveScheduler", !keepAliveScheduler.isShutdown());
        status.put("heartbeatScheduler", !heartbeatScheduler.isShutdown());
        status.put("sweepScheduler", !sweepScheduler.isShutdown());
        status.put("workerPool", !workerPool.isShutdown());
        status.put("inbox", inbox != null && inbox.isOpen());
        status.put("store", store != null && store.isConnected());
        status.put("running", running.get());
        return status;
    }
}

