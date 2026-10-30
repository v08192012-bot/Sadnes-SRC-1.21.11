package tech.sadnes.api.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface PlayerInteractEntityC2SPacketAccessor {
    @Accessor("entityId")
    int sadnes$getEntityId();

    @Mutable
    @Accessor("entityId")
    void sadnes$setEntityId(int i);

    @Invoker("attack")
    static PlayerInteractEntityC2SPacket sadnes$attack(Entity entity, boolean flag) {
        throw new AssertionError();
    }

    @Invoker("interact")
    static PlayerInteractEntityC2SPacket sadnes$interact(Entity entity, boolean flag, Hand hand) {
        throw new AssertionError();
    }

    @Invoker("interactAt")
    static PlayerInteractEntityC2SPacket sadnes$interactAt(Entity entity, boolean flag, Hand hand, Vec3d vec3d) {
        throw new AssertionError();
    }
}
