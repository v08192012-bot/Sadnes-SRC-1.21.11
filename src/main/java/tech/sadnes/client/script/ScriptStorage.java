package tech.sadnes.client.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeArray;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private JsonObject data;
    private boolean dirty;

    ScriptStorage(Path path) {
        this.file = path;
    }

    private JsonObject getData() {
        if (this.data != null) {
            return this.data;
        }

        this.data = new JsonObject();

        try {
            if (Files.isRegularFile(this.file)) {
                JsonElement jsonElement = JsonParser.parseString(Files.readString(this.file));
                if (jsonElement.isJsonObject()) {
                    this.data = jsonElement.getAsJsonObject();
                }
            }
        } catch (Exception exception) {
        }

        return this.data;
    }

    public Object get(String string, Object object) {
        JsonElement jsonElement = this.getData().get(string);
        return jsonElement != null && !jsonElement.isJsonNull() ? convertFromJson(jsonElement) : object;
    }

    public void set(String string, Object object) {
        JsonElement jsonElement = convertToJson(object);
        if (jsonElement != null && !jsonElement.isJsonNull()) {
            this.getData().add(string, jsonElement);
            this.dirty = true;
            this.save();
        } else {
            this.getData().remove(string);
            this.dirty = true;
            this.save();
        }
    }

    public void remove(String string) {
        this.getData().remove(string);
        this.dirty = true;
        this.save();
    }

    public List<String> keys() {
        return new ArrayList<>(this.getData().keySet());
    }

    public void clear() {
        this.data = null;
        this.dirty = false;

        try {
            Files.deleteIfExists(this.file);
        } catch (Exception exception) {
        }
    }

    public void save() {
        if (this.dirty && this.data != null) {
            try {
                Files.createDirectories(this.file.getParent());
                Files.writeString(this.file, GSON.toJson(this.data));
                this.dirty = false;
            } catch (Exception exception) {
            }
        }
    }

    public static JsonElement toJson(Object object) {
        return convertToJson(object);
    }

    public static Object fromJson(JsonElement jsonElement) {
        return convertFromJson(jsonElement);
    }

    private static JsonElement convertToJson(Object object) {
        if (object == null || Undefined.isUndefined(object)) {
            return JsonNull.INSTANCE;
        } else if (object instanceof Boolean flag) {
            return new JsonPrimitive(flag);
        } else if (object instanceof Number number) {
            return new JsonPrimitive(number.doubleValue());
        } else if (object instanceof CharSequence charSequence) {
            return new JsonPrimitive(charSequence.toString());
        } else if (!(object instanceof NativeArray nativeArray)) {
            if (object instanceof Scriptable scriptable) {
                JsonObject jsonObject = new JsonObject();

                for (Object object2 : scriptable.getIds()) {
                    String string = Context.toString(object2);
                    Object object3 = scriptable.get(string, scriptable);
                    if (object3 != Scriptable.NOT_FOUND) {
                        jsonObject.add(string, convertToJson(object3));
                    }
                }

                return jsonObject;
            } else {
                return new JsonPrimitive(String.valueOf(object));
            }
        } else {
            JsonArray jsonArray = new JsonArray();
            long l = nativeArray.getLength();

            for (int i = 0; i < l; i++) {
                jsonArray.add(convertToJson(nativeArray.get(i, nativeArray)));
            }

            return jsonArray;
        }
    }

    private static Object convertFromJson(JsonElement jsonElement) {
        if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (jsonPrimitive.isBoolean()) {
                return jsonPrimitive.getAsBoolean();
            } else {
                return jsonPrimitive.isNumber() ? jsonPrimitive.getAsDouble() : jsonPrimitive.getAsString();
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            Object[] objects = new Object[jsonArray.size()];

            for (int i = 0; i < objects.length; i++) {
                objects[i] = convertFromJson(jsonArray.get(i));
            }

            return ScriptValues.toArray(objects);
        } else {
            if (!jsonElement.isJsonObject()) {
                return null;
            }

            NativeObject nativeObject = ScriptValues.newObject();

            for (String string : jsonElement.getAsJsonObject().keySet()) {
                nativeObject.put(string, nativeObject, convertFromJson(jsonElement.getAsJsonObject().get(string)));
            }

            return nativeObject;
        }
    }
}
