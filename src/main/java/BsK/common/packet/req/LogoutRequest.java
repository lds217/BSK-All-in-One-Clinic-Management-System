package BsK.common.packet.req;

import BsK.common.packet.Packet;

public class LogoutRequest implements Packet {
    // This packet can be empty as the server identifies the user by channel.
    // If more specific logout information were needed (e.g., for auditing),
    // fields could be added here.
    public LogoutRequest() {
        // Constructor
    }
} 