package tech.sadnes.client.network.packet;

public enum ConfigKind {
    CLIENT(0),
    AUTOBUY(1),
    COLLECTOR(2);

    private static final ConfigKind[] VALUES = values();
    private final int id;

    ConfigKind(int i) {
        this.id = i;
    }

    public int getId() {
        return this.id;
    }

    public static ConfigKind byId(int i) {
        for (ConfigKind configKind : VALUES) {
            if (configKind.id == i) {
                return configKind;
            }
        }

        return CLIENT;
    }
}
