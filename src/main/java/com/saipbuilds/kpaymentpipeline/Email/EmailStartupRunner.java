package com.saipbuilds.kpaymentpipeline.Email;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EmailStartupRunner implements ApplicationRunner {

    private final EmailReceiverService receiver;

    public EmailStartupRunner(EmailReceiverService receiver) {
        this.receiver = receiver;
    }

    @Override
    public void run(ApplicationArguments args) {
        receiver.start();
    }
}
