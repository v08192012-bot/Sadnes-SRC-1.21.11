package tech.sadnes.client.feature.impl.misc.farmbase;

import it.unimi.dsi.fastutil.ints.IntList;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.api.mixin.accessor.EntityStatusS2CPacketAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.BoxEspMaskRenderer;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "FarmBase",
    description = "feature.impl.misc.farmbase.farmbase",
    displayName = "feature.impl.misc.farmbase.farmbase",
    category = Category.VISUALS,
    subcategory = Subcategory.HIGHLIGHTS
)
public final class FarmBase extends HudFeature<FarmBase.CounterEntry> {
    private final MultiSetting show = new MultiSetting("farmbase.show").addOption("hostile", true).addOption("passive", true).addOption("damager", true);
    private final MultiSetting chat = new MultiSetting("farmbase.chat").addOption("found", true).addOption("spawned", true);
    private final NumberSetting chatDelay = new NumberSetting("farmbase.chatDelay", 10.0F, 0.0F, 60.0F, 1.0F, Unit.SECOND)
        .setVisibleWhen(() -> this.chat.has("spawned"));
    private final ColorSetting hostileColor = new ColorSetting("farmbase.color.hostile", ColorUtil.rgb(235, 64, 52))
        .setVisibleWhen(() -> this.show.has("hostile"));
    private final ColorSetting passiveColor = new ColorSetting("farmbase.color.passive", ColorUtil.rgb(60, 200, 90))
        .setVisibleWhen(() -> this.show.has("passive"));
    private final ColorSetting emptyColor = new ColorSetting("farmbase.color.empty", ColorUtil.rgb(150, 150, 160));
    private final ColorSetting damagerColor = new ColorSetting("farmbase.color.damager", ColorUtil.rgb(245, 170, 40))
        .setVisibleWhen(() -> this.show.has("damager"));
    private final GroupSetting tracers = new GroupSetting("farmbase.tracers", true);
    private final ModeSetting tracerTargets = new ModeSetting(this.tracers, "farmbase.tracers.targets", new String[]{"all", "farm"});
    private final NumberSetting tracerRange = new NumberSetting(this.tracers, "farmbase.tracers.range", 96.0F, 16.0F, 256.0F, 4.0F, Unit.METER);
    private final NumberSetting tracerWidth = new NumberSetting(this.tracers, "farmbase.tracers.width", 2.0F, 0.5F, 4.0F, 0.1F, Unit.PIXEL);
    private final NumberSetting tracerFresh = new NumberSetting(this.tracers, "farmbase.tracers.fresh", 5.0F, 0.0F, 30.0F, 1.0F, Unit.SECOND);
    private final GroupSetting storage = new GroupSetting("farmbase.storage", false);
    private final MultiSetting storageShow = new MultiSetting(this.storage, "farmbase.storage.show")
        .addOption("chest", true)
        .addOption("trapped", true)
        .addOption("shulker", true)
        .addOption("ender", true)
        .addOption("other", true);
    private final NumberSetting storageRange = new NumberSetting(this.storage, "farmbase.storage.range", 64.0F, 16.0F, 256.0F, 4.0F, Unit.METER);
    private final NumberSetting storageFade = new NumberSetting(this.storage, "farmbase.storage.fade", 6.0F, 0.0F, 12.0F, 1.0F, Unit.METER);
    private final BooleanSetting storageHideOpened = new BooleanSetting(this.storage, "farmbase.storage.hideOpened", false);
    private final BooleanSetting storageTracers = new BooleanSetting(this.storage, "farmbase.storage.tracers", false);
    private final ColorSetting chestColor = new ColorSetting(this.storage, "farmbase.storage.color.chest", ColorUtil.rgb(255, 160, 0))
        .setVisibleWhen(() -> this.storageShow.has("chest"));
    private final ColorSetting trappedColor = new ColorSetting(this.storage, "farmbase.storage.color.trapped", ColorUtil.rgb(235, 64, 52))
        .setVisibleWhen(() -> this.storageShow.has("trapped"));
    private final ColorSetting shulkerColor = new ColorSetting(this.storage, "farmbase.storage.color.shulker", ColorUtil.rgb(180, 100, 220))
        .setVisibleWhen(() -> this.storageShow.has("shulker"));
    private final ColorSetting enderColor = new ColorSetting(this.storage, "farmbase.storage.color.ender", ColorUtil.rgb(120, 0, 255))
        .setVisibleWhen(() -> this.storageShow.has("ender"));
    private final ColorSetting otherColor = new ColorSetting(this.storage, "farmbase.storage.color.other", ColorUtil.rgb(140, 140, 140))
        .setVisibleWhen(() -> this.storageShow.has("other"));
    private final GroupSetting spots = new GroupSetting("farmbase.spots", false);
    private final ModeSetting spotsArea = new ModeSetting(this.spots, "farmbase.spots.area", new String[]{"spawners", "player", "both"});
    private final NumberSetting spotsLight = new NumberSetting(this.spots, "farmbase.spots.light", 0.0F, 0.0F, 15.0F, 1.0F);
    private final NumberSetting spotsRadius = new NumberSetting(this.spots, "farmbase.spots.radius", 8.0F, 1.0F, 16.0F, 1.0F, Unit.METER)
        .setVisibleWhen(() -> !this.spotsArea.is("spawners") ? true : false);
    private final NumberSetting spotsHeight = new NumberSetting(this.spots, "farmbase.spots.height", 4.0F, 0.0F, 8.0F, 1.0F, Unit.METER)
        .setVisibleWhen(() -> !this.spotsArea.is("spawners") ? true : false);
    private final BooleanSetting spotsZone = new BooleanSetting(this.spots, "farmbase.spots.zone", true)
        .setVisibleWhen(() -> !this.spotsArea.is("player") ? true : false);
    private final ColorSetting alwaysSpotColor = new ColorSetting(this.spots, "farmbase.spots.color.always", ColorUtil.rgb(225, 25, 25));
    private final ColorSetting potentialSpotColor = new ColorSetting(this.spots, "farmbase.spots.color.potential", ColorUtil.rgb(225, 225, 25));
    private final BooleanSetting panel = new BooleanSetting("farmbase.panel", true);
    private final SpawnerTracker spawnerTracker = new SpawnerTracker();
    private final DamagerDetector damagerDetector = new DamagerDetector();
    private final PendingSpawnTracker pendingSpawnTracker = new PendingSpawnTracker();
    private final SpawnClusterDetector spawnClusterDetector = new SpawnClusterDetector();
    private final FarmTracker farmTracker = new FarmTracker();
    private final StorageTracker storageTracker = new StorageTracker();
    private final SpawnableBlockScanner spawnableBlockScanner = new SpawnableBlockScanner();
    private final BoxEspMaskRenderer boxRenderer = new BoxEspMaskRenderer();
    private BlockPos lastScanPos;
    private final Map<Integer, Integer> freshSpawnTicks = new HashMap<>();
    private final Map<String, FarmBase.CounterEntry> counterEntries = new HashMap<>();
    private final StopWatch panelUpdateTimer = new StopWatch();
    private WeakReference<ClientWorld> lastWorld = new WeakReference<>(null);
    private boolean pendingStatusMessage;
    private boolean obfuscationWarned;
    private int ticks;

    public FarmBase() {
        super(10.0F, 160.0F);

        for (SpawnerCategory spawnerCategory : SpawnerCategory.values()) {
            BooleanSetting booleanSetting = this.show.getOption(spawnerCategory.getKey());
            if (booleanSetting != null) {
                booleanSetting.setIcon(spawnerCategory.getIcon());
            }
        }

        for (StorageType storageType : StorageType.values()) {
            BooleanSetting booleanSetting2 = this.storageShow.getOption(storageType.getKey());
            if (booleanSetting2 != null) {
                booleanSetting2.setIcon(storageType.getIcon());
            }
        }

        this.tracers.setIcon(Icon.CHART_LINE);
        this.storage.setIcon(Icon.BOXES);
        this.spots.setIcon(Icon.LIGHTBULB);
        this.style.setVisibleWhen(() -> false);
        this.rightAligned.setVisibleWhen(() -> false);
        this.dynamicHeader.setVisibleWhen(this.panel::getValue);
        this.chip.setVisibleWhen(this.panel::getValue);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.lastWorld = new WeakReference<>(mc.world);
        this.pendingSpawnTracker.resetGrace(this.ticks);
        this.pendingStatusMessage = true;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.boxRenderer.close();
        this.reset();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        if (this.lastWorld.get() != mc.world) {
            this.reset();
        }

        this.lastWorld = new WeakReference<>(mc.world);
        this.pendingSpawnTracker.resetGrace(this.ticks);
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            this.handlePacket(packetEvent.getPacket());
        } else {
            if (packetEvent.isSend() && packetEvent.getPacket() instanceof PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket) {
                BlockPos blockPos = playerInteractBlockC2SPacket.getBlockHitResult().getBlockPos();
                mc.execute(() -> {
                    if (!this.isInactive()) {
                        this.storageTracker.onInteract(mc.world, blockPos, this.ticks);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            this.ticks++;
            if (this.ticks % 10 == 1) {
                this.spawnerTracker.scanChunks(mc.world, mc.player.getChunkPos(), this.getViewDistance());
                this.updateFarms();
                this.updateStorage();
                if (this.pendingStatusMessage) {
                    this.printStatus();
                }
            }

            this.updateSpawnableSpots();
            this.freshSpawnTicks.values().removeIf(integer -> this.ticks - integer > (int)(this.tracerFresh.getValue() * 20.0F));

            for (PendingSpawnTracker.PendingSpawn pendingSpawn : this.pendingSpawnTracker.collectExpired(this.ticks, mc.player.getEntityPos())) {
                this.confirmSpawn(pendingSpawn);
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (!ServerManager.isNullWorld()) {
            if (renderEvent.isWorld()) {
                Vec3d vec3d = renderEvent.getCamera().getCameraPos();
                this.renderBoxes(renderEvent.getMatrices(), vec3d);
                this.renderSpawnableSpots(renderEvent.getMatrices());
                this.renderTracers(renderEvent.getMatrices(), vec3d, renderEvent.getTickDelta());
            } else {
                if (renderEvent.isHudPre()) {
                    this.renderLabels(renderEvent.getContext());
                }
            }
        }
    }

    private void reset() {
        this.spawnerTracker.clear();
        this.spawnClusterDetector.clear();
        this.freshSpawnTicks.clear();
        this.damagerDetector.clear();
        this.pendingSpawnTracker.clear();
        this.farmTracker.reset();
        this.storageTracker.reset();
        this.spawnableBlockScanner.clear();
        this.lastScanPos = null;
        this.obfuscationWarned = false;
        this.counterEntries.clear();
        this.ticks = 0;
    }

    private void handlePacket(Packet<?> packet) {
        switch (packet) {
            case BundleS2CPacket bundleS2CPacket -> {
                for (Packet<?> packet2 : bundleS2CPacket.getPackets()) {
                    this.handlePacket(packet2);
                }
            }
            case EntitySpawnS2CPacket entitySpawnS2CPacket -> mc.execute(() -> this.onEntitySpawn(entitySpawnS2CPacket));
            case ChunkDataS2CPacket chunkDataS2CPacket -> mc.execute(() -> this.onChunkData(chunkDataS2CPacket));
            case BlockEntityUpdateS2CPacket blockEntityUpdateS2CPacket when blockEntityUpdateS2CPacket.getBlockEntityType() == BlockEntityType.MOB_SPAWNER ->
                mc.execute(() -> this.onSpawnerData(blockEntityUpdateS2CPacket.getPos(), blockEntityUpdateS2CPacket.getNbt()));
            case BlockEventS2CPacket blockEventS2CPacket when blockEventS2CPacket.getBlock() == Blocks.SPAWNER -> mc.execute(() -> this.onSpawnerBlockEvent(blockEventS2CPacket.getPos()));
            case BlockEventS2CPacket blockEventS2CPacket2 when blockEventS2CPacket2.getType() == 1 && StorageType.fromBlock(blockEventS2CPacket2.getBlock()) != null -> {
                BlockPos blockPos = blockEventS2CPacket2.getPos();
                StorageType storageType = StorageType.fromBlock(blockEventS2CPacket2.getBlock());
                mc.execute(() -> {
                    if (!this.isInactive() && this.storage.isEnabled()) {
                        this.storageTracker.remember(blockPos, storageType);
                    }
                });
            }
            case EntitiesDestroyS2CPacket entitiesDestroyS2CPacket -> mc.execute(() -> this.onEntitiesRemoved(entitiesDestroyS2CPacket.getEntityIds()));
            case WorldEventS2CPacket worldEventS2CPacket when worldEventS2CPacket.getEventId() == 2004 -> mc.execute(() -> this.onSpawnerSpawnEvent(worldEventS2CPacket.getPos()));
            case EntityStatusS2CPacket entityStatusS2CPacket when entityStatusS2CPacket.getStatus() == 20 -> {
                int i = ((EntityStatusS2CPacketAccessor)entityStatusS2CPacket).sadnes$getEntityId();
                mc.execute(() -> this.onEntityDeathStatus(i));
            }
            case OpenScreenS2CPacket openScreenS2CPacket -> {
                StorageType storageType2 = StorageType.fromScreenHandler(openScreenS2CPacket.getScreenHandlerType());
                mc.execute(() -> {
                    if (!this.isInactive()) {
                        this.storageTracker.onScreenOpened(storageType2, this.ticks);
                    }
                });
            }
            case PlayerPositionLookS2CPacket playerPositionLookS2CPacket -> mc.execute(() -> this.pendingSpawnTracker.resetGrace(this.ticks));
            default -> {
            }
        }
    }

    private boolean isInactive() {
        return !this.isEnabled() || ServerManager.isNullWorld();
    }

    private void onEntitySpawn(EntitySpawnS2CPacket entitySpawnS2CPacket) {
        if (!this.isInactive()) {
            EntityType entityType = entitySpawnS2CPacket.getEntityType();
            Vec3d vec3d = new Vec3d(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ());
            if (entityType == EntityType.WOLF) {
                this.damagerDetector.recordSpawn(entitySpawnS2CPacket.getEntityId(), vec3d, this.ticks);
            }

            TrackedSpawner trackedSpawner = this.spawnerTracker.findSpawnerForEntity(entityType, vec3d.x, vec3d.y, vec3d.z);
            if (entityType.getSpawnGroup() != SpawnGroup.MISC || trackedSpawner != null && entityType == trackedSpawner.getEntityType()) {
                this.pendingSpawnTracker.add(new PendingSpawnTracker.PendingSpawn(entitySpawnS2CPacket.getEntityId(), entityType, vec3d, trackedSpawner, this.ticks));
                if (trackedSpawner == null) {
                    this.onMobSpawned(entityType, vec3d);
                }
            }
        }
    }

    private void onMobSpawned(EntityType<?> entityType, Vec3d vec3d) {
        if (!this.pendingSpawnTracker.isInGrace(this.ticks) && !(mc.player.squaredDistanceTo(vec3d) > 400.0)) {
            BlockPos blockPos = this.spawnClusterDetector.recordSpawn(entityType, vec3d, this.ticks);
            if (blockPos != null) {
                this.spawnerTracker.getOrCreateWithDefaults(blockPos, entityType).confirm();
                this.updateFarms();
            }
        }
    }

    private void onChunkData(ChunkDataS2CPacket chunkDataS2CPacket) {
        if (!this.isInactive()) {
            chunkDataS2CPacket.getChunkData().getBlockEntities(chunkDataS2CPacket.getChunkX(), chunkDataS2CPacket.getChunkZ()).accept((blockPos, blockEntityType, nbtCompound) -> {
                if (blockEntityType == BlockEntityType.MOB_SPAWNER) {
                    this.onSpawnerData(blockPos.toImmutable(), nbtCompound);
                } else {
                    StorageType storageType = StorageType.fromBlockEntityType(blockEntityType);
                    if (storageType != null && this.storage.isEnabled()) {
                        this.storageTracker.addPendingVerification(blockPos.toImmutable(), storageType);
                    }
                }
            });
        }
    }

    private void onSpawnerData(BlockPos blockPos, NbtCompound nbtCompound) {
        if (!this.isInactive() && nbtCompound != null) {
            EntityType entityType = nbtCompound.getCompound("SpawnData")
                .flatMap(nbtCompound2 -> nbtCompound2.getCompound("entity"))
                .flatMap(nbtCompound3 -> nbtCompound3.getString("id"))
                .flatMap(EntityType::get)
                .orElse(null);
            this.spawnerTracker.updateSpawnerRanges(blockPos, entityType, nbtCompound.getInt("SpawnRange", 4), nbtCompound.getInt("RequiredPlayerRange", 16));
        }
    }

    private void onSpawnerBlockEvent(BlockPos blockPos) {
        if (!this.isInactive()) {
            this.spawnerTracker.getOrCreateWithDefaults(blockPos, this.pendingSpawnTracker.findNearbyType(blockPos)).confirm();
        }
    }

    private void onEntitiesRemoved(IntList intList) {
        if (!this.isInactive()) {
            if (this.damagerDetector.handleRemoved(intList, this.ticks, System.currentTimeMillis())) {
                this.updateFarms();
            }
        }
    }

    private void onSpawnerSpawnEvent(BlockPos blockPos) {
        if (!this.isInactive()) {
            TrackedSpawner trackedSpawner = this.spawnerTracker.getSpawner(blockPos);
            if (trackedSpawner == null) {
                trackedSpawner = this.spawnerTracker.refreshSpawnerAt(mc.world, blockPos);
            }

            if (trackedSpawner == null) {
                trackedSpawner = this.spawnerTracker.getOrCreateWithDefaults(blockPos, this.pendingSpawnTracker.findNearbyType(blockPos));
            }

            trackedSpawner.confirm();
            PendingSpawnTracker.PendingSpawn pendingSpawn = this.pendingSpawnTracker.matchSpawner(trackedSpawner);
            if (pendingSpawn != null) {
                this.confirmSpawn(pendingSpawn);
            }
        }
    }

    private void onEntityDeathStatus(int i) {
        if (!this.isInactive()) {
            PendingSpawnTracker.PendingSpawn pendingSpawn = this.pendingSpawnTracker.takeExact(i);
            if (pendingSpawn != null) {
                this.confirmSpawn(pendingSpawn);
            }
        }
    }

    private void confirmSpawn(PendingSpawnTracker.PendingSpawn pendingSpawn) {
        TrackedSpawner trackedSpawner = pendingSpawn.source();
        if (trackedSpawner != null) {
            trackedSpawner.confirm();
            boolean flag = trackedSpawner.isHidden() && trackedSpawner.getEntityType() != pendingSpawn.type();
            if (flag) {
                this.spawnerTracker.getOrCreateWithDefaults(trackedSpawner.getPos(), pendingSpawn.type()).confirm();
            }

            this.freshSpawnTicks.put(pendingSpawn.entityId(), this.ticks);
            this.notifySpawn(pendingSpawn);
        }
    }

    private int getViewDistance() {
        return mc.options.getViewDistance().getValue() + 2;
    }

    private void updateStorage() {
        if (!this.storage.isEnabled()) {
            this.storageTracker.clearEntries();
        } else {
            double d = this.storageRange.getValue().floatValue();
            int i = Math.min(this.getViewDistance(), (int)Math.ceil(d / 16.0));
            this.storageTracker.update(mc.world, mc.player.getChunkPos(), i, mc.player.getEntityPos(), d, storageType -> this.storageShow.has(storageType.getKey()));
            this.checkObfuscatedStorage();
        }
    }

    private void checkObfuscatedStorage() {
        if (!this.obfuscationWarned && this.storageTracker.getMissingCount() >= 5 && this.chat.has("found")) {
            this.obfuscationWarned = true;
            ChatUtil.print(
                Language.translate("farmbase.message.obfuscated", "Сервер прячет блоки: контейнеров не видно в мире %d из %d — показываю их по пакетам"),
                this.storageTracker.getMissingCount(),
                this.storageTracker.getMissingCount() + this.storageTracker.getVerifiedCount()
            );
        }
    }

    private void updateSpawnableSpots() {
        if (!this.spots.isEnabled()) {
            if (this.lastScanPos != null) {
                this.spawnableBlockScanner.clear();
                this.lastScanPos = null;
            }
        } else {
            BlockPos blockPos = mc.player.getBlockPos();
            boolean flag = !blockPos.equals(this.lastScanPos);
            if (this.ticks % (flag ? 5 : 20) == 0) {
                this.lastScanPos = blockPos;
                int i = this.spotsLight.getValue().intValue();
                this.spawnableBlockScanner.reset();
                if (!this.spotsArea.is("player")) {
                    Vec3d vec3d = mc.player.getEntityPos();

                    for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
                        if (this.isSpawnerVisible(trackedSpawner) && !(vec3d.squaredDistanceTo(trackedSpawner.getBox().getCenter()) > 4096.0)) {
                            BlockPos blockPos2 = trackedSpawner.getPos();
                            int i2 = trackedSpawner.getSpawnRange();
                            this.spawnableBlockScanner.scanForEntity(mc.world, blockPos2.add(-i2, -1, -i2), blockPos2.add(i2, 1, i2), i, trackedSpawner.getEntityType());
                        }
                    }
                }

                if (!this.spotsArea.is("spawners")) {
                    int i3 = this.spotsRadius.getValue().intValue();
                    int i4 = this.spotsHeight.getValue().intValue();
                    this.spawnableBlockScanner.scanGeneric(mc.world, blockPos.add(-i3, -i4, -i3), blockPos.add(i3, i4, i3), i);
                }
            }
        }
    }

    private void updateFarms() {
        List<FarmTracker.Farm> farms = this.farmTracker
            .update(
                this.spawnerTracker.getSpawners(),
                this::isSpawnerVisible,
                this.damagerDetector.getDamagers(),
                damagerBlock -> this.show.has("damager"),
                mc.player.getChunkPos(),
                this.getViewDistance()
            );
        this.farmTracker.syncWaypoints();
        if (this.chat.has("found")) {
            for (FarmTracker.Farm farm : farms) {
                BlockPos blockPos = farm.getPos();
                ChatUtil.print(
                    Language.translate("farmbase.message.found", "Найдена ферма #%d: %s — %d %d %d"),
                    farm.getId(),
                    farm.describe(),
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
                );
            }
        }
    }

    private void printStatus() {
        this.pendingStatusMessage = false;
        if (this.chat.has("found")) {
            int i = 0;
            int i2 = 0;

            for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
                if (this.isSpawnerVisible(trackedSpawner)) {
                    i++;
                } else {
                    i2++;
                }
            }

            ChatUtil.print(Language.translate("farmbase.message.status", "Спавнеров рядом: %d, скрыто фильтром: %d"), i, i2);
        }
    }

    private void notifySpawn(PendingSpawnTracker.PendingSpawn pendingSpawn) {
        TrackedSpawner trackedSpawner = pendingSpawn.source();
        if (trackedSpawner != null && this.chat.has("spawned") && this.isSpawnerVisible(trackedSpawner)) {
            long l = System.currentTimeMillis();
            if (!((float)(l - trackedSpawner.getLastNotifyTime()) < this.chatDelay.getValue() * 1000.0F)) {
                trackedSpawner.setLastNotifyTime(l);
                BlockPos blockPos = BlockPos.ofFloored(pendingSpawn.pos());
                BlockPos blockPos2 = trackedSpawner.getPos();
                ChatUtil.print(
                    Language.translate("farmbase.message.spawned", "Новый моб «%s»: %d %d %d · спавнер %d %d %d"),
                    pendingSpawn.type().getName().getString(),
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ(),
                    blockPos2.getX(),
                    blockPos2.getY(),
                    blockPos2.getZ()
                );
            }
        }
    }

    private boolean isSpawnerVisible(TrackedSpawner trackedSpawner) {
        if (trackedSpawner.isIgnored()) {
            return false;
        } else {
            return trackedSpawner.isHidden() && !trackedSpawner.isConfirmed() ? false : this.isCategoryShown(trackedSpawner.getCategory());
        }
    }

    private boolean isCategoryShown(SpawnerCategory spawnerCategory) {
        return spawnerCategory == SpawnerCategory.EMPTY || this.show.has(spawnerCategory.getKey());
    }

    private int getCategoryColor(SpawnerCategory spawnerCategory) {
        switch (spawnerCategory) {
            case HOSTILE:
                return this.hostileColor.getValue();
            case PASSIVE:
                return this.passiveColor.getValue();
            case EMPTY:
                return this.emptyColor.getValue();
            case DAMAGER:
                return this.damagerColor.getValue();
            default:
                throw new MatchException(null, null);
        }
    }

    private int getStorageColor(StorageType storageType) {
        switch (storageType) {
            case CHEST:
                return this.chestColor.getValue();
            case TRAPPED:
                return this.trappedColor.getValue();
            case SHULKER:
                return this.shulkerColor.getValue();
            case ENDER:
                return this.enderColor.getValue();
            case OTHER:
                return this.otherColor.getValue();
            default:
                throw new MatchException(null, null);
        }
    }

    private float getStorageFadeAlpha(Vec3d vec3d, StorageEntry storageEntry) {
        double d = this.storageFade.getValue().floatValue();
        if (d <= 0.0) {
            return 1.0F;
        }

        double d2 = d * d;
        double d3 = vec3d.squaredDistanceTo(storageEntry.getBox().getCenter());
        return d3 >= d2 ? 1.0F : (float)(d3 / d2);
    }

    private void renderBoxes(MatrixStack matrixStack, Vec3d vec3d) {
        long l = System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();

        for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
            if (this.isSpawnerVisible(trackedSpawner) && ProjectionUtil.isVisible(trackedSpawner.getBox())) {
                (trackedSpawner.isHidden() ? arrayList2 : arrayList).add(new FarmBase.BoxEntry(trackedSpawner.getBox(), this.getCategoryColor(trackedSpawner.getCategory())));
            }
        }

        if (this.show.has("damager")) {
            for (DamagerBlock damagerBlock : this.damagerDetector.getDamagers()) {
                if (ProjectionUtil.isVisible(damagerBlock.getBox())) {
                    (damagerBlock.isActive(l) ? arrayList3 : arrayList).add(new FarmBase.BoxEntry(damagerBlock.getBox(), this.damagerColor.getValue()));
                }
            }
        }

        if (this.storage.isEnabled()) {
            this.collectStorageBoxes(vec3d, arrayList, arrayList2);
        }

        this.renderBoxes(matrixStack, vec3d, arrayList, 1.0F);
        this.renderBoxes(matrixStack, vec3d, arrayList2, 0.4F);
        this.renderBoxes(matrixStack, vec3d, arrayList3, 0.7F + 0.3F * (float)Math.sin(l / 150.0));
        this.renderSpawnZones(matrixStack, vec3d);
    }

    private void collectStorageBoxes(Vec3d vec3d, List<FarmBase.BoxEntry> boxEntries, List<FarmBase.BoxEntry> boxEntries2) {
        boolean flag = this.storageHideOpened.getValue();

        for (StorageEntry storageEntry : this.storageTracker.getEntries()) {
            boolean flag2 = this.storageTracker.isOpened(storageEntry.getPos());
            if (!flag2 || !flag) {
                float f = this.getStorageFadeAlpha(vec3d, storageEntry);
                if (!(f <= 0.075F) && ProjectionUtil.isVisible(storageEntry.getBox())) {
                    (!flag2 && !storageEntry.isPacketOnly() && !(f < 0.7F) ? boxEntries : boxEntries2)
                        .add(new FarmBase.BoxEntry(storageEntry.getBox(), this.getStorageColor(storageEntry.getType())));
                }
            }
        }
    }

    private void renderBoxes(MatrixStack matrixStack, Vec3d vec3d, List<FarmBase.BoxEntry> boxEntries, float f) {
        if (!boxEntries.isEmpty()) {
            try (BoxEspMaskRenderer.Batch batch = this.boxRenderer.begin(ColorUtil.multiplyAlpha(-1, f))) {
                for (FarmBase.BoxEntry boxEntry : boxEntries) {
                    Box box2 = boxEntry.box();
                    batch.addBox(
                        matrixStack,
                        (float)(box2.minX - vec3d.x),
                        (float)(box2.minY - vec3d.y),
                        (float)(box2.minZ - vec3d.z),
                        (float)(box2.maxX - vec3d.x),
                        (float)(box2.maxY - vec3d.y),
                        (float)(box2.maxZ - vec3d.z),
                        boxEntry.color()
                    );
                }

                batch.finish();
            }
        }
    }

    private void renderSpawnZones(MatrixStack matrixStack, Vec3d vec3d) {
        if (this.spots.isEnabled() && this.spotsZone.getValue() && !this.spotsArea.is("player")) {
            try (Render3DUtil.BoxBatch boxBatch = Render3DUtil.glowBoxBatch()) {
                for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
                    Box box = trackedSpawner.getSpawnZone();
                    if (this.isSpawnerVisible(trackedSpawner) && ProjectionUtil.isVisible(box)) {
                        boxBatch.outline(
                            matrixStack,
                            (float)(box.minX - vec3d.x),
                            (float)(box.minY - vec3d.y),
                            (float)(box.minZ - vec3d.z),
                            (float)(box.maxX - vec3d.x),
                            (float)(box.maxY - vec3d.y),
                            (float)(box.maxZ - vec3d.z),
                            ColorUtil.multiplyAlpha(this.getCategoryColor(trackedSpawner.getCategory()), 0.45F)
                        );
                    }
                }

                boxBatch.draw();
            }
        }
    }

    private void renderSpawnableSpots(MatrixStack matrixStack) {
        List<SpawnableBlockScanner.SpawnQuad> spawnQuads = this.spawnableBlockScanner.getQuads();
        if (!spawnQuads.isEmpty()) {
            int i = this.alwaysSpotColor.getValue();
            int i2 = this.potentialSpotColor.getValue();

            try (Render3DUtil.LineBatch lineBatch2 = Render3DUtil.lineBatch()) {
                for (SpawnableBlockScanner.SpawnQuad spawnQuad : spawnQuads) {
                    Vec3d vec3d = spawnQuad.northWest();
                    if (ProjectionUtil.isVisible(vec3d.x + 0.5, vec3d.y, vec3d.z + 0.5)) {
                        int i3 = spawnQuad.potential() ? i2 : i;
                        lineBatch2.line(matrixStack, vec3d, spawnQuad.southEast(), i3, i3, 1.5F);
                        lineBatch2.line(matrixStack, spawnQuad.northEast(), spawnQuad.southWest(), i3, i3, 1.5F);
                    }
                }

                lineBatch2.draw();
            }
        }
    }

    private void renderTracers(MatrixStack matrixStack, Vec3d vec3d, float f) {
        boolean flag = this.tracers.isEnabled();
        boolean flag2;
        if (this.storage.isEnabled() && this.storageTracers.getValue()) {
            flag2 = true;
        } else {
            flag2 = false;
            if (!flag) {
                return;
            }
        }

        Vec3d vec3d2 = ProjectionUtil.getPointInFront(1.0);
        float f2 = this.tracerWidth.getValue();

        try (
            Render3DUtil.LineBatch lineBatch2 = Render3DUtil.glowLineBatch();
            Render3DUtil.LineBatch lineBatch3 = Render3DUtil.lineBatch();
        ) {
            if (flag) {
                this.renderMobTracers(lineBatch2, lineBatch3, matrixStack, vec3d, vec3d2, f2, f);
            }

            if (flag2) {
                this.renderStorageTracers(lineBatch2, lineBatch3, matrixStack, vec3d, vec3d2, f2);
            }

            lineBatch2.draw();
            lineBatch3.draw();
        }
    }

    private void renderMobTracers(Render3DUtil.LineBatch lineBatch, Render3DUtil.LineBatch lineBatch2, MatrixStack matrixStack, Vec3d vec3d, Vec3d vec3d2, float f, float f2) {
        double d = this.tracerRange.getValue().floatValue();
        double d2 = d * d;
        int i = (int)(this.tracerFresh.getValue() * 20.0F);
        boolean flag = this.tracerTargets.is("farm");
        ArrayList arrayList = new ArrayList();

        for (Entity entity : TargetComponent.ENTITIES) {
            if (entity instanceof MobEntity mobEntity && mobEntity.isAlive()) {
                SpawnerCategory spawnerCategory = SpawnerCategory.fromEntityType(mobEntity.getType());
                if (this.show.has(spawnerCategory.getKey())) {
                    Vec3d vec3d3 = ProjectionUtil.getInterpolatedPos(mobEntity, f2);
                    Vec3d vec3d4 = vec3d3.add(0.0, mobEntity.getHeight() / 2.0, 0.0);
                    double d3 = vec3d.squaredDistanceTo(vec3d4);
                    if (!(d3 > d2) && (!flag || this.isNearFarm(vec3d3))) {
                        Integer integer = this.freshSpawnTicks.get(mobEntity.getId());
                        boolean flag2 = integer != null && this.ticks - integer <= i;
                        float f3 = 1.0F - 0.6F * (float)(Math.sqrt(d3) / d);
                        float f4 = flag2 ? 1.0F : f3 * 0.75F;
                        arrayList.add(
                            new FarmBase.TracerEntry(vec3d4, ColorUtil.multiplyAlpha(this.getCategoryColor(spawnerCategory), f4), flag2 ? f * 1.35F : f, flag2, d3)
                        );
                    }
                }
            }
        }

        drawTracers(lineBatch, lineBatch2, matrixStack, vec3d2, arrayList, 48);
    }

    private void renderStorageTracers(Render3DUtil.LineBatch lineBatch, Render3DUtil.LineBatch lineBatch2, MatrixStack matrixStack, Vec3d vec3d, Vec3d vec3d2, float f) {
        boolean flag = this.storageHideOpened.getValue();
        ArrayList arrayList = new ArrayList();

        for (StorageEntry storageEntry : this.storageTracker.getEntries()) {
            boolean flag2 = this.storageTracker.isOpened(storageEntry.getPos());
            if (!flag2 || !flag) {
                float f2 = this.getStorageFadeAlpha(vec3d, storageEntry);
                if (!(f2 <= 0.075F)) {
                    if (flag2) {
                        f2 *= 0.5F;
                    }

                    Vec3d vec3d3 = storageEntry.getBox().getCenter();
                    arrayList.add(
                        new FarmBase.TracerEntry(
                            vec3d3, ColorUtil.multiplyAlpha(this.getStorageColor(storageEntry.getType()), f2), f, false, vec3d.squaredDistanceTo(vec3d3)
                        )
                    );
                }
            }
        }

        drawTracers(lineBatch, lineBatch2, matrixStack, vec3d2, arrayList, 64);
    }

    private static void drawTracers(
        Render3DUtil.LineBatch lineBatch, Render3DUtil.LineBatch lineBatch2, MatrixStack matrixStack, Vec3d vec3d, List<FarmBase.TracerEntry> tracerEntries, int i
    ) {
        tracerEntries.sort(Comparator.comparingDouble(FarmBase.TracerEntry::distanceSq));
        int i2 = tracerEntries.size();
        int i3 = Math.min(i2, i);

        for (int i4 = 0; i4 < i3; i4++) {
            FarmBase.TracerEntry tracerEntry = (FarmBase.TracerEntry)tracerEntries.get(i4);
            drawTracer(lineBatch, lineBatch2, matrixStack, vec3d, tracerEntry.target(), tracerEntry.color(), tracerEntry.width(), tracerEntry.fresh());
        }
    }

    private static void drawTracer(
        Render3DUtil.LineBatch lineBatch, Render3DUtil.LineBatch lineBatch2, MatrixStack matrixStack, Vec3d vec3d, Vec3d vec3d2, int i, float f, boolean flag
    ) {
        double d = vec3d.distanceTo(vec3d2);
        if (!(d < 0.5)) {
            Vec3d vec3d3 = vec3d.lerp(vec3d2, Math.min(0.5, 2.5 / d));
            Vec3d vec3d4 = vec3d.lerp(vec3d2, Math.min(0.7, 4.0 / d));
            int i2 = ColorUtil.multiplyAlpha(i, 0.0F);
            int i3 = ColorUtil.multiplyAlpha(i, flag ? 0.35F : 0.18F);
            lineBatch.line(matrixStack, vec3d3, vec3d4, i2, i3, f * 3.0F);
            lineBatch.line(matrixStack, vec3d4, vec3d2, i3, i3, f * 3.0F);
            lineBatch2.line(matrixStack, vec3d, vec3d3, i2, i, f);
            lineBatch2.line(matrixStack, vec3d3, vec3d2, i, i, f);
        }
    }

    private boolean isNearFarm(Vec3d vec3d) {
        for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
            if (this.isSpawnerVisible(trackedSpawner) && trackedSpawner.getBox().getCenter().squaredDistanceTo(vec3d) <= 1024.0) {
                return true;
            }
        }

        if (!this.show.has("damager")) {
            return false;
        }

        for (DamagerBlock damagerBlock : this.damagerDetector.getDamagers()) {
            if (damagerBlock.getBox().getCenter().squaredDistanceTo(vec3d) <= 1024.0) {
                return true;
            }
        }

        return false;
    }

    private void renderLabels(DrawContext drawContext) {
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null) {
            Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();

            for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
                if (this.isSpawnerVisible(trackedSpawner)) {
                    Icon icon = trackedSpawner.isHidden() ? Icon.GHOST : trackedSpawner.getCategory().getIcon();
                    this.drawLabel(drawContext, msdfFont, vec3d, trackedSpawner.getPos(), trackedSpawner.getCategory(), icon, trackedSpawner.getDisplayName());
                }
            }

            if (this.show.has("damager")) {
                String string = Language.translate("farmbase.damager", "Блок-дамагер");

                for (DamagerBlock damagerBlock : this.damagerDetector.getDamagers()) {
                    this.drawLabel(drawContext, msdfFont, vec3d, damagerBlock.getPos(), SpawnerCategory.DAMAGER, SpawnerCategory.DAMAGER.getIcon(), string);
                }
            }
        }
    }

    private void drawLabel(DrawContext drawContext, MsdfFont msdfFont, Vec3d vec3d, BlockPos blockPos, SpawnerCategory spawnerCategory, Icon icon2, String string) {
        double d = blockPos.getX() + 0.5;
        double d2 = blockPos.getY() + 0.5;
        double d3 = blockPos.getZ() + 0.5;
        double d4 = Math.sqrt(vec3d.squaredDistanceTo(d, d2, d3));
        float f = MathHelper.clamp((float)(32.0 - d4) / 8.0F, 0.0F, 1.0F);
        if (!(f <= 0.01F) && ProjectionUtil.isVisible(d, d2, d3)) {
            Vector2f vector2f = ProjectionUtil.worldToScreen(d, d2, d3);
            if (vector2f != null) {
                float f2 = 6.0F + 3.0F;
                float f3 = 2.5F + f2 + 3.0F + msdfFont.getWidth(string, 6.0F) + 2.5F + 1.0F;
                float f4 = f2 + 2.5F * 2.0F;
                float f5 = MathUtil.roundToStep(vector2f.x - f3 / 2.0F, 0.5F);
                float f6 = MathUtil.roundToStep(vector2f.y - f4 / 2.0F, 0.5F);
                GuiRenderUtil.drawPanel(drawContext, f5, f6, f3, f4, f, f4 / 2.0F, false);
                new RenderUtil(drawContext, f5 + 2.5F, f6 + 2.5F, f2, f2)
                    .rect(ColorUtil.multiplyAlpha(this.getCategoryColor(spawnerCategory), f), Radius.all(f2 / 2.0F))
                    .icon(icon2, f2 * 0.22F, f2 * 0.22F, f2 * 0.56F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, f));
                new RenderUtil(drawContext, f5, f6, f3, f4)
                    .textTopLeft(msdfFont, string, 2.5F + f2 + 3.0F, (f4 - 6.0F) / 2.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 6.0F);
            }
        }
    }

    @Override
    public String getTitle() {
        return Language.translate("farmbase.panel.title", "Фермы");
    }

    @Override
    public Icon getIcon() {
        return Icon.DUNGEON;
    }

    @Override
    public boolean showHeader() {
        return this.panel.getValue() && super.showHeader();
    }

    @Override
    public boolean isInverted() {
        return false;
    }

    @Override
    public boolean isRightAligned() {
        return false;
    }

    @Override
    public void collectEntries(List<FarmBase.CounterEntry> counterEntries2) {
        if (this.panelUpdateTimer.hasPassedAndReset(250.0)) {
            this.updateCounterEntries();
        }

        for (FarmBase.CounterEntry counterEntry : this.counterEntries.values()) {
            if (counterEntry.active || counterEntry.animation.get() > 0.001F) {
                counterEntries2.add(counterEntry);
            }
        }

        this.counterEntries.values().removeIf(counterEntry2 -> !counterEntry2.active && counterEntry2.animation.get() <= 0.001F && counterEntry2.animation.isFinished());
    }

    @Override
    public void sortEntries(List<FarmBase.CounterEntry> counterEntries2) {
        counterEntries2.sort(
            Comparator.<FarmBase.CounterEntry>comparingInt(counterEntry -> counterEntry.order)
                .thenComparing(counterEntry2 -> !counterEntry2.header ? true : false)
                .thenComparingInt(counterEntry3 -> counterEntry3.index2)
        );
    }

    private void updateCounterEntries() {
        Iterator iterator2 = this.counterEntries.values().iterator();

        while (iterator2.hasNext()) {
            ((FarmBase.CounterEntry)iterator2.next()).active = false;
        }

        if (this.panel.getValue() && !ServerManager.isNullWorld()) {
            Vec3d vec3d = mc.player.getEntityPos();
            int i = 0;

            for (SpawnerCategory spawnerCategory : SpawnerCategory.values()) {
                if (spawnerCategory != SpawnerCategory.DAMAGER && this.isCategoryShown(spawnerCategory)) {
                    TreeMap<String, FarmBase.GroupStats> treeMap = new TreeMap<>();
                    int i2 = 0;

                    for (TrackedSpawner trackedSpawner : this.spawnerTracker.getSpawners()) {
                        if (trackedSpawner.getCategory() == spawnerCategory && this.isSpawnerVisible(trackedSpawner)) {
                            FarmBase.GroupStats groupStats = treeMap.computeIfAbsent(trackedSpawner.getDisplayName(), string -> new FarmBase.GroupStats());
                            groupStats.count++;
                            if (trackedSpawner.isHidden()) {
                                groupStats.hiddenCount++;
                            }

                            groupStats.nearestDistanceSq = Math.min(groupStats.nearestDistanceSq, vec3d.squaredDistanceTo(trackedSpawner.getBox().getCenter()));
                            i2++;
                        }
                    }

                    if (!treeMap.isEmpty()) {
                        this.putEntry(
                            "kind:" + spawnerCategory.getKey(),
                            () -> FarmBase.CounterEntry.header(spawnerCategory.ordinal(), spawnerCategory.getIcon(), () -> this.getCategoryColor(spawnerCategory)),
                            Language.translate("farmbase.show." + spawnerCategory.getKey()),
                            String.valueOf(i2),
                            "",
                            i++
                        );

                        for (Entry<String, FarmBase.GroupStats> entry : treeMap.entrySet()) {
                            FarmBase.GroupStats groupStats2 = entry.getValue();
                            String string2 = " · " + formatDistance(groupStats2.nearestDistanceSq);
                            if (groupStats2.hiddenCount > 0) {
                                string2 = string2 + " · " + String.format(Language.translate("farmbase.hidden", "скрытых %d"), groupStats2.hiddenCount);
                            }

                            this.putEntry(
                                spawnerCategory.getKey() + ":" + entry.getKey(),
                                () -> FarmBase.CounterEntry.row(spawnerCategory.ordinal(), spawnerCategory.getIcon(), () -> this.getCategoryColor(spawnerCategory)),
                                entry.getKey(),
                                "×" + groupStats2.count,
                                string2,
                                i++
                            );
                        }
                    }
                }
            }

            int i3 = this.addDamagerEntries(vec3d, i);
            this.addStorageEntries(vec3d, i3);
        }
    }

    private int addDamagerEntries(Vec3d vec3d, int i) {
        if (this.show.has("damager") && !this.damagerDetector.getDamagers().isEmpty()) {
            long l = System.currentTimeMillis();
            this.putEntry(
                "kind:damager",
                () -> FarmBase.CounterEntry.header(
                    SpawnerCategory.DAMAGER.ordinal(), SpawnerCategory.DAMAGER.getIcon(), () -> this.getCategoryColor(SpawnerCategory.DAMAGER)
                ),
                Language.translate("farmbase.show.damager"),
                String.valueOf(this.damagerDetector.getDamagers().size()),
                "",
                i++
            );
            List<DamagerBlock> damagerBlocks = new ArrayList<>(this.damagerDetector.getDamagers());
            damagerBlocks.sort(Comparator.comparingDouble(damagerBlock -> vec3d.squaredDistanceTo(damagerBlock.getBox().getCenter())));
            int i2 = damagerBlocks.size();
            int i3 = Math.min(i2, 5);

            for (int i4 = 0; i4 < i3; i4++) {
                DamagerBlock damagerBlock2 = damagerBlocks.get(i4);
                BlockPos blockPos = damagerBlock2.getPos();
                boolean flag = damagerBlock2.isActive(l);
                String string = flag ? Language.translate("farmbase.damager.active", "бьёт") : Language.translate("farmbase.damager.idle", "простой");
                String string2 = String.format(Locale.US, "%d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ());
                String string3 = " · " + formatDistance(vec3d.squaredDistanceTo(damagerBlock2.getBox().getCenter()));
                this.putEntry(
                        "damager:" + blockPos.asLong(),
                        () -> FarmBase.CounterEntry.row(
                            SpawnerCategory.DAMAGER.ordinal(), SpawnerCategory.DAMAGER.getIcon(), () -> this.getCategoryColor(SpawnerCategory.DAMAGER)
                        ),
                        string2,
                        string,
                        string3,
                        i++
                    )
                    .highlighted = flag;
            }
        }

        return i;
    }

    private void addStorageEntries(Vec3d vec3d, int i) {
        if (this.storage.isEnabled() && !this.storageTracker.getEntries().isEmpty()) {
            EnumMap<StorageType, FarmBase.GroupStats> enumMap = new EnumMap<>(StorageType.class);
            int i2 = 0;

            for (StorageEntry storageEntry : this.storageTracker.getEntries()) {
                FarmBase.GroupStats groupStats = enumMap.computeIfAbsent(storageEntry.getType(), storageType -> new FarmBase.GroupStats());
                groupStats.count++;
                if (this.storageTracker.isOpened(storageEntry.getPos())) {
                    groupStats.hiddenCount++;
                }

                groupStats.nearestDistanceSq = Math.min(groupStats.nearestDistanceSq, vec3d.squaredDistanceTo(storageEntry.getBox().getCenter()));
                i2++;
            }

            this.putEntry(
                "storage",
                () -> FarmBase.CounterEntry.header(SpawnerCategory.values().length, Icon.BOXES, this.chestColor::getValue),
                Language.translate("farmbase.storage", "Хранилища"),
                String.valueOf(i2),
                "",
                i++
            );

            for (Entry<StorageType, FarmBase.GroupStats> entry : enumMap.entrySet()) {
                StorageType storageType2 = entry.getKey();
                FarmBase.GroupStats groupStats2 = entry.getValue();
                String string = " · " + formatDistance(groupStats2.nearestDistanceSq);
                if (groupStats2.hiddenCount > 0) {
                    string = string + " · " + String.format(Language.translate("farmbase.storage.opened", "открыто %d"), groupStats2.hiddenCount);
                }

                this.putEntry(
                    "storage:" + storageType2.getKey(),
                    () -> FarmBase.CounterEntry.row(SpawnerCategory.values().length, storageType2.getIcon(), () -> this.getStorageColor(storageType2)),
                    Language.translate("farmbase.storage.show." + storageType2.getKey()),
                    "×" + groupStats2.count,
                    string,
                    i++
                );
            }
        }
    }

    private FarmBase.CounterEntry putEntry(String string, Supplier<FarmBase.CounterEntry> supplier, String string2, String string3, String string4, int i) {
        FarmBase.CounterEntry counterEntry = this.counterEntries.computeIfAbsent(string, string5 -> (FarmBase.CounterEntry)supplier.get());
        counterEntry.label = string2;
        counterEntry.value = string3;
        counterEntry.suffix = string4;
        counterEntry.index2 = i;
        counterEntry.active = true;
        return counterEntry;
    }

    private static String formatDistance(double d) {
        double d2 = Math.sqrt(d);
        return d2 >= 1000.0
            ? String.format(Locale.US, "%.1f", d2 / 1000.0) + Language.translate("gps.unit.kilometers", "km")
            : Math.round(d2) + Language.translate("gps.unit.meters", "m");
    }

    @Override
    protected float getEntryWidth(FarmBase.CounterEntry counterEntry) {
        return 7.0F;
    }

    @Override
    protected float getEntryValueWidth(FarmBase.CounterEntry counterEntry) {
        return BODY_FONT.getWidth(counterEntry.value, 6.0F);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, FarmBase.CounterEntry counterEntry) {
        float f5 = counterEntry.header ? f4 : f4 * 0.6F;
        new RenderUtil(drawContext, f, f2 + (f3 - 7.0F) / 2.0F, 7.0F, 7.0F)
            .icon(counterEntry.icon, 0.0F, 0.0F, 7.0F, this.applyAlpha(counterEntry.colorSupplier.getAsInt(), f5));
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, FarmBase.CounterEntry counterEntry) {
        int i = counterEntry.highlighted ? counterEntry.colorSupplier.getAsInt() : Theme.getTextColor();
        new RenderUtil(drawContext, f, f2, BODY_FONT.getWidth(counterEntry.value, 6.0F), f3).textMiddleLeft(BODY_FONT, counterEntry.value, 0.0F, 0.0F, this.applyAlpha(i, f4), 6.0F);
    }

    @Override
    protected String getEntryText(FarmBase.CounterEntry counterEntry) {
        return counterEntry.label;
    }

    @Override
    protected String getEntrySuffix(FarmBase.CounterEntry counterEntry) {
        return counterEntry.suffix;
    }

    @Override
    protected int getEntryColor(FarmBase.CounterEntry counterEntry, float f) {
        return counterEntry.header ? this.applyAlpha(Theme.getSecondaryText(), f) : this.applyAlpha(Theme.getTextColor(), f);
    }

    record BoxEntry(Box box, int color) {
    }

    static final class CounterEntry extends HudEntry {
        final int order;
        final Icon icon;
        final IntSupplier colorSupplier;
        final boolean header;
        String label = "";
        String value = "";
        String suffix = "";
        boolean highlighted;
        int index2;

        private CounterEntry(int i, Icon icon2, IntSupplier intSupplier, boolean flag) {
            this.order = i;
            this.icon = icon2;
            this.colorSupplier = intSupplier;
            this.header = flag;
        }

        static FarmBase.CounterEntry header(int i, Icon icon2, IntSupplier intSupplier) {
            return new FarmBase.CounterEntry(i, icon2, intSupplier, true);
        }

        static FarmBase.CounterEntry row(int i, Icon icon2, IntSupplier intSupplier) {
            return new FarmBase.CounterEntry(i, icon2, intSupplier, false);
        }
    }

    static final class GroupStats {
        int count;
        int hiddenCount;
        double nearestDistanceSq = Double.MAX_VALUE;
    }

    record TracerEntry(Vec3d target, int color, float width, boolean fresh, double distanceSq) {
    }
}
