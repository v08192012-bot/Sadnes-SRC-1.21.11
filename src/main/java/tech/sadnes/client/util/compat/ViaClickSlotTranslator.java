package tech.sadnes.client.util.compat;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.lang.reflect.Method;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.screen.sync.ItemStackHash.Impl;

final class ViaClickSlotTranslator {
    private static volatile Method mcToViaMethod;

    private ViaClickSlotTranslator() {
    }

    static boolean isSupported() {
        ProtocolVersion protocolVersion = ProtocolTranslator.getTargetVersion();
        return protocolVersion != null && protocolVersion.olderThanOrEqualTo(ProtocolVersion.v1_21_4);
    }

    static void sendClickSlot(ClickSlotC2SPacket clickSlotC2SPacket) throws ReflectiveOperationException {
        PacketWrapper packetWrapper = PacketWrapper.create(ServerboundPackets1_21_4.CONTAINER_CLICK, ProtocolTranslator.getPlayNetworkUserConnection());
        packetWrapper.write(Types.VAR_INT, clickSlotC2SPacket.syncId());
        packetWrapper.write(Types.VAR_INT, clickSlotC2SPacket.revision());
        packetWrapper.write(Types.SHORT, clickSlotC2SPacket.slot());
        packetWrapper.write(Types.BYTE, clickSlotC2SPacket.button());
        packetWrapper.write(Types.VAR_INT, clickSlotC2SPacket.actionType().getIndex());
        Int2ObjectMap<ItemStackHash> int2ObjectMap = clickSlotC2SPacket.modifiedStacks();
        packetWrapper.write(Types.VAR_INT, int2ObjectMap.size());

        for (Entry<ItemStackHash> entry : int2ObjectMap.int2ObjectEntrySet()) {
            packetWrapper.write(Types.SHORT, (short)entry.getIntKey());
            packetWrapper.write(VersionedTypes.V1_21_4.item, toViaItem((ItemStackHash)entry.getValue()));
        }

        packetWrapper.write(VersionedTypes.V1_21_4.item, toViaItem(clickSlotC2SPacket.cursor()));
        packetWrapper.scheduleSendToServer(Protocol1_21_4To1_21_5.class);
    }

    private static Item toViaItem(ItemStackHash itemStackHash) throws ReflectiveOperationException {
        ItemStack itemStack = itemStackHash instanceof Impl impl ? new ItemStack(impl.item(), impl.count()) : ItemStack.EMPTY;
        return (Item)getMcToViaMethod().invoke(null, itemStack, ProtocolVersion.v1_21_4);
    }

    private static Method getMcToViaMethod() throws ReflectiveOperationException {
        Method method = mcToViaMethod;
        if (method != null) {
            return method;
        }

        Class clazz = Class.forName("com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator");

        for (Method method2 : clazz.getMethods()) {
            if (method2.getName().equals("mcToVia") && method2.getParameterCount() == 2) {
                mcToViaMethod = method2;
                return method2;
            }
        }

        throw new NoSuchMethodException("ItemTranslator.mcToVia");
    }
}
