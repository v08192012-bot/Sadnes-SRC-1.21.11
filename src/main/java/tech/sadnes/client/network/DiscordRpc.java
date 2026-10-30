package tech.sadnes.client.network;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import tech.sadnes.client.Sadnes;

public final class DiscordRpc {
    private static final long RECONNECT_DELAY = 10000L;
    private static final long UPDATE_INTERVAL = 2000L;
    private static final long OVERRIDE_TIMEOUT = 5000L;
    private final long clientId;
    private final long pid;
    private final Object lock = new Object();
    private volatile boolean running;
    private volatile RichPresence overridePresence;
    private volatile RichPresence presence;
    private volatile long overrideTime;
    private Thread thread;
    private final DiscordIpc ipc = new DiscordIpc();

    public DiscordRpc(long l) {
        this.clientId = l;
        long l2 = 1L;

        try {
            l2 = ProcessHandle.current().pid();
        } catch (Throwable throwable) {
        }

        this.pid = l2;
    }

    public void start() {
        synchronized (this.lock) {
            if (!this.running) {
                this.running = true;
                this.thread = new Thread(this::runPresenceLoop, "Discord-RPC");
                this.thread.setDaemon(true);
                this.thread.start();
            }
        }
    }

    public void stop() {
        Thread thread2;
        synchronized (this.lock) {
            if (!this.running) {
                return;
            }

            this.running = false;
            thread2 = this.thread;
            this.thread = null;
            this.lock.notifyAll();
        }

        if (thread2 != null) {
            thread2.interrupt();
        }

        CompletableFuture.runAsync(this.ipc::close);
    }

    public void setOverridePresence(RichPresence richPresence) {
        this.overridePresence = richPresence;
        this.overrideTime = System.currentTimeMillis();
    }

    public void setPresence(RichPresence richPresence) {
        this.presence = richPresence;
    }

    private void runPresenceLoop() {
        RichPresence richPresence = null;
        String string = null;

        try {
            while (this.running) {
                if (!this.ipc.isConnected()) {
                    if (!this.ipc.connect(this.clientId)) {
                        this.sleep(10000L);
                        continue;
                    }

                    if (string == null) {
                        Sadnes.getLogger().info("Discord IPC connected");
                    }

                    richPresence = null;
                }

                RichPresence richPresence2 = this.getCurrentPresence();
                if (richPresence2 != null && !richPresence2.equals(richPresence)) {
                    try {
                        this.ipc.setActivity(richPresence2.toJson(), this.pid);
                        richPresence = richPresence2;
                        string = null;
                    } catch (IOException ioException) {
                        String string2 = ioException.toString();
                        if (!string2.equals(string)) {
                            Sadnes.getLogger().warn("Discord RPC send failed, reconnecting: {}", string2);
                        }

                        string = string2;
                        this.ipc.close();
                        richPresence = null;
                        this.sleep(10000L);
                        continue;
                    }
                }

                this.sleep(2000L);
            }
        } catch (Exception exception) {
            Sadnes.getLogger().warn("Discord RPC loop stopped", exception);
        } finally {
            this.ipc.close();
        }
    }

    private RichPresence getCurrentPresence() {
        RichPresence richPresence = this.overridePresence;
        return richPresence != null && System.currentTimeMillis() - this.overrideTime <= 5000L ? richPresence : this.presence;
    }

    private void sleep(long l) {
        synchronized (this.lock) {
            if (this.running) {
                try {
                    this.lock.wait(l);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
