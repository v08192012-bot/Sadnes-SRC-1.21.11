package tech.sadnes.client.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class FriendManager extends ArrayList<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger("FriendManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private static final String[] EMPTY = new String[0];
    private volatile String[] sharedFriends = EMPTY;

    public FriendManager() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath();
        Path path2 = path.resolve(ClientInfo.MOD_ID).resolve("friend");

        try {
            Files.createDirectories(path2);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create friend directory", ioException);
        }

        this.file = path2.resolve("friends.scfg");
    }

    public boolean isFriend(String string) {
        return string == null ? false : this.isSharedFriend(string) || this.findLocalFriend(string) != null;
    }

    public String getFriendName(String string) {
        if (string == null) {
            return null;
        } else {
            String string2 = this.findLocalFriend(string);
            if (string2 != null) {
                return string2;
            } else {
                return this.isSharedFriend(string) ? string : null;
            }
        }
    }

    private String findLocalFriend(String string) {
        int i = this.size();

        for (int i2 = 0; i2 < i; i2++) {
            String string2 = this.get(i2);
            if (string2.equalsIgnoreCase(string)) {
                return string2;
            }
        }

        return null;
    }

    private boolean isSharedFriend(String string) {
        for (String string2 : this.sharedFriends) {
            if (string2.equalsIgnoreCase(string)) {
                return true;
            }
        }

        return false;
    }

    public void setSharedFriends(Set<String> strings) {
        if (strings != null && !strings.isEmpty()) {
            ArrayList<String> strings2 = new ArrayList<>(strings.size());

            for (String string : strings) {
                if (string != null && !string.isBlank()) {
                    strings2.add(string);
                }
            }

            this.sharedFriends = strings2.toArray(EMPTY);
        } else {
            this.sharedFriends = EMPTY;
        }
    }

    public void addFriend(String string) {
        if (string != null && !this.isFriend(string)) {
            this.add(string);
            this.save();
        }
    }

    public void removeFriend(String string) {
        if (string != null) {
            this.removeIf(string2 -> string2.equalsIgnoreCase(string));
            this.save();
        }
    }

    public void clearFriends() {
        this.clear();
        this.save();
    }

    public void save() {
        try {
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = new JsonArray();

            for (String string : this) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("name", string);
                jsonArray.add(jsonObject2);
            }

            jsonObject.add("friends", jsonArray);
            String string2 = GSON.toJson(jsonObject);
            String string3 = DataEncryption.encryptString(string2);
            Files.writeString(this.file, string3, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save friends", exception);
        }
    }

    public void load() {
        if (Files.exists(this.file)) {
            try {
                String string = Files.readString(this.file, StandardCharsets.UTF_8);

                String string2;
                try {
                    string2 = DataEncryption.decryptString(string);
                } catch (Exception exception) {
                    string2 = string;
                }

                JsonObject jsonObject = GSON.fromJson(string2, JsonObject.class);
                if (jsonObject == null || !jsonObject.has("friends")) {
                    return;
                }

                this.clear();

                for (JsonElement jsonElement : jsonObject.getAsJsonArray("friends")) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if (jsonObject2.has("name")) {
                        this.add(jsonObject2.get("name").getAsString());
                    }
                }
            } catch (Exception exception2) {
                LOGGER.error("Failed to load friends", exception2);
            }
        }
    }
}
