package tech.sadnes.client.server.autobuy;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import tech.sadnes.client.server.AutoBuyItem;

public final class PurchaseHistory {
    private static final int MAX_ENTRIES = 5000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<PurchaseHistory.Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private PurchaseHistory() {
    }

    public static List<PurchaseHistory.Entry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static void add(AutoBuyItem autoBuyItem, int i, String string, PurchaseHistory.Result result) {
        if (autoBuyItem != null) {
            ENTRIES.add(0, new PurchaseHistory.Entry(autoBuyItem, i, string == null ? "" : string, LocalTime.now().format(TIME_FORMAT), result));

            while (ENTRIES.size() > 5000) {
                ENTRIES.remove(ENTRIES.size() - 1);
            }
        }
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public record Entry(AutoBuyItem item, int price, String seller, String time, PurchaseHistory.Result result) {
    }

    public enum Result {
        BOUGHT,
        LOST,
        UNVERIFIED,
        NO_MONEY;
    }
}
