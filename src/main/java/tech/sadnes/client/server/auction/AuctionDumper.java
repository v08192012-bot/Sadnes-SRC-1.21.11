package tech.sadnes.client.server.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.server.ServerManager;

public final class AuctionDumper {
    private static final Logger LOGGER = LoggerFactory.getLogger("AuctionDumper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final AuctionPriceParser PRICE_PARSER = new AuctionPriceParser();
    private static final Map<String, JsonObject> LOTS = new LinkedHashMap<>();
    private static Path lotsFile;
    private static String lotsServer;

    private AuctionDumper() {
    }

    public static Path getLotsFile() {
        String string = getServerName();
        if (lotsFile != null && string.equals(lotsServer)) {
            return lotsFile;
        }

        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("autobuy");

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create autobuy directory", ioException);
        }

        lotsFile = path.resolve("lots-" + string + ".json");
        lotsServer = string;
        LOTS.clear();
        loadLots();
        return lotsFile;
    }

    public static int getLotCount() {
        getLotsFile();
        return LOTS.size();
    }

    public static boolean hasPrice(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && PRICE_PARSER.getPrice(itemStack) > 0;
    }

    public static String dumpLot(ItemStack itemStack, String string) {
        if (itemStack != null && !itemStack.isEmpty()) {
            int i = PRICE_PARSER.getPrice(itemStack);
            getLotsFile();
            JsonObject jsonObject = serializeLot(itemStack, string, i);
            if (LOTS.putIfAbsent(jsonObject.get("key").getAsString(), jsonObject) != null) {
                return null;
            }

            saveLots();
            return jsonObject.get("name").getAsString() + " — " + i + " (" + LOTS.size() + ")";
        } else {
            return null;
        }
    }

    private static JsonObject serializeLot(ItemStack itemStack, String string, int i) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("item", Registries.ITEM.getId(itemStack.getItem()).toString());
        jsonObject.addProperty("name", itemStack.getName().getString());
        jsonObject.addProperty("count", itemStack.getCount());
        jsonObject.addProperty("price", i);
        jsonObject.addProperty("unitPrice", itemStack.getCount() > 1 ? i / itemStack.getCount() : i);
        jsonObject.addProperty("server", getServerName());
        if (string != null) {
            jsonObject.addProperty("screen", string);
        }

        String string2 = AuctionLoreUtil.getFunTimeSeller(itemStack);
        if (string2 != null) {
            jsonObject.addProperty("seller", string2);
        }

        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null && !loreComponent.lines().isEmpty()) {
            JsonArray jsonArray = new JsonArray();

            for (Text text : loreComponent.lines()) {
                jsonArray.add(text.getString());
            }

            jsonObject.add("lore", jsonArray);
        }

        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound nbtCompound = nbtComponent.copyNbt();
            jsonObject.addProperty("customData", nbtCompound.toString());
            NbtCompound nbtCompound2 = nbtCompound.getCompoundOrEmpty("PublicBukkitValues");
            String string3 = nbtCompound2.getString("spookyitems:spooky-item", nbtCompound.getString("spooky-item", ""));
            if (!string3.isEmpty()) {
                jsonObject.addProperty("tag", string3);
            }

            int i2 = nbtCompound2.getInt("spookyitems:sphere-item-level", 0);
            if (i2 > 0) {
                jsonObject.addProperty("level", i2);
            }
        }

        ProfileComponent profileComponent = itemStack.get(DataComponentTypes.PROFILE);
        if (profileComponent != null) {
            ProfileComponent.CODEC.encodeStart(JsonOps.INSTANCE, profileComponent).result().ifPresent(jsonElement -> jsonObject.add("profile", jsonElement));
        }

        JsonObject jsonObject2 = new JsonObject();
        writeEnchantments(itemStack.get(DataComponentTypes.ENCHANTMENTS), jsonObject2);
        writeEnchantments(itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS), jsonObject2);
        if (jsonObject2.size() > 0) {
            jsonObject.add("enchants", jsonObject2);
        }

        jsonObject.addProperty("components", itemStack.getComponents().toString());
        jsonObject.addProperty(
            "key", jsonObject.get("item").getAsString() + "|" + jsonObject.get("name").getAsString() + "|" + Integer.toHexString(buildFingerprint(itemStack, jsonObject).hashCode())
        );
        return jsonObject;
    }

    private static String buildFingerprint(ItemStack itemStack, JsonObject jsonObject) {
        StringBuilder builder = new StringBuilder(jsonObject.get("name").getAsString());
        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            for (Text text : loreComponent.lines()) {
                String string = text.getString();
                if (!AuctionLoreUtil.isAuctionInfoLine(string)) {
                    builder.append('|').append(string);
                }
            }
        }

        if (jsonObject.has("enchants")) {
            builder.append('|').append(jsonObject.get("enchants"));
        }

        return builder.toString();
    }

    private static void writeEnchantments(ItemEnchantmentsComponent itemEnchantmentsComponent, JsonObject jsonObject) {
        if (itemEnchantmentsComponent != null && !itemEnchantmentsComponent.isEmpty()) {
            for (RegistryEntry<net.minecraft.enchantment.Enchantment> registryEntry : itemEnchantmentsComponent.getEnchantments()) {
                String string = registryEntry.getKey().map(registryKey -> registryKey.getValue().toString()).orElse(registryEntry.getIdAsString());
                jsonObject.addProperty(string, itemEnchantmentsComponent.getLevel(registryEntry));
            }
        }
    }

    public static String getServerName() {
        if (ServerManager.isFunTime()) {
            return "funtime";
        } else if (ServerManager.isSpookyTime()) {
            return "spookytime";
        } else if (ServerManager.isHolyWorld()) {
            return "holyworld";
        } else {
            return ServerManager.isReallyWorld() ? "reallyworld" : "unknown";
        }
    }

    private static void saveLots() {
        try {
            JsonArray jsonArray = new JsonArray();

            for (JsonObject jsonObject : LOTS.values()) {
                jsonArray.add(jsonObject);
            }

            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.add("lots", jsonArray);
            Files.writeString(lotsFile, GSON.toJson(jsonObject2), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save auction lots", exception);
        }
    }

    private static void loadLots() {
        if (Files.exists(lotsFile)) {
            try {
                JsonObject jsonObject = GSON.fromJson(Files.readString(lotsFile, StandardCharsets.UTF_8), JsonObject.class);
                if (jsonObject == null || !jsonObject.has("lots")) {
                    return;
                }

                for (JsonElement jsonElement : jsonObject.getAsJsonArray("lots")) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if (jsonObject2.has("key")) {
                        LOTS.put(jsonObject2.get("key").getAsString(), jsonObject2);
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load auction lots", exception);
            }
        }
    }
}
