package com.loanmust.loan.common;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns sequential database ids into short, opaque, non-guessable tokens for use in URLs
 * (so {@code /applications/27} becomes {@code /applications/Kf3aB9x2Q}) and back again. Reversible
 * and deterministic: the same id always maps to the same token.
 *
 * The scheme is a keyed 42-bit multiplicative permutation (invertible mod 2^42) plus a salt,
 * rendered in base62 with a leading letter so an encoded token is never all-digits — which
 * lets the URL layer tell a real token from a plain numeric id and stay backward compatible.
 */
@Component
public class IdCodec {

    private static final Logger log = LoggerFactory.getLogger(IdCodec.class);

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BITS = 42;
    private static final long MASK = (1L << BITS) - 1;
    private static final long MULT = 2654435761L;      // odd → invertible mod 2^42
    private static final long SALT = 0x2A3B4C5D6EL & MASK;
    private static final long INV = modInverse(MULT);  // MULT * INV ≡ 1 (mod 2^42)
    private static final int BODY = 8;                 // 62^8 > 2^42

    /** id → opaque token. Null/negative ids return null. */
    public String encode(Long id) {
        if (id == null || id < 0) return null;
        long m = ((id & MASK) * MULT) & MASK;
        long x = m ^ SALT;
        char[] out = new char[BODY + 1];
        out[0] = ALPHABET.charAt(10 + (int) (x % 26));   // A–Z, guarantees non-numeric
        long v = x;
        for (int i = BODY; i >= 1; i--) {
            out[i] = ALPHABET.charAt((int) (v % 62));
            v /= 62;
        }
        return new String(out);
    }

    /** token → id, or null if the token is malformed. */
    public Long decode(String token) {
        if (token == null || token.length() != BODY + 1) return null;
        long v = 0;
        for (int i = 1; i < token.length(); i++) {
            int d = ALPHABET.indexOf(token.charAt(i));
            if (d < 0) return null;
            v = v * 62 + d;
        }
        long m = v ^ SALT;
        return (m * INV) & MASK;
    }

    private static long modInverse(long a) {
        long x = a;                       // a is odd
        for (int i = 0; i < 6; i++) {     // Newton iteration → inverse mod 2^64
            x *= (2 - a * x);
        }
        return x & MASK;                  // reduce to inverse mod 2^42
    }

    private static IdCodec instance;

    /** Static convenience for building redirect URLs in controllers: encodes, null-safe. */
    public static String tok(Long id) {
        return (instance == null || id == null) ? String.valueOf(id) : instance.encode(id);
    }

    @PostConstruct
    void selfTest() {
        instance = this;
        long[] samples = {0, 1, 2, 27, 42, 100, 999, 1000, 123456, 999999, 4_000_000_000L, MASK};
        for (long id : samples) {
            Long back = decode(encode(id));
            if (back == null || back != id) {
                throw new IllegalStateException("IdCodec self-test failed for id " + id + " -> " + encode(id) + " -> " + back);
            }
        }
        log.info("IdCodec ready (opaque URL ids enabled).");
    }
}
