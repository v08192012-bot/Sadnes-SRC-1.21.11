package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.event.impl.ItemPickupEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UsingItemChangeEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.SpecialPotion;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "Tracker",
    description = "feature.impl.misc.tracker",
    displayName = "feature.impl.misc.tracker",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CHAT
)
public class Tracker extends Feature {
    private final MultiSetting tracking = new MultiSetting(
        "tracker.tracking",
        BooleanSetting.of("splash_potions", true),
        BooleanSetting.of("item_pickup", true),
        BooleanSetting.of("dropped_totem", true),
        BooleanSetting.of("low_hp", true),
        BooleanSetting.of("armor_durability", true),
        BooleanSetting.of("potion_effects", true),
        BooleanSetting.of("player_eating", true),
        BooleanSetting.of("player_leave", true)
    );
    private final NumberSetting hpThreshold = new NumberSetting("tracker.hpThreshold", 6.0F, 2.0F, 20.0F, 1.0F).setVisibleWhen(() -> this.tracking.has("low_hp"));
    private final BooleanSetting hpHud = new BooleanSetting("tracker.hpHud", true).setVisibleWhen(() -> this.tracking.has("low_hp"));
    private final NumberSetting durabilityPercent = new NumberSetting("tracker.durabilityPercent", 0.25F, 0.05F, 1.0F, 0.05F)
        .setVisibleWhen(() -> this.tracking.has("armor_durability"));
    private final NumberSetting potionWarnSeconds = new NumberSetting("tracker.potionWarnSeconds", 3.0F, 1.0F, 10.0F, 1.0F)
        .setVisibleWhen(() -> this.tracking.has("potion_effects"));
    private static final Set<SpecialPotion> DANGEROUS_POTIONS = new HashSet<>(
        Arrays.asList(SpecialPotion.RADIATION, SpecialPotion.SLEEPS, SpecialPotion.ASSASSIN)
    );
    private static final long TOTEM_COOLDOWN_MS = 1500L;
    private static final long PICKUP_COOLDOWN_MS = 1500L;
    private static final long ARMOR_COOLDOWN_MS = 15000L;
    private static final long EATING_COOLDOWN_MS = 2500L;
    private static final long EFFECT_COOLDOWN_MS = 5000L;
    private boolean lowHpNotified;
    private final StopWatch armorCheckTimer = new StopWatch();
    private boolean pickupSoundHeard;
    private final StopWatch pickupSoundTimer = new StopWatch();
    private final Map<UUID, Long> totemCooldowns = new HashMap<>();
    private final Map<String, Long> pickupCooldowns = new HashMap<>();
    private final Map<String, Long> armorCooldowns = new HashMap<>();
    private final Map<String, Long> effectCooldowns = new HashMap<>();
    private final Map<UUID, Long> eatingCooldowns = new HashMap<>();
    private final Map<UUID, Tracker.ItemUseStart> itemUsers = new HashMap<>();
    private final List<Tracker.ThrownPotion> thrownPotions = new ArrayList<>();
    private final Animation lowHpAnimation = new Animation();

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            if (this.pickupSoundHeard && this.pickupSoundTimer.hasPassed(200.0)) {
                this.pickupSoundHeard = false;
            }

            this.checkLowHp();
            this.checkArmorDurability();
            this.checkPotionEffects();
            this.checkPlayerEating();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && !ServerManager.isNullWorld()) {
            Packet packet = packetEvent.getPacket();
            if (packet instanceof EntityStatusS2CPacket entityStatusS2CPacket) {
                mc.execute(() -> this.handleEntityStatus(entityStatusS2CPacket));
            } else if (packet instanceof PlaySoundS2CPacket playSoundS2CPacket) {
                mc.execute(() -> this.handleSound(playSoundS2CPacket));
            } else {
                if (packet instanceof WorldEventS2CPacket worldEventS2CPacket) {
                    mc.execute(() -> this.handleWorldEvent(worldEventS2CPacket));
                }
            }
        }
    }

    private void handleEntityStatus(EntityStatusS2CPacket entityStatusS2CPacket) {
        if (mc.world != null && mc.player != null) {
            if (this.tracking.has("dropped_totem") && entityStatusS2CPacket.getStatus() == 35) {
                Entity entity = entityStatusS2CPacket.getEntity(mc.world);
                if (entity instanceof PlayerEntity playerEntity && entity != mc.player) {
                    if (!this.isOnCooldown(this.totemCooldowns, playerEntity.getUuid(), 1500L)) {
                        String string = playerEntity.getGameProfile().name();
                        double d = mc.player.distanceTo(playerEntity);
                        MutableText mutableText = Text.literal(string)
                            .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(Text.literal(" потерял тотем").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                            .append(Text.literal(" (" + Math.round(d) + "м)").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
                        NotificationUtil.notify(NotificationType.WARNING, this.getDisplayName(), Items.TOTEM_OF_UNDYING.getDefaultStack(), mutableText);
                    }
                }
            }
        }
    }

    private void handleSound(PlaySoundS2CPacket playSoundS2CPacket) {
        if (this.tracking.has("item_pickup")) {
            String string = playSoundS2CPacket.getSound().value().id().getPath();
            if (string.equals("entity.item.pickup") || string.equals("item.armor.equip_chain")) {
                this.pickupSoundHeard = true;
                this.pickupSoundTimer.reset();
            }
        }
    }

    private void handleWorldEvent(WorldEventS2CPacket worldEventS2CPacket) {
        if (mc.world != null && mc.player != null) {
            int i = worldEventS2CPacket.getEventId();
            if (i == 2002 || i == 2007) {
                if (this.tracking.has("splash_potions")) {
                    double d = worldEventS2CPacket.getPos().getX() + 0.5;
                    double d2 = worldEventS2CPacket.getPos().getY() + 0.5;
                    double d3 = worldEventS2CPacket.getPos().getZ() + 0.5;
                    int i2 = worldEventS2CPacket.getData() & 16777215;
                    ItemStack itemStack = this.findPotionStack(d, d2, d3);
                    if (itemStack != null) {
                        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
                        if (potionContentsComponent != null && potionContentsComponent.hasEffects()) {
                            ArrayList arrayList = new ArrayList();
                            potionContentsComponent.getEffects().forEach(arrayList::add);
                            if (!arrayList.isEmpty()) {
                                this.handlePotionSplash(d, d2, d3, itemStack, arrayList);
                                return;
                            }
                        }
                    }

                    this.handleSpecialPotionSplash(d, d2, d3, i2);
                }
            }
        }
    }

    private void handlePotionSplash(double d, double d2, double d3, ItemStack itemStack, List<StatusEffectInstance> statusEffectInstances) {
        Box box = new Box(d - 4.0, d2 - 2.0, d3 - 4.0, d + 4.0, d2 + 2.0, d3 + 4.0);
        List<PlayerEntity> playerEntities = mc.world.getEntitiesByClass(PlayerEntity.class, box, playerEntity -> true);
        if (!playerEntities.isEmpty()) {
            boolean flag = this.isNearSolidBlock(d, d2, d3);
            PlayerEntity playerEntity2 = this.findDirectHitPlayer(d, d2, d3, playerEntities, flag);
            Optional optional = this.findSpecialPotionByName(itemStack.getName().getString());

            for (PlayerEntity playerEntity3 : playerEntities) {
                double d4 = this.squaredDistanceToEyes(d, d2, d3, playerEntity3);
                if (!(d4 >= 16.0)) {
                    double d5 = playerEntity3 == playerEntity2 && !flag && Math.sqrt(d4) < 2.0 ? 1.0 : 1.0 - Math.sqrt(d4) / 4.0;
                    ArrayList arrayList = new ArrayList();
                    StringBuilder builder = new StringBuilder();
                    double d6 = 0.0;
                    double d7 = 0.0;

                    for (StatusEffectInstance statusEffectInstance : statusEffectInstances) {
                        if (!statusEffectInstance.getEffectType().value().isInstant()) {
                            int i = statusEffectInstance.getDuration();
                            int i2 = (int)(d5 * i + 0.5);
                            if (i2 > 20) {
                                arrayList.add(new StatusEffectInstance(statusEffectInstance.getEffectType(), i2, statusEffectInstance.getAmplifier(), false, true));
                                d6 += i2;
                                d7 += i;
                                builder.append(Formatting.GRAY)
                                    .append(this.getEffectName(statusEffectInstance.getEffectType()))
                                    .append(Formatting.WHITE)
                                    .append(" ")
                                    .append(this.toRoman(statusEffectInstance.getAmplifier() + 1))
                                    .append(Formatting.GRAY)
                                    .append(" (")
                                    .append(this.formatDuration(i2 / 20))
                                    .append("/")
                                    .append(this.formatDuration(i / 20))
                                    .append(")\n");
                            }
                        }
                    }

                    if (!arrayList.isEmpty()) {
                        int i3 = d7 > 0.0 ? (int)(d6 / d7 * 100.0) : 0;
                        if (optional.isPresent() && DANGEROUS_POTIONS.contains(optional.get()) && playerEntity3 == mc.player) {
                            this.notifySpecialPotionOnSelf((SpecialPotion)optional.get());
                        }

                        this.notifyPotionHit(playerEntity3, itemStack.getName(), i3, builder.toString().trim());
                    }
                }
            }
        }
    }

    private void handleSpecialPotionSplash(double d, double d2, double d3, int i) {
        Optional optional = this.findSpecialPotionByColor(i);
        if (!optional.isEmpty()) {
            SpecialPotion specialPotion = (SpecialPotion)optional.get();
            Box box = new Box(d - 4.0, d2 - 2.0, d3 - 4.0, d + 4.0, d2 + 2.0, d3 + 4.0);
            List list = mc.world.getEntitiesByClass(PlayerEntity.class, box, playerEntity -> true);
            if (!list.isEmpty()) {
                boolean flag = this.isNearSolidBlock(d, d2, d3);
                PlayerEntity playerEntity2 = this.findDirectHitPlayer(d, d2, d3, list, flag);
                Iterator iterator2 = list.iterator();

                while (true) {
                    PlayerEntity playerEntity3;
                    double d4;
                    while (true) {
                        if (!iterator2.hasNext()) {
                            return;
                        }

                        playerEntity3 = (PlayerEntity)iterator2.next();
                        double d5 = this.squaredDistanceToEyes(d, d2, d3, playerEntity3);
                        if (playerEntity3 == playerEntity2 && !flag && Math.sqrt(d5) < 2.0) {
                            d4 = 1.0;
                            break;
                        }

                        if (d5 < 16.0) {
                            d4 = 1.0 - Math.sqrt(d5) / 4.0;
                            break;
                        }
                    }

                    ArrayList arrayList = new ArrayList();
                    StringBuilder builder = new StringBuilder();

                    for (SpecialPotion.Effect effect2 : specialPotion.getEffects()) {
                        int i2 = effect2.getDurationTicks();
                        int i3 = (int)(d4 * i2 + 0.5);
                        if (i3 > 20) {
                            arrayList.add(new StatusEffectInstance(effect2.effect(), i3, effect2.amplifier(), false, true));
                            builder.append(Formatting.GRAY)
                                .append(this.getEffectName(effect2.effect()))
                                .append(Formatting.WHITE)
                                .append(" ")
                                .append(this.toRoman(effect2.amplifier() + 1))
                                .append(Formatting.GRAY)
                                .append(" (")
                                .append(this.formatDuration(i3 / 20))
                                .append("/")
                                .append(this.formatDuration(i2 / 20))
                                .append(")\n");
                        }
                    }

                    if (!arrayList.isEmpty()) {
                        int i4 = (int)(d4 * 100.0);
                        if (DANGEROUS_POTIONS.contains(specialPotion) && playerEntity3 == mc.player) {
                            this.notifySpecialPotionOnSelf(specialPotion);
                        }

                        this.notifyPotionHit(playerEntity3, specialPotion.getDisplayName(), i4, builder.toString().trim());
                    }
                }
            }
        }
    }

    private PlayerEntity findDirectHitPlayer(double d, double d2, double d3, List<PlayerEntity> playerEntities, boolean flag) {
        if (flag) {
            return null;
        }

        PlayerEntity playerEntity = null;
        double d4 = Double.MAX_VALUE;

        for (PlayerEntity playerEntity2 : playerEntities) {
            double d5 = Math.sqrt(this.squaredDistanceToEyes(d, d2, d3, playerEntity2));
            if (d5 < d4) {
                d4 = d5;
                playerEntity = playerEntity2;
            }
        }

        return playerEntity;
    }

    private void notifyPotionHit(PlayerEntity playerEntity, Text text, int i, String string) {
        MutableText mutableText = Text.literal("");
        Text text2 = playerEntity.getDisplayName();
        mutableText.append(text2 == null ? Text.literal(playerEntity.getGameProfile().name()) : text2);
        mutableText.append(Text.literal(" получил ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        Style style = string.isEmpty()
            ? Style.EMPTY
            : Style.EMPTY
                .withHoverEvent(new ShowText(Text.literal("Активные эффекты:\n").setStyle(Style.EMPTY.withColor(Formatting.GOLD)).append(Text.literal(string))));
        mutableText.append(text.copy().setStyle(text.getStyle().withHoverEvent(style.getHoverEvent())));
        mutableText.append(Text.literal(" (" + i + "%)").setStyle(Style.EMPTY.withColor(16762967).withHoverEvent(style.getHoverEvent())));
        this.sendNotification(mutableText);
    }

    private void notifySpecialPotionOnSelf(SpecialPotion specialPotion) {
        MutableText mutableText = Text.literal("На вас кинули ").setStyle(Style.EMPTY.withColor(Formatting.RED)).append(specialPotion.getDisplayName().copy());
        this.sendNotification(mutableText);
    }

    @EventHandler
    public void onItemPickup(ItemPickupEvent itemPickupEvent) {
        if (this.tracking.has("item_pickup")) {
            if (itemPickupEvent.getItem() instanceof ItemEntity itemEntity) {
                ItemStack itemStack = itemEntity.getStack();
                if (this.isValuableItem(itemStack)) {
                    Entity entity = itemPickupEvent.getCollector();
                    double d = mc.player.distanceTo(entity);
                    String string = entity.getUuidAsString() + ":" + itemStack.getItem().toString();
                    if (!this.isOnCooldown(this.pickupCooldowns, string, 1500L)) {
                        String string2 = entity.getName().getString();
                        int i = itemStack.getCount();
                        MutableText mutableText = Text.literal("Игрок ")
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                            .append(Text.literal(string2).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                            .append(Text.literal(" поднял ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                            .append(itemStack.getName().copy());
                        if (i > 1) {
                            mutableText.append(Text.literal(" ×" + i).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                        }

                        mutableText.append(Text.literal(" (" + Math.round(d) + "м)").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
                        this.sendNotification(mutableText);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent entityRemoveEvent) {
        Entity entity = entityRemoveEvent.getEntity();
        if (entity != null) {
            if (entity instanceof PotionEntity potionEntity && this.tracking.has("splash_potions")) {
                this.thrownPotions.add(new Tracker.ThrownPotion(potionEntity.getX(), potionEntity.getY(), potionEntity.getZ(), potionEntity.getStack().copy(), System.currentTimeMillis()));
                if (this.thrownPotions.size() > 32) {
                    this.thrownPotions.remove(0);
                }
            }

            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
                && entity != mc.player
                && this.tracking.has("player_leave")
                && mc.player.distanceTo(abstractClientPlayerEntity) > 100.0F) {
                String string = abstractClientPlayerEntity.getGameProfile().name();
                float f = (float)Math.round(abstractClientPlayerEntity.getX() * 10.0) / 10.0F;
                float f2 = (float)Math.round(abstractClientPlayerEntity.getY() * 10.0) / 10.0F;
                float f3 = (float)Math.round(abstractClientPlayerEntity.getZ() * 10.0) / 10.0F;
                MutableText mutableText = Text.literal("Игрок ")
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                    .append(Text.literal(string).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                    .append(Text.literal(" вышел на ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                    .append(Text.literal(f + ", " + f2 + ", " + f3).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
                NotificationUtil.notify(NotificationType.INFO, this.getDisplayName(), mutableText);
            }

            if (entity instanceof ItemEntity itemEntity && this.pickupSoundHeard && this.tracking.has("item_pickup")) {
                ItemStack itemStack = itemEntity.getStack();
                if (this.isValuableItem(itemStack)) {
                    String string2 = "shulker:" + itemStack.getItem().toString();
                    if (!this.isOnCooldown(this.pickupCooldowns, string2, 1500L)) {
                        int i = itemStack.getCount();
                        MutableText mutableText2 = Text.literal("Кто-то поднял ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)).append(itemStack.getName().copy());
                        if (i > 1) {
                            mutableText2.append(Text.literal(" ×" + i).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                        }

                        mutableText2.append(Text.literal(" (шалкер рядом)").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
                        this.sendNotification(mutableText2);
                    }
                }
            }
        }
    }

    private void checkLowHp() {
        if (this.tracking.has("low_hp")) {
            float f = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (f > 0.0F && f <= this.hpThreshold.getValue()) {
                if (!this.lowHpNotified) {
                    MutableText mutableText = Text.literal("Низкое здоровье")
                        .setStyle(Style.EMPTY.withColor(Formatting.RED))
                        .append(Text.literal(" — ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                        .append(Text.literal(String.valueOf((int)Math.ceil(f))).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                        .append(Text.literal(" HP").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                    NotificationUtil.notify(NotificationType.ERROR, this.getDisplayName(), mutableText);
                    this.lowHpNotified = true;
                    return;
                }
            } else if (f > this.hpThreshold.getValue() + 1.0F) {
                this.lowHpNotified = false;
            }
        }
    }

    private void checkArmorDurability() {
        if (this.tracking.has("armor_durability")) {
            if (this.armorCheckTimer.hasPassed(5000.0) && mc.player.hurtTime != 0) {
                this.armorCheckTimer.reset();

                for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack itemStack = mc.player.getEquippedStack(equipmentSlot);
                    if (!itemStack.isEmpty() && itemStack.getMaxDamage() != 0) {
                        float f = (float)(itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage();
                        if (!(f > this.durabilityPercent.getValue())) {
                            String string = equipmentSlot.name() + ":" + itemStack.getItem().toString();
                            if (!this.isOnCooldown(this.armorCooldowns, string, 15000L)) {
                                float f2 = Math.round(f * 1000.0F) / 10.0F;
                                MutableText mutableText = Text.literal(this.getArmorSlotName(equipmentSlot))
                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                    .append(Text.literal(" — ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                                    .append(itemStack.getName().copy())
                                    .append(Text.literal(" (" + String.format("%.1f", f2) + "%)").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
                                NotificationUtil.notify(NotificationType.WARNING, this.getDisplayName(), itemStack, mutableText);
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkPotionEffects() {
        if (this.tracking.has("potion_effects")) {
            int i = (int)(this.potionWarnSeconds.getValue() * 20.0F);

            for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
                RegistryEntry registryEntry = statusEffectInstance.getEffectType();
                if ((
                        registryEntry.equals(StatusEffects.STRENGTH)
                            || registryEntry.equals(StatusEffects.SPEED)
                            || registryEntry.equals(StatusEffects.REGENERATION)
                            || registryEntry.equals(StatusEffects.FIRE_RESISTANCE)
                            || registryEntry.equals(StatusEffects.RESISTANCE)
                            || registryEntry.equals(StatusEffects.HASTE)
                    )
                    && statusEffectInstance.getDuration() <= i
                    && statusEffectInstance.getDuration() >= i - 1) {
                    String string = registryEntry.getIdAsString() + ":" + statusEffectInstance.getAmplifier();
                    if (!this.isOnCooldown(this.effectCooldowns, string, 5000L)) {
                        int i2 = statusEffectInstance.getAmplifier() + 1;
                        String string2 = this.getEffectName(registryEntry);
                        MutableText mutableText = Text.literal(string2 + (i2 > 1 ? " " + this.toRoman(i2) : ""))
                            .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(Text.literal(" закончится через ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                            .append(Text.literal((int)Math.ceil(statusEffectInstance.getDuration() / 20.0) + " сек.").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
                        NotificationUtil.notify(NotificationType.WARNING, this.getDisplayName(), registryEntry, mutableText);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onUsingItemChange(UsingItemChangeEvent usingItemChangeEvent) {
        if (this.tracking.has("player_eating")) {
            if (usingItemChangeEvent.getEntity() instanceof PlayerEntity playerEntity && playerEntity != mc.player) {
                if (usingItemChangeEvent.isUsingItem()) {
                    ItemStack itemStack = playerEntity.getActiveItem();
                    if (itemStack != null && !itemStack.isEmpty()) {
                        this.itemUsers.put(playerEntity.getUuid(), new Tracker.ItemUseStart(itemStack.copy(), playerEntity.age));
                    }
                } else {
                    this.itemUsers.remove(playerEntity.getUuid());
                }
            }
        }
    }

    private void checkPlayerEating() {
        if (!this.tracking.has("player_eating")) {
            this.itemUsers.clear();
        } else if (!this.itemUsers.isEmpty()) {
            ArrayList arrayList = new ArrayList();

            for (Entry entry : this.itemUsers.entrySet()) {
                UUID uuid = (UUID)entry.getKey();
                Tracker.ItemUseStart itemUseStart = (Tracker.ItemUseStart)entry.getValue();
                PlayerEntity playerEntity = mc.world.getPlayerByUuid(uuid);
                if (playerEntity == null) {
                    arrayList.add(uuid);
                } else {
                    int i = playerEntity.age - itemUseStart.startAge;
                    if (i >= 25) {
                        ItemStack itemStack = itemUseStart.stack;
                        if (!itemStack.isEmpty() && this.isTrackedConsumable(itemStack) && !this.isOnCooldown(this.eatingCooldowns, uuid, 2500L)) {
                            this.notifyPlayerConsumed(playerEntity, itemStack);
                        }
                    }
                }
            }

            arrayList.forEach(this.itemUsers::remove);
        }
    }

    private void notifyPlayerConsumed(PlayerEntity playerEntity, ItemStack itemStack) {
        int i = itemStack.getCount();
        MutableText mutableText = Text.literal(playerEntity.getGameProfile().name())
            .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
            .append(Text.literal(" " + this.getConsumeVerb(itemStack) + " ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(itemStack.getName().copy());
        if (i > 1 && i <= itemStack.getMaxCount()) {
            mutableText.append(Text.literal(" ×" + i).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        }

        this.sendNotification(mutableText);
        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContentsComponent != null && potionContentsComponent.hasEffects()) {
            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                int i2 = statusEffectInstance.getAmplifier() + 1;
                MutableText mutableText2 = Text.literal(" ▸ ")
                    .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
                    .append(Text.literal(this.getEffectName(statusEffectInstance.getEffectType())).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                if (i2 > 1) {
                    mutableText2.append(Text.literal(" " + this.toRoman(i2)).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
                }

                if (!statusEffectInstance.getEffectType().value().isInstant()) {
                    mutableText2.append(Text.literal(" (" + this.formatDuration(statusEffectInstance.getDuration() / 20) + ")").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
                }

                this.addChatMessage(mutableText2);
            }
        }
    }

    private void addChatMessage(MutableText mutableText) {
        if (mc.player != null) {
            mc.execute(() -> {
                if (mc.player != null && mc.inGameHud != null) {
                    mc.inGameHud.getChatHud().addMessage(mutableText);
                }
            });
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (!ServerManager.isNullWorld()) {
            boolean flag;
            if (this.tracking.has("low_hp")) {
                flag = this.hpHud.getValue();
            } else {
                flag = false;
            }

            float f = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            boolean flag2 = flag && f > 0.0F && f <= this.hpThreshold.getValue();
            this.lowHpAnimation.update();
            this.lowHpAnimation.animate(flag2 ? 1.0 : 0.0, 0.35, Easings.QUART_OUT);
            float f2 = this.lowHpAnimation.get();
            if (!(f2 <= 0.005F)) {
                float f3 = Math.min(1.0F, Math.max(0.0F, f / Math.max(1.0F, this.hpThreshold.getValue())));
                float f4 = (float)Math.pow(1.0F - f3, 1.4);
                float f5 = 280.0F - 110.0F * f4;
                float f6 = 0.9F + 0.1F * (float)Math.sin((float)System.currentTimeMillis() / f5);
                float f7 = Math.min(1.0F, f2 * (0.15F + 0.3F * f4) * f6);
                DrawContext drawContext = render2DEvent.getContext();
                float f8 = mc.getWindow().getScaledWidth();
                float f9 = mc.getWindow().getScaledHeight();
                int i = ColorUtil.setAlpha(15022389, (int)(f7 * 255.0F));
                int i2 = ColorUtil.setAlpha(15022389, 0);
                float f10 = f9 * (0.2F + 0.15F * f4);
                float f11 = f8 * (0.15F + 0.1F * f4);
                new RenderUtil(drawContext, 0.0F, 0.0F, f8, f10).rect(i2, i, i2, i, Radius.zero());
                new RenderUtil(drawContext, 0.0F, f9 - f10, f8, f10).rect(i, i2, i, i2, Radius.zero());
                new RenderUtil(drawContext, 0.0F, 0.0F, f11, f9).rect(i2, i2, i, i, Radius.zero());
                new RenderUtil(drawContext, f8 - f11, 0.0F, f11, f9).rect(i, i, i2, i2, Radius.zero());
            }
        }
    }

    private ItemStack findPotionStack(double d, double d2, double d3) {
        Box box = new Box(d - 5.0, d2 - 3.0, d3 - 5.0, d + 5.0, d2 + 3.0, d3 + 5.0);
        List list = mc.world.getEntitiesByClass(PotionEntity.class, box, potionEntity -> true);
        return !list.isEmpty() ? ((PotionEntity)list.get(0)).getStack() : this.findRecentThrownPotion(d, d2, d3);
    }

    private ItemStack findRecentThrownPotion(double d, double d2, double d3) {
        long l = System.currentTimeMillis();
        ItemStack itemStack = null;
        double d4 = 25.0;

        for (int i = this.thrownPotions.size() - 1; i >= 0; i--) {
            Tracker.ThrownPotion thrownPotion = this.thrownPotions.get(i);
            if (l - thrownPotion.time > 2000L) {
                this.thrownPotions.remove(i);
            } else {
                double d5 = d - thrownPotion.x;
                double d6 = d2 - thrownPotion.y;
                double d7 = d3 - thrownPotion.z;
                double d8 = d5 * d5 + d6 * d6 + d7 * d7;
                if (d8 < d4) {
                    d4 = d8;
                    itemStack = thrownPotion.stack;
                }
            }
        }

        return itemStack;
    }

    private boolean isValuableItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) {
            return false;
        }

        NbtCompound nbtCompound = nbtComponent.copyNbt();
        String string = itemStack.getName().getString();
        return nbtCompound.contains("don-item") || nbtCompound.contains("spooky-item") || string.contains("сфера") || string.contains("талисман")
            ? true
            : itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || itemStack.isOf(Items.GOLDEN_APPLE)
                || itemStack.isOf(Items.NETHERITE_SWORD)
                || itemStack.isOf(Items.NETHERITE_PICKAXE)
                || itemStack.isOf(Items.NETHERITE_AXE)
                || itemStack.isOf(Items.NETHERITE_HELMET)
                || itemStack.isOf(Items.NETHERITE_CHESTPLATE)
                || itemStack.isOf(Items.NETHERITE_LEGGINGS)
                || itemStack.isOf(Items.NETHERITE_BOOTS)
                || itemStack.isOf(Items.TOTEM_OF_UNDYING)
                || itemStack.isOf(Items.ELYTRA)
                || itemStack.isOf(Items.SHULKER_BOX);
    }

    private boolean isTrackedConsumable(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        if (!itemStack.isOf(Items.POTION) && !itemStack.isOf(Items.SPLASH_POTION) && !itemStack.isOf(Items.LINGERING_POTION) && !itemStack.isOf(Items.GOLDEN_APPLE)) {
            if (itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                return true;
            }

            if (!itemStack.isOf(Items.CHORUS_FRUIT)) {
                if (itemStack.isOf(Items.HONEY_BOTTLE)) {
                    return true;
                }

                if (itemStack.isOf(Items.MILK_BUCKET)) {
                    return true;
                }

                return itemStack.contains(DataComponentTypes.FOOD);
            }
        }

        return true;
    }

    private String getConsumeVerb(ItemStack itemStack) {
        return !itemStack.isOf(Items.POTION) && !itemStack.isOf(Items.MILK_BUCKET) && !itemStack.isOf(Items.HONEY_BOTTLE) ? "съел" : "выпил";
    }

    private String getArmorSlotName(EquipmentSlot equipmentSlot) {
        switch (equipmentSlot) {
            case HEAD:
                return "Шлем";
            case CHEST:
                return "Нагрудник";
            case LEGS:
                return "Поножи";
            case FEET:
                return "Ботинки";
            default:
                return equipmentSlot.name();
        }
    }

    private String getEffectName(RegistryEntry<StatusEffect> registryEntry) {
        if (registryEntry.equals(StatusEffects.STRENGTH)) {
            return "Сила";
        } else if (registryEntry.equals(StatusEffects.SPEED)) {
            return "Скорость";
        } else if (registryEntry.equals(StatusEffects.REGENERATION)) {
            return "Регенерация";
        } else if (registryEntry.equals(StatusEffects.FIRE_RESISTANCE)) {
            return "Огнестойкость";
        } else if (registryEntry.equals(StatusEffects.RESISTANCE)) {
            return "Сопротивление";
        } else if (registryEntry.equals(StatusEffects.HASTE)) {
            return "Спешка";
        } else if (registryEntry.equals(StatusEffects.SLOWNESS)) {
            return "Замедление";
        } else if (registryEntry.equals(StatusEffects.WEAKNESS)) {
            return "Слабость";
        } else if (registryEntry.equals(StatusEffects.POISON)) {
            return "Отравление";
        } else if (registryEntry.equals(StatusEffects.WITHER)) {
            return "Иссушение";
        } else if (registryEntry.equals(StatusEffects.HUNGER)) {
            return "Голод";
        } else if (registryEntry.equals(StatusEffects.BLINDNESS)) {
            return "Слепота";
        } else if (registryEntry.equals(StatusEffects.INVISIBILITY)) {
            return "Невидимость";
        } else if (registryEntry.equals(StatusEffects.HEALTH_BOOST)) {
            return "Прилив здоровья";
        } else if (registryEntry.equals(StatusEffects.INSTANT_HEALTH)) {
            return "Мгн. здоровье";
        } else if (registryEntry.equals(StatusEffects.INSTANT_DAMAGE)) {
            return "Мгн. урон";
        } else if (registryEntry.equals(StatusEffects.JUMP_BOOST)) {
            return "Прыгучесть";
        } else if (registryEntry.equals(StatusEffects.MINING_FATIGUE)) {
            return "Усталость";
        } else if (registryEntry.equals(StatusEffects.GLOWING)) {
            return "Свечение";
        } else if (registryEntry.equals(StatusEffects.NIGHT_VISION)) {
            return "Ночное зрение";
        } else {
            return registryEntry.equals(StatusEffects.ABSORPTION) ? "Поглощение" : registryEntry.getIdAsString();
        }
    }

    private String toRoman(int i) {
        switch (i) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return String.valueOf(i);
        }
    }

    private String formatDuration(int i) {
        if (i < 60) {
            return i + "с";
        }

        int i2 = i / 60;
        int i3 = i % 60;
        return i3 == 0 ? i2 + "м" : i2 + "м " + i3 + "с";
    }

    private double squaredDistanceToEyes(double d, double d2, double d3, Entity entity) {
        Vec3d vec3d = entity.getEyePos();
        double d4 = d - vec3d.x;
        double d5 = d2 - vec3d.y;
        double d6 = d3 - vec3d.z;
        return d4 * d4 + d5 * d5 + d6 * d6;
    }

    private boolean isNearSolidBlock(double d, double d2, double d3) {
        BlockPos blockPos = BlockPos.ofFloored(d, d2, d3);
        if (mc.world.getBlockState(blockPos).isSolidBlock(mc.world, blockPos)) {
            return true;
        } else if (mc.world.getBlockState(blockPos.down()).isSolidBlock(mc.world, blockPos.down())) {
            return true;
        } else if (mc.world.getBlockState(blockPos.north()).isSolidBlock(mc.world, blockPos.north())) {
            return true;
        } else if (mc.world.getBlockState(blockPos.south()).isSolidBlock(mc.world, blockPos.south())) {
            return true;
        } else {
            return mc.world.getBlockState(blockPos.east()).isSolidBlock(mc.world, blockPos.east())
                ? true
                : mc.world.getBlockState(blockPos.west()).isSolidBlock(mc.world, blockPos.west());
        }
    }

    private Optional<SpecialPotion> findSpecialPotionByName(String string) {
        for (SpecialPotion specialPotion : SpecialPotion.values()) {
            if (specialPotion != SpecialPotion.NULL && specialPotion.getDisplayName().getString().equals(string)) {
                return Optional.of(specialPotion);
            }
        }

        return Optional.empty();
    }

    private Optional<SpecialPotion> findSpecialPotionByColor(int i) {
        for (SpecialPotion specialPotion : SpecialPotion.values()) {
            if (specialPotion != SpecialPotion.NULL && (specialPotion.getColor() == i || specialPotion.getParticleColors().contains(i))) {
                return Optional.of(specialPotion);
            }
        }

        SpecialPotion specialPotion2 = null;
        double d = Double.MAX_VALUE;

        for (SpecialPotion specialPotion3 : SpecialPotion.values()) {
            if (specialPotion3 != SpecialPotion.NULL) {
                double d2 = this.colorDistance(i, specialPotion3.getColor());
                if (d2 < d && d2 < 30.0) {
                    d = d2;
                    specialPotion2 = specialPotion3;
                }
            }
        }

        return Optional.ofNullable(specialPotion2);
    }

    private double colorDistance(int i, int i2) {
        int i3 = i >> 16 & 0xFF;
        int i4 = i >> 8 & 0xFF;
        int i5 = i & 0xFF;
        int i6 = i2 >> 16 & 0xFF;
        int i7 = i2 >> 8 & 0xFF;
        int i8 = i2 & 0xFF;
        double d = (i3 + i6) / 2.0;
        double d2 = i3 - i6;
        double d3 = i4 - i7;
        double d4 = i5 - i8;
        double d5 = 2.0 + d / 256.0;
        double d6 = 2.0 + (255.0 - d) / 256.0;
        return Math.sqrt(d5 * d2 * d2 + 4.0 * d3 * d3 + d6 * d4 * d4);
    }

    private <K> boolean isOnCooldown(Map<K, Long> map, K value, long l) {
        long l2 = System.currentTimeMillis();
        Long longValue = (Long)map.get(value);
        if (longValue != null && l2 - longValue < l) {
            return true;
        }

        map.put(value, l2);
        if (map.size() > 256) {
            long l3 = l2 - l * 4L;
            map.values().removeIf(longValue2 -> longValue2 < l3);
        }

        return false;
    }

    private void sendNotification(MutableText mutableText) {
        if (mc.player != null) {
            mc.execute(
                () -> {
                    if (mc.player != null && mc.inGameHud != null) {
                        MutableText mutableText2 = this.createText(" Sadnes client")
                            .append(Text.literal(" → ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                            .append(mutableText);
                        mc.inGameHud.getChatHud().addMessage(mutableText2);
                    }
                }
            );
        }
    }

    private MutableText createText(String string) {
        MutableText mutableText = Text.literal("");
        int i = Theme.getPrimary();
        int i2 = Theme.getAccentLight();
        int i3 = string.length();

        for (int i4 = 0; i4 < i3; i4++) {
            float f = i3 <= 1 ? 0.0F : (float)i4 / (i3 - 1);
            int i5 = ColorUtil.interpolate(i, i2, f) & 16777215;
            mutableText.append(Text.literal(String.valueOf(string.charAt(i4))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(i5)).withBold(true)));
        }

        return mutableText;
    }

    public MultiSetting getTracking() {
        return this.tracking;
    }

    public NumberSetting getHpThreshold() {
        return this.hpThreshold;
    }

    public BooleanSetting getHpHud() {
        return this.hpHud;
    }

    public NumberSetting getDurabilityPercent() {
        return this.durabilityPercent;
    }

    public NumberSetting getPotionWarnSeconds() {
        return this.potionWarnSeconds;
    }

    public boolean isLowHpNotified() {
        return this.lowHpNotified;
    }

    public StopWatch getArmorCheckTimer() {
        return this.armorCheckTimer;
    }

    public boolean isPickupSoundHeard() {
        return this.pickupSoundHeard;
    }

    public StopWatch getPickupSoundTimer() {
        return this.pickupSoundTimer;
    }

    public Map<UUID, Long> getTotemCooldowns() {
        return this.totemCooldowns;
    }

    public Map<String, Long> getPickupCooldowns() {
        return this.pickupCooldowns;
    }

    public Map<String, Long> getArmorCooldowns() {
        return this.armorCooldowns;
    }

    public Map<String, Long> getEffectCooldowns() {
        return this.effectCooldowns;
    }

    public Map<UUID, Long> getEatingCooldowns() {
        return this.eatingCooldowns;
    }

    public Map<UUID, Tracker.ItemUseStart> getItemUsers() {
        return this.itemUsers;
    }

    public List<Tracker.ThrownPotion> getThrownPotions() {
        return this.thrownPotions;
    }

    public Animation getLowHpAnimation() {
        return this.lowHpAnimation;
    }

    record ItemUseStart(ItemStack stack, int startAge) {
    }

    record ThrownPotion(double x, double y, double z, ItemStack stack, long time) {
    }
}
