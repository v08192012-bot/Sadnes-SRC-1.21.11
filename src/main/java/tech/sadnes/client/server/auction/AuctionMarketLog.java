package tech.sadnes.client.server.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.math.StopWatch;

public final class AuctionMarketLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("AuctionMarket");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final AuctionPriceParser PRICE_PARSER = new AuctionPriceParser();
    private static final Map<String, JsonObject> ITEMS = new LinkedHashMap<>();
    private static final StopWatch SAVE_TIMER = new StopWatch();
    private static Path marketFile;
    private static String marketServer;
    private static boolean dirty;

    private AuctionMarketLog() {
    }

    public static int getItemCount() {
        getMarketFile();
        return ITEMS.size();
    }

    public static int scanContainer(ScreenHandler screenHandler) {
        if (screenHandler == null) {
            return 0;
        }

        getMarketFile();
        int i = 0;
        int i2 = Math.min(45, screenHandler.slots.size() - 36);

        for (int i3 = 0; i3 < i2; i3++) {
            ItemStack itemStack = screenHandler.getSlot(i3).getStack();
            if (!itemStack.isEmpty() && recordItem(itemStack)) {
                i++;
            }
        }

        if (dirty && SAVE_TIMER.hasPassedAndReset(5000.0)) {
            save();
        }

        return i;
    }

    public static void flush() {
        if (dirty && marketFile != null) {
            save();
        }
    }

    private static boolean recordItem(ItemStack itemStack) {
        JsonObject jsonObject = collectEnchantments(itemStack);
        String string = Registries.ITEM.getId(itemStack.getItem())
            + "|"
            + itemStack.getName().getString()
            + "|"
            + Integer.toHexString(("" + jsonObject + itemStack.get(DataComponentTypes.POTION_CONTENTS)).hashCode());
        JsonObject jsonObject2 = ITEMS.get(string);
        boolean flag = jsonObject2 == null;
        if (flag) {
            jsonObject2 = createEntry(itemStack, jsonObject);
            jsonObject2.addProperty("key", string);
            ITEMS.put(string, jsonObject2);
        }

        jsonObject2.addProperty("seen", jsonObject2.get("seen").getAsInt() + 1);
        dirty = true;
        int i = PRICE_PARSER.getPrice(itemStack);
        if (i <= 0) {
            return flag;
        }

        int i2 = i / Math.max(1, itemStack.getCount());
        JsonElement jsonElement = jsonObject2.get("minUnit");
        if (jsonElement == null || i2 < jsonElement.getAsInt()) {
            jsonObject2.addProperty("minUnit", i2);
        }

        JsonElement jsonElement2 = jsonObject2.get("maxUnit");
        if (jsonElement2 == null || i2 > jsonElement2.getAsInt()) {
            jsonObject2.addProperty("maxUnit", i2);
        }

        return flag;
    }

    private static JsonObject createEntry(ItemStack itemStack, JsonObject jsonObject) {
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("item", Registries.ITEM.getId(itemStack.getItem()).toString());
        jsonObject2.addProperty("name", itemStack.getName().getString());
        jsonObject2.addProperty("seen", 0);
        jsonObject2.addProperty("server", AuctionDumper.getServerName());
        if (jsonObject.size() > 0) {
            jsonObject2.add("enchants", jsonObject);
        }

        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContentsComponent != null) {
            JsonArray jsonArray = new JsonArray();

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                jsonArray.add(statusEffectInstance.toString());
            }

            if (!jsonArray.isEmpty()) {
                jsonObject2.add("effects", jsonArray);
            }
        }

        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            JsonArray jsonArray2 = new JsonArray();

            for (Text text : loreComponent.lines()) {
                String string = text.getString();
                if (!string.isBlank() && !AuctionLoreUtil.isAuctionInfoLine(string)) {
                    jsonArray2.add(string);
                }
            }

            if (!jsonArray2.isEmpty()) {
                jsonObject2.add("lore", jsonArray2);
            }
        }

        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            jsonObject2.addProperty("customData", nbtComponent.copyNbt().toString());
        }

        return jsonObject2;
    }

    private static JsonObject collectEnchantments(ItemStack itemStack) {
        JsonObject jsonObject = new JsonObject();
        writeEnchantments(itemStack.get(DataComponentTypes.ENCHANTMENTS), jsonObject);
        writeEnchantments(itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS), jsonObject);
        return jsonObject;
    }

    private static void writeEnchantments(ItemEnchantmentsComponent itemEnchantmentsComponent, JsonObject jsonObject) {
        if (itemEnchantmentsComponent != null && !itemEnchantmentsComponent.isEmpty()) {
            for (RegistryEntry<net.minecraft.enchantment.Enchantment> registryEntry : itemEnchantmentsComponent.getEnchantments()) {
                String string = registryEntry.getKey().map(registryKey -> registryKey.getValue().toString()).orElse(registryEntry.getIdAsString());
                jsonObject.addProperty(string, itemEnchantmentsComponent.getLevel(registryEntry));
            }
        }
    }

    private static Path getMarketFile() {
        String string = AuctionDumper.getServerName();
        if (marketFile != null && string.equals(marketServer)) {
            return marketFile;
        }

        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("autobuy");

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create autobuy directory", ioException);
        }

        marketFile = path.resolve("market-" + string + ".json");
        marketServer = string;
        ITEMS.clear();
        load();
        return marketFile;
    }

    private static void save() {
        dirty = false;

        try {
            JsonArray jsonArray = new JsonArray();

            for (JsonObject jsonObject : ITEMS.values()) {
                jsonArray.add(jsonObject);
            }

            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.add("items", jsonArray);
            Files.writeString(marketFile, GSON.toJson(jsonObject2), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save auction market log", exception);
        }
    }

    private static void load() {
        if (Files.exists(marketFile)) {
            try {
                JsonObject jsonObject = GSON.fromJson(Files.readString(marketFile, StandardCharsets.UTF_8), JsonObject.class);
                if (jsonObject == null || !jsonObject.has("items")) {
                    return;
                }

                for (JsonElement jsonElement : jsonObject.getAsJsonArray("items")) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if (jsonObject2.has("key")) {
                        ITEMS.put(jsonObject2.get("key").getAsString(), jsonObject2);
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load auction market log", exception);
            }
        }
    }
}
