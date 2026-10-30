package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("thirdPerson")
    boolean sadnes$isThirdPerson();

    @Accessor("thirdPerson")
    void sadnes$setThirdPerson(boolean flag);
}
