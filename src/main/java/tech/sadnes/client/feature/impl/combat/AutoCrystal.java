package tech.sadnes.client.feature.impl.combat;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.combat.CrystalPlacementUtil;
import tech.sadnes.client.util.combat.ExplosionDamageUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.network.TickPacketOrderTracker;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.player.PearlTrajectoryUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.player.ServerRotationTracker;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.client.util.world.HoleFinder;

@FeatureInfo(
    name = "AutoCrystal",
    description = "feature.impl.combat.autocrystal",
    displayName = "feature.impl.combat.autocrystal",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_WEAPONS
)
public final class AutoCrystal extends Feature {
    private final MultiSetting options = new MultiSetting(
        "autocrystal.options",
        BooleanSetting.of("explosionCover", true),
        BooleanSetting.of("throughWalls", false),
        BooleanSetting.of("silentSlots", true),
        BooleanSetting.of("renderDamage", true),
        BooleanSetting.of("mineBase", false)
    );
    private final MultiSetting pause = new MultiSetting(
        "autocrystal.pause",
        BooleanSetting.of("eating", true),
        BooleanSetting.of("auraTarget", false),
        BooleanSetting.of("autoTotem", true),
        BooleanSetting.of("noTotems", false),
        BooleanSetting.of("noArmor", false)
    );
    private final GroupSetting aim = new GroupSetting("autocrystal.aim", true);
    private final NumberSetting aimSmoothness = new NumberSetting(this.aim, "autocrystal.aimSmoothness", 35.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT);
    private final NumberSetting aimRandomization = new NumberSetting(this.aim, "autocrystal.aimRandomization", 0.5F, 0.0F, 2.0F, 0.1F);
    private final GroupSetting anchors = new GroupSetting("autocrystal.anchors", true);
    private final NumberSetting anchorCount = new NumberSetting(this.anchors, "autocrystal.anchorCount", 2.0F, 1.0F, 3.0F, 1.0F);
    private final NumberSetting anchorProtection = new NumberSetting(this.anchors, "autocrystal.anchorProtection", 2.0F, 0.0F, 2.0F, 1.0F);
    private final GroupSetting hole = new GroupSetting("autocrystal.hole", false);
    private final NumberSetting holeRange = new NumberSetting(this.hole, "autocrystal.holeRange", 8.0F, 2.0F, 16.0F, 1.0F);
    private final NumberSetting holeWalkDistance = new NumberSetting(this.hole, "autocrystal.holeWalkDistance", 6.0F, 2.0F, 16.0F, 0.5F);
    private final BooleanSetting holePearl = new BooleanSetting(this.hole, "autocrystal.holePearl", true);
    private final NumberSetting minTargetDamage = new NumberSetting("autocrystal.minTargetDamage", 2.0F, 0.5F, 20.0F, 0.5F);
    private final NumberSetting maxSelfDamage = new NumberSetting("autocrystal.maxSelfDamage", 8.0F, 0.0F, 20.0F, 1.0F);
    private final NumberSetting delay = new NumberSetting("autocrystal.delay", 0.0F, 0.0F, 5.0F, 1.0F);
    private final GroupSetting visuals = new GroupSetting("autocrystal.visuals", true);
    private final BooleanSetting placeAnimation = new BooleanSetting(this.visuals, "autocrystal.placeAnimation", true);
    private final AutoCrystalRenderer placeRenderer = new AutoCrystalRenderer();
    private final Object2IntOpenHashMap<BlockPos> coverThreatCounts = new Object2IntOpenHashMap<>();
    private final StopWatch coverResetTimer = new StopWatch();
    private final StopWatch pendingAnchorTimer = new StopWatch();
    private final StopWatch pearlTimer = new StopWatch();
    private final StopWatch placedCrystalTimer = new StopWatch();
    private final StopWatch breakTimer = new StopWatch();
    private final Set<BlockPos> knownAnchors = new HashSet<>();
    private final Int2ObjectOpenHashMap<StopWatch> attackedCrystals = new Int2ObjectOpenHashMap<>();
    private int ticksSincePlace;
    private int ticksSinceKillBlast = 10;
    private int ticksSinceAnchorBlast = 10;
    private Vec3d lastAnchorBlastPos;
    private boolean lastBlastLethal;
    private final StopWatch shieldBreakTimer = new StopWatch();
    private final StopWatch axeAttackTimer = new StopWatch();
    private boolean hurtTimeExpiring;
    private final StopWatch weaponAttackTimer = new StopWatch();
    private boolean breakPending;
    private boolean rotationsOverridden;
    private BlockPos miningPos;
    private Direction miningSide;
    private Vec3d miningHitVec;
    private int miningFinishTick;
    private boolean silentSlotSwapped;
    private final StopWatch mineTimer = new StopWatch();
    private BlockPos lastMinedPos;
    private final StopWatch minedTimer = new StopWatch();
    private boolean pendingCrystalPlace;
    private boolean placedThisTick;
    private BlockPos holeWalkTarget;
    private Vec3d lastPlayerPos;
    private Vec3d playerVelocity = Vec3d.ZERO;
    private double lastGroundY;
    private BlockPos pearlTargetPos;
    private Vector2f pearlRotation;
    private BlockPos supportPos;
    private BlockPos placedCrystalPos;
    private Vec3d placedCrystalVec;
    private final List<BlockPos> anchorPositions = new ArrayList<>();
    private final Set<BlockPos> pendingAnchorPlacements = new HashSet<>();
    private final Set<BlockPos> trackedAnchors = new HashSet<>();
    private BlockPos pendingAnchorPos;
    private CrystalPlacementUtil.PlacementResult cachedAnchorPlacement;
    private boolean anchorPlacementCached;
    private BlockPos renderPos;
    private Vec3d renderHitVec;
    private LivingEntity target;
    private LivingEntity previousTarget;
    private LivingEntity lastTarget;
    private int ticksSinceTargetChange = 10;
    private final Set<BlockPos> holeCandidates = new HashSet<>();
    private BlockPos holePos;
    private int lastAttackedEntityId = -1;
    private Vec3d aimPoint;
    private long nextJitterTime;
    private float yawJitter;
    private float pitchJitter;

    public static AutoCrystal getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AutoCrystal.class);
    }

    public LivingEntity getActiveTarget() {
        return this.isEnabled() && CrystalPlacementUtil.isValidTarget(this.target) ? this.target : null;
    }

    @Override
    public void onEnable() {
        this.abortMining();
        this.lastMinedPos = null;
        this.pendingCrystalPlace = false;
        this.placedThisTick = false;
        this.target = null;
        this.previousTarget = null;
        this.lastTarget = null;
        this.holeCandidates.clear();
        this.holePos = null;
        this.supportPos = null;
        this.placedCrystalPos = null;
        this.pendingAnchorPos = null;
        this.renderPos = null;
        this.renderHitVec = null;
        this.placeRenderer.reset();
        this.lastAttackedEntityId = -1;
        this.holeWalkTarget = null;
        this.lastPlayerPos = null;
        this.playerVelocity = Vec3d.ZERO;
        this.pearlTargetPos = null;
        this.pearlRotation = null;
        this.anchorPositions.clear();
        this.trackedAnchors.clear();
        this.pendingAnchorPlacements.clear();
        this.knownAnchors.clear();
        this.attackedCrystals.clear();
        this.coverThreatCounts.clear();
        this.resetAim();
        super.onEnable();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        int i = this.ticksSincePlace;
        int i2 = this.ticksSinceKillBlast;
        int i3 = this.ticksSinceAnchorBlast;
        int i4 = this.ticksSinceTargetChange;
        boolean flag = this.placedThisTick;
        int i5 = i - -1;
        this.ticksSincePlace = i5;
        int i6 = i2 - -1;
        this.ticksSinceKillBlast = i6;
        int i7 = i3 - -1;
        this.ticksSinceAnchorBlast = i7;
        int i8 = i4 - -1;
        this.ticksSinceTargetChange = i8;
        this.placedThisTick = false;
        this.pendingCrystalPlace = false;
        boolean flag2 = ServerManager.isNullWorld();
        Set set = this.holeCandidates;
        int i9;
        if (flag2 || mc.interactionManager == null || !mc.player.isAlive() || mc.player.isSpectator()) {
            this.miningPos = null;
            this.silentSlotSwapped = false;
            this.target = null;
            this.previousTarget = null;
            this.lastTarget = null;
            set.clear();
            AutoCrystalRenderer autoCrystalRenderer = this.placeRenderer;
            this.holePos = null;
            this.supportPos = null;
            this.placedCrystalPos = null;
            this.pendingAnchorPos = null;
            this.renderPos = null;
            this.renderHitVec = null;
            autoCrystalRenderer.reset();
            List list = this.anchorPositions;
            this.lastAttackedEntityId = -1;
            this.holeWalkTarget = null;
            list.clear();
            this.trackedAnchors.clear();
            this.pendingAnchorPlacements.clear();
            this.resetAim();
            i9 = this.ticksSincePlace;
        } else if (this.shouldPause()) {
            this.abortMining();
            StopWatch stopWatch = this.shieldBreakTimer;
            this.renderPos = null;
            this.renderHitVec = null;
            this.holeWalkTarget = null;
            this.aimPoint = null;
            stopWatch.reset();
            i9 = this.ticksSincePlace;
        } else {
            LivingEntity livingEntity = CrystalPlacementUtil.findTarget();
            int i10 = this.ticksSinceKillBlast;
            int i11 = this.ticksSinceTargetChange;
            LivingEntity livingEntity2 = this.previousTarget;
            LivingEntity livingEntity3 = this.lastTarget;
            label71:
            if (livingEntity != null) {
                int i12;
                LivingEntity livingEntity4;
                LivingEntity livingEntity5;
                LivingEntity livingEntity6;
                if (livingEntity != livingEntity2) {
                    LivingEntity livingEntity7;
                    int i13;
                    int i14;
                    List list2;
                    if (livingEntity == livingEntity3) {
                        this.target = livingEntity;
                        ArrayList arrayList = new ArrayList(set);
                        int i15 = this.ticksSinceKillBlast;
                        livingEntity7 = this.previousTarget;
                        i13 = i15;
                        i14 = i11;
                        list2 = arrayList;
                    } else {
                        this.target = livingEntity;
                        List list3 = List.of();
                        int i16 = this.ticksSinceKillBlast;
                        livingEntity7 = this.previousTarget;
                        i13 = i16;
                        i14 = 10;
                        list2 = list3;
                    }

                    this.ticksSinceTargetChange = i13;
                    this.lastTarget = livingEntity7;
                    set.clear();
                    Set set2 = this.trackedAnchors;
                    set.addAll(set2);
                    this.anchorPositions.clear();
                    set2.clear();
                    Collection collection = list2;
                    set2.addAll(collection);
                    StopWatch stopWatch2 = this.shieldBreakTimer;
                    this.holePos = null;
                    stopWatch2.reset();
                    livingEntity6 = this.target;
                    livingEntity4 = livingEntity6;
                    livingEntity5 = livingEntity6;
                    i12 = i14;
                } else {
                    i12 = i10;
                    livingEntity4 = livingEntity;
                    livingEntity5 = livingEntity;
                    livingEntity6 = livingEntity2;
                }

                label68: {
                    this.ticksSinceKillBlast = i12;
                    this.target = livingEntity5;
                    this.previousTarget = livingEntity6;
                    this.updateHoleWalk(livingEntity4);
                    boolean flag3 = this.pearlTimer.hasPassed(1000.0);
                    BlockPos blockPos = this.miningPos;
                    boolean flag4 = this.rotationsOverridden;
                    BlockPos blockPos2;
                    if (!flag3) {
                        boolean flag5 = this.isRotationBlocked();
                        BlockPos blockPos3 = this.miningPos;
                        boolean flag6 = this.rotationsOverridden;
                        if (!flag5) {
                            break label68;
                        }

                        blockPos2 = blockPos3;
                        if (flag6) {
                            break label68;
                        }
                    } else {
                        blockPos2 = blockPos;
                        if (flag4) {
                            break label68;
                        }
                    }

                    if (blockPos2 != null) {
                        this.continueMining();
                        i9 = this.ticksSincePlace;
                    } else {
                        this.restoreSilentSlot();
                        int i17 = this.ticksSincePlace;
                        NumberSetting numberSetting = this.delay;
                        if (flag) {
                            this.isAwaitingPlacedCrystal();
                            i9 = this.ticksSincePlace;
                        } else {
                            float f = numberSetting.getValue();
                            int i18 = this.ticksSincePlace;
                            LivingEntity livingEntity8 = this.target;
                            float f2 = i17;
                            if (!(f > f2)) {
                                this.aimPoint = null;
                                boolean flag7 = this.runCombat(livingEntity8);
                                int i19 = this.ticksSincePlace;
                                if (flag7) {
                                    i9 = 0;
                                } else {
                                    i9 = i19;
                                }
                            } else {
                                i9 = i18;
                            }
                        }
                    }
                    break label71;
                }

                this.abortMining();
                i9 = this.ticksSincePlace;
            } else {
                this.target = livingEntity;
                this.abortMining();
                List list4 = this.anchorPositions;
                this.supportPos = null;
                this.placedCrystalPos = null;
                list4.clear();
                LivingEntity livingEntity9 = this.previousTarget;
                BlockPos blockPos4 = this.holePos;
                BlockPos blockPos5;
                if (livingEntity9 != null) {
                    boolean flag8 = livingEntity9.isAlive();
                    blockPos5 = this.holePos;
                    Set set3 = this.trackedAnchors;
                    if (!flag8) {
                        set3.clear();
                        this.previousTarget = null;
                        this.lastTarget = null;
                        set.clear();
                        blockPos5 = null;
                    }
                } else {
                    blockPos5 = blockPos4;
                }

                this.holePos = blockPos5;
                this.renderPos = null;
                this.renderHitVec = null;
                this.lastAttackedEntityId = -1;
                this.holeWalkTarget = null;
                this.resetAim();
                int i20 = this.ticksSincePlace;
                if (this.previousTarget != null) {
                    boolean flag9 = mc.player.isOnGround();
                    int i21 = this.ticksSincePlace;
                    if (!flag9) {
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        LivingEntity livingEntity10 = this.previousTarget;
                        float f3 = clientPlayerEntity.distanceTo(livingEntity10);
                        int i22 = this.ticksSincePlace;
                        LivingEntity livingEntity11 = this.previousTarget;
                        if (24.0F >= f3) {
                            this.updateHoleWalk(livingEntity11);
                            i9 = this.ticksSincePlace;
                        } else {
                            i9 = i22;
                        }
                    } else {
                        i9 = i21;
                    }
                } else {
                    i9 = i20;
                }
            }
        }

        this.ticksSincePlace = i9;
    }

    @EventHandler
    public void onRotationUpdate(PreRotationEvent preRotationEvent) {
        this.rotationsOverridden = ThrowComponent.getInstance().isRotatingBusy();
        if (this.rotationsOverridden) {
            this.aimPoint = null;
            RotationComponent rotationComponent = RotationComponent.getInstance();
            boolean flag = rotationComponent.isActive() && rotationComponent.getPriority() == 1000;
            if (flag) {
                RotationComponent.rotate(new Rotation(Rotation.getCameraYaw(), Rotation.getCameraPitch()), 999.0F, 999.0F, 999.0F, 999.0F, 2, 1000, false);
            }
        }
    }

    @EventHandler
    public void onPostTick(HudPostRenderEvent hudPostRenderEvent) {
        if (!ServerManager.isNullWorld()) {
            if (this.pendingCrystalPlace && TickPacketOrderTracker.isTickClean()) {
                this.placePendingCrystal();
            }

            if (this.aim.isEnabled() && this.aimPoint != null) {
                this.applyRotation(this.aimPoint, true);
            }
        }
    }

    private void placePendingCrystal() {
        this.pendingCrystalPlace = false;
        boolean flag = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL) || InventoryUtil.findHotbarIndex(Items.END_CRYSTAL) != -1;
        boolean flag2;
        if (this.target != null) {
            flag2 = !this.target.isAlive();
        } else {
            flag2 = true;
        }

        if (this.placedCrystalPos == null && !(this.delay.getValue() > 0.0F) && mc.interactionManager != null) {
            if (this.shouldPause()) {
                return;
            }

            if (!flag2 && flag) {
                CrystalPlacementUtil.PlacementResult placementResult = this.supportPos != null
                    ? CrystalPlacementUtil.evaluateCrystalPlacement(
                        this.supportPos,
                        this.target,
                        this.options.has("throughWalls"),
                        this.getDamageLimits(),
                        this.attackedCrystals.keySet(),
                        this.getAirborneExtraTicks(this.target)
                    )
                    : CrystalPlacementUtil.findBestCrystalPlacement(
                        this.target,
                        this.options.has("throughWalls"),
                        this.getDamageLimits(),
                        this.attackedCrystals.keySet(),
                        this.getAirborneExtraTicks(this.target),
                        this.canMineBase()
                    );
                if (placementResult != null) {
                    if (placementResult.hit() == null) {
                        return;
                    }

                    HitResult hitResult = RaycastUtil.raycastBlocks(
                        ServerRotationTracker.getServerYaw(), ServerRotationTracker.getServerPitch(), mc.player.getBlockInteractionRange(), false
                    );
                    boolean flag3;
                    if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK) {
                        flag3 = blockHitResult.getBlockPos().equals(placementResult.pos());
                    } else {
                        flag3 = false;
                    }

                    if (flag3) {
                        if (!this.placeItemWithSwap(Items.END_CRYSTAL, (BlockHitResult)hitResult)) {
                            return;
                        }

                        this.renderPos = placementResult.pos();
                        this.markCrystalPlaced(placementResult.pos(), hitResult.getPos());
                        this.placedThisTick = true;
                        this.ticksSincePlace = 0;
                        return;
                    }
                }
            }
        }
    }

    private boolean placeItemWithSwap(Item item, BlockHitResult blockHitResult) {
        if (mc.player.getOffHandStack().isOf(item)) {
            return this.sendInteract(Hand.OFF_HAND, blockHitResult, item);
        }

        int i = InventoryUtil.findHotbarIndex(item);
        if (this.options.has("silentSlots")) {
            return InventoryUtil.withHotbarSlot(i, () -> this.sendInteract(Hand.MAIN_HAND, blockHitResult, item));
        }

        if (InventoryUtil.getSelectedSlot() != i) {
            InventoryUtil.setSelectedSlot(i);
        }

        return this.sendInteract(Hand.MAIN_HAND, blockHitResult, item);
    }

    private int getPingTicks() {
        return Math.min((this.getPing() + 49) / 50, 3);
    }

    private boolean canMineBase() {
        return this.options.has("mineBase") && this.mineTimer.hasPassed(400.0);
    }

    private int getAirborneExtraTicks(LivingEntity livingEntity) {
        boolean flag;
        if (!livingEntity.isOnGround()) {
            if (this.ticksSinceAnchorBlast > 30) {
                boolean flag2 = !this.axeAttackTimer.hasPassed(1500.0);
                return flag2 ? 2 + this.getPingTicks() : 0;
            }

            flag = true;
        } else {
            flag = false;
        }

        boolean flag3 = flag;
        return flag3 ? 2 + this.getPingTicks() : 0;
    }

    @EventHandler
    public void onMovementInput(MoveInputEvent moveInputEvent) {
        if (this.holeWalkTarget != null && !ServerManager.isNullWorld() && mc.player.isOnGround() && !moveInputEvent.isJump()) {
            MoveUtil.steerToBlock(moveInputEvent, this.holeWalkTarget);
        }
    }

    @EventHandler(priority = 500)
    public void onPlayerMove(PlayerTickEvent playerTickEvent) {
        if (!ServerManager.isNullWorld()) {
            Vec3d vec3d = mc.player.getEntityPos();
            boolean flag = this.lastPlayerPos == null;
            this.playerVelocity = flag ? Vec3d.ZERO : vec3d.subtract(this.lastPlayerPos);
            if (flag || mc.player.isOnGround()) {
                this.lastGroundY = vec3d.y;
            }

            this.lastPlayerPos = vec3d;
            if (this.pearlRotation != null) {
                if (this.isRotationBlocked()) {
                    this.pearlRotation = null;
                } else {
                    PearlTrajectoryUtil.Solution solution = this.findPearlTrajectoryIntoHole(this.pearlTargetPos);
                    if (solution == null) {
                        solution = this.findPearlTrajectoryNearHole(this.pearlTargetPos);
                    }

                    if (solution != null) {
                        this.pearlRotation.set(solution.rotation());
                    }
                }
            }
        }
    }

    private void updateHoleWalk(LivingEntity livingEntity) {
        GroupSetting groupSetting = this.hole;
        this.holeWalkTarget = null;
        if (groupSetting.isEnabled()) {
            BlockPos blockPos;
            boolean flag;
            label64: {
                flag = mc.player.isOnGround();
                double d = this.playerVelocity.y;
                blockPos = this.holeWalkTarget;
                boolean flag2;
                if (d > 0.65) {
                    if (flag) {
                        break label64;
                    }

                    flag2 = false;
                } else {
                    double d2 = mc.player.getY();
                    blockPos = this.holeWalkTarget;
                    double d3 = this.lastGroundY;
                    if (ExactMath.subtract(d2, d3) > 1.5) {
                        if (flag) {
                            break label64;
                        }

                        flag2 = false;
                    } else {
                        if (flag) {
                            break label64;
                        }

                        flag2 = true;
                    }
                }

                this.holeWalkTarget = blockPos;
                boolean flag3 = mc.player.isGliding();
                blockPos = this.holeWalkTarget;
                if (flag3 || flag2) {
                    return;
                }
            }

            this.holeWalkTarget = blockPos;
            double d4 = mc.player.getX();
            BlockPos blockPos2 = this.holeWalkTarget;
            double d5 = this.lastGroundY;
            long l;
            if (flag) {
                double d6 = mc.player.getY();
                blockPos2 = this.holeWalkTarget;
                l = Double.doubleToRawLongBits(d6);
            } else {
                l = Double.doubleToRawLongBits(d5);
            }

            this.holeWalkTarget = blockPos2;
            double d7 = mc.player.getZ();
            double d8 = Double.longBitsToDouble(l);
            BlockPos blockPos3 = BlockPos.ofFloored(d4, d8, d7);
            NumberSetting numberSetting = this.holeRange;
            if (!flag || !HoleFinder.isHole(blockPos3)) {
                float f = numberSetting.getValue();
                int i = MathHelper.floor(livingEntity.getY());
                Vec3d vec3d = livingEntity.getEntityPos();
                double d9 = f;
                int i2 = i - 1;
                BlockPos blockPos4 = MoveUtil.findNearestBlockInRange(blockPos3, d9, i2, vec3d);
                if (blockPos4 != null) {
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    int i3 = blockPos4.getX();
                    double d10 = mc.player.getY();
                    int i4 = blockPos4.getZ();
                    double d11 = ExactMath.add(i3, 0.5);
                    double d12 = ExactMath.add(i4, 0.5);
                    double d13 = Math.sqrt(clientPlayerEntity.squaredDistanceTo(d11, d10, d12));
                    NumberSetting numberSetting2 = this.holeWalkDistance;
                    BooleanSetting booleanSetting = this.holePearl;
                    if (flag && numberSetting2.getValue().floatValue() >= d13 && MoveUtil.canWalkTo(blockPos4)) {
                        this.holeWalkTarget = blockPos4;
                    } else if (booleanSetting.getValue()) {
                        this.throwPearlToHole(blockPos4);
                    }
                }
            }
        }
    }

    private boolean isRotationBlocked() {
        ThrowComponent throwComponent = ThrowComponent.getInstance();
        return throwComponent == null || throwComponent.isIdle();
    }

    private void throwPearlToHole(BlockPos blockPos) {
        if (this.pearlTimer.hasPassed(1000.0)) {
            if (this.isRotationBlocked()) {
                Slot slot = InventoryUtil.findSlotReverse(Items.ENDER_PEARL);
                if (slot != null) {
                    ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
                    ItemStack itemStack = slot.getStack();
                    if (!itemCooldownManager.isCoolingDown(itemStack)) {
                        PearlTrajectoryUtil.Solution solution;
                        label94: {
                            solution = this.findPearlTrajectoryIntoHole(blockPos);
                            boolean flag;
                            if (solution != null) {
                                Hand hand = mc.player.getActiveHand();
                                Hand hand2 = Hand.MAIN_HAND;
                                if (hand != hand2) {
                                    break label94;
                                }

                                flag = mc.player.getItemUseTime() == 0;
                            } else {
                                solution = this.findPearlTrajectoryNearHole(blockPos);
                                if (solution == null) {
                                    return;
                                }

                                Hand hand3 = mc.player.getActiveHand();
                                Hand hand4 = Hand.MAIN_HAND;
                                if (hand3 != hand4) {
                                    break label94;
                                }

                                flag = mc.player.getItemUseTime() == 0;
                            }

                            if (flag) {
                                this.pearlTargetPos = blockPos;
                                Vector2f vector2f = solution.rotation();
                                this.pearlRotation = vector2f;
                                ThrowComponent throwComponent = ThrowComponent.getInstance();
                                Hand hand5 = Hand.MAIN_HAND;
                                Vector2f vector2f2 = this.pearlRotation;
                                throwComponent.useSlotFull(slot, hand5, vector2f2);
                                this.pearlTimer.reset();
                                return;
                            }

                            this.pearlTargetPos = blockPos;
                            Vector2f vector2f3 = solution.rotation();
                            this.pearlRotation = vector2f3;
                            ThrowComponent throwComponent2 = ThrowComponent.getInstance();
                            Hand hand6 = Hand.OFF_HAND;
                            Vector2f vector2f4 = this.pearlRotation;
                            throwComponent2.useSlotFull(slot, hand6, vector2f4);
                            this.pearlTimer.reset();
                            return;
                        }

                        this.pearlTargetPos = blockPos;
                        Vector2f vector2f5 = solution.rotation();
                        this.pearlRotation = vector2f5;
                        ThrowComponent throwComponent3 = ThrowComponent.getInstance();
                        Hand hand7 = Hand.MAIN_HAND;
                        Vector2f vector2f6 = this.pearlRotation;
                        throwComponent3.useSlotFull(slot, hand7, vector2f6);
                        this.pearlTimer.reset();
                    }
                }
            }
        }
    }

    private Vec3d getHoleCenter(BlockPos blockPos) {
        return Vec3d.ofBottomCenter(HoleFinder.findBestPos(HoleFinder.getRegionHoles(blockPos), blockPos));
    }

    private PearlTrajectoryUtil.Solution findPearlTrajectoryIntoHole(BlockPos blockPos) {
        List list = HoleFinder.findHoles(blockPos.add(-4, -3, -4), blockPos.add(4, 0, 4));
        return PearlTrajectoryUtil.solveThrowWithVelocity(
            this.getHoleCenter(blockPos), this.playerVelocity, flight -> flight.ticks() <= 20 && this.isInsideHole(flight.destination(), list)
        );
    }

    private PearlTrajectoryUtil.Solution findPearlTrajectoryNearHole(BlockPos blockPos) {
        Vec3d vec3d = this.getHoleCenter(blockPos);
        return PearlTrajectoryUtil.solveThrowWithVelocity(vec3d, this.playerVelocity, flight -> {
            Vec3d vec3d2 = flight.destination();
            return flight.ticks() <= 20 && vec3d2.squaredDistanceTo(vec3d.x, vec3d2.y, vec3d.z) <= 2.25;
        });
    }

    private boolean isInsideHole(Vec3d vec3d, List<BlockPos> blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            if (blockPos.getX() == MathHelper.floor(vec3d.x) && blockPos.getZ() == MathHelper.floor(vec3d.z) && blockPos.getY() <= vec3d.y) {
                Box box = new Box(vec3d.x - 0.3, blockPos.getY(), vec3d.z - 0.3, vec3d.x + 0.3, vec3d.y + 1.8, vec3d.z + 0.3);
                if (mc.world.isSpaceEmpty(mc.player, box)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean runCombat(LivingEntity livingEntity) {
        this.renderPos = null;
        this.renderHitVec = null;
        this.anchorPlacementCached = false;
        this.hurtTimeExpiring = false;
        long l = this.getPing() + 150L;
        this.attackedCrystals.values().removeIf(stopWatch -> stopWatch.hasPassed((double)l));
        this.trackedAnchors
            .removeIf(
                blockPos -> !blockPos.isWithinDistance(mc.player.getEntityPos(), 16.0)
                    || this.pendingAnchorPlacements.contains(blockPos)
                        && !this.anchorPositions.contains(blockPos)
                        && !mc.world.getBlockState(blockPos).isOf(Blocks.RESPAWN_ANCHOR)
            );
        this.pendingAnchorPlacements.removeIf(blockPos2 -> !this.trackedAnchors.contains(blockPos2) && !mc.world.getBlockState(blockPos2).isOf(Blocks.RESPAWN_ANCHOR));

        for (BlockPos blockPos3 : this.trackedAnchors) {
            if (!this.anchorPositions.contains(blockPos3) && this.isUsableAnchor(blockPos3)) {
                this.anchorPositions.add(blockPos3);
            }
        }

        if (this.tryExplosionCover(livingEntity)) {
            return true;
        }

        if (this.tryWeaponAttack(livingEntity)) {
            return true;
        }

        boolean flag = this.canUseAnchors(livingEntity);
        if (!flag) {
            this.shieldBreakTimer.reset();
        }

        if (!flag && this.tryAxeAttack(livingEntity)) {
            return true;
        }

        if (flag && this.handleTrackedAnchors(livingEntity)) {
            return true;
        }

        boolean flag2;
        if (!flag) {
            flag2 = this.hasAnchorAgainst(livingEntity);
        } else {
            flag2 = false;
        }

        if (flag2 && this.handleAnchorsDirect(livingEntity)) {
            return true;
        }

        CrystalPlacementUtil.CrystalAttack crystalAttack = CrystalPlacementUtil.findCrystalToAttack(
            livingEntity, this.getDamageLimits(), this.placedCrystalPos, this.attackedCrystals.keySet()
        );
        CrystalPlacementUtil.PlacementResult placementResult = CrystalPlacementUtil.findBestCrystalPlacement(
            livingEntity,
            this.options.has("throughWalls"),
            this.getDamageLimits(),
            this.attackedCrystals.keySet(),
            this.getAirborneExtraTicks(livingEntity),
            this.canMineBase()
        );
        if (flag && this.tryShieldBreak(livingEntity, crystalAttack, placementResult)) {
            return true;
        }

        boolean flag3;
        if (crystalAttack != null) {
            flag3 = crystalAttack.crystal().getBlockPos().equals(this.placedCrystalPos);
        } else {
            flag3 = false;
        }

        boolean flag4 = this.supportPos != null;
        boolean flag5;
        if (flag2) {
            flag5 = !mc.player.shouldCancelInteraction();
        } else {
            flag5 = false;
        }

        boolean flag6;
        if (!flag && !flag3 && !flag4) {
            if (!flag5) {
                boolean flag7 = this.shouldUseCrystals(livingEntity, crystalAttack, placementResult);
                return flag7
                    ? this.handleCrystals(livingEntity, crystalAttack, placementResult)
                    : !flag5 && this.handleAnchorsDirect(livingEntity) || this.ticksSinceKillBlast >= 10 && this.handleCrystals(livingEntity, crystalAttack, placementResult);
            }

            flag6 = false;
        } else {
            flag6 = true;
        }

        boolean flag8 = flag6;
        return flag8
            ? this.handleCrystals(livingEntity, crystalAttack, placementResult)
            : !flag5 && this.handleAnchorsDirect(livingEntity) || this.ticksSinceKillBlast >= 10 && this.handleCrystals(livingEntity, crystalAttack, placementResult);
    }

    private boolean shouldUseCrystals(LivingEntity livingEntity, CrystalPlacementUtil.CrystalAttack crystalAttack, CrystalPlacementUtil.PlacementResult placementResult) {
        if (crystalAttack == null && placementResult == null) {
            return false;
        }

        double d = Math.max(this.delay.getValue().floatValue(), 1.0) * (ServerManager.isDonut() ? CombatUtil.randomInt(50, 150) : 50);
        double d2 = crystalAttack == null ? 0.0 : this.getDamageRate(crystalAttack.damage(), d);
        if (placementResult != null) {
            int i = placementResult.mineHit() != null ? 7 : (placementResult.supportHit() != null ? 3 : 2);
            double d3 = i * d + this.getPing();
            d2 = Math.max(d2, this.getDamageRate(placementResult.damage(), d3));
        }

        if (mc.player.shouldCancelInteraction()) {
            return true;
        }

        double d4 = 0.0;
        int i2 = 0;
        int i3 = 0;

        while (i3 < this.anchorPositions.size()) {
            CrystalPlacementUtil.PlacementResult placementResult2 = this.evaluateAnchor(this.anchorPositions.get(i3), livingEntity);
            if (placementResult2 == null && !this.isUsableAnchor(this.anchorPositions.get(i3))) {
                this.setPendingAnchor(this.anchorPositions.remove(i3));
            } else {
                if (placementResult2 == null) {
                    i2++;
                } else {
                    d4 = Math.max(d4, this.getDamageRate(placementResult2.damage(), this.getAnchorActionCount(placementResult2) * d));
                }

                i3++;
            }
        }

        CrystalPlacementUtil.PlacementResult placementResult3 = this.anchorPositions.size() - i2 < this.anchorCount.getValue() ? this.findAnchorPlacement(livingEntity) : null;
        if (placementResult3 != null) {
            d4 = Math.max(d4, this.getDamageRate(placementResult3.damage(), this.getAnchorActionCount(placementResult3) * d));
        }

        return d2 >= d4;
    }

    private double getDamageRate(CrystalPlacementUtil.ExplosionDamage explosionDamage, double d) {
        return explosionDamage.targetDamage() / Math.max(d, 500.0);
    }

    private boolean handleCrystals(LivingEntity livingEntity, CrystalPlacementUtil.CrystalAttack crystalAttack, CrystalPlacementUtil.PlacementResult placementResult) {
        BlockPos blockPos = this.supportPos;
        MultiSetting multiSetting = this.options;
        BlockPos blockPos2 = this.lastMinedPos;
        boolean flag = this.hurtTimeExpiring;
        StopWatch stopWatch = this.breakTimer;
        if (crystalAttack != null) {
            BlockPos blockPos3;
            if (blockPos != null) {
                boolean flag2 = multiSetting.has("throughWalls");
                CrystalPlacementUtil.DamageLimits damageLimits = this.getDamageLimits();
                IntSet intSet = this.attackedCrystals.keySet();
                int i = this.getAirborneExtraTicks(livingEntity);
                CrystalPlacementUtil.PlacementResult placementResult2 = CrystalPlacementUtil.evaluateCrystalPlacement(blockPos, livingEntity, flag2, damageLimits, intSet, i);
                boolean flag3 = this.executeCrystalPlacement(placementResult2);
                BlockPos blockPos4 = this.supportPos;
                BlockPos blockPos5 = this.lastMinedPos;
                if (flag3) {
                    return true;
                }

                boolean flag4 = blockPos4.equals(blockPos5);
                StopWatch stopWatch2 = this.minedTimer;
                boolean flag5 = this.hurtTimeExpiring;
                if (flag4) {
                    boolean flag6 = stopWatch2.hasPassed(500.0);
                    StopWatch stopWatch3 = this.mineTimer;
                    if (!flag6) {
                        return false;
                    }

                    stopWatch3.delay(2500L);
                    boolean flag7 = this.hurtTimeExpiring;
                    blockPos = null;
                    blockPos3 = null;
                    if (flag7) {
                        this.supportPos = null;
                        this.lastMinedPos = null;
                        return false;
                    }
                } else {
                    blockPos = null;
                    blockPos3 = null;
                    if (flag5) {
                        this.supportPos = null;
                        this.lastMinedPos = null;
                        return false;
                    }
                }
            } else {
                blockPos3 = blockPos2;
                if (flag) {
                    return false;
                }
            }

            this.supportPos = blockPos;
            this.lastMinedPos = blockPos3;
            if (this.breakCrystal(crystalAttack)) {
                return true;
            }

            if (!stopWatch.hasPassed(150.0)) {
                return false;
            }
        } else {
            BlockPos blockPos6;
            if (blockPos != null) {
                boolean flag8 = multiSetting.has("throughWalls");
                CrystalPlacementUtil.DamageLimits damageLimits2 = this.getDamageLimits();
                IntSet intSet2 = this.attackedCrystals.keySet();
                int i2 = this.getAirborneExtraTicks(livingEntity);
                CrystalPlacementUtil.PlacementResult placementResult3 = CrystalPlacementUtil.evaluateCrystalPlacement(blockPos, livingEntity, flag8, damageLimits2, intSet2, i2);
                boolean flag9 = this.executeCrystalPlacement(placementResult3);
                BlockPos blockPos7 = this.supportPos;
                BlockPos blockPos8 = this.lastMinedPos;
                if (flag9) {
                    return true;
                }

                boolean flag10 = blockPos7.equals(blockPos8);
                StopWatch stopWatch4 = this.minedTimer;
                if (flag10) {
                    boolean flag11 = stopWatch4.hasPassed(500.0);
                    StopWatch stopWatch5 = this.mineTimer;
                    if (!flag11) {
                        return false;
                    }

                    stopWatch5.delay(2500L);
                    blockPos = null;
                    blockPos6 = null;
                } else {
                    blockPos = null;
                    blockPos6 = null;
                }
            } else {
                blockPos6 = blockPos2;
            }

            this.supportPos = blockPos;
            this.lastMinedPos = blockPos6;
            stopWatch.reset();
        }

        return this.isAwaitingPlacedCrystal() ? false : this.executeCrystalPlacement(placementResult);
    }

    private int getAnchorActionCount(CrystalPlacementUtil.PlacementResult placementResult) {
        int i = CrystalPlacementUtil.countShieldableSides(placementResult.pos(), this.anchorProtection.getValue().intValue());
        if (placementResult.supportHit() != null) {
            return CrystalPlacementUtil.isReplaceablePlant(mc.world.getBlockState(placementResult.pos())) ? 4 + i : 3 + i;
        } else {
            return this.getAnchorCharges(placementResult.pos()) > 0 ? 1 + i : 2 + i;
        }
    }

    private int getPing() {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return playerListEntry == null ? 0 : Math.max(playerListEntry.getLatency(), 0);
    }

    private boolean isAwaitingPlacedCrystal() {
        if (this.placedCrystalPos == null) {
            return false;
        } else if (this.placedCrystalTimer.hasPassed((double)(this.getPing() + 150L))) {
            this.placedCrystalPos = null;
            return false;
        } else {
            this.aimAt(this.placedCrystalVec);
            return true;
        }
    }

    private boolean tryExplosionCover(LivingEntity livingEntity) {
        if (!this.options.has("explosionCover")) {
            return false;
        }

        if (this.coverResetTimer.hasPassed(5000.0)) {
            this.coverThreatCounts.clear();
        }

        Vec3d vec3d = mc.player.getEntityPos();
        this.knownAnchors.removeIf(blockPos -> !blockPos.isWithinDistance(vec3d, 16.0) || !mc.world.getBlockState(blockPos).isOf(Blocks.RESPAWN_ANCHOR));
        ArrayList<BlockPos> blockPoses = new ArrayList<>(this.anchorPositions);
        blockPoses.removeIf(blockPos2 -> this.evaluateAnchor(blockPos2, livingEntity) == null);
        CrystalPlacementUtil.BlockingPlacement blockingPlacement = CrystalPlacementUtil.findSelfProtectPlacement(
            livingEntity, this.knownAnchors, blockPoses, this.placedCrystalPos, this.attackedCrystals.keySet(), this.coverThreatCounts, this.options.has("throughWalls")
        );
        if (blockingPlacement == null) {
            return false;
        }

        this.renderPos = blockingPlacement.pos();
        if (!this.placeItem(Items.OBSIDIAN, blockingPlacement.hit())) {
            return false;
        }

        this.coverThreatCounts.addTo(blockingPlacement.threat(), 1);
        this.coverResetTimer.reset();
        return true;
    }

    private boolean canUseAnchors(LivingEntity livingEntity) {
        if (!this.anchors.isEnabled()) {
            return true;
        } else if (!mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, mc.player.getBlockPos())) {
            return this.hasAnchorSupplies() ? false : !this.hasAnchorAgainst(livingEntity);
        } else {
            return true;
        }
    }

    private boolean tryShieldBreak(LivingEntity livingEntity, CrystalPlacementUtil.CrystalAttack crystalAttack, CrystalPlacementUtil.PlacementResult placementResult) {
        if (!this.axeAttackTimer.hasPassed((double)(this.getPing() + 150L))) {
            return false;
        }

        if (!livingEntity.isOnGround()) {
            return false;
        }

        if (crystalAttack == null && InventoryUtil.countItem(Items.END_CRYSTAL) == 0) {
            return false;
        }

        float f = Math.max(crystalAttack == null ? 0.0F : crystalAttack.damage().targetDamage(), placementResult == null ? 0.0F : placementResult.damage().targetDamage());
        boolean flag = f >= livingEntity.getHealth() + livingEntity.getAbsorptionAmount() || f >= 0.8F * ExplosionDamageUtil.maxExplosionDamage(livingEntity);
        boolean flag2 = this.shieldBreakTimer.hasPassed(1500.0);
        Vec3d vec3d = !flag2 && flag ? null : this.getAxeAttackPoint(livingEntity);
        if (vec3d == null) {
            return false;
        }

        long l = Math.max(this.getPing() + 50L, 150L);
        boolean flag3 = this.ticksSinceAnchorBlast * 50L <= l;
        boolean flag4;
        if (livingEntity.hurtTime == 0 && !flag3) {
            flag4 = true;
        } else {
            flag4 = false;
            this.hurtTimeExpiring = livingEntity.hurtTime * 50L <= l;
        }

        return flag4 && this.attackWithAxe(livingEntity, vec3d);
    }

    private void resetKillBlastTicks() {
        if (this.ticksSinceKillBlast >= 10) {
            this.ticksSinceKillBlast = 0;
        }
    }

    private boolean tryAxeAttack(LivingEntity livingEntity) {
        if (this.lastAttackedEntityId != livingEntity.getId() && this.ticksSinceKillBlast >= 10 && livingEntity.hurtTime <= 0 && mc.player.getBlockY() == livingEntity.getBlockY()) {
            for (BlockPos blockPos : this.anchorPositions) {
                if (this.getAnchorCharges(blockPos) > 0 && this.evaluateAnchor(blockPos, livingEntity) != null) {
                    return false;
                }
            }

            return this.attackWithAxe(livingEntity, this.getAxeAttackPoint(livingEntity));
        } else {
            return false;
        }
    }

    private boolean hasAnchorAgainst(LivingEntity livingEntity) {
        for (BlockPos blockPos : this.anchorPositions) {
            if (this.evaluateAnchor(blockPos, livingEntity) != null) {
                return true;
            }
        }

        return false;
    }

    private Vec3d getAxeAttackPoint(LivingEntity livingEntity) {
        return this.findBreachAxeSlot() == -1 ? null : this.getAttackPoint(livingEntity);
    }

    private Vec3d getAttackPoint(LivingEntity livingEntity) {
        Vec3d vec3d = mc.player.getEyePos();
        double d = mc.player.getEntityInteractionRange();
        Vec3d vec3d2 = AimPointUtil.getAimPoint(livingEntity, (float)d, true);
        if (vec3d.squaredDistanceTo(vec3d2) > d * d) {
            return null;
        }

        Vec2f vec2f = RotationUtil.getRotations(vec3d, vec3d2);
        boolean flag = RaycastUtil.isLookingAt(livingEntity, vec2f.x, vec2f.y, (float)d, false);
        return flag ? vec3d2 : null;
    }

    private boolean attackWithAxe(LivingEntity livingEntity, Vec3d vec3d) {
        if (vec3d == null) {
            return false;
        }

        int i = this.moveToHotbar(this.findBreachAxeSlot());
        if (i == -1) {
            return false;
        }

        if (!this.aimAtAttackPoint(livingEntity, vec3d)) {
            return true;
        }

        this.axeAttackTimer.reset();
        this.lastAttackedEntityId = livingEntity.getId();
        return this.attackWithSlot(i, livingEntity);
    }

    private boolean tryWeaponAttack(LivingEntity livingEntity) {
        boolean flag = !this.weaponAttackTimer.hasPassed((double)(this.getPing() + 150L));
        if (flag) {
            return false;
        }

        int i = ShieldBreaker.getInstance().getAxeSlotFor(livingEntity);
        Vec3d vec3d = i == -1 ? null : this.getAttackPoint(livingEntity);
        if (vec3d == null) {
            return false;
        }

        int i2 = this.moveToHotbar(i);
        if (i2 == -1) {
            return false;
        }

        if (!this.aimAtAttackPoint(livingEntity, vec3d)) {
            return true;
        }

        this.weaponAttackTimer.reset();
        return this.attackWithSlot(i2, livingEntity);
    }

    private boolean aimAtAttackPoint(LivingEntity livingEntity, Vec3d vec3d) {
        float f = mc.player.getYaw();
        float f2 = mc.player.getPitch();
        this.aimAt(vec3d);
        boolean flag = mc.player.getYaw() != f || mc.player.getPitch() != f2;
        boolean flag2 = RaycastUtil.isLookingAt(livingEntity, mc.player.getYaw(), mc.player.getPitch(), (float)mc.player.getEntityInteractionRange(), false);
        return !flag || flag2;
    }

    private int findBreachAxeSlot() {
        Reference reference = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);
        int i = Math.min(36, mc.player.getInventory().size());

        for (int i2 = 0; i2 < i; i2++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i2);
            if (itemStack.isIn(ItemTags.SWORDS) && EnchantmentHelper.getLevel(reference, itemStack) > 0) {
                return i2;
            }
        }

        return -1;
    }

    private boolean attackWithSlot(int i, LivingEntity livingEntity) {
        if (this.options.has("silentSlots")) {
            return InventoryUtil.withHotbarSlot(i, () -> this.attackEntity(livingEntity));
        }

        if (InventoryUtil.getSelectedSlot() != i) {
            InventoryUtil.setSelectedSlot(i);
        }

        return this.attackEntity(livingEntity);
    }

    private boolean attackEntity(LivingEntity livingEntity) {
        PacketSender.sendSilent(PlayerInteractEntityC2SPacket.attack(livingEntity, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
        AttackTimer.onAttack();
        return true;
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
                this.onBlockStateUpdate(blockUpdateS2CPacket.getPos(), blockUpdateS2CPacket.getState());
            } else {
                if (packetEvent.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
                    chunkDeltaUpdateS2CPacket.visitUpdates(this::onBlockStateUpdate);
                }
            }
        }
    }

    private void onBlockStateUpdate(BlockPos blockPos, BlockState blockState) {
        BlockPos blockPos2 = blockPos.toImmutable();
        mc.execute(() -> {
            boolean flag = blockState.isOf(Blocks.RESPAWN_ANCHOR);
            if (flag || !this.pendingAnchorPlacements.contains(blockPos2)) {
                this.pendingAnchorPlacements.remove(blockPos2);
                if (flag) {
                    this.knownAnchors.add(blockPos2);
                } else {
                    this.knownAnchors.remove(blockPos2);
                }

                if (!flag) {
                    this.anchorPositions.remove(blockPos2);
                    this.trackedAnchors.remove(blockPos2);
                    if (blockPos2.equals(this.pendingAnchorPos)) {
                        this.pendingAnchorPos = null;
                    }
                }
            }
        });
    }

    private boolean shouldPause() {
        if ((!ServerManager.isDonut() || !AutoTotem.getInstance().isSwapping() && !ThrowComponent.getInstance().isDonutBusy()) && mc.currentScreen == null) {
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                return true;
            }

            if (this.pause.has("eating") && mc.player.isUsingItem()) {
                return true;
            }

            if (this.pause.has("auraTarget")) {
                AttackAura attackAura = AttackAura.getInstance();
                if (attackAura != null && attackAura.isEnabled() && attackAura.getCurrentTarget() != null && attackAura.getCurrentTarget().isAlive()) {
                    return true;
                }
            }

            if (this.pause.has("autoTotem")) {
                AutoTotem autoTotem = AutoTotem.getInstance();
                if (autoTotem != null
                    && autoTotem.isEnabled()
                    && autoTotem.isInDanger()
                    && !mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                    && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    return true;
                }
            }

            if (this.pause.has("noTotems") && InventoryUtil.countItem(Items.TOTEM_OF_UNDYING) == 0) {
                return true;
            }

            if (this.pause.has("noArmor")) {
                boolean flag;
                if (!mc.player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                    && !mc.player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                    && !mc.player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) {
                    flag = mc.player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
                } else {
                    flag = true;
                }

                if (flag) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (mc.player == null || mc.world == null) {
            this.placeRenderer.reset();
        } else if (renderEvent.isWorld()) {
            boolean flag;
            if (this.visuals.isEnabled() && mc.player.isAlive() && !mc.player.isSpectator()) {
                flag = !this.rotationsOverridden;
            } else {
                flag = false;
            }

            BlockPos blockPos = flag ? this.renderPos : null;
            if (flag && this.renderPos != null) {
                this.placeRenderer.render(renderEvent, blockPos, this.renderHitVec, this.placeAnimation.getValue());
            } else {
                this.placeRenderer.render(renderEvent, blockPos, null, this.placeAnimation.getValue());
            }
        } else {
            if (this.options.has("renderDamage") && renderEvent.isHudPost()) {
                DrawContext drawContext = renderEvent.getContext();
                float f = renderEvent.getTickDelta();
                LivingEntity livingEntity = this.target != null && this.target.isAlive() ? this.target : null;

                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity endCrystalEntity && endCrystalEntity.isAlive()) {
                        Vec3d vec3d = ProjectionUtil.getInterpolatedPos(endCrystalEntity, f);
                        Vector2f vector2f = ProjectionUtil.worldToScreen(vec3d.x, vec3d.y + endCrystalEntity.getHeight() + 0.3, vec3d.z);
                        if (vector2f != null) {
                            float f2 = CrystalPlacementUtil.getExplosionDamage(endCrystalEntity.getEntityPos(), mc.player);
                            String string = String.format(Locale.US, Language.translate("autocrystal.damage.self", "Self: %.1f"), f2);
                            int i = f2 >= 10.0F
                                ? ColorUtil.rgb(235, 64, 52)
                                : (f2 >= 5.0F ? ColorUtil.rgb(255, 180, 50) : ColorUtil.rgb(80, 220, 100));
                            if (livingEntity != null) {
                                float f3 = CrystalPlacementUtil.getExplosionDamage(endCrystalEntity.getEntityPos(), livingEntity);
                                String string2 = String.format(Locale.US, Language.translate("autocrystal.damage.target", "Target: %.1f"), f3);
                                int i2 = f3 >= 10.0F
                                    ? ColorUtil.rgb(80, 220, 100)
                                    : (f3 >= 5.0F ? ColorUtil.rgb(255, 180, 50) : ColorUtil.rgb(235, 64, 52));
                                float f4 = Fonts.INTER_MEDIUM.getWidth(string2, 7.0F);
                                float f5 = Fonts.INTER_MEDIUM.getWidth(string, 7.0F);
                                float f6 = Math.max(f4, f5) + 8.0F;
                                float f7 = vector2f.x - f6 / 2.0F;
                                float f8 = vector2f.y - 22.0F / 2.0F;
                                GuiRenderUtil.drawPanel(drawContext, f7, f8, f6, 22.0F, 1.0F, 4.0F, false);
                                new RenderUtil(drawContext, f7, f8 + 2.0F, f6, 9.0F).textCenter(Fonts.INTER_MEDIUM, string2, i2, 7.0F, true);
                                new RenderUtil(drawContext, f7, f8 + 12.0F, f6, 9.0F).textCenter(Fonts.INTER_MEDIUM, string, i, 7.0F, true);
                            } else {
                                float f9 = Fonts.INTER_MEDIUM.getWidth(string, 7.0F) + 8.0F;
                                float f10 = vector2f.x - f9 / 2.0F;
                                float f11 = vector2f.y - 12.0F / 2.0F;
                                GuiRenderUtil.drawPanel(drawContext, f10, f11, f9, 12.0F, 1.0F, 4.0F, false);
                                new RenderUtil(drawContext, f10, f11, f9, 12.0F).textCenter(Fonts.INTER_MEDIUM, string, i, 7.0F, true);
                            }
                        }
                    }
                }
            }
        }
    }

    private CrystalPlacementUtil.DamageLimits getDamageLimits() {
        return new CrystalPlacementUtil.DamageLimits(this.minTargetDamage.getValue(), this.maxSelfDamage.getValue());
    }

    private boolean breakCrystal(CrystalPlacementUtil.CrystalAttack crystalAttack) {
        BlockPos blockPos;
        boolean flag;
        boolean flag2;
        label28: {
            boolean flag3 = this.breakPending;
            int i = this.ticksSinceAnchorBlast;
            if (flag3) {
                int i2 = this.getPing();
                blockPos = this.placedCrystalPos;
                boolean flag4 = this.pendingCrystalPlace;
                long l = i * 50L;
                long l2 = i2;
                long l3 = l2 + 150L;
                long l4 = l2 & -2L ^ -3521146527543142539L;
                long l5 = l4 * l4;
                long l6 = (2L - l5) * l5;
                long l7 = (2L - l6) * l6;
                long l8 = (2L - l7) * l7;
                long l9 = (2L - l8) * l8;
                long l10 = (2L - l9) * l9;
                long l11 = 2L - l10;
                long l12 = l10 * l3 * l11;
                if (l <= l12) {
                    flag = false;
                    flag2 = flag4;
                    break label28;
                }
            }

            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = crystalAttack.aimPoint();
            Vec2f vec2f = RotationUtil.getRotations(vec3d, vec3d2);
            EndCrystalEntity endCrystalEntity = crystalAttack.crystal();
            float f = vec2f.x;
            float f2 = vec2f.y;
            float f3 = (float)mc.player.getEntityInteractionRange();
            boolean flag5 = RaycastUtil.isLookingAtEntity(endCrystalEntity, f, f2, f3, false);
            blockPos = this.placedCrystalPos;
            boolean flag6 = this.pendingCrystalPlace;
            if (flag5) {
                Vec3d vec3d3 = crystalAttack.aimPoint();
                this.aimAt(vec3d3);
                ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                ClientPlayerEntity clientPlayerEntity = mc.player;
                EndCrystalEntity endCrystalEntity2 = crystalAttack.crystal();
                clientPlayerInteractionManager.attackEntity(clientPlayerEntity, endCrystalEntity2);
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                Hand hand = Hand.MAIN_HAND;
                clientPlayerEntity2.swingHand(hand);
                float f4 = mc.player.getHealth();
                float f5 = mc.player.getAbsorptionAmount();
                float f6 = crystalAttack.damage().selfDamage();
                BlockPos blockPos2 = this.placedCrystalPos;
                boolean flag7 = this.pendingCrystalPlace;
                float f7 = ExactMath.add(ExactMath.add(f4, f5), -0.5F);
                AutoCrystal autoCrystal;
                boolean flag8;
                if (f6 >= f7) {
                    autoCrystal = this;
                    flag8 = true;
                } else {
                    autoCrystal = this;
                    flag8 = false;
                }

                autoCrystal.breakPending = flag8;
                this.placedCrystalPos = blockPos2;
                this.pendingCrystalPlace = flag7;
                Vec3d vec3d4 = crystalAttack.crystal().getEntityPos();
                float f8 = crystalAttack.damage().targetDamage();
                LivingEntity livingEntity = this.target;
                this.onAnchorExploded(vec3d4, f8, livingEntity);
                this.shieldBreakTimer.reset();
                Int2ObjectOpenHashMap int2ObjectOpenHashMap = this.attackedCrystals;
                int i3 = crystalAttack.crystal().getId();
                StopWatch stopWatch = new StopWatch();
                int2ObjectOpenHashMap.put(i3, stopWatch);
                this.breakTimer.reset();
                BlockPos blockPos3 = crystalAttack.crystal().getBlockPos();
                BlockPos blockPos4 = this.placedCrystalPos;
                boolean flag9 = blockPos3.equals(blockPos4);
                blockPos = this.placedCrystalPos;
                if (flag9) {
                    blockPos = null;
                    flag = true;
                    flag2 = true;
                } else {
                    flag = true;
                    flag2 = true;
                }
            } else {
                flag = false;
                flag2 = flag6;
            }
        }

        this.placedCrystalPos = blockPos;
        this.pendingCrystalPlace = flag2;
        return flag;
    }

    private boolean executeCrystalPlacement(CrystalPlacementUtil.PlacementResult placementResult) {
        if (placementResult == null) {
            return false;
        }

        this.renderPos = placementResult.pos();
        if (placementResult.mineHit() != null) {
            return this.startMiningBase(placementResult);
        }

        if (placementResult.supportHit() != null) {
            if (!this.placeItem(Items.OBSIDIAN, placementResult.supportHit())) {
                return false;
            }

            this.supportPos = placementResult.pos();
            this.lastMinedPos = null;
            this.pendingCrystalPlace = true;
            return true;
        } else {
            if (!this.placeItem(Items.END_CRYSTAL, placementResult.hit())) {
                return false;
            }

            this.markCrystalPlaced(placementResult.pos(), placementResult.hit().getPos());
            return true;
        }
    }

    private void markCrystalPlaced(BlockPos blockPos, Vec3d vec3d) {
        this.supportPos = null;
        this.placedCrystalPos = blockPos.up();
        this.placedCrystalVec = CrystalPlacementUtil.getAttackPoint(blockPos, vec3d);
        this.placedCrystalTimer.reset();
    }

    private boolean startMiningBase(CrystalPlacementUtil.PlacementResult placementResult) {
        BlockPos blockPos = placementResult.pos();
        int i = CrystalPlacementUtil.findBestPickaxeSlot();
        if (i == -1) {
            return false;
        } else {
            int i2 = CrystalPlacementUtil.getBreakTicks(mc.player.getInventory().getStack(i), blockPos);
            BlockHitResult blockHitResult = i2 > 4 ? null : this.aimAtBlockHit(placementResult.mineHit());
            int i3 = blockHitResult == null ? -1 : this.moveToHotbar(i);
            if (i3 == -1) {
                return false;
            } else {
                this.switchToSlot(i3);
                boolean flag = i2 == 0;
                PacketSender.sendSequenced(i4 -> {
                    if (flag) {
                        mc.interactionManager.breakBlock(blockPos);
                    }

                    return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, blockHitResult.getSide(), i4);
                });
                mc.player.swingHand(Hand.MAIN_HAND);
                this.mineTimer.reset();
                if (flag) {
                    this.onBaseMined(blockPos);
                    return true;
                } else {
                    this.miningPos = blockPos;
                    this.miningSide = blockHitResult.getSide();
                    this.miningHitVec = blockHitResult.getPos();
                    this.miningFinishTick = mc.player.age + i2;
                    return true;
                }
            }
        }
    }

    private void continueMining() {
        this.renderPos = this.miningPos;
        Vec3d vec3d = mc.player.getEyePos();
        double d = mc.player.getBlockInteractionRange();
        boolean flag = mc.world.getBlockState(this.miningPos).isReplaceable();
        boolean flag2 = vec3d.squaredDistanceTo(CombatUtil.clampToBox(vec3d, new Box(this.miningPos))) > d * d;
        int i = mc.player.age - this.miningFinishTick;
        if (!flag && !flag2 && i <= 10) {
            this.renderHitVec = this.miningHitVec;
            this.aimAt(this.miningHitVec);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.world.setBlockBreakingInfo(mc.player.getId(), this.miningPos, MathHelper.clamp(9 + i * 3, 0, 9));
            if (i >= 0 && mc.player.isOnGround()) {
                BlockPos blockPos = this.miningPos;
                Direction direction = this.miningSide;
                PacketSender.sendSequenced(i2 -> {
                    mc.interactionManager.breakBlock(blockPos);
                    return new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, i2);
                });
                mc.world.setBlockBreakingInfo(mc.player.getId(), blockPos, -1);
                this.miningPos = null;
                this.onBaseMined(blockPos);
            }
        } else {
            BlockPos blockPos2 = this.miningPos;
            this.abortMining();
            if (flag) {
                this.onBaseMined(blockPos2);
            }
        }
    }

    private void onBaseMined(BlockPos blockPos) {
        this.supportPos = blockPos;
        this.lastMinedPos = blockPos;
        this.minedTimer.reset();
        this.mineTimer.reset();
    }

    private void abortMining() {
        if (ServerManager.isNullWorld()) {
            this.miningPos = null;
            this.silentSlotSwapped = false;
        } else {
            this.restoreSilentSlot();
            if (this.miningPos != null) {
                mc.world.setBlockBreakingInfo(mc.player.getId(), this.miningPos, -1);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.miningPos, Direction.DOWN));
                this.miningPos = null;
            }
        }
    }

    private void switchToSlot(int i) {
        if (InventoryUtil.getSelectedSlot() != i) {
            if (!this.options.has("silentSlots")) {
                InventoryUtil.setSelectedSlot(i);
            } else {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                this.silentSlotSwapped = true;
            }
        }
    }

    private void restoreSilentSlot() {
        if (this.silentSlotSwapped) {
            this.silentSlotSwapped = false;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(InventoryUtil.getSelectedSlot()));
        }
    }

    private boolean handleAnchorsDirect(LivingEntity livingEntity) {
        boolean flag = mc.player.shouldCancelInteraction();
        List list = this.anchorPositions;
        BlockPos blockPos = this.holePos;
        boolean flag2;
        BlockPos blockPos2;
        if (flag) {
            flag2 = false;
            blockPos2 = blockPos;
        } else {
            int i = 0;
            int i2 = 0;
            CrystalPlacementUtil.PlacementResult placementResult = null;
            CrystalPlacementUtil.PlacementResult placementResult2 = null;
            CrystalPlacementUtil.PlacementResult placementResult3 = null;

            while (true) {
                int i3 = list.size();
                List list2 = this.anchorPositions;
                int i4 = this.ticksSinceKillBlast;
                if (i >= i3) {
                    boolean flag3;
                    List list3;
                    label186: {
                        label151: {
                            if (i4 < 10) {
                                flag3 = true;
                            } else {
                                if (placementResult3 != null) {
                                    boolean flag4 = this.executeAnchorPlacement(placementResult3, livingEntity);
                                    List list4 = this.anchorPositions;
                                    if (flag4) {
                                        if (placementResult3 != placementResult2 && placementResult2 != null) {
                                            blockPos2 = placementResult2.pos();
                                            flag2 = true;
                                        } else {
                                            flag2 = true;
                                            blockPos2 = null;
                                        }
                                        break;
                                    }

                                    flag3 = false;
                                    if (placementResult == null) {
                                        list3 = list4;
                                        break label186;
                                    }
                                    break label151;
                                }

                                flag3 = false;
                            }

                            if (placementResult == null) {
                                list3 = list2;
                                break label186;
                            }
                        }

                        boolean flag5 = this.executeAnchorPlacement(placementResult, livingEntity);
                        List list5 = this.anchorPositions;
                        BlockPos blockPos3 = this.holePos;
                        if (flag5) {
                            blockPos2 = blockPos3;
                            flag2 = true;
                            break;
                        }

                        list3 = list5;
                    }

                    BlockPos blockPos4;
                    label133: {
                        label187: {
                            boolean flag6;
                            int i5;
                            label188: {
                                label189: {
                                    int i6 = list3.size();
                                    float f = this.anchorCount.getValue();
                                    int i7 = this.ticksSinceKillBlast;
                                    blockPos4 = this.holePos;
                                    float f2 = i6 - i2;
                                    if (f > f2) {
                                        CrystalPlacementUtil.PlacementResult placementResult4 = this.findAnchorPlacement(livingEntity);
                                        i7 = this.ticksSinceKillBlast;
                                        BlockPos blockPos5 = this.holePos;
                                        if (placementResult4 != null) {
                                            if (flag3 && placementResult4.supportHit() == null) {
                                                BlockPos blockPos6 = placementResult4.pos();
                                                if (this.getAnchorCharges(blockPos6) > 0) {
                                                    BlockPos blockPos7 = placementResult4.pos();
                                                    this.trackAnchor(blockPos7);
                                                    int i8 = this.ticksSinceKillBlast;
                                                    if (placementResult2 == null) {
                                                        flag6 = true;
                                                        i5 = 10 - i8;
                                                        break label188;
                                                    }

                                                    flag6 = false;
                                                    break label189;
                                                }
                                            }

                                            boolean flag7 = this.executeAnchorPlacement(placementResult4, livingEntity);
                                            int i9 = this.ticksSinceKillBlast;
                                            blockPos4 = this.holePos;
                                            if (flag7) {
                                                blockPos2 = blockPos4;
                                                flag2 = true;
                                                break;
                                            }

                                            i7 = i9;
                                            if (placementResult2 != null) {
                                                flag6 = false;
                                                break label189;
                                            }
                                        } else {
                                            blockPos4 = blockPos5;
                                            if (placementResult2 != null) {
                                                flag6 = false;
                                                break label189;
                                            }
                                        }
                                    } else if (placementResult2 != null) {
                                        flag6 = false;
                                        break label189;
                                    }

                                    flag6 = true;
                                    if (!flag3) {
                                        break label133;
                                    }

                                    i5 = 10 - i7;
                                    break label188;
                                }

                                BlockPos blockPos8 = placementResult2.pos();
                                BlockPos blockPos9 = this.holePos;
                                boolean flag8 = blockPos8.equals(blockPos9);
                                int i10 = this.ticksSinceKillBlast;
                                blockPos4 = this.holePos;
                                if (flag8) {
                                    if (flag3) {
                                        if (placementResult2 == placementResult3) {
                                            break label133;
                                        }
                                        break label187;
                                    }
                                } else if (flag3) {
                                    i5 = 10 - i10;
                                    break label188;
                                }

                                if (placementResult2 == placementResult3) {
                                    break label133;
                                }
                                break label187;
                            }

                            int i11 = this.getPing();
                            blockPos4 = this.holePos;
                            int i12 = i5 & -1078991447;
                            int i13 = i12 ^ -1078991447;
                            long l = i12 + i13 + -1822832064 + i5 + -1393143786 + 1;
                            long l2 = 50L * l;
                            long l3 = l & -2L ^ -2552694796845958195L;
                            long l4 = l3 * l3;
                            long l5 = (2L - l4) * l4;
                            long l6 = (2L - l5) * l5;
                            long l7 = (2L - l6) * l6;
                            long l8 = (2L - l7) * l7;
                            long l9 = (2L - l8) * l8;
                            long l10 = 2L - l9;
                            long l11 = l9 * l2 * l10;
                            long l12 = i11;
                            long l13 = l12 + 50L;
                            long l14 = l12 & -2L ^ 1635043406458950381L;
                            long l15 = l14 * l14;
                            long l16 = (2L - l15) * l15;
                            long l17 = (2L - l16) * l16;
                            long l18 = (2L - l17) * l17;
                            long l19 = (2L - l18) * l18;
                            long l20 = (2L - l19) * l19;
                            long l21 = 2L - l20;
                            long l22 = l20 * l13 * l21;
                            if (l11 < l22 || flag6 || placementResult2 == placementResult3) {
                                break label133;
                            }
                        }

                        boolean flag9 = this.executeAnchorPlacement(placementResult2, livingEntity);
                        blockPos2 = this.holePos;
                        flag2 = flag9;
                        break;
                    }

                    blockPos2 = blockPos4;
                    flag2 = false;
                    break;
                }

                CrystalPlacementUtil.PlacementResult placementResult5;
                CrystalPlacementUtil.PlacementResult placementResult6;
                CrystalPlacementUtil.PlacementResult placementResult7;
                BlockPos blockPos10 = (BlockPos)list2.get(i);
                CrystalPlacementUtil.PlacementResult placementResult8 = this.evaluateAnchor(blockPos10, livingEntity);
                label173:
                if (placementResult8 != null) {
                    int i14 = this.getAnchorCharges(blockPos10);
                    List list6 = this.anchorPositions;
                    List list7;
                    if (i14 > 0) {
                        BlockHitResult blockHitResult = placementResult8.supportHit();
                        List list8 = this.anchorPositions;
                        if (blockHitResult == null) {
                            List list9;
                            label164: {
                                float f3 = placementResult8.damage().targetDamage();
                                List list10 = this.anchorPositions;
                                if (placementResult3 != null) {
                                    float f4 = placementResult3.damage().targetDamage();
                                    List list11 = this.anchorPositions;
                                    if (f3 > f4) {
                                        placementResult3 = placementResult8;
                                        list9 = list11;
                                        if (placementResult2 == null) {
                                            break label164;
                                        }
                                    } else {
                                        list9 = list11;
                                        if (placementResult2 == null) {
                                            break label164;
                                        }
                                    }
                                } else {
                                    placementResult3 = placementResult8;
                                    list9 = list10;
                                    if (placementResult2 == null) {
                                        break label164;
                                    }
                                }

                                float f5 = placementResult2.damage().targetDamage();
                                list = this.anchorPositions;
                                if (f5 >= f3) {
                                    placementResult5 = placementResult;
                                    placementResult6 = placementResult8;
                                    placementResult7 = placementResult3;
                                } else {
                                    placementResult5 = placementResult;
                                    placementResult6 = placementResult2;
                                    placementResult7 = placementResult3;
                                }
                                break label173;
                            }

                            list = list9;
                            placementResult5 = placementResult;
                            placementResult6 = placementResult8;
                            placementResult7 = placementResult3;
                            break label173;
                        }

                        list7 = list8;
                        if (placementResult != null) {
                            list = list8;
                            placementResult5 = placementResult;
                            placementResult6 = placementResult2;
                            placementResult7 = placementResult3;
                            break label173;
                        }
                    } else {
                        list7 = list6;
                        if (placementResult != null) {
                            list = list6;
                            placementResult5 = placementResult;
                            placementResult6 = placementResult2;
                            placementResult7 = placementResult3;
                            break label173;
                        }
                    }

                    list = list7;
                    placementResult5 = placementResult8;
                    placementResult6 = placementResult2;
                    placementResult7 = placementResult3;
                } else {
                    boolean flag10 = this.isUsableAnchor(blockPos10);
                    List list12 = this.anchorPositions;
                    if (!flag10) {
                        this.setPendingAnchor(blockPos10);
                        this.anchorPositions.remove(i);
                        List list13 = this.anchorPositions;
                        int i15 = i;
                        placementResult5 = placementResult;
                        CrystalPlacementUtil.PlacementResult placementResult9 = placementResult2;
                        list = list13;
                        i = i15;
                        placementResult = placementResult5;
                        placementResult2 = placementResult9;
                        continue;
                    }

                    i2++;
                    list = list12;
                    placementResult5 = placementResult;
                    placementResult6 = placementResult2;
                    placementResult7 = placementResult3;
                }

                int i16 = i + 1;
                placementResult3 = placementResult7;
                CrystalPlacementUtil.PlacementResult placementResult10 = placementResult6;
                i = i16;
                placementResult = placementResult5;
                placementResult2 = placementResult10;
            }
        }

        this.holePos = blockPos2;
        return flag2;
    }

    private boolean isUsableAnchor(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        if (!blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
            return false;
        }

        double d = mc.player.getBlockInteractionRange() + 2.0;
        Vec3d vec3d = mc.player.getEyePos();
        boolean flag = vec3d.squaredDistanceTo(CombatUtil.clampToBox(vec3d, new Box(blockPos))) <= d * d;
        return blockState.get(RespawnAnchorBlock.CHARGES) <= 0 && InventoryUtil.countItem(Items.GLOWSTONE) <= 0 ? false : flag;
    }

    private boolean handleTrackedAnchors(LivingEntity livingEntity) {
        CrystalPlacementUtil.PlacementResult placementResult = null;
        CrystalPlacementUtil.PlacementResult placementResult2 = null;
        int i = 0;

        while (i < this.anchorPositions.size()) {
            BlockPos blockPos = this.anchorPositions.get(i);
            CrystalPlacementUtil.PlacementResult placementResult3 = this.evaluateAnchor(blockPos, livingEntity);
            if ((placementResult3 == null || placementResult3.supportHit() == null) && (placementResult3 != null || this.isUsableAnchor(blockPos))) {
                if (placementResult3 != null && this.getAnchorCharges(blockPos) > 0) {
                    if (placementResult == null) {
                        placementResult = placementResult3;
                    }
                } else if (placementResult3 != null && placementResult2 == null) {
                    placementResult2 = placementResult3;
                }

                i++;
            } else {
                this.anchorPositions.remove(i);
            }
        }

        boolean flag = placementResult != null && this.ticksSinceKillBlast >= 10;
        if (flag && this.executeAnchorPlacement(placementResult, livingEntity)) {
            return true;
        } else {
            return placementResult2 != null && this.executeAnchorPlacement(placementResult2, livingEntity) ? true : flag && this.isRecentAnchorBlast();
        }
    }

    private CrystalPlacementUtil.PlacementResult evaluateAnchor(BlockPos blockPos, LivingEntity livingEntity) {
        return !CrystalPlacementUtil.isInteractable(blockPos, livingEntity)
            ? null
            : CrystalPlacementUtil.evaluateAnchorPosition(
                blockPos,
                livingEntity,
                Vec3d.ZERO,
                blockPos,
                this.canDetonateAnchor(),
                CrystalPlacementUtil.hasTotemAvailable(),
                this.options.has("throughWalls"),
                this.getDamageLimits()
            );
    }

    private CrystalPlacementUtil.PlacementResult findAnchorPlacement(LivingEntity livingEntity) {
        if (this.anchorPlacementCached) {
            return this.cachedAnchorPlacement;
        }

        this.anchorPlacementCached = true;
        this.cachedAnchorPlacement = null;
        if (!this.hasAnchorSupplies()) {
            return null;
        }

        if (this.pendingAnchorPos != null && this.pendingAnchorTimer.hasPassed(1000.0)) {
            this.pendingAnchorPos = null;
        }

        this.cachedAnchorPlacement = CrystalPlacementUtil.findBestAnchorPlacement(
            livingEntity,
            this.pendingAnchorPos,
            this.anchorPositions,
            this.canDetonateAnchor(),
            CrystalPlacementUtil.hasTotemAvailable(),
            this.options.has("throughWalls"),
            this.getDamageLimits(),
            3 + this.getPingTicks()
        );
        return this.cachedAnchorPlacement;
    }

    private boolean hasAnchorSupplies() {
        return InventoryUtil.countItem(Items.RESPAWN_ANCHOR) > 0 && InventoryUtil.countItem(Items.GLOWSTONE) > 0;
    }

    private boolean executeAnchorPlacement(CrystalPlacementUtil.PlacementResult placementResult, LivingEntity livingEntity) {
        if (placementResult == null) {
            return false;
        }

        boolean flag = this.isRecentAnchorBlast();
        this.renderPos = placementResult.pos();
        if (placementResult.supportHit() != null) {
            if (CrystalPlacementUtil.isReplaceablePlant(mc.world.getBlockState(placementResult.pos()))) {
                if (!this.breakBlockInstantly(placementResult.supportHit())) {
                    return false;
                }

                this.setPendingAnchor(placementResult.pos());
                return true;
            } else {
                if (this.isBlastAffected(placementResult.supportHit().getBlockPos())) {
                    return false;
                }

                if (!this.placeItem(Items.RESPAWN_ANCHOR, placementResult.supportHit())) {
                    return false;
                }

                this.trackAnchor(placementResult.pos());
                this.pendingAnchorPlacements.add(placementResult.pos().toImmutable());
                return true;
            }
        } else {
            this.trackAnchor(placementResult.pos());
            if (flag && this.lastBlastLethal) {
                return false;
            }

            if (this.countFacingGlowstone(placementResult.pos()) < this.anchorProtection.getValue()) {
                CrystalPlacementUtil.AnchorBlockPlacement anchorBlockPlacement = CrystalPlacementUtil.findAnchorShieldPlacement(
                    placementResult.pos(), livingEntity, CrystalPlacementUtil.hasTotemAvailable(), this.options.has("throughWalls"), this.getDamageLimits()
                );
                if (anchorBlockPlacement != null && !this.isBlastAffected(anchorBlockPlacement.hit().getBlockPos())) {
                    this.renderPos = anchorBlockPlacement.pos();
                    return this.placeItem(Items.OBSIDIAN, anchorBlockPlacement.hit());
                }
            }

            if (mc.player.shouldCancelInteraction()) {
                return false;
            }

            if (this.getAnchorCharges(placementResult.pos()) == 0) {
                if (this.pendingAnchorPlacements.contains(placementResult.pos())) {
                    return false;
                }

                if (!this.placeItem(Items.GLOWSTONE, placementResult.hit())) {
                    return false;
                }

                this.trackAnchor(placementResult.pos());
                return true;
            } else {
                if (flag) {
                    return false;
                }

                if (!this.detonateAnchor(placementResult.hit())) {
                    return false;
                }

                this.anchorPositions.remove(placementResult.pos());
                this.onAnchorExploded(Vec3d.ofCenter(placementResult.pos()), placementResult.damage().targetDamage(), livingEntity);
                return true;
            }
        }
    }

    private void onAnchorExploded(Vec3d vec3d, float f, LivingEntity livingEntity) {
        this.ticksSinceAnchorBlast = 0;
        this.lastAnchorBlastPos = vec3d;
        boolean flag = livingEntity != null && f >= livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        this.lastBlastLethal = flag;
        if (flag || f >= this.minTargetDamage.getValue()) {
            this.resetKillBlastTicks();
        }
    }

    private boolean isRecentAnchorBlast() {
        return this.ticksSinceAnchorBlast < 10 && this.ticksSinceAnchorBlast * 50L < this.getPing() + 50L;
    }

    private boolean isBlastAffected(BlockPos blockPos) {
        if (!this.isRecentAnchorBlast() || this.lastAnchorBlastPos == null) {
            return false;
        } else {
            return this.lastBlastLethal
                ? true
                : blockPos.isWithinDistance(this.lastAnchorBlastPos, 10.0) && mc.world.getBlockState(blockPos).getBlock().getBlastResistance() < 600.0F;
        }
    }

    private int getAnchorCharges(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        return blockState.isOf(Blocks.RESPAWN_ANCHOR) ? blockState.get(RespawnAnchorBlock.CHARGES) : 0;
    }

    private void trackAnchor(BlockPos blockPos) {
        BlockPos blockPos2 = blockPos.toImmutable();
        this.trackedAnchors.add(blockPos2);
        if (!this.anchorPositions.contains(blockPos2)) {
            this.anchorPositions.add(blockPos2);
        }
    }

    private void setPendingAnchor(BlockPos blockPos) {
        this.pendingAnchorPos = blockPos.toImmutable();
        this.pendingAnchorTimer.reset();
    }

    private int countFacingGlowstone(BlockPos blockPos) {
        Vec3d vec3d = mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).normalize();
        int i = 0;

        for (Direction direction : Direction.values()) {
            boolean flag = direction.getDoubleVector().dotProduct(vec3d) >= 0.0;
            if (flag && mc.world.getBlockState(blockPos.offset(direction)).isOf(Blocks.OBSIDIAN)) {
                i++;
            }
        }

        return i;
    }

    private boolean placeItem(Item item, BlockHitResult blockHitResult) {
        if (mc.player.getOffHandStack().isOf(item)) {
            return this.interactBlock(Hand.OFF_HAND, blockHitResult, item);
        }

        int i = this.findItemSlot(item);
        return i != -1 && this.interactWithSlot(i, blockHitResult, item);
    }

    private boolean interactWithSlot(int i, BlockHitResult blockHitResult, Item item) {
        if (this.options.has("silentSlots")) {
            return InventoryUtil.withHotbarSlot(i, () -> this.interactBlock(Hand.MAIN_HAND, blockHitResult, item));
        }

        if (InventoryUtil.getSelectedSlot() != i) {
            InventoryUtil.setSelectedSlot(i);
        }

        return this.interactBlock(Hand.MAIN_HAND, blockHitResult, item);
    }

    private boolean interactBlock(Hand hand, BlockHitResult blockHitResult, Item item) {
        boolean flag = mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
        BlockHitResult blockHitResult2 = this.aimAtBlockHit(blockHitResult);
        return blockHitResult2 == null || !flag && blockHitResult2.getSide() != blockHitResult.getSide() ? false : this.sendInteract(hand, blockHitResult2, item);
    }

    private BlockHitResult aimAtBlockHit(BlockHitResult blockHitResult) {
        float f = (float)mc.player.getBlockInteractionRange();
        this.renderHitVec = blockHitResult.getPos();
        this.aimAt(blockHitResult.getPos());
        BlockHitResult blockHitResult2;
        if (this.options.has("throughWalls")) {
            blockHitResult2 = AimPointUtil.raycastBlock(blockHitResult.getBlockPos(), mc.player.getYaw(), mc.player.getPitch(), f);
        } else {
            HitResult hitResult = RaycastUtil.raycastBlocks(mc.player.getYaw(), mc.player.getPitch(), f, false);
            blockHitResult2 = hitResult instanceof BlockHitResult ? (BlockHitResult)hitResult : null;
        }

        return blockHitResult2 != null && blockHitResult2.getBlockPos().equals(blockHitResult.getBlockPos()) ? blockHitResult2 : null;
    }

    private boolean breakBlockInstantly(BlockHitResult blockHitResult) {
        BlockHitResult blockHitResult2 = this.aimAtBlockHit(blockHitResult);
        if (blockHitResult2 == null) {
            return false;
        }

        BlockPos blockPos = blockHitResult2.getBlockPos();
        PacketSender.sendSequenced(i -> {
            mc.world.setBlockState(blockPos, mc.world.getFluidState(blockPos).getBlockState(), 3);
            return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, blockHitResult2.getSide(), i);
        });
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean sendInteract(Hand hand, BlockHitResult blockHitResult, Item item) {
        this.renderHitVec = blockHitResult.getPos();
        boolean flag = mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
        PacketSender.sendSequenced(i -> {
            this.predictInteraction(item, blockHitResult);
            return new PlayerInteractBlockC2SPacket(hand, blockHitResult, i);
        });
        if (flag && this.visuals.isEnabled() && this.placeAnimation.getValue()) {
            this.placeRenderer.onCrystalHit(blockHitResult);
        }

        mc.player.swingHand(hand);
        return true;
    }

    private void predictInteraction(Item item, BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState blockState = mc.world.getBlockState(blockPos);
        if (blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
            int i = blockState.get(RespawnAnchorBlock.CHARGES);
            if (item == Items.GLOWSTONE) {
                if (i < 4) {
                    mc.world.setBlockState(blockPos, blockState.with(RespawnAnchorBlock.CHARGES, i + 1), 3);
                    return;
                }
            } else if (i > 0) {
                mc.world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
            }
        } else if (item instanceof BlockItem blockItem) {
            BlockPos blockPos2 = blockState.isReplaceable() ? blockPos : blockPos.offset(blockHitResult.getSide());
            if (mc.world.getBlockState(blockPos2).isReplaceable()) {
                mc.world.setBlockState(blockPos2, blockItem.getBlock().getDefaultState(), 3);
                return;
            }
        }
    }

    private int findDetonatorSlot() {
        int i = InventoryUtil.findHotbarIndex(Items.TOTEM_OF_UNDYING);
        if (i != -1) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (this.isDetonatorItem(itemStack) && !mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                return i;
            }
        }

        int i2 = InventoryUtil.getSelectedSlot();
        ItemStack itemStack2 = mc.player.getInventory().getStack(i2);
        if (this.isDetonatorItem(itemStack2)) {
            return i2;
        }

        int i3 = Math.min(36, mc.player.getInventory().size());

        for (int i4 = 0; i4 < i3; i4++) {
            ItemStack itemStack3 = mc.player.getInventory().getStack(i4);
            if (this.isDetonatorItem(itemStack3)) {
                return i4;
            }
        }

        return -1;
    }

    private boolean canDetonateAnchor() {
        int i = this.findDetonatorSlot();
        if (i == -1) {
            return false;
        }

        int i2 = i < 9 ? i : this.findFreeHotbarSlot();
        return i2 == -1 ? false : !mc.player.getOffHandStack().isOf(Items.GLOWSTONE) || this.findNonGlowstoneSlot(i2) != -1;
    }

    private boolean isDetonatorItem(ItemStack itemStack) {
        return !itemStack.isEmpty() && !(itemStack.getItem() instanceof BlockItem);
    }

    private boolean detonateAnchor(BlockHitResult blockHitResult) {
        int i = this.findDetonatorSlot();
        int i2 = this.moveToHotbar(i);
        if (i2 == -1) {
            return false;
        }

        boolean flag = mc.player.getOffHandStack().isOf(Items.GLOWSTONE);
        int i3 = flag ? this.findNonGlowstoneSlot(i2) : -1;
        if (flag && i3 == -1) {
            return false;
        }

        if (flag) {
            InventoryUtil.clickSlot(i3, 40, SlotActionType.SWAP);
        }

        try {
            return this.interactWithSlot(i2, blockHitResult, mc.player.getInventory().getStack(i2).getItem());
        } finally {
            if (flag) {
                InventoryUtil.clickSlot(i3, 40, SlotActionType.SWAP);
            }
        }
    }

    private int findNonGlowstoneSlot(int i) {
        for (int i2 = 35; i2 >= 0; i2--) {
            if (i2 != i && !mc.player.getInventory().getStack(i2).isOf(Items.GLOWSTONE)) {
                if (i2 < 9) {
                    return i2 + 36;
                }

                return i2;
            }
        }

        return -1;
    }

    private int findItemSlot(Item item) {
        if (mc.player != null) {
            int i = InventoryUtil.findHotbarIndex(item);
            if (i == -1) {
                int i2 = InventoryUtil.findItemIndex(item, false);
                return this.moveToHotbar(i2);
            } else {
                return i;
            }
        } else {
            return -1;
        }
    }

    private int moveToHotbar(int i) {
        if (i < 0 || i >= 36) {
            return -1;
        }

        if (i < 9) {
            return i;
        }

        int i2 = this.findFreeHotbarSlot();
        if (i2 == -1) {
            return -1;
        }

        InventoryUtil.clickSlot(i, i2, SlotActionType.SWAP);
        return i2;
    }

    private int findFreeHotbarSlot() {
        int i = -1;
        int i2 = InventoryUtil.getSelectedSlot();
        int i3 = ServerManager.isDonut() && AutoTotem.getInstance().isEnabled() ? 8 : 9;

        for (int i4 = 0; i4 < i3; i4++) {
            if (i4 != i2 && mc.player.getInventory().getStack(i4).isEmpty()) {
                i = i4;
                break;
            }
        }

        if (i == -1) {
            for (int i5 = i3 - 1; i5 >= 0; i5--) {
                if (i5 != i2) {
                    return i5;
                }
            }
        }

        return i;
    }

    private void resetAim() {
        this.aimPoint = null;
        this.nextJitterTime = 0L;
        this.yawJitter = 0.0F;
        this.pitchJitter = 0.0F;
    }

    private void aimAt(Vec3d vec3d) {
        GroupSetting groupSetting = this.aim;
        this.aimPoint = vec3d;
        if (groupSetting.isEnabled()) {
            this.applyRotation(vec3d, false);
        }
    }

    private void applyRotation(Vec3d vec3d, boolean flag) {
        long l = System.currentTimeMillis();
        if (flag && l >= this.nextJitterTime) {
            float f = this.aimRandomization.getValue();
            this.yawJitter = CombatUtil.randomFloat(-f, f);
            this.pitchJitter = CombatUtil.randomFloat(-f, f);
            this.nextJitterTime = l + 300L;
        }

        Vec2f vec2f = RotationUtil.getRotations(mc.player.getEyePos(), vec3d);
        float f2 = CombatUtil.randomBoolean() ? CombatUtil.randomLerp(0.3F, 0.4F) : CombatUtil.randomLerp(0.6F, 0.8F);
        float f3 = MathHelper.lerp(CombatUtil.randomFloat(), CombatUtil.randomLerp(60.0F, 75.0F), 0.1F) * f2 * (100.0F / this.aimSmoothness.getValue());
        Rotation rotation = flag
            ? new Rotation(vec2f.x + this.yawJitter, MathHelper.clamp(vec2f.y + this.pitchJitter, -90.0F, 90.0F))
            : new Rotation(vec2f.x, MathHelper.clamp(vec2f.y, -90.0F, 90.0F));
        RotationComponent.rotate(rotation, f3, f3 / 2.0F, 20.0F, 20.0F, 2, 1000, false);
    }

    public MultiSetting getOptions() {
        return this.options;
    }

    public MultiSetting getPause() {
        return this.pause;
    }

    public GroupSetting getAim() {
        return this.aim;
    }

    public NumberSetting getAimSmoothness() {
        return this.aimSmoothness;
    }

    public NumberSetting getAimRandomization() {
        return this.aimRandomization;
    }

    public GroupSetting getAnchors() {
        return this.anchors;
    }

    public NumberSetting getAnchorCount() {
        return this.anchorCount;
    }

    public NumberSetting getAnchorProtection() {
        return this.anchorProtection;
    }

    public GroupSetting getHole() {
        return this.hole;
    }

    public NumberSetting getHoleRange() {
        return this.holeRange;
    }

    public NumberSetting getHoleWalkDistance() {
        return this.holeWalkDistance;
    }

    public BooleanSetting getHolePearl() {
        return this.holePearl;
    }

    public NumberSetting getMinTargetDamage() {
        return this.minTargetDamage;
    }

    public NumberSetting getMaxSelfDamage() {
        return this.maxSelfDamage;
    }

    public NumberSetting getDelay() {
        return this.delay;
    }

    public GroupSetting getVisuals() {
        return this.visuals;
    }

    public BooleanSetting getPlaceAnimation() {
        return this.placeAnimation;
    }

    public StopWatch getPendingAnchorTimer() {
        return this.pendingAnchorTimer;
    }

    public StopWatch getPearlTimer() {
        return this.pearlTimer;
    }

    public StopWatch getPlacedCrystalTimer() {
        return this.placedCrystalTimer;
    }

    public StopWatch getBreakTimer() {
        return this.breakTimer;
    }

    public int getTicksSincePlace() {
        return this.ticksSincePlace;
    }

    public int getTicksSinceKillBlast() {
        return this.ticksSinceKillBlast;
    }

    public int getTicksSinceAnchorBlast() {
        return this.ticksSinceAnchorBlast;
    }

    public BlockPos getHoleWalkTarget() {
        return this.holeWalkTarget;
    }

    public BlockPos getSupportPos() {
        return this.supportPos;
    }

    public BlockPos getPlacedCrystalPos() {
        return this.placedCrystalPos;
    }

    public Vec3d getPlacedCrystalVec() {
        return this.placedCrystalVec;
    }

    public BlockPos getPendingAnchorPos() {
        return this.pendingAnchorPos;
    }

    public BlockPos getRenderPos() {
        return this.renderPos;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public int getTicksSinceTargetChange() {
        return this.ticksSinceTargetChange;
    }

    public int getLastAttackedEntityId() {
        return this.lastAttackedEntityId;
    }

    public Vec3d getAimPoint() {
        return this.aimPoint;
    }

    public long getNextJitterTime() {
        return this.nextJitterTime;
    }

    public float getYawJitter() {
        return this.yawJitter;
    }

    public float getPitchJitter() {
        return this.pitchJitter;
    }
}
