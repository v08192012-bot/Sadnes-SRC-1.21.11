package tech.sadnes.client.network.packet;

import java.util.HashMap;
import java.util.Map;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ProtocolException;

public enum UserRole {
    DEFAULT(0),
    USER(1),
    BETA(2),
    MEDIA(3),
    SUPPORT(4),
    MODERATOR(5),
    ALPHA(6),
    ADMIN(7),
    OWNER(8);

    private final int wireId;
    private static final Map<Integer, UserRole> BY_WIRE_ID = new HashMap<>();

    UserRole(int i) {
        this.wireId = i;
    }

    public int getWireId() {
        return this.wireId;
    }

    public static void write(PacketBuffer packetBuffer, UserRole userRole) {
        packetBuffer.writeVarInt(userRole.wireId);
    }

    public static UserRole read(PacketBuffer packetBuffer) {
        int i = packetBuffer.readVarInt();
        UserRole userRole = BY_WIRE_ID.get(i);
        if (userRole == null) {
            throw new ProtocolException("Unknown role wireId " + i);
        } else {
            return userRole;
        }
    }

    static {
        for (UserRole userRole : values()) {
            BY_WIRE_ID.put(userRole.wireId, userRole);
        }
    }
}
