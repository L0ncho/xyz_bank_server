package cl.duoc.xyzbank.coreservice.auth.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * core-service is plain HTTP on its primary connector; PIN verification is the sole
 * exception (see channel-transport-security), so it gets its own TLS-only connector
 * here rather than switching the whole service to HTTPS.
 */
@Configuration
public class PinVerificationTlsConfig {

    @Value("${security.pin-verification-tls.port}")
    private int port;

    @Value("${security.pin-verification-tls.keystore-path}")
    private String keystorePath;

    @Value("${security.pin-verification-tls.keystore-password}")
    private String keystorePassword;

    @Value("${security.pin-verification-tls.keystore-type}")
    private String keystoreType;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> pinVerificationConnectorCustomizer() {
        return factory -> factory.addAdditionalTomcatConnectors(pinVerificationConnector());
    }

    private Connector pinVerificationConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(port);
        connector.setScheme("https");
        connector.setSecure(true);

        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);

        SSLHostConfig sslHostConfig = new SSLHostConfig();
        SSLHostConfigCertificate certificate =
                new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
        certificate.setCertificateKeystoreFile(keystorePath);
        certificate.setCertificateKeystorePassword(keystorePassword);
        certificate.setCertificateKeystoreType(keystoreType);
        sslHostConfig.addCertificate(certificate);
        protocol.addSslHostConfig(sslHostConfig);

        return connector;
    }
}
