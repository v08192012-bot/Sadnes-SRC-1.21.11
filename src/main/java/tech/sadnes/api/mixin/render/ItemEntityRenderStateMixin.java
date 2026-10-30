package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tech.sadnes.api.duck.TumbleState;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements TumbleState {
    @Unique
    private boolean sadnes$tumbling;
    @Unique
    private float sadnes$tumblePitch;

    @Override
    public boolean sadnes$tumbling() {
        return this.sadnes$tumbling;
    }

    @Override
    public void sadnes$setTumbling(boolean flag) {
        this.sadnes$tumbling = flag;
    }

    @Override
    public float sadnes$tumblePitch() {
        return this.sadnes$tumblePitch;
    }

    @Override
    public void sadnes$setTumblePitch(float f) {
        this.sadnes$tumblePitch = f;
    }
}
