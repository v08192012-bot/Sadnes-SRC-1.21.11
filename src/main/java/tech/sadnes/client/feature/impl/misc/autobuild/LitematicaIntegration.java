package tech.sadnes.client.feature.impl.misc.autobuild;

import baritone.api.schematic.ISchematic;
import baritone.api.schematic.IStaticSchematic;
import baritone.utils.schematic.litematica.LitematicaHelper;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LitematicaIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitematicaIntegration.class);
    volatile World schematicWorld;
    Method getChunkStateMethod;
    private boolean failed;

    public boolean isLitematicaLoaded() {
        return FabricLoader.getInstance().isModLoaded("litematica");
    }

    public List<LitematicaIntegration.PlacementInfo> getPlacements() {
        this.failed = false;
        ArrayList arrayList = new ArrayList();
        if (!this.isLitematicaLoaded()) {
            return arrayList;
        }

        try {
            List list = this.getAllPlacements();
            int i = list.size();

            for (int i2 = 0; i2 < i; i2++) {
                Object object = list.get(i2);
                arrayList.add(
                    new LitematicaIntegration.PlacementInfo(
                        i2, (String)invokeGetter(object, "getName"), String.valueOf(invokeGetter(object, "getHashId")), (Boolean)invokeGetter(object, "isEnabled")
                    )
                );
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            this.onIntegrationError(exception);
        }

        return arrayList;
    }

    public List<LitematicaIntegration.PlacementInfo> getActivePlacements() {
        List<LitematicaIntegration.PlacementInfo> placementInfos = this.getPlacements();
        boolean flag = this.failed;
        int i = this.getSelectedPlacementIndex();
        this.failed |= flag;
        ArrayList arrayList = new ArrayList();

        for (LitematicaIntegration.PlacementInfo placementInfo : placementInfos) {
            boolean flag2;
            if (i >= 0) {
                if (placementInfo.index() != i) {
                    continue;
                }

                flag2 = true;
            } else {
                flag2 = placementInfo.enabled();
            }

            boolean flag3 = flag2;
            if (flag3) {
                arrayList.add(placementInfo);
            }
        }

        return arrayList;
    }

    public LitematicaIntegration.PlacementInfo findPlacement(String string, String string2) {
        for (LitematicaIntegration.PlacementInfo placementInfo : this.getPlacements()) {
            boolean flag = string == null ? placementInfo.name().equals(string2) : string.equals(placementInfo.id());
            if (flag) {
                return placementInfo;
            }
        }

        return null;
    }

    public int getSelectedPlacementIndex() {
        this.failed = false;
        if (!this.isLitematicaLoaded()) {
            return -1;
        }

        try {
            Object object = this.getPlacementManager();
            Object object2 = invokeGetter(object, "getSelectedSchematicPlacement");
            return object2 == null ? -1 : this.getAllPlacements().indexOf(object2);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            this.onIntegrationError(exception);
            return -1;
        }
    }

    public LitematicaIntegration.PlacementSchematic loadSchematic(LitematicaIntegration.PlacementInfo placementInfo) {
        try {
            this.refreshSchematicWorld();
            Object object = this.getAllPlacements().get(placementInfo.index());
            Pair pair = LitematicaHelper.getSchematic(placementInfo.index());
            LongArrayList longArrayList = new LongArrayList();

            for (Object object2 : (Collection)invokeGetter(object, "getTouchedChunks")) {
                longArrayList.add(((ChunkPos)object2).toLong());
            }

            LitematicaIntegration.LoadedPlacementSchematic loadedPlacementSchematic = new LitematicaIntegration.LoadedPlacementSchematic(
                (IStaticSchematic)pair.getLeft(), (Vec3i)pair.getRight(), longArrayList, buildPlacementSignature(object)
            );
            loadedPlacementSchematic.loadReadyChunks();
            return new LitematicaIntegration.PlacementSchematic(loadedPlacementSchematic, (Vec3i)pair.getRight());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            this.onIntegrationError(exception);
            return null;
        }
    }

    public String getPlacementSignature(String string, String string2) {
        LitematicaIntegration.PlacementInfo placementInfo = this.findPlacement(string, string2);
        if (this.failed) {
            return null;
        }

        if (placementInfo == null) {
            return "";
        }

        try {
            return buildPlacementSignature(this.getAllPlacements().get(placementInfo.index()));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            this.onIntegrationError(exception);
            return null;
        }
    }

    public void refreshSchematicWorld() {
        if (!this.isLitematicaLoaded()) {
            this.schematicWorld = null;
        } else {
            try {
                Object object = Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler").getMethod("getSchematicWorld").invoke(null);
                this.schematicWorld = object instanceof World ? (World)object : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                this.schematicWorld = null;
            }
        }
    }

    private static String buildPlacementSignature(Object object) throws ReflectiveOperationException {
        StringBuilder builder = new StringBuilder();

        for (String string : List.of("getOrigin", "getRotation", "getMirror", "isEnabled")) {
            builder.append(invokeGetter(object, string)).append(';');
        }

        Map<?, ?> map = (Map<?, ?>)invokeGetter(object, "getEnabledRelativeSubRegionPlacements");

        for (Entry<?, ?> entry : map.entrySet()) {
            builder.append(entry.getKey()).append(':');

            for (String string2 : List.of("getPos", "getRotation", "getMirror")) {
                builder.append(invokeGetter(entry.getValue(), string2)).append(';');
            }
        }

        return builder.toString();
    }

    private static Object invokeGetter(Object object, String string) throws ReflectiveOperationException {
        return object.getClass().getMethod(string).invoke(object);
    }

    private List<?> getAllPlacements() throws ReflectiveOperationException {
        return (List<?>)invokeGetter(this.getPlacementManager(), "getAllSchematicsPlacements");
    }

    private Object getPlacementManager() throws ReflectiveOperationException {
        return Class.forName("fi.dy.masa.litematica.data.DataManager").getMethod("getSchematicPlacementManager").invoke(null);
    }

    void onIntegrationError(Throwable throwable) {
        this.failed = true;
        LOGGER.warn("Litematica integration failed", throwable);
    }

    public boolean isFailed() {
        return this.failed;
    }

    public final class LoadedPlacementSchematic implements ISchematic {
        private final IStaticSchematic source;
        private final Vec3i origin;
        private final int width;
        private final int height;
        private final int length;
        private final BlockState[] states;
        private final LongArrayList pendingChunks;
        private final Mutable mutablePos = new Mutable();
        private final String signature;
        private String lastChangedSignature;
        private boolean failed;
        private int loadedChunks;

        LoadedPlacementSchematic(IStaticSchematic iStaticSchematic, Vec3i vec3i, LongArrayList longArrayList, String string) {
            this.source = iStaticSchematic;
            this.origin = vec3i;
            this.width = iStaticSchematic.widthX();
            this.height = iStaticSchematic.heightY();
            this.length = iStaticSchematic.lengthZ();
            this.states = new BlockState[this.width * this.height * this.length];
            this.pendingChunks = longArrayList;
            this.signature = string;
        }

        public int getPendingChunkCount() {
            return this.pendingChunks.size();
        }

        public void loadReadyChunks() {
            World world = LitematicaIntegration.this.schematicWorld;
            if (world != null && !this.failed && !this.pendingChunks.isEmpty()) {
                try {
                    ChunkManager chunkManager = world.getChunkManager();
                    if (LitematicaIntegration.this.getChunkStateMethod == null) {
                        LitematicaIntegration.this.getChunkStateMethod = chunkManager.getClass().getMethod("getChunkState", int.class, int.class);
                    }

                    int i = 0;

                    for (int i2 = this.pendingChunks.size() - 1; i2 >= 0 && i < 4; i2--) {
                        long l = this.pendingChunks.getLong(i2);
                        int i3 = ChunkPos.getPackedX(l);
                        int i4 = ChunkPos.getPackedZ(l);
                        String string = ((Enum)LitematicaIntegration.this.getChunkStateMethod.invoke(chunkManager, i3, i4)).name();
                        if (string.equals("FILLED") || string.equals("RENDERED")) {
                            this.copyChunk(world.getChunk(i3, i4), i3, i4);
                            this.pendingChunks.removeLong(i2);
                            this.loadedChunks++;
                            i++;
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                    this.failed = true;
                    LitematicaIntegration.this.onIntegrationError(exception);
                }
            }
        }

        public boolean isSignatureChangeConfirmed(String string) {
            boolean flag = !this.signature.equals(string);
            boolean flag2;
            if (flag) {
                flag2 = string.equals(this.lastChangedSignature);
            } else {
                flag2 = false;
            }

            if (flag) {
                this.lastChangedSignature = string;
                return flag2;
            } else {
                this.lastChangedSignature = null;
                return flag2;
            }
        }

        private void copyChunk(Chunk chunk, int i, int i2) {
            int i3 = Math.max((i << 4) - this.origin.getX(), 0);
            int i4 = Math.min((i << 4) + 15 - this.origin.getX(), this.width - 1);
            int i5 = Math.max((i2 << 4) - this.origin.getZ(), 0);
            int i6 = Math.min((i2 << 4) + 15 - this.origin.getZ(), this.length - 1);

            for (int i7 = i3; i7 <= i4; i7++) {
                for (int i8 = i5; i8 <= i6; i8++) {
                    int i9 = (i7 * this.length + i8) * this.height;

                    for (int i10 = 0; i10 < this.height; i10++) {
                        this.states[i9 + i10] = chunk.getBlockState(
                            this.mutablePos.set(this.origin.getX() + i7, this.origin.getY() + i10, this.origin.getZ() + i8)
                        );
                    }
                }
            }
        }

        @Override
        public boolean inSchematic(int i, int i2, int i3, BlockState blockState) {
            return this.source.inSchematic(i, i2, i3, blockState) && this.states[(i * this.length + i3) * this.height + i2] != null;
        }

        @Override
        public BlockState desiredState(int i, int i2, int i3, BlockState blockState, List<BlockState> blockStates) {
            return this.states[(i * this.length + i3) * this.height + i2];
        }

        @Override
        public int widthX() {
            return this.width;
        }

        @Override
        public int heightY() {
            return this.height;
        }

        @Override
        public int lengthZ() {
            return this.length;
        }

        public int getLoadedChunks() {
            return this.loadedChunks;
        }
    }

    public record PlacementInfo(int index, String name, String id, boolean enabled) {
    }

    public record PlacementSchematic(LitematicaIntegration.LoadedPlacementSchematic schematic, Vec3i origin) {
    }
}
