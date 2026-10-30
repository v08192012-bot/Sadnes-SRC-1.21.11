package tech.sadnes.api.mixin.client;

import java.util.Arrays;
import net.minecraft.client.option.InactivityFpsLimit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InactivityFpsLimit.class)
public abstract class InactivityFpsLimitMixin {
    @Shadow
    @Final
    @Mutable
    private static InactivityFpsLimit[] field_52749;

    @Invoker("<init>")
    private static InactivityFpsLimit sadnes$new(String string, int i, String string2, String string3) {
        throw new AssertionError();
    }

    @Inject(
        method = "<clinit>",
        at = @At(
            value = "FIELD",
            opcode = 179,
            target = "Lnet/minecraft/client/option/InactivityFpsLimit;field_52749:[Lnet/minecraft/client/option/InactivityFpsLimit;",
            shift = Shift.AFTER
        )
    )
    private static void sadnes$addNever(CallbackInfo callbackInfo) {
        InactivityFpsLimit inactivityFpsLimit = sadnes$new("NEVER", field_52749.length, "never", "options.inactivityFpsLimit.never");
        InactivityFpsLimit[] inactivityFpsLimits = Arrays.copyOf(field_52749, field_52749.length + 1);
        inactivityFpsLimits[field_52749.length] = inactivityFpsLimit;
        field_52749 = inactivityFpsLimits;
    }
}
