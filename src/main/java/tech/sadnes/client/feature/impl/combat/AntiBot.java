package tech.sadnes.client.feature.impl.combat;

import com.mojang.authlib.GameProfile;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ModeSetting;

@FeatureInfo(
    name = "AntiBot",
    description = "feature.impl.combat.antibot",
    displayName = "feature.impl.combat.antibot",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public final class AntiBot extends Feature {
    private final ModeSetting mode = new ModeSetting("antibot.mode", new String[]{"matrix", "normal"});
    private final Set<UUID> pendingProfiles = new HashSet<>();
    private final Set<UUID> botIds = new HashSet<>();
    private final Set<UUID> suspiciousArmorIds = new HashSet<>();

    public static AntiBot getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AntiBot.class);
    }

    public static boolean isBotStatic(PlayerEntity playerEntity) {
        AntiBot antiBot = getInstance();
        if (antiBot != null) {
            return antiBot.isEnabled() ? antiBot.isBot(playerEntity) : false;
        } else {
            return false;
        }
    }

    public boolean isBot(PlayerEntity playerEntity) {
        ModeSetting modeSetting = this.mode;
        if (playerEntity != null) {
            boolean flag = modeSetting.is("normal");
            Set set = this.suspiciousArmorIds;
            if (flag) {
                boolean flag2 = ServerManager.isInvalidName(playerEntity.getGameProfile().name());
                Set set2 = this.botIds;
                if (!flag2) {
                    UUID uuid = playerEntity.getUuid();
                    return set2.contains(uuid);
                } else {
                    return true;
                }
            } else {
                UUID uuid2 = playerEntity.getUuid();
                return set.contains(uuid2);
            }
        } else {
            return false;
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.mode.is("normal") && packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof PlayerListS2CPacket playerListS2CPacket) {
                EnumSet enumSet = playerListS2CPacket.getActions();
                Action action = Action.ADD_PLAYER;
                if (enumSet.contains(action)) {
                    this.handlePlayerListAdd(playerListS2CPacket);
                    return;
                }
            }

            Packet packet = packetEvent.getPacket();
            if (packet instanceof PlayerRemoveS2CPacket) {
                Iterator iterator2 = ((PlayerRemoveS2CPacket)packet).profileIds().iterator();
                if (iterator2.hasNext()) {
                    do {
                        UUID uuid = (UUID)iterator2.next();
                        this.pendingProfiles.remove(uuid);
                        this.botIds.remove(uuid);
                    } while (iterator2.hasNext());

                    return;
                }
            }
        }
    }

    @EventHandler
    public void onUpdate(HudPostRenderEvent hudPostRenderEvent) {
        ClientWorld clientWorld = mc.world;
        ModeSetting modeSetting = this.mode;
        if (clientWorld != null) {
            boolean flag = modeSetting.is("normal");
            Set set = this.pendingProfiles;
            if (flag) {
                if (!set.isEmpty()) {
                    Iterator iterator2 = mc.world.getPlayers().iterator();
                    if (iterator2.hasNext()) {
                        do {
                            PlayerEntity playerEntity = (PlayerEntity)iterator2.next();
                            UUID uuid = playerEntity.getUuid();
                            if (set.contains(uuid)) {
                                boolean flag2 = this.hasFullUnenchantedArmor(playerEntity);
                                Set set2 = this.botIds;
                                if (flag2) {
                                    UUID uuid2 = playerEntity.getUuid();
                                    set2.add(uuid2);
                                }

                                UUID uuid3 = playerEntity.getUuid();
                                set.remove(uuid3);
                            }
                        } while (iterator2.hasNext());
                    }
                }
            } else {
                Iterator iterator3 = mc.world.getPlayers().iterator();
                if (iterator3.hasNext()) {
                    do {
                        PlayerEntity playerEntity2 = (PlayerEntity)iterator3.next();
                        if (playerEntity2 != null) {
                            ClientPlayerEntity clientPlayerEntity = mc.player;
                            if (playerEntity2 != clientPlayerEntity) {
                                boolean flag3 = this.hasSuspiciousArmor(playerEntity2);
                                Set set3 = this.suspiciousArmorIds;
                                if (!flag3) {
                                    UUID uuid4 = playerEntity2.getUuid();
                                    set3.remove(uuid4);
                                } else {
                                    UUID uuid5 = playerEntity2.getUuid();
                                    set3.add(uuid5);
                                }
                            }
                        }
                    } while (iterator3.hasNext());
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.reset();
    }

    private void handlePlayerListAdd(PlayerListS2CPacket playerListS2CPacket) {
        if (mc.getNetworkHandler() != null) {
            Iterator iterator2 = playerListS2CPacket.getPlayerAdditionEntries().iterator();
            if (iterator2.hasNext()) {
                do {
                    Entry entry = (Entry)iterator2.next();
                    GameProfile gameProfile = entry.profile();
                    if (gameProfile != null && entry.latency() >= 5 && gameProfile.properties().isEmpty()) {
                        boolean flag = this.hasDuplicateName(gameProfile);
                        Set set = this.botIds;
                        Set set2 = this.pendingProfiles;
                        if (!flag) {
                            UUID uuid = gameProfile.id();
                            set2.add(uuid);
                        } else {
                            UUID uuid2 = gameProfile.id();
                            set.add(uuid2);
                        }
                    }
                } while (iterator2.hasNext());
            }
        }
    }

    private boolean hasDuplicateName(GameProfile gameProfile) {
        Stream<PlayerListEntry> stream2 = mc.getNetworkHandler().getPlayerList().stream();
        Predicate<PlayerListEntry> predicate = playerListEntry -> playerListEntry.getProfile().name().equals(gameProfile.name()) && !playerListEntry.getProfile().id().equals(gameProfile.id());
        return stream2.filter(predicate).count() == 1L;
    }

    private boolean hasFullUnenchantedArmor(PlayerEntity playerEntity) {
        EquipmentSlot equipmentSlot = EquipmentSlot.HEAD;
        ItemStack itemStack = playerEntity.getEquippedStack(equipmentSlot);
        if (this.isUnenchantedArmor(itemStack)) {
            EquipmentSlot equipmentSlot2 = EquipmentSlot.CHEST;
            ItemStack itemStack2 = playerEntity.getEquippedStack(equipmentSlot2);
            if (this.isUnenchantedArmor(itemStack2)) {
                EquipmentSlot equipmentSlot3 = EquipmentSlot.LEGS;
                ItemStack itemStack3 = playerEntity.getEquippedStack(equipmentSlot3);
                if (this.isUnenchantedArmor(itemStack3)) {
                    EquipmentSlot equipmentSlot4 = EquipmentSlot.FEET;
                    ItemStack itemStack4 = playerEntity.getEquippedStack(equipmentSlot4);
                    return this.isUnenchantedArmor(itemStack4);
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

    private boolean isUnenchantedArmor(ItemStack itemStack) {
        ComponentType<EquippableComponent> componentType = DataComponentTypes.EQUIPPABLE;
        EquippableComponent equippableComponent = itemStack.get(componentType);
        if (equippableComponent != null) {
            return equippableComponent.slot().isArmorSlot() ? !itemStack.hasEnchantments() : false;
        } else {
            return false;
        }
    }

    private boolean hasSuspiciousArmor(PlayerEntity playerEntity) {
        EquipmentSlot equipmentSlot = EquipmentSlot.HEAD;
        ItemStack itemStack = playerEntity.getEquippedStack(equipmentSlot);
        EquipmentSlot equipmentSlot2 = EquipmentSlot.CHEST;
        ItemStack itemStack2 = playerEntity.getEquippedStack(equipmentSlot2);
        EquipmentSlot equipmentSlot3 = EquipmentSlot.LEGS;
        ItemStack itemStack3 = playerEntity.getEquippedStack(equipmentSlot3);
        EquipmentSlot equipmentSlot4 = EquipmentSlot.FEET;
        ItemStack itemStack4 = playerEntity.getEquippedStack(equipmentSlot4);
        byte b;
        if (!itemStack.isEmpty()) {
            if (!itemStack2.isEmpty()) {
                if (!itemStack3.isEmpty()) {
                    b = (byte)(itemStack4.isEmpty() ? 1 : 0);
                } else {
                    b = 1;
                }
            } else {
                b = 1;
            }
        } else {
            b = 1;
        }

        byte b2;
        if (itemStack.isEnchantable()) {
            if (itemStack2.isEnchantable()) {
                if (itemStack3.isEnchantable()) {
                    b2 = (byte)(itemStack4.isEnchantable() ? 0 : 1);
                } else {
                    b2 = 1;
                }
            } else {
                b2 = 1;
            }
        } else {
            b2 = 1;
        }

        if (!itemStack.isDamaged() && !itemStack2.isDamaged() && !itemStack3.isDamaged() && !itemStack4.isDamaged()) {
            Item item = Items.LEATHER_BOOTS;
            if (!itemStack4.isOf(item)) {
                Item item2 = Items.LEATHER_LEGGINGS;
                if (!itemStack3.isOf(item2)) {
                    Item item3 = Items.LEATHER_CHESTPLATE;
                    if (!itemStack2.isOf(item3)) {
                        Item item4 = Items.LEATHER_HELMET;
                        if (!itemStack.isOf(item4)) {
                            Item item5 = Items.IRON_BOOTS;
                            if (!itemStack4.isOf(item5)) {
                                Item item6 = Items.IRON_LEGGINGS;
                                if (!itemStack3.isOf(item6)) {
                                    Item item7 = Items.IRON_CHESTPLATE;
                                    if (!itemStack2.isOf(item7)) {
                                        Item item8 = Items.IRON_HELMET;
                                        int i = itemStack.isOf(item8) ? 0 : 1;
                                        if (!playerEntity.getOffHandStack().isEmpty() || playerEntity.getMainHandStack().isEmpty()) {
                                            return false;
                                        }

                                        int i2 = b | b2;
                                        if ((i | i2) != 0) {
                                            return false;
                                        }

                                        return playerEntity.getHungerManager().getFoodLevel() == 20;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!playerEntity.getOffHandStack().isEmpty() || playerEntity.getMainHandStack().isEmpty()) {
                return false;
            }

            int i3 = b | b2;
            return (0 | i3) != 0 ? false : playerEntity.getHungerManager().getFoodLevel() == 20;
        } else {
            Item item9 = Items.LEATHER_BOOTS;
            if (!itemStack4.isOf(item9)) {
                Item item10 = Items.LEATHER_LEGGINGS;
                if (!itemStack3.isOf(item10)) {
                    Item item11 = Items.LEATHER_CHESTPLATE;
                    if (!itemStack2.isOf(item11)) {
                        Item item12 = Items.LEATHER_HELMET;
                        if (!itemStack.isOf(item12)) {
                            Item item13 = Items.IRON_BOOTS;
                            if (!itemStack4.isOf(item13)) {
                                Item item14 = Items.IRON_LEGGINGS;
                                if (!itemStack3.isOf(item14)) {
                                    Item item15 = Items.IRON_CHESTPLATE;
                                    if (!itemStack2.isOf(item15)) {
                                        Item item16 = Items.IRON_HELMET;
                                        itemStack.isOf(item16);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (playerEntity.getOffHandStack().isEmpty()) {
                playerEntity.getMainHandStack().isEmpty();
                return false;
            } else {
                return false;
            }
        }
    }

    private void reset() {
        this.pendingProfiles.clear();
        this.botIds.clear();
        this.suspiciousArmorIds.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.reset();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.reset();
    }
}
