package BsK.common.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Locale;

/** SHA-256 fingerprint of a certificate, written as colon-separated upper-case hex. */
public final class CertificateFingerprint {

  private CertificateFingerprint() {}

  public static String sha256(X509Certificate certificate) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
      return HexFormat.ofDelimiter(":").withUpperCase().formatHex(digest);
    } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
      throw new IllegalStateException("Could not compute certificate fingerprint", e);
    }
  }

  /** Makes fingerprints comparable regardless of case, spaces or separators. */
  public static String normalize(String fingerprint) {
    if (fingerprint == null) {
      return "";
    }
    return fingerprint.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
  }
}
