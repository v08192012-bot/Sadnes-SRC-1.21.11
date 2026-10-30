package tech.sadnes.client.feature.impl.misc.autobuild;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.IMinecraft;

public final class BuildJobQueue implements IMinecraft {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildJobQueue.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file = mc.runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("autobuild").resolve("queue.json");
    private final List<BuildJob> jobs = new ArrayList<>();
    private final List<BuildJob> jobsView = Collections.unmodifiableList(this.jobs);

    public List<BuildJob> getJobs() {
        return this.jobsView;
    }

    public boolean isEmpty() {
        return this.jobs.isEmpty();
    }

    public void add(BuildJob buildJob) {
        this.jobs.add(buildJob);
        this.save();
    }

    public void remove(BuildJob buildJob) {
        if (this.jobs.remove(buildJob)) {
            this.save();
        }
    }

    public void clear() {
        this.jobs.clear();
        this.save();
    }

    public BuildJob findJob(String string, String string2) {
        for (BuildJob buildJob : this.jobs) {
            if (buildJob.isIn(string, string2)) {
                return buildJob;
            }
        }

        return null;
    }

    public void load() {
        this.jobs.clear();
        if (Files.isRegularFile(this.file)) {
            try {
                label64: {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
                        JsonArray jsonArray = GSON.fromJson(bufferedReader, JsonArray.class);
                        if (jsonArray != null) {
                            for (JsonElement jsonElement : jsonArray) {
                                this.jobs.add(deserialize(jsonElement.getAsJsonObject()));
                            }
                            break label64;
                        }
                    }

                    return;
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to read build queue {}", this.file, exception);
                this.jobs.clear();
            }
        }
    }

    public void save() {
        JsonArray jsonArray = new JsonArray();

        for (BuildJob buildJob : this.jobs) {
            jsonArray.add(serialize(buildJob));
        }

        try {
            Files.createDirectories(this.file.getParent());

            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
                GSON.toJson(jsonArray, bufferedWriter);
            }
        } catch (IOException ioException) {
            LOGGER.warn("Failed to save build queue {}", this.file, ioException);
        }
    }

    private static JsonObject serialize(BuildJob buildJob) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", buildJob.getName());
        jsonObject.addProperty("source", buildJob.getSource().name());
        if (buildJob.getFile() != null) {
            jsonObject.addProperty("file", buildJob.getFile());
        }

        if (buildJob.getPlacement() != null) {
            jsonObject.addProperty("placement", buildJob.getPlacement());
        }

        if (buildJob.getPlacementId() != null) {
            jsonObject.addProperty("placementId", buildJob.getPlacementId());
        }

        jsonObject.addProperty("rotation", buildJob.getRotation().name());
        jsonObject.addProperty("x", buildJob.getOrigin().getX());
        jsonObject.addProperty("y", buildJob.getOrigin().getY());
        jsonObject.addProperty("z", buildJob.getOrigin().getZ());
        Vec3i vec3i = buildJob.getSize();
        if (vec3i != null) {
            jsonObject.addProperty("sizeX", vec3i.getX());
            jsonObject.addProperty("sizeY", vec3i.getY());
            jsonObject.addProperty("sizeZ", vec3i.getZ());
        }

        jsonObject.addProperty("server", buildJob.getServer());
        jsonObject.addProperty("dimension", buildJob.getDimension());
        return jsonObject;
    }

    private static BuildJob deserialize(JsonObject jsonObject) {
        boolean flag;
        if (jsonObject.has("sizeX") && jsonObject.has("sizeY")) {
            flag = jsonObject.has("sizeZ");
        } else {
            flag = false;
        }

        return BuildJob.builder()
            .name(jsonObject.get("name").getAsString())
            .source(BuildJob.Source.valueOf(jsonObject.get("source").getAsString()))
            .file(jsonObject.has("file") ? jsonObject.get("file").getAsString() : null)
            .placement(jsonObject.has("placement") ? jsonObject.get("placement").getAsString() : null)
            .placementId(jsonObject.has("placementId") ? jsonObject.get("placementId").getAsString() : null)
            .rotation(BlockRotation.valueOf(jsonObject.get("rotation").getAsString()))
            .origin(new BlockPos(jsonObject.get("x").getAsInt(), jsonObject.get("y").getAsInt(), jsonObject.get("z").getAsInt()))
            .size(flag ? new Vec3i(jsonObject.get("sizeX").getAsInt(), jsonObject.get("sizeY").getAsInt(), jsonObject.get("sizeZ").getAsInt()) : null)
            .server(jsonObject.get("server").getAsString())
            .dimension(jsonObject.get("dimension").getAsString())
            .build();
    }
}
