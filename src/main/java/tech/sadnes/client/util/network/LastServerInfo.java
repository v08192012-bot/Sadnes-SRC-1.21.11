package tech.sadnes.client.util.network;

import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class LastServerInfo {
    private static ServerAddress address;
    private static ServerInfo serverInfo;

    private LastServerInfo() {
    }

    public static void set(ServerAddress serverAddress, ServerInfo serverInfo2) {
        if (serverAddress != null && serverInfo2 != null) {
            address = serverAddress;
            serverInfo = serverInfo2;
        }
    }

    public static ServerAddress getAddress() {
        return address;
    }

    public static ServerInfo getServerInfo() {
        return serverInfo;
    }

    public static boolean isPresent() {
        return address != null && serverInfo != null;
    }
}
