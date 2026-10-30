package tech.sadnes.client.feature.impl.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.Settings.Setting;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.BlockUpdateEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.MenuTickEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.movement.Baritone;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.world.StyledBoxRenderer;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.network.LastServerInfo;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.ServerRotationTracker;
import tech.sadnes.client.util.rotation.RotationUtil;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

@FeatureInfo(name = "AutoGamble", displayName = "feature.impl.misc.autogamble", category = Category.MISC, subcategory = Subcategory.MISC_SERVER)
public final class AutoGamble extends Feature {
    private final MultiSetting rtpOn = new MultiSetting("autogamble.rtpOn", BooleanSetting.of("lowHealth", true), BooleanSetting.of("playerNear", true));
    private final NumberSetting rtpHealth = new NumberSetting("autogamble.rtpHealth", 13.0F, 2.0F, 20.0F, 1.0F, Unit.HP)
        .setVisibleWhen(() -> this.rtpOn.isSelected("lowHealth"));
    private final NumberSetting rtpPlayerDistance = new NumberSetting("autogamble.rtpPlayerDistance", 40.0F, 5.0F, 100.0F, 5.0F, Unit.METER)
        .setVisibleWhen(() -> this.rtpOn.isSelected("playerNear"));
    private final GroupSetting autoEat = new GroupSetting("autogamble.autoEat", true);
    private final NumberSetting hunger = new NumberSetting(this.autoEat, "autogamble.hunger", 14.0F, 1.0F, 19.0F, 1.0F);
    private final GroupSetting autoReconnect = new GroupSetting("autogamble.autoReconnect", true);
    private final NumberSetting reconnectDelay = new NumberSetting(this.autoReconnect, "autogamble.reconnectDelay", 2.0F, 1.0F, 5.0F, 1.0F, Unit.SECOND);
    private final BooleanSetting cache = new BooleanSetting("autogamble.cache", false);
    private final BooleanSetting dropJunk = new BooleanSetting("autogamble.dropJunk", true);
    private final GroupSetting highlight = new GroupSetting("autogamble.highlight", true);
    private final ColorSetting highlightColor = new ColorSetting(this.highlight, "autogamble.highlight.color", -6599169);
    private final TimerUtil sellTimer = new TimerUtil();
    private final TimerUtil pauseTimer = new TimerUtil();
    private final TimerUtil equipTimer = new TimerUtil();
    private final TimerUtil targetProgressTimer = new TimerUtil();
    private final TimerUtil dropWaitTimer = new TimerUtil();
    private final TimerUtil repathTimer = new TimerUtil();
    private final TimerUtil itemScanTimer = new TimerUtil();
    private final TimerUtil itemPickupTimer = new TimerUtil();
    private final TimerUtil eatTimeoutTimer = new TimerUtil();
    private final TimerUtil eatCooldownTimer = new TimerUtil();
    private final TimerUtil useItemTimer = new TimerUtil();
    private final TimerUtil reconnectTimer = new TimerUtil();
    private final TimerUtil toolSwapTimer = new TimerUtil();
    private final TimerUtil dropTimer = new TimerUtil();
    private final TimerUtil rtpCooldownTimer = new TimerUtil();
    private final TimerUtil breakCheckTimer = new TimerUtil();
    private final TimerUtil stuckReconnectTimer = new TimerUtil();
    private final TimerUtil climbEscapeTimer = new TimerUtil();
    private AutoGamble.State state = AutoGamble.State.MINING;
    private int sellPrevSlot = -1;
    private int sellSwappedSlot = -1;
    private int sellHotbarSlot = -1;
    private int sellContainerSlot = -1;
    private int equipAttempts;
    private final Random random = new Random();
    private BlockPos brokenBlockPos;
    private Block brokenBlock;
    private int unbrokenCount;
    private ServerInfo reconnectServerInfo;
    private ServerAddress reconnectAddress;
    private BlockPos targetDebris;
    private BlockPos lastMinedDebris;
    private BlockPos exploreTarget;
    private BlockPos cachedGoal;
    private final Set<BlockPos> cachedDebris = new LinkedHashSet<>();
    private BlockPos exploreOrigin;
    private Vec3d lastPosition;
    private int stuckTicks;
    private double closestTargetDistance = Double.MAX_VALUE;
    private final Set<BlockPos> blacklistedDebris = new HashSet<>();
    private BlockPos itemGoalPos;
    private ItemEntity targetItem;
    private final Set<BlockPos> debrisPositions = ConcurrentHashMap.newKeySet();
    private volatile int scanGeneration;
    private final Set<Integer> ignoredItemIds = new HashSet<>();
    private double exploreAngle;
    private boolean pathing;
    private boolean waitingForDrop;
    private boolean ascending;
    private boolean breaking;
    private boolean eating;
    private boolean droppingJunk;
    private boolean dropClicked;
    private int lastDropSlot = -1;
    private float dropYaw;
    private boolean startedEating;
    private int eatPrevSlot = -1;
    private Hand eatHand = Hand.MAIN_HAND;
    private int eatSwappedSlot = -1;
    private int toolPrevSlot = -1;
    private int toolSwappedSlot = -1;
    private boolean enabledBaritone;
    private boolean escapingClimbable;
    private BlockPos escapeTarget;
    private final Map<Setting<?>, Object> savedBaritoneSettings = new HashMap<>();

    public static AutoGamble getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoGamble.class);
    }

    public boolean isBreaking() {
        return this.isEnabled() && this.breaking;
    }

    public boolean isEating() {
        return this.isEnabled() && this.eating;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.state = AutoGamble.State.MINING;
        this.pauseTimer.setTime(0L);
        this.sellPrevSlot = -1;
        this.sellSwappedSlot = -1;
        this.sellHotbarSlot = -1;
        this.sellContainerSlot = -1;
        this.equipAttempts = 0;
        this.resetBreakCheck();
        this.pathing = false;
        this.waitingForDrop = false;
        this.ascending = false;
        this.eating = false;
        this.droppingJunk = false;
        this.eatSwappedSlot = -1;
        this.targetDebris = null;
        this.lastMinedDebris = null;
        this.exploreTarget = null;
        this.exploreOrigin = null;
        this.lastPosition = null;
        this.eatCooldownTimer.setTime(0L);
        this.blacklistedDebris.clear();
        this.itemGoalPos = null;
        this.targetItem = null;
        this.toolPrevSlot = -1;
        this.toolSwappedSlot = -1;
        this.scanLoadedChunks();
        this.itemScanTimer.setTime(0L);
        this.ignoredItemIds.clear();
        this.repathTimer.setTime(0L);
        this.exploreAngle = this.random.nextDouble() * Math.PI * 2.0;
        this.enabledBaritone = false;
        Settings settings = BaritoneAPI.getSettings();
        this.savedBaritoneSettings.clear();

        for (Setting setting : List.of(
            settings.allowBreak,
            settings.allowDownward,
            settings.autoTool,
            settings.allowInventory,
            settings.allowPlace,
            settings.avoidance,
            settings.sprintInWater,
            settings.allowSprint,
            settings.mobAvoidanceRadius,
            settings.mobAvoidanceCoefficient,
            settings.acceptableThrowawayItems,
            settings.blocksToAvoid
        )) {
            this.savedBaritoneSettings.put(setting, setting.value instanceof List list ? new ArrayList(list) : setting.value);
        }

        ArrayList arrayList = new ArrayList<>(settings.blocksToAvoid.value);

        for (RegistryEntry registryEntry : Registries.BLOCK.iterateEntries(BlockTags.CLIMBABLE)) {
            if (!arrayList.contains(registryEntry.value())) {
                arrayList.add((Block)registryEntry.value());
            }
        }

        settings.blocksToAvoid.value = arrayList;
        this.escapingClimbable = false;
        this.escapeTarget = null;
        settings.avoidance.value = true;
        settings.mobAvoidanceRadius.value = 12;
        settings.mobAvoidanceCoefficient.value = 2.5;
        settings.sprintInWater.value = false;
        settings.allowBreak.value = false;
        settings.allowDownward.value = false;
        settings.allowPlace.value = true;
        settings.autoTool.value = true;
        settings.allowInventory.value = true;
        Baritone.getInstance().setAllowPlace(true);
        IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (iBaritone != null) {
            iBaritone.getPathingBehavior().cancelEverything();
        }
    }

    @Override
    public void onEnable() {
        this.scanGeneration++;
        this.debrisPositions.clear();
        this.breaking = false;
        this.stopClimbableEscape();
        this.cancelPathing();
        this.stopEating();
        this.restoreToolSlot();
        this.restoreSellSlots();
        this.savedBaritoneSettings.forEach((setting, object) -> ((Setting<Object>)setting).value = object);
        this.savedBaritoneSettings.clear();
        Baritone baritone2 = Baritone.getInstance();
        baritone2.setAllowPlace(false);
        if (this.enabledBaritone && baritone2.isEnabled()) {
            baritone2.setEnabled(false);
        }

        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.stopClimbableEscape();
        this.cancelPathing();
        this.stopEating();
        this.restoreToolSlot();
        this.resetBreakCheck();
        this.state = AutoGamble.State.MINING;
        this.targetDebris = null;
        this.lastMinedDebris = null;
        this.exploreTarget = null;
        this.exploreOrigin = null;
        this.lastPosition = null;
        this.targetItem = null;
        this.itemGoalPos = null;
        this.blacklistedDebris.clear();
        this.ignoredItemIds.clear();
        this.scanGeneration++;
        this.debrisPositions.clear();
        this.itemScanTimer.setTime(0L);
        this.waitingForDrop = false;
        this.ascending = false;
        this.droppingJunk = false;
        BaritoneAPI.getSettings().allowBreak.value = false;
        BaritoneAPI.getSettings().allowDownward.value = false;
        BaritoneAPI.getSettings().autoTool.value = true;
        BaritoneAPI.getSettings().allowInventory.value = true;
        this.sellPrevSlot = -1;
        this.sellSwappedSlot = -1;
        this.sellHotbarSlot = -1;
        this.sellContainerSlot = -1;
        this.pauseTimer.setDelay(2500L);
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
        if (blockUpdateEvent.getState().isOf(Blocks.RESPAWN_ANCHOR)) {
            this.debrisPositions.add(blockUpdateEvent.getPos().toImmutable());
        } else {
            this.debrisPositions.remove(blockUpdateEvent.getPos());
        }
    }

    @EventHandler
    public void onRender3D(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && this.highlight.isEnabled() && !this.debrisPositions.isEmpty()) {
            MatrixStack matrixStack = renderEvent.getMatrices();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            int i = this.highlightColor.getValue();
            int i2 = ColorUtil.multiplyAlpha(i, 0.4F);

            try (StyledBoxRenderer.Batch batch2 = StyledBoxRenderer.batch(StyledBoxRenderer.Style.GRADIENT)) {
                for (BlockPos blockPos : this.debrisPositions) {
                    float f = (float)(blockPos.getX() - vec3d.x);
                    float f2 = (float)(blockPos.getY() - vec3d.y);
                    float f3 = (float)(blockPos.getZ() - vec3d.z);
                    batch2.add(matrixStack, f, f2, f3, f + 1.0F, f2 + 1.0F, f3 + 1.0F, this.blacklistedDebris.contains(blockPos) ? i2 : i);
                }

                batch2.flush();
            }
        }
    }

    @EventHandler
    public void onUpdate(MenuTickEvent menuTickEvent) {
        if (this.reconnectServerInfo != null) {
            if (mc.getNetworkHandler() == null && this.stuckReconnectTimer.hasPassed(2000L)) {
                ConnectScreen.connect(new TitleScreen(), mc, this.reconnectAddress, this.reconnectServerInfo, false, null);
                this.reconnectServerInfo = null;
                this.reconnectAddress = null;
            }
        } else {
            Screen screen = mc.currentScreen;
            boolean flag;
            if (!(screen instanceof TitleScreen)
                && !(screen instanceof MultiplayerScreen)
                && !(screen instanceof DirectConnectScreen)
                && !(screen instanceof ConnectScreen)
                && !(screen instanceof ReconfiguringScreen)) {
                flag = screen instanceof LevelLoadingScreen;
            } else {
                flag = true;
            }

            if (this.autoReconnect.isEnabled() && !flag && mc.getNetworkHandler() == null && LastServerInfo.isPresent()) {
                if (this.reconnectTimer.hasPassed((long)(this.reconnectDelay.getValue() * 1000.0F))) {
                    this.reconnectTimer.reset();
                    ConnectScreen.connect(
                        new MultiplayerScreen(new TitleScreen()), mc, LastServerInfo.getAddress(), LastServerInfo.getServerInfo(), false, null
                    );
                }
            } else {
                this.reconnectTimer.reset();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.breaking = false;
        this.reconnectTimer.reset();
        if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.getNetworkHandler() != null) {
            if (this.brokenBlockPos != null && this.breakCheckTimer.hasPassed(1500L)) {
                boolean flag = mc.world.getBlockState(this.brokenBlockPos).isOf(this.brokenBlock);
                this.unbrokenCount = flag ? this.unbrokenCount + 1 : 0;
                boolean flag2 = Profile.getRole() != null && Profile.getRole().ordinal() >= Role.ADMIN.ordinal();
                if (flag && flag2) {
                    ChatUtil.print("Не вышло");
                }

                this.brokenBlockPos = null;
            }

            if (this.unbrokenCount >= 3) {
                this.reconnectStuck();
                return;
            }

            if (this.state == AutoGamble.State.PREPARING_SELL) {
                if (!this.sellTimer.hasPassed(150L)) {
                    return;
                }

                int i = this.findDebrisSlot();
                if (i != -1) {
                    this.sellPrevSlot = InventoryUtil.getSelectedSlot();
                    this.sellHotbarSlot = this.sellPrevSlot;

                    for (int i2 = 0; i2 < 9; i2++) {
                        if (mc.player.getInventory().getStack(i2).isEmpty()) {
                            this.sellHotbarSlot = i2;
                            break;
                        }
                    }

                    this.equipAttempts = 0;
                    this.equipForSell(i);
                    this.state = AutoGamble.State.BEFORE_SELL;
                    return;
                }

                if (this.sellTimer.hasPassed(1500L)) {
                    this.abortSell(500L);
                    return;
                }
            } else if (this.state == AutoGamble.State.BEFORE_SELL) {
                if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                    if (!this.equipTimer.hasPassed(1000L)) {
                        return;
                    }

                    int i3 = this.findDebrisSlot();
                    if (i3 != -1 && this.equipAttempts < 3) {
                        this.equipForSell(i3);
                        return;
                    }

                    ChatUtil.print(Language.translate("autogamble.equipFailed", "AutoGamble: не удалось взять якорь в руку для продажи"));
                    this.setEnabled(false);
                    return;
                }

                if (this.equipTimer.hasPassed(300L)) {
                    if (mc.currentScreen != null) {
                        return;
                    }

                    mc.getNetworkHandler().sendChatCommand("sell");
                    this.sellTimer.reset();
                    this.state = AutoGamble.State.WAITING_SELL;
                    return;
                }
            } else if (this.state == AutoGamble.State.WAITING_SELL) {
                if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                    ScreenHandler screenHandler = genericContainerScreen.getScreenHandler();

                    for (Slot slot : screenHandler.slots) {
                        if (slot.inventory == mc.player.getInventory() && slot.getStack().isOf(Items.RESPAWN_ANCHOR)) {
                            this.sellContainerSlot = slot.id;
                            mc.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                            this.sellTimer.reset();
                            this.state = AutoGamble.State.TRANSFERRED;
                            return;
                        }
                    }
                }

                if (this.sellTimer.hasPassed(5000L)) {
                    this.abortSell(3000L);
                    return;
                }
            } else {
                if (this.state == AutoGamble.State.TRANSFERRED) {
                    if (!this.sellTimer.hasPassed(150L)) {
                        return;
                    }

                    if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen2
                        && !this.sellTimer.hasPassed(3000L)
                        && genericContainerScreen2.getScreenHandler().getSlot(this.sellContainerSlot).getStack().isOf(Items.RESPAWN_ANCHOR)) {
                        return;
                    }

                    this.abortSell(500L);
                    return;
                }

                if (mc.currentScreen != null) {
                    this.stopClimbableEscape();
                    this.cancelPathing();
                    this.stopEating();
                    this.restoreToolSlot();
                    return;
                }

                boolean flag3 = !mc.world
                    .getEntitiesByClass(
                        WitherEntity.class, mc.player.getBoundingBox().expand(25.0), witherEntity -> witherEntity.isAlive() && mc.player.squaredDistanceTo(witherEntity) <= 625.0
                    )
                    .isEmpty();
                boolean flag4 = this.rtpOn.isSelected("lowHealth") && mc.player.getHealth() < this.rtpHealth.getValue() && mc.player.hurtTime > 0;
                double d = this.rtpPlayerDistance.getValue() * this.rtpPlayerDistance.getValue();
                boolean flag5;
                if (this.rtpOn.isSelected("playerNear")) {
                    flag5 = mc.world
                        .getPlayers()
                        .stream()
                        .anyMatch(
                            abstractClientPlayerEntity -> abstractClientPlayerEntity != mc.player
                                && abstractClientPlayerEntity.isAlive()
                                && mc.getNetworkHandler().getPlayerListEntry(abstractClientPlayerEntity.getUuid()) != null
                                && mc.player.squaredDistanceTo(abstractClientPlayerEntity) <= d
                        );
                } else {
                    flag5 = false;
                }

                boolean flag6 = mc.player.isSubmergedInWater();
                if ((flag3 || flag4 || flag5 || flag6) && this.rtpCooldownTimer.hasPassed(5000L)) {
                    this.randomTeleport();
                    return;
                }

                if (this.handleClimbableEscape()) {
                    return;
                }

                boolean flag7 = this.autoEat.isEnabled() && mc.player.getHungerManager().getFoodLevel() <= this.hunger.getValue();
                boolean flag8 = mc.player.getHealth() < 14.0F;
                boolean flag9;
                if (flag8) {
                    flag9 = !mc.player.hasStatusEffect(StatusEffects.REGENERATION);
                } else {
                    flag9 = false;
                }

                if ((this.eating || (flag7 || flag9) && !this.eatCooldownTimer.isNotReached()) && this.handleAutoEat()) {
                    return;
                }

                if (flag8) {
                    this.cancelPathing();
                    return;
                }

                if ((this.droppingJunk || this.dropJunk.getValue()) && this.handleDropJunk()) {
                    return;
                }

                if (this.pauseTimer.isNotReached()) {
                    return;
                }

                boolean flag10 = mc.player.getInventory().getEmptySlot() == -1;
                if (this.findDebrisSlot() == -1 || !flag10 && this.hasTargetsNearby(30.0)) {
                    if (flag10) {
                        this.cancelPathing();
                        this.restoreToolSlot();
                        return;
                    }

                    Baritone baritone = Baritone.getInstance();
                    if (baritone == null) {
                        return;
                    }

                    if (!baritone.isEnabled()) {
                        baritone.setEnabled(true);
                        this.enabledBaritone = true;
                    }

                    IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                    if (iBaritone == null) {
                        return;
                    }

                    boolean flag11 = iBaritone.getCustomGoalProcess().isActive();
                    boolean flag12;
                    if (!mc.player.isTouchingWater()) {
                        flag12 = mc.player.isSubmergedInWater();
                    } else {
                        flag12 = true;
                    }

                    BaritoneAPI.getSettings().allowSprint.value = this.getOriginalSetting(BaritoneAPI.getSettings().allowSprint) && !flag12;
                    BaritoneAPI.getSettings().allowPlace.value = !flag12;
                    baritone.setAllowPlace(!flag12);
                    if (flag12 && mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }

                    Vec3d vec3d = mc.player.getEntityPos();
                    boolean flag13;
                    if (this.lastPosition != null && !(vec3d.squaredDistanceTo(this.lastPosition) > 2.25)) {
                        flag13 = mc.interactionManager.isBreakingBlock();
                    } else {
                        flag13 = true;
                    }

                    if (flag13) {
                        this.lastPosition = vec3d;
                        this.stuckTicks = 0;
                    } else {
                        if (++this.stuckTicks >= 300 && this.rtpCooldownTimer.hasPassed(30000L)) {
                            this.randomTeleport();
                            return;
                        }

                        if (this.stuckTicks % 100 == 0) {
                            this.cancelPathing();
                            this.restoreToolSlot();
                            this.cachedGoal = null;
                            this.exploreTarget = null;
                            this.exploreAngle = this.exploreAngle
                                + (this.random.nextBoolean() ? 1.0 : -1.0) * ((Math.PI / 2) + this.random.nextDouble() * Math.PI / 2.0);
                            boolean flag14 = this.stuckTicks >= 200;
                            if (flag14) {
                                if (this.targetDebris != null && !this.waitingForDrop) {
                                    this.blacklistedDebris.add(this.targetDebris);
                                }

                                if (this.targetItem != null) {
                                    this.ignoredItemIds.add(this.targetItem.getId());
                                }

                                this.targetDebris = null;
                                this.targetItem = null;
                                this.itemGoalPos = null;
                                this.waitingForDrop = false;
                            }
                        }
                    }

                    if (this.targetDebris != null && !this.waitingForDrop && !mc.world.getBlockState(this.targetDebris).isOf(Blocks.RESPAWN_ANCHOR)) {
                        this.cancelPathing();
                        this.restoreToolSlot();
                        this.cachedDebris.remove(this.targetDebris);
                        this.lastMinedDebris = this.targetDebris;
                        this.waitingForDrop = true;
                        this.dropWaitTimer.reset();
                        this.blacklistedDebris.clear();
                    }

                    if (this.waitingForDrop
                        && this.targetDebris != null
                        && this.targetItem == null
                        && mc.world.getBlockState(this.targetDebris).isOf(Blocks.RESPAWN_ANCHOR)) {
                        this.cancelPathing();
                        this.waitingForDrop = false;
                    }

                    boolean flag15 = this.targetDebris != null && mc.player.getEyePos().squaredDistanceTo(this.targetDebris.toCenterPos()) <= 16.0;
                    if (!this.waitingForDrop && !flag15) {
                        BlockPos blockPos = this.targetDebris;
                        double d2 = this.targetDebris == null ? Double.MAX_VALUE : this.getTargetScore(this.targetDebris) - 2.0;
                        double d3 = this.targetItem == null ? Double.MAX_VALUE : mc.player.squaredDistanceTo(this.targetItem);

                        for (BlockPos blockPos2 : this.debrisPositions) {
                            if (!mc.world.getBlockState(blockPos2).isOf(Blocks.RESPAWN_ANCHOR)) {
                                this.debrisPositions.remove(blockPos2);
                            } else if (!this.blacklistedDebris.contains(blockPos2) && !this.isNearWater(blockPos2)) {
                                if (this.cache.getValue() && this.cachedDebris.add(blockPos2) && this.cachedDebris.size() > 64) {
                                    this.cachedDebris.remove(this.cachedDebris.iterator().next());
                                }

                                if (!blockPos2.equals(this.targetDebris) && !(mc.player.squaredDistanceTo(blockPos2.toCenterPos()) >= d3)) {
                                    double d4 = this.getTargetScore(blockPos2);
                                    if (!(d4 >= d2)) {
                                        d2 = d4;
                                        blockPos = blockPos2;
                                    }
                                }
                            }
                        }

                        if (blockPos != null && !blockPos.equals(this.targetDebris)) {
                            this.setTarget(blockPos);
                        }
                    }

                    if (this.targetItem != null && (!this.targetItem.isAlive() || !this.targetItem.getStack().isOf(Items.RESPAWN_ANCHOR))) {
                        this.cancelPathing();
                        this.targetItem = null;
                        this.itemGoalPos = null;
                    }

                    if (this.targetItem == null && this.itemScanTimer.hasPassed(250L)) {
                        this.itemScanTimer.reset();
                        double d5 = Double.MAX_VALUE;

                        for (ItemEntity itemEntity : this.findDroppedDebris(30.0)) {
                            double d6 = mc.player.squaredDistanceTo(itemEntity);
                            if (!(d6 >= d5)) {
                                d5 = d6;
                                this.targetItem = itemEntity;
                            }
                        }

                        if (this.targetItem != null) {
                            this.cancelPathing();
                            this.restoreToolSlot();
                            this.itemGoalPos = null;
                            this.itemPickupTimer.reset();
                        }
                    }

                    if (this.targetItem != null) {
                        if (!this.itemPickupTimer.hasPassed(30000L)) {
                            BaritoneAPI.getSettings().allowBreak.value = true;
                            BaritoneAPI.getSettings().allowDownward.value = true;
                            BlockPos blockPos3 = this.targetItem.getBlockPos();
                            if (this.pathing && blockPos3.equals(this.itemGoalPos) && !this.repathTimer.hasPassed(3000L)) {
                                return;
                            }

                            this.itemGoalPos = blockPos3;
                            iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(blockPos3));
                            this.pathing = true;
                            this.repathTimer.reset();
                            return;
                        }

                        this.cancelPathing();
                        this.ignoredItemIds.add(this.targetItem.getId());
                        this.targetItem = null;
                        this.itemGoalPos = null;
                    }

                    if (this.waitingForDrop) {
                        boolean flag16 = this.targetDebris != null && mc.player.squaredDistanceTo(this.targetDebris.toCenterPos()) <= 4.0;
                        if (!this.dropWaitTimer.hasPassed(5000L) && (!flag16 || this.targetItem != null || !this.dropWaitTimer.hasPassed(1000L))) {
                            if ((!this.pathing || this.repathTimer.hasPassed(3000L)) && this.targetDebris != null) {
                                BaritoneAPI.getSettings().allowBreak.value = true;
                                BaritoneAPI.getSettings().allowDownward.value = true;
                                iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetDebris));
                                this.pathing = true;
                                this.repathTimer.reset();
                                return;
                            }

                            return;
                        }

                        this.cancelPathing();
                        this.waitingForDrop = false;
                        this.targetDebris = null;
                        this.ascending = this.isBelowSurface(2);
                        return;
                    }

                    if (this.targetDebris == null && this.exploreTarget == null && !this.ascending && this.isBelowSurface(3)) {
                        this.cancelPathing();
                        this.ascending = true;
                    }

                    if (this.ascending) {
                        BaritoneAPI.getSettings().allowBreak.value = true;
                        BaritoneAPI.getSettings().allowDownward.value = false;
                        int i4 = this.getSurfaceY(mc.player.getBlockX(), mc.player.getBlockZ());
                        if (mc.player.getBlockY() >= i4 - 1) {
                            this.cancelPathing();
                            this.ascending = false;
                            this.exploreTarget = null;
                            BaritoneAPI.getSettings().allowBreak.value = false;
                        } else if (!this.pathing || !flag11 || this.repathTimer.hasPassed(10000L)) {
                            this.cancelPathing();
                            int i5 = mc.player.getBlockX() + this.random.nextInt(17) - 8;
                            int i6 = mc.player.getBlockZ() + this.random.nextInt(17) - 8;
                            if (!this.isChunkLoaded(i5, i6)) {
                                return;
                            }

                            BlockPos blockPos4 = new BlockPos(i5, this.getSurfaceY(i5, i6), i6);
                            iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(blockPos4));
                            this.pathing = true;
                            this.repathTimer.reset();
                        }

                        if (this.ascending) {
                            return;
                        }
                    }

                    if (this.targetDebris == null && this.cache.getValue() && !this.cachedDebris.isEmpty()) {
                        BlockPos blockPos5 = null;
                        double d7 = 22500.0;
                        Iterator iterator2 = this.cachedDebris.iterator();

                        while (iterator2.hasNext()) {
                            BlockPos blockPos6 = (BlockPos)iterator2.next();
                            double d8 = Math.pow(blockPos6.getX() + 0.5 - mc.player.getX(), 2.0) + Math.pow(blockPos6.getZ() + 0.5 - mc.player.getZ(), 2.0);
                            if (!(d8 > d7) && !this.blacklistedDebris.contains(blockPos6)) {
                                boolean flag17;
                                label531: {
                                    boolean flag18;
                                    if (this.isChunkLoaded(blockPos6.getX(), blockPos6.getZ())) {
                                        if (mc.world.getBlockState(blockPos6).isOf(Blocks.RESPAWN_ANCHOR)) {
                                            flag17 = this.isNearWater(blockPos6);
                                            break label531;
                                        }

                                        flag18 = true;
                                    } else {
                                        flag18 = false;
                                    }

                                    flag17 = flag18;
                                }

                                if (flag17) {
                                    iterator2.remove();
                                } else {
                                    d7 = d8;
                                    blockPos5 = blockPos6;
                                }
                            }
                        }

                        if (blockPos5 != null) {
                            if (this.isChunkLoaded(blockPos5.getX(), blockPos5.getZ())) {
                                this.setTarget(blockPos5);
                                return;
                            }

                            if (this.pathing && blockPos5.equals(this.cachedGoal) && (flag11 || !this.repathTimer.hasPassed(1000L))) {
                                return;
                            }

                            this.cancelPathing();
                            this.exploreTarget = null;
                            this.cachedGoal = blockPos5;
                            iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(blockPos5.getX(), blockPos5.getZ()));
                            this.pathing = true;
                            this.repathTimer.reset();
                            return;
                        }
                    }

                    if (this.targetDebris == null && !this.hasAnyTargetsNearby(100.0) && this.rtpCooldownTimer.hasPassed(30000L)) {
                        this.randomTeleport();
                        return;
                    }

                    if (this.targetDebris == null) {
                        BaritoneAPI.getSettings().allowBreak.value = false;
                        BaritoneAPI.getSettings().allowDownward.value = false;
                        if (this.exploreOrigin == null) {
                            this.exploreOrigin = mc.player.getBlockPos();
                        }

                        if (this.exploreTarget != null
                            && Math.hypot(this.exploreTarget.getX() - mc.player.getX(), this.exploreTarget.getZ() - mc.player.getZ()) <= 4.0) {
                            this.exploreTarget = null;
                        }

                        if (this.exploreTarget != null) {
                            if (flag11) {
                                return;
                            }

                            if (!this.repathTimer.hasPassed(2000L)) {
                                return;
                            }
                        }

                        this.cancelPathing();
                        this.exploreTarget = null;
                        double d9 = -1.0;
                        double d10 = this.exploreAngle;
                        int i7 = 0;
                        int i8 = 0;

                        for (int i9 = 0; i9 < 12; i9++) {
                            double d11 = this.exploreAngle + (i9 / 11.0 - 0.5) * (Math.PI * 2.0 / 3.0);
                            double d12 = Math.cos(d11);
                            double d13 = Math.sin(d11);
                            int i10 = 0;

                            for (byte b = 16; b <= 512; b += 8) {
                                int i11 = mc.player.getBlockX() + (int)Math.round(d12 * b);
                                int i12 = mc.player.getBlockZ() + (int)Math.round(d13 * b);
                                if (!this.isChunkLoaded(i11, i12)) {
                                    break;
                                }

                                i10 = b;
                            }

                            i10 -= 16;
                            if (i10 >= 16) {
                                int i13 = mc.player.getBlockX() + (int)Math.round(d12 * i10);
                                int i14 = mc.player.getBlockZ() + (int)Math.round(d13 * i10);
                                double d14 = Math.pow(i13 - this.exploreOrigin.getX(), 2.0) + Math.pow(i14 - this.exploreOrigin.getZ(), 2.0);
                                if (!(d14 <= d9)) {
                                    d9 = d14;
                                    d10 = d11;
                                    i7 = i13;
                                    i8 = i14;
                                }
                            }
                        }

                        if (d9 < 0.0) {
                            this.exploreAngle += Math.PI / 2;
                            return;
                        }

                        this.exploreAngle = d10;
                        this.exploreTarget = new BlockPos(i7, this.getSurfaceY(i7, i8), i8);
                        iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(i7, i8));
                        this.pathing = true;
                        this.repathTimer.reset();
                        return;
                    }

                    double d15 = mc.player.squaredDistanceTo(this.targetDebris.toCenterPos());
                    if (d15 < this.closestTargetDistance - 1.0) {
                        this.closestTargetDistance = d15;
                        this.targetProgressTimer.reset();
                    }

                    if (this.targetProgressTimer.hasPassed(60000L)) {
                        mc.interactionManager.cancelBlockBreaking();
                        this.cancelPathing();
                        this.restoreToolSlot();
                        this.blacklistedDebris.add(this.targetDebris);
                        this.cachedDebris.remove(this.targetDebris);
                        this.targetDebris = null;
                        this.ascending = this.isBelowSurface(2);
                        return;
                    }

                    Vec3d vec3d2 = mc.player.getEyePos();
                    Vec3d vec3d3 = this.targetDebris.toCenterPos();
                    BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d2, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
                    boolean flag19 = mc.player.isOnGround() && vec3d2.squaredDistanceTo(this.targetDebris.toCenterPos()) <= 16.0;
                    if (flag19 && !blockHitResult.getBlockPos().equals(this.targetDebris)) {
                        for (Direction direction : Direction.values()) {
                            Vec3d vec3d4 = this.targetDebris.toCenterPos().add(direction.getOffsetX() * 0.45, direction.getOffsetY() * 0.45, direction.getOffsetZ() * 0.45);
                            BlockHitResult blockHitResult2 = mc.world.raycast(new RaycastContext(vec3d2, vec3d4, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
                            if (blockHitResult2.getBlockPos().equals(this.targetDebris)) {
                                blockHitResult = blockHitResult2;
                                vec3d3 = vec3d4;
                                break;
                            }
                        }
                    }

                    BlockPos blockPos7 = blockHitResult.getBlockPos();
                    BlockState blockState = mc.world.getBlockState(blockPos7);
                    boolean flag20 = blockHitResult.getType() == Type.BLOCK
                        && !blockState.isAir()
                        && blockState.getHardness(mc.world, blockPos7) >= 0.0F
                        && vec3d2.squaredDistanceTo(blockPos7.toCenterPos()) <= 16.0;
                    BlockPos blockPos8 = !flag19 || !blockPos7.equals(this.targetDebris) && !flag20 ? null : blockPos7;
                    if (blockPos8 != null) {
                        this.cancelPathing();
                        if (iBaritone.getPathingBehavior().isPathing()) {
                            iBaritone.getPathingBehavior().cancelEverything();
                            return;
                        }

                        Vec3d vec3d5 = vec3d2.add(
                            Vec3d.fromPolar(ServerRotationTracker.getServerPitch(), ServerRotationTracker.getServerYaw())
                                .multiply(mc.player.getBlockInteractionRange())
                        );
                        BlockHitResult blockHitResult3 = mc.world.raycast(new RaycastContext(vec3d2, vec3d5, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
                        this.rotateTo(new Rotation(RotationUtil.getRotationsTo(vec3d3)));
                        BlockState blockState2 = mc.world.getBlockState(blockPos8);
                        if (!this.selectBestPickaxe(blockState2)) {
                            return;
                        }

                        this.breaking = true;
                        boolean flag21;
                        if (blockHitResult3.getType() == Type.BLOCK) {
                            flag21 = blockHitResult3.getBlockPos().equals(blockPos8);
                        } else {
                            flag21 = false;
                        }

                        if (flag21 && mc.interactionManager.updateBlockBreakingProgress(blockPos8, blockHitResult3.getSide())) {
                            mc.world.spawnBlockBreakingParticle(blockPos8, blockHitResult3.getSide());
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }

                        boolean flag22;
                        if (!mc.world.getBlockState(blockPos8).isOf(blockState2.getBlock())) {
                            flag22 = !(blockState2.getBlock() instanceof FallingBlock);
                        } else {
                            flag22 = false;
                        }

                        if (flag22) {
                            this.brokenBlockPos = blockPos8;
                            this.brokenBlock = blockState2.getBlock();
                            this.breakCheckTimer.reset();
                            if (blockPos8.equals(this.targetDebris)) {
                                this.pauseTimer.setDelay(400 + this.random.nextInt(700));
                                if (this.hasWaterBelow(blockPos8) && this.rtpCooldownTimer.hasPassed(30000L)) {
                                    this.randomTeleport();
                                    return;
                                }

                                return;
                            }
                        }

                        return;
                    }

                    if (this.pathing && (flag11 || !this.repathTimer.hasPassed(1000L))) {
                        return;
                    }

                    iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(this.targetDebris, 2));
                    this.pathing = true;
                    this.repathTimer.reset();
                    return;
                }

                this.cancelPathing();
                BaritoneAPI.getSettings().autoTool.value = false;
                BaritoneAPI.getSettings().allowInventory.value = false;
                this.restoreToolSlot();
                this.ascending = this.ascending | this.isBelowSurface(2);
                this.targetDebris = null;
                this.targetItem = null;
                this.itemGoalPos = null;
                this.waitingForDrop = false;
                this.sellTimer.reset();
                this.state = AutoGamble.State.PREPARING_SELL;
                return;
            }
        }
    }

    private void scanLoadedChunks() {
        int i = ++this.scanGeneration;
        this.debrisPositions.clear();
        if (mc.world != null && mc.player != null) {
            ClientChunkManager clientChunkManager = mc.world.getChunkManager();
            int i2 = mc.player.getBlockX() >> 4;
            int i3 = mc.player.getBlockZ() >> 4;
            int i4 = mc.options.getViewDistance().getValue();
            CompletableFuture.runAsync(() -> {
                for (int i5 = i2 - i4; i5 <= i2 + i4; i5++) {
                    for (int i6 = i3 - i4; i6 <= i3 + i4; i6++) {
                        if (i != this.scanGeneration) {
                            return;
                        }

                        WorldChunk worldChunk = clientChunkManager.getChunk(i5, i6, ChunkStatus.FULL, false);
                        if (worldChunk != null) {
                            ChunkSection[] chunkSections = worldChunk.getSectionArray();
                            int i7 = chunkSections.length;

                            for (int i8 = 0; i8 < i7; i8++) {
                                ChunkSection chunkSection = chunkSections[i8];
                                if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(blockState -> blockState.isOf(Blocks.RESPAWN_ANCHOR))) {
                                    int i9 = worldChunk.getBottomY() + (i8 << 4);
                                    int i10 = worldChunk.getPos().getStartX();
                                    int i11 = worldChunk.getPos().getStartZ();

                                    for (int i12 = 0; i12 < 16; i12++) {
                                        for (int i13 = 0; i13 < 16; i13++) {
                                            for (int i14 = 0; i14 < 16; i14++) {
                                                if (chunkSection.getBlockState(i13, i12, i14).isOf(Blocks.RESPAWN_ANCHOR)) {
                                                    this.debrisPositions.add(new BlockPos(i10 + i13, i9 + i12, i11 + i14));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean isNearLastMined(BlockPos blockPos) {
        if (this.lastMinedDebris == null) {
            return false;
        }

        int i = Math.max(
            Math.abs(blockPos.getX() - this.lastMinedDebris.getX()),
            Math.max(Math.abs(blockPos.getY() - this.lastMinedDebris.getY()), Math.abs(blockPos.getZ() - this.lastMinedDebris.getZ()))
        );
        return i <= 2;
    }

    private double getTargetScore(BlockPos blockPos) {
        double d = Math.sqrt(mc.player.squaredDistanceTo(blockPos.toCenterPos()));
        if (d <= 8.0) {
            return d - 100.0;
        }

        double d2;
        double d3;
        if (this.getSurfaceY(blockPos.getX(), blockPos.getZ()) - 1 == blockPos.getY()) {
            d2 = d;
            d3 = 0.0;
        } else {
            d2 = d;
            d3 = 12.0;
        }

        return this.isNearLastMined(blockPos) ? d2 + d3 - 12.0 : d2 + d3 - 0.0;
    }

    private boolean hasTargetsNearby(double d) {
        double d2 = d * d;

        for (BlockPos blockPos : this.debrisPositions) {
            if (this.isValidDebris(blockPos) && mc.player.squaredDistanceTo(blockPos.toCenterPos()) <= d2) {
                return true;
            }
        }

        return this.findDroppedDebris(d).stream().anyMatch(itemEntity -> mc.player.squaredDistanceTo(itemEntity) <= d2);
    }

    private boolean hasAnyTargetsNearby(double d) {
        double d2 = d * d;

        for (BlockPos blockPos : this.debrisPositions) {
            if (this.isValidDebris(blockPos) && mc.player.squaredDistanceTo(blockPos.toCenterPos()) <= d2) {
                return true;
            }
        }

        if (!this.cache.getValue()) {
            return false;
        }

        for (BlockPos blockPos2 : this.cachedDebris) {
            if (!this.blacklistedDebris.contains(blockPos2) && mc.player.squaredDistanceTo(blockPos2.toCenterPos()) <= d2) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidDebris(BlockPos blockPos) {
        return !this.blacklistedDebris.contains(blockPos) && mc.world.getBlockState(blockPos).isOf(Blocks.RESPAWN_ANCHOR) && !this.isNearWater(blockPos);
    }

    private List<ItemEntity> findDroppedDebris(double d) {
        return mc.world
            .getEntitiesByClass(
                ItemEntity.class,
                mc.player.getBoundingBox().expand(d),
                itemEntity -> itemEntity.isAlive()
                    && itemEntity.getStack().isOf(Items.RESPAWN_ANCHOR)
                    && !this.ignoredItemIds.contains(itemEntity.getId())
                    && !this.isNearWater(itemEntity.getBlockPos())
            );
    }

    private boolean isNearWater(BlockPos blockPos) {
        if (mc.world.getFluidState(blockPos).isIn(FluidTags.WATER)) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && mc.world.getFluidState(blockPos.offset(direction)).isIn(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasWaterBelow(BlockPos blockPos) {
        for (int i = 1; i <= 32; i++) {
            BlockPos blockPos2 = blockPos.down(i);
            if (mc.world.getFluidState(blockPos2).isIn(FluidTags.WATER)) {
                return true;
            }

            if (!mc.world.getBlockState(blockPos2).isAir()) {
                return false;
            }
        }

        return false;
    }

    private void setTarget(BlockPos blockPos) {
        this.cancelPathing();
        this.restoreToolSlot();
        this.targetItem = null;
        this.itemGoalPos = null;
        this.targetDebris = blockPos;
        this.ascending = false;
        this.closestTargetDistance = Double.MAX_VALUE;
        this.targetProgressTimer.reset();
        this.exploreTarget = null;
        this.cachedGoal = null;
        BaritoneAPI.getSettings().allowBreak.value = true;
        BaritoneAPI.getSettings().allowDownward.value = true;
        ArrayList arrayList = new ArrayList(this.getOriginalSetting(BaritoneAPI.getSettings().acceptableThrowawayItems));

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (this.isThrowawayBlock(itemStack) && !arrayList.contains(itemStack.getItem())) {
                arrayList.add(itemStack.getItem());
            }
        }

        BaritoneAPI.getSettings().acceptableThrowawayItems.value = arrayList;
    }

    private boolean isThrowawayBlock(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockState blockState = block.getDefaultState();
            return blockState.isFullCube(mc.world, BlockPos.ORIGIN)
                && !(block instanceof BlockWithEntity)
                && !(block instanceof FallingBlock)
                && !blockState.isIn(BlockTags.BEACON_BASE_BLOCKS)
                && !itemStack.isOf(Items.RESPAWN_ANCHOR)
                && !itemStack.isOf(Items.TNT);
        } else {
            return false;
        }
    }

    private void randomTeleport() {
        this.stopClimbableEscape();
        this.cancelPathing();
        this.stopEating();
        this.restoreToolSlot();
        this.targetDebris = null;
        this.targetItem = null;
        this.itemGoalPos = null;
        this.waitingForDrop = false;
        this.ascending = false;
        this.droppingJunk = false;
        this.exploreTarget = null;
        this.exploreOrigin = null;
        this.cachedGoal = null;
        this.lastPosition = null;
        this.lastMinedDebris = null;
        this.cachedDebris.clear();
        this.blacklistedDebris.clear();
        this.ignoredItemIds.clear();
        mc.getNetworkHandler().sendChatCommand("rtp");
        this.rtpCooldownTimer.reset();
        this.pauseTimer.setDelay(2500L);
    }

    private void reconnectStuck() {
        this.resetBreakCheck();
        ServerInfo serverInfo = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry() : LastServerInfo.getServerInfo();
        ServerAddress serverAddress = serverInfo != null ? ServerAddress.parse(serverInfo.address) : LastServerInfo.getAddress();
        if (serverInfo != null && serverAddress != null) {
            String string = Language.translate("autogamble.stuckReconnect", "AutoGamble: блок не ломается, перезаходим на сервер");
            ChatUtil.print(string);
            this.reconnectServerInfo = serverInfo;
            this.reconnectAddress = serverAddress;
            this.stuckReconnectTimer.reset();
            mc.getNetworkHandler().getConnection().disconnect(Text.literal(string));
        }
    }

    private void resetBreakCheck() {
        this.brokenBlockPos = null;
        this.unbrokenCount = 0;
    }

    private boolean handleDropJunk() {
        if (!this.droppingJunk) {
            int i = 0;

            for (int i2 = 0; i2 < 36; i2++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i2);
                if (!itemStack.isEmpty() && !this.isValuableItem(itemStack)) {
                    i++;
                }
            }

            if (i < 8 || this.findJunkSlot() == -1) {
                return false;
            }

            this.cancelPathing();
            this.restoreToolSlot();
            this.dropYaw = ServerRotationTracker.getServerYaw() + 180.0F;
            this.droppingJunk = true;
            this.dropClicked = false;
            this.lastDropSlot = -1;
            this.dropTimer.reset();
        }

        this.rotateTo(new Rotation(this.dropYaw, 0.0F));
        if (!this.dropTimer.hasPassed(this.dropClicked ? 150L : 400L)) {
            return true;
        }

        int i3 = this.findJunkSlot();
        if (i3 == -1) {
            this.droppingJunk = false;
            this.pauseTimer.setDelay(300L);
            return true;
        }

        if (i3 == this.lastDropSlot && !this.dropTimer.hasPassed(1000L)) {
            return true;
        }

        InventoryUtil.clickQuickMove(i3 < 9 ? i3 + 36 : i3);
        this.lastDropSlot = i3;
        this.dropClicked = true;
        this.dropTimer.reset();
        return true;
    }

    private int findJunkSlot() {
        int i = -1;
        int i2 = 0;

        for (int i3 = 0; i3 < 36; i3++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i3);
            if (this.isThrowawayBlock(itemStack) && itemStack.getCount() > i2) {
                i2 = itemStack.getCount();
                i = i3;
            }
        }

        for (int i4 = 0; i4 < 36; i4++) {
            ItemStack itemStack2 = mc.player.getInventory().getStack(i4);
            if (!itemStack2.isEmpty() && i4 != i && !this.isValuableItem(itemStack2)) {
                return i4;
            }
        }

        return -1;
    }

    private boolean isValuableItem(ItemStack itemStack) {
        boolean flag;
        if (!itemStack.isIn(ItemTags.PICKAXES)
            && !itemStack.isIn(ItemTags.AXES)
            && !itemStack.isIn(ItemTags.SHOVELS)
            && !itemStack.isIn(ItemTags.HOES)
            && !itemStack.isIn(ItemTags.SWORDS)) {
            flag = itemStack.isOf(Items.SHEARS);
        } else {
            flag = true;
        }

        boolean flag2;
        if (!itemStack.isOf(Items.ROTTEN_FLESH)
            && !itemStack.isOf(Items.SPIDER_EYE)
            && !itemStack.isOf(Items.POISONOUS_POTATO)
            && !itemStack.isOf(Items.PUFFERFISH)
            && !itemStack.isOf(Items.CHORUS_FRUIT)) {
            flag2 = itemStack.isOf(Items.SUSPICIOUS_STEW);
        } else {
            flag2 = true;
        }

        return (!itemStack.contains(DataComponentTypes.FOOD) || flag2) && !flag && !itemStack.isOf(Items.TOTEM_OF_UNDYING) ? itemStack.isOf(Items.RESPAWN_ANCHOR) : true;
    }

    private boolean handleAutoEat() {
        if (!this.eating) {
            boolean flag = mc.player.getHealth() < 14.0F;
            Slot slot = null;
            if (flag) {
                slot = InventoryUtil.findSlotReverse(Items.ENCHANTED_GOLDEN_APPLE);
                if (slot == null) {
                    slot = InventoryUtil.findSlotReverse(Items.GOLDEN_APPLE);
                }
            }

            if (slot == null) {
                slot = InventoryUtil.findBestFood();
            }

            if (slot == null) {
                slot = InventoryUtil.findSlotReverse(Items.GOLDEN_APPLE);
            }

            if (slot == null) {
                slot = InventoryUtil.findSlotReverse(Items.ENCHANTED_GOLDEN_APPLE);
            }

            if (slot == null) {
                return false;
            }

            IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (iBaritone != null) {
                iBaritone.getPathingBehavior().cancelEverything();
            }

            this.pathing = false;
            this.restoreToolSlot();
            this.eatPrevSlot = InventoryUtil.getSelectedSlot();
            boolean flag2 = slot.inventory == mc.player.getInventory();
            boolean flag3 = flag2 && slot.getIndex() < 9;
            boolean flag4 = flag2 && slot.getIndex() == 40;
            this.eatHand = flag3 ? Hand.MAIN_HAND : Hand.OFF_HAND;
            if (flag3) {
                InventoryUtil.setSelectedSlot(slot.getIndex());
            } else if (!flag4) {
                InventoryUtil.swapToHand(slot, Hand.OFF_HAND);
                this.eatSwappedSlot = slot.id;
            }

            this.eating = true;
            this.startedEating = false;
            this.eatTimeoutTimer.reset();
            this.useItemTimer.reset();
            return true;
        } else {
            boolean flag5 = mc.player.isUsingItem();
            this.startedEating |= flag5;
            boolean flag6 = mc.player.getStackInHand(this.eatHand).contains(DataComponentTypes.FOOD);
            boolean flag7;
            if (!this.startedEating && this.eatTimeoutTimer.hasPassed(4000L)) {
                flag7 = true;
            } else {
                flag7 = this.eatTimeoutTimer.hasPassed(10000L);
            }

            if (this.startedEating && !flag5) {
                boolean flag8 = mc.player.getHungerManager().getFoodLevel() < 20;
                if (!flag8 || !flag6) {
                    this.stopEating();
                    this.pauseTimer.setDelay(250L);
                    return true;
                }

                this.startedEating = false;
                this.eatTimeoutTimer.reset();
            } else if (flag7) {
                this.stopEating();
                this.eatCooldownTimer.setDelay(10000L);
                return true;
            }

            if (!flag6) {
                return true;
            }

            mc.options.useKey.setPressed(true);
            if (!flag5 && this.useItemTimer.hasPassed(250L)) {
                this.useItemTimer.reset();
                mc.interactionManager.interactItem(mc.player, this.eatHand);
            }

            return true;
        }
    }

    private void stopEating() {
        if (this.eating) {
            this.eating = false;
            mc.options.useKey.setPressed(false);
            if (mc.player != null && mc.interactionManager != null) {
                if (this.eatSwappedSlot != -1) {
                    InventoryUtil.swapToHand(this.eatSwappedSlot, Hand.OFF_HAND);
                    this.eatPrevSlot = -1;
                    this.eatSwappedSlot = -1;
                } else {
                    InventoryUtil.setSelectedSlot(this.eatPrevSlot);
                    this.eatPrevSlot = -1;
                    this.eatSwappedSlot = -1;
                }
            }
        }
    }

    private boolean handleClimbableEscape() {
        if (mc.player.isClimbing() && !mc.player.isOnGround() && !mc.player.isTouchingWater()) {
            if (!this.escapingClimbable || this.climbEscapeTimer.hasPassed(8000L)) {
                boolean flag = this.escapingClimbable;
                this.stopEating();
                this.cancelPathing();
                this.restoreToolSlot();
                this.releaseMovementKeys();
                this.escapingClimbable = true;
                this.escapeTarget = this.findEscapePosition();
                this.climbEscapeTimer.reset();
                if ((this.escapeTarget == null || flag) && this.rtpCooldownTimer.hasPassed(5000L)) {
                    this.randomTeleport();
                    return true;
                }
            }

            if (this.escapeTarget == null) {
                return true;
            } else {
                double d = mc.player.getY() - this.escapeTarget.getY();
                boolean flag2 = this.escapeTarget.getX() != mc.player.getBlockX() || this.escapeTarget.getZ() != mc.player.getBlockZ();
                boolean flag3 = flag2 && d >= -1.0 && d <= 0.6;
                double d2 = Math.toDegrees(Math.atan2(this.escapeTarget.getZ() + 0.5 - mc.player.getZ(), this.escapeTarget.getX() + 0.5 - mc.player.getX()))
                    - 90.0;
                double d3 = Math.toRadians(MathHelper.wrapDegrees(d2 - mc.player.getYaw()));
                double d4 = flag3 ? Math.cos(d3) : 0.0;
                double d5 = flag3 ? -Math.sin(d3) : 0.0;
                mc.options.jumpKey.setPressed(d < -0.1);
                mc.options.sneakKey.setPressed(false);
                mc.options.forwardKey.setPressed(d4 > 0.38);
                mc.options.backKey.setPressed(d4 < -0.38);
                mc.options.leftKey.setPressed(d5 > 0.38);
                if (d5 < -0.38) {
                    mc.options.rightKey.setPressed(true);
                    return true;
                } else {
                    mc.options.rightKey.setPressed(false);
                    return true;
                }
            }
        } else {
            this.stopClimbableEscape();
            return false;
        }
    }

    private BlockPos findEscapePosition() {
        BlockPos blockPos = mc.player.getBlockPos();
        int i = 0;

        while (i > -48 && mc.world.getBlockState(blockPos.up(i - 1)).isIn(BlockTags.CLIMBABLE)) {
            i--;
        }

        int i2 = 0;

        while (i2 < 32 && mc.world.getBlockState(blockPos.up(i2 + 1)).isIn(BlockTags.CLIMBABLE)) {
            i2++;
        }

        BlockPos blockPos2 = null;
        double d = Double.MAX_VALUE;

        for (int i3 = 0; i3 <= 2; i3++) {
            BlockPos blockPos3 = blockPos.up(i - i3);
            if (i3 > 0 && !this.isPassable(blockPos3)) {
                break;
            }

            if (this.isStandablePosition(blockPos3)) {
                blockPos2 = blockPos3;
                d = (i3 - i) * 0.5;
                break;
            }
        }

        int i4;
        int i5;
        if (this.isPassable(blockPos.up(i2 + 1)) && this.isPassable(blockPos.up(i2 + 2))) {
            i4 = i2 + 1;
            i5 = i;
        } else {
            i4 = i2;
            i5 = i;
        }

        for (; i5 <= i4; i5++) {
            double d2 = (i5 > 0 ? i5 : -i5 * 0.5) + 0.5;
            if (!(d2 >= d)) {
                for (Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                    BlockPos blockPos4 = blockPos.up(i5).offset(direction);
                    if (this.isStandablePosition(blockPos4)) {
                        blockPos2 = blockPos4;
                        d = d2;
                        break;
                    }
                }
            }
        }

        return blockPos2;
    }

    private boolean isStandablePosition(BlockPos blockPos) {
        BlockPos blockPos2 = blockPos.down();
        boolean flag = mc.world.getBlockState(blockPos2).isSideSolidFullSquare(mc.world, blockPos2, Direction.UP);
        return flag && this.isPassable(blockPos) && this.isPassable(blockPos.up()) && !mc.world.getFluidState(blockPos).isIn(FluidTags.LAVA);
    }

    private boolean isPassable(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        return blockState.isIn(BlockTags.CLIMBABLE) || blockState.getCollisionShape(mc.world, blockPos).isEmpty();
    }

    private void stopClimbableEscape() {
        if (this.escapingClimbable) {
            this.escapingClimbable = false;
            this.escapeTarget = null;
            this.releaseMovementKeys();
        }
    }

    private void releaseMovementKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    private void rotateTo(Rotation rotation) {
        if (!RotationComponent.getInstance().isActive()) {
            mc.player.setYaw(ServerRotationTracker.getServerYaw());
            mc.player.setPitch(ServerRotationTracker.getServerPitch());
        }

        RotationComponent.rotateTo(rotation, 40.0F, 40.0F, 2, 10);
    }

    private int getSurfaceY(int i, int i2) {
        int i3 = mc.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, i, i2);
        if (mc.world.getBlockState(new BlockPos(i, i3 - 1, i2)).isOf(Blocks.SNOW)) {
            i3--;
        }

        return i3;
    }

    private void cancelPathing() {
        if (this.pathing) {
            IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (iBaritone != null) {
                iBaritone.getPathingBehavior().cancelEverything();
                this.pathing = false;
            }
        }
    }

    private boolean selectBestPickaxe(BlockState blockState) {
        int i = InventoryUtil.getSelectedSlot();
        int i2 = -1;
        float f = 0.0F;
        int i3 = 0;

        while (i3 < 36) {
            ItemStack itemStack = mc.player.getInventory().getStack(i3);
            if (itemStack.isIn(ItemTags.PICKAXES)) {
                float f2 = itemStack.getMiningSpeedMultiplier(blockState);
                if (f2 > f || f2 == f && i2 != -1 && i3 == i) {
                    f = f2;
                    i2 = i3++;
                    continue;
                }
            }

            i3++;
        }

        if (i2 != -1 && i2 != i) {
            mc.interactionManager.cancelBlockBreaking();
            if (this.toolPrevSlot == -1) {
                this.toolPrevSlot = i;
            }

            if (i2 < 9) {
                InventoryUtil.setSelectedSlot(i2);
                return true;
            }

            if (!this.toolSwapTimer.hasPassed(500L)) {
                return false;
            }

            InventoryUtil.swapToHand(i2, Hand.MAIN_HAND);
            if (this.toolSwappedSlot == -1) {
                this.toolSwappedSlot = i2;
            }

            this.toolSwapTimer.reset();
            return false;
        } else {
            return true;
        }
    }

    private void restoreToolSlot() {
        if (mc.player != null && mc.interactionManager != null && this.toolPrevSlot != -1) {
            mc.interactionManager.cancelBlockBreaking();
            if (this.toolSwappedSlot != -1) {
                InventoryUtil.swapToHand(this.toolSwappedSlot, Hand.MAIN_HAND);
                this.toolPrevSlot = -1;
                this.toolSwappedSlot = -1;
            } else {
                InventoryUtil.setSelectedSlot(this.toolPrevSlot);
                this.toolPrevSlot = -1;
                this.toolSwappedSlot = -1;
            }
        }
    }

    private void restoreSellSlots() {
        if (mc.player != null && mc.interactionManager != null && this.sellPrevSlot != -1) {
            if (this.sellSwappedSlot != -1) {
                InventoryUtil.setSelectedSlot(this.sellHotbarSlot);
                InventoryUtil.swapToHand(this.sellSwappedSlot, Hand.MAIN_HAND);
            }

            InventoryUtil.setSelectedSlot(this.sellPrevSlot);
            this.sellPrevSlot = -1;
            this.sellSwappedSlot = -1;
            this.sellHotbarSlot = -1;
        }
    }

    private int findDebrisSlot() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }

        return -1;
    }

    private void equipForSell(int i) {
        boolean flag = i < 9;
        if (flag) {
            this.sellHotbarSlot = i;
        }

        InventoryUtil.setSelectedSlot(this.sellHotbarSlot);
        if (!flag) {
            InventoryUtil.swapToHand(i, Hand.MAIN_HAND);
            this.sellSwappedSlot = i;
        }

        this.equipTimer.reset();
        this.equipAttempts++;
    }

    private void abortSell(long l) {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            mc.player.closeHandledScreen();
        }

        this.restoreSellSlots();
        BaritoneAPI.getSettings().autoTool.value = true;
        BaritoneAPI.getSettings().allowInventory.value = true;
        this.state = AutoGamble.State.MINING;
        this.pauseTimer.setDelay(l);
    }

    private <T> T getOriginalSetting(Setting<T> setting) {
        return (T)this.savedBaritoneSettings.getOrDefault(setting, setting.value);
    }

    private boolean isBelowSurface(int i) {
        return mc.player.getBlockY() < this.getSurfaceY(mc.player.getBlockX(), mc.player.getBlockZ()) - i;
    }

    private boolean isChunkLoaded(int i, int i2) {
        return mc.world.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(i), ChunkSectionPos.getSectionCoord(i2));
    }

    enum State {
        MINING,
        PREPARING_SELL,
        BEFORE_SELL,
        WAITING_SELL,
        TRANSFERRED;
    }
}
