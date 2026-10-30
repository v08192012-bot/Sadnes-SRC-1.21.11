package tech.sadnes.client.feature.impl.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

final class AutoCrystalRenderer {
    private final AutoCrystalRenderer.AnimatedBox placeBox = new AutoCrystalRenderer.AnimatedBox();
    private final AutoCrystalRenderer.AnimatedBox targetBox = new AutoCrystalRenderer.AnimatedBox();
    private final Animation pulseAnimation = new Animation();
    private final List<AutoCrystalRenderer.Spark> sparks = new ArrayList<>();

    void onCrystalHit(BlockHitResult blockHitResult) {
        this.targetBox.update(blockHitResult.getPos(), true);
        this.pulseAnimation.update();
        this.pulseAnimation.animate(1.0, 0.055, Easings.CUBIC_OUT).setOnFinish(() -> this.pulseAnimation.animate(0.0, 0.28, Easings.CUBIC_OUT));
        long l = System.nanoTime();
        this.sparks.removeIf(spark -> spark.age(l) >= spark.lifetime());
        int i = this.sparks.size() + 12 - 144;
        if (i > 0) {
            this.sparks.subList(0, i).clear();
        }

        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        Vec3d vec3d = blockHitResult.getSide().getDoubleVector();
        Vec3d vec3d2 = blockHitResult.getPos().add(vec3d.multiply(0.025));

        for (int i2 = 0; i2 < 12; i2++) {
            Vec3d vec3d3 = new Vec3d(threadLocalRandom.nextDouble(-1.0, 1.0), threadLocalRandom.nextDouble(-1.0, 1.0), threadLocalRandom.nextDouble(-1.0, 1.0));
            Vec3d vec3d4 = vec3d3.subtract(vec3d.multiply(vec3d3.dotProduct(vec3d))).add(vec3d.multiply(threadLocalRandom.nextDouble(0.35, 1.0))).normalize();
            this.sparks.add(new AutoCrystalRenderer.Spark(vec3d2, vec3d4.multiply(threadLocalRandom.nextDouble(0.9, 2.4)), l, threadLocalRandom.nextDouble(0.35, 0.6)));
        }
    }

    void reset() {
        this.placeBox.clear();
        this.targetBox.clear();
        this.resetEffects();
    }

    private void resetEffects() {
        this.pulseAnimation.setOnFinish(null);
        this.pulseAnimation.forceValue(0.0);
        this.pulseAnimation.setRunning(false);
        this.sparks.clear();
    }

    void render(RenderEvent renderEvent, BlockPos blockPos, Vec3d vec3d, boolean flag) {
        this.placeBox.update(blockPos == null ? null : Vec3d.ofCenter(blockPos), flag);
        this.targetBox.update(vec3d, flag);
        this.pulseAnimation.update();
        if (!flag) {
            this.resetEffects();
        }

        long l = System.nanoTime();
        this.sparks.removeIf(spark -> spark.age(l) >= spark.lifetime());
        if (this.placeBox.currentPos != null || this.targetBox.currentPos != null || !this.sparks.isEmpty()) {
            int i = Theme.getPrimary();
            float f = this.pulseAnimation.get();
            Vec3d vec3d2 = renderEvent.getCamera().getCameraPos();
            MatrixStack matrixStack = renderEvent.getMatrices();

            try (Render3DUtil.BoxBatch boxBatch = Render3DUtil.glowBoxBatch()) {
                this.placeBox.render(boxBatch, matrixStack, vec3d2, 0.502, i, 0.18F);
                this.targetBox.render(boxBatch, matrixStack, vec3d2, 0.1 + f * 0.09, ColorUtil.multiplyAlpha(i, 0.24F + f * 0.3F), 0.35F);
                this.targetBox.render(boxBatch, matrixStack, vec3d2, 0.055 * (1.0 + f * 0.85), Theme.tintWhite(0.5F + f * 0.3F), 0.65F + f * 0.2F);
                boxBatch.draw();
            }

            if (!this.sparks.isEmpty()) {
                this.renderSparks(matrixStack, l);
            }
        }
    }

    private void renderSparks(MatrixStack matrixStack, long l) {
        int i = Theme.getPrimary();
        int i2 = Theme.tintWhite(0.75F);

        try (
            Render3DUtil.LineBatch lineBatch2 = Render3DUtil.glowLineBatch();
            Render3DUtil.LineBatch lineBatch3 = Render3DUtil.lineBatch();
        ) {
            for (AutoCrystalRenderer.Spark spark : this.sparks) {
                double d = spark.age(l);
                Vec3d vec3d = spark.position(d + 0.012);
                if (ProjectionUtil.isVisible(vec3d.x, vec3d.y, vec3d.z)) {
                    Vec3d vec3d2 = spark.position(Math.max(0.0, d - 0.035));
                    float f = (float)Math.pow(1.0 - d / spark.lifetime(), 2.0);
                    int i3 = ColorUtil.multiplyAlpha(i2, f);
                    int i4 = ColorUtil.multiplyAlpha(i, f * 0.12F);
                    lineBatch2.line(matrixStack, vec3d2, vec3d, ColorUtil.multiplyAlpha(i4, 0.35F), ColorUtil.multiplyAlpha(i, f * 0.5F), 4.0F);
                    lineBatch3.line(matrixStack, vec3d2, vec3d, i4, i3, 1.5F);
                }
            }

            lineBatch2.draw();
            lineBatch3.draw();
        }
    }

    static final class AnimatedBox {
        private final Animation moveAnimation = new Animation();
        private final Animation alphaAnimation = new Animation();
        private final StopWatch lastSeenTimer = new StopWatch();
        private Vec3d fromPos;
        private Vec3d toPos;
        Vec3d currentPos;

        void update(Vec3d vec3d, boolean flag) {
            this.moveAnimation.update();
            this.alphaAnimation.update();
            if (this.currentPos != null) {
                this.currentPos = this.fromPos.lerp(this.toPos, this.moveAnimation.getValue());
            }

            if (vec3d != null) {
                this.lastSeenTimer.reset();
                if (this.currentPos == null || !flag) {
                    this.fromPos = this.toPos = this.currentPos = vec3d;
                    this.moveAnimation.forceValue(1.0);
                } else if (!vec3d.equals(this.toPos)) {
                    this.fromPos = this.currentPos;
                    this.toPos = vec3d;
                    this.moveAnimation.forceValue(0.0);
                    this.moveAnimation.animate(1.0, 0.18, Easings.CUBIC_OUT);
                }
            }

            boolean flag2;
            double d;
            if (vec3d != null || flag && this.currentPos != null && !this.lastSeenTimer.hasPassed(100.0)) {
                flag2 = true;
                d = 1.0;
            } else {
                flag2 = false;
                d = 0.0;
            }

            double d2 = d;
            if (!flag) {
                this.alphaAnimation.forceValue(d2);
            } else if (this.alphaAnimation.getToValue() != d2) {
                this.alphaAnimation.animate(d2, flag2 ? 0.16 : 0.28, Easings.CUBIC_OUT);
            }

            if (!flag2 && this.alphaAnimation.get() <= 0.001F) {
                this.clear();
            }
        }

        void render(Render3DUtil.BoxBatch boxBatch, MatrixStack matrixStack, Vec3d vec3d, double d, int i, float f) {
            if (this.currentPos != null && !(this.alphaAnimation.get() <= 0.001F)) {
                Box box = new Box(this.currentPos, this.currentPos).expand(d);
                if (ProjectionUtil.isVisible(box)) {
                    Box box2 = box.offset(-vec3d.x, -vec3d.y, -vec3d.z);
                    int i2 = ColorUtil.multiplyAlpha(i, this.alphaAnimation.get());
                    boxBatch.fill(matrixStack, box2, ColorUtil.multiplyAlpha(i2, f));
                    boxBatch.outline(matrixStack, box2, i2);
                }
            }
        }

        void clear() {
            this.fromPos = null;
            this.toPos = null;
            this.currentPos = null;
            this.moveAnimation.forceValue(1.0);
            this.alphaAnimation.forceValue(0.0);
        }
    }

    record Spark(Vec3d origin, Vec3d velocity, long createdNanos, double lifetime) {
        double age(long l) {
            return (l - this.createdNanos) / 1.0E9;
        }

        Vec3d position(double d) {
            double d2 = (1.0 - Math.exp(-3.5 * d)) / 3.5;
            return this.origin.add(this.velocity.multiply(d2)).add(0.0, -1.4 * d * d, 0.0);
        }
    }
}
