package org.acme.artemis.consumer.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoMessageController {

    private final JmsTemplate jmsTemplate;

    @Value("${acme.jms.demo-queue:acme.demo.queue}")
    private String demoQueue;

    public DemoMessageController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @PostMapping(path = "/messages", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> send(@RequestBody String body) {
        jmsTemplate.convertAndSend(demoQueue, body);
        return ResponseEntity.accepted().build();
    }
}
