package tech.sadnes.client.feature.impl.combat;

import java.util.Random;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.RangeSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.MovementSimulator;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.player.ServerRotationTracker;

@FeatureInfo(
    name = "TriggerBot",
    description = "feature.impl.combat.triggerbot",
    displayName = "feature.impl.combat.triggerbot",
    category = Category.COMBAT,
    subcategory = Subcategory.AURA
)
public class TriggerBot extends Feature {
    private LivingEntity target;
    private final MultiSetting targets = new MultiSetting(
        "triggerbot.targets",
        BooleanSetting.of("players", true),
        BooleanSetting.of("invisible", true),
        BooleanSetting.of("naked", true),
        BooleanSetting.of("teammates", false),
        BooleanSetting.of("animals", false),
        BooleanSetting.of("mobs", false)
    );
    private final ModeSetting sprintMode = new ModeSetting("triggerbot.sprintMode", new String[]{"packet", "legit", "keep"});
    private final MultiSetting checks = new MultiSetting(
        "triggerbot.checks", BooleanSetting.of("no_hit_eating", true), BooleanSetting.of("no_hit_container", false), BooleanSetting.of("swap_on_hit", false)
    );
    private final NumberSetting dist = new NumberSetting("triggerbot.dist", 3.0F, 1.0F, 6.0F, 0.1F);
    private final RangeSetting delay = new RangeSetting("triggerbot.delay", 0.0F, 100.0F, 0.0F, 500.0F, 5.0F, Unit.MILLISECOND);
    private final BooleanSetting throughWalls = new BooleanSetting("triggerbot.throughWalls");
    private final BooleanSetting critOnly = new BooleanSetting("triggerbot.critOnly");
    private final BooleanSetting smart = new BooleanSetting("triggerbot.smart").setVisibleWhen(this.critOnly::getValue);
    private final Random random = new Random();
    private final FunTimeHitSwapHandler swapHandler = new FunTimeHitSwapHandler();

    public static TriggerBot getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(TriggerBot.class);
    }

    public LivingEntity findTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        if (!this.throughWalls.getValue()) {
            return mc.targetedEntity instanceof LivingEntity livingEntity && TargetComponent.isValidTarget(livingEntity, this.targets, false) ? livingEntity : null;
        }

        double d = this.getRange();
        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = RaycastUtil.getDirection(mc.player.getYaw(), mc.player.getPitch());
        Vec3d vec3d3 = vec3d.add(vec3d2.multiply(d));
        Box box = mc.player.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0);
        EntityHitResult entityHitResult = RaycastUtil.raycastEntities(
            mc.player, vec3d, vec3d3, box, entity2 -> entity2 instanceof LivingEntity && TargetComponent.isValidTarget((LivingEntity)entity2, this.targets, true), d
        );
        if (entityHitResult != null) {
            Entity entity3 = entityHitResult.getEntity();
            if (entity3 instanceof LivingEntity) {
                return (LivingEntity)entity3;
            }
        }

        return null;
    }

    public double getRange() {
        return ScriptAuraHooks.modifyRange("trigger", this.dist.getValue().floatValue());
    }

    @EventHandler
    public void onMovementInput(MoveInputEvent moveInputEvent) {
        if (mc.player != null && this.sprintMode.is("legit")) {
            LivingEntity livingEntity = this.findTarget();
            if (livingEntity != null) {
                if (CombatUtil.isCritReady(livingEntity, (float)this.getRange())) {
                    moveInputEvent.setForward(0.0F);
                }
            }
        }
    }

    @EventHandler
    public void onPreUpdate(Render2DEvent render2DEvent) {
        this.target = this.findTarget();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.tryAttack();
        if (this.target != null && ServerManager.isFunTime() && this.checks.has("swap_on_hit") && this.willAttackInTicks(1) && !this.isSwapBlocked()) {
            this.swapHandler.tick(true);
        } else {
            this.swapHandler.tick(false);
        }
    }

    @EventHandler
    public void onPostAttack(PostMotionEvent postMotionEvent) {
        this.swapHandler.onAttackFinished();
    }

    private void tryAttack() {
        if (this.target != null) {
            if (!this.isSpecialWeapon(mc.player.getMainHandStack()) || !(mc.player.getAttackCooldownProgress(0.0F) < 0.85F)) {
                if (CombatUtil.canAttack(
                    this.target,
                    false,
                    this.sprintMode.is("legit"),
                    this.critOnly.getValue(),
                    this.smart.getValue(),
                    0L,
                    (float)this.getRange(),
                    this.throughWalls.getValue()
                )) {
                    if (!this.shouldSkipHit()) {
                        boolean flag;
                        if (this.throughWalls.getValue() && ServerManager.isHolyWorld()) {
                            flag = !mc.player.canSee(this.target);
                        } else {
                            flag = false;
                        }

                        AttackAura.AttackHooks attackHooks = CombatUtil.createSnapAttack(
                            flag, this.target, ServerRotationTracker.getServerYaw(), ServerRotationTracker.getServerPitch()
                        );
                        AttackAura.AttackHooks attackHooks2 = ServerManager.isFunTime()
                            ? this.swapHandler.createSwapAction(this.isSwapBlocked())
                            : CombatUtil.createOffhandSwap(this.checks.has("swap_on_hit"));
                        Runnable[] runnables = CombatUtil.getSprintResetActions(this.sprintMode.is("packet"));
                        CombatUtil.attack(this.target, runnables[0], runnables[1], () -> {
                            attackHooks.pre().run();
                            attackHooks2.pre().run();
                        }, () -> {
                            attackHooks2.post().run();
                            attackHooks.post().run();
                        }, Hand.MAIN_HAND, true, this.getRange(), this.throughWalls.getValue());
                    }
                }
            }
        }
    }

    private boolean willAttackInTicks(int i) {
        long l = -50L * i;
        if (!this.shouldSkipHit() && AttackTimer.hasCooldownPassed(l)) {
            if (CombatUtil.canAttack(
                this.target,
                false,
                this.sprintMode.is("legit"),
                this.critOnly.getValue(),
                this.smart.getValue(),
                l,
                (float)this.getRange(),
                this.throughWalls.getValue()
            )) {
                return true;
            }

            if (CombatUtil.getRayDistance(this.target) > this.getRange()) {
                return false;
            }

            MovementSimulator movementSimulator = MovementSimulator.simulate(i);
            return !movementSimulator.isOnGround() && movementSimulator.getFallDistance() > 0.0F;
        } else {
            return false;
        }
    }

    private boolean isSwapBlocked() {
        return FunTimeHitSwapHandler.isSwapBlocked() || this.shouldSkipHit();
    }

    @Override
    public void onEnable() {
        this.swapHandler.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null && this.throughWalls.getValue() && ServerManager.isHolyWorld() && !ProtocolUtil.isVersion("1.20.6")) {
            ChatUtil.print(Language.translate("aura.holyWallsVersion", "Для рабочих ударов через стены зайди с версии 1.20.6!"));
        }
    }

    private boolean shouldSkipHit() {
        boolean flag;
        if (mc.player.isUsingItem()) {
            flag = mc.player.getActiveItem().isOf(Items.SHIELD);
        } else {
            flag = false;
        }

        return this.checks.has("no_hit_eating") && mc.player.isUsingItem() && !flag
            ? true
            : this.checks.has("no_hit_container") && mc.currentScreen instanceof HandledScreen && !(mc.currentScreen instanceof InventoryScreen);
    }

    private boolean isSpecialWeapon(ItemStack itemStack) {
        return itemStack.isOf(Items.MACE) || itemStack.getItem() instanceof AxeItem || itemStack.getItem() instanceof TridentItem;
    }
}
