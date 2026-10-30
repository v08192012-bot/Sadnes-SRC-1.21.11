package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor("lastSprinting")
    boolean sadnes$getLastSprinting();

    @Accessor("lastSprinting")
    void sadnes$setLastSprinting(boolean flag);

    @Accessor("lastPlayerInput")
    void sadnes$setLastPlayerInput(PlayerInput playerInput);

    @Invoker("sendSprintingPacket")
    void sadnes$invokeSendSprintingPacket();

    @Invoker("canStartSprinting")
    boolean sadnes$invokeCanStartSprinting();

    @Invoker("shouldStopSprinting")
    boolean sadnes$invokeShouldStopSprinting();
}
