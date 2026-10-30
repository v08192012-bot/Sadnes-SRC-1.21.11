package tech.sadnes.client.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class AutoAuthStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoAuth");
    private static final Pattern LINE_PATTERN = Pattern.compile("ip:\\s*(\\S+)\\s*,\\s*nick:\\s*(\\S+)\\s*,\\s*password:\\s*(.+)");
    private static final List<AutoAuthStorage.Entry> ENTRIES = new ArrayList<>();
    private static Path filePath;
    private static Path previewFile;
    private static boolean loaded;

    private AutoAuthStorage() {
    }

    public static String getPassword(String string, String string2) {
        if (string != null && string2 != null) {
            ensureLoaded();
            int i = ENTRIES.size();

            for (int i2 = 0; i2 < i; i2++) {
                AutoAuthStorage.Entry entry = ENTRIES.get(i2);
                if (entry.ip().equalsIgnoreCase(string) && entry.nick().equalsIgnoreCase(string2)) {
                    return entry.password();
                }
            }
        }

        return null;
    }

    public static void setPassword(String string, String string2, String string3) {
        if (string != null && string2 != null && string3 != null && !string3.isEmpty()) {
            ensureLoaded();
            ENTRIES.removeIf(entry -> entry.ip().equalsIgnoreCase(string) && entry.nick().equalsIgnoreCase(string2));
            ENTRIES.add(new AutoAuthStorage.Entry(string, string2, string3));
            save();
        }
    }

    public static void openPreview() {
        ensureLoaded();
        if (previewFile != null) {
            previewFile.toFile().delete();
        }

        try {
            previewFile = Files.createTempFile("sadnes-passwords", ".txt");
            previewFile.toFile().deleteOnExit();
            Files.writeString(previewFile, serialize(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            LOGGER.error("Failed to prepare auth password preview", ioException);
            return;
        }

        Util.getOperatingSystem().open(previewFile);
    }

    private static Path getFilePath() {
        if (filePath != null) {
            return filePath;
        }

        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("autoauth");

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create autoauth directory", ioException);
        }

        filePath = path.resolve("passwords.scfg");
        return filePath;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            Path path = getFilePath();
            if (Files.exists(path)) {
                try {
                    String string = Files.readString(path, StandardCharsets.UTF_8);

                    String string2;
                    try {
                        string2 = DataEncryption.decryptString(string);
                    } catch (Exception exception) {
                        string2 = string;
                    }

                    for (String string3 : string2.split("\\R")) {
                        Matcher matcher2 = LINE_PATTERN.matcher(string3.trim());
                        if (matcher2.matches()) {
                            ENTRIES.add(new AutoAuthStorage.Entry(matcher2.group(1), matcher2.group(2), matcher2.group(3).trim()));
                        }
                    }
                } catch (IOException ioException) {
                    LOGGER.error("Failed to load auth passwords", ioException);
                }
            }
        }
    }

    private static void save() {
        try {
            Files.writeString(getFilePath(), DataEncryption.encryptString(serialize()), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save auth passwords", exception);
        }
    }

    private static String serialize() {
        StringBuilder builder = new StringBuilder(ENTRIES.size() * 64);
        int i = ENTRIES.size();

        for (int i2 = 0; i2 < i; i2++) {
            AutoAuthStorage.Entry entry = ENTRIES.get(i2);
            builder.append("ip: ")
                .append(entry.ip())
                .append(", nick: ")
                .append(entry.nick())
                .append(", password: ")
                .append(entry.password())
                .append(System.lineSeparator());
        }

        return builder.toString();
    }

    record Entry(String ip, String nick, String password) {
    }
}
