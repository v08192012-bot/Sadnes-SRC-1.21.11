package tech.sadnes.client.handler;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.impl.movement.Flight;
import tech.sadnes.client.util.IMinecraft;

public class ServerPacketFilter {
    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (IMinecraft.mc.player != null && IMinecraft.mc.world != null) {
            if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                String string = gameMessageS2CPacket.content().getString();
                String string2 = IMinecraft.mc.player.getName().getString();
                if (string.contains("Grim") && !string.contains(string2)) {
                    packetEvent.cancel();
                }
            }

            Packet packet = packetEvent.getPacket();
            if (packet instanceof ClickSlotC2SPacket) {
                ;
            }

            if (packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket
                && Flight.getInstance().isEnabled()
                && Flight.getInstance().getFlightMode().is("Funtime Elytra")) {
                RegistryEntry registryEntry = playSoundS2CPacket.getSound();
                if (registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_GENERIC
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_LEATHER
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_CHAIN
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_IRON
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_GOLD
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND
                    || registryEntry == SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE) {
                    packetEvent.cancel();
                }
            }
        }
    }
}
