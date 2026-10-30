package tech.sadnes.client.server;


public enum ServerType {
    FUNTIME("funtime"),
    SPOOKYTIME("funtime"),
    HOLYWORLD("holyworld"),
    REALLYWORLD("reallyworld");

    private final String apiName;

    public boolean isCurrent() {
        switch (this) {
            case FUNTIME:
                return ServerManager.isFunTime();
            case SPOOKYTIME:
                return ServerManager.isSpookyTime();
            case HOLYWORLD:
                return ServerManager.isHolyWorld();
            case REALLYWORLD:
                return ServerManager.isReallyWorld();
            default:
                throw new MatchException(null, null);
        }
    }

    public ServerType resolve() {
        return this == FUNTIME && ServerManager.isSpookyTime() ? SPOOKYTIME : this;
    }

    public String getApiName() {
        return this.apiName;
    }

    ServerType(String string) {
        this.apiName = string;
    }
}
