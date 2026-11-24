package BsK.server.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds a self-signed X.509 certificate for an EC P-256 key pair using only the JDK.
 *
 * <p>Clients trust the server by pinning this certificate's fingerprint, not by checking names or
 * dates, so the certificate only needs to be well-formed. It is valid until the end of 2099 so it
 * never has to be renewed.
 */
final class SelfSignedCertificateGenerator {

  // ecdsa-with-SHA256 (1.2.840.10045.4.3.2)
  private static final byte[] OID_ECDSA_WITH_SHA256 = {
    0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x04, 0x03, 0x02
  };
  // id-at-commonName (2.5.4.3)
  private static final byte[] OID_COMMON_NAME = {0x55, 0x04, 0x03};

  private SelfSignedCertificateGenerator() {}

  static X509Certificate generate(KeyPair keyPair, String commonName)
      throws GeneralSecurityException {
    byte[] signatureAlgorithm = sequence(tlv(0x06, OID_ECDSA_WITH_SHA256));
    byte[] name =
        sequence(
            tlv(
                0x31, // SET
                sequence(
                    tlv(0x06, OID_COMMON_NAME),
                    tlv(0x0C, commonName.getBytes(StandardCharsets.UTF_8))))); // UTF8String

    ZonedDateTime notBefore = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
    byte[] validity =
        sequence(
            tlv(0x17, ascii(notBefore.format(DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'")))),
            tlv(0x18, ascii("20991231235959Z"))); // GeneralizedTime

    byte[] tbsCertificate =
        sequence(
            tlv(0xA0, tlv(0x02, new byte[] {0x02})), // [0] version v3
            tlv(0x02, randomSerialNumber()),
            signatureAlgorithm,
            name, // issuer
            validity,
            name, // subject (self-signed)
            keyPair.getPublic().getEncoded()); // SubjectPublicKeyInfo, already DER

    Signature signer = Signature.getInstance("SHA256withECDSA");
    signer.initSign(keyPair.getPrivate());
    signer.update(tbsCertificate);
    byte[] signature = signer.sign(); // DER-encoded ECDSA signature

    byte[] signatureBits = new byte[signature.length + 1]; // leading 0 = no unused bits
    System.arraycopy(signature, 0, signatureBits, 1, signature.length);

    byte[] certificate = sequence(tbsCertificate, signatureAlgorithm, tlv(0x03, signatureBits));
    return (X509Certificate)
        CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(certificate));
  }

  private static byte[] randomSerialNumber() {
    byte[] serial = new byte[16];
    new SecureRandom().nextBytes(serial);
    serial[0] &= 0x7F; // must be positive
    serial[0] |= 0x01; // and non-zero, with no redundant leading zero byte
    return new BigInteger(serial).toByteArray();
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }

  private static byte[] sequence(byte[]... parts) {
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      content.writeBytes(part);
    }
    return tlv(0x30, content.toByteArray());
  }

  /** DER tag-length-value. */
  private static byte[] tlv(int tag, byte[] value) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(tag);
    int length = value.length;
    if (length < 0x80) {
      out.write(length);
    } else {
      int byteCount = (Integer.SIZE - Integer.numberOfLeadingZeros(length) + 7) / 8;
      out.write(0x80 | byteCount);
      for (int i = byteCount - 1; i >= 0; i--) {
        out.write(length >>> (8 * i));
      }
    }
    out.writeBytes(value);
    return out.toByteArray();
  }
}
