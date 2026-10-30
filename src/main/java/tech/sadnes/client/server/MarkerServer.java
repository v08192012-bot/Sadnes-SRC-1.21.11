package tech.sadnes.client.server;

public enum MarkerServer {
    ALL_SERVER("marker.server.all"),
    FUNTIME("marker.server.funtime"),
    SPOOKY_TIME("marker.server.spooky"),
    HOLY_WORLD("marker.server.holy");

    private final String translationKey;

    MarkerServer(String string) {
        this.translationKey = string;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public boolean isActive() {
        switch (this) {
            case ALL_SERVER:
                return true;
            case FUNTIME:
                return ServerManager.isFunTime();
            case SPOOKY_TIME:
                return ServerManager.isSpookyTime();
            case HOLY_WORLD:
                return ServerManager.isHolyWorld();
            default:
                throw new MatchException(null, null);
        }
    }

    public static boolean anyActive(MarkerServer[] markerServers) {
        if (markerServers == null) {
            return false;
        }

        for (MarkerServer markerServer : markerServers) {
            if (markerServer.isActive()) {
                return true;
            }
        }

        return false;
    }
}
