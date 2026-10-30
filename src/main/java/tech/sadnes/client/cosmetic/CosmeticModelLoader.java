package tech.sadnes.client.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tech.sadnes.client.ClientInfo;

final class CosmeticModelLoader {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private CosmeticModelLoader() {
    }

    static Cosmetic parse(CosmeticType cosmeticType, String string, byte[] bytes) {
        JsonObject jsonObject = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject jsonObject2 = jsonObject.getAsJsonObject("model").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject jsonObject3 = jsonObject2.getAsJsonObject("description");
        float f = jsonObject3.get("texture_width").getAsFloat();
        float f2 = jsonObject3.get("texture_height").getAsFloat();
        List list = sortBones(jsonObject2.getAsJsonArray("bones"));
        HashMap<String, Integer> hashMap = new HashMap<>();
        CosmeticModel.Bone[] bones = new CosmeticModel.Bone[list.size()];
        int i = bones.length;

        for (int i2 = 0; i2 < i; i2++) {
            JsonObject jsonObject4 = (JsonObject)list.get(i2);
            int i3 = jsonObject4.has("parent") ? hashMap.getOrDefault(jsonObject4.get("parent").getAsString(), -1) : -1;
            float[] floats = readVec3(jsonObject4.get("pivot"), 0.0F);
            float[] floats2 = readVec3(jsonObject4.get("rotation"), 0.0F);
            bones[i2] = new CosmeticModel.Bone(
                i3,
                floats[0] / 16.0F,
                -floats[1] / 16.0F,
                floats[2] / 16.0F,
                floats2[0] * (float) (Math.PI / 180.0),
                floats2[1] * (float) (Math.PI / 180.0),
                floats2[2] * (float) (Math.PI / 180.0),
                concat(buildCubes(jsonObject4.getAsJsonArray("cubes"), f, f2), buildPolyMesh(jsonObject4.getAsJsonObject("poly_mesh")))
            );
            hashMap.put(jsonObject4.get("name").getAsString(), i2);
        }

        JsonObject jsonObject5 = jsonObject.getAsJsonObject("textureAnimation");
        CosmeticModel cosmeticModel = new CosmeticModel(
            bones,
            parseAnimations(jsonObject, hashMap, i),
            jsonObject5 == null ? 1 : jsonObject5.get("frameCount").getAsInt(),
            jsonObject5 == null ? 1.0F : jsonObject5.get("frameTime").getAsFloat(),
            jsonObject5 == null ? 0.0F : jsonObject5.get("frameHeight").getAsFloat() / f2
        );
        int i4 = jsonObject.get("category").getAsInt();
        boolean flag = jsonObject.get("pos").getAsInt() == 2;
        Cosmetic.Anchor anchor = cosmeticType == CosmeticType.SWORD
            ? Cosmetic.Anchor.HAND
            : (i4 == 2 ? Cosmetic.Anchor.ROOT : (i4 == 3 ? Cosmetic.Anchor.HEAD_TOP : (flag ? Cosmetic.Anchor.HEAD : Cosmetic.Anchor.BODY)));
        String string2 = "dynamic/cosmetic_" + cosmeticType.getId() + "_" + string;
        return new Cosmetic(
            string,
            cosmeticType,
            stripNamePrefix(jsonObject.get("name").getAsString()),
            anchor,
            jsonObject.get("x").getAsFloat(),
            jsonObject.get("y").getAsFloat(),
            jsonObject.get("z").getAsFloat(),
            jsonObject.get("scale").getAsFloat(),
            cosmeticModel,
            Identifier.of(ClientInfo.MOD_ID, string2),
            Identifier.of(ClientInfo.MOD_ID, string2 + "_preview"),
            jsonObject.has("smooth") && jsonObject.get("smooth").getAsBoolean()
        );
    }

    private static String stripNamePrefix(String string) {
        int i = string.indexOf(95);
        boolean flag = i > 0 && string.lastIndexOf(32, i) < 0;
        return flag ? string.substring(i + 1) : string;
    }

    private static List<JsonObject> sortBones(JsonArray jsonArray) {
        ArrayList arrayList = new ArrayList();

        for (JsonElement jsonElement : jsonArray) {
            arrayList.add(jsonElement.getAsJsonObject());
        }

        ArrayList arrayList2 = new ArrayList();
        HashSet hashSet = new HashSet();

        while (!arrayList.isEmpty()) {
            int i = arrayList.size();
            Iterator iterator2 = arrayList.iterator();

            while (iterator2.hasNext()) {
                JsonObject jsonObject = (JsonObject)iterator2.next();
                if (!jsonObject.has("parent") || hashSet.contains(jsonObject.get("parent").getAsString())) {
                    arrayList2.add(jsonObject);
                    hashSet.add(jsonObject.get("name").getAsString());
                    iterator2.remove();
                }
            }

            if (arrayList.size() == i) {
                arrayList2.addAll(arrayList);
                break;
            }
        }

        return arrayList2;
    }

    private static float[] buildCubes(JsonArray jsonArray, float f, float f2) {
        if (jsonArray == null) {
            return new float[0];
        }

        float[] floats = new float[jsonArray.size() * 6 * 23];
        int i = 0;
        Vector3f vector3f = new Vector3f();
        float[][] floatses = new float[4][3];

        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            float[] floats2 = readVec3(jsonObject.get("origin"), 0.0F);
            float[] floats3 = readVec3(jsonObject.get("size"), 0.0F);
            float f3 = jsonObject.has("inflate") ? jsonObject.get("inflate").getAsFloat() : 0.0F;
            float[] floats4 = new float[]{floats2[0] - f3, floats2[1] - f3, floats2[2] - f3};
            float[] floats5 = new float[]{floats2[0] + floats3[0] + f3, floats2[1] + floats3[1] + f3, floats2[2] + floats3[2] + f3};
            Matrix4f matrix4f = new Matrix4f();
            if (jsonObject.has("rotation")) {
                float[] floats6 = readVec3(jsonObject.get("pivot"), 0.0F);
                float[] floats7 = readVec3(jsonObject.get("rotation"), 0.0F);
                matrix4f.translate(floats6[0] / 16.0F, -floats6[1] / 16.0F, floats6[2] / 16.0F)
                    .rotateZYX(floats7[2] * (float) (Math.PI / 180.0), floats7[1] * (float) (Math.PI / 180.0), floats7[0] * (float) (Math.PI / 180.0))
                    .translate(-floats6[0] / 16.0F, floats6[1] / 16.0F, -floats6[2] / 16.0F);
            }

            for (CosmeticModelLoader.Face face : CosmeticModelLoader.Face.values()) {
                float[] floats8 = resolveFaceUv(jsonObject.get("uv"), face, floats3);
                if (floats8 != null) {
                    for (int i2 = 0; i2 < 4; i2++) {
                        int i3 = face.corners[i2];
                        floatses[i2][0] = (i3 & 4) != 0 ? floats5[0] : floats4[0];
                        floatses[i2][1] = (i3 & 2) != 0 ? floats5[1] : floats4[1];
                        floatses[i2][2] = (i3 & 1) != 0 ? floats5[2] : floats4[2];
                    }

                    if (!Arrays.equals(floatses[0], floatses[1]) && !Arrays.equals(floatses[0], floatses[3])) {
                        matrix4f.transformDirection(face.normalX, face.normalY, face.normalZ, vector3f);
                        floats[i++] = vector3f.x;
                        floats[i++] = vector3f.y;
                        floats[i++] = vector3f.z;
                        int i4 = (int)floats8[4];

                        for (int i5 = 0; i5 < 4; i5++) {
                            matrix4f.transformPosition(floatses[i5][0] / 16.0F, -floatses[i5][1] / 16.0F, floatses[i5][2] / 16.0F, vector3f);
                            int i6 = (i5 + 4 - i4) % 4;
                            boolean flag = i6 == 1 || i6 == 2;
                            boolean flag2 = i6 >= 2;
                            floats[i++] = vector3f.x;
                            floats[i++] = vector3f.y;
                            floats[i++] = vector3f.z;
                            floats[i++] = (floats8[0] + (flag ? floats8[2] : 0.0F)) / f;
                            floats[i++] = (floats8[1] + (flag2 ? floats8[3] : 0.0F)) / f2;
                        }
                    }
                }
            }
        }

        return Arrays.copyOf(floats, i);
    }

    private static float[] buildPolyMesh(JsonObject jsonObject) {
        if (jsonObject == null) {
            return new float[0];
        }

        JsonArray jsonArray = jsonObject.getAsJsonArray("positions");
        JsonArray jsonArray2 = jsonObject.getAsJsonArray("normals");
        JsonArray jsonArray3 = jsonObject.getAsJsonArray("uvs");
        JsonArray jsonArray4 = jsonObject.getAsJsonArray("polys");
        float[] floats = new float[jsonArray4.size() * 23];
        int i = 0;

        for (JsonElement jsonElement : jsonArray4) {
            JsonArray jsonArray5 = jsonElement.getAsJsonArray();
            float[] floats2 = readVec3(jsonArray2.get(jsonArray5.get(0).getAsJsonArray().get(1).getAsInt()), 0.0F);
            floats[i++] = floats2[0];
            floats[i++] = -floats2[1];
            floats[i++] = floats2[2];

            for (int i2 = 0; i2 < 4; i2++) {
                JsonArray jsonArray6 = jsonArray5.get(Math.min(i2, jsonArray5.size() - 1)).getAsJsonArray();
                float[] floats3 = readVec3(jsonArray.get(jsonArray6.get(0).getAsInt()), 0.0F);
                JsonArray jsonArray7 = jsonArray3.get(jsonArray6.get(2).getAsInt()).getAsJsonArray();
                floats[i++] = floats3[0] / 16.0F;
                floats[i++] = -floats3[1] / 16.0F;
                floats[i++] = floats3[2] / 16.0F;
                floats[i++] = jsonArray7.get(0).getAsFloat();
                floats[i++] = 1.0F - jsonArray7.get(1).getAsFloat();
            }
        }

        return floats;
    }

    private static float[] concat(float[] floats, float[] floats2) {
        float[] floats3 = Arrays.copyOf(floats, floats.length + floats2.length);
        System.arraycopy(floats2, 0, floats3, floats.length, floats2.length);
        return floats3;
    }

    private static float[] resolveFaceUv(JsonElement jsonElement, CosmeticModelLoader.Face face, float[] floats) {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject().getAsJsonObject(face.key);
            if (jsonObject == null) {
                return null;
            }

            JsonArray jsonArray = jsonObject.getAsJsonArray("uv");
            JsonArray jsonArray2 = jsonObject.getAsJsonArray("uv_size");
            float f = jsonObject.has("uv_rotation") ? jsonObject.get("uv_rotation").getAsInt() / 90 : 0.0F;
            return new float[]{jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat(), jsonArray2.get(0).getAsFloat(), jsonArray2.get(1).getAsFloat(), f};
        } else {
            float f2 = jsonElement.getAsJsonArray().get(0).getAsFloat();
            float f3 = jsonElement.getAsJsonArray().get(1).getAsFloat();
            float f4 = (float)Math.floor(floats[0]);
            float f5 = (float)Math.floor(floats[1]);
            float f6 = (float)Math.floor(floats[2]);
            switch (face) {
                case NORTH:
                    return new float[]{f2 + f6, f3 + f6, f4, f5, 0.0F};
                case SOUTH:
                    return new float[]{f2 + f6 * 2.0F + f4, f3 + f6, f4, f5, 0.0F};
                case EAST:
                    return new float[]{f2, f3 + f6, f6, f5, 0.0F};
                case WEST:
                    return new float[]{f2 + f6 + f4, f3 + f6, f6, f5, 0.0F};
                case UP:
                    return new float[]{f2 + f6, f3, f4, f6, 0.0F};
                case DOWN:
                    return new float[]{f2 + f6 + f4, f3 + f6, f4, -f6, 0.0F};
                default:
                    throw new MatchException(null, null);
            }
        }
    }

    private static Map<CosmeticModel.Pose, CosmeticAnimation> parseAnimations(JsonObject jsonObject, Map<String, Integer> map, int i) {
        EnumMap enumMap = new EnumMap<>(CosmeticModel.Pose.class);
        JsonElement jsonElement = jsonObject.get("animation");
        if (jsonElement != null && jsonElement.isJsonObject()) {
            JsonObject jsonObject2 = jsonElement.getAsJsonObject().getAsJsonObject("animations");
            if (jsonObject2 == null) {
                return enumMap;
            }

            CosmeticAnimation cosmeticAnimation = null;

            for (Entry entry : jsonObject2.entrySet()) {
                String string = ((String)entry.getKey()).toLowerCase(Locale.ROOT);
                int i2 = string.lastIndexOf(46);
                String string2 = string.substring(i2 + 1);
                if ((i2 <= 0 || !string2.equals("water") && !string2.equals("sneak") || !jsonObject2.has(((String)entry.getKey()).substring(0, i2)))
                    && !string2.equals("gui")
                    && !string2.equals("afk")
                    && !string2.contains("water")) {
                    CosmeticAnimation cosmeticAnimation2 = parseAnimation(((JsonElement)entry.getValue()).getAsJsonObject(), map, i);
                    if (string2.contains("elytra")) {
                        enumMap.put(CosmeticModel.Pose.GLIDE, cosmeticAnimation2);
                    } else if (string2.contains("flying")) {
                        enumMap.putIfAbsent(CosmeticModel.Pose.GLIDE, cosmeticAnimation2);
                    } else if (string2.contains("swim")) {
                        enumMap.putIfAbsent(CosmeticModel.Pose.SWIM, cosmeticAnimation2);
                    } else if (string2.equals("sneak")) {
                        enumMap.putIfAbsent(CosmeticModel.Pose.SNEAK, cosmeticAnimation2);
                    } else if (string2.contains("mov") || string2.contains("walk") || string2.contains("run")) {
                        enumMap.putIfAbsent(CosmeticModel.Pose.MOVE, cosmeticAnimation2);
                    } else if (string2.contains("idle") || string2.contains("main")) {
                        enumMap.putIfAbsent(CosmeticModel.Pose.IDLE, cosmeticAnimation2);
                    } else if (cosmeticAnimation == null) {
                        cosmeticAnimation = cosmeticAnimation2;
                    }
                }
            }

            if (cosmeticAnimation != null) {
                enumMap.putIfAbsent(CosmeticModel.Pose.IDLE, cosmeticAnimation);
                return enumMap;
            }
        }

        return enumMap;
    }

    private static CosmeticAnimation parseAnimation(JsonObject jsonObject, Map<String, Integer> map, int i) {
        CosmeticAnimation.Track[] tracks = new CosmeticAnimation.Track[i];
        CosmeticAnimation.Track[] tracks2 = new CosmeticAnimation.Track[i];
        CosmeticAnimation.Track[] tracks3 = new CosmeticAnimation.Track[i];
        JsonObject jsonObject2 = jsonObject.getAsJsonObject("bones");
        if (jsonObject2 != null) {
            for (Entry entry : jsonObject2.entrySet()) {
                Integer integer = (Integer)map.get(entry.getKey());
                if (integer != null) {
                    JsonObject jsonObject3 = ((JsonElement)entry.getValue()).getAsJsonObject();
                    tracks[integer] = parseTrack(jsonObject3.get("rotation"), 0.0F);
                    tracks2[integer] = parseTrack(jsonObject3.get("position"), 0.0F);
                    tracks3[integer] = parseTrack(jsonObject3.get("scale"), 1.0F);
                }
            }
        }

        JsonElement jsonElement = jsonObject.get("animation_length");
        return new CosmeticAnimation(jsonElement != null && !jsonElement.isJsonNull() ? jsonElement.getAsFloat() : 0.0F, tracks, tracks2, tracks3);
    }

    private static CosmeticAnimation.Track parseTrack(JsonElement jsonElement, float f) {
        if (jsonElement == null) {
            return null;
        }

        ArrayList arrayList = new ArrayList();
        if (jsonElement.isJsonObject() && !jsonElement.getAsJsonObject().has("post") && !jsonElement.getAsJsonObject().has("vector")) {
            for (Entry entry : jsonElement.getAsJsonObject().entrySet()) {
                arrayList.add(parseKeyframe(Float.parseFloat((String)entry.getKey()), (JsonElement)entry.getValue(), f));
            }

            arrayList.sort(Comparator.comparingDouble(CosmeticModelLoader.Keyframe::time));
        } else {
            arrayList.add(parseKeyframe(0.0F, jsonElement, f));
        }

        if (arrayList.isEmpty()) {
            return null;
        }

        int i = arrayList.size();
        float[] floats = new float[i];
        float[] floats2 = new float[i * 3];
        float[] floats3 = new float[i * 3];
        boolean[] booleans = new boolean[i];

        for (int i2 = 0; i2 < i; i2++) {
            CosmeticModelLoader.Keyframe keyframe = (CosmeticModelLoader.Keyframe)arrayList.get(i2);
            floats[i2] = keyframe.time();
            booleans[i2] = keyframe.smooth();
            System.arraycopy(keyframe.pre(), 0, floats2, i2 * 3, 3);
            System.arraycopy(keyframe.post(), 0, floats3, i2 * 3, 3);
        }

        return new CosmeticAnimation.Track(floats, floats2, floats3, booleans);
    }

    private static CosmeticModelLoader.Keyframe parseKeyframe(float f, JsonElement jsonElement, float f2) {
        if (!jsonElement.isJsonObject()) {
            float[] floats = readVec3(jsonElement, f2);
            return new CosmeticModelLoader.Keyframe(f, floats, floats, false);
        }

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement jsonElement2 = jsonObject.has("post") ? jsonObject.get("post") : (jsonObject.has("vector") ? jsonObject.get("vector") : jsonObject.get("pre"));
        JsonElement jsonElement3 = jsonObject.has("pre") ? jsonObject.get("pre") : jsonElement2;
        boolean flag;
        if (jsonObject.has("lerp_mode")) {
            flag = jsonObject.get("lerp_mode").getAsString().equals("catmullrom");
        } else {
            flag = false;
        }

        return new CosmeticModelLoader.Keyframe(f, readVec3(jsonElement3, f2), readVec3(jsonElement2, f2), flag);
    }

    private static float[] readVec3(JsonElement jsonElement, float f) {
        if (jsonElement == null) {
            return new float[]{f, f, f};
        } else if (!jsonElement.isJsonArray()) {
            float f2 = jsonElement.getAsFloat();
            return new float[]{f2, f2, f2};
        } else {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            return new float[]{jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat(), jsonArray.get(2).getAsFloat()};
        }
    }

    enum Face {
        NORTH("north", 0.0F, 0.0F, -1.0F, 2, 6, 4, 0),
        SOUTH("south", 0.0F, 0.0F, 1.0F, 7, 3, 1, 5),
        EAST("east", -1.0F, 0.0F, 0.0F, 3, 2, 0, 1),
        WEST("west", 1.0F, 0.0F, 0.0F, 6, 7, 5, 4),
        UP("up", 0.0F, -1.0F, 0.0F, 3, 7, 6, 2),
        DOWN("down", 0.0F, 1.0F, 0.0F, 0, 4, 5, 1);

        final String key;
        final float normalX;
        final float normalY;
        final float normalZ;
        final int[] corners;

        Face(String string, float f, float f2, float f3, int... ints) {
            this.key = string;
            this.normalX = f;
            this.normalY = f2;
            this.normalZ = f3;
            this.corners = ints;
        }
    }

    record Keyframe(float time, float[] pre, float[] post, boolean smooth) {
    }
}
