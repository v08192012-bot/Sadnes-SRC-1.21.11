package tech.sadnes.client.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class LocalConfigStorage implements ConfigStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("LocalConfigStorage");
    private static final String EXTENSION = ".scfg";
    private final Path directory;

    public LocalConfigStorage(Path path) {
        this.directory = path;

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create config directory: {}", path, ioException);
        }
    }

    public static LocalConfigStorage createDefault() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath();
        return new LocalConfigStorage(path.resolve(ClientInfo.MOD_ID).resolve("config"));
    }

    @Override
    public boolean write(String string, String string2) {
        Path path = this.directory.resolve(string + ".scfg");
        Path path2 = this.directory.resolve(string + ".scfg.tmp");

        try {
            String string3 = DataEncryption.encryptString(string2);
            Files.writeString(path2, string3, StandardCharsets.UTF_8);

            try {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ioException) {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to save config '{}'", string, exception);
            return false;
        }
    }

    @Override
    public String read(String string) {
        Path path = this.directory.resolve(string + ".scfg");
        if (!Files.exists(path)) {
            return null;
        }

        try {
            String string2 = Files.readString(path, StandardCharsets.UTF_8);

            try {
                return DataEncryption.decryptString(string2);
            } catch (Exception exception) {
                return string2;
            }
        } catch (IOException ioException) {
            LOGGER.error("Failed to load config '{}'", string, ioException);
            return null;
        }
    }

    @Override
    public boolean delete(String string) {
        Path path = this.directory.resolve(string + ".scfg");

        try {
            return Files.deleteIfExists(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to delete config '{}'", string, ioException);
            return false;
        }
    }

    @Override
    public List<String> list() {
        ArrayList<String> strings = new ArrayList<>();
        if (!Files.isDirectory(this.directory)) {
            return strings;
        }

        try (Stream<Path> stream= Files.list(this.directory)) {
            stream.filter(path -> path.toString().endsWith(".scfg")).map(path2 -> {
                String string = path2.getFileName().toString();
                return string.substring(0, string.length() - ".scfg".length());
            }).forEach(strings::add);
        } catch (IOException ioException) {
            LOGGER.error("Failed to list configs", ioException);
        }

        return strings;
    }

    @Override
    public boolean exists(String string) {
        return Files.exists(this.directory.resolve(string + ".scfg"));
    }

    @Override
    public Path getDirectory() {
        return this.directory;
    }
}
