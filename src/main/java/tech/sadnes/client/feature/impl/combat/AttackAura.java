package tech.sadnes.client.feature.impl.combat;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.event.impl.SprintEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.FreeLookComponent;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.feature.impl.combat.aura.rotation.FunTimeRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.LegitRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.MimicalRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.NormalRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.SnapsRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.SpookyRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.TestRotation;
import tech.sadnes.client.feature.impl.combat.aura.rotation.TrainedRotation;
import tech.sadnes.client.feature.impl.combat.rotation.HumanizedAimHandler;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ButtonSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.RangeSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.setting.WaveSetting;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.player.MovementSimulator;
import tech.sadnes.client.util.player.RaycastUtil;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

@FeatureInfo(
    name = "AttackAura",
    description = "feature.impl.combat.attackaura",
    displayName = "feature.impl.combat.attackaura",
    category = Category.COMBAT,
    subcategory = Subcategory.AURA
)
public final class AttackAura extends Feature {
    private final MultiSetting targets = new MultiSetting(
        "aura.targets",
        BooleanSetting.of("players", true),
        BooleanSetting.of("invisible", true),
        BooleanSetting.of("naked", true),
        BooleanSetting.of("teammates", false),
        BooleanSetting.of("animals", false),
        BooleanSetting.of("mobs", false)
    );
    private final ModeSetting sortMode = new ModeSetting("aura.sortMode", new String[]{"adaptive", "distance", "health", "aim"}).setValue("aim");
    public final NumberSetting range = new NumberSetting("aura.range", 3.0F, 2.0F, 6.0F, 0.1F);
    private final NumberSetting preRange = new NumberSetting("aura.preRange", 1.0F, 0.0F, 3.0F, 0.1F)
        .setVisibleWhen(() -> !this.componentMode.is("zaporozhets") ? true : false);
    private final BooleanSetting throughWalls = new BooleanSetting("aura.throughWalls", true);
    private static final String[] COMPONENT_MODES = new String[]{
        "normal", "funtime", "ftsnaps", "spooky", "snaps", "legit", "zaporozhets", "mimical", "trained", "test"
    };
    private static final String[] ESP_STYLES = new String[]{
        "diamond", "circle", "old_circle", "ghosts", "crystal", "lightning", "infinity", "sunlight", "discharge"
    };
    private final ModeSetting componentMode = new ModeSetting("aura.componentMode", COMPONENT_MODES);
    public final ModeSetting moveCorrection = new ModeSetting("aura.moveCorrection", new String[]{"focus", "targeted", "subtle", "none"}).setValue("subtle");
    public final ModeSetting sprint = new ModeSetting("aura.sprint", new String[]{"packet", "legit", "keep"});
    private final NumberSetting snapsFov = new NumberSetting("aura.snapsFov", 360.0F, 30.0F, 360.0F, 5.0F, Unit.DEGREE)
        .setVisibleWhen(() -> this.componentMode.is("snaps"));
    private final NumberSetting legitSpeed = new NumberSetting("aura.legitSpeed", 110.0F, 35.0F, 240.0F, 1.0F, Unit.DEGREE_PER_SECOND)
        .setVisibleWhen(() -> this.componentMode.is("legit"));
    private final NumberSetting ftsnapsFov = new NumberSetting("aura.ftsnapsFov", 180.0F, 10.0F, 360.0F, 5.0F, Unit.DEGREE)
        .setVisibleWhen(() -> this.componentMode.is("ftsnaps"));
    private final RangeSetting ftsnapsHang = new RangeSetting("aura.ftsnapsHang", 2.0F, 8.0F, 1.0F, 20.0F, 1.0F)
        .setVisibleWhen(() -> this.componentMode.is("ftsnaps"));
    private final BooleanSetting hitRandom = new BooleanSetting("aura.hitRandom", true).setVisibleWhen(() -> this.componentMode.is("ftsnaps"));
    private final GroupSetting pitchWave = new GroupSetting("aura.pitchWave", true).setVisibleWhen(() -> this.componentMode.is("funtime"));
    private final WaveSetting pitchWaveShape = new WaveSetting(this.pitchWave, "aura.waveShape", WaveSetting.Axis.YAW).setDisplayName("waveDegrees");
    private final GroupSetting yawWave = new GroupSetting("aura.yawWave", true).setVisibleWhen(() -> this.componentMode.is("funtime"));
    private final WaveSetting yawWaveShape = new WaveSetting(this.yawWave, "aura.waveShape", WaveSetting.Axis.PITCH).setDisplayName("waveDegrees");
    private final Setting<Runnable> trainedPanel = new ButtonSetting("aura.trainedPanel", () -> Sadnes.getInstance().getClickGui().toggleNeuroPanel())
        .setVisibleWhen(() -> this.componentMode.is("trained"));
    private final ModeSetting attackMode = new ModeSetting("aura.attackMode", new String[]{"1.8", "1.9+"}).setValue("1.9+");
    private final BooleanSetting smartCrits = new BooleanSetting("aura.smartCrits", false);
    private final MultiSetting checks = new MultiSetting(
        "aura.checks",
        BooleanSetting.of("no_hit_eating", true),
        BooleanSetting.of("no_hit_container", false),
        BooleanSetting.of("no_hit_elytra", true),
        BooleanSetting.of("weapon_only", false),
        BooleanSetting.of("off_on_death", true),
        BooleanSetting.of("break_shield", false),
        BooleanSetting.of("sync_attack", false),
        BooleanSetting.of("swap_on_hit", false),
        BooleanSetting.of("hit_shared_vehicle", true),
        BooleanSetting.of("show_fov", false).setVisibleWhen(() -> this.componentMode.is("snaps"))
    );
    private final GroupSetting targetEsp = new GroupSetting("aura.targetEsp", true);
    private final ModeSetting espStyle = new ModeSetting(this.targetEsp, "aura.espStyle", ESP_STYLES);
    private final ModeSetting espDepth = new ModeSetting(this.targetEsp, "aura.espDepth", new String[]{"auto", "always", "never"})
        .setVisibleWhen(() -> !this.espStyle.is("crystal") && !this.espStyle.is("lightning") ? false : true);
    private final ModeSetting espColorMode = new ModeSetting(this.targetEsp, "aura.espColorMode", new String[]{"client", "fade", "static"});
    private final ColorSetting espColor = new ColorSetting(this.targetEsp, "aura.espColor", -12417035).setVisibleWhen(() -> this.espColorMode.is("static"));
    private final ColorSetting espFadeColor1 = new ColorSetting(this.targetEsp, "aura.espFadeColor1", -12417035)
        .setVisibleWhen(() -> this.espColorMode.is("fade"));
    private final ColorSetting espFadeColor2 = new ColorSetting(this.targetEsp, "aura.espFadeColor2", -5218320).setVisibleWhen(() -> this.espColorMode.is("fade"));
    public LivingEntity target;
    private LivingEntity lastTarget;
    private float inRangeTicks = 0.0F;
    private final Vector2f serverRotation = new Vector2f();
    private boolean boostedJitter;
    public float rotationProgress;
    private int attackCount = 0;
    private final FunTimeHitSwapHandler swapController = new FunTimeHitSwapHandler();
    private final TimerUtil rotationAnimator = new TimerUtil();
    private final ActionSequence tickTracker = new ActionSequence();
    private AttackAura.SnapDirection snapDirection = AttackAura.SnapDirection.READY;
    public final StopWatch hurtTimer = new StopWatch();
    public boolean recentlyHurt = false;
    private final StopWatch attackCooldownTimer = new StopWatch();
    private float fovCircleRadius;
    private long lastFovFrameNanos;
    private final Map<String, TaskHandler> rotationHandlers = Map.of(
        "normal",
        new NormalRotation(this),
        "funtime",
        new FunTimeRotation(this),
        "ftsnaps",
        new HumanizedAimHandler(this),
        "spooky",
        new SpookyRotation(this),
        "snaps",
        new SnapsRotation(this),
        "legit",
        new LegitRotation(this),
        "mimical",
        new MimicalRotation(this),
        "trained",
        new TrainedRotation(this),
        "test",
        new TestRotation(this)
    );
    private double jitterX;
    private double jitterY;
    private long lastJitterTime;

    public AttackAura() {
        this.updateModeVisibility();
    }

    public static AttackAura getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AttackAura.class);
    }

    private void updateModeVisibility() {
        BooleanSetting booleanSetting = this.componentMode.getModeToggle("test");
        if (booleanSetting != null) {
            booleanSetting.setVisibleWhen(AttackAura::isAdminRole);
        }

        if (!isAdminRole() && this.componentMode.is("test")) {
            this.componentMode.setValue(COMPONENT_MODES[0]);
        }

        for (String string : new String[]{"funtime", "ftsnaps"}) {
            BooleanSetting booleanSetting2 = this.componentMode.getModeToggle(string);
            if (booleanSetting2 != null) {
                booleanSetting2.setVisibleWhen(() -> false);
            }
        }
    }

    private static boolean isAdminRole() {
        Role role = Profile.getRole();
        return role != null && role.ordinal() >= Role.ADMIN.ordinal();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.rotationHandlers.values().forEach(TaskHandler::reset);
        if (mc.player != null) {
            this.serverRotation.set(mc.player.getYaw(), mc.player.getPitch());
            if (this.throughWalls.getValue() && ServerManager.isHolyWorld() && !ProtocolUtil.isVersion("1.20.6")) {
                ChatUtil.print(Language.translate("aura.holyWallsVersion", "Для рабочих ударов через стены зайди с версии 1.20.6!"));
            }
        }
    }

    @Override
    public void onEnable() {
        TaskHandler taskHandler = this.getRotationHandler();
        if (taskHandler != null) {
            taskHandler.restart();
        }

        this.reset();
        super.onEnable();
    }

    @EventHandler
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player != null && this.target != null && !MoveUtil.isRidingNautilus()) {
            if (this.moveCorrection.is("focus")) {
                moveInputEvent.setForward(1.0F);
                moveInputEvent.setStrafe(0.0F);
                MoveUtil.fixMovementToTarget(moveInputEvent, mc.player.getYaw(), this.target);
            }

            if (this.moveCorrection.is("targeted")) {
                MoveUtil.fixMovementToTarget(moveInputEvent, mc.player.getYaw(), this.target);
            }

            if (this.moveCorrection.is("none") && this.componentMode.is("ftsnaps") && FreeLookComponent.isActive()) {
                MoveUtil.fixMovement(moveInputEvent, FreeLookComponent.getYaw());
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
            this.serverRotation.set(playerMoveC2SPacket.getYaw(this.serverRotation.x), playerMoveC2SPacket.getPitch(this.serverRotation.y));
        } else {
            if (packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket playerPositionLookS2CPacket) {
                this.serverRotation.set(playerPositionLookS2CPacket.change().yaw(), playerPositionLookS2CPacket.change().pitch());
            }
        }
    }

    @EventHandler(priority = -500)
    public void onSprint(SprintEvent sprintEvent) {
        if (this.sprint.is("legit") && CombatUtil.isCritReady(this.target, (float)this.getAttackRange())) {
            sprintEvent.setSprinting(false);
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        boolean flag = this.snapsFov.getValue() < 180.0F;
        if (mc.player != null && this.componentMode.is("snaps") && this.checks.getOption("show_fov").getValue() && flag) {
            float f = mc.getWindow().getScaledWidth() / 2.0F;
            float f2 = mc.getWindow().getScaledHeight() / 2.0F;
            float f3 = f2 / (float)Math.tan(Math.toRadians(mc.options.getFov().getValue().intValue() / 2.0));
            float f4 = Math.min(this.snapsFov.getValue() / 2.0F, 89.0F);
            float f5 = Math.min(f3 * (float)Math.tan(Math.toRadians(f4)), f + f2);
            long l = System.nanoTime();
            float f6 = Math.min((float)(l - this.lastFovFrameNanos) / 1.0E9F, 0.1F);
            this.lastFovFrameNanos = l;
            AttackAura attackAura;
            float f7;
            if (this.fovCircleRadius <= 0.0F) {
                attackAura = this;
                f7 = f5;
            } else {
                attackAura = this;
                f7 = this.fovCircleRadius + (f5 - this.fovCircleRadius) * (1.0F - (float)Math.exp(-f6 * 18.0F));
            }

            attackAura.fovCircleRadius = f7;
            float f8 = this.fovCircleRadius;
            int i = ColorUtil.multiplyAlpha(Theme.getPrimary(), 0.75F);
            new RenderUtil(render2DEvent.getContext(), f - f8, f2 - f8, f8 * 2.0F, f8 * 2.0F).shadow(1.0F, i, Radius.all(f8));
        } else {
            this.fovCircleRadius = 0.0F;
        }
    }

    @EventHandler
    public void onRenderWorld(Render3DEvent render3DEvent) {
        TaskHandler taskHandler = this.getRotationHandler();
        if (taskHandler != null && taskHandler.getPhase() == TaskHandler.Phase.RENDER) {
            if (mc.player != null && this.target != null) {
                taskHandler.update(this.target);
            } else {
                taskHandler.reset();
            }
        }
    }

    @EventHandler
    public void onUpdate(HudPostRenderEvent hudPostRenderEvent) {
        this.tickTracker.tick();
        if (mc.player == null || mc.world == null) {
            this.reset();
        } else if (!ProtocolUtil.isAvailable() && this.componentMode.is("zaporozhets")) {
            NotificationUtil.error(this.getDisplayName(), Language.translate("aura.needVersion117", "Нужно зайти на сервер с версии 1.17 и выше!"));
            this.setEnabled(false);
        } else {
            TriggerBot triggerBot = TriggerBot.getInstance();
            if (triggerBot != null && triggerBot.isEnabled()) {
                triggerBot.setEnabled(false);
            }

            if (this.checks.getOption("off_on_death").getValue() && !mc.player.isAlive()) {
                this.setEnabled(false);
            } else {
                this.updateTarget();
                if (this.target == null) {
                    this.reset();
                } else if (this.isTargetReachable()) {
                    this.inRangeTicks++;
                } else {
                    this.inRangeTicks = 0.0F;
                }
            }
        }
    }

    @EventHandler
    public void onUpdateAttack(HudPostRenderEvent hudPostRenderEvent) {
        if (Criticals.isPostMode()) {
            this.tryAttack();
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!Criticals.isPostMode()) {
            this.tryAttack();
        }

        this.updateSwapState();
        TaskHandler taskHandler = this.getRotationHandler();
        if (taskHandler != null && taskHandler.getPhase() == TaskHandler.Phase.TICK) {
            if (this.target == null) {
                taskHandler.onStart();
            } else {
                taskHandler.update(this.target);
            }
        }
    }

    private void updateSwapState() {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        LivingEntity livingEntity = this.target;
        FunTimeHitSwapHandler funTimeHitSwapHandler = this.swapController;
        if (clientPlayerEntity != null) {
            if (livingEntity != null) {
                boolean flag = ServerManager.isServer("funtime");
                ModeSetting modeSetting = this.attackMode;
                if (!flag && !ServerManager.isLocalProxy()) {
                    funTimeHitSwapHandler.tick(false);
                } else {
                    boolean flag2 = modeSetting.is("1.9+");
                    MultiSetting multiSetting = this.checks;
                    if (flag2) {
                        if (multiSetting.getOption("swap_on_hit").getValue()) {
                            if (this.willAttackInTicks(1)) {
                                if (this.shouldSwapOnHit()) {
                                    funTimeHitSwapHandler.tick(false);
                                } else {
                                    funTimeHitSwapHandler.tick(true);
                                }
                            } else {
                                funTimeHitSwapHandler.tick(false);
                            }
                        } else {
                            funTimeHitSwapHandler.tick(false);
                        }
                    } else {
                        funTimeHitSwapHandler.tick(false);
                    }
                }
            } else {
                funTimeHitSwapHandler.tick(false);
            }
        } else {
            funTimeHitSwapHandler.tick(false);
        }
    }

    private boolean shouldSwapOnHit() {
        return FunTimeHitSwapHandler.isSwapBlocked() ? true : this.isAttackBlocked();
    }

    public boolean willAttackInTicks(int i) {
        if (this.isAttackBlocked()) {
            return false;
        }

        long l = i * -50L;
        if (AttackTimer.hasCooldownPassed(l)) {
            boolean flag = this.canAttackIn(l);
            LivingEntity livingEntity = this.target;
            if (!flag) {
                double d = CombatUtil.getRayDistance(livingEntity);
                double d2 = this.getAttackRange();
                if (!(d > d2)) {
                    MovementSimulator movementSimulator = MovementSimulator.simulate(i);
                    return !movementSimulator.isOnGround() ? movementSimulator.getFallDistance() > 0.0F : false;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @EventHandler
    public void onAttack(PostMotionEvent postMotionEvent) {
        this.swapController.onAttackFinished();
    }

    @EventHandler(priority = -500)
    public void onPostUpdate(HudPostRenderEvent hudPostRenderEvent) {
        if (mc.player != null) {
            TaskHandler taskHandler = this.getRotationHandler();
            if (this.target == null) {
                if (taskHandler != null && taskHandler.getPhase() != TaskHandler.Phase.TICK) {
                    taskHandler.onStart();
                }
            } else if (ScriptAuraHooks.hasRotationMode(this.componentMode.getValue())) {
                ScriptAuraHooks.applyRotation(this.componentMode.getValue(), this.target, this.getAttackRange());
            } else {
                if (taskHandler != null && taskHandler.getPhase() == TaskHandler.Phase.FRAME) {
                    taskHandler.update(this.target);
                }
            }
        }
    }

    private TaskHandler getRotationHandler() {
        return this.rotationHandlers.get(this.componentMode.getValue().toLowerCase(Locale.ROOT));
    }

    private boolean isAttackBlocked() {
        if (mc.player != null) {
            boolean flag;
            if (mc.player.isUsingItem()) {
                Item item = mc.player.getActiveItem().getItem();
                Item item2 = Items.SHIELD;
                if (item != item2) {
                    flag = false;
                } else {
                    flag = true;
                }
            } else {
                flag = false;
            }

            ItemStack itemStack = mc.player.getMainHandStack();
            boolean flag2;
            if (!(itemStack.getItem() instanceof AxeItem)) {
                TagKey tagKey = ItemTags.SWORDS;
                flag2 = itemStack.isIn(tagKey);
            } else {
                flag2 = true;
            }

            boolean flag3 = mc.player.isUsingItem();
            MultiSetting multiSetting = this.checks;
            if (flag3 && !flag && multiSetting.getOption("no_hit_eating").getValue()) {
                return true;
            }

            Screen screen = mc.currentScreen;
            if (screen instanceof HandledScreen) {
                if ((HandledScreen)screen instanceof InventoryScreen) {
                    if (flag2) {
                        return false;
                    }
                } else {
                    if (multiSetting.getOption("no_hit_container").getValue()) {
                        return true;
                    }

                    if (flag2) {
                        return false;
                    }
                }
            } else if (flag2) {
                return false;
            }

            return multiSetting.getOption("weapon_only").getValue();
        } else {
            return true;
        }
    }

    private void tryAttack() {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        LivingEntity livingEntity = this.target;
        if (clientPlayerEntity != null && livingEntity != null) {
            AutoMace autoMace = AutoMace.getInstance();
            if (autoMace != null && autoMace.isActive()) {
                return;
            }

            if (this.canAttack() && !this.isAttackBlocked()) {
                TaskHandler taskHandler = this.getRotationHandler();
                if (taskHandler == null) {
                    this.performAttack();
                    return;
                }

                if (!taskHandler.isActive()) {
                    this.performAttack();
                    return;
                }
            }
        }
    }

    private void performAttack() {
        MultiSetting multiSetting = this.checks;
        boolean flag = multiSetting.getOption("no_hit_eating").getValue();
        LivingEntity livingEntity = this.target;
        if (flag) {
            boolean flag2 = mc.player.isUsingItem();
            livingEntity = this.target;
            if (flag2) {
                Item item = mc.player.getActiveItem().getItem();
                Item item2 = Items.SHIELD;
                livingEntity = this.target;
                if (item == item2) {
                    ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    clientPlayerInteractionManager.stopUsingItem(clientPlayerEntity);
                    return;
                }
            }
        }

        boolean flag3 = multiSetting.getOption("break_shield").getValue();
        AttackAura.AttackHooks attackHooks = CombatUtil.createShieldBreakSwap(livingEntity, flag3);
        boolean flag4 = ServerManager.isServer("funtime");
        FunTimeHitSwapHandler funTimeHitSwapHandler = this.swapController;
        AttackAura.AttackHooks attackHooks2;
        ModeSetting modeSetting;
        if (!flag4 && !ServerManager.isLocalProxy()) {
            AttackAura.AttackHooks attackHooks3 = CombatUtil.createOffhandSwap(multiSetting.isSelected("swap_on_hit"));
            ModeSetting modeSetting2 = this.componentMode;
            attackHooks2 = attackHooks3;
            modeSetting = modeSetting2;
        } else {
            boolean flag5 = this.shouldSwapOnHit();
            AttackAura.AttackHooks attackHooks4 = funTimeHitSwapHandler.createSwapAction(flag5);
            ModeSetting modeSetting3 = this.componentMode;
            attackHooks2 = attackHooks4;
            modeSetting = modeSetting3;
        }

        boolean flag6 = modeSetting.is("zaporozhets");
        LivingEntity livingEntity2 = this.target;
        AttackAura.AttackHooks attackHooks5 = CombatUtil.createRotationPackets(flag6, livingEntity2);
        boolean flag7 = this.throughWalls.getValue();
        LivingEntity livingEntity3 = this.target;
        Vector2f vector2f = this.serverRotation;
        LivingEntity livingEntity4;
        boolean flag8;
        if (flag7) {
            boolean flag9 = ServerManager.isServer("holy");
            LivingEntity livingEntity5 = this.target;
            if (flag9) {
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                LivingEntity livingEntity6 = this.target;
                boolean flag10 = clientPlayerEntity2.canSee(livingEntity6);
                LivingEntity livingEntity7 = this.target;
                flag8 = !flag10;
                livingEntity4 = livingEntity7;
            } else {
                livingEntity4 = livingEntity5;
                flag8 = false;
            }
        } else {
            livingEntity4 = livingEntity3;
            flag8 = false;
        }

        float f = vector2f.x;
        float f2 = vector2f.y;
        AttackAura.AttackHooks attackHooks6 = CombatUtil.createSnapAttack(flag8, livingEntity4, f, f2);
        Runnable[] runnables = CombatUtil.getSprintResetActions(this.sprint.is("packet"));
        AttackAura.AttackHooks attackHooks7 = CombatUtil.createMovementStop();
        TaskHandler taskHandler = this.getRotationHandler();
        LivingEntity livingEntity8 = this.target;
        Hand hand = Hand.MAIN_HAND;
        Runnable runnable = () -> {
            attackHooks2.post.run();
            attackHooks6.post.run();
            attackHooks5.post.run();
            attackHooks7.post.run();
            this.attackCooldownTimer.delay(150L);
            if (taskHandler != null) {
                taskHandler.onStop();
            }
        };
        Runnable runnable2 = () -> {
            attackHooks7.pre.run();
            attackHooks5.pre.run();
            attackHooks6.pre.run();
            attackHooks2.pre.run();
        };
        Runnable runnable3 = () -> {
            attackHooks.post.run();
            runnables[1].run();
            this.attackCount++;
        };
        Runnable runnable4 = () -> {
            attackHooks.pre.run();
            runnables[0].run();
        };
        CombatUtil.attack(livingEntity8, runnable4, runnable3, runnable2, runnable, hand);
    }

    public boolean canAttack() {
        return this.canAttackIn(0L);
    }

    public boolean canAttackIn(long l) {
        if (this.target != null) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            LivingEntity livingEntity = this.target;
            if (clientPlayerEntity != null) {
                double d = CombatUtil.getRayDistance(livingEntity);
                double d2 = this.getAttackRange();
                boolean flag = mc.player.isGliding();
                MultiSetting multiSetting = this.checks;
                if ((!flag || !multiSetting.getOption("no_hit_elytra").getValue() || d2 >= d)
                    && (!ServerManager.isServer("skytime") && !ServerManager.isServer("funsky.su") || !mc.player.isOnGround())) {
                    ModeSetting modeSetting;
                    label178: {
                        boolean flag2 = ServerManager.isServer("spookytime");
                        float f = this.inRangeTicks;
                        modeSetting = this.componentMode;
                        float f2;
                        if (!flag2) {
                            boolean flag3 = ServerManager.isServer("dildo");
                            float f3 = this.inRangeTicks;
                            if (!flag3) {
                                break label178;
                            }

                            f2 = f3;
                        } else {
                            f2 = f;
                        }

                        if (0.0F >= f2) {
                            return false;
                        }
                    }

                    boolean flag4 = modeSetting.is("legit");
                    ModeSetting modeSetting2 = this.sprint;
                    boolean flag5;
                    boolean flag6;
                    float f4;
                    LivingEntity livingEntity2;
                    boolean flag7;
                    boolean flag8;
                    if (!flag4 && !modeSetting.is("test")) {
                        if (modeSetting2.is("legit") && !mc.player.isGliding()) {
                            Criticals criticals = Criticals.getInstance();
                            LivingEntity livingEntity3 = this.target;
                            ModeSetting modeSetting3 = this.attackMode;
                            LivingEntity livingEntity4;
                            if (criticals != null) {
                                boolean flag9 = criticals.isEnabled();
                                LivingEntity livingEntity5 = this.target;
                                if (flag9) {
                                    boolean flag10 = criticals.getType().is("Grim 1.17+ Air");
                                    LivingEntity livingEntity6 = this.target;
                                    if (flag10) {
                                        boolean flag11 = mc.player.isOnGround();
                                        livingEntity4 = this.target;
                                        if (!flag11) {
                                            int i;
                                            if (!modeSetting3.is("1.9+")) {
                                                i = (int)((Long)(Object)modeSetting3 & -256L);
                                            } else {
                                                i = (int)((Long)(Object)modeSetting3 & -256L | 1L);
                                            }

                                            Boolean flag12 = Boolean.TRUE;
                                            Object object = this.smartCrits.getValue();
                                            flag5 = flag12.equals(object);
                                            double d3 = this.getAttackRange();
                                            flag6 = this.throughWalls.getValue();
                                            int i2 = i & -256;
                                            f4 = (float)d3;
                                            if (i2 == 0) {
                                                return CombatUtil.canAttack(livingEntity4, false, true, false, flag5, l, f4, flag6);
                                            }

                                            return CombatUtil.canAttack(livingEntity4, false, true, true, flag5, l, f4, flag6);
                                        }
                                    } else {
                                        livingEntity4 = livingEntity6;
                                    }
                                } else {
                                    livingEntity4 = livingEntity5;
                                }
                            } else {
                                livingEntity4 = livingEntity3;
                            }

                            int i3;
                            byte b;
                            if (!modeSetting3.is("1.9+")) {
                                i3 = (int)((Long)(Object)modeSetting3 & -256L);
                                b = 0;
                            } else {
                                i3 = (int)((Long)(Object)modeSetting3 & -256L | 1L);
                                b = 1;
                            }

                            Boolean flag13 = Boolean.TRUE;
                            Object object2 = this.smartCrits.getValue();
                            flag5 = flag13.equals(object2);
                            double d4 = this.getAttackRange();
                            flag6 = this.throughWalls.getValue();
                            int i4 = i3 & -256 | b;
                            f4 = (float)d4;
                            livingEntity2 = livingEntity4;
                            flag7 = false;
                            flag8 = true;
                            if (i4 == 0) {
                                return CombatUtil.canAttack(livingEntity4, false, true, false, flag5, l, f4, flag6);
                            }
                        } else {
                            Criticals criticals2 = Criticals.getInstance();
                            LivingEntity livingEntity7 = this.target;
                            ModeSetting modeSetting4 = this.attackMode;
                            LivingEntity livingEntity8;
                            if (criticals2 != null) {
                                boolean flag14 = criticals2.isEnabled();
                                LivingEntity livingEntity9 = this.target;
                                if (flag14) {
                                    boolean flag15 = criticals2.getType().is("Grim 1.17+ Air");
                                    LivingEntity livingEntity10 = this.target;
                                    if (flag15) {
                                        boolean flag16 = mc.player.isOnGround();
                                        livingEntity8 = this.target;
                                        if (!flag16) {
                                            int i5;
                                            if (!modeSetting4.is("1.9+")) {
                                                i5 = (int)((Long)(Object)modeSetting4 & -256L);
                                            } else {
                                                i5 = (int)((Long)(Object)modeSetting4 & -256L | 1L);
                                            }

                                            Boolean flag17 = Boolean.TRUE;
                                            Object object3 = this.smartCrits.getValue();
                                            flag5 = flag17.equals(object3);
                                            double d5 = this.getAttackRange();
                                            flag6 = this.throughWalls.getValue();
                                            int i6 = i5 & -256;
                                            f4 = (float)d5;
                                            if (i6 == 0) {
                                                return CombatUtil.canAttack(livingEntity8, false, false, false, flag5, l, f4, flag6);
                                            }

                                            return CombatUtil.canAttack(livingEntity8, false, false, true, flag5, l, f4, flag6);
                                        }
                                    } else {
                                        livingEntity8 = livingEntity10;
                                    }
                                } else {
                                    livingEntity8 = livingEntity9;
                                }
                            } else {
                                livingEntity8 = livingEntity7;
                            }

                            int i7;
                            byte b2;
                            if (!modeSetting4.is("1.9+")) {
                                i7 = (int)((Long)(Object)modeSetting4 & -256L);
                                b2 = 0;
                            } else {
                                i7 = (int)((Long)(Object)modeSetting4 & -256L | 1L);
                                b2 = 1;
                            }

                            Boolean flag18 = Boolean.TRUE;
                            Object object4 = this.smartCrits.getValue();
                            flag5 = flag18.equals(object4);
                            double d6 = this.getAttackRange();
                            flag6 = this.throughWalls.getValue();
                            int i8 = i7 & -256 | b2;
                            f4 = (float)d6;
                            livingEntity2 = livingEntity8;
                            flag7 = false;
                            flag8 = false;
                            if (i8 == 0) {
                                return CombatUtil.canAttack(livingEntity8, false, false, false, flag5, l, f4, flag6);
                            }
                        }
                    } else if (modeSetting2.is("legit") && !mc.player.isGliding()) {
                        Criticals criticals3 = Criticals.getInstance();
                        LivingEntity livingEntity11 = this.target;
                        ModeSetting modeSetting5 = this.attackMode;
                        LivingEntity livingEntity12;
                        if (criticals3 != null) {
                            boolean flag19 = criticals3.isEnabled();
                            LivingEntity livingEntity13 = this.target;
                            if (flag19) {
                                boolean flag20 = criticals3.getType().is("Grim 1.17+ Air");
                                LivingEntity livingEntity14 = this.target;
                                if (flag20) {
                                    boolean flag21 = mc.player.isOnGround();
                                    livingEntity12 = this.target;
                                    if (!flag21) {
                                        int i9;
                                        if (!modeSetting5.is("1.9+")) {
                                            i9 = (int)((Long)(Object)modeSetting5 & -256L);
                                        } else {
                                            i9 = (int)((Long)(Object)modeSetting5 & -256L | 1L);
                                        }

                                        Boolean flag22 = Boolean.TRUE;
                                        Object object5 = this.smartCrits.getValue();
                                        flag5 = flag22.equals(object5);
                                        double d7 = this.getAttackRange();
                                        flag6 = this.throughWalls.getValue();
                                        int i10 = i9 & -256;
                                        f4 = (float)d7;
                                        if (i10 == 0) {
                                            return CombatUtil.canAttack(livingEntity12, true, true, false, flag5, l, f4, flag6);
                                        }

                                        return CombatUtil.canAttack(livingEntity12, true, true, true, flag5, l, f4, flag6);
                                    }
                                } else {
                                    livingEntity12 = livingEntity14;
                                }
                            } else {
                                livingEntity12 = livingEntity13;
                            }
                        } else {
                            livingEntity12 = livingEntity11;
                        }

                        int i11;
                        byte b3;
                        if (!modeSetting5.is("1.9+")) {
                            i11 = (int)((Long)(Object)modeSetting5 & -256L);
                            b3 = 0;
                        } else {
                            i11 = (int)((Long)(Object)modeSetting5 & -256L | 1L);
                            b3 = 1;
                        }

                        Boolean flag23 = Boolean.TRUE;
                        Object object6 = this.smartCrits.getValue();
                        flag5 = flag23.equals(object6);
                        double d8 = this.getAttackRange();
                        flag6 = this.throughWalls.getValue();
                        int i12 = i11 & -256 | b3;
                        f4 = (float)d8;
                        livingEntity2 = livingEntity12;
                        flag7 = true;
                        flag8 = true;
                        if (i12 == 0) {
                            return CombatUtil.canAttack(livingEntity12, true, true, false, flag5, l, f4, flag6);
                        }
                    } else {
                        Criticals criticals4 = Criticals.getInstance();
                        LivingEntity livingEntity15 = this.target;
                        ModeSetting modeSetting6 = this.attackMode;
                        LivingEntity livingEntity16;
                        if (criticals4 != null) {
                            boolean flag24 = criticals4.isEnabled();
                            LivingEntity livingEntity17 = this.target;
                            if (flag24) {
                                boolean flag25 = criticals4.getType().is("Grim 1.17+ Air");
                                LivingEntity livingEntity18 = this.target;
                                if (flag25) {
                                    boolean flag26 = mc.player.isOnGround();
                                    livingEntity16 = this.target;
                                    if (!flag26) {
                                        int i13;
                                        if (!modeSetting6.is("1.9+")) {
                                            i13 = (int)((Long)(Object)modeSetting6 & -256L);
                                        } else {
                                            i13 = (int)((Long)(Object)modeSetting6 & -256L | 1L);
                                        }

                                        Boolean flag27 = Boolean.TRUE;
                                        Object object7 = this.smartCrits.getValue();
                                        flag5 = flag27.equals(object7);
                                        double d9 = this.getAttackRange();
                                        flag6 = this.throughWalls.getValue();
                                        int i14 = i13 & -256;
                                        f4 = (float)d9;
                                        if (i14 == 0) {
                                            return CombatUtil.canAttack(livingEntity16, true, false, false, flag5, l, f4, flag6);
                                        }

                                        return CombatUtil.canAttack(livingEntity16, true, false, true, flag5, l, f4, flag6);
                                    }
                                } else {
                                    livingEntity16 = livingEntity18;
                                }
                            } else {
                                livingEntity16 = livingEntity17;
                            }
                        } else {
                            livingEntity16 = livingEntity15;
                        }

                        int i15;
                        byte b4;
                        if (!modeSetting6.is("1.9+")) {
                            i15 = (int)((Long)(Object)modeSetting6 & -256L);
                            b4 = 0;
                        } else {
                            i15 = (int)((Long)(Object)modeSetting6 & -256L | 1L);
                            b4 = 1;
                        }

                        Boolean flag28 = Boolean.TRUE;
                        Object object8 = this.smartCrits.getValue();
                        boolean flag29 = flag28.equals(object8);
                        double d10 = this.getAttackRange();
                        boolean flag30 = this.throughWalls.getValue();
                        int i16 = i15 & -256 | b4;
                        float f5 = (float)d10;
                        if (i16 == 0) {
                            return CombatUtil.canAttack(livingEntity16, true, false, false, flag29, l, f5, flag30);
                        }

                        return CombatUtil.canAttack(livingEntity16, true, false, true, flag29, l, f5, flag30);
                    }

                    return CombatUtil.canAttack(livingEntity2, flag7, flag8, true, flag5, l, f4, flag6);
                }
            }
        }

        return false;
    }

    public boolean isTargetReachable() {
        if (this.target != null) {
            if (mc.player != null) {
                ClientWorld clientWorld = mc.world;
                LivingEntity livingEntity = this.target;
                if (clientWorld != null) {
                    double d = this.getAttackRange();
                    boolean flag = this.throughWalls.getValue();
                    return CombatUtil.isInAttackRange(livingEntity, d, flag);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public double getAttackRange() {
        double d = this.range.getValue().floatValue();
        double d2 = Math.max(3.0, d);
        double d3 = ScriptAuraHooks.modifyRange("aura", d2);
        if (this.target != null) {
            if (mc.player != null) {
                if (ServerManager.isSpookyArena()) {
                    Vec3d vec3d = mc.player.getEyePos();
                    Vec3d vec3d2 = this.target.getTrackedPosition().getPos();
                    double d4 = vec3d.distanceTo(vec3d2);
                    Vec3d vec3d3 = this.target.getEntityPos();
                    double d5 = vec3d.distanceTo(vec3d3);
                    ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
                    UUID uuid = mc.player.getUuid();
                    int i = clientPlayNetworkHandler.getPlayerListEntry(uuid).getLatency();
                    int i2 = Math.max(0, i);
                    double d6 = ExactMath.subtract(5.5, d3);
                    double d7 = ExactMath.add(ExactMath.divide(i2, 250.0), 1.0);
                    double d8 = ExactMath.subtract(d4, d5);
                    double d9 = ExactMath.multiply(d7, d8);
                    double d10 = MathHelper.clamp(ExactMath.subtract(d6, d9), 0.0, 2.5);
                    return Double.longBitsToDouble(Double.doubleToRawLongBits(ExactMath.add(d3, d10)));
                } else {
                    return Double.longBitsToDouble(Double.doubleToRawLongBits(d3));
                }
            } else {
                return Double.longBitsToDouble(Double.doubleToRawLongBits(d3));
            }
        } else {
            long l = Double.doubleToRawLongBits(d3);
            return Double.longBitsToDouble(l);
        }
    }

    public void refreshModes() {
        this.componentMode.setModes(COMPONENT_MODES, ScriptAuraHooks.getRotationModes());
        this.espStyle.setModes(ESP_STYLES, ScriptAuraHooks.getEspModes());
        this.updateModeVisibility();
    }

    private void updateTarget() {
        if (mc.player == null) {
            this.target = null;
        } else {
            double d = this.range.getValue() + this.preRange.getValue();
            boolean flag = this.throughWalls.getValue();
            if (this.isCurrentTargetValid(d, flag)) {
                this.lastTarget = this.target;
            } else {
                if (this.isValidLastTarget(this.lastTarget, d, flag)) {
                    this.target = this.lastTarget;
                } else {
                    this.target = TargetComponent.findTarget(d, this.targets, this.sortMode, flag, this::isWithinSnapsFov);
                }

                if (this.target != null) {
                    this.lastTarget = this.target;
                }
            }
        }
    }

    private boolean isValidLastTarget(LivingEntity livingEntity, double d, boolean flag) {
        return livingEntity != null
            && livingEntity.isAlive()
            && CombatUtil.getDistance(livingEntity) <= d
            && TargetComponent.isValidTarget(livingEntity, this.targets, flag)
            && this.isWithinSnapsFov(livingEntity);
    }

    private boolean isCurrentTargetValid(double d, boolean flag) {
        return this.target == null
            ? false
            : this.target.isAlive()
                && CombatUtil.getRayDistance(this.target) <= d
                && TargetComponent.isValidTarget(this.target, this.targets, flag)
                && this.isWithinSnapsFov(this.target);
    }

    private boolean isWithinSnapsFov(LivingEntity livingEntity) {
        boolean flag = this.componentMode.is("ftsnaps");
        if ((!this.componentMode.is("snaps") || !(this.snapsFov.getValue() < 360.0F)) && !flag) {
            return true;
        }

        boolean flag2 = FreeLookComponent.isActive();
        Vec3d vec3d = flag
            ? RaycastUtil.getDirection(flag2 ? FreeLookComponent.getYaw() : mc.player.getYaw(), flag2 ? FreeLookComponent.getPitch() : mc.player.getPitch())
            : CombatUtil.getServerLookVector();
        Vec3d vec3d2 = mc.player.getEyePos();
        Vec3d vec3d3 = CombatUtil.getClosestPoint(vec3d2, livingEntity).subtract(vec3d2).normalize();
        float f = flag ? this.ftsnapsFov.getValue() : this.snapsFov.getValue();
        return vec3d.dotProduct(vec3d3) >= Math.cos(Math.toRadians(f / 2.0));
    }

    private void reset() {
        this.swapController.reset();
        this.rotationHandlers.values().forEach(TaskHandler::reset);
        TargetComponent.clearTarget();
        TargetComponent.updateEntities();
        this.target = null;
        this.lastTarget = null;
        this.boostedJitter = false;
    }

    public double[] computeAimJitter() {
        long l = System.currentTimeMillis();
        double d = this.lastJitterTime == 0L ? 0.05 : Math.min((l - this.lastJitterTime) / 1000.0, 0.2);
        this.lastJitterTime = l;
        double d2 = Math.exp(-d / 0.3);
        double d3 = Math.sqrt(1.0 - d2 * d2);
        this.jitterX = this.jitterX * d2 + d3 * CombatUtil.RANDOM.nextGaussian();
        this.jitterY = this.jitterY * d2 + d3 * CombatUtil.RANDOM.nextGaussian();
        double d4 = (this.target != null && this.willAttackInTicks(1) ? 0.1F : (this.boostedJitter ? 12.0 : 9.0)) + Math.sin(l / 1250.0);
        return new double[]{this.jitterX * d4 * 0.45, this.jitterY * d4 * 0.28 * 0.45};
    }

    public boolean shouldRotateTo(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return false;
        }

        if (mc.player.hurtTime > 0) {
            this.recentlyHurt = true;
            this.hurtTimer.reset();
        }

        if (this.hurtTimer.hasPassed(2000.0)) {
            this.recentlyHurt = false;
        }

        return !this.recentlyHurt;
    }

    public LivingEntity getCurrentTarget() {
        return this.target;
    }

    public ModeSetting getRotationMode() {
        return this.componentMode;
    }

    public ModeSetting getMoveCorrectionMode() {
        return this.moveCorrection;
    }

    public ModeSetting getAttackModeSetting() {
        return this.attackMode;
    }

    public GroupSetting getTargetEspGroup() {
        return this.targetEsp;
    }

    public String getActiveEspStyle() {
        return this.targetEsp.isEnabled() ? this.espStyle.getValue() : "";
    }

    public String getEspDepthMode() {
        return this.espDepth.getValue();
    }

    public int getEspColorValue() {
        String string = this.espColorMode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -892481938:
                if (string.equals("static")) {
                    return this.espColor.getValue();
                }
                break;
            case 3135100:
                if (string.equals("fade")) {
                    b = 1;
                }
        }

        switch (b) {
            case 0:
                return this.espColor.getValue();
            case 1:
                return ColorUtil.fade(10, 0, this.espFadeColor1.getValue(), this.espFadeColor2.getValue());
            default:
                return ColorUtil.setAlpha(Theme.getPrimary(), 255);
        }
    }

    public int[] getEspFadeColors() {
        return new int[]{
            ColorUtil.fade(10, 0, this.espFadeColor1.getValue(), this.espFadeColor2.getValue()),
            ColorUtil.fade(10, 90, this.espFadeColor1.getValue(), this.espFadeColor2.getValue()),
            ColorUtil.fade(10, 180, this.espFadeColor1.getValue(), this.espFadeColor2.getValue()),
            ColorUtil.fade(10, 210, this.espFadeColor1.getValue(), this.espFadeColor2.getValue())
        };
    }

    public BooleanSetting getThroughWallsSetting() {
        return this.throughWalls;
    }

    public boolean isHitSharedVehicle() {
        return this.checks.isSelected("hit_shared_vehicle");
    }

    public NumberSetting getPreRangeSetting() {
        return this.preRange;
    }

    public MultiSetting getTargets() {
        return this.targets;
    }

    public ModeSetting getSortMode() {
        return this.sortMode;
    }

    public NumberSetting getRange() {
        return this.range;
    }

    public NumberSetting getPreRange() {
        return this.preRange;
    }

    public BooleanSetting getThroughWalls() {
        return this.throughWalls;
    }

    public ModeSetting getComponentMode() {
        return this.componentMode;
    }

    public ModeSetting getMoveCorrection() {
        return this.moveCorrection;
    }

    public ModeSetting getSprint() {
        return this.sprint;
    }

    public NumberSetting getSnapsFov() {
        return this.snapsFov;
    }

    public NumberSetting getLegitSpeed() {
        return this.legitSpeed;
    }

    public NumberSetting getFtsnapsFov() {
        return this.ftsnapsFov;
    }

    public RangeSetting getFtsnapsHang() {
        return this.ftsnapsHang;
    }

    public BooleanSetting getHitRandom() {
        return this.hitRandom;
    }

    public GroupSetting getPitchWave() {
        return this.pitchWave;
    }

    public WaveSetting getPitchWaveShape() {
        return this.pitchWaveShape;
    }

    public GroupSetting getYawWave() {
        return this.yawWave;
    }

    public WaveSetting getYawWaveShape() {
        return this.yawWaveShape;
    }

    public Setting<Runnable> getTrainedPanel() {
        return this.trainedPanel;
    }

    public ModeSetting getAttackMode() {
        return this.attackMode;
    }

    public BooleanSetting getSmartCrits() {
        return this.smartCrits;
    }

    public MultiSetting getChecks() {
        return this.checks;
    }

    public GroupSetting getTargetEsp() {
        return this.targetEsp;
    }

    public ModeSetting getEspStyle() {
        return this.espStyle;
    }

    public ModeSetting getEspDepth() {
        return this.espDepth;
    }

    public ModeSetting getEspColorMode() {
        return this.espColorMode;
    }

    public ColorSetting getEspColor() {
        return this.espColor;
    }

    public ColorSetting getEspFadeColor1() {
        return this.espFadeColor1;
    }

    public ColorSetting getEspFadeColor2() {
        return this.espFadeColor2;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public LivingEntity getLastTarget() {
        return this.lastTarget;
    }

    public float getInRangeTicks() {
        return this.inRangeTicks;
    }

    public Vector2f getServerRotation() {
        return this.serverRotation;
    }

    public boolean isBoostedJitter() {
        return this.boostedJitter;
    }

    public float getRotationProgress() {
        return this.rotationProgress;
    }

    public int getAttackCount() {
        return this.attackCount;
    }

    public TimerUtil getRotationAnimator() {
        return this.rotationAnimator;
    }

    public ActionSequence getTickTracker() {
        return this.tickTracker;
    }

    public AttackAura.SnapDirection getSnapDirection() {
        return this.snapDirection;
    }

    public StopWatch getHurtTimer() {
        return this.hurtTimer;
    }

    public boolean isRecentlyHurt() {
        return this.recentlyHurt;
    }

    public StopWatch getAttackCooldownTimer() {
        return this.attackCooldownTimer;
    }

    public double getJitterX() {
        return this.jitterX;
    }

    public double getJitterY() {
        return this.jitterY;
    }

    public long getLastJitterTime() {
        return this.lastJitterTime;
    }

    public void setBoostedJitter(boolean flag) {
        this.boostedJitter = flag;
    }

    public record AttackHooks(Runnable pre, Runnable post) {
        public static final AttackAura.AttackHooks NONE = new AttackAura.AttackHooks(() -> {}, () -> {});
    }

    public enum SnapDirection {
        UP(0, -1),
        UP_RIGHT(1, -1),
        RIGHT(1, 0),
        DOWN_RIGHT(1, 1),
        DOWN(0, 1),
        DOWN_LEFT(-1, 1),
        LEFT(-1, 0),
        UP_LEFT(-1, -1),
        READY(0, 0);

        private final int dx;
        private final int dy;

        public static AttackAura.SnapDirection randomDiagonalOrSide() {
            AttackAura.SnapDirection[] snapDirections = new AttackAura.SnapDirection[]{UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN_LEFT, LEFT, UP_LEFT};
            return snapDirections[CombatUtil.randomInt(0, snapDirections.length - 1)];
        }

        public static Vector2f scaled(AttackAura.SnapDirection snapDirection2, float f) {
            return scaledXY(snapDirection2, f, f);
        }

        public static Vector2f scaledXY(AttackAura.SnapDirection snapDirection2, float f, float f2) {
            return new Vector2f(snapDirection2.dx * f, snapDirection2.dy * f2);
        }

        public boolean isDiagonal() {
            return this.dx != 0 && this.dy != 0;
        }

        public int getDx() {
            return this.dx;
        }

        public int getDy() {
            return this.dy;
        }

        SnapDirection(int i, int i2) {
            this.dx = i;
            this.dy = i2;
        }
    }

    public enum SnapSide {
        UP(-1),
        UP_RIGHT(1);

        private final int sign;

        public int getSign() {
            return this.sign;
        }

        SnapSide(int i) {
            this.sign = i;
        }
    }
}
