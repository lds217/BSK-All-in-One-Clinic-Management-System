package BsK.server.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes passwords with salted PBKDF2-HMAC-SHA256.
 *
 * <p>Stored format: {@code pbkdf2_sha256$<iterations>$<base64 salt>$<base64 hash>}. The iteration
 * count is part of the stored value, so it can be raised later without breaking existing hashes.
 */
public final class PasswordHasher {

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String PREFIX = "pbkdf2_sha256";
  private static final int ITERATIONS = 600_000;
  private static final int MAX_ITERATIONS = 10_000_000;
  private static final int SALT_BYTES = 16;
  private static final int HASH_BYTES = 32;

  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordHasher() {}

  public static String hash(String password) {
    Objects.requireNonNull(password, "password");
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    byte[] derived = derive(password, salt, ITERATIONS);
    Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
    return String.join(
        "$",
        PREFIX,
        Integer.toString(ITERATIONS),
        encoder.encodeToString(salt),
        encoder.encodeToString(derived));
  }

  public static boolean verify(String password, String storedHash) {
    if (password == null) {
      return false;
    }
    ParsedHash parsed = parse(storedHash);
    if (parsed == null) {
      return false;
    }
    byte[] derived = derive(password, parsed.salt(), parsed.iterations());
    return MessageDigest.isEqual(derived, parsed.hash());
  }

  /** Returns true only for a complete, well-formed hash produced by this class. */
  public static boolean isHash(String value) {
    return parse(value) != null;
  }

  private static ParsedHash parse(String value) {
    if (value == null) {
      return null;
    }
    String[] parts = value.split("\\$", -1);
    if (parts.length != 4 || !PREFIX.equals(parts[0])) {
      return null;
    }
    try {
      int iterations = Integer.parseInt(parts[1]);
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] hash = Base64.getDecoder().decode(parts[3]);
      if (iterations < 1
          || iterations > MAX_ITERATIONS
          || salt.length == 0
          || hash.length != HASH_BYTES) {
        return null;
      }
      return new ParsedHash(iterations, salt, hash);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BYTES * 8);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Password hashing is unavailable", e);
    } finally {
      spec.clearPassword();
    }
  }

  private record ParsedHash(int iterations, byte[] salt, byte[] hash) {}
}
