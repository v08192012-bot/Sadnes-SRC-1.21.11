package tech.sadnes.client.feature.impl.misc.autobuild;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;

public final class BuildJob {
    private final String name;
    private final BuildJob.Source source;
    private final String file;
    private final String placement;
    private final String placementId;
    private final BlockRotation rotation;
    private final String server;
    private final String dimension;
    private BlockPos origin;
    private Vec3i size;
    private Box bounds;
    private BuildJob.State state = BuildJob.State.NEW;
    private FilteredSchematic schematic;
    private String error;
    private LitematicaIntegration.LoadedPlacementSchematic placementHandle;
    private BuildProgress progress;
    private boolean started;
    private boolean skipClearing;
    private boolean orientationProblem;

    BuildJob(String string, BuildJob.Source source2, String string2, String string3, String string4, BlockRotation blockRotation, String string5, String string6, BlockPos blockPos, Vec3i vec3i) {
        this.name = string;
        this.source = source2;
        this.file = string2;
        this.placement = string3;
        this.placementId = string4;
        this.rotation = blockRotation == null ? BlockRotation.NONE : blockRotation;
        this.server = string5;
        this.dimension = string6;
        this.setBounds(blockPos == null ? BlockPos.ORIGIN : blockPos, vec3i);
    }

    public boolean isIn(String string, String string2) {
        return this.server.equals(string) && this.dimension.equals(string2);
    }

    public void markLoading() {
        this.state = BuildJob.State.LOADING;
    }

    public void markReady(FilteredSchematic filteredSchematic, Vec3i vec3i) {
        this.schematic = filteredSchematic;
        this.setBounds(new BlockPos(vec3i), new Vec3i(filteredSchematic.widthX(), filteredSchematic.heightY(), filteredSchematic.lengthZ()));
        this.state = BuildJob.State.READY;
    }

    public void markFailed(String string) {
        this.error = string;
        this.state = BuildJob.State.FAILED;
    }

    public void reset() {
        this.state = BuildJob.State.NEW;
        this.schematic = null;
        this.placementHandle = null;
        this.progress = null;
        this.started = false;
        this.skipClearing = false;
    }

    public void resetFlags() {
        this.started = false;
        this.skipClearing = false;
    }

    private void setBounds(BlockPos blockPos, Vec3i vec3i) {
        this.origin = blockPos;
        this.size = vec3i;
        if (vec3i == null) {
            this.bounds = null;
        } else {
            this.bounds = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + vec3i.getX(), blockPos.getY() + vec3i.getY(), blockPos.getZ() + vec3i.getZ());
        }
    }

    public static BuildJob.BuildJobBuilder builder() {
        return new BuildJob.BuildJobBuilder();
    }

    public String getName() {
        return this.name;
    }

    public BuildJob.Source getSource() {
        return this.source;
    }

    public String getFile() {
        return this.file;
    }

    public String getPlacement() {
        return this.placement;
    }

    public String getPlacementId() {
        return this.placementId;
    }

    public BlockRotation getRotation() {
        return this.rotation;
    }

    public String getServer() {
        return this.server;
    }

    public String getDimension() {
        return this.dimension;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public Vec3i getSize() {
        return this.size;
    }

    public Box getBounds() {
        return this.bounds;
    }

    public boolean isStarted() {
        return this.started;
    }

    public BuildJob.State getState() {
        return this.state;
    }

    public FilteredSchematic getSchematic() {
        return this.schematic;
    }

    public String getError() {
        return this.error;
    }

    public LitematicaIntegration.LoadedPlacementSchematic getPlacementHandle() {
        return this.placementHandle;
    }

    public BuildJob setPlacementHandle(LitematicaIntegration.LoadedPlacementSchematic loadedPlacementSchematic) {
        this.placementHandle = loadedPlacementSchematic;
        return this;
    }

    public BuildProgress getProgress() {
        return this.progress;
    }

    public BuildJob setProgress(BuildProgress buildProgress) {
        this.progress = buildProgress;
        return this;
    }

    public BuildJob setStarted(boolean flag) {
        this.started = flag;
        return this;
    }

    public boolean isSkipClearing() {
        return this.skipClearing;
    }

    public BuildJob setSkipClearing(boolean flag) {
        this.skipClearing = flag;
        return this;
    }

    public boolean isOrientationProblem() {
        return this.orientationProblem;
    }

    public BuildJob setOrientationProblem(boolean flag) {
        this.orientationProblem = flag;
        return this;
    }

    public static class BuildJobBuilder {
        private String name;
        private BuildJob.Source source;
        private String file;
        private String placement;
        private String placementId;
        private BlockRotation rotation;
        private String server;
        private String dimension;
        private BlockPos origin;
        private Vec3i size;

        BuildJobBuilder() {
        }

        public BuildJob.BuildJobBuilder name(String string) {
            this.name = string;
            return this;
        }

        public BuildJob.BuildJobBuilder source(BuildJob.Source source2) {
            this.source = source2;
            return this;
        }

        public BuildJob.BuildJobBuilder file(String string) {
            this.file = string;
            return this;
        }

        public BuildJob.BuildJobBuilder placement(String string) {
            this.placement = string;
            return this;
        }

        public BuildJob.BuildJobBuilder placementId(String string) {
            this.placementId = string;
            return this;
        }

        public BuildJob.BuildJobBuilder rotation(BlockRotation blockRotation) {
            this.rotation = blockRotation;
            return this;
        }

        public BuildJob.BuildJobBuilder server(String string) {
            this.server = string;
            return this;
        }

        public BuildJob.BuildJobBuilder dimension(String string) {
            this.dimension = string;
            return this;
        }

        public BuildJob.BuildJobBuilder origin(BlockPos blockPos) {
            this.origin = blockPos;
            return this;
        }

        BuildJob.BuildJobBuilder size(Vec3i vec3i) {
            this.size = vec3i;
            return this;
        }

        public BuildJob build() {
            return new BuildJob(
                this.name, this.source, this.file, this.placement, this.placementId, this.rotation, this.server, this.dimension, this.origin, this.size
            );
        }

        @Override
        public String toString() {
            return "BuildJob.BuildJobBuilder(name="
                + this.name
                + ", source="
                + this.source
                + ", file="
                + this.file
                + ", placement="
                + this.placement
                + ", placementId="
                + this.placementId
                + ", rotation="
                + this.rotation
                + ", server="
                + this.server
                + ", dimension="
                + this.dimension
                + ", origin="
                + this.origin
                + ", size="
                + this.size
                + ")";
        }
    }

    public enum Source {
        FILE,
        LITEMATICA;
    }

    public enum State {
        NEW,
        LOADING,
        READY,
        FAILED;
    }
}
