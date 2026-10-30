package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tech.sadnes.api.duck.ChinaHatHolder;
import tech.sadnes.api.duck.LabelHideable;
import tech.sadnes.api.duck.TransparentAlphaHolder;
import tech.sadnes.client.render.ChinaHatPhysics;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements ChinaHatHolder, LabelHideable, TransparentAlphaHolder {
    @Unique
    private float sadnes$transparentAlpha = 1.0F;
    @Unique
    private boolean sadnes$hideLabel;
    @Unique
    private ChinaHatPhysics sadnes$chinaHat;
    @Unique
    private boolean sadnes$chinaHatHelmet;

    @Override
    public float sadnes$getTransparentAlpha() {
        return this.sadnes$transparentAlpha;
    }

    @Override
    public void sadnes$setTransparentAlpha(float f) {
        this.sadnes$transparentAlpha = f;
    }

    @Override
    public boolean sadnes$hideLabel() {
        return this.sadnes$hideLabel;
    }

    @Override
    public void sadnes$setHideLabel(boolean flag) {
        this.sadnes$hideLabel = flag;
    }

    @Override
    public ChinaHatPhysics sadnes$chinaHat() {
        return this.sadnes$chinaHat;
    }

    @Override
    public void sadnes$setChinaHat(ChinaHatPhysics chinaHatPhysics) {
        this.sadnes$chinaHat = chinaHatPhysics;
    }

    @Override
    public boolean sadnes$chinaHatHelmet() {
        return this.sadnes$chinaHatHelmet;
    }

    @Override
    public void sadnes$setChinaHatHelmet(boolean flag) {
        this.sadnes$chinaHatHelmet = flag;
    }
}
