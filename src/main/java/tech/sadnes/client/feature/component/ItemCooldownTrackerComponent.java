package tech.sadnes.client.feature.component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UsingItemChangeEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.hud.TargetCooldowns;
import tech.sadnes.client.server.CooldownItem;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.math.StopWatch;

public final class ItemCooldownTrackerComponent extends FeatureComponent {
    private static final long USE_TOLERANCE_MS = 200L;
    private final Map<UUID, Map<CooldownItem, StopWatch>> cooldowns = new HashMap<>();
    private final Map<UUID, ItemCooldownTrackerComponent.UsageSession> activeUses = new HashMap<>();

    public static ItemCooldownTrackerComponent getInstance() {
        return Sadnes.getInstance().getComponentManager().getComponent(ItemCooldownTrackerComponent.class);
    }

    @EventHandler
    public void onUsingItemChange(UsingItemChangeEvent usingItemChangeEvent) {
        if (usingItemChangeEvent.getEntity() instanceof PlayerEntity playerEntity) {
            if (usingItemChangeEvent.isUsingItem()) {
                ItemStack itemStack = playerEntity.getActiveItem();
                CooldownItem cooldownItem = CooldownItem.fromStack(itemStack);
                if (cooldownItem != null) {
                    this.activeUses.put(playerEntity.getUuid(), new ItemCooldownTrackerComponent.UsageSession(cooldownItem, itemStack.getMaxUseTime(playerEntity) * 50L, new StopWatch()));
                }
            } else {
                ItemCooldownTrackerComponent.UsageSession usageSession = this.activeUses.remove(playerEntity.getUuid());
                if (usageSession != null) {
                    boolean flag = usageSession.watch.getElapsed() + 200L >= usageSession.requiredMs;
                    if (flag) {
                        this.startCooldown(playerEntity, usageSession.item);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.world != null) {
            this.cooldowns.values().removeIf(map -> {
                map.entrySet().removeIf(entry -> this.computeRemaining(entry.getKey(), entry.getValue()) == 0L);
                return map.isEmpty();
            });
            this.activeUses.keySet().removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.cooldowns.clear();
        this.activeUses.clear();
    }

    public void startCooldown(PlayerEntity playerEntity, CooldownItem cooldownItem) {
        if (playerEntity != null && cooldownItem != null) {
            this.cooldowns.computeIfAbsent(playerEntity.getUuid(), uuid -> new EnumMap<>(CooldownItem.class)).computeIfAbsent(cooldownItem, cooldownItem2 -> new StopWatch()).reset();
        }
    }

    public boolean isOnCooldown(PlayerEntity playerEntity, CooldownItem cooldownItem) {
        return this.getRemainingMs(playerEntity, cooldownItem) > 0L;
    }

    public long getRemainingMs(PlayerEntity playerEntity, CooldownItem cooldownItem) {
        if (playerEntity != null && cooldownItem != null) {
            Map map = this.cooldowns.get(playerEntity.getUuid());
            if (map == null) {
                return 0L;
            }

            StopWatch stopWatch = (StopWatch)map.get(cooldownItem);
            return stopWatch == null ? 0L : this.computeRemaining(cooldownItem, stopWatch);
        } else {
            return 0L;
        }
    }

    public Map<CooldownItem, Long> getCooldowns(PlayerEntity playerEntity) {
        if (playerEntity == null) {
            return Map.of();
        }

        Map<CooldownItem, StopWatch> map = this.cooldowns.get(playerEntity.getUuid());
        if (map != null && !map.isEmpty()) {
            EnumMap enumMap = new EnumMap<>(CooldownItem.class);

            for (Entry<CooldownItem, StopWatch> entry : map.entrySet()) {
                long l = this.computeRemaining((CooldownItem)entry.getKey(), (StopWatch)entry.getValue());
                if (l > 0L) {
                    enumMap.put((CooldownItem)entry.getKey(), l);
                }
            }

            return enumMap;
        } else {
            return Map.of();
        }
    }

    public float getProgress(PlayerEntity playerEntity, CooldownItem cooldownItem) {
        return cooldownItem == null ? 0.0F : (float)this.getRemainingMs(playerEntity, cooldownItem) / (float)this.getDurationMs(cooldownItem);
    }

    public long getDurationMs(CooldownItem cooldownItem) {
        TargetCooldowns targetCooldowns = TargetCooldowns.getInstance();
        if (targetCooldowns != null && targetCooldowns.isCustomDurations()) {
            return targetCooldowns.getCustomDurationMs(cooldownItem);
        } else {
            return ServerManager.isHolyWorld() ? cooldownItem.getHolyWorldCooldownMs() : cooldownItem.getCooldownMs();
        }
    }

    public void clearPlayer(PlayerEntity playerEntity) {
        if (playerEntity != null) {
            this.cooldowns.remove(playerEntity.getUuid());
            this.activeUses.remove(playerEntity.getUuid());
        }
    }

    private long computeRemaining(CooldownItem cooldownItem, StopWatch stopWatch) {
        return Math.max(this.getDurationMs(cooldownItem) - stopWatch.getElapsed(), 0L);
    }

    record UsageSession(CooldownItem item, long requiredMs, StopWatch watch) {
    }
}
