package tech.sadnes.client.feature.impl.misc.autobuild;

import baritone.api.BaritoneAPI;
import baritone.api.schematic.IStaticSchematic;
import baritone.api.schematic.format.ISchematicFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.IMinecraft;

public final class SchematicFileManager implements IMinecraft {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchematicFileManager.class);
    private final Path schematicsDir = mc.runDirectory.toPath().resolve("schematics");
    private Path cachedPath;
    private long cachedModified;
    private IStaticSchematic cachedSchematic;

    public Path getSchematicsDir() {
        try {
            Files.createDirectories(this.schematicsDir);
        } catch (IOException ioException) {
            LOGGER.warn("Failed to create schematics folder {}", this.schematicsDir, ioException);
        }

        return this.schematicsDir;
    }

    public List<String> listSchematics() {
        List<String> strings = new ArrayList<>();

        try (Stream<Path> stream = Files.list(this.getSchematicsDir())) {
            stream.filter(path -> Files.isRegularFile(path) && isSupportedFile(path)).forEach(path2 -> strings.add(path2.getFileName().toString()));
        } catch (IOException ioException) {
            LOGGER.warn("Failed to list schematics in {}", this.schematicsDir, ioException);
        }

        strings.sort(String.CASE_INSENSITIVE_ORDER);
        return strings;
    }

    public CompletableFuture<IStaticSchematic> loadSchematic(Path path) {
        CompletableFuture completableFuture = new CompletableFuture();
        long l = path.toFile().lastModified();
        boolean flag = path.equals(this.cachedPath) && l == this.cachedModified;
        if (flag) {
            completableFuture.complete(this.cachedSchematic);
            return completableFuture;
        }

        Optional optional = BaritoneAPI.getProvider().getSchematicSystem().getByFile(path.toFile());
        boolean flag2;
        if (!Files.isRegularFile(path)) {
            flag2 = true;
        } else {
            flag2 = false;
            if (!optional.isEmpty()) {
                CompletableFuture.<IStaticSchematic>supplyAsync(() -> parseSchematic((ISchematicFormat)optional.get(), path))
                    .whenComplete((iStaticSchematic, throwable) -> mc.execute(() -> {
                        if (throwable != null) {
                            LOGGER.warn("Failed to parse schematic {}", path, throwable);
                            completableFuture.completeExceptionally(new IOException(String.format(Language.translate("autobuild.error.parse"), path.getFileName())));
                        } else {
                            this.cachedPath = path;
                            this.cachedModified = l;
                            this.cachedSchematic = iStaticSchematic;
                            completableFuture.complete(iStaticSchematic);
                        }
                    }));
                return completableFuture;
            }
        }

        String string = Language.translate(flag2 ? "autobuild.error.notFound" : "autobuild.error.format");
        completableFuture.completeExceptionally(new IOException(String.format(string, path.getFileName())));
        return completableFuture;
    }

    public List<String> importSchematics(List<Path> paths) {
        ArrayList arrayList = new ArrayList();
        Path path = this.getSchematicsDir().toAbsolutePath().normalize();

        for (Path path2 : paths) {
            if (Files.isRegularFile(path2) && isSupportedFile(path2)) {
                Path path3 = path2.toAbsolutePath().normalize();
                Path path4 = path.resolve(path3.getFileName().toString());

                try {
                    if (!path3.equals(path4)) {
                        Files.copy(path3, path4, StandardCopyOption.REPLACE_EXISTING);
                    }

                    arrayList.add(path4.getFileName().toString());
                } catch (IOException ioException) {
                    LOGGER.warn("Failed to import schematic {}", path3, ioException);
                }
            }
        }

        return arrayList;
    }

    private static IStaticSchematic parseSchematic(ISchematicFormat iSchematicFormat, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return iSchematicFormat.parse(inputStream);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    private static boolean isSupportedFile(Path path) {
        String string = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int i = string.lastIndexOf(46);
        return i >= 0 && BaritoneAPI.getProvider().getSchematicSystem().getFileExtensions().contains(string.substring(i + 1));
    }
}
