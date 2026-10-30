package tech.sadnes.client.staff;

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
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class StaffManager extends ArrayList<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger("StaffManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    public StaffManager() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath();
        Path path2 = path.resolve(ClientInfo.MOD_ID).resolve("staff");

        try {
            Files.createDirectories(path2);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create staff directory", ioException);
        }

        this.file = path2.resolve("staff.scfg");
    }

    public boolean isStaff(String string) {
        return this.find(string) != null;
    }

    public String find(String string) {
        if (string == null) {
            return null;
        }

        int i = this.size();

        for (int i2 = 0; i2 < i; i2++) {
            String string2 = this.get(i2);
            if (string2.equalsIgnoreCase(string)) {
                return string2;
            }
        }

        return null;
    }

    public void addStaff(String string) {
        if (string != null && !string.isBlank() && !this.isStaff(string)) {
            this.add(string);
            this.save();
        }
    }

    public void removeStaff(String string) {
        if (string != null) {
            this.removeIf(string2 -> string2.equalsIgnoreCase(string));
            this.save();
        }
    }

    public void clearStaff() {
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

            jsonObject.add("staffs", jsonArray);
            String string2 = GSON.toJson(jsonObject);
            String string3 = DataEncryption.encryptString(string2);
            Files.writeString(this.file, string3, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save staff", exception);
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
                if (jsonObject == null || !jsonObject.has("staffs")) {
                    return;
                }

                this.clear();

                for (JsonElement jsonElement : jsonObject.getAsJsonArray("staffs")) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if (jsonObject2.has("name")) {
                        this.add(jsonObject2.get("name").getAsString());
                    }
                }
            } catch (Exception exception2) {
                LOGGER.error("Failed to load staff", exception2);
            }
        }
    }
}
