package BsK.client.network;

import BsK.common.security.CertificateFingerprint;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.extern.slf4j.Slf4j;

/**
 * TLS for the connection to the server.
 *
 * <p>The server uses a self-signed certificate, so the client trusts it by fingerprint: on the first
 * encrypted connection it saves the server's fingerprint in {@code config/config.properties}
 * ({@value #FINGERPRINT_KEY}) and from then on refuses any other certificate. Host names and IP
 * addresses are not checked, so the server's address can change without a new certificate.
 */
@Slf4j
public final class ClientTls {

  public static final String ENABLED_KEY = "client.tls.enabled";
  public static final String FINGERPRINT_KEY = "client.tls.server_fingerprint";

  private static final Path CONFIG_FILE = Path.of("config", "config.properties");

  private ClientTls() {}

  /** TLS is on unless the config explicitly says {@code client.tls.enabled=false}. */
  public static boolean isEnabled(Properties config) {
    return !"false".equalsIgnoreCase(config.getProperty(ENABLED_KEY, "true").trim());
  }

  public static String webSocketScheme(Properties config) {
    return isEnabled(config) ? "wss" : "ws";
  }

  /**
   * Adds the TLS handler at the front of the pipeline when TLS is enabled.
   *
   * @return the handler, or {@code null} when TLS is disabled
   */
  public static SslHandler addTo(ChannelPipeline pipeline, Properties config, String host, int port)
      throws SSLException {
    if (!isEnabled(config)) {
      return null;
    }
    SslContext context =
        SslContextBuilder.forClient()
            .sslProvider(SslProvider.JDK)
            .protocols("TLSv1.3", "TLSv1.2")
            .trustManager(new PinningTrustManager(config))
            .build();
    SslHandler handler = context.newHandler(pipeline.channel().alloc(), host, port);
    pipeline.addFirst("tls", handler);
    return handler;
  }

  /** A message for staff explaining a failed TLS handshake. */
  public static String describeHandshakeFailure(Throwable failure) {
    for (Throwable t = failure; t != null; t = t.getCause()) {
      if (t instanceof FingerprintMismatchException mismatch) {
        return """
            Chứng chỉ bảo mật của máy chủ KHÔNG KHỚP với chứng chỉ đã lưu trên máy này.

            Đã lưu:   %s
            Máy chủ:  %s

            Nếu máy chủ vừa được cài lại hoặc thay máy, hãy xoá dòng %s
            trong file config/config.properties rồi mở lại ứng dụng.
            Nếu không, đừng kết nối và báo ngay cho quản trị viên."""
            .formatted(mismatch.expected, mismatch.actual, FINGERPRINT_KEY);
      }
    }
    return """
        Không thể thiết lập kết nối mã hoá với máy chủ.

        Máy chủ có thể đang chạy phiên bản cũ chưa hỗ trợ mã hoá.
        Hãy cập nhật máy chủ, hoặc tạm thời đặt %s=false
        trong file config/config.properties.

        Chi tiết lỗi: %s"""
        .formatted(ENABLED_KEY, failure == null ? "" : failure.getMessage());
  }

  private static synchronized void saveFingerprint(Properties config, String fingerprint) {
    config.setProperty(FINGERPRINT_KEY, fingerprint);
    if (!Files.exists(CONFIG_FILE)) {
      log.warn("{} not found; server fingerprint is only kept until the app closes.", CONFIG_FILE);
      return;
    }
    try {
      Properties onDisk = new Properties();
      try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
        onDisk.load(in);
      }
      onDisk.setProperty(FINGERPRINT_KEY, fingerprint);
      try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
        onDisk.store(out, "BSK Clinic Management System Configuration");
      }
    } catch (IOException e) {
      log.error("Could not save the server fingerprint to {}", CONFIG_FILE, e);
    }
  }

  static final class FingerprintMismatchException extends CertificateException {
    final String expected;
    final String actual;

    FingerprintMismatchException(String expected, String actual) {
      super("Server certificate fingerprint " + actual + " does not match the saved " + expected);
      this.expected = expected;
      this.actual = actual;
    }
  }

  /** Trusts exactly one server certificate, identified by its SHA-256 fingerprint. */
  private static final class PinningTrustManager extends X509ExtendedTrustManager {
    private final Properties config;

    PinningTrustManager(Properties config) {
      this.config = config;
    }

    private void verify(X509Certificate[] chain) throws CertificateException {
      if (chain == null || chain.length == 0) {
        throw new CertificateException("Server sent no certificate");
      }
      String actual = CertificateFingerprint.sha256(chain[0]);
      synchronized (ClientTls.class) {
        String expected = config.getProperty(FINGERPRINT_KEY, "").trim();
        if (expected.isEmpty()) {
          log.warn("First encrypted connection: trusting server certificate {}", actual);
          saveFingerprint(config, actual);
          return;
        }
        if (!CertificateFingerprint.normalize(expected)
            .equals(CertificateFingerprint.normalize(actual))) {
          log.error("Server certificate {} does not match saved fingerprint {}", actual, expected);
          throw new FingerprintMismatchException(expected, actual);
        }
      }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      verify(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      verify(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      verify(chain);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      throw new CertificateException("Client certificates are not used");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      throw new CertificateException("Client certificates are not used");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      throw new CertificateException("Client certificates are not used");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}
