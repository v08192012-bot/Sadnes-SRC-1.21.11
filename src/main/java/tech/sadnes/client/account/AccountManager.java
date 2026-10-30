package tech.sadnes.client.account;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class AccountManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AccountManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<Account> accounts = new ArrayList<>();
    private final Path file;
    private Account activeAccount;

    public AccountManager() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath();
        Path path2 = path.resolve(ClientInfo.MOD_ID).resolve("accounts");

        try {
            Files.createDirectories(path2);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create accounts directory", ioException);
        }

        this.file = path2.resolve("accounts.scfg");
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }

    public boolean isActive(Account account) {
        return account != null && account == this.activeAccount;
    }

    public Account addOffline(String string) {
        if (string == null) {
            return null;
        }

        String string2 = string.trim();
        if (string2.isEmpty()) {
            return null;
        }

        Account account = Account.offline(string2);
        this.accounts.add(account);
        this.save();
        return account;
    }

    public void add(Account account) {
        if (account != null) {
            this.accounts.add(account);
            this.save();
        }
    }

    public void remove(Account account) {
        if (account != null) {
            this.accounts.remove(account);
            if (this.activeAccount == account) {
                this.activeAccount = null;
            }

            this.save();
        }
    }

    public void removeNonFavorites() {
        if (!this.accounts.isEmpty()) {
            if (this.activeAccount != null && !this.activeAccount.isFavorite()) {
                this.activeAccount = null;
            }

            this.accounts.removeIf(account2 -> !account2.isFavorite());
            this.save();
        }
    }

    public boolean hasNonFavorites() {
        Iterator iterator2 = this.accounts.iterator();

        while (iterator2.hasNext()) {
            if (!((Account)iterator2.next()).isFavorite()) {
                return true;
            }
        }

        return false;
    }

    public boolean apply(Account account) {
        if (account == null) {
            return false;
        }

        try {
            UUID uuid = this.resolveUuid(account);
            String string = account.getAccessToken() == null ? "0" : account.getAccessToken();
            Session session = new Session(account.getUsername(), uuid, string, Optional.empty(), Optional.empty());
            ((MinecraftClientAccessor)MinecraftClient.getInstance()).sadnes$setSession(session);
            this.activeAccount = account;
            this.save();
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to apply account", exception);
            return false;
        }
    }

    private UUID resolveUuid(Account account) {
        String string = account.getUuid();
        if (string != null && !string.isBlank()) {
            try {
                return parseUuid(string);
            } catch (Exception exception) {
            }
        }

        return offlineUuid(account.getUsername());
    }

    public static UUID offlineUuid(String string) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + string).getBytes(StandardCharsets.UTF_8));
    }

    private static UUID parseUuid(String string) {
        if (string.contains("-")) {
            return UUID.fromString(string);
        }

        String string2 = string.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
        return UUID.fromString(string2);
    }

    public void save() {
        try {
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = new JsonArray();

            for (Account account : this.accounts) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("type", account.getType().name());
                jsonObject2.addProperty("username", account.getUsername());
                if (account.getUuid() != null) {
                    jsonObject2.addProperty("uuid", account.getUuid());
                }

                if (account.getAccessToken() != null) {
                    jsonObject2.addProperty("accessToken", account.getAccessToken());
                }

                if (account.getMsAuth() != null) {
                    jsonObject2.addProperty("msAuth", account.getMsAuth());
                }

                if (account.isCookieAccount()) {
                    jsonObject2.addProperty("cookieLogin", true);
                }

                if (account.isRefreshTokenAccount()) {
                    jsonObject2.addProperty("refreshTokenLogin", true);
                }

                if (account.isFavorite()) {
                    jsonObject2.addProperty("favorite", true);
                }

                jsonArray.add(jsonObject2);
            }

            jsonObject.add("accounts", jsonArray);
            jsonObject.addProperty("active", this.activeAccount == null ? -1 : this.accounts.indexOf(this.activeAccount));
            String string = GSON.toJson(jsonObject);
            String string2 = DataEncryption.encryptString(string);
            Files.writeString(this.file, string2, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save accounts", exception);
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
                if (jsonObject != null && jsonObject.has("accounts")) {
                    this.accounts.clear();
                    JsonArray jsonArray = jsonObject.getAsJsonArray("accounts");
                    Iterator iterator2 = jsonArray.iterator();

                    while (true) {
                        JsonObject jsonObject2;
                        Account.Type type;
                        while (true) {
                            if (!iterator2.hasNext()) {
                                int i = jsonObject.has("active") ? jsonObject.get("active").getAsInt() : -1;
                                Account account = i >= 0 && i < this.accounts.size() ? this.accounts.get(i) : null;
                                if (account != null) {
                                    this.apply(account);
                                }

                                return;
                            }

                            JsonElement jsonElement = (JsonElement)iterator2.next();
                            jsonObject2 = jsonElement.getAsJsonObject();
                            if (jsonObject2.has("type") && jsonObject2.has("username")) {
                                try {
                                    type = Account.Type.valueOf(jsonObject2.get("type").getAsString());
                                    break;
                                } catch (IllegalArgumentException illegalArgumentException) {
                                }
                            }
                        }

                        String string3 = jsonObject2.get("username").getAsString();
                        String string4 = jsonObject2.has("uuid") ? jsonObject2.get("uuid").getAsString() : null;
                        String string5 = jsonObject2.has("accessToken") ? jsonObject2.get("accessToken").getAsString() : "0";
                        String string6 = jsonObject2.has("msAuth") ? jsonObject2.get("msAuth").getAsString() : null;
                        boolean flag = jsonObject2.has("cookieLogin") && jsonObject2.get("cookieLogin").getAsBoolean();
                        boolean flag2 = jsonObject2.has("refreshTokenLogin") && jsonObject2.get("refreshTokenLogin").getAsBoolean();
                        Account account2 = new Account(type, string3, string4, string5, string6, flag, flag2);
                        account2.setFavorite(jsonObject2.has("favorite") && jsonObject2.get("favorite").getAsBoolean());
                        this.accounts.add(account2);
                    }
                }
            } catch (Exception exception2) {
                LOGGER.error("Failed to load accounts", exception2);
            }
        }
    }

    public Account getActiveAccount() {
        return this.activeAccount;
    }
}
