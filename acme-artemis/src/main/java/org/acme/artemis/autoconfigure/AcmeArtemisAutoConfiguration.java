package org.acme.artemis.autoconfigure;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import org.acme.artemis.config.AcmeArtemisProperties;

/**
 * Registers a pooled {@link ConnectionFactory}, {@link JmsTemplate}, and the
 * default {@link DefaultJmsListenerContainerFactory} for
 * {@link org.springframework.jms.annotation.JmsListener}.
 */
@AutoConfiguration(before = JmsAutoConfiguration.class)
@ConditionalOnClass({ ActiveMQJMSConnectionFactory.class, ConnectionFactory.class })
@EnableConfigurationProperties(AcmeArtemisProperties.class)
@ConditionalOnProperty(prefix = "acme.artemis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AcmeArtemisAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ConnectionFactory.class)
    public ConnectionFactory connectionFactory(AcmeArtemisProperties properties) {
        var target = new ActiveMQJMSConnectionFactory(
                properties.getBrokerUrl(), properties.getUser(), properties.getPassword());
        if (properties.getClientId() != null && !properties.getClientId().isBlank()) {
            target.setClientID(properties.getClientId());
        }
        var caching = new CachingConnectionFactory(target);
        caching.setSessionCacheSize(properties.getSessionCacheSize());
        return caching;
    }

    @Bean
    @ConditionalOnMissingBean(JmsTemplate.class)
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "jmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory, AcmeArtemisProperties properties) {
        var factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        factory.setConcurrency(properties.getConcurrency());
        return factory;
    }
}
