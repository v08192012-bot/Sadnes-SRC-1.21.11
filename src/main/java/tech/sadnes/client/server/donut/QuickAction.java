package tech.sadnes.client.server.donut;

import java.util.Arrays;

public enum QuickAction {
    SHOP("shop", "Быстрая покупка", "Quick Buy"),
    SELL("sell", "Продать"),
    RTP("rtp");

    private static final QuickAction[] VALUES = values();
    private final String id;
    private final String[] aliases;

    QuickAction(String string, String... strings) {
        this.id = string;
        this.aliases = strings;
    }

    public static QuickAction byId(String string) {
        return Arrays.stream(VALUES).filter(quickAction -> quickAction.id.equals(string)).findFirst().orElse(null);
    }

    public boolean run() {
        return QuickActionsUtil.trigger(this.id, this.aliases);
    }

    public String getId() {
        return this.id;
    }
}
