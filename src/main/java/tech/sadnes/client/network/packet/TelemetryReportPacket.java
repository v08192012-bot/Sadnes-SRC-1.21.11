package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class TelemetryReportPacket implements ServerboundPacket<ServerMainPacketListener> {
    public static final int MAX_TYPE_LENGTH = 128;
    public static final int MAX_ENTRY_LENGTH = 64;
    public static final int MAX_ENTRIES = 512;
    private final String type;
    private final List<String> entries;

    public TelemetryReportPacket(String string, List<String> strings) {
        this.type = string;
        this.entries = List.copyOf(strings);
    }

    public TelemetryReportPacket(PacketBuffer packetBuffer) {
        this.type = packetBuffer.readString(128);
        this.entries = packetBuffer.readList(512, packetBuffer2 -> packetBuffer2.readString(64));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.type, 128);
        packetBuffer.writeList(this.entries, 512, (packetBuffer2, string) -> packetBuffer2.writeString(string, 64));
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.telemetry().onReport(this);
    }

    public String getType() {
        return this.type;
    }

    public List<String> getEntries() {
        return this.entries;
    }
}
