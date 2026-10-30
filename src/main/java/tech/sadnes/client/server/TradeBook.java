package tech.sadnes.client.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.math.StopWatch;

public final class TradeBook {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeBook.class);
    private static final Gson GSON = new Gson();
    private final Map<String, List<TradeBook.PriceSample>> priceSamples = new HashMap<>();
    private final List<TradeBook.Lot> lots = new ArrayList<>();
    private final StopWatch saveTimer = new StopWatch();
    private boolean loaded;
    private boolean dirty;

    public static boolean isPlainItem(ItemStack itemStack, Item item) {
        return itemStack.isOf(item) && !itemStack.hasEnchantments() && !itemStack.contains(DataComponentTypes.CUSTOM_NAME);
    }

    public void recordPrice(Item item, long l) {
        this.ensureLoaded();
        long l2 = System.currentTimeMillis();
        List<TradeBook.PriceSample> priceSamples2 = this.priceSamples.computeIfAbsent(getItemId(item), string -> new ArrayList<>());
        priceSamples2.removeIf(priceSample -> l2 - priceSample.time() > 43200000L);
        priceSamples2.add(new TradeBook.PriceSample(l2, l));
        this.dirty = true;
    }

    public long getAveragePrice(Item item, long l) {
        this.ensureLoaded();
        long l2 = System.currentTimeMillis();
        long l3 = 0L;
        long l4 = l2;
        int i = 0;

        for (TradeBook.PriceSample priceSample : this.priceSamples.getOrDefault(getItemId(item), List.of())) {
            if (l2 - priceSample.time() <= l) {
                l3 += priceSample.price();
                l4 = Math.min(l4, priceSample.time());
                i++;
            }
        }

        boolean flag = i >= 10 && l2 - l4 >= 300000L;
        return flag ? l3 / i : -1L;
    }

    public List<TradeBook.Lot> getLots(Item item2) {
        this.ensureLoaded();
        String string = getItemId(item2);
        String string2 = getPlayerName();
        return this.lots.stream().filter(lot -> lot.item().equals(string) && lot.owner().equals(string2)).toList();
    }

    public int getOwnedCount(Item item) {
        return this.getLots(item).stream().mapToInt(TradeBook.Lot::count).sum();
    }

    public void addLot(Item item, int i, long l) {
        this.ensureLoaded();
        this.lots.add(new TradeBook.Lot(getPlayerName(), getItemId(item), i, l, System.currentTimeMillis()));
        this.save();
    }

    public void removeLot(TradeBook.Lot lot) {
        if (this.lots.remove(lot)) {
            this.save();
        }
    }

    public void consumeLots(Item item2, int i) {
        int i2 = i;

        for (TradeBook.Lot lot : this.getLots(item2)) {
            if (i2 <= 0) {
                break;
            }

            int i3 = lot.count() - Math.min(i2, lot.count());
            i2 -= lot.count() - i3;
            this.lots.remove(lot);
            if (i3 > 0) {
                this.lots.add(new TradeBook.Lot(lot.owner(), lot.item(), i3, Math.round(lot.unitCost() * i3), lot.time()));
            }
        }

        if (i2 < i) {
            this.save();
        }
    }

    public void tick() {
        if (this.dirty && this.saveTimer.hasPassed(60000.0)) {
            this.save();
        }
    }

    public void save() {
        if (this.loaded) {
            this.dirty = false;
            this.saveTimer.reset();
            JsonObject jsonObject = new JsonObject();

            for (Entry entry : this.priceSamples.entrySet()) {
                JsonArray jsonArray = new JsonArray();

                for (TradeBook.PriceSample priceSample : (List<TradeBook.PriceSample>)entry.getValue()) {
                    JsonArray jsonArray2 = new JsonArray();
                    jsonArray2.add(priceSample.time());
                    jsonArray2.add(priceSample.price());
                    jsonArray.add(jsonArray2);
                }

                jsonObject.add((String)entry.getKey(), jsonArray);
            }

            JsonArray jsonArray3 = new JsonArray();

            for (TradeBook.Lot lot : this.lots) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("owner", lot.owner());
                jsonObject2.addProperty("item", lot.item());
                jsonObject2.addProperty("count", lot.count());
                jsonObject2.addProperty("cost", lot.cost());
                jsonObject2.addProperty("time", lot.time());
                jsonArray3.add(jsonObject2);
            }

            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.add("lots", jsonArray3);
            jsonObject3.add("samples", jsonObject);
            this.writeFile(GSON.toJson(jsonObject3));
        }
    }

    private void writeFile(String string) {
        Path path = getFilePath();
        Path path2 = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path2, string, StandardCharsets.UTF_8);
            Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioException) {
            LOGGER.warn("Failed to save trade book: {}", ioException.getMessage());
        }
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.loaded = true;
            Path path = getFilePath();
            if (Files.exists(path)) {
                try {
                    JsonObject jsonObject = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
                    if (jsonObject.has("lots")) {
                        this.loadLots(jsonObject.getAsJsonArray("lots"));
                    }

                    if (jsonObject.has("samples")) {
                        this.loadSamples(jsonObject.getAsJsonObject("samples"));
                    }
                } catch (Exception exception) {
                    LOGGER.warn("Failed to load trade book: {}", exception.getMessage());
                    this.backupBadFile(path);
                }
            }
        }
    }

    private void loadLots(JsonArray jsonArray) {
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String string = jsonObject.has("owner") ? jsonObject.get("owner").getAsString() : getPlayerName();
            this.lots
                .add(
                    new TradeBook.Lot(
                        string, jsonObject.get("item").getAsString(), jsonObject.get("count").getAsInt(), jsonObject.get("cost").getAsLong(), jsonObject.get("time").getAsLong()
                    )
                );
        }
    }

    private void loadSamples(JsonObject jsonObject) {
        for (Entry entry : jsonObject.entrySet()) {
            ArrayList arrayList = new ArrayList();

            for (JsonElement jsonElement : ((JsonElement)entry.getValue()).getAsJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                arrayList.add(new TradeBook.PriceSample(jsonArray.get(0).getAsLong(), jsonArray.get(1).getAsLong()));
            }

            this.priceSamples.put((String)entry.getKey(), arrayList);
        }
    }

    private void backupBadFile(Path path) {
        try {
            Files.copy(path, path.resolveSibling(path.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioException) {
            LOGGER.warn("Failed to keep the unreadable trade book: {}", ioException.getMessage());
        }
    }

    private static String getItemId(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    private static String getPlayerName() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    private static Path getFilePath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("trade-book.json");
    }

    public record Lot(String owner, String item, int count, long cost, long time) {
        public double unitCost() {
            return (double)this.cost / this.count;
        }
    }

    record PriceSample(long time, long price) {
    }
}
