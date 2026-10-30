package tech.sadnes.client.feature.impl.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import org.joml.Vector2f;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.AutoCrystal;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.PearlTrajectoryUtil;

@FeatureInfo(
    name = "TargetPearl",
    description = "feature.impl.misc.targetpearl",
    displayName = "feature.impl.misc.targetpearl",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_ITEMS
)
public final class TargetPearl extends Feature {
    private final ModeSetting mode = new ModeSetting("targetpearl.mode", new String[]{"key", "auto"});
    private final KeybindSetting bind = new KeybindSetting("targetpearl.bind").setVisibleWhen(() -> this.mode.is("key"));
    private final BooleanSetting onlyTarget = new BooleanSetting("targetpearl.onlyTarget", false);
    private final NumberSetting distance = new NumberSetting("targetpearl.distance", 10.0F, 8.0F, 20.0F, 1.0F);
    private final BooleanSetting pingCompensation = new BooleanSetting("targetpearl.pingCompensation", true);
    private final NumberSetting delay = new NumberSetting("targetpearl.delay", 0.0F, 0.0F, 500.0F, 10.0F, Unit.MILLISECOND);
    private final Set<Integer> handledPearls = new HashSet<>();
    private final Map<UUID, Long> recentTargets = new HashMap<>();
    private final StopWatch throwTimer = new StopWatch();
    private Vector2f pendingRotation;
    private boolean keyPressed;

    @Override
    public void onEnable() {
        this.handledPearls.clear();
        this.recentTargets.clear();
        this.pendingRotation = null;
        this.keyPressed = false;
        super.onEnable();
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (this.mode.is("key")) {
            boolean flag = screenMouseClickEvent.isPress();
            KeybindSetting keybindSetting = this.bind;
            if (flag) {
                int i = screenMouseClickEvent.getButton();
                if (keybindSetting.matchesKey(i)) {
                    this.keyPressed = true;
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (this.mode.is("key")) {
            boolean flag = screenKeyEvent.isPress();
            KeybindSetting keybindSetting = this.bind;
            if (flag) {
                int i = screenKeyEvent.getKey();
                if (keybindSetting.matchesKey(i)) {
                    this.keyPressed = true;
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            if (mc.player.isAlive()) {
                this.updateRecentTargets();
                Set<Integer> integers = this.handledPearls;
                Predicate<Integer> predicate = integer -> !(mc.world.getEntityById(integer) instanceof EnderPearlEntity);
                integers.removeIf(predicate);
                boolean flag = this.handlePendingThrow();
                ModeSetting modeSetting = this.mode;
                if (!flag) {
                    boolean flag2 = modeSetting.is("key");
                    boolean flag3 = this.keyPressed;
                    if (!flag2 || flag3) {
                        this.processPearls();
                        return;
                    }
                }
            }
        }
    }

    private void processPearls() {
        ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
        ItemStack itemStack = Items.ENDER_PEARL.getDefaultStack();
        boolean flag = itemCooldownManager.isCoolingDown(itemStack);
        if (ThrowComponent.getInstance().isIdle() && !flag) {
            Slot slot = InventoryUtil.findSlotReverse(Items.ENDER_PEARL);
            if (slot == null) {
                this.keyPressed = false;
                return;
            }

            Iterator iterator2 = this.findEnemyPearls().iterator();

            EnderPearlEntity enderPearlEntity;
            TargetPearl.PearlAim pearlAim;
            do {
                if (!iterator2.hasNext()) {
                    this.keyPressed = false;
                    return;
                }

                enderPearlEntity = (EnderPearlEntity)iterator2.next();
                pearlAim = this.computePearlAim(enderPearlEntity);
            } while (pearlAim == null);

            if (pearlAim.rotation() != null) {
                Vector2f vector2f = pearlAim.rotation();
                this.throwPearl(enderPearlEntity, slot, vector2f);
                return;
            }
        }
    }

    private TargetPearl.PearlAim computePearlAim(EnderPearlEntity enderPearlEntity) {
        PearlTrajectoryUtil.Flight flight2 = PearlTrajectoryUtil.simulatePearl(enderPearlEntity);
        if (flight2 != null && !(mc.player.getEntityPos().distanceTo(flight2.destination()) <= this.distance.getValue().floatValue())) {
            Entity entity = enderPearlEntity.getOwner();
            PearlTrajectoryUtil.Solution solution = PearlTrajectoryUtil.solveThrow(flight2.impact(), entity);
            if (solution != null) {
                if (solution.flight().destination().distanceTo(flight2.destination()) > 3.0) {
                    return null;
                }

                PearlTrajectoryUtil.Flight flight3 = PearlTrajectoryUtil.simulateThrow(solution.rotation().x, solution.rotation().y, null);
                boolean flag = flight3 != null && flight3.hitEntity() == entity;
                double d = this.pingCompensation.getValue() ? ServerManager.getPing() / 50.0 : 0.0;
                double d2 = 2.0 + d / 2.0;
                double d3 = flight2.ticks() - d / 2.0;
                boolean flag2 = flag && d2 + flight3.ticks() < d3 + 1.0;
                boolean flag3 = enderPearlEntity.age + d < this.delay.getValue().floatValue() / 50.0;
                return new TargetPearl.PearlAim(!flag2 && !flag3 ? solution.rotation() : null);
            }
        }

        return null;
    }

    private void throwPearl(EnderPearlEntity enderPearlEntity, Slot slot, Vector2f vector2f) {
        Hand hand = mc.player.getActiveHand();
        Hand hand2 = Hand.MAIN_HAND;
        if (hand == hand2) {
            if (mc.player.getItemUseTime() != 0) {
                float f = vector2f.x;
                float f2 = vector2f.y;
                RotationComponent.rotateTo(new Rotation(f, f2), 999.0F, 999.0F, 2, 6);
                ThrowComponent throwComponent = ThrowComponent.getInstance();
                Hand hand3 = Hand.OFF_HAND;
                throwComponent.useSlotFull(slot, hand3, vector2f);
                Set set = this.handledPearls;
                Integer integer = enderPearlEntity.getId();
                set.add(integer);
                StopWatch stopWatch = this.throwTimer;
                this.pendingRotation = vector2f;
                stopWatch.reset();
                this.keyPressed = false;
            } else {
                float f3 = vector2f.x;
                float f4 = vector2f.y;
                RotationComponent.rotateTo(new Rotation(f3, f4), 999.0F, 999.0F, 2, 6);
                ThrowComponent throwComponent2 = ThrowComponent.getInstance();
                Hand hand4 = Hand.MAIN_HAND;
                throwComponent2.useSlotFull(slot, hand4, vector2f);
                Set set2 = this.handledPearls;
                Integer integer2 = enderPearlEntity.getId();
                set2.add(integer2);
                StopWatch stopWatch2 = this.throwTimer;
                this.pendingRotation = vector2f;
                stopWatch2.reset();
                this.keyPressed = false;
            }
        } else {
            float f5 = vector2f.x;
            float f6 = vector2f.y;
            RotationComponent.rotateTo(new Rotation(f5, f6), 999.0F, 999.0F, 2, 6);
            ThrowComponent throwComponent3 = ThrowComponent.getInstance();
            Hand hand5 = Hand.MAIN_HAND;
            throwComponent3.useSlotFull(slot, hand5, vector2f);
            Set set3 = this.handledPearls;
            Integer integer3 = enderPearlEntity.getId();
            set3.add(integer3);
            StopWatch stopWatch3 = this.throwTimer;
            this.pendingRotation = vector2f;
            stopWatch3.reset();
            this.keyPressed = false;
        }
    }

    private boolean handlePendingThrow() {
        Vector2f vector2f = this.pendingRotation;
        boolean flag;
        if (vector2f != null) {
            boolean flag2 = ThrowComponent.getInstance().isIdle();
            Vector2f vector2f2 = this.pendingRotation;
            if (flag2) {
                ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
                ItemStack itemStack = Items.ENDER_PEARL.getDefaultStack();
                boolean flag3 = itemCooldownManager.isCoolingDown(itemStack);
                StopWatch stopWatch = this.throwTimer;
                if (!flag3) {
                    int i = ServerManager.getPing();
                    double d = 150 + i;
                    boolean flag4 = stopWatch.hasPassed(d);
                    vector2f = this.pendingRotation;
                    if (!flag4) {
                        flag = true;
                    } else {
                        flag = false;
                        vector2f = null;
                    }
                } else {
                    flag = false;
                    vector2f = null;
                }
            } else {
                float f = vector2f2.x;
                float f2 = this.pendingRotation.y;
                RotationComponent.rotateTo(new Rotation(f, f2), 999.0F, 999.0F, 2, 6);
                this.throwTimer.reset();
                vector2f = this.pendingRotation;
                flag = true;
            }
        } else {
            flag = false;
        }

        this.pendingRotation = vector2f;
        return flag;
    }

    private List<EnderPearlEntity> findEnemyPearls() {
        ArrayList arrayList = new ArrayList();

        for (Entity entity2 : mc.world.getEntities()) {
            if (entity2 instanceof EnderPearlEntity enderPearlEntity && !this.handledPearls.contains(enderPearlEntity.getId())) {
                Entity entity3 = enderPearlEntity.getOwner();
                boolean flag;
                if (entity3 instanceof PlayerEntity && entity3 != mc.player) {
                    flag = entity3.isAlive();
                } else {
                    flag = false;
                }

                if (flag && (!this.onlyTarget.getValue() || this.isRecentTarget(entity3))) {
                    arrayList.add(enderPearlEntity);
                }
            }
        }

        arrayList.sort(
            Comparator.<EnderPearlEntity, Boolean>comparing(enderPearlEntity2 -> !this.isRecentTarget(enderPearlEntity2.getOwner()) ? true : false)
                .thenComparingDouble(enderPearlEntity3 -> mc.player.squaredDistanceTo(enderPearlEntity3.getOwner()))
        );
        return arrayList;
    }

    private void updateRecentTargets() {
        long l = System.currentTimeMillis();
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null && attackAura.isEnabled() && attackAura.getCurrentTarget() != null) {
            this.recentTargets.put(attackAura.getCurrentTarget().getUuid(), l);
        }

        AutoCrystal autoCrystal = AutoCrystal.getInstance();
        if (autoCrystal != null) {
            LivingEntity livingEntity = autoCrystal.getActiveTarget();
            if (livingEntity != null) {
                this.recentTargets.put(livingEntity.getUuid(), l);
            }
        }

        this.recentTargets.values().removeIf(longValue -> l - longValue > 5000L);
    }

    private boolean isRecentTarget(Entity entity) {
        return this.recentTargets.containsKey(entity.getUuid());
    }

    record PearlAim(Vector2f rotation) {
    }
}
