package tech.sadnes.api.mixin.compat;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.movement.Baritone;

@Mixin(value = Movement.class, remap = false)
public abstract class BaritoneMovementMixin {
    @Shadow
    @Final
    protected IBaritone baritone;
    @Shadow
    private MovementState currentState;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void sadnes$continueJump(CallbackInfoReturnable<MovementStatus> callbackInfoReturnable) {
        Baritone baritone2 = Baritone.getInstance();
        if (baritone2 != null) {
            MovementStatus movementStatus = baritone2.overrideMovementStatus(this.baritone, (Movement)(Object)this);
            if (movementStatus != null) {
                callbackInfoReturnable.setReturnValue(movementStatus);
            }
        }
    }

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lbaritone/pathing/movement/MovementState;getTarget()Lbaritone/pathing/movement/MovementState$MovementTarget;",
            ordinal = 0
        )
    )
    private void sadnes$improveMovement(CallbackInfoReturnable<MovementStatus> callbackInfoReturnable) {
        Baritone baritone2 = Baritone.getInstance();
        if (baritone2 != null) {
            baritone2.modifyMovement(this.baritone, (Movement)(Object)this, this.currentState);
        }
    }
}
