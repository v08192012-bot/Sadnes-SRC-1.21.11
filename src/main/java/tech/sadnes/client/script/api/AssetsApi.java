package tech.sadnes.client.script.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptPathValidator;
import tech.sadnes.client.script.ScriptStorage;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class AssetsApi {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AssetsApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "path", objects -> resolve(script, ScriptValues.argString(objects, 0)).toString());
        ScriptValues.putFunction(nativeObject, "dir", objects2 -> script.getAssetsDir().toString());
        ScriptValues.putFunction(nativeObject, "exists", objects3 -> Files.exists(resolve(script, ScriptValues.argString(objects3, 0))));
        ScriptValues.putFunction(
            nativeObject,
            "list",
            objects4 -> {
                Path path = resolve(script, ScriptValues.argString(objects4, 0));
                ArrayList arrayList = new ArrayList();
                if (Files.isDirectory(path)) {
                    try (Stream<Path> stream = Files.list(path)) {
                        stream.sorted(Comparator.comparing(Path::toString))
                            .forEach(path2 -> arrayList.add(script.getAssetsDir().toAbsolutePath().normalize().relativize(path2).toString().replace('\\', '/')));
                    } catch (IOException ioException) {
                    }
                }

                return ScriptValues.toArray(arrayList);
            }
        );
        ScriptValues.putFunction(nativeObject, "read", objects5 -> {
            Path path3 = resolve(script, ScriptValues.argString(objects5, 0));

            try {
                return Files.isRegularFile(path3) ? Files.readString(path3) : null;
            } catch (IOException ioException2) {
                throw Context.reportRuntimeError("assets.read: " + ioException2.getMessage());
            }
        });
        ScriptValues.putFunction(nativeObject, "write", objects6 -> {
            Path path4 = resolve(script, ScriptValues.argString(objects6, 0));

            try {
                Files.createDirectories(path4.getParent());
                Files.writeString(path4, ScriptValues.argString(objects6, 1));
                return true;
            } catch (IOException ioException3) {
                throw Context.reportRuntimeError("assets.write: " + ioException3.getMessage());
            }
        });
        ScriptValues.putFunction(nativeObject, "delete", objects7 -> {
            try {
                return Files.deleteIfExists(resolve(script, ScriptValues.argString(objects7, 0)));
            } catch (IOException ioException4) {
                return false;
            }
        });
        ScriptValues.putFunction(nativeObject, "json", objects8 -> {
            Path path5 = resolve(script, ScriptValues.argString(objects8, 0));
            if (!Files.isRegularFile(path5)) {
                return ScriptValues.hasArg(objects8, 1) ? objects8[1] : null;
            }

            try {
                return ScriptStorage.fromJson(JsonParser.parseString(Files.readString(path5)));
            } catch (Exception exception) {
                throw Context.reportRuntimeError("assets.json: " + exception.getMessage());
            }
        });
        ScriptValues.putFunction(nativeObject, "writeJson", objects9 -> {
            Path path6 = resolve(script, ScriptValues.argString(objects9, 0));

            try {
                Files.createDirectories(path6.getParent());
                Files.writeString(path6, GSON.toJson(ScriptStorage.toJson(ScriptValues.hasArg(objects9, 1) ? objects9[1] : null)));
                return true;
            } catch (IOException ioException5) {
                throw Context.reportRuntimeError("assets.writeJson: " + ioException5.getMessage());
            }
        });
        scriptableObject.put("assets", scriptableObject, nativeObject);
    }

    private static Path resolve(Script script, String string) {
        return ScriptPathValidator.resolveSafe(script.getAssetsDir(), string);
    }

    public static String getOsName() {
        String string = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (string.contains("win")) {
            return "windows";
        } else {
            return string.contains("mac") ? "macos" : "linux";
        }
    }
}
