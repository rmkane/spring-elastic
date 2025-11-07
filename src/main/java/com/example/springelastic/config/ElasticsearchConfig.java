package com.example.springelastic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    
    @Value("${spring.data.elasticsearch.uris:https://localhost:9200}")
    private String uris;
    
    @Value("${spring.data.elasticsearch.username:elastic}")
    private String username;
    
    @Value("${spring.data.elasticsearch.password:}")
    private String password;
    
    @Value("${ES_CERT_PATH:}")
    private String certificatePath;
    
    @Override
    public ClientConfiguration clientConfiguration() {
        String[] hosts = Arrays.stream(uris.split(","))
                .map(uri -> uri.replace("https://", "").replace("http://", "").trim())
                .toArray(String[]::new);
        
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(hosts);
        
        // Configure SSL - always use SSL for HTTPS connections
        if (uris.startsWith("https://")) {
            try {
                SSLContext sslContext;
                
                if (StringUtils.hasText(certificatePath) && Files.exists(Paths.get(certificatePath))) {
                    // Use provided certificate
                    CertificateFactory factory = CertificateFactory.getInstance("X.509");
                    try (FileInputStream fis = new FileInputStream(certificatePath)) {
                        Certificate cert = factory.generateCertificate(fis);
                        
                        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                        trustStore.load(null, null);
                        trustStore.setCertificateEntry("ca", cert);
                        
                        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                            javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
                        tmf.init(trustStore);
                        
                        sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, tmf.getTrustManagers(), null);
                    }
                } else {
                    // Trust all certificates (for development)
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        }
                    }, new java.security.SecureRandom());
                }
                
                return builder.usingSsl(sslContext)
                        .withBasicAuth(username, password)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure SSL for Elasticsearch", e);
            }
        }
        
        return builder.withBasicAuth(username, password).build();
    }
}

