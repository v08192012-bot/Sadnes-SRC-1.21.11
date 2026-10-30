package tech.sadnes.client.network;

import com.google.gson.JsonObject;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import tech.sadnes.client.Sadnes;

public final class DiscordIpc implements Closeable {
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int MAX_PIPES = 10;
    private static final int MAX_FRAME_LENGTH = 65536;
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private volatile DiscordIpc.Transport transport;

    public boolean isConnected() {
        return this.transport != null;
    }

    public boolean connect(long l) {
        Throwable throwable = null;

        for (int i = 0; i < 10; i++) {
            DiscordIpc.Transport transport2 = null;

            try {
                if (IS_WINDOWS) {
                    transport2 = new DiscordIpc.WindowsPipeTransport("\\\\.\\pipe\\discord-ipc-" + i);
                } else {
                    Path path = getTempDir().resolve("discord-ipc-" + i);
                    if (Files.exists(path)) {
                        transport2 = new DiscordIpc.UnixSocketTransport(path);
                    }
                }
            } catch (Throwable throwable2) {
                throwable = throwable2;
            }

            if (transport2 != null) {
                this.transport = transport2;

                try {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("v", 1);
                    jsonObject.addProperty("client_id", Long.toString(l));
                    this.writeFrame(0, jsonObject.toString());
                    this.readFrame();
                    Sadnes.getLogger().debug("Discord IPC connected on index {}", i);
                    return true;
                } catch (IOException ioException) {
                    this.close();
                }
            }
        }

        if (throwable != null) {
            Sadnes.getLogger().warn("Discord IPC connect failed: {}", throwable.toString());
        }

        return false;
    }

    public void setActivity(JsonObject jsonObject, long l) throws IOException {
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("pid", l);
        jsonObject2.add("activity", jsonObject);
        JsonObject jsonObject3 = new JsonObject();
        jsonObject3.addProperty("cmd", "SET_ACTIVITY");
        jsonObject3.add("args", jsonObject2);
        jsonObject3.addProperty("nonce", UUID.randomUUID().toString());
        this.writeFrame(1, jsonObject3.toString());
        this.readFrame();
    }

    @Override
    public synchronized void close() {
        DiscordIpc.Transport transport2 = this.transport;
        if (transport2 != null) {
            this.transport = null;

            try {
                transport2.close();
            } catch (IOException ioException) {
            }
        }
    }

    private void writeFrame(int i, String string) throws IOException {
        DiscordIpc.Transport transport2 = this.transport;
        if (transport2 == null) {
            throw new IOException("IPC closed");
        }

        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(i);
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
        transport2.write(byteBuffer.array());
    }

    private void readFrame() throws IOException {
        DiscordIpc.Transport transport2 = this.transport;
        if (transport2 == null) {
            throw new IOException("IPC closed");
        }

        byte[] bytes = new byte[8];
        transport2.readFully(bytes);
        int i = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        if (i >= 0 && i <= 65536) {
            transport2.readFully(new byte[i]);
        } else {
            throw new IOException("Bad IPC frame length " + i);
        }
    }

    private static Path getTempDir() {
        for (String string : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
            String string2 = System.getenv(string);
            if (string2 != null && !string2.isEmpty()) {
                return Path.of(string2);
            }
        }

        return Path.of("/tmp");
    }

    interface Transport extends Closeable {
        void write(byte[] bytes) throws IOException;

        void readFully(byte[] bytes) throws IOException;
    }

    static final class UnixSocketTransport implements DiscordIpc.Transport {
        private final SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);

        UnixSocketTransport(Path path) throws IOException {
            this.channel.connect(UnixDomainSocketAddress.of(path));
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

            while (byteBuffer.hasRemaining()) {
                this.channel.write(byteBuffer);
            }
        }

        @Override
        public void readFully(byte[] bytes) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

            while (byteBuffer.hasRemaining()) {
                if (this.channel.read(byteBuffer) < 0) {
                    throw new IOException("IPC stream closed");
                }
            }
        }

        @Override
        public void close() throws IOException {
            this.channel.close();
        }
    }

    static final class WindowsPipeTransport implements DiscordIpc.Transport {
        private final RandomAccessFile pipe;

        WindowsPipeTransport(String string) throws IOException {
            this.pipe = new RandomAccessFile(string, "rw");
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            this.pipe.write(bytes);
        }

        @Override
        public void readFully(byte[] bytes) throws IOException {
            this.pipe.readFully(bytes);
        }

        @Override
        public void close() throws IOException {
            this.pipe.close();
        }
    }
}
