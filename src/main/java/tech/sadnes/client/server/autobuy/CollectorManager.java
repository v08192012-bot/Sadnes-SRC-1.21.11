package tech.sadnes.client.server.autobuy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.config.cloud.CloudConfigSync;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.ConfigKind;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerType;

public final class CollectorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CollectorManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int SLOT_COUNT = 41;
    public static final int MAIN_INVENTORY_SIZE = 27;
    public static final int ARMOR_SLOT_START = 36;
    public static final int OFFHAND_SLOT = 40;
    public static final int MAX_BUNDLE_ENTRIES = 4;
    public static final int MAX_BUNDLES = 3;
    public static final List<ServerType> SUPPORTED_SERVERS = List.of(ServerType.FUNTIME, ServerType.SPOOKYTIME, ServerType.HOLYWORLD);
    private static final Set<Item> VANILLA_ITEMS = Set.of(
        Items.TOTEM_OF_UNDYING,
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.GOLDEN_APPLE,
        Items.GOLDEN_CARROT,
        Items.CHORUS_FRUIT,
        Items.ENDER_PEARL,
        Items.WIND_CHARGE,
        Items.FIREWORK_ROCKET,
        Items.EXPERIENCE_BOTTLE,
        Items.SHULKER_BOX,
        Items.BUNDLE,
        Items.DIAMOND_SWORD,
        Items.DIAMOND_HELMET,
        Items.DIAMOND_CHESTPLATE,
        Items.DIAMOND_LEGGINGS,
        Items.DIAMOND_BOOTS
    );
    private static final Set<Item> LOW_PRIORITY_ITEMS = Set.of(Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.CHORUS_FRUIT);
    private static final Set<Item> DONATE_ITEMS = Set.of(
        Items.SUGAR,
        Items.ENDER_EYE,
        Items.DRIED_KELP,
        Items.NETHERITE_SCRAP,
        Items.FIRE_CHARGE,
        Items.SNOWBALL,
        Items.POPPED_CHORUS_FRUIT,
        Items.PRISMARINE_SHARD,
        Items.NETHER_STAR,
        Items.RED_DYE,
        Items.ORANGE_DYE,
        Items.PINK_SHULKER_BOX,
        Items.LIGHT_BLUE_SHULKER_BOX,
        Items.RED_SHULKER_BOX,
        Items.MAGENTA_SHULKER_BOX,
        Items.LIME_SHULKER_BOX
    );
    private final AutoBuyManager autoBuyManager;
    private final Map<ServerType, CollectorManager.CollectorSlot[]> layouts = new EnumMap<>(ServerType.class);
    private final Map<String, Integer> prices = new HashMap<>();
    private final List<CollectorManager.Preset> presets = new ArrayList<>();
    private final Map<ServerType, String> activePresets = new EnumMap<>(ServerType.class);
    private final Path configPath;
    private ServerType server = ServerType.FUNTIME;
    private CloudConfigSync cloudConfig;

    public CollectorManager(AutoBuyManager autoBuyManager2) {
        this.autoBuyManager = autoBuyManager2;

        for (ServerType serverType : SUPPORTED_SERVERS) {
            CollectorManager.CollectorSlot[] collectorSlots = new CollectorManager.CollectorSlot[41];

            for (int i = 0; i < 41; i++) {
                collectorSlots[i] = new CollectorManager.CollectorSlot();
            }

            this.layouts.put(serverType, collectorSlots);
        }

        this.configPath = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("autobuy").resolve("collector.json");
    }

    public void setServer(ServerType serverType) {
        if (serverType != this.server && this.layouts.containsKey(serverType)) {
            this.server = serverType;
            this.save();
        }
    }

    private CollectorManager.CollectorSlot[] getCurrentLayout() {
        return this.layouts.get(this.server);
    }

    public List<AutoBuyItem> getAvailableItems() {
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();

        for (AutoBuyItem autoBuyItem : this.autoBuyManager.getItems()) {
            if (autoBuyItem.isAvailableOn(this.server) && this.isCollectable(autoBuyItem) && hashSet.add(autoBuyItem.getId())) {
                arrayList.add(autoBuyItem);
            }
        }

        arrayList.sort(Comparator.comparingInt(CollectorManager::sortPriority));
        return arrayList;
    }

    private static int sortPriority(AutoBuyItem autoBuyItem) {
        Item item = autoBuyItem.getItem();
        switch (autoBuyItem.getSource()) {
            case CRUSHER:
                if (isEquippable(item)) {
                    return 0;
                }

                if (item == Items.NETHERITE_SWORD) {
                    return 11;
                }

                if (item == Items.MACE) {
                    return 12;
                }

                return 13;
            case SPHERE:
                if (item == Items.TOTEM_OF_UNDYING) {
                    return 20;
                }

                return 21;
            case DONATE:
                break;
            case POTION:
                if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) {
                    break;
                }

                return 40;
            default:
                if (LOW_PRIORITY_ITEMS.contains(item)) {
                    return 50;
                }

                return 60;
        }

        return 30;
    }

    public static boolean isEquippable(Item item) {
        EquippableComponent equippableComponent = item.getComponents().get(DataComponentTypes.EQUIPPABLE);
        return equippableComponent != null && equippableComponent.slot().isArmorSlot();
    }

    private boolean isCollectable(AutoBuyItem autoBuyItem) {
        switch (autoBuyItem.getSource()) {
            case CRUSHER:
            case SPHERE:
            case POTION:
            case GEAR:
                return true;
            case DONATE:
                return DONATE_ITEMS.contains(autoBuyItem.getItem());
            case VANILLA:
                return VANILLA_ITEMS.contains(autoBuyItem.getItem());
            default:
                return false;
        }
    }

    public CollectorManager.CollectorSlot getSlot(int i) {
        return this.getCurrentLayout()[i];
    }

    public boolean isUnavailable(AutoBuyItem autoBuyItem) {
        return autoBuyItem != null && !autoBuyItem.isAvailableOn(this.server);
    }

    public int countUnavailable() {
        int i = 0;

        for (CollectorManager.CollectorSlot collectorSlot : this.getCurrentLayout()) {
            if (this.isUnavailable(collectorSlot.item)) {
                i++;
            }

            for (CollectorManager.BundleEntry bundleEntry : collectorSlot.bundleEntries) {
                if (this.isUnavailable(bundleEntry.item)) {
                    i++;
                }
            }
        }

        return i;
    }

    public boolean canPlace(int i, AutoBuyItem autoBuyItem) {
        if (autoBuyItem.getItem() == Items.BUNDLE && this.countBundlesExcept(i) >= 3) {
            return false;
        }

        EquipmentSlot equipmentSlot = switch (i) {
            case 36 -> EquipmentSlot.HEAD;
            case 37 -> EquipmentSlot.CHEST;
            case 38 -> EquipmentSlot.LEGS;
            case 39 -> EquipmentSlot.FEET;
            default -> null;
        };
        if (equipmentSlot == null) {
            return true;
        }

        EquippableComponent equippableComponent = autoBuyItem.getItem().getComponents().get(DataComponentTypes.EQUIPPABLE);
        return equippableComponent != null && equippableComponent.slot() == equipmentSlot;
    }

    private int countBundlesExcept(int i) {
        int i2 = 0;
        CollectorManager.CollectorSlot[] collectorSlots = this.getCurrentLayout();

        for (int i3 = 0; i3 < 41; i3++) {
            if (i3 != i && collectorSlots[i3].isBundle()) {
                i2++;
            }
        }

        return i2;
    }

    public void placeItem(int i, AutoBuyItem autoBuyItem) {
        this.placeItem(i, autoBuyItem, autoBuyItem.getMaxCount());
    }

    public void placeItem(int i, AutoBuyItem autoBuyItem, int i2) {
        if (this.canPlace(i, autoBuyItem)) {
            CollectorManager.CollectorSlot collectorSlot = this.getCurrentLayout()[i];
            collectorSlot.item = autoBuyItem;
            collectorSlot.count = Math.clamp(i2, 1, autoBuyItem.getMaxCount());
            collectorSlot.bundleEntries.clear();
            this.save();
        }
    }

    public void clearSlot(int i) {
        CollectorManager.CollectorSlot collectorSlot = this.getCurrentLayout()[i];
        collectorSlot.item = null;
        collectorSlot.count = 0;
        collectorSlot.bundleEntries.clear();
        this.save();
    }

    public void clearLayout() {
        this.activePresets.remove(this.server);
        this.resetSlots();
        this.save();
    }

    private void resetSlots() {
        for (CollectorManager.CollectorSlot collectorSlot : this.getCurrentLayout()) {
            collectorSlot.item = null;
            collectorSlot.count = 0;
            collectorSlot.bundleEntries.clear();
        }
    }

    public void setSlotCount(int i, int i2) {
        CollectorManager.CollectorSlot collectorSlot = this.getCurrentLayout()[i];
        if (!collectorSlot.isEmpty()) {
            collectorSlot.count = Math.clamp(i2, 1, collectorSlot.item.getMaxCount());
        }
    }

    public int addToBundle(int i, AutoBuyItem autoBuyItem, int i2) {
        CollectorManager.CollectorSlot collectorSlot = this.getCurrentLayout()[i];
        if (collectorSlot.isBundle() && autoBuyItem.getItem() != Items.BUNDLE) {
            int i3 = autoBuyItem.getMaxCount();
            int i4 = 0;

            for (CollectorManager.BundleEntry bundleEntry : collectorSlot.bundleEntries) {
                if (bundleEntry.item == autoBuyItem && i4 < i2) {
                    int i5 = Math.min(i2 - i4, i3 - bundleEntry.count);
                    if (i5 > 0) {
                        bundleEntry.count += i5;
                        i4 += i5;
                    }
                }
            }

            while (i4 < i2 && collectorSlot.bundleEntries.size() < 4) {
                int i6 = Math.min(i2 - i4, i3);
                collectorSlot.bundleEntries.add(new CollectorManager.BundleEntry(autoBuyItem, i6));
                i4 += i6;
            }

            if (i4 > 0) {
                this.save();
            }

            return i4;
        } else {
            return 0;
        }
    }

    public void removeFromBundle(int i, CollectorManager.BundleEntry bundleEntry) {
        this.getCurrentLayout()[i].bundleEntries.remove(bundleEntry);
        this.save();
    }

    public void setBundleEntryCount(CollectorManager.BundleEntry bundleEntry, int i) {
        bundleEntry.count = Math.clamp(i, 1, bundleEntry.item.getMaxCount());
    }

    public void setBundleEntries(int i, List<CollectorManager.BundleEntry> bundleEntries2) {
        CollectorManager.CollectorSlot collectorSlot = this.getCurrentLayout()[i];
        if (collectorSlot.isBundle()) {
            collectorSlot.bundleEntries.clear();
            collectorSlot.bundleEntries.addAll(bundleEntries2);
            this.save();
        }
    }

    public boolean hasPrice(AutoBuyItem autoBuyItem) {
        return this.prices.containsKey(autoBuyItem.getName());
    }

    public int getPrice(AutoBuyItem autoBuyItem) {
        return this.prices.getOrDefault(autoBuyItem.getName(), 0);
    }

    public void setPrice(AutoBuyItem autoBuyItem, int i) {
        this.prices.put(autoBuyItem.getName(), i);
        this.save();
    }

    public long getTotalCost() {
        long l = 0L;

        for (CollectorManager.CollectorSlot collectorSlot : this.getCurrentLayout()) {
            if (!collectorSlot.isEmpty()) {
                l += (long)this.getPrice(collectorSlot.item) * collectorSlot.count;

                for (CollectorManager.BundleEntry bundleEntry : collectorSlot.bundleEntries) {
                    l += (long)this.getPrice(bundleEntry.item) * bundleEntry.count;
                }
            }
        }

        return l;
    }

    public List<CollectorManager.Preset> getPresets() {
        return Collections.unmodifiableList(this.presets);
    }

    public String getActivePresetName() {
        return this.activePresets.get(this.server);
    }

    public void savePreset(String string) {
        CollectorManager.Preset preset = new CollectorManager.Preset(string, this.snapshotSlots());
        this.presets.removeIf(preset2 -> preset2.name().equals(string));
        this.presets.add(preset);
        this.activePresets.put(this.server, string);
        this.save();
    }

    private List<CollectorManager.SlotData> snapshotSlots() {
        ArrayList arrayList = new ArrayList();
        CollectorManager.CollectorSlot[] collectorSlots = this.getCurrentLayout();

        for (int i = 0; i < 41; i++) {
            CollectorManager.CollectorSlot collectorSlot = collectorSlots[i];
            if (!collectorSlot.isEmpty()) {
                ArrayList arrayList2 = new ArrayList();

                for (CollectorManager.BundleEntry bundleEntry : collectorSlot.bundleEntries) {
                    arrayList2.add(new CollectorManager.BundleEntryData(bundleEntry.item.getName(), bundleEntry.count));
                }

                arrayList.add(new CollectorManager.SlotData(i, collectorSlot.item.getName(), collectorSlot.count, List.copyOf(arrayList2)));
            }
        }

        return List.copyOf(arrayList);
    }

    private void updateActivePreset() {
        String string = this.activePresets.get(this.server);
        if (string != null) {
            int i = this.presets.size();

            for (int i2 = 0; i2 < i; i2++) {
                if (this.presets.get(i2).name().equals(string)) {
                    this.presets.set(i2, new CollectorManager.Preset(string, this.snapshotSlots()));
                    return;
                }
            }

            this.activePresets.remove(this.server);
        }
    }

    public void deletePreset(CollectorManager.Preset preset) {
        this.presets.remove(preset);
        this.activePresets.values().removeIf(string -> string.equals(preset.name()));
        this.save();
    }

    public void applyPreset(CollectorManager.Preset preset) {
        this.activePresets.put(this.server, preset.name());
        this.resetSlots();
        CollectorManager.CollectorSlot[] collectorSlots = this.getCurrentLayout();

        for (CollectorManager.SlotData slotData : preset.slots()) {
            AutoBuyItem autoBuyItem = this.autoBuyManager.getByName(slotData.id());
            if (autoBuyItem != null && !this.isUnavailable(autoBuyItem) && slotData.slot() >= 0 && slotData.slot() < 41) {
                CollectorManager.CollectorSlot collectorSlot = collectorSlots[slotData.slot()];
                collectorSlot.item = autoBuyItem;
                collectorSlot.count = Math.clamp(slotData.count(), 1, autoBuyItem.getMaxCount());

                for (CollectorManager.BundleEntryData bundleEntryData : slotData.bundle()) {
                    if (collectorSlot.bundleEntries.size() >= 4) {
                        break;
                    }

                    AutoBuyItem autoBuyItem2 = this.autoBuyManager.getByName(bundleEntryData.id());
                    if (autoBuyItem2 != null && !this.isUnavailable(autoBuyItem2)) {
                        collectorSlot.bundleEntries.add(new CollectorManager.BundleEntry(autoBuyItem2, Math.clamp(bundleEntryData.count(), 1, autoBuyItem2.getMaxCount())));
                    }
                }
            }
        }

        this.save();
    }

    public void save() {
        this.updateActivePreset();

        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("server", this.server.name());
            JsonObject jsonObject2 = new JsonObject();

            for (Entry entry : this.activePresets.entrySet()) {
                jsonObject2.addProperty(((ServerType)entry.getKey()).name(), (String)entry.getValue());
            }

            jsonObject.add("activePresets", jsonObject2);
            JsonObject jsonObject3 = new JsonObject();

            for (Entry entry2 : this.layouts.entrySet()) {
                jsonObject3.add(((ServerType)entry2.getKey()).name(), serializeLayout((CollectorManager.CollectorSlot[])entry2.getValue()));
            }

            jsonObject.add("layouts", jsonObject3);
            jsonObject.add("prices", this.serializePrices());
            jsonObject.add("presets", this.serializePresets());
            Files.writeString(this.configPath, GSON.toJson(jsonObject), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save collector layout", exception);
        }
    }

    private JsonArray serializePresets() {
        JsonArray jsonArray = new JsonArray();

        for (CollectorManager.Preset preset : this.presets) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", preset.name());
            JsonArray jsonArray2 = new JsonArray();

            for (CollectorManager.SlotData slotData : preset.slots()) {
                JsonObject jsonObject2 = serializeSlot(slotData.slot(), slotData.id(), slotData.count());
                if (!slotData.bundle().isEmpty()) {
                    JsonArray jsonArray3 = new JsonArray();

                    for (CollectorManager.BundleEntryData bundleEntryData : slotData.bundle()) {
                        jsonArray3.add(serializeEntry(bundleEntryData.id(), bundleEntryData.count()));
                    }

                    jsonObject2.add("bundle", jsonArray3);
                }

                jsonArray2.add(jsonObject2);
            }

            jsonObject.add("slots", jsonArray2);
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    private JsonObject serializePrices() {
        JsonObject jsonObject = new JsonObject();

        for (Entry entry : this.prices.entrySet()) {
            jsonObject.addProperty((String)entry.getKey(), (Number)entry.getValue());
        }

        return jsonObject;
    }

    private static JsonArray serializeLayout(CollectorManager.CollectorSlot[] collectorSlots) {
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < 41; i++) {
            CollectorManager.CollectorSlot collectorSlot = collectorSlots[i];
            if (!collectorSlot.isEmpty()) {
                JsonObject jsonObject = serializeSlot(i, collectorSlot.item.getName(), collectorSlot.count);
                if (!collectorSlot.bundleEntries.isEmpty()) {
                    JsonArray jsonArray2 = new JsonArray();

                    for (CollectorManager.BundleEntry bundleEntry : collectorSlot.bundleEntries) {
                        jsonArray2.add(serializeEntry(bundleEntry.item.getName(), bundleEntry.count));
                    }

                    jsonObject.add("bundle", jsonArray2);
                }

                jsonArray.add(jsonObject);
            }
        }

        return jsonArray;
    }

    private static JsonObject serializeSlot(int i, String string, int i2) {
        JsonObject jsonObject = serializeEntry(string, i2);
        jsonObject.addProperty("slot", i);
        return jsonObject;
    }

    private static JsonObject serializeEntry(String string, int i) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", string);
        jsonObject.addProperty("count", i);
        return jsonObject;
    }

    public void load() {
        if (Files.exists(this.configPath)) {
            try {
                JsonObject jsonObject = GSON.fromJson(Files.readString(this.configPath, StandardCharsets.UTF_8), JsonObject.class);
                if (jsonObject == null) {
                    return;
                }

                if (jsonObject.has("server")) {
                    try {
                        ServerType serverType = ServerType.valueOf(jsonObject.get("server").getAsString());
                        if (this.layouts.containsKey(serverType)) {
                            this.server = serverType;
                        }
                    } catch (IllegalArgumentException illegalArgumentException) {
                    }
                }

                if (jsonObject.has("layouts")) {
                    for (Entry entry : this.layouts.entrySet()) {
                        JsonObject jsonObject2 = jsonObject.getAsJsonObject("layouts");
                        if (jsonObject2.has(((ServerType)entry.getKey()).name())) {
                            this.loadLayout(jsonObject2.getAsJsonArray(((ServerType)entry.getKey()).name()), (CollectorManager.CollectorSlot[])entry.getValue());
                        }
                    }
                } else if (jsonObject.has("slots")) {
                    this.loadLayout(jsonObject.getAsJsonArray("slots"), this.layouts.get(ServerType.FUNTIME));
                }

                if (jsonObject.has("prices")) {
                    for (Entry entry2 : jsonObject.getAsJsonObject("prices").entrySet()) {
                        this.prices.put((String)entry2.getKey(), ((JsonElement)entry2.getValue()).getAsInt());
                    }
                }

                if (jsonObject.has("activePresets")) {
                    for (Entry entry3 : jsonObject.getAsJsonObject("activePresets").entrySet()) {
                        try {
                            ServerType serverType2 = ServerType.valueOf((String)entry3.getKey());
                            if (this.layouts.containsKey(serverType2)) {
                                this.activePresets.put(serverType2, ((JsonElement)entry3.getValue()).getAsString());
                            }
                        } catch (IllegalArgumentException illegalArgumentException2) {
                        }
                    }
                }

                if (jsonObject.has("presets")) {
                    for (JsonElement jsonElement : jsonObject.getAsJsonArray("presets")) {
                        this.presets.add(parsePreset(jsonElement.getAsJsonObject()));
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load collector layout", exception);
            }
        }
    }

    private static CollectorManager.Preset parsePreset(JsonObject jsonObject) {
        ArrayList arrayList = new ArrayList();

        for (JsonElement jsonElement : jsonObject.getAsJsonArray("slots")) {
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
            ArrayList arrayList2 = new ArrayList();
            if (jsonObject2.has("bundle")) {
                for (JsonElement jsonElement2 : jsonObject2.getAsJsonArray("bundle")) {
                    JsonObject jsonObject3 = jsonElement2.getAsJsonObject();
                    arrayList2.add(new CollectorManager.BundleEntryData(jsonObject3.get("id").getAsString(), jsonObject3.get("count").getAsInt()));
                }
            }

            arrayList.add(new CollectorManager.SlotData(jsonObject2.get("slot").getAsInt(), jsonObject2.get("id").getAsString(), jsonObject2.get("count").getAsInt(), List.copyOf(arrayList2)));
        }

        return new CollectorManager.Preset(jsonObject.get("name").getAsString(), List.copyOf(arrayList));
    }

    public void initCloudConfig(SadnesSocketClient sadnesSocketClient) {
        this.cloudConfig = new CloudConfigSync(ConfigKind.COLLECTOR, sadnesSocketClient, this::exportCloudConfig, this::importCloudConfig);
    }

    public String exportCloudConfig() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("prices", this.serializePrices());
        jsonObject.add("presets", this.serializePresets());
        return GSON.toJson(jsonObject);
    }

    public boolean importCloudConfig(String string) {
        if (string == null) {
            return false;
        }

        try {
            JsonObject jsonObject = GSON.fromJson(string, JsonObject.class);
            if (jsonObject == null) {
                return false;
            }

            if (jsonObject.has("prices")) {
                for (Entry entry : jsonObject.getAsJsonObject("prices").entrySet()) {
                    this.prices.putIfAbsent((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsInt());
                }
            }

            if (jsonObject.has("presets")) {
                for (JsonElement jsonElement : jsonObject.getAsJsonArray("presets")) {
                    CollectorManager.Preset preset = parsePreset(jsonElement.getAsJsonObject());
                    this.presets.add(new CollectorManager.Preset(this.uniquePresetName(preset.name()), preset.slots()));
                }
            }

            this.save();
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to apply cloud collector config", exception);
            return false;
        }
    }

    private String uniquePresetName(String string) {
        String string2 = string;

        for (int i = 2; this.presetExists(string2); i++) {
            string2 = string + " (" + i + ")";
        }

        return string2;
    }

    private boolean presetExists(String string) {
        for (CollectorManager.Preset preset : this.presets) {
            if (preset.name().equals(string)) {
                return true;
            }
        }

        return false;
    }

    private void loadLayout(JsonArray jsonArray, CollectorManager.CollectorSlot[] collectorSlots) {
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int i = jsonObject.get("slot").getAsInt();
            AutoBuyItem autoBuyItem = this.autoBuyManager.getByName(jsonObject.get("id").getAsString());
            if (i >= 0 && i < 41 && autoBuyItem != null) {
                CollectorManager.CollectorSlot collectorSlot = collectorSlots[i];
                collectorSlot.item = autoBuyItem;
                collectorSlot.count = Math.clamp(jsonObject.get("count").getAsInt(), 1, autoBuyItem.getMaxCount());
                if (jsonObject.has("bundle")) {
                    for (JsonElement jsonElement2 : jsonObject.getAsJsonArray("bundle")) {
                        if (collectorSlot.bundleEntries.size() >= 4) {
                            break;
                        }

                        JsonObject jsonObject2 = jsonElement2.getAsJsonObject();
                        AutoBuyItem autoBuyItem2 = this.autoBuyManager.getByName(jsonObject2.get("id").getAsString());
                        if (autoBuyItem2 != null) {
                            collectorSlot.bundleEntries.add(new CollectorManager.BundleEntry(autoBuyItem2, Math.clamp(jsonObject2.get("count").getAsInt(), 1, autoBuyItem2.getMaxCount())));
                        }
                    }
                }
            }
        }
    }

    public ServerType getServer() {
        return this.server;
    }

    public CloudConfigSync getCloudConfig() {
        return this.cloudConfig;
    }

    public static final class BundleEntry {
        final AutoBuyItem item;
        int count;

        BundleEntry(AutoBuyItem autoBuyItem, int i) {
            this.item = autoBuyItem;
            this.count = i;
        }

        public AutoBuyItem getItem() {
            return this.item;
        }

        public int getCount() {
            return this.count;
        }
    }

    public record BundleEntryData(String id, int count) {
    }

    public static final class CollectorSlot {
        AutoBuyItem item;
        int count;
        final List<CollectorManager.BundleEntry> bundleEntries = new ArrayList<>();

        public boolean isEmpty() {
            return this.item == null;
        }

        public boolean isBundle() {
            return this.item != null && this.item.getItem() == Items.BUNDLE;
        }

        public List<CollectorManager.BundleEntry> getBundleEntries() {
            return Collections.unmodifiableList(this.bundleEntries);
        }

        public AutoBuyItem getItem() {
            return this.item;
        }

        public int getCount() {
            return this.count;
        }
    }

    public record Preset(String name, List<CollectorManager.SlotData> slots) {
    }

    public record SlotData(int slot, String id, int count, List<CollectorManager.BundleEntryData> bundle) {
    }
}
