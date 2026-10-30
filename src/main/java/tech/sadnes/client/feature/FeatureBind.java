package tech.sadnes.client.feature;

import tech.sadnes.client.animation.Animation;

public class FeatureBind extends Bind {
    private boolean enableState;
    private boolean shown;
    private final Animation bindAnimation = new Animation();

    public FeatureBind(int i, BindMode bindMode) {
        this(i, bindMode, true, true);
    }

    public FeatureBind(int i, BindMode bindMode, boolean flag) {
        this(i, bindMode, true, flag);
    }

    public FeatureBind(int i, BindMode bindMode, boolean flag, boolean flag2) {
        this.key = i;
        this.mode = bindMode != null ? bindMode : BindMode.HOLD;
        this.enableState = flag;
        this.shown = flag2;
    }

    public boolean getEnableState() {
        return this.enableState;
    }

    @Override
    public boolean isVisible() {
        return this.shown;
    }

    @Override
    public Animation getAnimation() {
        return this.bindAnimation;
    }

    public void setEnableState(boolean flag) {
        this.enableState = flag;
    }

    @Override
    public void setVisible(boolean flag) {
        this.shown = flag;
    }
}
