package tech.sadnes.client.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import tech.sadnes.client.util.crypto.AssetCrypto;
import ua.mintantileak.profile.Profile;

public final class ScriptPackage {
    public static final String EXTENSION = ".ss";
    public static final String COMPILED_DIR = "compiled";
    private final Map<String, byte[]> entries;
    private Path assetsDir;

    private ScriptPackage(Map<String, byte[]> map) {
        this.entries = map;
    }

    public static Path compile(Path path) throws IOException {
        Path path2 = path.resolveSibling("compiled").resolve(path.getFileName() + ".ss");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.setComment("author=" + Profile.getUsername() + "\nuid=" + Profile.getUid());

            for (String string : List.of("src", "assets")) {
                Path path3 = path.resolve(string);
                if (Files.isDirectory(path3)) {
                    List<Path> paths;
                    try (Stream<Path> stream = Files.walk(path3)) {
                        paths = stream.filter(path4 -> Files.isRegularFile(path4)).sorted().toList();
                    }

                    for (Path path5 : paths) {
                        zipOutputStream.putNextEntry(new ZipEntry(string + "/" + path3.relativize(path5).toString().replace('\\', '/')));
                        zipOutputStream.write(Files.readAllBytes(path5));
                        zipOutputStream.closeEntry();
                    }
                }
            }
        }

        Files.createDirectories(path2.getParent());
        Files.write(path2, AssetCrypto.encrypt(byteArrayOutputStream.toByteArray()));
        return path2;
    }

    public static ScriptPackage load(Path path) throws IOException {
        byte[] bytes = AssetCrypto.decrypt(Files.readAllBytes(path));
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);

        try {
            for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream.getNextEntry()) {
                if (!zipEntry.isDirectory()) {
                    String string = zipEntry.getName().replace('\\', '/');
                    if (!string.contains("..")) {
                        linkedHashMap.put(string, zipInputStream.readAllBytes());
                    }
                }
            }
        } catch (Throwable throwable) {
            try {
                zipInputStream.close();
            } catch (Throwable throwable2) {
                throwable.addSuppressed(throwable2);
            }

            throw throwable;
        }

        zipInputStream.close();
        if (linkedHashMap.isEmpty()) {
            throw new IOException("пустой или повреждённый пакет");
        } else {
            return new ScriptPackage(linkedHashMap);
        }
    }

    public void extractTo(Path path) throws IOException {
        Files.createDirectories(path.resolve("src"));
        Files.createDirectories(path.resolve("assets"));

        for (Entry entry : this.entries.entrySet()) {
            Path path2 = path.resolve((String)entry.getKey());
            Files.createDirectories(path2.getParent());
            Files.write(path2, (byte[])entry.getValue());
        }
    }

    public static ScriptPackage empty() {
        return new ScriptPackage(Map.of());
    }

    public String readSource(String string) {
        byte[] bytes = this.entries.get("src/" + string.replace('\\', '/'));
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    public Path getAssetsDir() {
        if (this.assetsDir != null) {
            return this.assetsDir;
        }

        try {
            this.assetsDir = Files.createTempDirectory("sadnes-script-");
            Files.createDirectories(this.assetsDir.resolve("assets"));
            String string = "assets/";

            for (Entry entry : this.entries.entrySet()) {
                if (((String)entry.getKey()).startsWith(string)) {
                    Path path = this.assetsDir.resolve((String)entry.getKey());
                    Files.createDirectories(path.getParent());
                    Files.write(path, (byte[])entry.getValue());
                }
            }
        } catch (IOException ioException) {
            throw new IllegalStateException("не удалось распаковать ассеты скрипта: " + ioException.getMessage(), ioException);
        }

        return this.assetsDir;
    }

    public void cleanup() {
        if (this.assetsDir != null) {
            try (Stream<Path> stream = Files.walk(this.assetsDir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ioException) {
                    }
                });
            } catch (IOException ioException2) {
            }

            this.assetsDir = null;
        }
    }
}
