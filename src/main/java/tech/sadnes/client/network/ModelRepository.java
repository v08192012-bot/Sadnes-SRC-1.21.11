package tech.sadnes.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import local.sadnes.LocalResources;
import local.sadnes.OfflineRouting;
import net.minecraft.util.Identifier;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.render.texture.AvatarIconCache;

public final class ModelRepository {
    private static final String VERSION_FILE = ".sadnes-model";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(Redirect.NORMAL).build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sadnes-models");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile List<ModelRepository.RemoteModel> catalog = List.of();
    private static volatile boolean fetching;
    static final Map<String, Float> DOWNLOAD_PROGRESS = new ConcurrentHashMap<>();
    private static final Map<String, String> DOWNLOAD_ERRORS = new ConcurrentHashMap<>();

    private ModelRepository() {
    }

    public static List<ModelRepository.RemoteModel> getCatalog() {
        return catalog;
    }

    public static boolean isFetching() {
        return fetching;
    }

    public static boolean isDownloading(String string) {
        return DOWNLOAD_PROGRESS.containsKey(string);
    }

    public static float getProgress(String string) {
        Float floatValue = DOWNLOAD_PROGRESS.get(string);
        return floatValue == null ? -1.0F : floatValue;
    }

    public static String getError(String string) {
        String string2 = DOWNLOAD_ERRORS.get(string);
        return string2 == null ? "" : string2;
    }

    public static boolean isInstalled(String string) {
        return FiguraAvatarManager.resolveAvatar(string) != null;
    }

    public static boolean hasUpdate(ModelRepository.RemoteModel remoteModel) {
        if (!isInstalled(remoteModel.name())) {
            return false;
        }

        String string = readInstalledHash(remoteModel.name());
        return !string.isEmpty() && !string.equals(remoteModel.sha256());
    }

    public static CompletableFuture<Void> fetchCatalog() {
        if (fetching) {
            return CompletableFuture.completedFuture(null);
        }

        fetching = true;
        return CompletableFuture.runAsync(
            () -> {
                try {
                    String json;
                    InputStream localStream = LocalResources.open("/models/index.json");
                    if (localStream != null) {
                        try (InputStream inputStream = localStream) {
                            json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } else {
                        HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(getBaseUrl() + "/index.json")))
                            .timeout(Duration.ofSeconds(15L))
                            .GET()
                            .build();
                        HttpResponse httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (httpResponse.statusCode() != 200) {
                            throw new IOException("HTTP " + httpResponse.statusCode());
                        }

                        json = (String)httpResponse.body();
                    }

                    catalog = parseCatalog(json);
                } catch (Exception exception) {
                    Sadnes.getLogger().warn("[models] каталог не получен: {}", exception.toString());
                } finally {
                    fetching = false;
                }
            },
            EXECUTOR
        );
    }

    private static List<ModelRepository.RemoteModel> parseCatalog(String string) {
        JsonObject jsonObject = GSON.fromJson(string, JsonObject.class);
        ArrayList arrayList = new ArrayList();
        if (jsonObject != null && jsonObject.has("models")) {
            for (JsonElement jsonElement : jsonObject.getAsJsonArray("models")) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                arrayList.add(
                    new ModelRepository.RemoteModel(
                        jsonObject2.get("name").getAsString(),
                        jsonObject2.has("size") ? jsonObject2.get("size").getAsLong() : 0L,
                        jsonObject2.has("modifiedMs") ? jsonObject2.get("modifiedMs").getAsLong() : 0L,
                        jsonObject2.has("sha256") ? jsonObject2.get("sha256").getAsString() : "",
                        jsonObject2.has("icon") && jsonObject2.get("icon").getAsBoolean()
                    )
                );
            }

            arrayList.sort(Comparator.comparing(ModelRepository.RemoteModel::name, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(arrayList);
        } else {
            return List.copyOf(arrayList);
        }
    }

    public static Identifier getIcon(ModelRepository.RemoteModel remoteModel) {
        if (!remoteModel.icon()) {
            return null;
        }

        String string = "/models/" + encodeName(remoteModel.name()) + ".png";
        if (LocalResources.exists(string)) {
            return AvatarIconCache.getFromResource("remote:" + remoteModel.name(), "/sadnes_local" + string);
        }

        return AvatarIconCache.getFromUrl("remote:" + remoteModel.name(), getBaseUrl() + "/" + encodeName(remoteModel.name()) + ".png");
    }

    public static CompletableFuture<Void> download(ModelRepository.RemoteModel remoteModel) {
        if (DOWNLOAD_PROGRESS.putIfAbsent(remoteModel.name(), 0.0F) != null) {
            return CompletableFuture.completedFuture(null);
        }

        DOWNLOAD_ERRORS.remove(remoteModel.name());
        return CompletableFuture.runAsync(() -> {
            Path path = null;

            try {
                path = downloadAndExtract(remoteModel);
                install(remoteModel.name(), path);
                writeInstalledHash(remoteModel.name(), remoteModel.sha256());
            } catch (Exception exception) {
                Sadnes.getLogger().warn("[models] {} не скачалась: {}", remoteModel.name(), exception.toString());
                DOWNLOAD_ERRORS.put(remoteModel.name(), exception.getMessage() == null ? exception.toString() : exception.getMessage());
                deleteRecursive(path);
            } finally {
                DOWNLOAD_PROGRESS.remove(remoteModel.name());
            }
        }, EXECUTOR);
    }

    private static Path downloadAndExtract(ModelRepository.RemoteModel remoteModel) throws Exception {
        InputStream source;
        long l;
        InputStream localStream = LocalResources.open("/models/" + encodeName(remoteModel.name()) + ".zip");
        if (localStream != null) {
            source = localStream;
            l = remoteModel.size();
        } else {
            HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(getBaseUrl() + "/" + encodeName(remoteModel.name()) + ".zip")))
                .timeout(Duration.ofMinutes(5L))
                .GET()
                .build();
            HttpResponse httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofInputStream());
            if (httpResponse.statusCode() != 200) {
                throw new IOException("HTTP " + httpResponse.statusCode());
            }

            l = httpResponse.headers().firstValueAsLong("content-length").orElse(remoteModel.size());
            source = (InputStream)httpResponse.body();
        }

        Path path = Files.createTempDirectory("sadnes-model-");
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        try (
            InputStream inputStream = source;
            ModelRepository.ProgressInputStream progressInputStream = new ModelRepository.ProgressInputStream(inputStream, l, remoteModel.name());
            DigestInputStream digestInputStream = new DigestInputStream(progressInputStream, messageDigest);
            ZipInputStream zipInputStream = new ZipInputStream(digestInputStream);
        ) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                Path path2 = path.resolve(zipEntry.getName()).normalize();
                if (!path2.startsWith(path)) {
                    throw new IOException("bad entry " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(path2);
                } else {
                    Files.createDirectories(path2.getParent());
                    Files.copy(zipInputStream, path2, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            zipInputStream.closeEntry();
            digestInputStream.transferTo(OutputStream.nullOutputStream());
        }

        String string = HexFormat.of().formatHex(messageDigest.digest());
        if (!remoteModel.sha256().isEmpty() && !remoteModel.sha256().equals(string)) {
            deleteRecursive(path);
            throw new IOException("checksum mismatch");
        } else {
            return path;
        }
    }

    private static void install(String string, Path path) throws IOException {
        Path path2 = getModelsDirectory().resolve(string);
        Path path3 = findAvatarRoot(path);
        deleteRecursive(path2);
        Files.createDirectories(path2.getParent());

        try {
            Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioException) {
            copyRecursive(path3, path2);
            deleteRecursive(path);
        }
    }

    private static Path findAvatarRoot(Path path) throws IOException {
        if (!Files.exists(path.resolve("avatar.json")) && !Files.exists(path.resolve("avatar.png"))) {
            try (Stream stream = Files.list(path)) {
                List list2 = stream.toList();
                if (list2.size() == 1 && Files.isDirectory((Path)list2.get(0))) {
                    return (Path)list2.get(0);
                }
            }

            return path;
        } else {
            return path;
        }
    }

    private static String readInstalledHash(String string) {
        Path path = getModelsDirectory().resolve(string).resolve(".sadnes-model");

        try {
            return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8).trim() : "";
        } catch (IOException ioException) {
            return "";
        }
    }

    private static void writeInstalledHash(String string, String string2) {
        if (string2 != null && !string2.isEmpty()) {
            try {
                Files.writeString(getModelsDirectory().resolve(string).resolve(".sadnes-model"), string2, StandardCharsets.UTF_8);
            } catch (IOException ioException) {
                Sadnes.getLogger().warn("[models] метка версии не записана для {}: {}", string, ioException.toString());
            }
        }
    }

    private static Path getModelsDirectory() {
        return FiguraAvatarManager.getAvatarsDirectory();
    }

    private static String getBaseUrl() {
        return BackendUrls.url("/models");
    }

    private static String encodeName(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void copyRecursive(Path path, Path path2) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path path3 : stream.toList()) {
                Path path4 = path2.resolve(path.relativize(path3).toString());
                if (Files.isDirectory(path3)) {
                    Files.createDirectories(path4);
                } else {
                    Files.createDirectories(path4.getParent());
                    Files.copy(path3, path4, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursive(Path path) {
        if (path != null && Files.exists(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path2 -> {
                    try {
                        Files.delete(path2);
                    } catch (IOException ioException) {
                    }
                });
            } catch (IOException ioException2) {
            }
        }
    }

    static final class ProgressInputStream extends InputStream {
        private final InputStream delegate;
        private final long totalBytes;
        private final String modelName;
        private long bytesRead;

        ProgressInputStream(InputStream inputStream, long l, String string) {
            this.delegate = inputStream;
            this.totalBytes = l;
            this.modelName = string;
        }

        @Override
        public int read() throws IOException {
            int i = this.delegate.read();
            if (i >= 0) {
                this.addProgress(1);
            }

            return i;
        }

        @Override
        public int read(byte[] bytes, int i, int i2) throws IOException {
            int i3 = this.delegate.read(bytes, i, i2);
            if (i3 > 0) {
                this.addProgress(i3);
            }

            return i3;
        }

        @Override
        public void close() throws IOException {
            this.delegate.close();
        }

        private void addProgress(int i) {
            this.bytesRead += i;
            if (this.totalBytes > 0L) {
                ModelRepository.DOWNLOAD_PROGRESS.put(this.modelName, Math.min(1.0F, (float)this.bytesRead / (float)this.totalBytes));
            }
        }
    }

    public record RemoteModel(String name, long size, long modifiedMs, String sha256, boolean icon) {
    }
}
