package cl.duoc.xyzbank.bffatm.shared.infrastructure.rest;

import jakarta.servlet.http.HttpServletRequest;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Resolves the ATM terminal's identity from the client certificate presented over
 * {@code bff-atm}'s mTLS connector (server.ssl.client-auth: need) -- the terminal's common
 * name is the terminal identifier used throughout the ATM channel (session issuance,
 * caller-context resolution).
 */
public final class ClientCertificateTerminalResolver {

    private static final String CERTIFICATE_REQUEST_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private ClientCertificateTerminalResolver() {
    }

    public static Optional<String> resolveTerminalId(HttpServletRequest request) {
        Object attribute = request.getAttribute(CERTIFICATE_REQUEST_ATTRIBUTE);
        if (!(attribute instanceof X509Certificate[] certificates) || certificates.length == 0) {
            return Optional.empty();
        }
        return commonNameOf(certificates[0]);
    }

    private static Optional<String> commonNameOf(X509Certificate certificate) {
        try {
            LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName());
            return ldapName.getRdns().stream()
                    .filter(rdn -> rdn.getType().equalsIgnoreCase("CN"))
                    .map(Rdn::getValue)
                    .map(Object::toString)
                    .findFirst();
        } catch (InvalidNameException exception) {
            return Optional.empty();
        }
    }
}
