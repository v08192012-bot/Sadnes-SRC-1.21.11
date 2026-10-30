package tech.sadnes.client.feature.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.combat.AntiBot;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.CombatUtil;

public final class TargetComponent extends FeatureComponent {
    public static final List<LivingEntity> LIVING_ENTITIES = new ArrayList<>();
    public static final List<Entity> ENTITIES = new ArrayList<>();
    private static final Set<String> PRIORITY_NAMES = ConcurrentHashMap.newKeySet();
    private static LivingEntity target;
    private static LivingEntity lastTarget;
    private static double lastRange;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.world == null) {
            clearEntities();
            target = null;
        } else {
            updateEntities();
        }
    }

    public static LivingEntity getTarget() {
        return target;
    }

    public static LivingEntity getCurrentTarget() {
        return target;
    }

    public static LivingEntity getLastTarget() {
        return lastTarget;
    }

    public static LivingEntity getPreviousTarget() {
        return lastTarget;
    }

    public static void updateEntities() {
        if (mc.world != null && mc.player != null) {
            LIVING_ENTITIES.clear();
            ENTITIES.clear();

            for (Entity entity : mc.world.getEntities()) {
                ENTITIES.add(entity);
                if (entity != mc.player && entity instanceof LivingEntity livingEntity) {
                    LIVING_ENTITIES.add(livingEntity);
                }
            }
        } else {
            clearEntities();
        }
    }

    public static List<LivingEntity> getTargets(double d, boolean flag, MultiSetting multiSetting, boolean flag2) {
        if (mc.world != null && mc.player != null) {
            if (target != null && !isValidTarget(target, multiSetting, flag2)) {
                target = null;
            }

            if (flag) {
                lastRange = d;
            }

            return LIVING_ENTITIES.stream()
                .filter(livingEntity -> isValidTarget(livingEntity, multiSetting, flag2))
                .filter(livingEntity2 -> CombatUtil.getDistance(livingEntity2) <= d && livingEntity2.isAlive())
                .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public static List<Entity> getTargets(double d, Predicate<Entity> predicate, boolean flag, MultiSetting multiSetting, boolean flag2) {
        if (mc.world != null && mc.player != null) {
            if (target != null && !isValidTarget(target, multiSetting, flag2)) {
                target = null;
            }

            if (flag) {
                lastRange = d;
            }

            return LIVING_ENTITIES.stream().filter(predicate).filter(livingEntity -> CombatUtil.getDistance(livingEntity) <= d && livingEntity.isAlive()).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public static List<Entity> getEntities(double d, Predicate<Entity> predicate) {
        if (mc.world != null && mc.player != null) {
            if (target != null && !predicate.test(target)) {
                target = null;
            }

            return ENTITIES.stream().filter(predicate).filter(entity2 -> CombatUtil.getDistance(entity2) <= d).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public static LivingEntity findTarget(double d, MultiSetting multiSetting, ModeSetting modeSetting, boolean flag, Predicate<LivingEntity> predicate) {
        return findTarget(d, true, multiSetting, modeSetting, flag, predicate);
    }

    public static LivingEntity findTarget(double d, boolean flag, MultiSetting multiSetting, ModeSetting modeSetting, boolean flag2, Predicate<LivingEntity> predicate) {
        if (!isValidTarget(target, multiSetting, flag2) || CombatUtil.getDistance(target) > d || !predicate.test(target)) {
            lastTarget = target = selectBestTarget(d, flag, multiSetting, modeSetting, flag2, predicate).orElse(null);
        }

        return target;
    }

    public static boolean hasTarget(MultiSetting multiSetting, ModeSetting modeSetting, boolean flag) {
        return findTarget(lastRange, false, multiSetting, modeSetting, flag, livingEntity -> true) != null;
    }

    public static void clearTarget() {
        target = null;
    }

    public static boolean isPriority(String string) {
        return string != null && PRIORITY_NAMES.contains(string.toLowerCase(Locale.ROOT));
    }

    public static boolean isPriority(LivingEntity livingEntity) {
        return livingEntity instanceof PlayerEntity && isPriority(((PlayerEntity)livingEntity).getGameProfile().name());
    }

    public static void togglePriority(String string) {
        if (string != null) {
            String string2 = string.toLowerCase(Locale.ROOT);
            if (!PRIORITY_NAMES.remove(string2)) {
                PRIORITY_NAMES.add(string2);
            }
        }
    }

    public static boolean isValidTarget(LivingEntity livingEntity, MultiSetting multiSetting, boolean flag) {
        if (mc.player == null || livingEntity == null || livingEntity instanceof ArmorStandEntity || !livingEntity.isAlive()) {
            return false;
        }

        if (livingEntity.equals(mc.player)) {
            return false;
        }

        if (!isOptionEnabled(multiSetting, "invisible", "Невидимые") && livingEntity.isInvisible()) {
            return false;
        }

        if (livingEntity instanceof PlayerEntity playerEntity) {
            if (!isOptionEnabled(multiSetting, "players", "Игроки")) {
                return false;
            }

            if (AntiBot.isBotStatic(playerEntity)) {
                return false;
            }

            boolean flag2;
            if (playerEntity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                && playerEntity.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                && playerEntity.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) {
                flag2 = playerEntity.getEquippedStack(EquipmentSlot.FEET).isEmpty();
            } else {
                flag2 = false;
            }

            if (!isOptionEnabled(multiSetting, "naked", "Голые") && flag2) {
                return false;
            }

            if (!isOptionEnabled(multiSetting, "teammates", "Тиммейты") && Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name())) {
                return false;
            }
        } else if (livingEntity instanceof AnimalEntity) {
            if (!isOptionEnabled(multiSetting, "animals", "Животные")) {
                return false;
            }
        } else {
            if (!(livingEntity instanceof MobEntity)) {
                return false;
            }

            if (!isOptionEnabled(multiSetting, "mobs", "Мобы")) {
                return false;
            }
        }

        return flag || checkVisibility(livingEntity);
    }

    public static boolean isVisible(Entity entity) {
        if (mc.player != null && entity != null) {
            return ServerManager.isFunTime() && mc.player.isSwimming() && entity.getY() > mc.player.getY()
                ? true
                : AimPointUtil.hasVisiblePoint(entity, entity.getBoundingBox());
        } else {
            return false;
        }
    }

    private static boolean checkVisibility(LivingEntity livingEntity) {
        return isVisible(livingEntity);
    }

    private static Optional<LivingEntity> selectBestTarget(
        double d, boolean flag, MultiSetting multiSetting, ModeSetting modeSetting, boolean flag2, Predicate<LivingEntity> predicate
    ) {
        List list = getTargets(d, flag, multiSetting, flag2);
        list.removeIf(predicate.negate());
        if (list.isEmpty()) {
            return Optional.empty();
        }

        if (isMode(modeSetting, "adaptive", "Адаптивный")) {
            list.sort(
                Comparator.comparingDouble(TargetComponent::negativeArmorScore)
                    .thenComparingDouble(CombatUtil::getCenterRotationDistance)
                    .thenComparingDouble(livingEntity -> CombatUtil.getDistance(livingEntity))
                    .thenComparingDouble(TargetComponent::effectiveHealth)
            );
        } else if (isMode(modeSetting, "distance", "Дистанция")) {
            list.sort(
                Comparator.<LivingEntity>comparingDouble(livingEntity2 -> CombatUtil.getDistance(livingEntity2)).thenComparingDouble(livingEntity3 -> effectiveHealth(livingEntity3))
            );
        } else if (isMode(modeSetting, "health", "Здоровье")) {
            list.sort(Comparator.comparingDouble(TargetComponent::effectiveHealth).thenComparingDouble(livingEntity4 -> CombatUtil.getDistance(livingEntity4)));
        } else if (isMode(modeSetting, "aim", "Наводка")) {
            list.sort(Comparator.comparingDouble(CombatUtil::getCenterRotationDistance).thenComparingDouble(livingEntity5 -> CombatUtil.getDistance(livingEntity5)));
        }

        list.sort(Comparator.<LivingEntity>comparingInt(livingEntity6 -> isPriority(livingEntity6) ? 0 : 1));
        return Optional.of((LivingEntity)list.get(0));
    }

    private static double negativeArmorScore(LivingEntity livingEntity) {
        return livingEntity instanceof PlayerEntity playerEntity ? -getArmor(playerEntity) : -livingEntity.getArmor();
    }

    private static double getArmor(PlayerEntity playerEntity) {
        return playerEntity.getArmor();
    }

    private static double effectiveHealth(LivingEntity livingEntity) {
        return livingEntity != null ? livingEntity.getHealth() + livingEntity.getAbsorptionAmount() : 0.0;
    }

    private static boolean isOptionEnabled(MultiSetting multiSetting, String string, String string2) {
        if (multiSetting == null) {
            return true;
        }

        BooleanSetting booleanSetting = multiSetting.getOption(string);
        if (booleanSetting == null) {
            booleanSetting = multiSetting.getOption(string2);
        }

        return booleanSetting == null ? true : Boolean.TRUE.equals(booleanSetting.getValue());
    }

    private static boolean isMode(ModeSetting modeSetting, String string, String string2) {
        return modeSetting == null ? false : modeSetting.is(string) || modeSetting.is(string2);
    }

    private static void clearEntities() {
        LIVING_ENTITIES.clear();
        ENTITIES.clear();
    }
}
