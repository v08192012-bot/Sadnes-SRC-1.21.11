package tech.sadnes.api.mixin.compat;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.process.BuilderProcess;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.misc.AutoBuild;

@Mixin(value = BuilderProcess.class, remap = false)
public class BaritoneBuilderMixin {
    @Inject(method = "placeAt", at = @At("HEAD"), cancellable = true, require = 0)
    private void sadnes$scaffoldBlock(int i, int i2, int i3, BlockState blockState, CallbackInfoReturnable<BlockState> callbackInfoReturnable) {
        BlockState blockState2 = AutoBuild.getSchematicBlockAt(i, i2, i3);
        if (blockState2 != null) {
            callbackInfoReturnable.setReturnValue(blockState2);
        }
    }

    @ModifyArg(
        method = "assemble(Lbaritone/process/BuilderProcess$BuilderCalculationContext;Ljava/util/List;Z)Lbaritone/api/pathing/goals/Goal;",
        at = @At(
            value = "INVOKE",
            target = "Lbaritone/process/BuilderProcess$JankyGoalComposite;<init>(Lbaritone/api/pathing/goals/Goal;Lbaritone/api/pathing/goals/Goal;)V"
        ),
        index = 0,
        require = 0
    )
    private Goal sadnes$rememberPlacementGoals(Goal goal) {
        if (goal instanceof GoalComposite) {
            AutoBuild.setPlacementGoals(((GoalComposite)goal).goals());
        }

        return goal;
    }
}
