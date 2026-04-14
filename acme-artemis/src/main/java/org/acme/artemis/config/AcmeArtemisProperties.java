package org.acme.artemis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Bindings under {@code acme.artemis.*}. Matches defaults from
 * {@code acme-infra} Artemis image.
 */
@Data
@ConfigurationProperties(prefix = "acme.artemis")
public class AcmeArtemisProperties {

    /**
     * When false, auto-configuration does not register JMS beans (other apps can
     * supply their own).
     */
    private boolean enabled = true;

    /**
     * Artemis core URL, e.g. {@code tcp://localhost:61616}.
     */
    private String brokerUrl = "tcp://localhost:61616";

    private String user = "artemis";

    private String password = "artemis";

    /**
     * Optional JMS client id for durable subscriptions.
     */
    private String clientId;

    /**
     * Sessions cached by
     * {@link org.springframework.jms.connection.CachingConnectionFactory}.
     */
    private int sessionCacheSize = 10;

    /**
     * Listener concurrency, e.g. {@code 1} or {@code 3-10}.
     */
    private String concurrency = "1-5";
}
