package tech.sadnes.client.feature.impl.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.process.IBuilderProcess;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.RotatedSchematic;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
import baritone.api.utils.interfaces.IGoalRenderPos;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.MenuTickEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.misc.autobuild.BaritoneBuilderControl;
import tech.sadnes.client.feature.impl.misc.autobuild.BuildBlockUtil;
import tech.sadnes.client.feature.impl.misc.autobuild.BuildJob;
import tech.sadnes.client.feature.impl.misc.autobuild.BuildJobQueue;
import tech.sadnes.client.feature.impl.misc.autobuild.BuildProgress;
import tech.sadnes.client.feature.impl.misc.autobuild.FilteredSchematic;
import tech.sadnes.client.feature.impl.misc.autobuild.LitematicaIntegration;
import tech.sadnes.client.feature.impl.misc.autobuild.MaterialShopper;
import tech.sadnes.client.feature.impl.misc.autobuild.SchematicFileManager;
import tech.sadnes.client.feature.impl.misc.autobuild.SchematicPreviewRenderer;
import tech.sadnes.client.feature.impl.misc.autobuild.StallDetector;
import tech.sadnes.client.feature.impl.movement.Baritone;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ButtonSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.ChatMessageUtil;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.AutoReconnector;
import tech.sadnes.client.util.network.LastServerInfo;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.ServerInputTracker;
import tech.sadnes.client.util.player.ServerRotationTracker;

@FeatureInfo(
    name = "AutoBuild",
    description = "feature.impl.misc.autobuild.autobuild",
    displayName = "feature.impl.misc.autobuild.autobuild",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public final class AutoBuild extends HudFeature<AutoBuild.MaterialEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBuild.class);
    private final ModeSetting source = new ModeSetting("autobuild.source", new String[]{"file", "litematica"});
    private final ModeSetting file = new ModeSetting("autobuild.file", new String[]{"none"}).setVisibleWhen(() -> this.source.is("file")).setOnChange(this::onFileChange);
    private final TextSetting selected = (TextSetting)new TextSetting("autobuild.selected", "").setHidden(true);
    private final ModeSetting rotation = new ModeSetting("autobuild.rotation", new String[]{"0", "90", "180", "270"}).setVisibleWhen(() -> this.source.is("file"));
    private final Setting<Runnable> add = new ButtonSetting("autobuild.add", "autobuild.add.button", this::addJob);
    private final Setting<Runnable> folder = new ButtonSetting("autobuild.folder", "autobuild.folder.button", this::openFolder);
    private final Setting<Runnable> clearQueue = new ButtonSetting("autobuild.clearQueue", "autobuild.clearQueue.button", this::clearJobs);
    private final ModeSetting directional = new ModeSetting("autobuild.directional", new String[]{"skip", "any", "exact"}).setOnChange(this::onDirectionalChange);
    private final BooleanSetting replace = new BooleanSetting("autobuild.replace", false).setOnChange(this::resetJobFlags);
    private final BooleanSetting clear = new BooleanSetting("autobuild.clear", false).setOnChange(this::resetJobFlags);
    private final BooleanSetting inventory = new BooleanSetting("autobuild.inventory", true);
    private final NumberSetting speed = new NumberSetting("autobuild.speed", 2.5F, 1.0F, 5.0F, 0.5F);
    private final BooleanSetting walk = new BooleanSetting("autobuild.walk", true);
    private final NumberSetting reach = new NumberSetting("autobuild.reach", 3.0F, 2.5F, 4.5F, 0.5F, Unit.METER);
    private final BooleanSetting headTurn = new BooleanSetting("autobuild.headTurn", true);
    private final GroupSetting rejoin = new GroupSetting("autobuild.rejoin", true);
    private final NumberSetting rejoinEvery = new NumberSetting(this.rejoin, "autobuild.rejoinEvery", 40.0F, 20.0F, 600.0F, 5.0F, Unit.SECOND);
    private final GroupSetting autoBuy = new GroupSetting("autobuild.autoBuy", false);
    private final TextSetting maxPrice = new TextSetting(this.autoBuy, "autobuild.maxPrice", "", "autobuild.maxPrice.hint");
    private final NumberSetting overpay = new NumberSetting(this.autoBuy, "autobuild.overpay", 50.0F, 0.0F, 300.0F, 10.0F, Unit.PERCENT);
    private final GroupSetting preview = new GroupSetting("autobuild.preview", true);
    private final BooleanSetting markers = new BooleanSetting(this.preview, "autobuild.markers", true);
    private final ColorSetting color = new ColorSetting(this.preview, "autobuild.color", ColorUtil.rgb(90, 160, 255));
    private final BooleanSetting ownsBaritone = (BooleanSetting)new BooleanSetting("autobuild.ownsBaritone", false).setHidden(true);
    private final SchematicFileManager schematicFiles = new SchematicFileManager();
    private final BuildJobQueue jobQueue = new BuildJobQueue();
    private final LitematicaIntegration litematica = new LitematicaIntegration();
    private final BaritoneBuilderControl builderControl = new BaritoneBuilderControl(() -> this.activeJob != null);
    private final SchematicPreviewRenderer previewRenderer = new SchematicPreviewRenderer(this.schematicFiles);
    private final StallDetector stallDetector = new StallDetector();
    private final MaterialShopper shopper = new MaterialShopper();
    private final AutoReconnector rejoinHandler = new AutoReconnector();
    private final StopWatch hurtTimer = new StopWatch();
    private final Map<Item, AutoBuild.MaterialEntry> materialEntries = new HashMap<>();
    private final StopWatch entryRefreshTimer = new StopWatch();
    private List<String> knownFiles = List.of();
    private boolean updatingFileModes;
    private BuildJob activeJob;
    private String currentServer;
    private String currentDimension;
    private boolean builderConfigured;
    private boolean paused;
    private boolean rescanRequested;
    private boolean jobFinished;
    private boolean farPlacementWarned;
    private boolean shopCheckedAfterScan;
    private boolean shopUnsupportedWarned;
    private int tickCounter;
    private int idleTicks;
    private int unknownWorldTicks;
    private int anarchyNumber = -1;
    private int noProgressRetries;
    private int lastPlacedCount;
    private int initialPlacementVersion;
    private int stubbornCount;
    private final StopWatch stubbornReportTimer = new StopWatch();
    private int unbreakableCount;
    private int lastLookTick;
    private Rotation targetRotation;
    private BuildJob lastStartedJob;
    private int buildingTicks;
    private int lastStallProgress = -1;
    private boolean rejoinedOnStall;
    private Goal[] placementGoals = new Goal[0];
    private int goalsTick;
    private int strandedCount;
    private int lastStrandedProgress;

    public AutoBuild() {
        super(10.0F, 200.0F);
        this.style.setHidden(true);
        this.rightAligned.setHidden(true);
    }

    public static AutoBuild getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoBuild.class);
    }

    public static void importFiles(List<Path> paths) {
        boolean flag = Sadnes.getInstance().getFeatureManager() != null;
        AutoBuild autoBuild = flag ? getInstance() : null;
        if (autoBuild != null && paths != null && !paths.isEmpty()) {
            autoBuild.importPaths(paths);
        }
    }

    public static boolean handleLook(float f, float f2) {
        return getInstance().lookAt(f, f2);
    }

    public static boolean isLookDiverged() {
        return getInstance().isLookOff();
    }

    public static void setPlacementGoals(Goal[] goals) {
        getInstance().storePlacementGoals(goals);
    }

    public static BlockState getSchematicBlockAt(int i, int i2, int i3) {
        BuildJob buildJob = getInstance().activeJob;
        FilteredSchematic filteredSchematic = buildJob == null ? null : buildJob.getSchematic();
        if (filteredSchematic == null) {
            return null;
        }

        int i4 = i - buildJob.getOrigin().getX();
        int i5 = i2 - buildJob.getOrigin().getY();
        int i6 = i3 - buildJob.getOrigin().getZ();
        BlockState blockState = filteredSchematic.getTargetState(i4, i5, i6);
        boolean flag;
        if (blockState != null && !blockState.isAir()) {
            flag = filteredSchematic.shouldBuild(i4, i5, i6, blockState);
        } else {
            flag = false;
        }

        boolean flag2;
        if (flag && BuildBlockUtil.getKind(blockState) == BuildBlockUtil.BlockKind.BLOCK && blockState.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
            flag2 = !(blockState.getBlock() instanceof FallingBlock);
        } else {
            flag2 = false;
        }

        if (filteredSchematic.isClearMode() || flag2 && filteredSchematic.isPostponed(BlockPos.asLong(i4, i5, i6))) {
            return flag ? blockState : null;
        } else {
            return null;
        }
    }

    public static BlockRotation parseRotation(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case 1815:
                if (string.equals("90")) {
                    return BlockRotation.CLOCKWISE_90;
                }
                break;
            case 48873:
                if (string.equals("180")) {
                    return BlockRotation.CLOCKWISE_180;
                }
                break;
            case 49803:
                if (string.equals("270")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                return BlockRotation.CLOCKWISE_90;
            case 1:
                return BlockRotation.CLOCKWISE_180;
            case 2:
                return BlockRotation.COUNTERCLOCKWISE_90;
            default:
                return BlockRotation.NONE;
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        this.jobQueue.load();
        this.refreshFileList();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.paused = this.jobFinished = this.shopUnsupportedWarned = false;
        this.activeJob = null;
        this.tickCounter = this.unknownWorldTicks = 0;
        this.anarchyNumber = -1;
        this.resetHelpers();
        this.refreshFileList();
    }

    @Override
    public void onEnable() {
        this.stopActiveJob();
        this.releaseBuilder();
        this.rejoinHandler.cancel();
        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        IBaritone iBaritone = this.getBaritone();
        if (this.activeJob != null && iBaritone != null && iBaritone.getBuilderProcess().isActive()) {
            iBaritone.getPathingBehavior().cancelEverything();
        }

        for (BuildJob buildJob : this.jobQueue.getJobs()) {
            buildJob.resetFlags();
            if (buildJob.getSource() == BuildJob.Source.LITEMATICA) {
                buildJob.reset();
            }
        }

        this.activeJob = null;
        this.paused = false;
        this.anarchyNumber = -1;
        this.unknownWorldTicks = this.buildingTicks = 0;
        this.resetHelpers();
        boolean flag;
        if (this.rejoinHandler.consumeReason() != null) {
            flag = this.rejoinedOnStall;
        } else {
            flag = false;
        }

        this.rejoinedOnStall = false;
        if (flag) {
            this.printChat("autobuild.chat.rejoinedOnStall");
        }
    }

    @EventHandler
    public void onServerJoin(MenuTickEvent menuTickEvent) {
        this.rejoinHandler.tick();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && this.shopper.isActive()) {
            if (packetEvent.getPacket() instanceof ShowDialogS2CPacket showDialogS2CPacket) {
                mc.execute(() -> this.shopper.onDialog(showDialogS2CPacket.dialog().value()));
            }

            Packet packet = packetEvent.getPacket();
            if (packet instanceof GameMessageS2CPacket && !((GameMessageS2CPacket)packet).overlay()) {
                String string = ChatMessageUtil.getPlainText((GameMessageS2CPacket)packetEvent.getPacket());
                mc.execute(() -> this.shopper.onChatMessage(string));
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        IBaritone iBaritone = this.getBaritone();
        Baritone baritone = Baritone.getInstance();
        if (mc.player != null && mc.world != null && iBaritone != null && baritone != null) {
            if (mc.player.hurtTime > 0) {
                this.hurtTimer.reset();
            }

            if (this.tickCounter % 40 == 0) {
                this.refreshFileList();
            }

            if (this.tickCounter++ % 20 == 0) {
                this.updateAnarchyNumber();
            }

            this.currentServer = this.getServerKey();
            this.currentDimension = this.getDimensionKey();
            this.unknownWorldTicks = this.currentServer == null ? this.unknownWorldTicks + 1 : 0;
            boolean flag = this.currentServer == null && this.unknownWorldTicks < 100;
            if (flag) {
                return;
            }

            BuildJob buildJob = this.jobQueue.findJob(this.currentServer, this.currentDimension);
            boolean flag2 = this.activeJob != null && this.activeJob != buildJob;
            if (flag2) {
                this.stopActiveJob();
            }

            if (buildJob == null) {
                this.releaseBuilder();
                this.handleNoJob();
                return;
            }

            this.configureBuilder(baritone);
            switch (buildJob.getState()) {
                case NEW:
                    this.loadJob(buildJob);
                    return;
                case READY:
                    this.tickJob(buildJob, iBaritone.getBuilderProcess());
                    return;
                case FAILED:
                    this.printChat("autobuild.chat.dropped", buildJob.getName(), buildJob.getError());
                    this.jobQueue.remove(buildJob);
                    return;
                case LOADING:
            }
        }
    }

    private void configureBuilder(Baritone baritone) {
        if (!this.builderConfigured) {
            this.builderControl.saveSettings();
            this.builderConfigured = true;
        }

        if (!baritone.isEnabled()) {
            baritone.setEnabled(true);
            this.ownsBaritone.setValue(true);
        }

        baritone.setAllowPlace(true);
        this.builderControl.applyBuildSettings(this.replace.getValue() || this.clear.getValue(), this.getUsableSlots() > 9, this.directional.getValue());
        this.builderControl.applyMovementSettings(this.getPlaceDelay(), this.walk.getValue(), this.reach.getValue());
    }

    private void releaseBuilder() {
        Baritone baritone = Baritone.getInstance();
        if (this.builderConfigured) {
            this.builderControl.restoreSettings();
            baritone.setAllowPlace(false);
            this.builderConfigured = false;
        }

        if (this.ownsBaritone.getValue()) {
            if (baritone.isEnabled()) {
                baritone.setEnabled(false);
            }

            this.ownsBaritone.setValue(false);
        }
    }

    private void handleNoJob() {
        if (this.jobFinished && this.jobQueue.isEmpty()) {
            this.printChat("autobuild.chat.allDone");
            this.setEnabled(false);
        } else {
            boolean flag;
            if (this.preview.isEnabled() && this.source.is("file")) {
                flag = !this.file.is("none");
            } else {
                flag = false;
            }

            if (flag) {
                this.previewRenderer.loadSize(this.file.getValue());
            } else {
                this.previewRenderer.loadSize(null);
            }
        }
    }

    private void loadJob(BuildJob buildJob) {
        buildJob.markLoading();
        if (buildJob.getSource() == BuildJob.Source.FILE) {
            this.schematicFiles.loadSchematic(this.schematicFiles.getSchematicsDir().resolve(buildJob.getFile())).whenComplete((iStaticSchematic, throwable) -> {
                if (throwable != null) {
                    buildJob.markFailed(throwable.getMessage());
                } else {
                    buildJob.markReady(new FilteredSchematic(rotateSchematic(iStaticSchematic, buildJob.getRotation())), buildJob.getOrigin());
                }
            });
        } else {
            LitematicaIntegration.PlacementInfo placementInfo = this.litematica.findPlacement(buildJob.getPlacementId(), buildJob.getPlacement());
            boolean flag;
            if (placementInfo != null) {
                flag = !placementInfo.enabled();
            } else {
                flag = false;
            }

            LitematicaIntegration.PlacementSchematic placementSchematic = placementInfo != null && !flag ? this.litematica.loadSchematic(placementInfo) : null;
            if (placementSchematic == null) {
                String string = !this.litematica.isLitematicaLoaded()
                    ? "autobuild.error.noLitematica"
                    : (this.litematica.isFailed() ? "autobuild.error.litematica" : (flag ? "autobuild.error.placementDisabled" : "autobuild.error.placement"));
                buildJob.markFailed(String.format(Language.translate(string), buildJob.getPlacement()));
            } else {
                buildJob.markReady(new FilteredSchematic(placementSchematic.schematic()), placementSchematic.origin());
                buildJob.setPlacementHandle(placementSchematic.schematic());
                this.jobQueue.save();
            }
        }
    }

    private void tickJob(BuildJob buildJob, IBuilderProcess iBuilderProcess) {
        boolean flag;
        boolean flag2;
        label166: {
            flag = this.directional.is("exact");
            boolean flag3;
            if (!this.directional.is("skip")) {
                if (flag) {
                    flag2 = buildJob.isOrientationProblem();
                    break label166;
                }

                flag3 = false;
            } else {
                flag3 = true;
            }

            flag2 = flag3;
        }

        buildJob.getSchematic().setClearAir(this.clear.getValue()).setKeepExisting(!this.replace.getValue()).setIgnoreDirection(flag2).setIgnoreAxis(flag);
        LitematicaIntegration.LoadedPlacementSchematic loadedPlacementSchematic = buildJob.getPlacementHandle();
        if (loadedPlacementSchematic != null) {
            if (this.tickCounter % 20 == 0 && this.checkPlacementChanged(buildJob, loadedPlacementSchematic)) {
                return;
            }

            if (this.tickCounter % 5 == 0) {
                loadedPlacementSchematic.loadReadyChunks();
            }
        }

        if (this.shopper.isActive()) {
            this.tickShopping(buildJob, iBuilderProcess);
        } else if (!buildJob.isStarted()) {
            boolean flag4 = loadedPlacementSchematic != null && loadedPlacementSchematic.getLoadedChunks() == 0;
            if (flag4) {
                this.warnFarPlacement(buildJob);
            } else {
                this.startBuilder(buildJob, iBuilderProcess);
            }
        } else {
            BuildProgress buildProgress = buildJob.getProgress();
            buildProgress.tick(mc.world, mc.player.getBlockPos());
            this.buildingTicks++;
            if (this.paused) {
                if (iBuilderProcess.isActive() && !iBuilderProcess.isPaused()) {
                    iBuilderProcess.pause();
                }
            } else {
                boolean flag5;
                if (this.buildingTicks >= this.rejoinEvery.getValue() * 20.0F) {
                    flag5 = !this.stallDetector.isMaterialsShort();
                } else {
                    flag5 = false;
                }

                boolean flag6;
                if (this.rejoin.isEnabled() && flag5) {
                    flag6 = this.isRejoinReady();
                } else {
                    flag6 = false;
                }

                if (flag6) {
                    this.leaveServer("autobuild.leave.planned");
                } else {
                    if (!this.shopCheckedAfterScan && buildProgress.isScanned()) {
                        this.shopCheckedAfterScan = true;
                        if (this.tryAutoBuyMaterials(buildProgress)) {
                            return;
                        }
                    }

                    boolean flag7 = buildJob.getSchematic().isClearMode();
                    if (iBuilderProcess.isActive()) {
                        this.idleTicks = 0;
                        if (flag7) {
                            this.tickClearing(buildJob, iBuilderProcess, buildProgress);
                        } else if (!this.handleGoalTrap(buildJob, iBuilderProcess)) {
                            DefaultedList defaultedList = mc.player.getInventory().getMainStacks();
                            boolean flag8;
                            if (flag) {
                                flag8 = !buildJob.isOrientationProblem();
                            } else {
                                flag8 = false;
                            }

                            StallDetector.Action action;
                            boolean flag9;
                            label139: {
                                int i = this.getPlaceDelay() * 3 + 40;
                                action = iBuilderProcess.isPaused()
                                    ? this.stallDetector.checkPaused(buildProgress, this.classifyMissing(buildJob.getSchematic(), defaultedList), flag8)
                                    : this.stallDetector.checkBuilding(buildProgress, defaultedList, this.getUsableSlots(), this.getSneakTargetCell(buildProgress), i);
                                boolean flag10;
                                if (action != StallDetector.Action.MATERIALS) {
                                    if (this.stallDetector.isMaterialsShort()) {
                                        flag9 = this.shopper.canRetry();
                                        break label139;
                                    }

                                    flag10 = false;
                                } else {
                                    flag10 = true;
                                }

                                flag9 = flag10;
                            }

                            if (!flag9 || !this.tryAutoBuyMaterials(buildProgress)) {
                                this.handleStallResult(buildJob, iBuilderProcess, action);
                            }
                        }
                    } else {
                        boolean flag11 = !this.builderControl.isDoneBuilding();
                        if (!flag11 || ++this.idleTicks >= 60) {
                            if (flag7) {
                                if (!flag11) {
                                    buildJob.setSkipClearing(true);
                                    this.startBuilder(buildJob, iBuilderProcess);
                                } else {
                                    buildJob.setSkipClearing(false);
                                    this.startBuilder(buildJob, iBuilderProcess);
                                }
                            } else if (!this.rescanRequested) {
                                this.rescanRequested = true;
                                buildProgress.restartScan();
                            } else {
                                if (buildProgress.isScanned()) {
                                    this.handleBuilderFinished(buildJob, flag11);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void startBuilder(BuildJob buildJob, IBuilderProcess iBuilderProcess) {
        boolean flag = buildJob.getProgress() == null;
        if (flag) {
            buildJob.setProgress(new BuildProgress(buildJob.getSchematic(), buildJob.getOrigin()));
            this.notifyKeyed(NotificationType.INFO, "autobuild.notify.started");
            this.printChat("autobuild.chat.started", buildJob.getName(), buildJob.getOrigin().getX(), buildJob.getOrigin().getY(), buildJob.getOrigin().getZ());
        }

        boolean flag2 = this.lastStartedJob != buildJob;
        this.lastStartedJob = buildJob;
        if (flag2) {
            this.noProgressRetries = 0;
            this.lastPlacedCount = -1;
            this.shopCheckedAfterScan = false;
            this.stubbornCount = this.unbreakableCount = 0;
            this.lastStallProgress = -1;
            this.buildingTicks = this.strandedCount = 0;
        }

        this.initialPlacementVersion = buildJob.getPlacementHandle() == null ? 0 : buildJob.getPlacementHandle().getLoadedChunks();
        buildJob.getSchematic().setClearMode(!buildJob.isSkipClearing());
        LOGGER.info("AutoBuild starts the builder for {}, clearing {}", buildJob.getName(), buildJob.getSchematic().isClearMode());
        iBuilderProcess.build(buildJob.getName(), buildJob.getSchematic(), buildJob.getOrigin());
        buildJob.setStarted(true);
        this.activeJob = buildJob;
        this.resetHelpers();
    }

    private int getPlaceDelay() {
        return Math.round(20.0F / this.speed.getValue());
    }

    private long getSneakTargetCell(BuildProgress buildProgress) {
        BlockHitResult blockHitResult = this.getBaritone().getPathingBehavior().isPathing() ? null : this.getForcedBlockHit(Input.SNEAK);
        return blockHitResult == null ? Long.MIN_VALUE : buildProgress.toSolidCell(blockHitResult.getBlockPos().offset(blockHitResult.getSide()));
    }

    private BlockHitResult getForcedBlockHit(Input input) {
        IBaritone iBaritone = this.getBaritone();
        HitResult hitResult = iBaritone.getInputOverrideHandler().isInputForcedDown(input) ? iBaritone.getPlayerContext().objectMouseOver() : null;
        boolean flag = hitResult instanceof BlockHitResult && hitResult.getType() == Type.BLOCK;
        return flag ? (BlockHitResult)hitResult : null;
    }

    private boolean lookAt(float f, float f2) {
        RotationComponent rotationComponent = RotationComponent.getInstance();
        boolean flag = BaritoneAPI.getSettings().freeLook.value;
        if (this.headTurn.getValue() && flag && rotationComponent != null && this.isBuilding()) {
            boolean flag2 = rotationComponent.getPriority() > 2;
            if (flag2) {
                this.stallDetector.resetIdle();
                return true;
            }

            int i = mc.player.age - this.lastLookTick;
            this.lastLookTick = mc.player.age;
            boolean flag3 = i < 0 || i > 1;
            if (flag3) {
                mc.player.setYaw(ServerRotationTracker.getServerYaw());
                mc.player.setPitch(ServerRotationTracker.getServerPitch());
            }

            float f3 = this.speed.getValue() * 10.0F;
            this.targetRotation = new Rotation(f, f2);
            RotationComponent.rotateTo(this.targetRotation, f3, f3, 10, 2);
            return true;
        } else {
            return false;
        }
    }

    private boolean isLookOff() {
        boolean flag = this.targetRotation != null && mc.player != null && mc.player.age == this.lastLookTick;
        if (!this.getBaritone().getPathingBehavior().isPathing() && flag) {
            Rotation rotation2 = new Rotation(ServerRotationTracker.getServerYaw(), ServerRotationTracker.getServerPitch());
            return rotation2.distanceTo(this.targetRotation) > 2.0F;
        } else {
            return false;
        }
    }

    private void storePlacementGoals(Goal[] goals) {
        this.placementGoals = goals;
        this.goalsTick = mc.player.age;
    }

    private StallDetector.MovementStall detectGoalProblem(IBuilderProcess iBuilderProcess) {
        IBaritone iBaritone = this.getBaritone();
        IPathingBehavior iPathingBehavior = iBaritone.getPathingBehavior();
        Goal goal = iPathingBehavior.getGoal();
        boolean flag;
        if (mc.player.isOnGround() && mc.currentScreen == null) {
            flag = this.getForcedInputs(iBaritone).isEmpty();
        } else {
            flag = false;
        }

        boolean flag2;
        if (!iBuilderProcess.isPaused() && goal != null && iPathingBehavior.getCurrent() == null) {
            flag2 = flag;
        } else {
            flag2 = false;
        }

        boolean flag3;
        if (flag2) {
            flag3 = goal.isInGoal(iBaritone.getPlayerContext().playerFeet());
        } else {
            flag3 = false;
        }

        boolean flag4;
        if (flag3) {
            flag4 = !iPathingBehavior.getInProgress().isPresent();
        } else {
            flag4 = false;
        }

        return flag2 && !flag3 ? this.stallDetector.checkMovement(flag4, true) : this.stallDetector.checkMovement(flag4, false);
    }

    private boolean updateStranded(BuildProgress buildProgress, int i) {
        int i2 = buildProgress.getTotal() - buildProgress.getRemaining();
        boolean flag = this.strandedCount > 0 && i2 == this.lastStrandedProgress;
        boolean flag2;
        if (i <= 4) {
            flag2 = flag;
        } else {
            flag2 = true;
        }

        if (!flag2) {
            this.strandedCount = 0;
            this.lastStrandedProgress = i2;
            return flag2;
        } else if (flag) {
            this.strandedCount++;
            this.lastStrandedProgress = i2;
            return flag2;
        } else {
            this.strandedCount = 1;
            this.lastStrandedProgress = i2;
            return flag2;
        }
    }

    private boolean handleGoalTrap(BuildJob buildJob, IBuilderProcess iBuilderProcess) {
        StallDetector.MovementStall movementStall = this.detectGoalProblem(iBuilderProcess);
        boolean flag = Math.abs(mc.player.age - this.goalsTick) <= 1;
        if (movementStall != StallDetector.MovementStall.NONE && flag) {
            boolean flag2 = movementStall == StallDetector.MovementStall.CUT_OFF;
            BetterBlockPos betterBlockPos = this.getBaritone().getPlayerContext().playerFeet();
            BuildProgress buildProgress = buildJob.getProgress();
            LongArrayList longArrayList = new LongArrayList();
            boolean flag3 = false;

            for (Goal goal : this.placementGoals) {
                boolean flag4;
                if (!flag2) {
                    flag4 = goal.isInGoal(betterBlockPos);
                } else {
                    flag4 = true;
                }

                if (flag4 && goal instanceof IGoalRenderPos iGoalRenderPos) {
                    BlockPos blockPos = goal instanceof GoalBlock ? iGoalRenderPos.getGoalPos().down() : iGoalRenderPos.getGoalPos();
                    long l = buildProgress.toSolidCell(blockPos);
                    if (l != Long.MIN_VALUE) {
                        longArrayList.add(l);
                        flag3 |= !mc.world.getBlockState(blockPos).isAir();
                    }
                }
            }

            if (longArrayList.isEmpty()) {
                return false;
            }

            boolean flag5;
            if (flag2) {
                flag5 = this.updateStranded(buildProgress, longArrayList.size());
            } else {
                flag5 = false;
            }

            boolean flag6 = flag5 && this.strandedCount >= 3;
            String string = flag2 ? "cut off from own goals" : "held inside own goal";
            this.logState(string + ", cells " + longArrayList.size() + ", stranded times " + this.strandedCount);
            if (flag6 && !this.tryRejoinOnStall(buildProgress)) {
                this.abandonJob(buildJob);
            }

            if (flag6) {
                return true;
            }

            if (flag5) {
                buildProgress.postpone(longArrayList);
            } else {
                this.addStubborn(buildProgress.postponeOrSkip(longArrayList));
            }

            this.stallDetector.resetIdle();
            this.stallDetector.requestResume();
            if (flag3) {
                buildJob.setStarted(false);
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean tryAutoBuyMaterials(BuildProgress buildProgress) {
        if (!this.autoBuy.isEnabled()) {
            return false;
        }

        if (!this.shopper.isSupported()) {
            if (!this.shopUnsupportedWarned) {
                this.printChat("autobuild.chat.shopUnsupported");
            }

            this.shopUnsupportedWarned = true;
            return false;
        } else {
            DefaultedList defaultedList = mc.player.getInventory().getMainStacks();
            if (!this.shopper.start(buildProgress.getMaterials(defaultedList, defaultedList.size()))) {
                return false;
            }

            this.notifyKeyed(NotificationType.INFO, "autobuild.notify.shopping");
            return true;
        }
    }

    private void tickShopping(BuildJob buildJob, IBuilderProcess iBuilderProcess) {
        buildJob.getProgress().tick(mc.world, mc.player.getBlockPos());
        if (iBuilderProcess.isActive() && !iBuilderProcess.isPaused()) {
            iBuilderProcess.pause();
        }

        if (this.shopper.tick(Math.max(this.maxPrice.getInt(0), 0), this.overpay.getValue() / 100.0F)) {
            this.printChat("autobuild.chat.shopDone", this.shopper.getBoughtCount(), this.shopper.getFailedCount());
            this.stallDetector.reset();
            if (!this.paused && iBuilderProcess.isActive()) {
                this.resumeBuilder(iBuilderProcess);
            }
        }
    }

    private void tickClearing(BuildJob buildJob, IBuilderProcess iBuilderProcess, BuildProgress buildProgress) {
        BlockHitResult blockHitResult = this.getForcedBlockHit(Input.CLICK_LEFT);
        long l = blockHitResult == null ? Long.MIN_VALUE : buildProgress.toCell(blockHitResult.getBlockPos());
        StallDetector.Action action = this.stallDetector.checkClearing(buildProgress, l, iBuilderProcess.isPaused());
        if (action == StallDetector.Action.RESUME) {
            this.resumeBuilder(iBuilderProcess);
        }

        boolean flag = action == StallDetector.Action.STUBBORN;
        boolean flag2;
        if (action == StallDetector.Action.ABANDON) {
            flag2 = true;
        } else {
            flag2 = false;
            if (!flag) {
                return;
            }
        }

        this.logState("clearing stalled, " + action);
        if (!this.tryRejoinOnStall(buildProgress)) {
            if (flag) {
                buildProgress.skipCell(this.stallDetector.getStubbornPos());
                this.printChat("autobuild.chat.unbreakable", ++this.unbreakableCount);
            }

            if (flag2) {
                this.printChat("autobuild.chat.clearingStuck");
            }

            buildJob.setSkipClearing(flag2);
            this.startBuilder(buildJob, iBuilderProcess);
        }
    }

    private boolean tryRejoinOnStall(BuildProgress buildProgress) {
        int i = buildProgress.getTotal() - buildProgress.getRemaining();
        boolean flag = i == this.lastStallProgress;
        if (!this.rejoin.isEnabled() || !this.canRejoin() || flag) {
            return false;
        }

        if (!this.isRejoinReady()) {
            return true;
        }

        this.lastStallProgress = i;
        this.rejoinedOnStall = true;
        this.leaveServer("autobuild.leave.stalled");
        return true;
    }

    private boolean canRejoin() {
        return mc.getCurrentServerEntry() != null && LastServerInfo.isPresent();
    }

    private boolean isRejoinReady() {
        boolean flag = this.hurtTimer.hasPassed(30000.0);
        return this.canRejoin() && mc.currentScreen == null && !this.shopper.isActive() && flag;
    }

    private void leaveServer(String string) {
        this.logState("leaving the server, " + string);
        this.rejoinHandler.disconnect(this.getDisplayName() + ": " + Language.translate(string));
    }

    private String getForcedInputs(IBaritone iBaritone) {
        StringBuilder builder = new StringBuilder();

        for (Input input : Input.values()) {
            if (iBaritone.getInputOverrideHandler().isInputForcedDown(input)) {
                builder.append(input).append(' ');
            }
        }

        return builder.toString().trim();
    }

    private void logState(String string) {
        IBaritone iBaritone = this.getBaritone();
        IPathingBehavior iPathingBehavior = iBaritone.getPathingBehavior();
        IPathExecutor iPathExecutor = iPathingBehavior.getCurrent();
        boolean flag = iPathExecutor != null && iPathExecutor.getPosition() >= 0 && iPathExecutor.getPosition() < iPathExecutor.getPath().movements().size();
        String string2 = flag ? iPathExecutor.getPath().movements().get(iPathExecutor.getPosition()).getClass().getSimpleName() : "none";
        BetterBlockPos betterBlockPos = iBaritone.getPlayerContext().playerFeet();
        Goal goal = iPathingBehavior.getGoal();
        boolean flag2;
        if (goal != null) {
            flag2 = goal.isInGoal(betterBlockPos);
        } else {
            flag2 = false;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 9; i++) {
            builder.append(mc.player.getInventory().getMainStacks().get(i).getItem()).append(' ');
        }

        RotationComponent rotationComponent = RotationComponent.getInstance();
        BuildProgress buildProgress = this.activeJob.getProgress();
        LOGGER.info(
            "AutoBuild {}: clearing {}, paused {}, movement {}, searching path {}, feet {} in goal {}, on ground {}, look {} {}, keys [{}], slot {} {}, hotbar [{}], server sees {}, head priority {}, obstacles nearby {}, missing nearby {}, unsupported {}, postponed {}, placement goals {} aged {}, remaining {}, seconds of building since join {}, goal {}",
            string,
            this.activeJob.getSchematic().isClearMode(),
            iBaritone.getBuilderProcess().isPaused(),
            string2,
            iPathingBehavior.getInProgress().isPresent(),
            betterBlockPos.toShortString(),
            flag2,
            mc.player.isOnGround(),
            Math.round(mc.player.getYaw()),
            Math.round(mc.player.getPitch()),
            this.getForcedInputs(iBaritone),
            InventoryUtil.getSelectedSlot(),
            mc.player.getMainHandStack().getItem(),
            builder.toString().trim(),
            ServerInputTracker.describeInput(),
            rotationComponent == null ? -1 : rotationComponent.getPriority(),
            buildProgress.getObstacleCount(),
            buildProgress.getMismatchedNearby().size(),
            buildProgress.getUnsupportedCount(),
            buildProgress.getPostponedCount(),
            this.placementGoals.length,
            mc.player.age - this.goalsTick,
            buildProgress.getRemaining(),
            this.buildingTicks / 20,
            goal
        );
    }

    private void handleStallResult(BuildJob buildJob, IBuilderProcess iBuilderProcess, StallDetector.Action action) {
        boolean flag = action == StallDetector.Action.STUBBORN || action == StallDetector.Action.UNSUPPORTED || action == StallDetector.Action.ABANDON;
        if (flag) {
            this.logState("building stalled, " + action);
        }

        switch (action) {
            case MATERIALS:
                this.notifyKeyed(NotificationType.WARNING, "autobuild.notify.materials");
                return;
            case RESUME:
                this.resumeBuilder(iBuilderProcess);
                return;
            case ORIENTATION:
                buildJob.setOrientationProblem(true);
                this.notifyKeyed(NotificationType.WARNING, "autobuild.notify.orientation");
                buildJob.setStarted(false);
                return;
            case UNPLACEABLE:
                Set set = this.builderControl.getMissingStates();
                buildJob.getSchematic().skipStates(set);
                this.printChat("autobuild.chat.unplaceable", set.size());
                this.resumeBuilder(iBuilderProcess);
                return;
            case UNSUPPORTED:
                this.printChat("autobuild.chat.unsupported", this.stallDetector.getUnsupportedCount());
                return;
            case STUBBORN:
                if (this.tryRejoinOnStall(buildJob.getProgress())) {
                    return;
                }

                buildJob.getProgress().skipCell(this.stallDetector.getStubbornPos());
                this.addStubborn(1);
                return;
            case ABANDON:
                this.abandonJob(buildJob);
            case NONE:
        }
    }

    private void addStubborn(int i) {
        if (i != 0) {
            boolean flag = this.stubbornCount == 0;
            this.stubbornCount += i;
            if (flag) {
                this.stubbornReportTimer.reset();
            }

            boolean flag2;
            if (!flag) {
                flag2 = this.stubbornReportTimer.hasPassedAndReset(10000.0);
            } else {
                flag2 = true;
            }

            if (flag2) {
                this.printChat("autobuild.chat.stubborn", this.stubbornCount);
            }
        }
    }

    private StallDetector.PauseReason classifyMissing(FilteredSchematic filteredSchematic, List<ItemStack> itemStacks) {
        if (this.builderControl.isParseFailed()) {
            return StallDetector.PauseReason.SHORTAGE;
        }

        Set<BlockState> blockStates = this.builderControl.getMissingStates();
        if (blockStates.isEmpty()) {
            return StallDetector.PauseReason.OTHER;
        }

        HashSet hashSet = new HashSet();

        for (ItemStack itemStack : itemStacks) {
            hashSet.add(itemStack.getItem());
        }

        boolean flag = false;

        for (BlockState blockState : blockStates) {
            if (!hashSet.contains(BuildBlockUtil.getItem(blockState))) {
                return StallDetector.PauseReason.SHORTAGE;
            }

            flag |= !filteredSchematic.isSkippedState(blockState);
        }

        return flag ? StallDetector.PauseReason.UNPLACEABLE : StallDetector.PauseReason.OTHER;
    }

    private void resumeBuilder(IBuilderProcess iBuilderProcess) {
        this.builderControl.resetMissing();
        iBuilderProcess.resume();
    }

    private void handleBuilderFinished(BuildJob buildJob, boolean flag) {
        BuildProgress buildProgress = buildJob.getProgress();
        LitematicaIntegration.LoadedPlacementSchematic loadedPlacementSchematic = buildJob.getPlacementHandle();
        boolean flag2 = loadedPlacementSchematic != null && loadedPlacementSchematic.getPendingChunkCount() > 0;
        boolean flag3 = loadedPlacementSchematic != null && loadedPlacementSchematic.getLoadedChunks() > this.initialPlacementVersion;
        if (buildProgress.getRemaining() == 0) {
            if (!flag2) {
                this.completeJob(buildJob);
            } else if (flag3) {
                buildJob.resetFlags();
            } else {
                this.warnFarPlacement(buildJob);
            }
        } else {
            int i = buildProgress.pruneUnsupported(mc.world);
            if (i > 0) {
                this.printChat("autobuild.chat.unsupported", i);
                buildProgress.restartScan();
            } else {
                buildProgress.clearPostponed();
                int i2 = buildProgress.getTotal() - buildProgress.getRemaining();
                if (i2 > this.lastPlacedCount || flag3) {
                    this.noProgressRetries = 0;
                }

                this.lastPlacedCount = i2;
                boolean flag4 = !flag && ++this.noProgressRetries > 3;
                if (flag4) {
                    this.abandonJob(buildJob);
                } else {
                    buildJob.resetFlags();
                }
            }
        }
    }

    private void warnFarPlacement(BuildJob buildJob) {
        if (!this.farPlacementWarned) {
            this.farPlacementWarned = true;
            this.printChat("autobuild.chat.farPlacement", buildJob.getName());
        }
    }

    private boolean checkPlacementChanged(BuildJob buildJob, LitematicaIntegration.LoadedPlacementSchematic loadedPlacementSchematic) {
        this.litematica.refreshSchematicWorld();
        String string = this.litematica.getPlacementSignature(buildJob.getPlacementId(), buildJob.getPlacement());
        if (string != null && loadedPlacementSchematic.isSignatureChangeConfirmed(string)) {
            boolean flag = this.paused;
            this.stopActiveJob();
            buildJob.reset();
            this.paused = flag;
            return true;
        } else {
            return false;
        }
    }

    private void completeJob(BuildJob buildJob) {
        int i = buildJob.getProgress().getRemaining() + buildJob.getProgress().getManual();
        NotificationUtil.notify(NotificationType.SUCCESS, this.getDisplayName(), Language.translate("autobuild.notify.done"));
        this.printChat(i == 0 ? "autobuild.chat.done" : "autobuild.chat.doneManual", buildJob.getName(), i);
        this.finishJob(buildJob);
        this.jobFinished = true;
    }

    private void abandonJob(BuildJob buildJob) {
        NotificationUtil.notify(NotificationType.WARNING, this.getDisplayName(), Language.translate("autobuild.notify.incomplete"));
        this.printChat("autobuild.chat.incomplete", buildJob.getName(), buildJob.getProgress().getRemaining() + buildJob.getProgress().getManual());
        this.finishJob(buildJob);
        this.jobFinished = true;
    }

    private void stopActiveJob() {
        if (this.activeJob != null) {
            IBaritone iBaritone = this.getBaritone();
            if (iBaritone != null && iBaritone.getBuilderProcess().isActive()) {
                iBaritone.getPathingBehavior().cancelEverything();
            }

            this.activeJob.setStarted(false);
            this.activeJob = null;
            this.paused = false;
            this.resetHelpers();
        }
    }

    private void onDirectionalChange() {
        if (this.activeJob != null) {
            this.activeJob.setStarted(false);
        }
    }

    private void resetJobFlags() {
        if (this.activeJob != null) {
            this.activeJob.resetFlags();
        }
    }

    private void resetHelpers() {
        this.builderControl.reset();
        this.stallDetector.reset();
        this.shopper.stop();
        this.rescanRequested = this.farPlacementWarned = false;
        this.idleTicks = 0;
    }

    private void refreshFileList() {
        List list = this.schematicFiles.listSchematics();
        String string = list.contains(this.selected.getValue()) ? this.selected.getValue() : this.file.getValue();
        boolean flag = !list.equals(this.knownFiles);
        if (list.contains(string) && !string.equals(this.file.getValue()) || flag) {
            this.knownFiles = list;
            this.updatingFileModes = true;
            if (flag) {
                this.file.setModes(list.isEmpty() ? List.of("none") : list);
            }

            if (list.contains(string)) {
                this.file.setValue(string);
            }

            this.updatingFileModes = false;
        }
    }

    private void onFileChange() {
        if (!this.updatingFileModes && !this.file.is("none")) {
            this.selected.setValue(this.file.getValue());
        }
    }

    private void importPaths(List<Path> paths) {
        List list = this.schematicFiles.importSchematics(paths);
        if (!list.isEmpty()) {
            String string = (String)list.get(list.size() - 1);
            this.selected.setValue(string);
            this.source.setValue("file");
            this.refreshFileList();
            this.printChat("autobuild.chat.imported", string);
        }
    }

    public void addJob() {
        if (this.source.is("litematica")) {
            this.addLitematicaJobs();
        } else {
            this.refreshFileList();
            if (this.file.is("none")) {
                this.printChat("autobuild.chat.noFiles", this.schematicFiles.getSchematicsDir());
            } else {
                this.addFileJob(this.file.getValue(), parseRotation(this.rotation.getValue()));
            }
        }
    }

    public void addFileJob(String string, BlockRotation blockRotation) {
        if (mc.player != null && mc.world != null) {
            BlockPos blockPos = mc.player.getBlockPos();
            String string2 = this.resolveServerKey();
            if (string2 != null) {
                String string3 = this.getDimensionKey();
                this.schematicFiles
                    .loadSchematic(this.schematicFiles.getSchematicsDir().resolve(string))
                    .whenComplete(
                        (iStaticSchematic, throwable) -> {
                            if (throwable != null) {
                                ChatUtil.print(throwable.getMessage());
                            } else {
                                int i = string.lastIndexOf(46);
                                BuildJob buildJob = BuildJob.builder()
                                    .name(i > 0 ? string.substring(0, i) : string)
                                    .source(BuildJob.Source.FILE)
                                    .file(string)
                                    .rotation(blockRotation)
                                    .origin(blockPos)
                                    .server(string2)
                                    .dimension(string3)
                                    .build();
                                buildJob.markReady(new FilteredSchematic(rotateSchematic(iStaticSchematic, blockRotation)), blockPos);
                                this.enqueueJob(buildJob);
                                this.printChat("autobuild.chat.added", buildJob.getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
                            }
                        }
                    );
            }
        }
    }

    public void addLitematicaJobs() {
        if (mc.player != null && mc.world != null) {
            List<LitematicaIntegration.PlacementInfo> placementInfos = this.litematica.getActivePlacements();
            if (this.litematica.isLitematicaLoaded() && !this.litematica.isFailed()) {
                String string = this.resolveServerKey();
                if (string != null) {
                    String string2 = this.getDimensionKey();
                    int i = 0;

                    for (LitematicaIntegration.PlacementInfo placementInfo : placementInfos) {
                        boolean flag = false;

                        for (BuildJob buildJob : this.jobQueue.getJobs()) {
                            flag |= placementInfo.id().equals(buildJob.getPlacementId()) && buildJob.isIn(string, string2);
                        }

                        if (!flag) {
                            this.enqueueJob(
                                BuildJob.builder()
                                    .name(placementInfo.name())
                                    .source(BuildJob.Source.LITEMATICA)
                                    .placement(placementInfo.name())
                                    .placementId(placementInfo.id())
                                    .server(string)
                                    .dimension(string2)
                                    .build()
                            );
                            i++;
                        }
                    }

                    if (i == 0) {
                        this.printChat("autobuild.chat.noPlacements");
                    } else {
                        this.printChat("autobuild.chat.addedPlacements", i);
                    }
                }
            } else if (this.litematica.isLitematicaLoaded()) {
                this.printChat("autobuild.error.litematica");
            } else {
                this.printChat("autobuild.error.noLitematica");
            }
        }
    }

    public void pause() {
        if (this.activeJob == null) {
            this.printChat("autobuild.chat.idle");
        } else {
            this.paused = true;
            this.printChat("autobuild.chat.paused", this.activeJob.getName());
        }
    }

    public void resume() {
        if (!this.paused) {
            this.printChat("autobuild.chat.notPaused");
        } else {
            this.paused = false;
            IBaritone iBaritone = this.getBaritone();
            if (iBaritone != null && iBaritone.getBuilderProcess().isActive()) {
                this.resumeBuilder(iBaritone.getBuilderProcess());
            }

            this.printChat("autobuild.chat.resumed");
        }
    }

    public void skip() {
        BuildJob buildJob = this.activeJob == null && mc.world != null ? this.jobQueue.findJob(this.getServerKey(), this.getDimensionKey()) : this.activeJob;
        if (buildJob == null) {
            this.printChat("autobuild.chat.idle");
        } else {
            this.finishJob(buildJob);
            this.printChat("autobuild.chat.skipped", buildJob.getName());
        }
    }

    public void clearJobs() {
        this.stopActiveJob();
        this.jobQueue.clear();
        this.jobFinished = false;
        this.printChat("autobuild.chat.cleared");
    }

    public boolean removeJobAt(int i) {
        boolean flag = i >= 0 && i < this.jobQueue.getJobs().size();
        if (flag) {
            this.finishJob(this.jobQueue.getJobs().get(i));
        }

        return flag;
    }

    public List<BuildJob> getJobs() {
        return this.jobQueue.getJobs();
    }

    public List<String> getFiles() {
        return this.schematicFiles.listSchematics();
    }

    public void openFolder() {
        Util.getOperatingSystem().open(this.schematicFiles.getSchematicsDir());
    }

    public List<BuildProgress.MaterialCount> getMaterials() {
        if (this.activeJob == null) {
            return List.of();
        }

        BuildProgress buildProgress = this.activeJob.getProgress();
        return buildProgress != null && mc.player != null ? buildProgress.getMaterials(mc.player.getInventory().getMainStacks(), this.getUsableSlots()) : List.of();
    }

    private int getUsableSlots() {
        return !this.inventory.getValue() && !this.autoBuy.isEnabled() ? 9 : mc.player.getInventory().getMainStacks().size();
    }

    public boolean isBuilding() {
        IBaritone iBaritone = this.getBaritone();
        return !this.isEnabled() || this.activeJob == null || !this.activeJob.isStarted() || this.paused || iBaritone == null
            ? false
            : iBaritone.getBuilderProcess().isActive() && !iBaritone.getBuilderProcess().isPaused();
    }

    private void enqueueJob(BuildJob buildJob) {
        this.jobQueue.add(buildJob);
        if (!this.isEnabled()) {
            this.setEnabled(true);
        }
    }

    private void finishJob(BuildJob buildJob) {
        if (buildJob == this.activeJob) {
            this.stopActiveJob();
        }

        this.jobQueue.remove(buildJob);
        this.jobFinished = false;
    }

    private static ISchematic rotateSchematic(ISchematic iSchematic, BlockRotation blockRotation) {
        return blockRotation == BlockRotation.NONE ? iSchematic : new RotatedSchematic(iSchematic, blockRotation);
    }

    private IBaritone getBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    private String getServerKey() {
        ServerInfo serverInfo = mc.getCurrentServerEntry();
        boolean flag = serverInfo == null || serverInfo.address == null;
        if (flag) {
            return mc.getServer() == null ? "singleplayer" : "singleplayer:" + mc.getServer().getSaveProperties().getLevelName();
        } else {
            String string = serverInfo.address.toLowerCase(Locale.ROOT);
            if (!isAnarchyServer()) {
                return string;
            } else {
                return this.anarchyNumber < 0 ? null : string + "#" + this.anarchyNumber;
            }
        }
    }

    private String resolveServerKey() {
        if (!this.isEnabled()) {
            this.anarchyNumber = -1;
        }

        this.updateAnarchyNumber();
        String string = this.getServerKey();
        if (string == null) {
            this.printChat("autobuild.chat.unknownWorld");
        }

        return string;
    }

    private void updateAnarchyNumber() {
        if (this.anarchyNumber < 0 && isAnarchyServer()) {
            this.anarchyNumber = ServerManager.getAnarchyNumber();
        }
    }

    private static boolean isAnarchyServer() {
        return ServerManager.isFunTime() || ServerManager.isSpookyTime() || ServerManager.isHolyWorld() || ServerManager.isReallyWorld();
    }

    private String getDimensionKey() {
        return mc.world.getRegistryKey().getValue().toString();
    }

    private void notifyKeyed(NotificationType notificationType, String string) {
        NotificationUtil.notifyKeyed(notificationType, this.getDisplayName(), Language.translate(string));
    }

    private void printChat(String string, Object... objects) {
        ChatUtil.print(Language.translate(string), objects);
    }

    @EventHandler
    public void onRender3D(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && this.preview.isEnabled() && mc.player != null && this.currentServer != null) {
            MatrixStack matrixStack = renderEvent.getMatrices();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            int i = this.color.getValue();

            try (Render3DUtil.BoxBatch boxBatch2 = Render3DUtil.boxBatch()) {
                for (BuildJob buildJob : this.jobQueue.getJobs()) {
                    if (buildJob.getBounds() != null && buildJob.isIn(this.currentServer, this.currentDimension)) {
                        SchematicPreviewRenderer.drawBox(boxBatch2, matrixStack, vec3d, buildJob.getBounds(), i, buildJob == this.activeJob ? 1.0F : 0.45F);
                    }
                }

                Box box = this.activeJob == null && this.source.is("file")
                    ? this.previewRenderer.getPreviewBox(mc.player.getBlockPos(), this.rotation.is("90") || this.rotation.is("270"))
                    : null;
                if (box != null) {
                    SchematicPreviewRenderer.drawBox(boxBatch2, matrixStack, vec3d, box, i, 0.7F);
                }

                boolean flag = this.markers.getValue() && this.activeJob != null && this.activeJob.getProgress() != null;
                if (flag) {
                    SchematicPreviewRenderer.drawBlockMarkers(boxBatch2, matrixStack, vec3d, this.activeJob.getProgress().getMismatchedNearby(), i);
                }

                boxBatch2.draw();
            }
        }
    }

    @Override
    public String getTitle() {
        BuildProgress buildProgress = this.activeJob == null ? null : this.activeJob.getProgress();
        if (buildProgress == null) {
            return Language.translate("autobuild.hud.title");
        } else {
            return !buildProgress.isScanned()
                ? this.activeJob.getName()
                : this.activeJob.getName()
                    + " · "
                    + (this.activeJob.getSchematic().isClearMode() ? Language.translate("autobuild.hud.clearing") : buildProgress.getPercent() + "%");
        }
    }

    @Override
    public Icon getIcon() {
        return Icon.CUBE;
    }

    @Override
    public boolean showHeader() {
        return this.activeJob != null || super.showHeader();
    }

    @Override
    public void collectEntries(List<AutoBuild.MaterialEntry> materialEntries2) {
        if (this.entryRefreshTimer.hasPassedAndReset(250.0)) {
            Iterator iterator2 = this.materialEntries.values().iterator();

            while (iterator2.hasNext()) {
                ((AutoBuild.MaterialEntry)iterator2.next()).active = false;
            }

            int i = 0;

            for (BuildProgress.MaterialCount materialCount : this.getMaterials()) {
                if (materialCount.missing() == 0 || i++ >= 12) {
                    break;
                }

                AutoBuild.MaterialEntry materialEntry = this.materialEntries.computeIfAbsent(materialCount.item(), AutoBuild.MaterialEntry::new);
                materialEntry.missing = materialCount.missing();
                materialEntry.countText = "×" + materialCount.missing();
                materialEntry.progressText = " " + materialCount.have() + "/" + materialCount.need();
                materialEntry.active = true;
            }
        }

        for (AutoBuild.MaterialEntry materialEntry2 : this.materialEntries.values()) {
            if (materialEntry2.active || materialEntry2.animation.get() > 0.001F) {
                materialEntries2.add(materialEntry2);
            }
        }

        this.materialEntries.values().removeIf(materialEntry3 -> !materialEntry3.active && materialEntry3.animation.get() <= 0.001F && materialEntry3.animation.isFinished());
    }

    @Override
    public void sortEntries(List<AutoBuild.MaterialEntry> materialEntries2) {
        materialEntries2.sort(Comparator.<AutoBuild.MaterialEntry>comparingInt(materialEntry -> materialEntry.missing).reversed());
    }

    protected float getIconWidth(AutoBuild.MaterialEntry materialEntry) {
        return 8.0F;
    }

    protected float getEntryValueWidth(AutoBuild.MaterialEntry materialEntry) {
        return BODY_FONT.getWidth(materialEntry.countText, 6.0F);
    }

    protected void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4, AutoBuild.MaterialEntry materialEntry) {
        float f5 = MathUtil.roundToStep(f, 0.5F);
        float f6 = MathUtil.roundToStep(f2 + f3 / 2.0F - 4.0F, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f5, f6, 0.5F);
        GuiRenderUtil.drawCachedItem(drawContext, materialEntry, materialEntry.stack, f5, f6, f4);
        MatrixUtil.pop(drawContext);
    }

    protected void renderValue(DrawContext drawContext, float f, float f2, float f3, float f4, AutoBuild.MaterialEntry materialEntry) {
        new RenderUtil(drawContext, f, f2, BODY_FONT.getWidth(materialEntry.countText, 6.0F), f3)
            .textMiddleLeft(BODY_FONT, materialEntry.countText, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4), 6.0F);
    }

    protected String getLabel(AutoBuild.MaterialEntry materialEntry) {
        return materialEntry.name;
    }

    protected String getSuffix(AutoBuild.MaterialEntry materialEntry) {
        return materialEntry.progressText;
    }

    @Override
    protected String getEntryText(AutoBuild.MaterialEntry materialEntry) {
        return this.getLabel(materialEntry);
    }

    @Override
    protected String getEntrySuffix(AutoBuild.MaterialEntry materialEntry) {
        return this.getSuffix(materialEntry);
    }

    @Override
    protected float getEntryWidth(AutoBuild.MaterialEntry materialEntry) {
        return this.getIconWidth(materialEntry);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, AutoBuild.MaterialEntry materialEntry) {
        this.renderIcon(drawContext, f, f2, f3, f4, materialEntry);
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, AutoBuild.MaterialEntry materialEntry) {
        this.renderValue(drawContext, f, f2, f3, f4, materialEntry);
    }
    static final class MaterialEntry extends HudEntry {
        final ItemStack stack;
        final String name;
        String countText = "";
        String progressText = "";
        int missing;

        private MaterialEntry(Item item) {
            this.stack = new ItemStack(item);
            this.name = this.stack.getName().getString();
        }
    }
}
