package tech.sadnes.api.mixin.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityAttributeInstance.class)
public class EntityAttributeInstanceMixin {
    @Mutable
    @Shadow
    @Final
    private Map<Identifier, EntityAttributeModifier> idToModifiers;

    @Inject(method = {"addModifier", "addTemporaryModifier"}, at = @At("HEAD"), require = 0)
    private void onAddModifier(EntityAttributeModifier entityAttributeModifier, CallbackInfo callbackInfo) {
        if (this.idToModifiers instanceof Object2ObjectArrayMap && this.idToModifiers.size() >= 16) {
            this.idToModifiers = new Object2ObjectOpenHashMap<>(this.idToModifiers);
        }
    }
}
