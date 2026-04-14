package org.acme.artemis.consumer.jms;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemoMessageListener {

    @JmsListener(destination = "${acme.jms.demo-queue:acme.demo.queue}")
    public void onMessage(String body) {
        log.info("Received: {}", body);
    }
}
