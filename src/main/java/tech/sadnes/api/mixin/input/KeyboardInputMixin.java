package tech.sadnes.api.mixin.input;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.event.impl.MoveInputEvent;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Unique
    private MoveInputEvent sadnes$event;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyboardInput;getMovementMultiplier(ZZ)F", ordinal = 0))
    private float sadnes$forwardAxis(boolean flag, boolean flag2) {
        float f = inputAxis(flag, flag2);
        float f2 = inputAxis(this.playerInput.left(), this.playerInput.right());
        this.movementVector = new Vec2f(f2, f);
        this.sadnes$event = new MoveInputEvent(f, f2, this.playerInput.jump(), this.playerInput.sneak(), 0.3);
        this.sadnes$event.post();
        if (this.sadnes$event.isCancelled()) {
            this.playerInput = new PlayerInput(false, false, false, false, false, false, this.playerInput.sprint());
            return 0.0F;
        } else {
            float f3 = MathHelper.clamp(this.sadnes$event.getForward(), -1.0F, 1.0F);
            float f4 = MathHelper.clamp(this.sadnes$event.getStrafe(), -1.0F, 1.0F);
            this.playerInput = new PlayerInput(
                f3 > 0.0F, f3 < 0.0F, f4 > 0.0F, f4 < 0.0F, this.sadnes$event.isJump(), this.sadnes$event.isSneak(), this.playerInput.sprint()
            );
            return f3;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyboardInput;getMovementMultiplier(ZZ)F", ordinal = 1))
    private float sadnes$strafeAxis(boolean flag, boolean flag2) {
        return this.sadnes$event.isCancelled() ? 0.0F : MathHelper.clamp(this.sadnes$event.getStrafe(), -1.0F, 1.0F);
    }

    private static float inputAxis(boolean flag, boolean flag2) {
        if (flag == flag2) {
            return 0.0F;
        } else {
            return flag ? 1.0F : -1.0F;
        }
    }
}
