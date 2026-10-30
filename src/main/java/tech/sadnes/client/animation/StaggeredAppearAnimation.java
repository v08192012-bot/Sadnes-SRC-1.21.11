package tech.sadnes.client.animation;

public final class StaggeredAppearAnimation {
    private final Animation progressAnimation = new Animation();

    public void reset() {
        this.progressAnimation.forceValue(0.0);
    }

    public void update() {
        this.progressAnimation.update();
        this.progressAnimation.animate(1.0, 0.73, Easings.LINEAR);
    }

    public float getProgress(int i) {
        float f = (this.progressAnimation.get() * 0.73F - Math.min(i, 10) * 0.028F) / 0.45F;
        if (f <= 0.0F) {
            return 0.0F;
        } else {
            return f >= 1.0F ? 1.0F : (float)Easings.QUAD_OUT.ease(f);
        }
    }

    public float getOffset(int i) {
        return (1.0F - this.getProgress(i)) * 9.0F;
    }
}
