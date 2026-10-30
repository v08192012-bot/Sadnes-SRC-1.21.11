package tech.sadnes.client.network;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class PacketBuffer {
    private static final int MAX_STRING_LENGTH = 32767;
    private byte[] buffer;
    private int writerIndex;
    private int readerIndex;

    private PacketBuffer(byte[] bytes, int i) {
        this.buffer = bytes;
        this.writerIndex = i;
    }

    public static PacketBuffer create() {
        return new PacketBuffer(new byte[32], 0);
    }

    public static PacketBuffer wrap(byte[] bytes) {
        return new PacketBuffer(bytes, bytes.length);
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public int getWriterIndex() {
        return this.writerIndex;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(this.buffer, this.writerIndex);
    }

    public int readableBytes() {
        return this.writerIndex - this.readerIndex;
    }

    public PacketBuffer writeVarInt(int i) {
        while ((i & -128) != 0) {
            this.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        this.writeByte(i);
        return this;
    }

    public int readVarInt() {
        int i = 0;
        byte b = 0;

        int i2;
        do {
            i2 = this.readByte();
            if (b == 28 && (i2 & 240) != 0) {
                throw new ProtocolException("VarInt malformed");
            }

            i |= (i2 & 127) << b;
            b += 7;
            if (b > 35) {
                throw new ProtocolException("VarInt too long");
            }
        } while ((i2 & 128) != 0);

        return i;
    }

    public PacketBuffer writeVarLong(long l) {
        while ((l & -128L) != 0L) {
            this.writeByte((int)(l & 127L | 128L));
            l >>>= 7;
        }

        this.writeByte((int)l);
        return this;
    }

    public long readVarLong() {
        long l = 0L;
        byte b = 0;

        int i;
        do {
            i = this.readByte();
            if (b == 63 && (i & 254) != 0) {
                throw new ProtocolException("VarLong malformed");
            }

            l |= (long)(i & 127) << b;
            b += 7;
            if (b > 70) {
                throw new ProtocolException("VarLong too long");
            }
        } while ((i & 128) != 0);

        return l;
    }

    public PacketBuffer writeBoolean(boolean flag) {
        return flag ? this.writeByte(1) : this.writeByte(0);
    }

    public boolean readBoolean() {
        return this.readByte() != 0;
    }

    public PacketBuffer writeLong(long l) {
        for (byte b = 56; b >= 0; b -= 8) {
            this.writeByte((int)(l >>> b));
        }

        return this;
    }

    public long readLong() {
        long l = 0L;

        for (int i = 0; i < 8; i++) {
            l = l << 8 | this.readByte() & 0xFF;
        }

        return l;
    }

    public PacketBuffer writeDouble(double d) {
        return this.writeLong(Double.doubleToLongBits(d));
    }

    public double readDouble() {
        return Double.longBitsToDouble(this.readLong());
    }

    public PacketBuffer writeString(String string) {
        return this.writeString(string, 32767);
    }

    public PacketBuffer writeString(String string, int i) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > i) {
            throw new ProtocolException("String too long: " + bytes.length);
        }

        this.writeVarInt(bytes.length);
        this.writeBytes(bytes, bytes.length);
        return this;
    }

    public String readString() {
        return this.readString(32767);
    }

    public String readString(int i) {
        int i2 = this.readVarInt();
        if (i2 < 0 || i2 > i) {
            throw new ProtocolException("Bad string length: " + i2);
        }

        if (i2 > this.readableBytes()) {
            throw new ProtocolException("Truncated string");
        }

        String string = new String(this.buffer, this.readerIndex, i2, StandardCharsets.UTF_8);
        this.readerIndex += i2;
        return string;
    }

    public PacketBuffer writeByteArray(byte[] bytes, int i) {
        if (bytes.length > i) {
            throw new ProtocolException("byte[] too long: " + bytes.length);
        }

        this.writeVarInt(bytes.length);
        this.writeBytes(bytes, bytes.length);
        return this;
    }

    public byte[] readByteArray(int i) {
        int i2 = this.readVarInt();
        if (i2 < 0 || i2 > i) {
            throw new ProtocolException("Bad byte[] length: " + i2);
        }

        if (i2 > this.readableBytes()) {
            throw new ProtocolException("Truncated byte[]");
        }

        byte[] bytes = Arrays.copyOfRange(this.buffer, this.readerIndex, this.readerIndex + i2);
        this.readerIndex += i2;
        return bytes;
    }

    public PacketBuffer writeUuid(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUuid() {
        return new UUID(this.readLong(), this.readLong());
    }

    public PacketBuffer writeEnum(Enum<?> enum2) {
        return this.writeVarInt(enum2.ordinal());
    }

    public <E extends Enum<E>> E readEnum(Class<E> clazz) {
        int i = this.readVarInt();
        Enum[] enums = (Enum[])clazz.getEnumConstants();
        if (i >= 0 && i < enums.length) {
            return (E)enums[i];
        } else {
            throw new ProtocolException("Bad enum ordinal " + i + " for " + clazz.getSimpleName());
        }
    }

    public <T> PacketBuffer writeList(List<T> list, int i, BiConsumer<PacketBuffer, T> biConsumer) {
        if (list.size() > i) {
            throw new ProtocolException("List too long: " + list.size());
        }

        this.writeVarInt(list.size());

        for (T value : list) {
            biConsumer.accept(this, value);
        }

        return this;
    }

    public <T> List<T> readList(int i, Function<PacketBuffer, T> function) {
        int i2 = this.readVarInt();
        if (i2 < 0 || i2 > i) {
            throw new ProtocolException("Bad list count: " + i2);
        }

        if (i2 > this.readableBytes()) {
            throw new ProtocolException("List count exceeds remaining bytes");
        }

        ArrayList arrayList = new ArrayList();

        for (int i3 = 0; i3 < i2; i3++) {
            arrayList.add(function.apply(this));
        }

        return arrayList;
    }

    private PacketBuffer writeByte(int i) {
        this.ensureCapacity(1);
        this.buffer[this.writerIndex++] = (byte)i;
        return this;
    }

    private int readByte() {
        if (this.readerIndex >= this.writerIndex) {
            throw new ProtocolException("Read past end of buffer");
        } else {
            return this.buffer[this.readerIndex++] & 0xFF;
        }
    }

    private void writeBytes(byte[] bytes, int i) {
        this.ensureCapacity(i);
        System.arraycopy(bytes, 0, this.buffer, this.writerIndex, i);
        this.writerIndex += i;
    }

    private void ensureCapacity(int i) {
        if (this.writerIndex + i > this.buffer.length) {
            this.buffer = Arrays.copyOf(this.buffer, Math.max(this.buffer.length * 2, this.writerIndex + i));
        }
    }
}
