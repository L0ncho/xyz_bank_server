package cl.duoc.xyzbank.sharedsecurity.callercontext;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PinHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String pin) {
        return encoder.encode(pin);
    }

    public boolean matches(String pin, String hash) {
        return encoder.matches(pin, hash);
    }
}
