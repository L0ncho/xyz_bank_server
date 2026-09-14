package cl.duoc.xyzbank.bffatm.shared.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bff-atm's mTLS connector")
class BffAtmTlsE2ETest {

    /*
     * Cases:
     * 1. Accepts a TLS handshake when the client presents the terminal certificate
     * 2. Refuses the connection when the client presents no certificate
     */

    private static final TrustManager TRUST_ALL = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("accepts the handshake when the client presents the terminal certificate")
    void acceptsHandshakeWithTerminalCertificate() throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        try (InputStream keystoreStream =
                getClass().getClassLoader().getResourceAsStream("tls/terminal-keystore.p12")) {
            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            clientKeyStore.load(keystoreStream, "xyzbank-dev".toCharArray());
            keyManagerFactory.init(clientKeyStore, "xyzbank-dev".toCharArray());
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] {TRUST_ALL}, new SecureRandom());

        String response = requestHealthOver(sslContext.getSocketFactory());

        assertTrue(response.contains("200"), "expected a 200 response, got: " + response);
    }

    @Test
    @DisplayName("refuses the connection when the client presents no certificate")
    void refusesConnectionWithNoCertificate() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {TRUST_ALL}, new SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();

        // TLS 1.3 can complete the initial handshake before the server has actually
        // verified the peer certificate, so the rejection may only surface once the
        // client tries to exchange application data (the request or the response),
        // not necessarily at startHandshake() itself.
        assertThrows(Exception.class, () -> requestHealthOver(factory));
    }

    private String requestHealthOver(SSLSocketFactory factory) throws Exception {
        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port)) {
            socket.startHandshake();
            socket.getOutputStream()
                    .write("GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes());
            socket.getOutputStream().flush();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.readLine();
            }
        }
    }
}
