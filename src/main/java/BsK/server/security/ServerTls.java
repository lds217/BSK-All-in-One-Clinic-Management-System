package BsK.server.security;

import BsK.common.security.CertificateFingerprint;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.OptionalSslHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * TLS for the client connection. On first start the server creates its own key and self-signed
 * certificate in {@code config/tls/server.p12} and reuses it afterwards. Clients trust the server by
 * remembering the certificate's fingerprint, so no certificate authority or host name is involved.
 */
@Slf4j
public final class ServerTls {

  public static final Path DEFAULT_KEYSTORE = Path.of("config", "tls", "server.p12");

  private static final String ALIAS = "bsk-server";
  // The keystore sits next to the database on the server PC; disk access is the real protection.
  private static final char[] KEYSTORE_PASSWORD = "bsk-server-tls".toCharArray();

  private final SslContext sslContext;
  private final String fingerprint;
  private volatile Consumer<String> notice = message -> {};

  private ServerTls(SslContext sslContext, String fingerprint) {
    this.sslContext = sslContext;
    this.fingerprint = fingerprint;
  }

  public static ServerTls loadOrCreate(Path keystorePath)
      throws IOException, GeneralSecurityException {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    if (Files.exists(keystorePath)) {
      try (InputStream in = Files.newInputStream(keystorePath)) {
        keyStore.load(in, KEYSTORE_PASSWORD);
      }
      log.info("Loaded TLS certificate from {}", keystorePath.toAbsolutePath());
    } else {
      createKeystore(keyStore, keystorePath);
      log.warn("Created a new TLS certificate at {}", keystorePath.toAbsolutePath());
    }

    PrivateKey key = (PrivateKey) keyStore.getKey(ALIAS, KEYSTORE_PASSWORD);
    Certificate certificate = keyStore.getCertificate(ALIAS);
    if (key == null || !(certificate instanceof X509Certificate x509)) {
      throw new GeneralSecurityException(
          "Keystore " + keystorePath + " has no '" + ALIAS + "' key. Delete it to create a new one.");
    }

    SslContext context =
        SslContextBuilder.forServer(key, x509)
            .sslProvider(SslProvider.JDK)
            .protocols("TLSv1.3", "TLSv1.2")
            .build();
    return new ServerTls(context, CertificateFingerprint.sha256(x509));
  }

  private static void createKeystore(KeyStore keyStore, Path keystorePath)
      throws IOException, GeneralSecurityException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    KeyPair keyPair = generator.generateKeyPair();
    X509Certificate certificate =
        SelfSignedCertificateGenerator.generate(keyPair, "BSK Clinic Server");

    keyStore.load(null, null);
    keyStore.setKeyEntry(
        ALIAS, keyPair.getPrivate(), KEYSTORE_PASSWORD, new Certificate[] {certificate});

    Path parent = keystorePath.toAbsolutePath().getParent();
    Files.createDirectories(parent);
    Path temp = Files.createTempFile(parent, "server", ".p12.tmp");
    try {
      try (OutputStream out = Files.newOutputStream(temp)) {
        keyStore.store(out, KEYSTORE_PASSWORD);
      }
      Files.move(temp, keystorePath, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  /** Where to report clients that connect without encryption (e.g. the server dashboard). */
  public void setNotice(Consumer<String> notice) {
    this.notice = notice;
  }

  /** SHA-256 fingerprint of the server certificate, shown on the dashboard for checking clients. */
  public String fingerprint() {
    return fingerprint;
  }

  /**
   * The first handler of a new connection. With {@code allowPlaintext}, clients that have not been
   * updated yet can still connect without encryption on the same port.
   */
  public ChannelHandler newHandler(SocketChannel channel, boolean allowPlaintext) {
    if (!allowPlaintext) {
      return sslContext.newHandler(channel.alloc());
    }
    return new OptionalSslHandler(sslContext) {
      @Override
      protected ChannelHandler newNonSslHandler(ChannelHandlerContext ctx) {
        log.warn(
            "Client {} connected WITHOUT encryption. Update it, then set server.tls.allow_plaintext=false.",
            ctx.channel().remoteAddress());
        notice.accept("Máy trạm " + ctx.channel().remoteAddress()
            + " kết nối KHÔNG mã hoá (chưa cập nhật phiên bản mới).");
        return null;
      }
    };
  }
}
