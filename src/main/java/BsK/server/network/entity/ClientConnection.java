package BsK.server.network.entity;

import io.netty.channel.Channel;
import lombok.Data;
import java.time.Instant;
import java.time.Duration;

@Data
public class ClientConnection {
    private final String ipAddress;
    private final int port;
    private final Instant connectTime;
    private Instant lastActivityTime;
    private long bytesSent;
    private long bytesReceived;
    private final int sessionId;
    private String userRole;
    private int userId;

    public ClientConnection(Channel channel, int sessionId) {
        String remoteAddress = channel.remoteAddress().toString();
        // Remove the leading '/' if present and split by ':'
        String[] parts = remoteAddress.replaceFirst("^/", "").split(":");
        this.ipAddress = parts[0];
        this.port = Integer.parseInt(parts[1]);
        this.connectTime = Instant.now();
        this.lastActivityTime = Instant.now();
        this.sessionId = sessionId;
        this.userRole = "GUEST";
        this.bytesSent = 0;
        this.bytesReceived = 0;
    }

    public void updateLastActivity() {
        this.lastActivityTime = Instant.now();
    }

    public void addBytesSent(long bytes) {
        this.bytesSent += bytes;
    }

    public void addBytesReceived(long bytes) {
        this.bytesReceived += bytes;
    }

    public String getConnectionDuration() {
        Duration duration = Duration.between(connectTime, Instant.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getLastActivityDuration() {
        Duration duration = Duration.between(lastActivityTime, Instant.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "Just now";
        } else if (minutes == 1) {
            return "1 minute ago";
        } else {
            return minutes + " minutes ago";
        }
    }
} 