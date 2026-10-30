package tech.sadnes.client.network.packet;

import java.util.List;
import java.util.UUID;
import tech.sadnes.client.network.PacketBuffer;

public final class PlayerState {
    public static final int NAME_MAX = 48;
    public static final int SERVER_MAX = 128;
    public static final int WORLD_MAX = 128;
    public static final int ITEM_MAX = 128;
    public static final int ARMOR_SLOTS = 4;
    private final String username;
    private final String server;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final String mainHand;
    private final String offHand;
    private final List<String> armor;
    private final boolean hasPing;
    private final int pingType;
    private final double pingX;
    private final double pingY;
    private final double pingZ;
    private final UUID pingEntity;

    public PlayerState(String string, String string2, String string3, double d, double d2, double d3, String string4, String string5, List<String> strings) {
        this(string, string2, string3, d, d2, d3, string4, string5, strings, false, 0, 0.0, 0.0, 0.0, null);
    }

    public PlayerState(
        String string,
        String string2,
        String string3,
        double d,
        double d2,
        double d3,
        String string4,
        String string5,
        List<String> strings,
        boolean flag,
        int i,
        double d4,
        double d5,
        double d6,
        UUID uuid
    ) {
        this.username = string;
        this.server = string2;
        this.world = string3;
        this.x = d;
        this.y = d2;
        this.z = d3;
        this.mainHand = string4;
        this.offHand = string5;
        this.armor = List.copyOf(strings);
        this.hasPing = flag;
        this.pingType = i;
        this.pingX = d4;
        this.pingY = d5;
        this.pingZ = d6;
        this.pingEntity = uuid;
    }

    public boolean hasPingEntity() {
        return this.pingEntity != null;
    }

    public static PlayerState read(PacketBuffer packetBuffer) {
        String string = packetBuffer.readString(48);
        String string2 = packetBuffer.readString(128);
        String string3 = packetBuffer.readString(128);
        double d = packetBuffer.readDouble();
        double d2 = packetBuffer.readDouble();
        double d3 = packetBuffer.readDouble();
        String string4 = packetBuffer.readString(128);
        String string5 = packetBuffer.readString(128);
        List list = packetBuffer.readList(4, packetBuffer2 -> packetBuffer2.readString(128));
        boolean flag = packetBuffer.readBoolean();
        int i = 0;
        double d4 = 0.0;
        double d5 = 0.0;
        double d6 = 0.0;
        UUID uuid = null;
        if (flag) {
            i = packetBuffer.readVarInt();
            d4 = packetBuffer.readDouble();
            d5 = packetBuffer.readDouble();
            d6 = packetBuffer.readDouble();
            if (packetBuffer.readBoolean()) {
                uuid = packetBuffer.readUuid();
            }
        }

        return new PlayerState(string, string2, string3, d, d2, d3, string4, string5, list, flag, i, d4, d5, d6, uuid);
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.username, 48);
        packetBuffer.writeString(this.server, 128);
        packetBuffer.writeString(this.world, 128);
        packetBuffer.writeDouble(this.x);
        packetBuffer.writeDouble(this.y);
        packetBuffer.writeDouble(this.z);
        packetBuffer.writeString(this.mainHand, 128);
        packetBuffer.writeString(this.offHand, 128);
        packetBuffer.writeList(this.armor, 4, (packetBuffer2, string) -> packetBuffer2.writeString(string, 128));
        packetBuffer.writeBoolean(this.hasPing);
        if (this.hasPing) {
            packetBuffer.writeVarInt(this.pingType);
            packetBuffer.writeDouble(this.pingX);
            packetBuffer.writeDouble(this.pingY);
            packetBuffer.writeDouble(this.pingZ);
            boolean flag = this.pingEntity != null;
            packetBuffer.writeBoolean(flag);
            if (flag) {
                packetBuffer.writeUuid(this.pingEntity);
            }
        }
    }

    public String getUsername() {
        return this.username;
    }

    public String getServer() {
        return this.server;
    }

    public String getWorld() {
        return this.world;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public String getMainHand() {
        return this.mainHand;
    }

    public String getOffHand() {
        return this.offHand;
    }

    public List<String> getArmor() {
        return this.armor;
    }

    public boolean isHasPing() {
        return this.hasPing;
    }

    public int getPingType() {
        return this.pingType;
    }

    public double getPingX() {
        return this.pingX;
    }

    public double getPingY() {
        return this.pingY;
    }

    public double getPingZ() {
        return this.pingZ;
    }

    public UUID getPingEntity() {
        return this.pingEntity;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof PlayerState playerState)) {
            return false;
        } else {
            if (Double.compare(this.getX(), playerState.getX()) != 0) {
                return false;
            }

            if (Double.compare(this.getY(), playerState.getY()) != 0) {
                return false;
            }

            if (Double.compare(this.getZ(), playerState.getZ()) != 0) {
                return false;
            }

            if (this.isHasPing() != playerState.isHasPing()) {
                return false;
            }

            if (this.getPingType() != playerState.getPingType()) {
                return false;
            }

            if (Double.compare(this.getPingX(), playerState.getPingX()) != 0) {
                return false;
            }

            if (Double.compare(this.getPingY(), playerState.getPingY()) != 0) {
                return false;
            }

            if (Double.compare(this.getPingZ(), playerState.getPingZ()) != 0) {
                return false;
            }

            String string = this.getUsername();
            String string2 = playerState.getUsername();
            if (string == null) {
                if (string2 != null) {
                    return false;
                }
            } else if (!string.equals(string2)) {
                return false;
            }

            String string3 = this.getServer();
            String string4 = playerState.getServer();
            if (string3 == null) {
                if (string4 != null) {
                    return false;
                }
            } else if (!string3.equals(string4)) {
                return false;
            }

            String string5 = this.getWorld();
            String string6 = playerState.getWorld();
            if (string5 == null) {
                if (string6 != null) {
                    return false;
                }
            } else if (!string5.equals(string6)) {
                return false;
            }

            String string7 = this.getMainHand();
            String string8 = playerState.getMainHand();
            if (string7 == null) {
                if (string8 != null) {
                    return false;
                }
            } else if (!string7.equals(string8)) {
                return false;
            }

            String string9 = this.getOffHand();
            String string10 = playerState.getOffHand();
            if (string9 == null) {
                if (string10 != null) {
                    return false;
                }
            } else if (!string9.equals(string10)) {
                return false;
            }

            List list = this.getArmor();
            List list2 = playerState.getArmor();
            if (list == null) {
                if (list2 != null) {
                    return false;
                }
            } else if (!list.equals(list2)) {
                return false;
            }

            UUID uuid = this.getPingEntity();
            UUID uuid2 = playerState.getPingEntity();
            if (uuid == null) {
                if (uuid2 != null) {
                    return false;
                }
            } else if (!uuid.equals(uuid2)) {
                return false;
            }

            return true;
        }
    }

    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(this.getX());
        int i = 59 + (int)(l >>> 32 ^ l);
        long l2 = Double.doubleToLongBits(this.getY());
        int i2 = i * 59 + (int)(l2 >>> 32 ^ l2);
        long l3 = Double.doubleToLongBits(this.getZ());
        int i3 = ((i2 * 59 + (int)(l3 >>> 32 ^ l3)) * 59 + (this.isHasPing() ? 79 : 97)) * 59 + this.getPingType();
        long l4 = Double.doubleToLongBits(this.getPingX());
        int i4 = i3 * 59 + (int)(l4 >>> 32 ^ l4);
        long l5 = Double.doubleToLongBits(this.getPingY());
        int i5 = i4 * 59 + (int)(l5 >>> 32 ^ l5);
        long l6 = Double.doubleToLongBits(this.getPingZ());
        int i6 = i5 * 59 + (int)(l6 >>> 32 ^ l6);
        String string = this.getUsername();
        int i7 = i6 * 59 + (string == null ? 43 : string.hashCode());
        String string2 = this.getServer();
        int i8 = i7 * 59 + (string2 == null ? 43 : string2.hashCode());
        String string3 = this.getWorld();
        int i9 = i8 * 59 + (string3 == null ? 43 : string3.hashCode());
        String string4 = this.getMainHand();
        int i10 = i9 * 59 + (string4 == null ? 43 : string4.hashCode());
        String string5 = this.getOffHand();
        int i11 = i10 * 59 + (string5 == null ? 43 : string5.hashCode());
        List list = this.getArmor();
        int i12 = i11 * 59 + (list == null ? 43 : list.hashCode());
        UUID uuid = this.getPingEntity();
        return uuid == null ? i12 * 59 + 43 : i12 * 59 + uuid.hashCode();
    }
}
