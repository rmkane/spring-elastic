package org.acme.artemis.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

class AcmeArtemisPropertiesBindingTest {

    @Test
    void bindsAcmeArtemisPrefix() {
        var env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        sources.addFirst(
                new MapPropertySource(
                        "test",
                        Map.of(
                                "acme.artemis.broker-url",
                                "tcp://broker.example:61616",
                                "acme.artemis.user",
                                "u",
                                "acme.artemis.password",
                                "p",
                                "acme.artemis.concurrency",
                                "2")));

        Binder binder = Binder.get(env);
        AcmeArtemisProperties props = binder.bindOrCreate("acme.artemis", AcmeArtemisProperties.class);

        assertThat(props.getBrokerUrl()).isEqualTo("tcp://broker.example:61616");
        assertThat(props.getUser()).isEqualTo("u");
        assertThat(props.getPassword()).isEqualTo("p");
        assertThat(props.getConcurrency()).isEqualTo("2");
    }
}
