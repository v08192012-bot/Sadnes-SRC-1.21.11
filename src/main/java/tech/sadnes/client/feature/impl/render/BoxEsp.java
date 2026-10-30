package tech.sadnes.client.feature.impl.render;

import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.BlockUpdateEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.BoxEspMaskRenderer;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.setting.ColorSetting;

@FeatureInfo(
    name = "BoxEsp",
    description = "feature.impl.render.boxesp",
    displayName = "feature.impl.render.boxesp",
    category = Category.VISUALS,
    subcategory = Subcategory.HIGHLIGHTS
)
public final class BoxEsp extends Feature {
    private final ColorSetting color = new ColorSetting("boxesp.color", -8484097);
    private final BoxEspMaskRenderer boxRenderer = new BoxEspMaskRenderer();
    private final Map<String, OptionalInt> blockFilters = new ConcurrentHashMap<>();
    private final Map<String, OptionalInt> entityFilters = new ConcurrentHashMap<>();
    private final Map<BlockPos, BoxEsp.BlockMatch> matchedBlocks = new ConcurrentHashMap<>();
    private final Map<Block, Boolean> blockMatchCache = new ConcurrentHashMap<>();
    private final Map<Block, OptionalInt> blockColorCache = new ConcurrentHashMap<>();
    private volatile int scanGeneration;

    public static BoxEsp getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(BoxEsp.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!this.blockFilters.isEmpty()) {
            this.rescanChunks();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.boxRenderer.close();
        this.scanGeneration++;
        this.matchedBlocks.clear();
        this.blockMatchCache.clear();
        this.blockColorCache.clear();
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
        if (!this.blockFilters.isEmpty()) {
            BlockPos blockPos = blockUpdateEvent.getPos();
            if (this.matchesBlock(blockUpdateEvent.getState())) {
                this.matchedBlocks.put(blockPos.toImmutable(), new BoxEsp.BlockMatch(blockUpdateEvent.getState(), this.getBlockColor(blockUpdateEvent.getState())));
            } else {
                this.matchedBlocks.remove(blockPos);
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.matchedBlocks.clear();
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && mc.world != null && mc.player != null) {
            if (!this.matchedBlocks.isEmpty() || !this.entityFilters.isEmpty()) {
                int i = this.color.getValue();
                MatrixStack matrixStack = renderEvent.getMatrices();
                Vec3d vec3d = renderEvent.getCamera().getCameraPos();

                try (BoxEspMaskRenderer.Batch batch = this.boxRenderer.begin(i)) {
                    this.renderBlocks(matrixStack, vec3d, i, batch);
                    this.renderEntities(matrixStack, vec3d, renderEvent.getTickDelta(), i, batch);
                    batch.finish();
                }
            }
        }
    }

    private void renderBlocks(MatrixStack matrixStack, Vec3d vec3d, int i, BoxEspMaskRenderer.Batch batch) {
        if (!this.matchedBlocks.isEmpty()) {
            for (Entry entry : this.matchedBlocks.entrySet()) {
                BlockPos blockPos = (BlockPos)entry.getKey();
                BoxEsp.BlockMatch blockMatch = (BoxEsp.BlockMatch)entry.getValue();
                int i2 = blockMatch.color().orElse(i);
                VoxelShape voxelShape = blockMatch.state().getOutlineShape(mc.world, blockPos);
                if (voxelShape.isEmpty()) {
                    this.addBox(batch, matrixStack, vec3d, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1.0, blockPos.getY() + 1.0, blockPos.getZ() + 1.0, i2);
                } else {
                    voxelShape.forEachBox(
                        (d, d2, d3, d4, d5, d6) -> this.addBox(
                            batch,
                            matrixStack,
                            vec3d,
                            blockPos.getX() + d,
                            blockPos.getY() + d2,
                            blockPos.getZ() + d3,
                            blockPos.getX() + d4,
                            blockPos.getY() + d5,
                            blockPos.getZ() + d6,
                            i2
                        )
                    );
                }
            }
        }
    }

    private void addBox(
        BoxEspMaskRenderer.Batch batch, MatrixStack matrixStack, Vec3d vec3d, double d, double d2, double d3, double d4, double d5, double d6, int i
    ) {
        batch.addBox(
            matrixStack,
            (float)(d - vec3d.x),
            (float)(d2 - vec3d.y),
            (float)(d3 - vec3d.z),
            (float)(d4 - vec3d.x),
            (float)(d5 - vec3d.y),
            (float)(d6 - vec3d.z),
            i
        );
    }

    private void renderEntities(MatrixStack matrixStack, Vec3d vec3d, float f, int i, BoxEspMaskRenderer.Batch batch) {
        if (!this.entityFilters.isEmpty()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity != mc.player && entity.isAlive()) {
                    OptionalInt optionalInt = this.getEntityColor(entity);
                    if (optionalInt != null) {
                        int i2 = optionalInt.orElse(i);
                        Box box = entity.getBoundingBox();
                        float f2 = (float)((box.maxX - box.minX) / 2.0);
                        float f3 = (float)((box.maxZ - box.minZ) / 2.0);
                        float f4 = (float)(box.maxY - box.minY) - (entity.isSneaking() ? 0.1F : 0.0F);
                        Vec3d vec3d2 = ProjectionUtil.getInterpolatedPos(entity, f);
                        matrixStack.push();
                        matrixStack.translate(vec3d2.x - vec3d.x, vec3d2.y - vec3d.y, vec3d2.z - vec3d.z);
                        matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(getBodyYaw(entity, f)));
                        batch.addBox(matrixStack, -f2, 0.0F, -f3, f2, f4, f3, i2);
                        matrixStack.pop();
                    }
                }
            }
        }
    }

    private static float getBodyYaw(Entity entity, float f) {
        return entity instanceof LivingEntity livingEntity ? MathHelper.lerp(f, livingEntity.lastBodyYaw, livingEntity.bodyYaw) : MathHelper.lerp(f, entity.lastYaw, entity.getYaw());
    }

    public boolean addBlock(String string, OptionalInt optionalInt) {
        String string2 = this.normalizeQuery(string);
        if (!string2.isEmpty() && this.blockFilters.putIfAbsent(string2, optionalInt) == null) {
            this.onBlockFiltersChanged();
            return true;
        } else {
            return false;
        }
    }

    public boolean removeBlock(String string) {
        String string2 = this.normalizeQuery(string);
        if (!string2.isEmpty() && this.blockFilters.remove(string2) != null) {
            this.onBlockFiltersChanged();
            return true;
        } else {
            return false;
        }
    }

    public boolean addEntity(String string, OptionalInt optionalInt) {
        String string2 = this.normalizeQuery(string);
        return !string2.isEmpty() && this.entityFilters.putIfAbsent(string2, optionalInt) == null;
    }

    public boolean removeEntity(String string) {
        String string2 = this.normalizeQuery(string);
        return !string2.isEmpty() && this.entityFilters.remove(string2) != null;
    }

    public void clearAll() {
        this.scanGeneration++;
        this.blockFilters.clear();
        this.entityFilters.clear();
        this.matchedBlocks.clear();
        this.blockMatchCache.clear();
        this.blockColorCache.clear();
    }

    public Map<String, OptionalInt> getBlockFilters() {
        return Map.copyOf(this.blockFilters);
    }

    public Map<String, OptionalInt> getEntityFilters() {
        return Map.copyOf(this.entityFilters);
    }

    private void onBlockFiltersChanged() {
        this.blockMatchCache.clear();
        this.blockColorCache.clear();
        if (this.isEnabled()) {
            this.rescanChunks();
        }
    }

    private void rescanChunks() {
        int i = ++this.scanGeneration;
        this.matchedBlocks.clear();
        if (!this.blockFilters.isEmpty() && mc.world != null && mc.player != null) {
            ClientChunkManager clientChunkManager = mc.world.getChunkManager();
            int i2 = mc.player.getBlockPos().getX() >> 4;
            int i3 = mc.player.getBlockPos().getZ() >> 4;
            int i4 = mc.options.getViewDistance().getValue();
            CompletableFuture.runAsync(() -> {
                for (int i5 = i2 - i4; i5 <= i2 + i4; i5++) {
                    for (int i6 = i3 - i4; i6 <= i3 + i4; i6++) {
                        if (i != this.scanGeneration) {
                            return;
                        }

                        WorldChunk worldChunk = clientChunkManager.getChunk(i5, i6, ChunkStatus.FULL, false);
                        if (worldChunk != null) {
                            this.scanChunk(worldChunk, i);
                        }
                    }
                }
            });
        }
    }

    private void scanChunk(WorldChunk worldChunk, int i) {
        int i2 = worldChunk.getPos().getStartX();
        int i3 = worldChunk.getPos().getStartZ();
        int i4 = worldChunk.getHighestNonEmptySection();
        int i5 = worldChunk.getBottomY() >> 4;

        for (int i6 = 0; i6 <= i4; i6++) {
            if (i != this.scanGeneration) {
                return;
            }

            ChunkSection chunkSection = worldChunk.getSection(i6);
            if (!chunkSection.isEmpty()) {
                for (int i7 = 0; i7 < 16; i7++) {
                    int i8 = i6 + i5 << 4 | i7;

                    for (int i9 = 0; i9 < 16; i9++) {
                        for (int i10 = 0; i10 < 16; i10++) {
                            BlockState blockState = chunkSection.getBlockState(i9, i7, i10);
                            if (!blockState.isAir() && this.matchesBlock(blockState)) {
                                this.matchedBlocks
                                    .put(new BlockPos(i2 | i9, i8, i3 | i10), new BoxEsp.BlockMatch(blockState, this.getBlockColor(blockState)));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean matchesBlock(BlockState blockState) {
        Block block = blockState.getBlock();
        Boolean flag = this.blockMatchCache.get(block);
        if (flag != null) {
            return flag;
        }

        Identifier identifier = Registries.BLOCK.getId(block);
        boolean flag2 = identifier != null
            && this.findMatch(this.blockFilters, identifier.toString().toLowerCase(Locale.ROOT), identifier.getPath().toLowerCase(Locale.ROOT)) != null;
        this.blockMatchCache.put(block, flag2);
        return flag2;
    }

    private OptionalInt getBlockColor(BlockState blockState) {
        Block block = blockState.getBlock();
        OptionalInt optionalInt = this.blockColorCache.get(block);
        if (optionalInt != null) {
            return optionalInt;
        }

        Identifier identifier = Registries.BLOCK.getId(block);
        OptionalInt optionalInt2 = identifier == null
            ? null
            : this.findMatch(this.blockFilters, identifier.toString().toLowerCase(Locale.ROOT), identifier.getPath().toLowerCase(Locale.ROOT));
        OptionalInt optionalInt3 = optionalInt2 == null ? OptionalInt.empty() : optionalInt2;
        this.blockColorCache.put(block, optionalInt3);
        return optionalInt3;
    }

    private OptionalInt getEntityColor(Entity entity) {
        Identifier identifier = EntityType.getId(entity.getType());
        String string = identifier == null ? "" : identifier.toString().toLowerCase(Locale.ROOT);
        String string2 = identifier == null ? "" : identifier.getPath().toLowerCase(Locale.ROOT);
        String string3 = entity.getName().getString().toLowerCase(Locale.ROOT);
        String string4 = entity.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return this.findMatch(this.entityFilters, string, string2, string3, string4);
    }

    private OptionalInt findMatch(Map<String, OptionalInt> map, String... strings) {
        for (Entry entry : map.entrySet()) {
            String string = (String)entry.getKey();

            for (String string2 : strings) {
                if (!string2.isEmpty() && string2.contains(string)) {
                    return (OptionalInt)entry.getValue();
                }
            }
        }

        return null;
    }

    private String normalizeQuery(String string) {
        return string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
    }

    record BlockMatch(BlockState state, OptionalInt color) {
    }
}
