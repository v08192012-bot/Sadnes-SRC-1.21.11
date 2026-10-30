package tech.sadnes.api.mixin.accessor;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("movementMultiplier")
    Vec3d sadnes$getMovementMultiplier();

    @Accessor("fluidHeight")
    Object2DoubleMap<TagKey<Fluid>> sadnes$getFluidHeight();

    @Accessor("submergedFluidTag")
    Set<TagKey<Fluid>> sadnes$getSubmergedFluidTag();

    @Invoker("movementInputToVelocity")
    static Vec3d sadnes$invokeMovementInputToVelocity(Vec3d vec3d, float f, float f2) {
        throw new AssertionError();
    }
}
