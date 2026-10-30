package tech.sadnes.client.ui.notification;

import tech.sadnes.client.lang.Language;

public final class NotificationSources {
    private NotificationSources() {
    }

    public static String autoBuy() {
        return Language.translate("notify.source.autobuy", "Auto-buy");
    }

    public static String config() {
        return Language.translate("notify.source.config", "Config");
    }

    public static String scripts() {
        return Language.translate("notify.source.scripts", "Scripts");
    }

    public static String party() {
        return Language.translate("notify.source.party", "Party");
    }

    public static String events() {
        return Language.translate("notify.source.events", "Events");
    }

    public static String network() {
        return Language.translate("notify.source.network", "Network");
    }

    public static String reconnect() {
        return Language.translate("notify.source.reconnect", "Reconnect");
    }

    public static String inventory() {
        return Language.translate("notify.source.inventory", "Inventory");
    }

    public static String friends() {
        return Language.translate("notify.source.friends", "Friends");
    }
}
