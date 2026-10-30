package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class ShareCodesPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final boolean found;
    private final String configId;
    private final String shareId;
    private final boolean isPublic;
    private final List<ShareCode> codes;
    private final List<ShareActivation> activations;

    public ShareCodesPacket(int i, boolean flag, String string, String string2, boolean flag2, List<ShareCode> shareCodes, List<ShareActivation> shareActivations) {
        this.requestId = i;
        this.found = flag;
        this.configId = string;
        this.shareId = string2;
        this.isPublic = flag2;
        this.codes = List.copyOf(shareCodes);
        this.activations = List.copyOf(shareActivations);
    }

    public ShareCodesPacket(PacketBuffer packetBuffer) {
        this.requestId = packetBuffer.readVarInt();
        this.found = packetBuffer.readBoolean();
        this.configId = packetBuffer.readString(32);
        this.shareId = packetBuffer.readString(32);
        this.isPublic = packetBuffer.readBoolean();
        this.codes = packetBuffer.readList(128, ShareCode::read);
        this.activations = packetBuffer.readList(256, ShareActivation::read);
    }

    public static ShareCodesPacket notFound(int i, String string) {
        return new ShareCodesPacket(i, false, string, "", false, List.of(), List.of());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeBoolean(this.found);
        packetBuffer.writeString(this.configId, 32);
        packetBuffer.writeString(this.shareId, 32);
        packetBuffer.writeBoolean(this.isPublic);
        packetBuffer.writeList(this.codes, 128, (packetBuffer2, shareCode) -> shareCode.write(packetBuffer2));
        packetBuffer.writeList(this.activations, 256, (packetBuffer3, shareActivation) -> shareActivation.write(packetBuffer3));
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.configs().onShareCodes(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public boolean isFound() {
        return this.found;
    }

    public String getConfigId() {
        return this.configId;
    }

    public String getShareId() {
        return this.shareId;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public List<ShareCode> getCodes() {
        return this.codes;
    }

    public List<ShareActivation> getActivations() {
        return this.activations;
    }
}
