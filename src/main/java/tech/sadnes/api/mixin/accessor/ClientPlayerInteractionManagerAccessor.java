package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("currentBreakingProgress")
    float sadnes$getCurrentBreakingProgress();

    @Accessor("currentBreakingPos")
    BlockPos sadnes$getCurrentBreakingPos();

    @Invoker("syncSelectedSlot")
    void sadnes$invokeSyncSelectedSlot();

    @Accessor("lastSelectedSlot")
    int sadnes$getLastSelectedSlot();

    @Accessor("lastSelectedSlot")
    void sadnes$setLastSelectedSlot(int i);

    @Invoker("sendSequencedPacket")
    void invokeSendSequencedPacket(ClientWorld clientWorld, SequencedPacketCreator sequencedPacketCreator);
}
